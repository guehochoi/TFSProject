package Master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class FileSystem {

	public final String currentDir = System.getProperty("user.dir");
	private final FSLogger fsLogger = new FSLogger(this);

	public class TFSFile
	{
		String fileName = "";
		String md5FileName = "";
		Semaphore sema = new Semaphore(1);
		LinkedList<ChunkTracker.ChunkServerInfo> chunkServers = new LinkedList<ChunkTracker.ChunkServerInfo>();
	}

	public class TFSDirectory
	{
		SortedSet<TFSFile> files;
		SortedSet<String> subdirectories;

		public TFSDirectory()
		{        
			subdirectories = new TreeSet<String>();

			files = new TreeSet<TFSFile>(new Comparator<TFSFile>()
			{
				public int compare(TFSFile a, TFSFile b)
				{
					return a.fileName.compareTo(b.fileName);
				}
			});
		}
	}

	ConcurrentHashMap<String,TFSDirectory> directoryHash;

	public FileSystem()
	{
		directoryHash = new ConcurrentHashMap<String,TFSDirectory>();
		directoryHash.put("\\", new TFSDirectory());
		fsLogger.start();

		File backupFile = new File(fsLogger.persistentFileName);

		if(backupFile.exists())
		{
			restoreFS();
		}
	}

	public String createFile(String filename, List<ChunkTracker.ChunkServerInfo> servers)
	{
		TFSFile test = getFile(filename);

		if(test != null)
		{
			return filename + " already exists";
		}

		String directoryPath = getDirectoryPath(filename);

		if(directoryHash.containsKey(directoryPath) && isValidFileName(filename))
		{
			TFSFile file = new TFSFile();
			file.fileName = trimFileName(filename);
			file.md5FileName = getMD5(filename);

			String[] transactionString = new String[servers.size()*2 + 2];
			transactionString[0] = "createFile";
			transactionString[1] = filename;
			int count = 2;
			
			for(ChunkTracker.ChunkServerInfo info : servers)
			{
				transactionString[count] = info.ipAddress;
				transactionString[count+1] = Integer.toString(info.port);
				count += 2;
				file.chunkServers.add(info);
			}
			
			fsLogger.beginTransaction(transactionString);
			TFSDirectory dir = directoryHash.get(directoryPath);
			dir.files.add(file);
			fsLogger.commitTransaction();
			return "success";
		}

		return filename + ": invalid filename or directory";
	}

	public boolean createDirectory(String directoryName)
	{
		String parentDir = getDirectoryPath(directoryName);

		if(directoryHash.containsKey(parentDir) && isValidDirectoryName(directoryName))
		{
			fsLogger.beginTransaction("createDirectory",directoryName);
			TFSDirectory parentTFSDir = directoryHash.get(parentDir);
			parentTFSDir.subdirectories.add(directoryName);
			directoryHash.put(directoryName, new TFSDirectory());
			fsLogger.commitTransaction();
			return true;
		}

		return false;
	}

	public boolean deleteDirectory(String directoryPath)
	{
		String parentDir = getDirectoryPath(directoryPath);
		
		if(directoryHash.containsKey(directoryPath) && !directoryPath.equals("\\"))
		{
			fsLogger.beginTransaction("deleteDirectory",directoryPath);
			directoryHash.remove(directoryPath);
			TFSDirectory parent = directoryHash.get(parentDir);
			parent.subdirectories.remove(directoryPath);
			fsLogger.commitTransaction();
		}

		return true;
	}
	
	
	public boolean deleteFile(String filepath) {
		fsLogger.beginTransaction("deleteFile",filepath);
		String parentDir = getDirectoryPath(filepath);
		
		if(directoryHash.containsKey(parentDir))
		{
			TFSDirectory dir = directoryHash.get(parentDir);
			
			for(TFSFile f : dir.files)
			{
				if(f.fileName.equals(trimFileName(filepath)))
				{
					dir.files.remove(f);
					fsLogger.commitTransaction();
					break;
				}
			}

			return true;
		}
		
		return false;
	}

	private String getDirectoryPath(String path)
	{
		if(path != "" && path.contains("\\")) //Either the root directory or empty
		{
			int lastIndex = path.lastIndexOf('\\');

			if(lastIndex == 0)
			{
				return "\\";
			}
			else
			{
				return path.substring(0, path.lastIndexOf('\\'));
			}
		}

		return path;
	}

	private boolean isValidFileName(String filename)
	{
		// Not really sure what we're accepting as valid file names, so just return true for now.
		return true;
	}

	private boolean isValidDirectoryName(String directoryName)
	{
		// Not really sure what we're accepting as valid directory names, so just return true for now.
		return true;
	}

	private String trimFileName(String filename)
	{
		return filename.substring(filename.lastIndexOf('\\')+1);
	}

	public TFSFile getFile(String filename)
	{
		String parentDir = getDirectoryPath(filename);
		
		if(directoryHash.containsKey(parentDir))
		{
			TFSDirectory dir = directoryHash.get(parentDir);
			String searchFile = trimFileName(filename);
			
			for(TFSFile f : dir.files)
			{
				if(f.fileName.equals(searchFile))
				{
					return f;
				}
			}
		}
		
		return null;
	}

	public SortedSet<String> getSubdirectories(String directory)
	{
		if( directoryHash.containsKey(directory))
		{
			return directoryHash.get(directory).subdirectories;
		}
		else
		{
			return null;
		}
	}

	private String getMD5(String fileName)
	{
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(fileName.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			String hashtext = bigInt.toString(16);
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
			return hashtext;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void restoreFS()
	{
		File myFile = new File(fsLogger.persistentFileName);

		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(myFile));
			String readLine;

			while((readLine = br.readLine()) != null)
			{
				String[] split = readLine.split(" :");

				if(split[1] == null)
				{
					continue;
				}

				if(split[1].equals("d"))
				{
					String parentDir = this.getDirectoryPath(split[0]);

					if(directoryHash.containsKey(parentDir))
					{
						directoryHash.get(parentDir).subdirectories.add(split[0]);
						directoryHash.put(split[0], new TFSDirectory());
					}
				}
				else if(split[split.length - 1].equals("f"))
				{
					String dir = this.getDirectoryPath(split[0]);

					if(directoryHash.containsKey(dir))
					{
						TFSFile f = new TFSFile();
						f.fileName = trimFileName(split[0]);
						f.md5FileName = getMD5(split[0]);
						directoryHash.get(dir).files.add(f);
						
						for(int i = 1; i < split.length - 1; i++)
						{
							String ipAddress = split[i];
							int port = Integer.parseInt(split[i+1]);
							ChunkTracker.ChunkServerInfo info = new ChunkTracker.ChunkServerInfo(ipAddress,port,0);
							f.chunkServers.add(info);
						}
					}
				}
			}

			br.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
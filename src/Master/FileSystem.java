package Master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

public class FileSystem {

	public final String currentDir = System.getProperty("user.dir");
	private final FSLogger fsLogger = new FSLogger(this);

	private class TFSFile
	{
		String fileName = "";
		byte[] fileContent;
	}

	private class TFSDirectory
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

	Hashtable<String,TFSDirectory> directoryHash;

	public FileSystem()
	{
		directoryHash = new Hashtable<String,TFSDirectory>();
		directoryHash.put("\\", new TFSDirectory());
		fsLogger.start();
		
		File backupFile = new File(fsLogger.persistentFileName);
		
		if(backupFile.exists())
		{
			restoreFS();
		}
	}

	public boolean createFile(String filename)
	{
		String directoryPath = getDirectoryPath(filename);

		if(directoryHash.containsKey(directoryPath) && isValidFileName(filename))
		{
			fsLogger.beginTransaction("createFile",filename);
			TFSFile file = new TFSFile();
			file.fileName = trimFileName(filename);

			try {
				File myFile = new File(currentDir + filename);
				
				if(myFile.exists())
				{
					fsLogger.removeTransaction();
					System.err.println("Tried to create a file that already existed.");
					return false;
				}
				
				myFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			TFSDirectory dir = directoryHash.get(directoryPath);
			dir.files.add(file);
			fsLogger.commitTransaction();
			return true;
		}

		return false;
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

			File myDir = new File(currentDir + directoryName);
			
			if(myDir.exists())
			{
				fsLogger.removeTransaction();
				System.err.println("Tried to create a directory that already existed.");
				return false;
			}
			
			myDir.mkdir();
			fsLogger.commitTransaction();
			return true;
		}

		return false;
	}

	public boolean deleteDirectory(String directoryPath)
	{
		TFSDirectory parentDir = directoryHash.get(directoryPath);
		parentDir.subdirectories.remove(directoryPath);

		if(directoryHash.containsKey(directoryPath))
		{
			fsLogger.beginTransaction("deleteDirectory",directoryPath);
			TFSDirectory dir = directoryHash.get(directoryPath);

			for(TFSFile file : dir.files)
			{
				File f = new File(currentDir + File.pathSeparator + directoryPath + File.pathSeparator + file.fileName);
				
				if (!f.exists()) {
					fsLogger.removeTransaction();
					System.err.println("Error: file not exist");
					return false;
				}
				
				if (!f.isFile()) {
					fsLogger.removeTransaction();
					System.err.println("Error: not file");
					return false;
				}
				
				if (f.delete()) {
					System.out.println(f.getPath() + " is deleted successfully");
				}else {
					fsLogger.removeTransaction();
					System.err.println("Error: file deletion");
					return false;
				}
			}

			for(String subdir : dir.subdirectories)
			{
				if (!deleteDirectory(subdir))
					return false;
			}
			
			fsLogger.commitTransaction();
		}
		
		return true;
	}
	
	public boolean writeFile(byte[] content, String filename) {
		String directoryPath = getDirectoryPath(filename);

		if(directoryHash.containsKey(directoryPath) && isValidFileName(filename))
		{
			fsLogger.beginTransaction("createFile",filename);
			TFSFile file = new TFSFile();
			file.fileName = trimFileName(filename);

			try {
				File myFile = new File(currentDir + filename);
				
				if(myFile.exists())
				{
					fsLogger.removeTransaction();
					System.err.println("Tried to create a file that already existed.");
					return false;
				}
				
				myFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			file.fileContent = content;
			TFSDirectory dir = directoryHash.get(directoryPath);
			dir.files.add(file);
			fsLogger.commitTransaction();
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

	public void printDirectory(String directoryPath, int depth)
	{
		if(directoryHash.containsKey(directoryPath))
		{
			System.out.println(directoryPath);
			TFSDirectory dir = directoryHash.get(directoryPath);

			for(TFSFile file : dir.files)
			{
				printTabs(depth+1);
				System.out.println(file.fileName);
			}

			for(String subdir : dir.subdirectories)
			{
				printTabs(depth+1);
				printDirectory(subdir,depth + 1);
			}
		}
	}

	private void printTabs(int numTabs)
	{
		for(int i = 0; i < numTabs; i++)
		{
			System.out.print("   ");
		}
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
				else if(split[1].equals("f"))
				{
					String dir = this.getDirectoryPath(split[0]);

					if(directoryHash.containsKey(dir))
					{
						TFSFile f = new TFSFile();
						f.fileName = split[0];
						directoryHash.get(dir).files.add(f);
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
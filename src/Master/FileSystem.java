package Master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

public class FileSystem {

	public final String currentDir = System.getProperty("user.dir");
	private final String backupFileName = "TFSBackup.txt";

	private class TFSFile{
		String fileName = "";
		// Honestly can't think of any other attributes a TFSFile would have, maybe a list of chunks values
		// or list of chunk names... This IS just the skeleton :)
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
		
		File backupFile = new File(backupFileName);
		
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
			TFSFile file = new TFSFile();
			file.fileName = trimFileName(filename);

			try {
				File myFile = new File(currentDir + filename);
				if(myFile.exists())
				{
					System.err.println("Tried to create a file that already existed.");
					return false;
				}
				myFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			TFSDirectory dir = directoryHash.get(directoryPath);
			dir.files.add(file);
			return true;
		}

		return false;
	}

	public boolean createDirectory(String directoryName)
	{
		String dirPath = getDirectoryPath(directoryName);

		if(directoryHash.containsKey(dirPath) && isValidDirectoryName(directoryName))
		{
			TFSDirectory parentDir = directoryHash.get(dirPath);
			parentDir.subdirectories.add(directoryName);
			directoryHash.put(directoryName, new TFSDirectory());

			File myDir = new File(currentDir + directoryName);
			myDir.mkdir();
		}

		return false;
	}

	public boolean deleteDirectory(String directoryPath)
	{
		TFSDirectory parentDir = directoryHash.get(directoryPath);
		parentDir.subdirectories.remove(directoryPath);

		if(directoryHash.containsKey(directoryPath))
		{
			TFSDirectory dir = directoryHash.get(directoryPath);

			for(TFSFile file : dir.files)
			{
				File f = new File(currentDir + File.pathSeparator + directoryPath + File.pathSeparator + file.fileName);
				if (!f.exists()) {
					System.err.println("Error: file not exist");
					return false;
				}
				if (!f.isFile()) {
					System.err.println("Error: not file");
					return false;
				}
				if (f.delete()) {
					// file deleted successfully
					System.out.println(f.getPath() + " is deleted successfully");
				}else {
					System.err.println("Error: file deletion");
					return false;
				}
			}

			for(String subdir : dir.subdirectories)
			{
				if (!deleteDirectory(subdir))
					return false;
			}
			
		}else {
			System.err.println("Directory hash is not present");
			return false;
		}
		return true;
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
	
	public boolean backupFS()
	{
		try {
			File myFile = new File(backupFileName);
			
			if(myFile.exists())
			{
				myFile.delete();
			}
			
			myFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		backupDirectory("\\");
		return true;
	}
	
	private void backupDirectory(String directory)
	{
		TFSDirectory dir = directoryHash.get(directory);
		File myFile = new File(backupFileName);
		
		try {
			FileWriter fw = new FileWriter(myFile.getAbsolutePath(),true);
			
			if(!directory.equals("\\"))
			{
				fw.append(directory + "\n");
			}
			
			for(TFSFile f : dir.files)
			{
				fw.append(f.fileName + "\n");
			}
			
			fw.close();
			
			for(String subDir : dir.subdirectories)
			{
				backupDirectory(subDir);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void restoreFS()
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(backupFileName));
			String readLine = null, currentDir = null;
			
			while((readLine = br.readLine()) != null)
			{
				if(readLine.contains("\\"))
				{
					currentDir = readLine;
					String parentDir = getDirectoryPath(currentDir);
					
					if(directoryHash.containsKey(parentDir))
					{
						directoryHash.put(currentDir, new TFSDirectory());
						directoryHash.get(parentDir).subdirectories.add(currentDir);
					}
				}
				else if(currentDir != null)
				{
					TFSFile f = new TFSFile();
					f.fileName = readLine;
					directoryHash.get(currentDir).files.add(f);
				}
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
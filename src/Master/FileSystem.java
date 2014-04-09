package Master;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

public class FileSystem {
	
	public final String currentDir = System.getProperty("user.dir");
	
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
	
	public void deleteDirectory(String directoryPath)
	{
		TFSDirectory parentDir = directoryHash.get(directoryPath);
		parentDir.subdirectories.remove(directoryPath);
		
		if(directoryHash.containsKey(directoryPath))
		{
			TFSDirectory dir = directoryHash.get(directoryPath);
			
			for(TFSFile file : dir.files)
			{
				//Do whatever we have to do to remove the file
			}
			
			for(String subdir : dir.subdirectories)
			{
				deleteDirectory(subdir);
			}
		}
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
}

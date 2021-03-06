package Master;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.nio.file.StandardOpenOption;


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

	public boolean appendDataToFile(String tfsFile, byte[] dataToAppend,
			int dataSize) {


		Path tfsPath = Paths.get(currentDir + tfsFile);
		File myFile = new File(currentDir + tfsFile);
		String directoryPath = getDirectoryPath(tfsFile);

		//Put bytes into buffer to append.
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(dataSize);
		byte[] sizeBytes = bb.array();

		if (directoryHash.containsKey(directoryPath)
				&& isValidFileName(tfsFile)) {
			fsLogger.beginTransaction("appendToFile", tfsFile);
			if (!myFile.exists()) {
				if (!createFile(tfsFile)) {
					fsLogger.removeTransaction();
				}
			}
			try {
				Files.write(tfsPath, sizeBytes, StandardOpenOption.APPEND);
			} catch (IOException e) {
				fsLogger.removeTransaction();
				e.printStackTrace();
			}
			try {
				Files.write(tfsPath, dataToAppend, StandardOpenOption.APPEND);
			} catch (IOException e) {
				fsLogger.removeTransaction();
				e.printStackTrace();
			}
			fsLogger.commitTransaction();
			return true;
		}

		return false;
	}

	/**
	 * Gets a byte array from the offset into file specified and of the size
	 * specified.
	 * 
	 * Gross amount of try/catch, could add throws instead, maybe later.
	 * 
	 * @param tfsFilePath
	 *            Filepath within TFS of file to read.
	 * @param offset
	 *            Offset to jump into file.
	 * @param size
	 *            Size of byte array to read.
	 * @return Byte array read.
	 */
	public byte[] readBytesFromFile(String tfsFilePath, int offset, int size) {
		byte[] bytesRead = new byte[size];
		File check = new File(currentDir + tfsFilePath);
		RandomAccessFile file = null;

		String directoryPath = getDirectoryPath(tfsFilePath);

		if (!check.exists()) {
			System.err.println("File specified does not exist.");
			return null;
		}

		if (directoryHash.containsKey(directoryPath)
				&& isValidFileName(tfsFilePath)) {
			try {
				file = new RandomAccessFile(currentDir + tfsFilePath, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				if (file.length() > offset) {
					file.seek(offset);
					file.read(bytesRead);
					file.close();
				} else {
					return null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bytesRead;
		}
		return null;
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
		if (parentDir == null) {
			System.err.println("No such path is in the hash: either the directory not exists, or backup file corrupted");
			return false;
		}
		parentDir.subdirectories.remove(directoryPath);

		String dirpath = directoryPath;
		if (directoryPath.indexOf("\\")==0) {
			dirpath = directoryPath.replaceFirst(Matcher.quoteReplacement("\\"), " ").trim();
		}
		dirpath = dirpath.replaceAll(Matcher.quoteReplacement("\\"), Matcher.quoteReplacement(File.separator));


		if(directoryHash.containsKey(directoryPath))
		{
			fsLogger.beginTransaction("deleteDirectory",directoryPath);
			TFSDirectory dir = directoryHash.get(directoryPath);

			for(TFSFile file : dir.files)
			{
				File f = null;
				if (file.fileName.contains("\\")) {
					f = new File(dirpath + File.separator + file.fileName.substring(file.fileName.lastIndexOf('\\')+1));
				}else {
					 f= new File(dirpath + File.separator + file.fileName);
				}

				if (!f.exists()) {
					fsLogger.removeTransaction();
					System.err.println("Error: "+f.getPath()+" not exist");
					return false;
				}

				if (!f.isFile()) {
					fsLogger.removeTransaction();
					System.err.println("Error: "+f.getPath()+" is not a file");
					return false;
				}

				if (f.delete()) {
					System.out.println(f.getPath() + " is deleted successfully");
				}else {
					fsLogger.removeTransaction();
					System.err.println("Error: "+f.getPath()+" failed to delete");
					return false;
				}
			}
			if (!dir.subdirectories.isEmpty()) {
				for(String subdir : dir.subdirectories)
				{
					if (!deleteDirectory(subdir)) {
						return false;
					}
				}
			}

			File f = new File(dirpath);
			if (f.delete()) {					
				System.out.println(f.getPath() + " is deleted successfully");
			}else {
				fsLogger.removeTransaction();
				System.err.println("Error: "+f.getPath()+" failed to delete");
				return false;
			}

			fsLogger.commitTransaction();
		}

		return true;
	}
	
	
	public boolean deleteFile(String filepath) {
		fsLogger.beginTransaction("deleteFile",filepath);
		File f= new File(filepath);
		
		if (!f.exists()) {
			fsLogger.removeTransaction();
			System.err.println("Error: "+f.getPath()+" not exist");
			return false;
		}

		if (!f.isFile()) {
			fsLogger.removeTransaction();
			System.err.println("Error: "+f.getPath()+" is not a file");
			return false;
		}

		if (f.delete()) {
			fsLogger.commitTransaction();
			System.out.println(f.getPath() + " is deleted successfully");
		}else {
			fsLogger.removeTransaction();
			System.err.println("Error: "+f.getPath()+" failed to delete");
			return false;
		}
		
		return true;
	}
	
	

	public boolean writeFile(String filename, byte[] data) {
		String directoryPath = getDirectoryPath(filename);

		if(directoryHash.containsKey(directoryPath) && isValidFileName(filename))
		{
			fsLogger.beginTransaction("writeFile",filename);

			try {
				File myFile = new File(currentDir + filename);

				if(!myFile.exists())
				{
					fsLogger.removeTransaction();
					System.err.println("Attempt to write to a file that does not exist");
					return false;
				}

				FileOutputStream fs = new FileOutputStream(myFile);
				fs.write(data);
				fs.close();
				fsLogger.commitTransaction();
				return true;
			} catch (IOException e) {
				fsLogger.removeTransaction();
				e.printStackTrace();
				return false;
			}
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

	@SuppressWarnings("finally")
	public boolean read(String src, String dst) {

		File srcFile = new File(currentDir + src);
		File dstFile = new File(dst);

		if(directoryHash.containsKey(getDirectoryPath(src)) && isValidFileName(src))
		{
			fsLogger.beginTransaction("read",src);
			if(!srcFile.exists()) {
				System.out.println(src + " does not exist.");
				fsLogger.removeTransaction();
				return false;
			}

			if(dstFile.exists())
			{
				System.out.println(dst + " already exists.");
				fsLogger.removeTransaction();
				return false;
			}

			FileInputStream fin = null;
			FileOutputStream fout = null;
			try {

				fin = new FileInputStream(srcFile);

				byte fileContent[] = new byte[(int)srcFile.length()];
				fin.read(fileContent);

				fout = new FileOutputStream(dstFile);
				fout.write(fileContent);

			} catch (FileNotFoundException e) {
				System.out.println(src + " not found.");
				fsLogger.removeTransaction();
				return false;
			}
			catch (IOException e) {
				e.printStackTrace();
				fsLogger.removeTransaction();
				return false;
			} finally {
				try {
					if (fin != null) {
						fin.close();
					}
					if(fout != null) {
						fout.close();
					}
					fsLogger.commitTransaction();
					return true;
				} catch (IOException ioe) {
					System.out.println("Error while closing stream: " + ioe);
					fsLogger.removeTransaction();
					return false;
				}
			}
		}
		return false;
	}

}
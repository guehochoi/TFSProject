package Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import Master.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

/** Test4:  Store a file on the local machine in a target TFS file specified by its path.

Input:  local file path, TFS file

Functionality:  If the TFS file exists then return an error message.  Otherwise, create the TFS
file, read the content of the local file and store it in the TFS File.

Example:  Test4 C:\MyDocuments\Image.png 1\File1.png If 1\File1.png exists then return error.
Otherwise, create 1/File1.png, read the content of C:\MyDocument\Image.png, write the retrieved
content into 1\File1.png */

public class Test4 {

	public static String rootDirectory = "\\usr";
	public FileSystem fs;

	public Test4() {
		fs = new FileSystem();
		fs.createDirectory(rootDirectory);
	}

	public void storeLocal(String localPath, String TFSpath) throws IOException {

		Path path = Paths.get(localPath);
		byte[] data = Files.readAllBytes(path);
		
		String[] split = localPath.split("/");
		String filename = split[split.length-1];
		
		fs.writeFile(data, filename);
	}

	public static void main(String[] args) {
		Test4 test = new Test4();
		try {
			test.storeLocal("/Users/aluo/Workspace/usc/485/TFSProject/README.md", "asdf");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
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

// NOTE: Please make sure to use quotes when entering a local path if it has spaces.

// Example Run: "C:\Users\Public\Pictures\Sample Pictures\Desert.jpg" \\Desert.jpg

public class Test4 {
	public static FileSystem fs = new FileSystem();

	public static void storeLocal(String localPath, String TFSpath) throws IOException {
		Path path = Paths.get(localPath);
		byte[] data = Files.readAllBytes(path);
		
		//fs.createFile(TFSpath);
	}

	public static void main(String[] args){
		if(args.length < 2)
		{
			System.err.println("Incorrect number of arguments");
			return;
		}
		else
		{
			try {
				storeLocal(args[0],args[1]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
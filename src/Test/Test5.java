package Test;

import java.util.ArrayList;
import java.util.List;

import Master.FileSystem;

/*	Test5:  Read the content of a TFS file and store it on the specified file on the local machine.
 *  Input:  TFS file, local file path
 *
 *  Functionality:  If the TFS file does not exist then return an error message.  
 *                  Otherwise, open the TFS file, read the content of the file, 
 *                  write the content of the file to the local filesystem file.
 *  
 *  Example:  Test5 1\File1.png C:\MyDocument\Pic.png
 *            If either 1\File1.png does not exist or 
 *            C:\MyDocument\Pic.png exists then return the appropriate error message.  
 *            Otherwise, open the TFS file 1\File1.png and read its content into memory.  
 *            Create and open C:\MyDocument\Pic.png, write the retrieved content into it, and close this file.
 *  */

public class Test5 {
	public static String rootDirectory = "";
	public FileSystem fs;

	public Test5() {
		fs = new FileSystem();
	}
	
	public void read(String src, String dst) {
	   //fs.read(src, dst);
	}

	public static void main(String[] args) {
		Test5 t5 = new Test5();

		if (args.length == 2) {
			String srcPath = args[0];
			String dstPath = args[1];

     		t5.read(srcPath, dstPath);
		}
    }

}

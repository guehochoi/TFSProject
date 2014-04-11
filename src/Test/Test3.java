package Test;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.regex.Matcher;

import Master.FileSystem;

/**	Test3:  Delete a hierarchical directory structure including the files in those directories.
	Input:  Path 
	Functionality:  The input path identifies the directory whose content along with itself must be deleted. 
	
	Note:  When an adversary invokes Test3 twice (or more) in a row, the application should return the meaningful error messages produced by TFS.
	
	Example:  Test3 1\2
	Assuming the directory sturcture from Test2 above, this test would delete 3 directories and 9 files.
	The deleted directories are 1\2, 1\2\4 and 1\2\5.  The fires deleted are:
	1\2\File1
	1\2\File2
	1\2\File3
	1\2\4\File1
	1\2\4\File2
	1\2\4\File3
	1\2\5\File1
	1\2\5\File2
	1\2\5\File3*/


public class Test3 {
	
	FileSystem fs;
	
	public Test3(){
		fs = new FileSystem();
	}
	
	public void delDirectoryPath(String tfsDirPath){
		fs.deleteDirectory(tfsDirPath);
	}
	
	public static void main(String[] args) {
		Test3 t3 = new Test3();
		
		String dirPath = args[0];
		t3.delDirectoryPath(dirPath);
		System.out.println("Directories and files under " + dirPath + " were deleted.");
	}
	
}

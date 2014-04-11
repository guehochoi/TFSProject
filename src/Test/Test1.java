package Test;

import java.util.ArrayList;
import java.util.List;

import Master.FileSystem;

/*	Test1:  Create a hierarchical directory structure.  Its input is the number of directories to create and is a
 *	value greater than 1.  This test program creates a directory named "1" and two subdirectories underneath it,
 *	2 and 3.  It repeats the process for these subdirectories recursively creating a subdirectory for each leaf
 *  directory until it has created the total number of specified directories. 
 *	
 *	Input:  an integer denoting the number of directories
 * 	Note:  When an adversary invokes Test1 twice (or more) in a row, the application should return the meaningful
 *  error messages produced by TFS.
 *  Example:  Test1 7
 *  With the input value 7, the resulting directory structure would be
 *  1
 *  1\2
 *  1\3
 *  1\2\4
 *  1\2\5
 *  1\3\6
 *  1\3\7
 *  */

public class Test1 {
	public static String rootDirectory = "\\usr";
	public FileSystem fs;

	public Test1() {
		fs = new FileSystem();
		fs.createDirectory(rootDirectory);
	}
	
	public Test1(int maxDepth){
		directoryCreate(maxDepth, 1, rootDirectory);
	}

	public void directoryCreate(int maxDepth, int currDir, String rootDir) {
		if (currDir > maxDepth) {
			return;
		}
		
		String myDir = rootDir + "\\" + currDir;
		fs.createDirectory(myDir);
		
		directoryCreate(maxDepth, currDir * 2, myDir);
		directoryCreate(maxDepth, (currDir * 2) + 1, myDir);
	}
	

	public static void main(String[] args) {
		Test1 test1 = new Test1();

		int dirNumber = 0;
		if (args.length > 0) {
			try {
				dirNumber = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Argument " + args[0]
						+ " must be an integer.");
				System.exit(1);
			}
		}

		if (dirNumber < 1) {
			System.err.println(dirNumber + " is not a valid input.");
		}

		test1.directoryCreate(dirNumber, 1, rootDirectory);
		System.out.println("Directories created.");
	}
}

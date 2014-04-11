package Test;

import Master.FileSystem;

/*	Test2: Create N files in a directory and its subdirectories until the leaf subdirectories.  Each file
 *  in a directory is named File1, File2, ..., FileN
 *  Input:  Path, N
 *  Functionality:  The Path identifies the root directory and its subdirectories that should have X files.
 *  It might be "1\2" in the above example.
 *  N is the number of files to create in path and each of its subdirectories
 *  
 *  Note:  When an adversary invokes Test2 twice (or more) in a row, the application should return the
 *  meaningful error messages produced by TFS.
 *  Example:  Test2 1\2 3
 *  Assuming the directory structure from the Test1 example above, this Test would create 3 files in 
 *  each directory 1\2, 1\2\4 and 1\2\5.  The files in each directory would be named File1, File2, and File3.
 *  
 *  */

public class Test2 {
	
	public FileSystem fs;
	boolean rootBool;
	
	public Test2() {
		rootBool = true;
		fs = new FileSystem();
	}

	public void createFiles(int numFiles, String rootDir) {
		
		//Only run this on first rootdir.
		//if(rootBool){
		for (int i = 1; i <= numFiles; i++) {
			String fileName = "";
			fileName = "\\File" + i + ".txt";
			fs.createFile(rootDir + fileName);
		}
			//rootBool = false;
		//}

		for (String subDir : fs.getSubdirectories(rootDir)) {
				createFiles(numFiles,subDir);
		}
	}

	public static void main(String[] args) {
		String dirPath = "\\usr\\";
		int numFiles = 0;
		Test2 t2 = new Test2();

		if (args.length > 0) {
			dirPath = dirPath + args[0];
			try {
				numFiles = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Argument " + args[1]
						+ " must be an integer.");
				System.exit(1);
			}
		}
		t2.createFiles(numFiles, dirPath);
	}
}

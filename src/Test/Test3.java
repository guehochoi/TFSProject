package Test;

import java.io.File;
import java.util.Stack;

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
	public static String rootDirectory = "\\usr";
	public FileSystem fs;
	
	private enum status {
		SUCCESS, PATH_NOT_EXIST, ATTEMPT_ON_ROOT, FILE_FAILURE, DIR_FAILURE
	}
	public Test3() {
		fs = new FileSystem();
	}
	
	/**
	 * 
	 * @param dirpath directory path
	 * @return status
	 */
	public static status delDir(String dirpath) {
		String abspath = rootDirectory + File.separator + dirpath;
		// ask master if abspath exists
			// if not, return status.PATH_NOT_EXIST
		// if the directory is root directory, (eg. null or space)
			// return ATTEMPT_ON_ROOT
		String currentPath = abspath;
		while (true) {
			// if there exists subdirectory in current path
				// currentPath = subdirectory path
				// continue;
			// remove all files in the directory,
				// return status.FILE_FAILURE if fails to delete file
			// exist the directory (currentPth = upperdir)
			// remove the exited directory (use last index of file.seperator)
				// return status.DIR_FAILURE if fails to delete dir
			// if currentPath = abspath and there exists no currentPath
				// remove all files in the directory, and then
				break;
		}
		
		return status.SUCCESS;
	}
	
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("invalid argument");
			System.exit(1);
		}
		switch (delDir(args[0])) {
		case SUCCESS:
			System.out.println("Successfully Deleted " + args[0]);
			break;
		case PATH_NOT_EXIST:
			System.out.println("Error: The path: " + args[0] + " does not exist");
			break;
		case ATTEMPT_ON_ROOT:
			System.out.println("Error: Root cannot be deleted");
			break;
		case FILE_FAILURE:
			System.out.println("Error: File cannot be deleted");
			break;
		case DIR_FAILURE:
			System.out.println("Error: Directory cannot be deleted");
		default: System.out.println("invalid error"); break;
		}
	}
	
}
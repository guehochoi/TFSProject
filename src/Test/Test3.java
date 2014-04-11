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
	public status delDir(String dirpath) {
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
	
	/**
	 * 
	 */
	public boolean delDir2(String dirpath) {
		String str = dirpath;
		str = str.replaceAll("/+", Matcher.quoteReplacement("\\"));
		return fs.deleteDirectory(str);
	}
	
	
	public void testSetup() {
		
		String filesToCreate[] = {
				"1\\2\\File2",
				"1\\2\\File3",
				"1\\2\\4\\File1",
				"1\\2\\4\\File2",
				"1\\2\\4\\File3",
				"1\\2\\5\\File1",
				"1\\2\\5\\File2",
				"1\\2\\5\\File3"
		};
		
		File f = null;
		fs.createDirectory(rootDirectory);
		
		for (String fn : filesToCreate) {
			
			String[] split = fn.split("\\\\");
			for (int i=0; i < split.length; i++) {
				StringBuilder fnbuild = new StringBuilder();
				for (int j=0; j <= i; j++) {
					if (j==0)
						fnbuild.append(split[j]);
					else 
						fnbuild.append(File.separator + split[j]);
				}
				f = new File("usr"+File.separator+fnbuild.toString());
				if (!f.exists()) {
					if (fnbuild.toString().contains("File")) {
						System.out.println("Creating " + f.getAbsolutePath());
						String str = rootDirectory + "\\" + fnbuild.toString();
						str.replaceAll(Matcher.quoteReplacement(File.separator), Matcher.quoteReplacement("\\"));
						fs.createFile(str);
					}else {
						System.out.println("Creating " + f.getAbsolutePath());
						String str = rootDirectory + "\\" + fnbuild.toString();
						str.replaceAll(Matcher.quoteReplacement(File.separator), Matcher.quoteReplacement("\\"));
						fs.createDirectory(str);
					}
				}
			}			
		}
		
		
		
	}
	
	public static void main(String args[]) {
		
		if (args.length != 1) {
			System.out.println("invalid argument");
			System.exit(1);
		}
		Test3 t = new Test3();
		//t.testSetup();  /* uncomment this when previous tests are run before */
		if (t.delDir2("\\usr\\"+args[0])) {
			System.out.println("Deletion success");
		}else {
			System.err.println("Deletion fail");
		}
//		switch (t.delDir(args[0])) {
//		case SUCCESS:
//			System.out.println("Successfully Deleted " + args[0]);
//			break;
//		case PATH_NOT_EXIST:
//			System.out.println("Error: The path: " + args[0] + " does not exist");
//			break;
//		case ATTEMPT_ON_ROOT:
//			System.out.println("Error: Root cannot be deleted");
//			break;
//		case FILE_FAILURE:
//			System.out.println("Error: File cannot be deleted");
//			break;
//		case DIR_FAILURE:
//			System.out.println("Error: Directory cannot be deleted");
//		default: System.out.println("invalid error"); break;
//		}
		
		
	}
	
}

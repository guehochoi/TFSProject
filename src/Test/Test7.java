package Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import Master.FileSystem;

/*	Test 7:  Count the number of logical files stored in a TFS file using Test6 and printout 
 * 	the results.
 * 	Input:  A TFS file generated using Test6
 * 	Functionality:  If the input TFS file does not exist then return an error.  Otherwise, 
 * 	counts the number of logical files stored in a TFS file (generated using Test6) by 
 * 	reading the size and payload pairs in the specified file name.
 * 	Example:  Test7 1/File1.haystack
 * 	Assumption:  Input file, 1/File1.haystack, is generated using Test6.
 * 	*/

public class Test7 {

	FileSystem fs;
	int fileCount;
	
	public Test7() {
		fs = new FileSystem();
		fileCount = 0;
		
	}
	
	public void countFilesContained(String tfsPath, int offset, int size){
		byte[] bytesRead = fs.readBytesFromFile(tfsPath, offset, size);
		if(bytesRead == null){
			return;
		}
		int newOffset = ByteBuffer.wrap(bytesRead).getInt();
		fileCount++;
		countFilesContained(tfsPath, newOffset + offset + size, size );
	}

	public static void main(String[] args) {
		Test7 t7 = new Test7();
		
		if (args.length != 1) {
			System.err.println("Invalid number of arguments.");
		}
		String tfsFileName = args[0];
		t7.countFilesContained(tfsFileName, 0, 4);
		System.out.println("Files contained in " + tfsFileName + " = " + t7.fileCount);
	}

}

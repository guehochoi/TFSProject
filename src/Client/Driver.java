package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import Client.Client.OpenTFSFile;

public class Driver {

	public enum Command {
		CAT, CD, CP, EXIT, HELP, KILLCS, LS, LSCS, MKDIR, MKFILE, PWD, RM, RMDIR, UNKNOWN, UNIT1, UNIT2, UNIT3, UNIT4, UNIT5, UNIT6, UNIT7, UNIT8
	};

	Client myClient = new Client();
	Hashtable<String,Command> commandHash = new Hashtable<String,Command>();

	public static void main(String[] args) {
		Driver myDriver = new Driver();
		myDriver.start();
	}

	public Driver()
	{
		setupHashCommands(commandHash);
	}

	public void start()
	{
		Command lastCommand = Command.UNKNOWN;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		while(lastCommand != Command.EXIT)
		{
			System.out.print("> ");
			String line = "";

			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] args = line.split(" ");

			if(commandHash.containsKey(args[0].toLowerCase()))
			{
				lastCommand = processCommand(args);
			}
			else
			{
				lastCommand = Command.UNKNOWN;
				System.out.println("Command not found. Type help for a list of commands.");
			}
		}
	}

	public Command processCommand(String[] args)
	{
		Command currentCommand = commandHash.get(args[0].toLowerCase());

		switch(currentCommand)
		{
		case CD:
			if(args.length < 2)
			{
				System.out.println("Invalid input to cd command (must specify directory name)");
			}
			else if(!myClient.changeDirectory(args[1]))
			{
				System.out.println(args[1] + ": no such directory");
			}
			break;
		case CP:
			if(args.length < 3)
			{
				System.out.println("Invalid input to cp, please specify local file location and remote file location");
			}
			else
			{
				copyFile(args[1],args[2]);
			}
		case EXIT:
			break;
		case HELP:
			printHelp();
			break;
		case KILLCS:
			if(args.length != 3)
			{
				System.out.println("Invalid arguments to killcs, please specify an ip address and port");
				break;
			}

			killChunkServer(args[1],args[2]);
			break;
		case MKDIR:
			if(args.length < 2)
			{
				System.out.println("Invalid input to mkdir command (must specify directory path or name)");
				break;
			}
			
			String createResult = myClient.createDirectory(args[1]);
			
			if(!createResult.contains("success"));
			{
				System.out.println(createResult);
			}
			break;
		case MKFILE:
			makeFile(args);
			break;
		case LS:
			String[] result = myClient.lsDirectory();
			if(result != null)
			{
				for(String s : result)
				{
					System.out.println(s);
				}
			}
			break;
		case LSCS:
			String[] lscs = myClient.lscs();
			if(lscs != null)
			{
				for(String s : lscs)
				{
					System.out.println(s);
				}
			}
			break;
		case PWD:
			System.out.println(myClient.printWorkingDirectory());
			break;
		case RM:
			if(args.length < 2)
			{
				System.out.println("Invalid input to rm command (must specify file path or name)");
			}
			else if(!myClient.removeFile(args[1]))
			{
				System.out.println("Unable to remove file " + args[1]);
			}
			break;
		case RMDIR:
			if(args.length < 2)
			{
				System.out.println("Invalid input to rmdir command (must specify file path or name)");
			}
			else if(!myClient.removeDirectory(args[1]))
			{
				System.out.println("Unable to remove directory " + args[1]);
			}
			break;
		case UNKNOWN:
			break;
		case UNIT1:
			if (args.length < 2) {
				System.out
						.println("Invalid input to unit1 command (must specify number of directories to create)");
			} else {
				int max = Integer.parseInt(args[1]);
				int fan = Integer.parseInt(args[2]);
				if (fan == 0)
					simpleUnit1(max);
				if (fan > max)
					fan = max;
				unit1(max, fan);
			}
			break;
		case UNIT2:
			if (args.length < 2) {
				System.out
						.println("Invalid input to unit2 command (must specify number of directories to create)");
			} else {
				unit2(args[1], Integer.parseInt(args[2]));
			}
			break;
		case UNIT3:
			if (args.length < 2) {
				System.out
						.println("Invalid input to unit3 command (must specify number of directories to create)");
			}
			unit3(args[1]);
			break;
		case UNIT4:
			if (args.length != 2) {
				System.out.println("Incorrect number of arguments for unit4.");
			} else {
				try {
					unit4(args[1], args[2]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			break;
		case UNIT5:
			if (args.length != 2) {
				System.out.println("Incorrect number of arguments for unit5.");
			} else {
				unit5(args[1], args[2]);
			}
			break;
		case UNIT6:
			if (args.length != 3) {
				System.err.println("Check arguments.");
				break;
			}
			unit6(args);
			break;
		case UNIT7:
			if (args.length != 2) {
				System.err.println("Check arguments.");
				break;
			}
			unit7(args);

			break;
		case UNIT8:
			break;

		default:

			break;
		}

		return currentCommand;
	}

	public void makeFile(String[] args)
	{
		if(args.length < 2)
		{
			System.out.println("Invalid input to mkfile command (must specify file path or name) (optional: #replicas)");
		}
		else
		{
			int numReplicas = 1;

			if(args.length == 3)
			{
				try
				{
					int newReplicas = Integer.parseInt(args[2]);
					numReplicas = newReplicas;
				}
				catch(NumberFormatException e)
				{
					System.out.println("Invalid argument passes to mkfile. Number of replicas must be an integer");
				}
			}
			String ret = myClient.createFile(args[1],numReplicas);

			if(!ret.contains("success"))
			{
				System.out.println(ret);
			}
		}
	}

	public void copyFile(String localPath, String remotePath)
	{
		Client.OpenTFSFile file = myClient.openFile(remotePath);

		try {
			Path path = Paths.get(localPath);
			byte[] data = Files.readAllBytes(path);

			if(file.openResult[0].contains("success"))
			{
				myClient.writeFile(file, data);
			}
			else
			{
				System.out.println(file.openResult[0]);
			}

			myClient.closeFile(file);
		} catch (IOException e) {
			System.err.println("Error reading local file (verify path?)");
			myClient.closeFile(file);
		}
	}
	
	public void killChunkServer(String ipAddress, String port)
	{
		int myPort = 0;

		try
		{
			myPort = Integer.parseInt(port);
		}
		catch(NumberFormatException e)
		{
			System.out.println("Invalid port, please specify an integer");
		}
		
		myClient.killChunkServer(ipAddress, myPort);
	}

	/**
	 * Unit 1 creates directories in a 'fanout' manner. The first number passed
	 * as an argument is the number of directories to be created the second is
	 * the fanout for those directories.
	 * 
	 * @param maxDirectories total number of directories to create
	 * @param fanout how many subdirectories exist in each directory
	 */
	public void unit1(int maxDirectories, int fanout){
		makeDirs(maxDirectories, fanout);
	}
	
	/**
	 * Makes directory structure with no fanout.  This is the simple case.
	 * @param max total number of directories to create
	 */
	public void simpleUnit1(int max){
		for(int i = 1; i<max+1; i++){
			myClient.createDirectory("\\" + i);
		}
	}
	
	/**
     * Makes the directory structure with the given depth and 
     * fan limits in a tree pattern, filling each rung first
     * @param depth how deep the directory goes
     * @param fan how wide the tree is (fanout)
     */
    public void makeDirs(int depth, int fan) {
        String one = "\\1";
        myClient.createDirectory(one);
        for (int i = 2; i < depth + 1; i++) {
        	String toCreate = (one + getFileName(i, fan, ""));
            myClient.createDirectory(toCreate.substring(0, toCreate.length()-1));
        }
    }

    /**
     * Recursively determines the path name of a given child, with the given
     * branching factor and accumulates in the string field
     * @param child which directory do you want to find the parent of?
     * @param fan branching factor
     * @param name accumulator for the filename
     * @return the file name relative to the base 1/ directory
     */
    public String getFileName(int child, int fan, String name) {
        if (child == 1) {
            return "\\" + name;
        }
        else {
            return getFileName(getParent(child, fan), fan, child + "\\" + name);
        }
    }

    /**
     * Uses modular division to determine the parent of the given
     * node with the given branching factor 
     * @param child
     * @param fan branching factor
     * @return the given childs parent in the tree
     */
    public int getParent(int child, int fan) {
        return (child + 1) / fan;
    }
	
	/**
	 * Unit 2 creates files within each directory and subdirectory specified.
	 * @param rootDir directory to start creating files within.
	 * @param numFiles number of files to be created within that directory
	 */
	public void unit2(String rootDir, int numFiles) {
		for (int i = 1; i <= numFiles; i++) {
			String fileName = "";
			fileName = "\\File" + i + ".txt";
			myClient.createFile(rootDir + fileName, 3);
		}
		String[] subDirs = myClient.getSubdirectories(rootDir);
		if (subDirs.length == 0 || subDirs == null || subDirs[0].equals("")) {
			return;
		}
		for (String subDir : subDirs) {
			subDir = subDir.substring(0, subDir.length() - 1 );
			unit2(rootDir + "\\" +  subDir, numFiles);
		}
	}
	
	/**
	 * Unit 3 deletes the directory specified
	 * @param directoryName name of directory to delete
	 */
	public void unit3(String directoryName){
		myClient.removeDirectory(directoryName);
	}

	public void unit4(String localPath, String TFSpath) throws IOException {
		Path path = Paths.get(localPath);
		byte[] data = Files.readAllBytes(path);

		myClient.createFile(TFSpath, 1);
		OpenTFSFile file = myClient.openFile(TFSpath);
		myClient.writeFile(file, data);
		myClient.closeFile(file);
	}
	
	public void unit5(String src, String dst) {
		OpenTFSFile file = myClient.openFile(src);
		myClient.readFile(file);
		myClient.closeFile(file);
	}

	public void unit6(String[] args) {
		//Get number of bytes in local file.
		String localFile = args[1]; 
		String TFSFile = args[2];

		Path localPath = Paths.get(localFile);
		byte[] localData = null;

		try {
			localData = Files.readAllBytes(localPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ByteBuffer bb = ByteBuffer.allocate( localData.length);
		//bb.putInt(localData.length);
		bb.put(localData);
		OpenTFSFile openFile = myClient.openFile(TFSFile);
		if(openFile.openResult[0].equals("File not found")) {
			myClient.createFile(TFSFile, 3);
			
			if(!openFile.openResult[0].equals("success"))
			{
				System.out.println(openFile.openResult[0]);
				myClient.closeFile(openFile);
			}

			openFile = myClient.openFile(TFSFile);
		}

		myClient.appendFile(openFile, bb.array());
		myClient.closeFile(openFile);
	}

	public void unit7(String[] args) {
		String TFSFile = args[1];
		OpenTFSFile openFile = myClient.openFile(TFSFile);
		if(openFile.openResult[0].equals("File not found")) {
			System.out.println("File not found");
			return;
		}

		int[] fileCount = {0}; 
		countFilesContained(openFile, 0, 4, fileCount);
		System.out.println("Files contained in " + TFSFile + " = " + fileCount[0]);	
		myClient.closeFile(openFile);	
	}

	public void countFilesContained(OpenTFSFile TFSFile, int offset, int size, int[] fileCount){
		byte[] bytesRead = myClient.readFile(TFSFile, offset, size);
		if(bytesRead == null){
			return;
		}
		int newOffset = ByteBuffer.wrap(bytesRead).getInt();
		fileCount[0]++;
		countFilesContained(TFSFile, newOffset + offset + size, size, fileCount);
	}


	public void setupHashCommands(Hashtable<String,Command> commandHash)
	{
		commandHash.put("cd", Command.CD);
		commandHash.put("cp", Command.CP);
		commandHash.put("exit", Command.EXIT);
		commandHash.put("help", Command.HELP);
		commandHash.put("killcs", Command.KILLCS);
		commandHash.put("ls", Command.LS);
		commandHash.put("lscs", Command.LSCS);
		commandHash.put("mkdir", Command.MKDIR);
		commandHash.put("mkfile", Command.MKFILE);
		commandHash.put("pwd", Command.PWD);
		commandHash.put("rm", Command.RM);
		commandHash.put("rmdir", Command.RMDIR);
		commandHash.put("unit1", Command.UNIT1);
		commandHash.put("unit2", Command.UNIT2);
		commandHash.put("unit3", Command.UNIT3);
		commandHash.put("unit4", Command.UNIT4);
		commandHash.put("unit5", Command.UNIT5);
		commandHash.put("unit6", Command.UNIT6);
		commandHash.put("unit7",  Command.UNIT7);
	}

	public void printHelp()
	{
		System.out.println("Supported Commands: ");
		System.out.println("cd - changes directory to the specified path (or relative to current directory if not specified)");
		System.out.println("cp - copies local file to filesystem (eg: C:\\test.txt \\usr\\newTest.txt");
		System.out.println("exit - exits the program");
		System.out.println("ls - list contents of current directory");
		System.out.println("mkdir - creates a directory at the specified path (or the current directory if not specified)");
		System.out.println("mkfile - creates a file at the specified path (or the current directory if not specified)");
		System.out.println("pwd - prints the current working directory");
	}
}	


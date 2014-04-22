package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

public class Driver {
	
	public enum Command {
		CD, CP, EXIT, HELP, LS, MKDIR, MKFILE, PWD, RM, RMDIR, UNKNOWN, UNIT1, UNIT2, UNIT3, UNIT4, UNIT5, UNIT6, UNIT7
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
		case MKDIR:
			if(args.length < 2)
			{
				System.out.println("Invalid input to mkdir command (must specify directory path or name)");
			}
			else if(!myClient.createDirectory(args[1]))
			{
				System.out.println("Unable to create directory " + args[1]);
			}
			break;
		case MKFILE:
			if(args.length < 2)
			{
				System.out.println("Invalid input to mkfile command (must specify file path or name)");
			}
			else
			{
				String ret = myClient.createFile(args[1],1);
				if(ret.contains("success"))
				{
					break;
				}
				else
				{
					System.out.println(ret);
				}
			}
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
			if(args.length < 2)
			{
				System.out.println("Invalid input to unit1 command (must specify number of directories to create)");
			}
			else 
			{
				unit1(Integer.parseInt(args[1]), 1, "");
			}
			break;
		case UNIT2:
			if(args.length < 2)
			{
				System.out.println("Invalid input to unit1 command (must specify number of directories to create)");
			}
			else 
			{
				unit2(args[2], Integer.parseInt(args[1]));
			}
			break;
		case UNIT3:
			if(args.length < 2){
				System.out.println("Invalid input to unit1 command (must specify number of directories to create)");
			}
				unit3(args[1]);
			break;
		case UNIT4:
			break;
		case UNIT5:
			break;
		case UNIT6:
			break;
		case UNIT7:
			break;
			
		default:
	
			break;
		}
		
		return currentCommand;
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
	
	public void unit1(int maxDepth, int currDir, String myDir) {
		if (currDir > maxDepth) {
			return;
		}

		myDir = myDir + "\\" +  currDir;		
		myClient.createDirectory(myDir);

		unit1(maxDepth, currDir * 2, myDir);
		unit1(maxDepth, (currDir * 2) + 1, myDir);
	}
	
	public void unit2(String rootDir, int numFiles){
		for (int i = 1; i <= numFiles; i++) {
			String fileName = "";
			fileName = "\\File" + i + ".txt";
			myClient.createFile(rootDir + fileName, 1);
		}
		//TODO: Add getSubDirectories call to master
		/*if(myClient.getSubdirectories(rootDir) == null){
			System.err.println("Directories did not exist.  Files may not have been created.");
			return;
		}
		for (String subDir : myClient.getSubdirectories(rootDir)) {
				unit2(numFiles,subDir);
		}*/
	}
	
	public void unit3(String directoryName){
		myClient.removeDirectory(directoryName);
	}

	public void setupHashCommands(Hashtable<String,Command> commandHash)
	{
		commandHash.put("cd", Command.CD);
		commandHash.put("cp", Command.CP);
		commandHash.put("exit", Command.EXIT);
		commandHash.put("help", Command.HELP);
		commandHash.put("ls", Command.LS);
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


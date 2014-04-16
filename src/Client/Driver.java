package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class Driver {
	
	public enum Command {CD,EXIT,HELP,LS,MKDIR,MKFILE,PWD,UNKNOWN};

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
		case EXIT:
			break;
		case HELP:
			printHelp();
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
			else if(!myClient.createFile(args[1]))
			{
				System.out.println("Unable to create file " + args[1]);
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
		case UNKNOWN:
			break;
		default:
			break;
		}
		
		return currentCommand;
	}

	public void setupHashCommands(Hashtable<String,Command> commandHash)
	{
		commandHash.put("cd", Command.CD);
		commandHash.put("exit", Command.EXIT);
		commandHash.put("help", Command.HELP);
		commandHash.put("ls", Command.LS);
		commandHash.put("mkdir", Command.MKDIR);
		commandHash.put("mkfile", Command.MKFILE);
		commandHash.put("pwd", Command.PWD);
	}

	public void printHelp()
	{
		System.out.println("Supported Commands: ");
		System.out.println("cd - changes directory to the specified path (or relative to current directory if not specified)");
		System.out.println("exit - exits the program");
		System.out.println("ls - list contents of current directory");
		System.out.println("mkdir - creates a directory at the specified path (or the current directory if not specified)");
		System.out.println("pwd - prints the current working directory");
	}
}

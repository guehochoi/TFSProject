package Master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import Master.FileSystem.TFSDirectory;
import Master.FileSystem.TFSFile;

public class Master {

	int masterPort = 666;
	FileSystem fs = new FileSystem();

	public static void main(String[] args) {
		Master masterServer = new Master();
		boolean done = false;
		ServerSocket server = null;

		try {
			server = new ServerSocket(masterServer.masterPort);
		} catch (IOException e1) {
			System.out.println("Unable to connect to port " + masterServer.masterPort + ". Aborting...");
			System.exit(0);
		}
		
		while(!done)
		{
			try {
				Socket clientSocket = server.accept(); //Accepts a connection, other connections are put on queue!
			    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			    String inputLine = "", command = "";

			    while ((inputLine = in.readLine()) != null && !inputLine.equals("END_OF_TRANSMISSION")) {
			    	command = command.concat(inputLine);
			    }
			    
			    System.out.println("Received command: " + command);
			    String output[] = masterServer.processRequest(command);
			    
			    for(String s : output)
			    {
			    	out.println(s);
			    }
			    
			    out.close();
			} catch (IOException e) {
				System.out.println("Error processing a network request. Skipping request.");
			}
		}
	}
	
	public String[] processRequest(String command)
	{
		int spaceIndex = command.indexOf(' ');

		if(spaceIndex == -1)
		{
			spaceIndex = command.length();
		}
				
		String firstArg = command.substring(0,spaceIndex);
				
		switch(firstArg)
		{
		case "directoryExists":
			return checkDirectoryExists(command, spaceIndex);
		case "ls":
			return lsDirectory(command, spaceIndex);
		case "createDirectory" :
			return createDirectory(command, spaceIndex);
		case "createFile" :
			return createFile(command, spaceIndex);
		case "removeFile" :
			return removeFile(command, spaceIndex);
		case "removeDirectory" :
			return removeDirectory(command, spaceIndex);
		}

		return null;
	}
	
	private String[] checkDirectoryExists(String command, int spaceIndex)
	{
		String[] ret = new String[1];
		if(fs.directoryHash.containsKey(command.substring(spaceIndex+1,command.length())))
		{
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "false";
			return ret;
		}
	}
	
	private String[] lsDirectory(String command, int spaceIndex)
	{
		if(fs.directoryHash.containsKey(command.substring(spaceIndex+1,command.length())))
		{
			TFSDirectory dir = fs.directoryHash.get(command.substring(spaceIndex+1,command.length()));
			
			String[] ret = new String[dir.files.size() + dir.subdirectories.size()];
			int count = 0;
			
			for(String subDir : dir.subdirectories)
			{
				ret[count] = subDir.substring(subDir.lastIndexOf('\\') + 1,subDir.length()) + "\\";
				count++;
			}

			for(TFSFile f : dir.files)
			{
				ret[count] = f.fileName.substring(f.fileName.lastIndexOf('\\')+1,f.fileName.length());
				count++;
			}
			
			return ret;
		}
		
		String ret[] = new String[1];
		return ret;
	}
	
	private String[] createDirectory(String command, int spaceIndex)
	{
		String[] ret = new String[1];

		if(fs.createDirectory(command.substring(spaceIndex+1,command.length())))
		{
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "false";
			return ret;
		}
	}

	private String[] createFile(String command, int spaceIndex)
	{
		String[] ret = new String[1];

		if(fs.createFile(command.substring(spaceIndex+1,command.length())))
		{
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "false";
			return ret;
		}
	}

	private String[] removeFile(String command, int spaceIndex)
	{
		String[] ret = new String[1];

		if(fs.deleteFile(command.substring(spaceIndex+1,command.length())))
		{
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "false";
			return ret;
		}
	}

	private String[] removeDirectory(String command, int spaceIndex)
	{
		String[] ret = new String[1];

		if(fs.deleteDirectory(command.substring(spaceIndex+1,command.length())))
		{
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "false";
			return ret;
		}
	}
}

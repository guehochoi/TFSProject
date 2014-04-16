package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	private String masterIpAddress = "localhost";
	private int    masterPort = 666;
	private String currentDir = "\\";
	Socket masterConnection;

	public Client()
	{
		
	}
	
	public String printWorkingDirectory()
	{
		return currentDir;
	}
	
	public String[] lsDirectory()
	{
		return sendQuery("ls " + currentDir);
	}
	
	public boolean changeDirectory(String newDirectory)
	{
		if(newDirectory.equals("..") && !currentDir.equals("\\"))
		{
			int lastDirIndex = currentDir.lastIndexOf("\\");

			if(lastDirIndex == 0)
			{
				currentDir = "\\";
				return true;
			}
			
			newDirectory = currentDir.substring(0,currentDir.lastIndexOf("\\"));
		}
		else if(newDirectory.equals("..") && currentDir.equals("\\"))
		{
			return true;
		}
		
		String queryCommand = "directoryExists ";

		if(newDirectory.charAt(0) == '\\')
		{
			queryCommand = queryCommand.concat(newDirectory);
		}
		else
		{
			if(currentDir.charAt(currentDir.length()-1) == '\\')
			{
				queryCommand = queryCommand.concat(currentDir + newDirectory);
			}
			else
			{
				queryCommand = queryCommand.concat(currentDir + "\\" + newDirectory);
			}
		}
		
		String result[] = sendQuery(queryCommand);

		if(result[0].contains("true"))
		{
			currentDir = currentDir + newDirectory;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean createDirectory(String directoryPath)
	{
		String queryCommand = "createDirectory ";

		if(directoryPath.charAt(0) == '\\')
		{
			queryCommand = queryCommand.concat(queryCommand + directoryPath);
		}
		else
		{
			if(currentDir.equals("\\"))
			{
				queryCommand = queryCommand.concat(currentDir + directoryPath);
			}
			else
			{
				queryCommand = queryCommand.concat(currentDir + "\\" + directoryPath);
			}

		}
		
		String[] result = sendQuery(queryCommand);

		if(result[0].contains("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean createFile(String directoryPath)
	{
		String queryCommand = "createFile ";

		if(directoryPath.charAt(0) == '\\')
		{
			queryCommand = queryCommand.concat(queryCommand + directoryPath);
		}
		else
		{
			if(currentDir.equals("\\"))
			{
				queryCommand = queryCommand.concat(currentDir + directoryPath);
			}
			else
			{
				queryCommand = queryCommand.concat(currentDir + "\\" + directoryPath);
			}

		}
		
		String[] result = sendQuery(queryCommand);

		if(result[0].contains("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private String[] sendQuery(String query)
	{
		if(masterConnection == null || masterConnection.isClosed())
		{
			beginMasterSession();
		}
		
	    try {
			PrintWriter out = new PrintWriter(masterConnection.getOutputStream(), true);
			
			out.println(query);
			out.println("END_OF_TRANSMISSION");
			out.flush();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(masterConnection.getInputStream()));
			String fromServer, ret = "";

            while ((fromServer = in.readLine()) != null) 
            {
            	ret = ret.concat(fromServer + "\n");
            }
            
            out.close();
            in.close();
            return ret.split("\n");
		} catch (IOException e) {
			System.err.println("Error reading/writing to master connection: " + e.getMessage());
		}

		return null;
	}
	
	public void setMasterDetails(String ipAddress, int port)
	{
		masterIpAddress = ipAddress;
		masterPort = port;
	}
	
	public void beginMasterSession()
	{
		try {
			masterConnection = new Socket(masterIpAddress,masterPort);
		} catch (IOException e) {
			System.err.println("Unable to connect to master server on " + masterIpAddress + ":" + masterPort);
			System.err.println("Check your connection to the server and try again");
			System.exit(0);
		}
	}
}
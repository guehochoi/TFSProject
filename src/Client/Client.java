package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Client {
	private String masterIpAddress = "localhost";
	private int    masterPort = 666;
	private String chunkServerIpAddress = "localhost";
	private int    chunkServerPort = 667;

	private String currentDir = "\\";
	Socket masterConnection;
	Socket chunkServerConnection;

	public Client()
	{
		
	}

	private String formatRemotePath(String remotePath)
	{
		if(remotePath.charAt(0) == '\\')
		{
			return remotePath;
		}
		else
		{
			if(currentDir.charAt(currentDir.length()-1) == '\\')
			{
				return currentDir + remotePath;
			}
			else
			{
				return currentDir + "\\" + remotePath;
			}
		}
	}

	public String printWorkingDirectory()
	{
		return currentDir;
	}
	
	public String[] lsDirectory()
	{
		return sendMasterQuery("ls " + currentDir);
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
		
		String queryCommand = "directoryExists " + formatRemotePath(newDirectory);
		String result[] = sendMasterQuery(queryCommand);

		if(result[0].contains("true"))
		{
			currentDir = formatRemotePath(newDirectory);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean createDirectory(String directoryPath)
	{
		String queryCommand = "createDirectory " + formatRemotePath(directoryPath);
		String[] result = sendMasterQuery(queryCommand);

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
		String queryCommand = "createFile " + formatRemotePath(directoryPath);
		String[] result = sendMasterQuery(queryCommand);

		if(result[0].contains("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean removeFile(String filepath)
	{
		String queryCommand = "removeFile " + formatRemotePath(filepath);
		String[] result = sendMasterQuery(queryCommand);

		if(result[0].contains("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean removeDirectory(String directoryPath)
	{
		String queryCommand = "removeDirectory " + formatRemotePath(directoryPath);
		String[] result = sendMasterQuery(queryCommand);

		if(result[0].contains("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean writeFile(String remotePath, byte[] data)
	{
		String command = "writeFile " + formatRemotePath(remotePath);
		ByteBuffer bb = ByteBuffer.allocate(4 + command.length() + data.length);
		bb.putInt(command.length());
		bb.put(command.getBytes());
		bb.put(data);
		byte[] result = sendChunkServerQuery(bb.array());
		
		if(result != null && result.toString().contains("success"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public byte[] sendChunkServerQuery(byte[] data)
	{
		if(chunkServerConnection == null || chunkServerConnection.isClosed())
		{
			beginChunkServerSession();
		}
		
		try {
			DataOutputStream out = new DataOutputStream(chunkServerConnection.getOutputStream());
			
			out.write(data);
			out.flush();
			
			DataInputStream in = new DataInputStream(chunkServerConnection.getInputStream());

			byte[] ret = new byte[in.readInt()];
			in.readFully(ret);

			out.close();
			in.close();
			return ret;
		} catch (IOException e) {
			System.err.println("Error reading/writing to chunk server connection: " + e.getMessage());
		}

		byte[] ret = new byte[1];
		return ret;
	}
	
	private String[] sendMasterQuery(String query)
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
	
	
	public void beginChunkServerSession()
	{
		try {
			chunkServerConnection = new Socket(chunkServerIpAddress,chunkServerPort);
		} catch (IOException e) {
			System.err.println("Unable to connect to master server on " + masterIpAddress + ":" + masterPort);
			System.err.println("Check your connection to the server and try again");
			System.exit(0);
		}
	}
}
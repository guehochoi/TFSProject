package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Client {
	private String masterIpAddress = "localhost";
	private int    masterPort = 666;
	private String chunkServerIpAddress = "localhost";
	private int    chunkServerPort = 667;

	private String currentDir = "\\";
	Socket masterConnection;
	Socket chunkServerConnection;
	
	public class OpenTFSFile
	{
		String[] openResult;
		String filepath;
		
		public OpenTFSFile(String[] openResult, String filepath)
		{
			this.openResult = openResult;
			this.filepath = filepath;
		}
		
		public String getfileName()
		{
			if(openResult != null && openResult.length >= 2)
			{
				return openResult[1];
			}
			
			return null;
		}
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

	public String createFile(String directoryPath, int numToCreate)
	{
		String queryCommand = "createFile " + formatRemotePath(directoryPath) + " " + numToCreate;
		String[] result = sendMasterQuery(queryCommand);
		return result[0];
	}
	
	public OpenTFSFile openFile(String filename)
	{
		String queryCommand = "openFile " + formatRemotePath(filename);
		String[] result = sendMasterQuery(queryCommand);
		return new OpenTFSFile(result, filename);
	}
	
	public void closeFile(OpenTFSFile file)
	{
		String queryCommand = "closeFile " + formatRemotePath(file.filepath);
		sendMasterQuery(queryCommand);
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
	
	public boolean writeFile(OpenTFSFile openFile, byte[] data)
	{
		String command = "writeFile " + openFile.getfileName() + " ";
		ByteBuffer bb = ByteBuffer.allocate(4 + command.length() + data.length);
		bb.putInt(command.length() + data.length);
		bb.put(command.getBytes());
		bb.put(data);
		
		boolean ret = false;
		
		for(int i = 2; i < openFile.openResult.length; i++)
		{
			String[] args = openFile.openResult[i].split(":");
			String ipAddress = args[0];
			int port = Integer.parseInt(args[1]);
			byte[] result = sendChunkServerQuery(ipAddress,port,bb.duplicate().array());
			
			if(ret == false && result.toString().contains("success"))
			{
				ret = true;
			}
		}
		
		return ret;
	}
	
	public boolean appendFile(OpenTFSFile openFile, byte[] data)
	{
		String command = "appendFile " + openFile.getfileName() + " ";
		ByteBuffer bb = ByteBuffer.allocate(4 + command.length() + data.length);
		bb.putInt(command.length() + data.length);
		bb.put(command.getBytes());
		bb.put(data);

		boolean ret = false;
		
		for(int i = 2; i < openFile.openResult.length; i++)
		{
			String[] args = openFile.openResult[i].split(":");
			String ipAddress = args[0];
			int port = Integer.parseInt(args[1]);
			byte[] result = sendChunkServerQuery(ipAddress,port,bb.array());
			
			if(ret == false && result.toString().contains("success"))
			{
				ret = true;
			}
		}
		
		return ret;
	}

	public byte[] readFile(OpenTFSFile file)
	{
		if(file.openResult.length < 2)
		{
			return null;
		}

		String command = "readFile " + file.getfileName() + " 0 " + "0"; //the zero zero specifies at offset 0 and length of the whole file
		ByteBuffer bb = ByteBuffer.allocate(4 + command.length());
		bb.putInt(command.length());
		bb.put(command.getBytes());

		for(int i = 2; i < file.openResult.length; i++)
		{
			String[] args = file.openResult[i].split(":");
			String ipAddress = args[0];
			int port = Integer.parseInt(args[1]);
			byte[] result = sendChunkServerQuery(ipAddress,port,bb.array());
			
			if(result[0] == 0 && result.length == 1)
			{
				continue;
			}
			
			return result;
		}
		
		return null;
	}

	public byte[] readFile(OpenTFSFile file, int offset, int size)
	{
		if(file.openResult.length < 2)
		{
			return null;
		}

		String command = "readFile " + file.getfileName() + " " + offset + " " + size;
		ByteBuffer bb = ByteBuffer.allocate(4 + command.length());
		bb.putInt(command.length());
		bb.put(command.getBytes());

		for(int i = 2; i < file.openResult.length; i++)
		{
			String[] args = file.openResult[i].split(":");
			String ipAddress = args[0];
			int port = Integer.parseInt(args[1]);
			byte[] result = sendChunkServerQuery(ipAddress,port,bb.array());
			
			if(result[0] == 0 && result.length == 1)
			{
				continue;
			}
			
			return result;
		}
		
		return null;
	}

	public byte[] sendChunkServerQuery(String ipAddress, int port, byte[] data)
	{
		Socket chunkServerConnection = null;
		try {
				chunkServerConnection = new Socket(ipAddress, port);
		} catch (IOException e1) {
			System.out.println("Error making connection to chunkserver " + ipAddress + " on port " + port);
			byte[] ret = new byte[1];
			return ret;
		}
		
		try {
			DataOutputStream out = new DataOutputStream(chunkServerConnection.getOutputStream());
			
			if(data.length < 2)
			{
				System.out.println("EL OH EL DATA");
			}
			
			out.write(data);
			//out.close();
			
			DataInputStream in = new DataInputStream(chunkServerConnection.getInputStream());

			byte[] ret = new byte[in.readInt()];
			in.readFully(ret);

			out.close();
			in.close();
			chunkServerConnection.close();
			return ret;
		} catch (IOException e) {
			System.out.println("Error making connection to chunkserver " + ipAddress + " on port " + port);
			System.out.println(e.getMessage());
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
	
	public void setChunkServerDetails(String ipAddress, int port)
	{
		String myIp;
		try {
			myIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			myIp = "1";
		}

		if(myIp.equals(ipAddress))
		{
			chunkServerIpAddress = "localhost";
		}
		else
		{
			chunkServerIpAddress = ipAddress;
		}

		chunkServerPort = port;
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
			System.err.println("Unable to connect to chunk server on " + chunkServerIpAddress + ":" + chunkServerPort);
			System.err.println("Check your connection to the server and try again");
			System.exit(0);
		}
	}
}
package ChunkServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ChunkServer {
	
	public String currentDir = System.getProperty("user.dir");
	private int chunkServerPort = 667;
	String myIp = null;
	Socket masterConnection = null;
	String masterIpAddress = null;
	int masterPort = 666;
	
	public ChunkServer()
	{
		try {
			myIp = InetAddress.getLocalHost().getHostAddress();
			masterIpAddress = myIp;
			String[] result = sendMasterQuery("registerChunkServer " +  myIp + ":" +  Integer.toString(chunkServerPort));
			currentDir = currentDir.concat("\\" + "cs" + result[0] + "\\");
			
			File myFile = new File(currentDir);
			
			if(!myFile.exists())
			{
				myFile.mkdir();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ChunkServer cs = new ChunkServer();
		boolean done = false;
		ServerSocket server = null;

		try {
			server = new ServerSocket(cs.chunkServerPort);
		} catch (IOException e1) {
			System.out.println("Unable to connect to port " + cs.chunkServerPort + ". Aborting...");
			System.exit(0);
		}
		
		while(!done)
		{
			try {
				Socket clientSocket = server.accept(); //Accepts a connection, other connections are put on queue!
				DataInputStream in = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			    
			    int length = in.available();
			    byte[] data = new byte[length];
			    in.readFully(data);
			    
			    byte[] sendData = cs.processData(data);

			    if(sendData == null)
			    {
			    	out.close();
			    	continue;
			    }

			    ByteBuffer bb = ByteBuffer.allocate(4 + sendData.length);
			    bb.putInt(sendData.length);
			    bb.put(sendData);
			    out.write(bb.array());
			    out.close();
			} catch (IOException e) {
				System.out.println("Error processing a network request. Skipping request.");
			}
		}
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
	
	public byte[] processData(byte[] data)
	{
		ByteBuffer bb = ByteBuffer.wrap(data);
		int commandLength = bb.getInt();
		String command = new String(data,4,commandLength);
		String[] args = command.split(" ");
		
		ByteBuffer trimmedBuffer = ByteBuffer.allocate(data.length - 4 - commandLength);
		trimmedBuffer.put(data,4 + commandLength,data.length - 4 - commandLength);
		
		System.out.println("Received commmand: " + command);

		switch(args[0])
		{
		case "createFile":
			return createFile(command);
		case "deleteFile":
			return deleteFile(command);
		case "writeFile":
			return writeFile(command,trimmedBuffer);
		case "readFile":
			return readFile(command);
		}

		return "Command not found".getBytes();
	}
	
	public byte[] createFile(String command)
	{
		String[] args = command.split(" ");
		
		if(args.length < 2)
		{
			return "Invalid Arguments".getBytes();
		}
		
		String fileName = args[1];
		File myFile = new File(currentDir + fileName);

		if(myFile.exists())
		{
			return "File Already Created".getBytes();
		}
		else
		{
			try {
				if(myFile.createNewFile())
				{
					return "success".getBytes();
				}
				else
				{
					return "Error creating file".getBytes();
				}
			} catch (IOException e) {
				return "Error creating file".getBytes();
			}
		}
	}

	public byte[] deleteFile(String command)
	{
		String[] args = command.split(" ");
		
		if(args.length < 2)
		{
			return "Invalid Arguments".getBytes();
		}
		
		String fileName = args[1];
		File myFile = new File(currentDir + fileName);

		if(!myFile.exists())
		{
			return "File does not exist".getBytes();
		}
		else
		{
			if(myFile.delete())
			{
				return "success".getBytes();
			}
			else
			{
				return "Error deleting file".getBytes();
			}
		}
	}

	public byte[] writeFile(String command, ByteBuffer data)
	{
		String[] args = command.split(" ");
		
		if(args.length < 2)
		{
			return "Invalid Arguments".getBytes();
		}
		
		String fileName = args[1];

		File myFile = new File(currentDir + fileName);

		if(!myFile.exists())
		{
			return "File Does Not Exist".getBytes();
		}

		try {
			FileOutputStream fs = new FileOutputStream(myFile);
			fs.write(data.array());
			fs.close();
			return "success".getBytes();
		} catch (IOException e) {
			return "Error writing to file".getBytes();
		}
	}
	
	public byte[] readFile(String command)
	{
		String[] args = command.split(" ");

		if(args.length != 4)
		{
			return null;
		}
		
		int offset;
		long size;

		try
		{
			offset = Integer.parseInt(args[2]);
			size = Long.parseLong(args[3]);
		}
		catch(NumberFormatException e)
		{
			return null;
		}

		RandomAccessFile file = null;

		try {
			file = new RandomAccessFile(currentDir + args[1], "r");
		} catch (FileNotFoundException e) {
			return null;
		}

		try {
			if (file.length() > offset) {
				if(size == 0)
				{
					size = file.length();
				}

				byte[] bytesRead = new byte[(int) size];
				file.seek(offset);
				file.read(bytesRead);
				file.close();
				return bytesRead;
			} else {
				file.close();
				return null;
			}
		} catch (IOException e) {
			return null;
		}
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
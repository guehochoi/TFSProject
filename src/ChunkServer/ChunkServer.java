package ChunkServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ChunkServer {
	
	public final String currentDir = System.getProperty("user.dir");
	private int chunkServerPort = 667;

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
		case "writeFile":
			return writeFile(command,trimmedBuffer);
		case "readFile":
			return readFile(command);
		}

		return "Command not found".getBytes();
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
			return "Invalid Arguments".getBytes();
		}

		int offset = Integer.parseInt(args[2]), size = Integer.parseInt(args[3]);

		RandomAccessFile file = null;

		try {
			file = new RandomAccessFile(currentDir + args[1], "r");
		} catch (FileNotFoundException e) {
			return "File Does Not Exist".getBytes();
		}

		try {
			if (file.length() > offset) {
				byte[] bytesRead = new byte[size];
				file.seek(offset);
				file.read(bytesRead);
				file.close();
				return bytesRead;
			} else {
				file.close();
				return "Length of file exceeds offset".getBytes();
			}
		} catch (IOException e) {
			return "Unable to read file".getBytes();
		}
	}
}

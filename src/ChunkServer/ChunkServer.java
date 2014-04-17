package ChunkServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ChunkServer {
	
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
			    out.write(sendData);
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
		String command = new String(data,4,commandLength+1);
		String[] args = command.split(" ");

		switch(args[0])
		{
		case "writeFile":
			return writeFile(command,bb);
		case "readFile":
			break;
		}

		return null;
	}
	
	public byte[] writeFile(String command, ByteBuffer data)
	{
		String[] args = command.split(" ");
		
		if(args.length < 2)
		{
			return null;
		}
		
		String fileName = args[1];

		File myFile = new File(fileName);

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
}

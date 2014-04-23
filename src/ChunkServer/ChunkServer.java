package ChunkServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkServer {

	public String currentDir = System.getProperty("user.dir");
	private int chunkServerPort = 667;
	String myIp = null;
	Socket masterConnection = null;
	String masterIpAddress = null;
	int masterPort = 666;

	public ChunkServer(int port)
	{
		try {
			chunkServerPort = port;
			myIp = InetAddress.getLocalHost().getHostAddress();
			masterIpAddress = myIp;
			String[] result = sendMasterQuery("registerChunkServer " +  myIp + ":" +  Integer.toString(chunkServerPort));
			currentDir = currentDir.concat("\\" + "cs" + result[0] + "\\");

			File myFile = new File(currentDir);

			if(!myFile.exists())
			{
				myFile.mkdir();
			}

			synchronize();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int port = 667;

		if(args.length >= 1)
		{
			try
			{
				port = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException e)
			{
				System.out.println("Fatal error - invalid port supplied to chunkserver. Returing from thread.");
				return;
			}
		}

		ChunkServer cs = new ChunkServer(port);
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
				System.out.println(cs.chunkServerPort + ": Begin accepting connections");
				Socket clientSocket = server.accept(); //Accepts a connection, other connections are put on queue!
				DataInputStream in = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

				int length = in.readInt();

				byte[] data = new byte[length];
				in.readFully(data);

				byte[] sendData = cs.processData(data);

				if(sendData == null)
				{
					byte[] ret = new byte[1];
					ret[0] = 0;
					out.write(ret);
					out.close();
					continue;
				}

				ByteBuffer bb = ByteBuffer.allocate(4 + sendData.length);
				bb.putInt(sendData.length);
				bb.put(sendData);
				out.write(bb.array());
				out.flush();
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
		if(data == null)
		{
			return null;
		}

		String command = new String(data);
		int spaceIndex = command.indexOf(" ");
		int lastSpace = command.lastIndexOf(" ");

		if(spaceIndex < 0)
		{
			spaceIndex = command.length() - 1;
		}
		else if(lastSpace < 0)
		{
			lastSpace = command.length() - 1;
		}

		String function = command.substring(0,spaceIndex);

		if(function.equals("writeFile") || function.equals("append"))
		{
			System.out.println(this.chunkServerPort + ": Received commmand: " + command.substring(0,lastSpace));
		}
		else
		{
			System.out.println(this.chunkServerPort + ": Received commmand: " + command);
		}

		switch(function)
		{
		case "createFile":
			return createFile(command);
		case "deleteFile":
			return deleteFile(command);
		case "writeFile":
			return writeFile(command,data);
		case "readFile":
			return readFile(command);
		case "appendFile":
			return appendFile(command,data);
		case "updateFile":
			return updateFile(command);
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
			if(args.length > 2) //This is a replicated file... grab the other chunkservers!
			{
				try {
					File metaFile = new File(currentDir + fileName + ".meta");
					metaFile.createNewFile();
					FileWriter fw = new FileWriter(metaFile);
					fw.write("v0" + "\n");

					for(int i = 2; i < args.length; i++)
					{
						fw.write(args[i] + "\n");
					}

					fw.close();
				} catch (IOException e) {
					System.out.println("Unable to create/write to the meta file - file may not be synchronized with replicas");
				}
			}

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
				File metaFile = new File(currentDir + fileName + ".meta");

				if(metaFile.exists())
				{
					updateMetaFile(metaFile,-1);
				}

				return "success".getBytes();
			}
			else
			{
				return "Error deleting file".getBytes();
			}
		}
	}

	public byte[] writeFile(String command, byte[] data)
	{
		String[] args = command.split(" ");

		if(args.length < 3)
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
			myFile.delete();
			FileOutputStream fs = new FileOutputStream(myFile);
			fs.write(args[2].getBytes());
			fs.close();

			File metaFile = new File(currentDir + fileName + ".meta");

			if(metaFile.exists())
			{
				updateMetaFile(metaFile,0);
			}

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

	public byte[] appendFile(String command, byte[] data)
	{
		String[] args = command.split(" ");

		if(args.length < 3)
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
			FileOutputStream fs = new FileOutputStream(myFile,true);
			fs.write(args[2].getBytes());
			fs.close();

			File metaFile = new File(currentDir + fileName + ".meta");

			if(metaFile.exists())
			{
				updateMetaFile(metaFile,0);
			}

			return "success".getBytes();
		} catch (IOException e) {
			return "Error appending to file".getBytes();
		}
	}

	private byte[] updateFile(String command)
	{
		String[] args = command.split(" ");

		if(args.length != 3)
		{
			return null;
		}

		String fileName = args[1];
		String versionNumber = args[2];

		try {
			File metaFile = new File(currentDir + fileName + ".meta");

			if(metaFile.exists())
			{

				BufferedReader reader = new BufferedReader(new FileReader(metaFile));
				int currentVersion = versionNumToInt(reader.readLine());
				reader.close();

				if(currentVersion < 0)
				{
					String query = "deleteFile " + fileName;
					return query.getBytes();
				}
				else if(currentVersion <= versionNumToInt(versionNumber))
				{
					return "no update".getBytes();
				}
				else
				{
					Path path = Paths.get(currentDir + fileName);
					byte[] data = Files.readAllBytes(path);
					String query = "writeFile " + fileName + " ";
					ByteBuffer bb = ByteBuffer.allocate(4 + query.length() + data.length);
					bb.putInt(currentVersion);
					bb.put(query.getBytes());
					bb.put(data);
					return bb.array();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] ret = new byte[1];
		ret[0] = 0;
		return ret;
	}

	private void updateMetaFile(File metaFile, int newVersion)
	{
		try {
			File tempFile = new File(currentDir + "temp.txt");
			BufferedReader reader = new BufferedReader(new FileReader(metaFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String currentLine;

			while((currentLine = reader.readLine()) != null)
			{
				if(!currentLine.contains("v"))
				{
					writer.write(currentLine + "\n");
				}
				else
				{
					if(newVersion == 0)
					{
						int version = versionNumToInt(currentLine);
						version++;
						writer.write("v" + version + "\n");
					}
					else
					{
						writer.write("v" + newVersion + "\n");
					}
				}
			}

			reader.close();
			writer.close();
			metaFile.delete();
			tempFile.renameTo(metaFile);
		} catch (IOException e) {
			System.out.println("Error updating meta file version, syncronization for file may no longer work");
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

	private int versionNumToInt(String versionNum)
	{
		String versionString = versionNum.substring(1,versionNum.length());
		int version = Integer.parseInt(versionString);
		return version;
	}

	public void synchronize()
	{
		Path dir = Paths.get(currentDir);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{meta}")) 
		{
			for (Path entry: stream) 
			{
				File metaFile = new File(entry.toUri());
				String fileName = metaFile.getName();
				fileName = fileName.substring(0,fileName.indexOf('.'));
				System.out.println("Checking for any file updates: " + fileName);

				BufferedReader reader = new BufferedReader(new FileReader(metaFile));

				String versionLine = reader.readLine();
				String currentLine, testHost = this.myIp+ ":" + this.chunkServerPort;

				while((currentLine = reader.readLine()) != null)
				{
					if(currentLine.equals(testHost))
					{
						continue;
					}
					else
					{
						String sendQuery = "updateFile " + fileName + " " + versionLine;
						ByteBuffer bb = ByteBuffer.allocate(4 + sendQuery.length());
						bb.putInt(sendQuery.length());
						bb.put(sendQuery.getBytes());
						String ipAddress = currentLine.substring(0,currentLine.indexOf(":"));
						int port = Integer.parseInt(currentLine.substring(currentLine.indexOf(":")+1,currentLine.length()));
						byte[] result = sendChunkServerQuery(ipAddress,port,bb.duplicate().array());

						if(result.length > 1)
						{
							reader.close();
							if(new String(result).equals("no update"))
							{
								break;
							}
							else if(result.length > 1 && result[0] == 'd') //if it's delete...
							{
								processData(result);
							}
							else //it's write... so parse out the new version number
							{
								ByteBuffer buf = ByteBuffer.wrap(result);
								updateMetaFile(metaFile,buf.getInt());
								byte[] data = new byte[result.length - 4];
								buf.get(data,0,result.length-4);
								processData(data);
							}

							break;
						}
					}
				}

				reader.close();
			}
		} catch (IOException e) {
			System.out.println("Unable to send synchronization requests to other fileservers, files may not be up to date.");
		}
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

			out.write(data);
			out.flush();

			DataInputStream in = new DataInputStream(chunkServerConnection.getInputStream());

			byte[] ret = new byte[in.readInt()];
			in.readFully(ret);

			out.close();
			in.close();
			chunkServerConnection.close();
			return ret;
		} catch (IOException e) {
			System.out.println("Error making connection to chunkserver " + ipAddress + " on port " + port);
		}

		byte[] ret = new byte[1];
		return ret;
	}
}
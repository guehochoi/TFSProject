package Master;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;

import Master.FileSystem.TFSDirectory;
import Master.FileSystem.TFSFile;

public class Master {

	int masterPort = 666;
	FileSystem fs = new FileSystem();
	ChunkTracker ct = new ChunkTracker();

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
				Socket clientSocket = server.accept();
				requestProcessor rp = new requestProcessor(clientSocket, masterServer);
				new Thread(rp).start();
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
		case "openFile":
			return openFile(command, spaceIndex);
		case "closeFile":
			return closeFile(command, spaceIndex);
		case "removeDirectory" :
			return removeDirectory(command, spaceIndex);
		case "removeFile":
			return removeFile(command, spaceIndex);
		case "registerChunkServer":
			return registerChunkServer(command, spaceIndex);
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
		String[] args = command.split(" ");
		String fileName = args[1];
		int numReplicas = Integer.parseInt(args[2]);
		List<ChunkTracker.ChunkServerInfo> servers = ct.getChunkServers(numReplicas);

		if(servers.isEmpty())
		{
			ret[0] = "Unable to find " + numReplicas + " chunkservers for file creation. Creation aborted";
			return ret;
		}
		
		ret[0] = fs.createFile(fileName, servers);
		TFSFile file = fs.getFile(fileName);
		
		if(file != null && ret[0].contains("success"))
		{
			String queryString = "createFile " + file.md5FileName;
			ByteBuffer bb = ByteBuffer.allocate(4 + queryString.length());
			bb.putInt(queryString.length());
			bb.put(queryString.getBytes());

			for(ChunkTracker.ChunkServerInfo info : file.chunkServers)
			{
				sendChunkServerQuery(info.ipAddress, info.port, bb.array());
			}
		}
		
		return ret;
	}

	private String[] removeFile(String command, int spaceIndex)
	{
		String[] ret = new String[1];
		String filename = command.substring(spaceIndex+1,command.length());
		TFSFile file = fs.getFile(filename);
		
		if(file == null)
		{
			ret[0] = "File not found";
			return ret;
		}
		else
		{
			deleteTFSFile(file,filename);
			ret[0] = "true";
			return ret;
		}
	}

	private String[] removeDirectory(String command, int spaceIndex)
	{
		String[] ret = new String[1];
		String dirName = command.substring(spaceIndex+1,command.length());
		if(fs.directoryHash.containsKey(dirName))
		{
			deleteTFSDirectory(dirName);
			ret[0] = "true";
			return ret;
		}
		else
		{
			ret[0] = "Directory not found";
			return ret;
		}
	}

	private String[] registerChunkServer(String command, int spaceIndex)
	{
		String[] ret = new String[1];
		String ipAddress = command.substring(spaceIndex+1,command.indexOf(':'));
		String port = command.substring(command.indexOf(':')+1,command.length());
		int csNum = ct.registerChunkServer(ipAddress, Integer.parseInt(port));
		ret[0] = Integer.toString(csNum);
		return ret;
	}

	private String[] openFile(String command, int spaceIndex)
	{
		String fileName = command.substring(spaceIndex+1,command.length());
		TFSFile file = fs.getFile(fileName);
		
		if(file != null)
		{
			try {
				file.sema.acquire();
				String[] ret = new String[file.chunkServers.size() + 2];
				ret[0] = "success";
				ret[1] = file.md5FileName;
				int count = 2;

				for(ChunkTracker.ChunkServerInfo info : file.chunkServers)
				{
					ret[count] = info.ipAddress + ":" +  info.port;
				}

				return ret;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		String[] ret = new String[1];
		ret[0] = "File not found";
		return ret;
	}

	private String[] closeFile(String command, int spaceIndex)
	{
		String fileName = command.substring(spaceIndex+1,command.length());
		TFSFile file = fs.getFile(fileName);
		file.sema.release();
		return new String[1];
	}
	
	private void deleteTFSFile(TFSFile file, String filename)
	{
		String queryString = "deleteFile " + file.md5FileName;
		ByteBuffer bb = ByteBuffer.allocate(4 + queryString.length());
		bb.putInt(queryString.length());
		bb.put(queryString.getBytes());
		
		try {
			file.sema.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for(ChunkTracker.ChunkServerInfo info : file.chunkServers)
		{
			sendChunkServerQuery(info.ipAddress, info.port, bb.array());
		}

		file.sema.release();
		fs.deleteFile(filename);
	}
	
	private void deleteTFSDirectory(String dir)
	{
		for(TFSFile file: fs.directoryHash.get(dir).files)
		{
			deleteTFSFile(file,dir + "\\" + file.fileName);
		}
		
		for(String subdir : fs.directoryHash.get(dir).subdirectories)
		{
			deleteTFSDirectory(subdir);
		}
		
		fs.deleteDirectory(dir);
	}

	public byte[] sendChunkServerQuery(String ipAddress, int port, byte[] data)
	{
		Socket chunkServerConnection = null;
		try {
				chunkServerConnection = new Socket(ipAddress, port);
		} catch (IOException e1) {
			System.out.println("Error making connection to chunkserver " + ipAddress + " on port " + port);
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


class requestProcessor implements Runnable
{
	Socket clientSocket = null;
	Master masterServer = null;

	public requestProcessor(Socket clientSocket, Master masterServer)
	{
		this.clientSocket = clientSocket;
		this.masterServer = masterServer;
	}

	public void run() {
		try {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String inputLine = "", command = "";

			while ((inputLine = in.readLine()) != null && !inputLine.equals("END_OF_TRANSMISSION")) {
				command = command.concat(inputLine);
			}

			System.out.println("Received command: " + command);
			String output[] = masterServer.processRequest(command);
			
			if(output == null)
			{
				out.close();
				in.close();
				return;
			}

			for(String s : output)
			{
				out.println(s);
			}

			out.close();
			in.close();
		} catch (IOException e) {
			System.out.println("Error processing a network request. Skipping request.");
		}
	}
}
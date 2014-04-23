package Master;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ChunkTracker {
	List<ChunkServerInfo> liveChunkServers = Collections.synchronizedList(new ArrayList<ChunkServerInfo>());
	List<ChunkServerInfo> deadChunkServers = Collections.synchronizedList(new ArrayList<ChunkServerInfo>());
	
	int numChunkServers = 0;
	File chunkServerFile = new File("ChunkServers.txt");

	static public class ChunkServerInfo
	{
		int port = 0;
		String ipAddress;
		int numRegistered = 0;
		
		public ChunkServerInfo(String myipAddress, int port, int numRegistered)
		{
			this.port = port;
			this.ipAddress = myipAddress;
			this.numRegistered = numRegistered;
		}
	}
	
	public ChunkTracker()
	{
		try {
			if(!chunkServerFile.exists())
			{
				chunkServerFile.createNewFile();
				System.out.println("No previous chunkservers, server will operate on new chunkservers");
			}
			else
			{
				Path filePath = Paths.get(chunkServerFile.getAbsolutePath());
				List<String> lines = Files.readAllLines(filePath,Charset.defaultCharset());

				for(String line : lines)
				{
					String[] args = line.split(":");
					int port = Integer.parseInt(args[1]);
					int numRegistered = Integer.parseInt(args[2]);
					numChunkServers = numRegistered > numChunkServers ? numRegistered : numChunkServers;
					ChunkServerInfo info = new ChunkServerInfo(args[0],port,numRegistered);
					liveChunkServers.add(info);
				}
			}
		} catch (IOException e) {
			System.err.println("Error opening ChunkServers.txt... server will operate newly registered chunkservers");
		}
	}

	public int registerChunkServer(String ipAddress, int port)
	{
		if(deadChunkServers != null)
		{
			synchronized(deadChunkServers)
			{
				for(ChunkServerInfo info : deadChunkServers)
				{
					if(info.ipAddress.equals(ipAddress) && info.port == port)
					{
						liveChunkServers.add(info);
						deadChunkServers.remove(info);
						return info.numRegistered;
					}
				}
			}
		}

		if(liveChunkServers != null)
		{
			synchronized(liveChunkServers)
			{
				for(ChunkServerInfo info : liveChunkServers)
				{
					if(info.ipAddress.equals(ipAddress) && info.port == port)
					{
						return info.numRegistered;
					}
				}
			}
		}

		ChunkServerInfo info = new ChunkServerInfo(ipAddress, port, ++numChunkServers);

		synchronized(liveChunkServers)
		{
			liveChunkServers.add(info);
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(chunkServerFile, true));
			bw.append(info.ipAddress + ":" + info.port + ":" + info.numRegistered + "\n");
			bw.close();
		} catch (IOException e) {
			System.err.println("Error saving newly registered chunkserver.");
		}
		
		return numChunkServers;
	}
	
	public void heartBeats()
	{
	}
	
	// returns amount random chunk servers from the active chunkserver list
	public List<ChunkServerInfo> getChunkServers(int amount)
	{
		if(liveChunkServers == null || amount > liveChunkServers.size())
		{
			return new LinkedList<ChunkServerInfo>();
		}

		Collections.shuffle(liveChunkServers);
		return liveChunkServers.subList(0, amount);
	}
}
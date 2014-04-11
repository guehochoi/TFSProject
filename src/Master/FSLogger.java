package Master;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class FSLogger implements Runnable {
	
	private FileSystem fs;
	private final Semaphore sema = new Semaphore(0);
	private boolean exitLogger = false;
	
	public final String persistentFileName = "TFSPersist.txt";
	
	private final File persistentFile = new File(persistentFileName);
	private final File uncommitLog = new File("uncommited.txt");
	private final File commitLog = new File("commited.txt");
	
	private Thread t;
	private Thread parentThread;
	
	private List<String[]> commitList = new LinkedList<String[]>();
	
	public FSLogger(FileSystem fs)
	{
		this.fs = fs;
	}

	@Override
	public void run() 
	{
		while(exitLogger == false)
		{
			try 
			{
				sema.acquire();
				
				if(!commitList.isEmpty())
				{
					processTransaction();
					updatePersistentFile();
				}
				
				if(!parentThread.isAlive())
				{
					if(commitList.isEmpty())
					{
						exitLogger = true;
					}
					else
					{
						sema.release();
					}
				}
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	public void start()
	{
		parentThread = Thread.currentThread();
		t = new Thread(this,"FSLoggerThread");
		t.start();
	}
	
	public void end()
	{
		exitLogger = true;
	}

	public void beginTransaction(String ... args)
	{
		commitList.add(args);
	}
	
	public void commitTransaction()
	{
		sema.release();
	}
	
	public void removeTransaction()
	{
		if(!commitList.isEmpty())
		{
			commitList.remove(commitList.size()-1);
		}
	}
	
	private void processTransaction()
	{
		String args[] = commitList.remove(0);
		
		try 
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(uncommitLog.getAbsoluteFile(),true));
			
			for(int i = 0; i < args.length; i++)
			{
				bw.append(args[i] + " ");
			}
			
			bw.append("\n");
			bw.close();
		} 
		catch (IOException e) 
		{}
	}
	
	public void updatePersistentFile()
	{
		try 
		{
			BufferedReader bw = new BufferedReader(new FileReader(uncommitLog));
			String readLine = bw.readLine();
			bw.close();
			
			if(readLine == null)
			{
				return;
			}

			String args[] = readLine.split(" ");

			if(args[0] != null && args[1] != null)
			{
				switch(args[0])
				{
				case "createFile":
					writeToPersistentFile(args[1],"f");
					break;				
				case "createDirectory" :
					writeToPersistentFile(args[1],"d");
					break;
				case "deleteDirectory" :
				case "deleteFile" :
					deleteFromPersistentFile(args[1]);
					break;
				}

				updateUnCommitLog(readLine);
				updateCommitLog(readLine);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
		
	private void updateUnCommitLog(String deleteLine)
	{
		try 
		{
			File tempFile = new File("temp.txt");
			BufferedReader reader = new BufferedReader(new FileReader(uncommitLog));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			String currentLine;
			
			while((currentLine = reader.readLine()) != null)
			{
				if(!currentLine.equals(deleteLine))
				{
					writer.write(currentLine + "\n");
				}
				else
				{
				}
			}
			
			reader.close();
			writer.close();
			uncommitLog.delete();
			tempFile.renameTo(uncommitLog);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private void updateCommitLog(String readLine)
	{
		try 
		{
			FileWriter fw = new FileWriter(commitLog,true);
			fw.append(readLine + "\n");
			fw.close();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void writeToPersistentFile(String name, String type)
	{
		try 
		{
			FileWriter fw = new FileWriter(persistentFile,true);
			fw.append(name + " :" + type + "\n");
			fw.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void deleteFromPersistentFile(String name)
	{
		File tempFile = new File("temp.txt");
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(persistentFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			String currentLine;
			
			while((currentLine = reader.readLine()) != null)
			{
				if(!currentLine.contains(name))
				{
					writer.write(currentLine + "\n");
				}
			}
			
			reader.close();
			writer.close();
			persistentFile.delete();
			tempFile.renameTo(persistentFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}












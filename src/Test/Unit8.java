package Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ChunkServer.ChunkServer;
import Client.Client;
import Client.Driver;

public class Unit8 {
	
	/**
	 * Unit8:  Multiple instances of Test6 running using different TFS Clients, appending different images to one TFS file.  
	 * This requires atomic append where the TFS defines the offset at which it appends an image.
	 * 
	 * Remember that command is necessary, so you do need to give command argument
	 * eg.) unit6 c:\a.jpg b.jpg 
	 */
	public Unit8() {
		System.out.println("Please complete the arguments.");
		System.out.print("> ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = new String();
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] args = line.split(" ");
		final String[] firstArgs = args;
		Thread one = new Thread() {
		    public void run() {
		        try {
		        	System.out.println(firstArgs.toString());
		        	Driver myDriver = new Driver();
		        	myDriver.processCommand(firstArgs);
		        } 
		        finally
		        {}
		    }  
		};
		
		line = new String();
		args = null;
		System.out.print("> ");
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		args = line.split(" ");
		final String[] secondArgs = args;
		
		Thread two = new Thread() {
		    public void run() {
		        try {
		        	System.out.println(secondArgs.toString());
		        	Driver myDriver = new Driver();
		        	myDriver.processCommand(secondArgs);
		        } 
		        finally
		        {}
		    }  
		};
		System.out.print("> ");
		line = new String();
		args = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		args = line.split(" ");
		final String[] thirdArgs = args;

		Thread three = new Thread() {
		    public void run() {
		        try {
		        	System.out.println(thirdArgs.toString());
		        	Driver myDriver = new Driver();
		        	myDriver.processCommand(thirdArgs);
		        } 
		        finally
		        {}
		    }  
		};

		three.start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		two.start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		one.start();
			
	}
	
	public static void main(String[] args) {
		Unit8 unit8 = new Unit8();
	}
	
}

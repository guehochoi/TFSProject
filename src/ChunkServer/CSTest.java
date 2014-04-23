package ChunkServer;

public class CSTest {

	public static void main(String[] args) {
		final int port1 = 667, port2 = 668, port3 = 669;
		
		Thread one = new Thread() {
		    public void run() {
		        try {
		        	String[] args = new String[1];
		        	args[0] = Integer.toString(port1);
		        	ChunkServer.main(args);
		        } 
		        finally
		        {}
		    }  
		};

		Thread two = new Thread() {
		    public void run() {
		        try {
		        	String[] args = new String[1];
		        	args[0] = Integer.toString(port2);
		        	ChunkServer.main(args);
		        } 
		        finally
		        {}
		    }  
		};

		Thread three = new Thread() {
		    public void run() {
		        try {
		        	String[] args = new String[1];
		        	args[0] = Integer.toString(port3);
		        	ChunkServer.main(args);
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

}

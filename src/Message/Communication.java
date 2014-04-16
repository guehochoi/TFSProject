package Message;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Communication extends Thread {
	
	public static int PORT = 8905;
	
	private ServerSocket serverSocket;
	private ArrayList<Message> buffer = new ArrayList<Message>(); // do i need synchronization?
	
	private boolean isClosed = false;
	
	public Communication() throws IOException {
		serverSocket = new ServerSocket(PORT);
		serverSocket.setSoTimeout(20000); // 20sec timeout 
		start();
	}
	public Communication(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(20000); // 20sec timeout 
		start();
	}
	
	public void run() {
		while (true) {
			
			try {
				Socket server = serverSocket.accept();
				DataInputStream in = new DataInputStream(server.getInputStream());
				String input = in.readUTF();
				buffer.add(new Message(input, server));
				//System.out.println("Data Recieved from " + server.getInetAddress() + ", " + input);
				
				if (isClosed) {
					serverSocket.close();
					break;
				}
			}catch(SocketTimeoutException s) {
				//socket timed out
				break;
			}catch(IOException ex) {
				//ex.printStackTrace();
				break;
			}
		}
	}
	
	public boolean isInboxEmpty() {
		return buffer.isEmpty();
	}
	
	/**
	 * output: first of inbox (Message structure needed)
	 * Note that the first element will be removed from the inbox
	 */
	public Message popFirstInbox() {
		return buffer.remove(0);
	}
	
	/**
	 * output: data of first inbox
	 * It does NOT remove the first element
	 */
	public String getFirstInbox_data() {
		return buffer.get(0).data;
	}
	
	/**
	 * output: sender's socket of first inbox
	 * It does NOT remove the first element
	 */
	public Socket getFirstInbox_sender() {
		return buffer.get(0).sender;
	}
	
	public void removeFirstInbox() {
		buffer.remove(0);
	}
	
	public Message getFirstInbox() {
		return buffer.get(0);
	}
	
	/**
	 * Reply to the sender of the first message in the inbox. 
	 * Note1: Removes the message from inbox.
	 * Note2: closes the sender socket
	 * @param msg is reply message to the sender of first message in the inbox
	 * @return true if successful
	 */
	public boolean replyToFirst(String msg) {
		try {
			DataOutputStream out = new DataOutputStream(buffer.get(0).sender.getOutputStream());
			out.writeUTF(msg);
			
			buffer.get(0).sender.close();
			buffer.remove(0);
			
		}catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * sends out the message, but closes connection right away.
	 * @param hostname - network address
	 * @param port
	 * @param msg
	 * @return true if successfuly sent 
	 */
	public static boolean msgTo(String hostname, int port, String msg) {
		try {
			Socket client = new Socket(hostname, port);
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(msg);
			out.close();
			client.close();
			return true;
		} catch ( IOException e ) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * sends out the message and wait for response, then closes socket
	 * @param hostname - network address
	 * @param port
	 * @param msg
	 * @return reply string , null if failure
	 */
	public String msgToAndWait(String hostname, int port, String msg) {
		try {
			Socket client = new Socket(hostname, port);
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(msg);
			
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			String reply = in.readUTF();
			client.close();
			return reply;
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void closeCom() {
		isClosed = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	public class Message {
		public String data;
		public Socket sender;
		Message(String data, Socket sender) {
			this.data = data;
			this.sender = sender;
		}
	}
	
	
	public static void main(String[] args) {
		Communication com=null;
		try {
			com = new Communication();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

		while (true) {
			if (com.isInboxEmpty()) {
				try {
					System.out.println("empty, going sleep...");
					Thread.sleep(1000);
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				continue;
			}
			
			System.out.println(com.getFirstInbox_data());
			com.replyToFirst("Hello back from server");
			break;
		}
		com.closeCom();
	}
}

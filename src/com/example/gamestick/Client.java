package com.example.gamestick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;
import android.widget.ImageView;
/**
 * Represents the client. The client send information to the server regarding the
 * commands achieved by the user on the game sticks.
 * @author Cédric Andreolli - Intel Corporation
 *
 */
public class Client extends Thread{
	/**
	 * The maximum size of the command queue
	 */
	private static final int MAX_SIZE = 5;
	/**
	 * The socket use to send the commands
	 */
	private Socket clientSocketCommand = null;

	/**
	 * The port used to send commands.
	 */
	int commandPort;
	/**
	 * The command queue. It's size is limited by the MAX_SIZE constant.
	 */
	private Queue<String> commandQueue = null;

	/**
	 * A reference to the image view.
	 */
	private ImageView imageView = null;

	/**
	 * The inet address of the server.
	 */
	String inetAddress = null;

	/**
	 * Indicates that the command thread is over.
	 */
	protected boolean isCommandOver;

	/**
	 * Indicates that the server must be shutted down
	 */
	private boolean isShuttedDown;

	/**
	 * Indicates that the video thread is over.
	 */
	protected boolean isVideoOver;

	/**
	 * The runnable executed when the socket are closed.
	 */
	private Runnable onCloseListener;

	/**
	 * The buffer used to read the socket.
	 */
	private BufferedReader reader = null;

	/**
	 * Is the client running ?
	 */
	private boolean running = true;

	/**
	 * The port used to received the video stream.
	 */
	private int videoPort;
	/**
	 * The runnable in charge of displaying the video stream.
	 */
	private Runnable videoRunnable = null;
	/**
	 * A reference to the video thread.
	 */
	private Thread videoThread = null;
	/**
	 * A reference to the socket writer
	 */
	private PrintWriter writer = null;
	/**
	 * Default constructor. It builds the client application. This application show 2 sticks
	 * that allow the user to send commands over the network and retrieve a video stream.
	 * @param inetAddress The inet address of the server
	 * @param commandPort The port used to send the commands
	 * @param videoPort The port used to send the video stream
	 * @param imageView A reference on the image view used to blit the video stream.
	 */
	public Client(String inetAddress, int commandPort, int videoPort, ImageView imageView) {
		this.inetAddress = inetAddress;
		this.isShuttedDown = false;
		this.running = true;
		this.commandPort = commandPort;
		this.videoPort = videoPort;
		this.imageView = imageView;
		this.isVideoOver = false;
		this.isCommandOver = false;
		this.commandQueue = new LinkedList<String>();
		this.videoRunnable = new RegisterImageRunnable(this);
	}

	/**
	 * Change the acceleration of the servo motor identified by its position.
	 * @param value The new acceleration value.
	 * @param servo The servo motor's position.
	 * @return The command has been added to the command queue.
	 * @throws IOException
	 */
	public synchronized boolean changeAcceleration(int value, int servo) throws IOException{
		String command = "ACC;"+servo+";"+value+"\r\n";
		this.notifyAll();
		if(this.commandQueue.size()>=MAX_SIZE)
			this.commandQueue.removeAll(this.commandQueue);
		return this.commandQueue.offer(command);
	}

	/**
	 * Change the position of the servo motor identified by its position.
	 * @param value The new position value.
	 * @param servo The servo motor's position.
	 * @return The command has been added to the command queue.
	 * @throws IOException
	 */
	public synchronized boolean changePosition(int value, int servo) throws IOException{
		String command = "MOVE;"+servo+";"+value+"\r\n";
		this.notifyAll();
		if(this.commandQueue.size()>=MAX_SIZE)
			this.commandQueue.removeAll(this.commandQueue);
		return this.commandQueue.offer(command);
	}

	/**
	 * Change the speed of the servo motor identified by its position.
	 * @param value The new speed value.
	 * @param servo The servo motor's position.
	 * @return The command has been added to the command queue.
	 * @throws IOException
	 */
	public synchronized boolean changeSpeed(int value, int servo) throws IOException{
		String command = "SPEED;"+servo+";"+value+"\r\n";
		this.notifyAll();
		if(this.commandQueue.size()>=MAX_SIZE)
			this.commandQueue.removeAll(this.commandQueue);
		return this.commandQueue.offer(command);
	}

	/**
	 * Close the client, close the sockets. This method can be used to restart the client.
	 * @param restart Do we need to restart the client ?
	 */
	public void close(boolean restart) {
		this.setRunning(false);
		while(!this.isAlive() && !this.videoThread.isAlive()){
			synchronized (this) {
				this.notifyAll();
			}
		}
		if(restart)
			this.onCloseListener.run();
	}

	/**
	 * 
	 * @return The command port
	 */
	public int getCommandPort() {
		return this.commandPort;
	}

	/**
	 * 
	 * @return The image view
	 */
	public ImageView getImageView() {
		return this.imageView;
	}

	/**
	 * 
	 * @return The inet address
	 */
	public String getInetAddress() {
		return this.inetAddress;
	}

	/**
	 * 
	 * @return The OnCloseListener.
	 */
	public Runnable getOnCloseListener() {
		return this.onCloseListener;
	}

	/**
	 * 
	 * @return The video port.
	 */
	public int getVideoPort() {
		return this.videoPort;
	}

	/**
	 * 
	 * @return Is the command thread finished ?
	 */
	public boolean isCommandOver() {
		return this.isCommandOver;
	}

	/**
	 * 
	 * @return Is the client running ?
	 */
	public synchronized boolean isRunning() {
		return this.running;
	}

	/**
	 * 
	 * @return Must the client be shutted down.
	 */
	public boolean isShuttedDown() {
		return this.isShuttedDown;
	}

	/**
	 * 
	 * @return Is the video stream thread over ?
	 */
	public boolean isVideoOver() {
		return this.isVideoOver;
	}


	@Override
	public void run() {
		/**
		 * This thread is in charge of creating the command socket. It also creates
		 * the thread used to stream the video.
		 */
		InetAddress add;
		//We create the video stream thread
		this.videoThread = new Thread(this.videoRunnable);
		this.videoThread.start();
		try {
			add = InetAddress.getByName(this.inetAddress);
			//Create the command socket
			this.clientSocketCommand = new Socket(add, this.commandPort);
			//Get the reader and writer from the socket
			this.writer = new PrintWriter(this.clientSocketCommand.getOutputStream());
			this.reader = new BufferedReader(new InputStreamReader(this.clientSocketCommand.getInputStream()));

			/**
			 * While the client is running, we read the command queue. If it's empty,
			 * the current thread is put in the waiting state. Every command added in
			 * the command queue awakes the current thread.
			 */
			while(this.isRunning()){
				if(!this.commandQueue.isEmpty()){
					String command = null;
					synchronized (this) {
						//Every access to the command queue are done in
						//a synchronized area.
						command = this.commandQueue.poll();
					}
					this.writer.write(command);
					this.writer.flush();
					this.reader.readLine();
				}else{
					synchronized (this) {
						this.wait();
					}
				}
			}

		} catch (Exception e) {
			//If a problem happens. In most cases, problems come from
			//closed sockets.
			this.setRunning(false);
		}
		try {
			this.clientSocketCommand.close();
		} catch (Exception e) {
		}
		synchronized (Client.this) {
			Log.i("client", "command is going to be over ?");
			if(Client.this.isVideoOver){
				Log.i("client", "ok command is proceed");
				synchronized (Client.this) {
					try {
						Client.this.wait(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if(!Client.this.isShuttedDown())
					Client.this.onCloseListener.run();
			}else{
				Client.this.isCommandOver = true;
			}
		}
	}

	public void setCommandOver(boolean isCommandOver) {
		this.isCommandOver = isCommandOver;
	}

	public void setCommandPort(int commandPort) {
		this.commandPort = commandPort;
	}
	public void setImageView(ImageView imageView) {
		this.imageView = imageView;
	}

	public void setInetAddress(String inetAddress) {
		this.inetAddress = inetAddress;
	}

	public void setOnCloseListener(Runnable onCloseListener) {
		this.onCloseListener = onCloseListener;
	}
	public void setOnSocketCloseListener(Runnable runnable) {
		this.onCloseListener = runnable;
	}

	public synchronized void setRunning(boolean running) {
		this.running = running;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void setShuttedDown(boolean isShuttedDown) {
		this.isShuttedDown = isShuttedDown;
	}

	public void setVideoOver(boolean isVideoOver) {
		this.isVideoOver = isVideoOver;
	}

	public void setVideoPort(int videoPort) {
		this.videoPort = videoPort;
	}
}

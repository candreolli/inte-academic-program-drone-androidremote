package com.example.gamestick;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
/**
 * Register and display a new image.
 * @author Cédric Andreolli - Intel Corporation
 *
 */
public class RegisterImageRunnable implements Runnable{
	/**
	 * A reference on the image to display.
	 */
	private Bitmap bmp = null;
	/**
	 * A reference on the client.
	 */
	private Client client = null;

	/**
	 * The client socket used to retrieve the image.
	 */
	Socket clientSocketVideo = null;

	/**
	 * Default constructor.
	 * @param client The client.
	 */
	public RegisterImageRunnable(Client client) {
		this.client = client;
	}

	/**
	 * Erase the BMP image from the phone memory.
	 */
	private void cleanBMP() {
		if(this.bmp != null)
			this.bmp.recycle();
		this.bmp = null;
	}

	/**
	 * Register a JPEG image.
	 * @param buffer The image bytes.
	 * @param width The width of the image in pixels.
	 * @param height The height of the image in pixels.
	 */
	private void registerImage(byte[] buffer, int width, int height) {
		/**
		 * To save memory, we need to recycle the previous image.
		 */
		this.cleanBMP();

		this.bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
		this.client.getImageView().post(new Runnable() {

			@Override
			public void run() {
				RegisterImageRunnable.this.client.getImageView().setImageBitmap(Bitmap.createScaledBitmap(RegisterImageRunnable.this.bmp,
						RegisterImageRunnable.this.client.getImageView().getWidth(),
						RegisterImageRunnable.this.client.getImageView().getHeight(), false));
			}
		});

		/*FileOutputStream outStream = null;
		try {
			String name = Environment.getExternalStorageDirectory().getCanonicalPath();


			outStream = new FileOutputStream(name+"/img.jpg");
			outStream.write(buffer);
			outStream.close();
		}catch(Exception e){
			e.printStackTrace();
		}*/
	}

	@Override
	public void run() {
		while(this.client.isRunning()){
			try {
				InetAddress add;
				add = InetAddress.getByName(this.client.getInetAddress());

				this.clientSocketVideo = new Socket(add, this.client.getVideoPort());
				BufferedInputStream inBuf = new BufferedInputStream(this.clientSocketVideo.getInputStream());
				InputStreamReader inStr = new InputStreamReader(inBuf);
				BufferedReader reader = new BufferedReader(inStr);
				PrintWriter writer = new PrintWriter(this.clientSocketVideo.getOutputStream());
				while(this.client.isRunning()){
					int size = 0, width = 0, height = 0;

					writer.println("TAKE_PICTURE");
					writer.flush();

					String s = reader.readLine();
					size = Integer.parseInt(s);
					writer.println("SIZE RECEIVED "+size);
					writer.flush();

					s = reader.readLine();
					width = Integer.parseInt(s);
					writer.println("WIDTH RECEIVED "+width);
					writer.flush();

					s = reader.readLine();
					height = Integer.parseInt(s);
					writer.println("HEIGHT RECEIVED "+height);
					writer.flush();

					byte[] buffer = new byte[size];
					int res;
					int offset = 0;
					int currentSize = size;
					while(currentSize>0){
						res = inBuf.read(buffer, offset, currentSize);
						offset += res;
						currentSize -= res;
					}

					//register the image
					this.registerImage(buffer, width, height);
					writer.println("DATA RECEIVED");
					writer.flush();
				}

			} catch (Exception e) {
				this.client.setRunning(false);

				Log.i("client", "Connection LOST RegisterImage");

			}

		}
		try {
			this.clientSocketVideo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		synchronized (this.client) {
			Log.i("client", "Video is going to be over ?");
			if(this.client.isCommandOver()){
				Log.i("client", "OK run is proceed");
				synchronized (RegisterImageRunnable.this.client) {
					try {
						RegisterImageRunnable.this.client.wait(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if(!this.client.isShuttedDown())
					this.client.getOnCloseListener().run();
			}else{
				this.client.setVideoOver(true);
				synchronized (this.client) {
					this.client.notifyAll();
				}
			}
		}

	}

}

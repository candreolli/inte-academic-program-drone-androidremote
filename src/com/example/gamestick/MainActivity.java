package com.example.gamestick;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity {
	/**
	 * The client.
	 */
	private Client client = null;
	/**
	 * The image view used to display the camera stream.
	 */
	private ImageView imageView = null;
	/**
	 * The left game stick.
	 */
	private GameStick layoutStickLeft = null;
	/**
	 * The right game stick.
	 */
	private GameStick layoutStickRight = null;
	/**
	 * The NFC Adapter.
	 */
	private NfcAdapter nfcAdapter = null;
	/**
	 * The pending intent used to start this activity when the NFC tag is discovered.
	 */
	private PendingIntent pendingIntent = null;

	/**
	 * 
	 * @return The status bar height
	 */
	public int computeAndroidStatusBarHeight() {
		int res = 0;
		int id = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (id > 0) {
			res = this.getResources().getDimensionPixelSize(id);
		}
		return res;
	}
	/**
	 * Creates a new client. The client is in charge of the communications with the server.
	 */
	private void createNewClient() {
		Log.i("client", "Client is launching...");
		Settings settings = Settings.getInstance(this);
		this.client = new Client(settings.getInetAddress(),
				settings.getCommandPort(),
				settings.getVideoPort(), this.imageView);
		this.client.setOnSocketCloseListener(new Runnable() {

			@Override
			public void run() {
				Settings settings = Settings.getInstance(MainActivity.this);
				MainActivity.this.client = new Client(settings.getInetAddress(),
						settings.getCommandPort(), settings.getVideoPort(), MainActivity.this.imageView);
				MainActivity.this.client.setOnCloseListener(this);
				MainActivity.this.client.start();
			}
		});

		this.client.start();
	}




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);
		this.imageView = (ImageView)this.findViewById(R.id.IDImageView);
		this.createNewClient();

		this.layoutStickLeft = (GameStick) this.findViewById(R.id.IDStickLeft);
		this.layoutStickRight = (GameStick) this.findViewById(R.id.IDStickRight);
		this.setLayoutSizes();

		this.registerJoystickChangeEventListener();

		this.registerNFCEventListener();
	}

	public void onNewIntent(Intent intent) {
		String[] parsedData = this.parseData(intent.getData());
		Log.i("client", intent.getData().toString());
		Settings settings = Settings.getInstance(this);
		settings.setCommandPort(Integer.parseInt(parsedData[1]));
		settings.setVideoPort(Integer.parseInt(parsedData[2]));
		settings.setInetAddress(parsedData[0]);
		this.client.close(true);
	}
	@Override
	protected void onPause() {
		super.onPause();
		this.nfcAdapter.disableForegroundDispatch(this);
		this.client.setShuttedDown(true);
	}
	@Override
	protected void onResume() {
		super.onResume();
		this.nfcAdapter.enableForegroundDispatch(this, this.pendingIntent, null, null);
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.client.close(false);
	}

	private String[] parseData(Uri data) {
		if(data != null){
			String dataStr = data.toString();
			String begin = dataStr.substring(7);
			Log.i("client", "TAG BEGIN: "+begin);
			return begin.split("/");
		}
		return new String[]{};
	}

	/**
	 * Register the behavior on JostickChange event.
	 */
	private void registerJoystickChangeEventListener() {
		final TextView tvl = (TextView) this.findViewById(R.id.jleft);
		this.layoutStickLeft.setOnJoystickChangeListener(new OnJoystickChangedListener() {

			@Override
			public void readValues(Point newPosition) {
				tvl.setText(""+newPosition);
				if(MainActivity.this.client != null){
					try {
						MainActivity.this.client.changePosition(newPosition.x, 1);
						MainActivity.this.client.changePosition(newPosition.y, 5);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		final TextView tvr = (TextView) this.findViewById(R.id.jright);
		this.layoutStickRight.setOnJoystickChangeListener(new OnJoystickChangedListener() {

			@Override
			public void readValues(Point newPosition) {
				tvr.setText(""+newPosition);
				if(MainActivity.this.client != null){
					try {
						MainActivity.this.client.changePosition(newPosition.y, 3);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	/**
	 * Register the behavior on NFC push message.
	 */
	private void registerNFCEventListener() {
		this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		this.pendingIntent = PendingIntent.getActivity(
				this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		this.nfcAdapter.setNdefPushMessage(null, this, this);
	}

	private void setLayoutSizes() {
		//Retrieve the size of the screen
		Display display = this.getWindowManager().getDefaultDisplay();
		Point outSize = new Point();
		display.getSize(outSize);

		int screenWidth = outSize.x;
		int screenHeight = outSize.y;

		//Compute the new layout width and height
		int newLayoutWidth = screenWidth / 2;
		int newLayoutHeight = newLayoutWidth;

		int marginTop = (screenHeight - this.computeAndroidStatusBarHeight() - newLayoutWidth)/2;

		//Set the margin top
		LayoutParams paramLeft = new LayoutParams(newLayoutWidth, newLayoutHeight);
		LayoutParams paramRight = new LayoutParams(newLayoutWidth, newLayoutHeight);
		paramLeft.topMargin = marginTop;
		paramRight.topMargin = marginTop;

		this.layoutStickLeft.setLayoutParams(paramLeft);
		this.layoutStickRight.setLayoutParams(paramRight);

	}
}

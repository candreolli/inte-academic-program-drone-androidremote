package com.example.gamestick;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Settings {
	private static final String COMMAND_PORT = "command_port";
	private static final String DEFAULT__INET_ADDRESS_VALUE = "192.168.1.157";
	private static final int DEFAULT_COMMAND_PORT_VALUE = 8888;
	private static final int DEFAULT_VIDEO_PORT_VALUE = 8889;
	private static final String INET_ADDRESS = "inet_address";
	private static Settings instance = null;
	public static final String PREF_SETTING = "setting_file";
	private static final String VIDEO_PORT = "video_port";

	public static Settings getInstance(Context ctx){
		if(instance == null)
			instance = new Settings(ctx);
		return instance;
	}
	private int commandPort = 0;

	private Context context = null;

	private String inetAdress;

	private SharedPreferences settings = null;

	private int videoPort = 0;

	private Settings(Context ctx){
		this.context = ctx;
		this.settings = this.context.getSharedPreferences(PREF_SETTING, Context.MODE_PRIVATE);

	}

	public int getCommandPort() {
		if(this.commandPort == 0){
			this.commandPort = this.settings.getInt(COMMAND_PORT, DEFAULT_COMMAND_PORT_VALUE);
		}
		return this.commandPort;
	}

	public String getInetAddress() {
		if(this.inetAdress == null || this.inetAdress==""){
			this.inetAdress = this.settings.getString(INET_ADDRESS, DEFAULT__INET_ADDRESS_VALUE);
		}
		return this.inetAdress;
	}

	public int getVideoPort() {
		if(this.videoPort == 0){
			this.videoPort = this.settings.getInt(VIDEO_PORT, DEFAULT_VIDEO_PORT_VALUE);
		}
		return this.videoPort;
	}

	public void setCommandPort(int commandPort) {
		this.commandPort = commandPort;
		Editor editor = this.settings.edit();
		editor.putInt(COMMAND_PORT, commandPort);
		editor.commit();
	}

	public void setInetAddress(String address){
		this.inetAdress = address;
		Editor editor = this.settings.edit();
		editor.putString(INET_ADDRESS, address);
		editor.commit();
	}

	public void setVideoPort(int videoPort) {
		this.videoPort = videoPort;
		Editor editor = this.settings.edit();
		editor.putInt(VIDEO_PORT, videoPort);
		editor.commit();
	}
}

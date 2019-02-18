package com.connectsdk.service.convergence;

import java.util.List;

import android.app.Activity;

/**
 * <p>
 * This class extending android {@link Activity} class. To use this library, the common android class which extending
 * {@link Activity} class should replaced with extending {@link NSListener} class. The workflow of application will
 * not significantly changed since {@link NSListener} class also extending {@link Activity}. 
 * </p>
 * <p>
 * {@link NSListener} provide abstract function or interface for another class in library to access/call the android
 * main class which enable android to discover, connect, and communicate with N-Service capable devices like Samsung Smart TV, BD Player and Home-Theather seamlessly. 
 * </p>
 * 
 * @author I Made Krisna Widhiastra (im.krisna@gmail.com)
 * 
 * @version 1.0.1
 * 
 * */
public abstract class NSListener {

	protected String tag;
	
	/**
	 * <p><b><i>public NSListener(String tag)</i></b></p>
	 * <p>
	 * Constructor for tagging the android class to be able to communicate with {@link com.imkrisna.samsung.nservice} library
	 * </p>
	 * 
	 * <pre>
	 * {@code
	 * public class MainActivity extends NSListener {
	 * 	public MainActivity(){
	 * 		super("MainActivity");
	 * 	}
	 * }
	 * }
	 * </pre>
	 * 
	 * @param tag - Unique Identifier tag to differentiate between {@link NSListener} instance, Commonly use class name for identifier.
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public NSListener(String tag){
		this.tag = tag;
	}	
	
	/**
	 * @since 1.0.0
	 * */
	public abstract void onWifiChanged();
	
	/**
	 * <p><b><i>public abstract void onDeviceChanged(List<{@link NSDevice}> devices)</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSDiscovery} process. When the search process found a
	 * device which capable to do N-Service feature, the list of capable devices will be updated and
	 * this method notified and receiving the new list of devices.
	 * </p>
	 * 
	 * @param devices - Collection of founded N-Service capable device
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onDeviceChanged(List<NSDevice> devices);
	
	/**
	 * <p><b><i>public abstract void onConnected()</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when the connection
	 * is successfully made a handshake with N-Service capable device.
	 * </p>
	 * 
	 * @since 1.0.0
	 * 
	 * @param device*/
	public abstract void onConnected(NSDevice device);
	
	/**
	 * <p><b><i>public abstract void onDisconnected()</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when the connection
	 * is disconnected either by the android or by the paired N-Service capable device.
	 * </p>
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onDisconnected();
	
	/**
	 * <p><b><i>public abstract void onConnectionFailed(int code)</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when the connection
	 * attempt is failed.
	 * </p>
	 * 
	 * @param code - Error code thrown by {@link NSConnection}. The value is HTTP Response code
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onConnectionFailed(int code);
	
	/**
	 * <p><b><i>public abstract void onMessageSent()</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when the sending message
	 * to paired N-Service device is succeed.
	 * </p>
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onMessageSent();
	
	/**
	 * <p><b><i>public abstract void onMessageSendFailed(int code)</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when the sending message
	 * attempt is failed.
	 * </p>
	 * 
	 * @param code - Error thrown by {@link NSConnection}. The value is HTTP Response code
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onMessageSendFailed(int code);
	
	/**
	 * <p><b><i>public abstract void onMessageReceived(String message)</i></b></p>
	 * <p>
	 * Event handler method listening to {@link NSConnection} process that will be notified when there are an incoming
	 * message from paired N-Service device received by the process.
	 * </p>
	 * 
	 * @param message - Message string from paired N-Service device
	 * 
	 * @since 1.0.0
	 * 
	 * */
	public abstract void onMessageReceived(String message);
	
	/**
	 * <p><b><i>protected String getTag()</i></b></p>
	 * 
	 * @return Identifier tag for this class instance
	 * 
	 * @since 1.0.0
	 * 
	 * */
	protected String getTag(){
		return this.tag;
	}	
	
}

package com.connectsdk.service.convergence;

/**
 * <p>
 * This class represents single N-Service capable device. N-Service Device commonly manufactured
 * by Samsung Electronics such as Smart TV, Smart Blu-Ray Players, Set-Top Boxes and many more.
 * </p>
 * 
 * @version 1.0.1
 * 
 * @author I Made Krisna Widhiastra (im.krisna@gmail.com)
 * 
 * */
public class NSDevice {
	
	private String deviceIP;
	private String deviceName;
	
	/**
	 * <p>Constructor for NSDevice Object</p>
	 * <pre>
	 * {@code
	 * NSDevice device = new NSDevice("192.168.137.200", "[TV]Samsung LED46");
	 * }
	 * </pre>
	 * 
	 * @param ip IPv4 Address of NService Device
	 * @param name NService Friendly Device Name
	 * 
	 * @since 1.0.0
	 * 
	 */
	public NSDevice(String ip, String name){
		this.deviceIP 	= ip;
		this.deviceName = name;		
	}
	
	/**
	 * <p><i><b>public String getIP()</b></i></p>
	 * 
	 * <p>Retrieving NSDevice IPv4 Address. Used for connecting this device to NSDevice
	 * N-Screen Frameworks API</p>
	 * 
	 * <p><i>Example: 192.168.137.200</i></p>
	 * 
	 * @return NSDevice IPv4 Address
	 * 
	 * @since 1.0.0
	 * 
	 */
	public String getIP(){
		return this.deviceIP;
	}
	
	/**
	 * <p><i><b>public String getName()</b></i></p>
	 * 
	 * <p>Retrieving NSDevice Friendly Name. Used for more User-Friendly Display
	 * than IPv4 Address</p>
	 * 
	 * <p><i>Example: [TV]Samsung LED46</i></p>
	 * 
	 * @return NSDevice Friendly Name
	 * 
	 * @since 1.0.0
	 * 
	 */
	public String getName(){
		return this.deviceName;
	}
	
	/**
	 * 
	 * @since 1.0.1
	 * 
	 * */
	public String getNameAndIp(String separator){
		return (this.getName() + separator + this.getIP());
	}
	
}

/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.device;

/**
 * This enumeration class has different device connection states handled by
 * the CSC library.
 * 
 * @author <b>Suryaprakash</b> on Jul 31, 2014 3:53:14 PM </br>
 *
 */
public enum DeviceConnectionState {
	/**
	 * This indicates that device is connected.
	 */
	CONNECTED,
	/**
	 * This indicates that device is not connected.
	 */
	NOT_CONNECTED,
	/**
	 * This indicates that device is connecting.
	 */
	CONNECTING,
	/**
	 * This indicates error in device connection.
	 */
	ERROR_IN_CONNECTION
}

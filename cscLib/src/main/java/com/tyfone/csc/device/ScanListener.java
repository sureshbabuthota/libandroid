/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.device;

import com.tyfone.csc.CSC;

/**
 * This class is used as a listener when application calls
 * {@link CSC#getAvailableDevices(DeviceType, ScanListener)}. It receives scan
 * start event, scan stop event and device information when a device is found.
 * 
 * @author Surya Prakash
 */
public interface ScanListener {

	/**
	 * To be called when scanning starts for getting a list of remote devices.
	 */
	void onScanStart();

	/**
	 * To be called when remote device is found.
	 * 
	 * @see CSC#getAvailableDevices(DeviceType, ScanListener)
	 * 
	 * @param device
	 *            {@link DeviceDetails} instance.
	 */
	void onDeviceFound(DeviceDetails deviceDetails);

	/**
	 * To be called when remote device is found.
	 * 
	 * @see CSC#getAvailableDevices(DeviceType, ScanListener)
	 * 
	 * @param device
	 *            {@link DeviceDetails} instance.
	 * @param rssi
	 *            RSSI value of a BLE device.
	 */
	void onDeviceFound(DeviceDetails onDeviceFound, int rssi);

	/**
	 * To be called when scanning for remote devices stops.
	 */
	void onScanStop();
}

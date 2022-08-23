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
 * <p>
 * This enum class has the device types supported by library.
 * </p>
 * 
 * Created by <b>Surya Prakash</b> on Jul 30, 2014 2:11:43 PM </br>
 * 
 */
public enum DeviceType {

	/**
	 * If user sets <b>ALL</b>, it takes all types of devices
	 * {@link DeviceType#SIDE_SAFE}, {@link DeviceType#SIDE_TAP},
	 * {@link DeviceType#BT_CLASSIC}, {@link DeviceType#BLE}
	 * //TODO: Pradeep_Code_Review - Use SDIO in comments as SIDE_SAF and
	 * SIDETAP
	 * 
	 * @see SIDE_SAF
	 * @see SIDE_TAP
	 * @see BT_CLASSIC
	 * @see BLE
	 */
	ALL,
	/**
	 * SD_IO represents a MicroSD form factor of a CSC Device.
	 */
	SD_IO,
	/**
	 * Represents a bluetooth form factor of a CSC device with BT
	 * Classic module in it.
	 */
	BT_CLASSIC,
	/**
	 * Represents a bluetooth form factor of a CSC device with BLE module in it.
	 * 
	 * BLE is a low energy bluetooth device used to communicate with other
	 * Bluetooth LE devices. Android supports BLE from Version 4.3 onwards.
	 */
	BLE
}

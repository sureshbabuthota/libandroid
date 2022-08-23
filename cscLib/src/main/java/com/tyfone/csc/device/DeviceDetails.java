/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.device;

import android.bluetooth.BluetoothDevice;

/**
 * Represents a remote Bluetooth device. A {@link DeviceDetails} lets you to
 * query information about it, such as the name, address, class and bonding
 * state.
 *
 * 
 * <p class="note">
 * <strong>Note:</strong> Requires the
 * {@link android.Manifest.permission#BLUETOOTH} permission.
 * 
 * @author Srikar
 *
 */
public final class DeviceDetails {
	
	/**
	 * Represent device MAC address in case of bluetooth device or mounted path in case of SDIO. 
	 * 
	 */
	private String address;
	/**
	 * Name of the device in case of bluetooth,null if it is SDIO.
	 */
	private String name;
	/**
	 * State of the device in case of bluetooth,null if it is SDIO.
	 */
	private DeviceBondState status;
	private DeviceType deviceType;
	
	/**
	 * 
	 * Flag to state weather device acts like a slave or master,slave if value is true else master.
	 */
	private boolean isSlave;
	
	/**
	 * Default constructor to create DeviceDetails instance.
	 */
	public DeviceDetails() {
	}
	
	/**
	 * Constructor to create instance of DeviceDetails, given address and {@link DeviceType}.
	 * @param address MAC Address or Mounted path(SDIO).
	 * @param deviceType {@link DeviceType}.
	 * 
	 * @see DeviceType
	 */
	public DeviceDetails(String address,DeviceType deviceType) {
		this.address=address;
		this.deviceType=deviceType;
		this.isSlave=true;
		
	}
	
	/**
	 * Constructor to create instance of Device details, given address and {@link DeviceType}.
	 * @param address MAC Address or Mounted path(SDIO).
	 * @param deviceType {@link DeviceType} of this.
	 * @param isSlave  Flag to state weather device acts like a slave or master.
	 * 
	 * @see DeviceType
	 */
	public DeviceDetails(String address,DeviceType deviceType,boolean isSlave) {
		this.address=address;
		this.deviceType=deviceType;
		this.isSlave=false;
		
	}

	/**
	 * This method returns device MAC address of bluetooth device or mounted path in case of SDIO, otherwise
	 * returns NULL.
	 * 
	 * @return device MAC address or mounted path(SDIO).
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * This method returns device name if available, otherwise returns NULL.
	 * 
	 * @return device name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the bond state of a remote device.
	 * <p>
	 * Possible values for the bond state are: {@link BluetoothDevice#BOND_NONE}
	 * {@link BluetoothDevice#BOND_BONDING}, {@link BluetoothDevice#BOND_BONDED}.
	 * <p>
	 * Requires {@link android.Manifest.permission#BLUETOOTH}.
	 * 
	 * @return bonded state of a device.
	 */
	public DeviceBondState getStatus() {
		return status;
	}

	/**
	 * Sets MAC address or mounted path(SDIO) of a device.
	 * 
	 * @param address
	 *            MAC address or mounted path of a device.
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * Sets name of a device.
	 * 
	 * @param name
	 *            name of device.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets bonded state of a device.
	 * <p>
	 * Possible values for the status are: {@link DeviceBondState#BOND_NONE}
	 * {@link DeviceBondState#BOND_BONDING}, {@link DeviceBondState#BOND_BONDED}.
	 * </p>
	 * Requires {@link android.Manifest.permission#BLUETOOTH}.
	 * 
	 * @param bondBonded
	 *            Bonded state of a device.
	 */
	public void setStatus(DeviceBondState bondBonded) {
		this.status = bondBonded;
	}

	/**
	 * This method returns the {@link DeviceType}.
	 * 
	 * @return deviceType of a device.
	 * @see DeviceType
	 */
	public DeviceType getDeviceType() {
		return deviceType;
	}

	/**
	 * This method sets {@link #deviceType}.
	 * 
	 * @param deviceType
	 *            type of the device
	 */
	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}
	
	/**
	 * Returns a value to determine weather the device is acting like a slave or master. If true device is slave else master.
	 * @return value to state the device is slave or master.
	 */
	public boolean isSlave() {
		return isSlave;
	}
	
	
}

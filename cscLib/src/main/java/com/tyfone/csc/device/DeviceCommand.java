/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc.device;

import java.util.logging.Logger;

import com.tyfone.csc.helper.ConversionHelper;

/**
 * Enum to define basic device commands.
 * 
 * @author <b>Srikar</b>, created on Aug 25, 2014 11:30:12 AM </br>
 *
 */
public enum DeviceCommand {

	/**
	 * Reset smart card
	 */
	RESET_SMARTCARD(0x300101),
	/**
	 * To enable RF communication.
	 */
	RF_ON(0),
	/**
	 * To disable RF communication.
	 */
	RF_OFF(0),
	/**
	 * To get the firmware version of the
	 * device.
	 */
	FW_VERSION(0x300104),
	
	/**
	 * To power on SDIO device.
	 */
	POWER_ON(0X0001),
	/**
	 * To power off SDIO device.
	 */
	POWER_OFF(0X0002),
	/**
	 * To get ATR of a device.
	 */
	GET_ATR(0x0003);

	private int deviceCommandInstruction;

	/**
	 * Constructs device command.
	 * 
	 * @param deviceCommandInstruction
	 */
	private DeviceCommand(final int deviceCommandInstruction) {
		this.deviceCommandInstruction = deviceCommandInstruction;
	}

	/**
	 * This method returns byte array of device command instruction.
	 * 
	 * @return device command as byte array.
	 */
	public byte[] getDeviceCommandInstruction() {
//		Logger.info("DeviceCommand" + getClass().getSimpleName(),
//				Integer.toString(deviceCommandInstruction, 16));
		return ConversionHelper.hexStringToByteArray(Integer.toString(
				deviceCommandInstruction, 16));
	}
}

/* 
 * Created by author <b>suryaprakash</b> on Jul 30, 2014 2:22:44 PM </br>
 * Project: CSC Library </br>
 * 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.communication;

import com.tyfone.csc.CSC;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.CommandAPDU;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * 
 * Interface to get connection state of a {@link Device}, to find list of
 * {@link Applet}s present in a {@link Smartcard}, and to receive response from an
 * {@link Applet}.
 */
public interface CSCCallback {

	/**
	 * To be called once a response for a command is received.
	 * 
	 * @param response
	 *            response in bytes.
	 * 
	 * @param error
	 *            error code for this response.
	 * 
	 * 
	 */
	void onReceiveResponse(byte[] response, ErrorCodes error);

	/**
	 * To  be called once the response for a command APDU is received.
	 * 
	 * @param response
	 *            instance of {@link ResponseAPDU}.
	 * @param error
	 *            error code for this response.
	 * 
	 * @see Applet#sendApdu(CommandAPDU)
	 * @see CommandAPDU
	 * @see ResponseAPDU
	 */
	void onReceiveResponse(ResponseAPDU response, Applet applet,
						   ErrorCodes error);

	/**
	 * To be called when {@link DeviceConnectionState} of a device is changed. <br>
	 * <b>Note:</b> {@link Device} for the DeviceDetails set in
	 * {@link CSC#connectToDevice(DeviceDetails, CSCCallback)} is returned
	 * through this callback method.</b>
	 * 
	 * @param device
	 *            {@link Device} for which connection state is changed.
	 * @param deviceState
	 *            {@link DeviceConnectionState} of a Device.
	 * 
	 * @see Device
	 * @see DeviceDetails
	 * @see DeviceConnectionState
	 */
	void onConnectionChange(Device device, DeviceConnectionState deviceState);

	/**
	 * TODO: Need to be implemented.
	 * 
	 * @param value
	 */
	void onUpdateRSSI(float value);

	/**
	 * To be called once status for the list of appletIDs are obtained.
	 * 
	 * @see  Smartcard#findApplet(java.util.List)
	 * @param smartcard
	 *            SmartCard instance on which find applet is performed. Find
	 *            applet status can be obtained by calling
	 *            {@link Smartcard#getFindAppletResponseList()}.
	 */
	void onFindAppletResponse(Smartcard smartcard, ErrorCodes errorCodes);

	/**
	 * To be called once status of selectApplet command is received during
	 * {@link Smartcard#setApplet(Applet)}.
	 * 
	 * @param applet
	 *            selected {@link Applet} instance.
	 * @param selectedState
	 *            {@link Applet} selected state, is true if Applet is selected.
	 */
	void onSetAppletStatus(Applet applet, boolean selectedState);

}

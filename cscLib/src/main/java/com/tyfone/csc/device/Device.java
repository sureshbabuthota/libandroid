/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc.device;

import java.util.ArrayList;
import java.util.List;

import com.tyfone.csc.CSC;
import com.tyfone.csc.ble.BLEDevice;
import com.tyfone.csc.bt.BTClassicDevice;
import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.BluetoothHelper;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.sdio.SDIODevice;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * 
 * <p>
 * This is an abstract class for getting a list of available devices, connecting
 * to a selected device and establishing a communication channel.
 * </p>
 * 
 * <br>
 * Get a {@link Device} from the set of devices returned by
 * {@link CSC#getAvailableDevices(DeviceType, ScanListener)}. </b>
 * 
 * <p>
 * {@link CSC#connectToDevice(DeviceDetails, CSCCallback)} creates
 * {@link Device} instance and a connection is established by calling
 * {@link #connect(DeviceDetails, CSCCallback)}. Connection status can be checked by
 * calling {@link #getConnectionStatus()}.
 * </p>
 * <p>
 * Instance of a device is returned through a callback
 * {@link ScanListener#onDeviceFound(DeviceDetails)}
 * </p>
 * 
 * <p>
 * Once connection is established, use {@link #getAvailableCards()}
 * to get list of available {@link Smartcard}s present in the device.
 * </p>
 * 
 * @author Srikar and Suryaprakash.
 * 
 */
public abstract class Device implements CSCCallback {
	// Initializing variables
	protected DeviceType deviceType = null;
	protected static ScanListener scanListner;

	/**
	 * This method fetches all the available devices based on DeviceType. Identified devices are sent to application using
	 * {@link ScanListener} callback.
	 * 
	 * @param deviceType
	 *            Type of available devices to check.
	 * @param scanListner
	 *            Sends available remote devices to application as
	 *            a callback.
	 * @throws CSCException
	 *             with {@link ErrorCodes}
	 */
	public static void getAvailableDevices(DeviceType deviceType,
			ScanListener scanListner) throws CSCException {
		Device.scanListner = scanListner;

		// Sending scan started callback to application
		if (scanListner != null)
			scanListner.onScanStart();

		switch (deviceType) {
		case ALL:
			BTClassicDevice.getAvailableDevices();
			if (BluetoothHelper.hasBLESupport())
				BLEDevice.getAvailableDevices();
			SDIODevice.getAvailableDevices();
			break;

		case BT_CLASSIC:
			BTClassicDevice.getAvailableDevices();
			break;

		case BLE:
			if (BluetoothHelper.hasBLESupport())
				BLEDevice.getAvailableDevices();
			break;

		case SD_IO:
			SDIODevice.getAvailableDevices();
			break;

		default:
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		}

	}

	private ArrayList<Smartcard> smartCardsList;

	/**
	 * This method establishes connection with a Device. If the device
	 * is already connected, it will use the same connection for the further
	 * communication.
	 * 
	 * @param cscCallback
	 *            Sends the device connection and communication
	 *            information to application as callback.
	 * @param deviceDetails
	 *            Device details to connect.
	 * @throws CSCException
	 * */
	public abstract void connect(DeviceDetails deviceDetails,
			CSCCallback cscCallback) throws CSCException;

	/**
	 * This method disconnects communication channel between the connected devices.
	 * */
	public abstract void disconnect();

	/**
	 * This method checks connection status of a device. It returns
	 * status of device connection as {@link DeviceConnectionState}.
	 * 
	 * @return connection state of device.
	 */
	public abstract DeviceConnectionState getConnectionStatus();

	/**
	 * This method returns smart cards available in device.
	 * 
	 * @return list of {@link Smartcard}s present.
	 */
	public List<Smartcard> getAvailableCards() {
		return listOfAvailableSmartCards();
	}

	/**
	 * This method will return smart cards available in device.
	 * 
	 * @return returns List of {@link Smartcard} present.
	 */

	// Should implement returning the list of multiple SmartCard objects.
	// Currently this method will return only one SmartCard as Sidekey support
	// only one.
	private ArrayList<Smartcard> listOfAvailableSmartCards() {
		if (smartCardsList == null || smartCardsList.size() == 0) {
			smartCardsList = new ArrayList<Smartcard>();
			smartCardsList.add(new Smartcard(this));
		}

		return smartCardsList;
	}

	/**
	 * <p>
	 * This method communicates with a device by sending a command.
	 * </p>
	 * 
	 * @param command
	 *            command bytes to write to a device.
	 * @return response as bytes.
	 */
	public abstract byte[] sendCommand(byte[] commandBytes) throws CSCException;

	/**
	 * <p>
	 * This method communicates with a device by sending a command.
	 * </p>
	 * 
	 * <p>
	 * Response from a device is returned through
	 * {@link CSCCallback#onReceiveResponse(ResponseAPDU, com.tyfone.csc.smartcard.Applet, ErrorCodes)}.
	 * </p>
	 * 
	 * @param command
	 *            command to write to a remote device.
	 * @param cscCallback
	 *            To send response data received from a remote device.
	 */
	public abstract void sendCommand(byte[] commandBytes,
			ReadWriteCallback readWriteCallback);

	/**
	 * <p>
	 * This method sends basic device commands like POWER ON, RF ON  etc.
	 * </p>
	 * 
	 * @param deviceCommand
	 *            {@link DeviceCommand} to send to device.
	 * @return string if any value is received from device else null.
	 */
	public abstract String sendCommand(DeviceCommand deviceCommand)
			throws CSCException;

	/**
	 * This method enables polling option to receive RSSI value.
	 */
	public abstract int enablePolling(Boolean isPolling, int interval);

	/**
	 * This method stops Device scan based on {@link DeviceType}.
	 * 
	 * @param deviceType
	 *            device type to stop scanning for.
	 * 
	 * @throws CSCException
	 */
	public static void stopScanning(DeviceType deviceType) throws CSCException {
		switch (deviceType) {
		case ALL:
			if (BluetoothHelper.hasBLESupport())
				BLEDevice.stopDeviceScanning();
			BTClassicDevice.stopDeviceScanning();
			break;
		case BLE:
			if (BluetoothHelper.hasBLESupport())
				BLEDevice.stopDeviceScanning();
			else if (scanListner != null)
				scanListner.onScanStop();
			break;
		case BT_CLASSIC:
			BTClassicDevice.stopDeviceScanning();
			break;
		default:
			// Stop scanning is not required for SDIO just returning the method.
			return;

		}
	}

	/**
	 * This method returns Device Type based on selected device.
	 * 
	 * @return {@link #deviceType}.
	 */
	public DeviceType getDeviceType() {
		return deviceType;
	}

	/**
	 * This method sets device type of a {@link Device}.
	 * 
	 * @param deviceType
	 *            Type of device.
	 */
	public void setDeviceType(DeviceType devType) {
		deviceType = devType;
	}

	/**
	 * This method returns device information of the selected device.
	 * 
	 * @return Device details of the select {@link Device}.
	 */
	public abstract DeviceDetails getDeviceDetails();
}

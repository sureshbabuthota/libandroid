package com.tyfone.csc.sdio;

import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceCommand;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;
import com.tyfone.csc.device.ScanListener;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.helper.SDIOFinder;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * This class is derived from {@link Device} abstract class. This implementation
 * is specific to SDIO device.
 * 
 * It connects and communicates with a SDIO Device.
 * 
 * 
 * <p>
 * Connection status of a device can be fetched through
 * {@link #getConnectionStatus()}.
 * </p>
 * 
 * @see Device
 * 
 * 
 * @author Srikar created on 7 Oct 2014
 * 
 */
public class SDIODevice extends Device {

	private DeviceDetails mDeviceDetails;
	private CSCCallback registeredCallback;
	private SDIOCommunication sdioCommunication;

	/**
	 * Constructor to create an SDIODevice instance for a given
	 * {@link DeviceDetails}.
	 * 
	 * @param deviceDetails
	 *            {@link DeviceDetails} instance.
	 */
	public SDIODevice(DeviceDetails deviceDetails) {
		mDeviceDetails = deviceDetails;
	}

	/**
	 * This method checks for available SDIO device and sends device details
	 * through a callback when a device is found.
	 * 
	 * @throws CSCException
	 *             with {@link ErrorCodes}
	 * @see ScanListener#onDeviceFound(DeviceDetails)
	 */
	public static void getAvailableDevices() throws CSCException {
		findMountedSDIO();
	}

	/**
	 * This method checks for available SDIO device and sends device details
	 * through a callback when a device is found.
	 * 
	 * @throws CSCException
	 *             with {@link ErrorCodes}
	 * @see ScanListener#onDeviceFound(DeviceDetails)
	 */

	private static void findMountedSDIO() throws CSCException {
		String mountedPath = SDIOFinder.findConnectedSmartCard();
		DeviceDetails deviceDetails = new DeviceDetails(mountedPath,
				DeviceType.SD_IO);
		scanListner.onDeviceFound(deviceDetails);

	}

	@Override
	public void onReceiveResponse(byte[] response, ErrorCodes error) {
		registeredCallback.onReceiveResponse(response, error);
	}

	@Override
	public void onReceiveResponse(ResponseAPDU response, Applet applet,
			ErrorCodes error) {
		registeredCallback.onReceiveResponse(response, applet, error);
	}

	@Override
	public void onConnectionChange(Device device,
			DeviceConnectionState deviceState) {
		registeredCallback.onConnectionChange(device, deviceState);
	}

	@Override
	public void onUpdateRSSI(float value) {

	}

	@Override
	public void onFindAppletResponse(Smartcard smartcard, ErrorCodes errorCodes) {
		registeredCallback.onFindAppletResponse(smartcard, errorCodes);
	}

	@Override
	public void onSetAppletStatus(Applet applet, boolean selectedState) {
		registeredCallback.onSetAppletStatus(applet, selectedState);
	}

	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback cscCallback)
			throws CSCException {
		setDeviceType(deviceDetails.getDeviceType());
		registeredCallback = cscCallback;
		if (sdioCommunication == null)
			sdioCommunication = new SDIOCommunication(this);
		sdioCommunication.connect(deviceDetails, cscCallback);
	}

	@Override
	public void disconnect() {
		if (sdioCommunication != null)
			sdioCommunication.disconnect();
	}

	@Override
	public DeviceConnectionState getConnectionStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#sendCommand(byte[])
	 */
	@Override
	public byte[] sendCommand(byte[] commandBytes) throws CSCException {
		return sdioCommunication.write(commandBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#sendCommand(byte[],
	 * com.tyfone.csc.communication.CSCCallback)
	 */
	@Override
	public void sendCommand(byte[] commandBytes, ReadWriteCallback readWriteCallback) {

		sdioCommunication.write(commandBytes, readWriteCallback);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#enablePolling(java.lang.Boolean, int)
	 */
	@Override
	public int enablePolling(Boolean isPolling, int interval) {
		// TODO: Need to define use case.
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#getDeviceDetails()
	 */
	@Override
	public DeviceDetails getDeviceDetails() {
		return mDeviceDetails;
	}

	@Override
	public String sendCommand(DeviceCommand deviceCommand) throws CSCException {
		return sdioCommunication.sendcommand(deviceCommand);
	}

}

/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.sdio;

import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.Communication;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceCommand;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.ResponseAPDU;
//import com.tyfone.csc.log.Logger;

/**
 * Communication module implementation for the SDIO device. Used to connect and
 * communicate with the SDIO device.
 * 
 * <p>
 * Connection state of a device is notified through a registered
 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
 * </p>
 * 
 * <p>
 * 
 * Use {@link #write(byte[], CSCCallback)} method to write a command to a
 * {@link Device}. Response for this is returned through a callback
 * {@link CSCCallback#onReceiveResponse(ResponseAPDU, com.tyfone.csc.smartcard.Applet, ErrorCodes)}
 * </p>
 * <P>
 * Use {@link #write(byte[])} to get back the response as a return type.
 * 
 * </p>
 * 
 * 
 * @see SDIOCommunication#connect(DeviceDetails, CSCCallback)
 * @see DeviceConnectionState
 * @see CSCCallback
 * 
 * @author Srikar created on  Oct 7, 2014
 */

  class SDIOCommunication extends Communication {

	private SDIODevice mSdioDevice;
	private DeviceConnectionState mDeviceConnectionState;
	private boolean isPowerOn;
	private boolean isRFOn;

	public SDIOCommunication(SDIODevice sdioDevice) throws CSCException {
		this.mSdioDevice = sdioDevice;
		try {
			System.loadLibrary("cscdirectio");
		} catch (UnsatisfiedLinkError e) {
			throw new RuntimeException(new CSCException(
					ErrorCodes.ERROR_UNABLE_TO_LOAD_NATIVE_LIBRARY));
		} catch (Exception e) {
			throw new RuntimeException(new CSCException(
					ErrorCodes.ERROR_UNABLE_TO_LOAD_NATIVE_LIBRARY));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.tyfone.csc.communication.Communication#connect(com.tyfone.csc.device
	 * .DeviceDetails, com.tyfone.csc.communication.CSCCallback)
	 */
	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback cscCallback)
			throws CSCException {
		String mountedPath = deviceDetails.getAddress();
		byte[] sdCardPath = (mountedPath + "\0").getBytes();
		boolean initializeStatus = initializeCSC(sdCardPath);
		if (initializeStatus) {
			mDeviceConnectionState = DeviceConnectionState.CONNECTED;
		} else {
			mDeviceConnectionState = DeviceConnectionState.NOT_CONNECTED;

		}
		cscCallback.onConnectionChange(mSdioDevice, mDeviceConnectionState);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.Communication#disconnect()
	 */
	@Override
	public void disconnect() {
		try {
			if (mDeviceConnectionState == DeviceConnectionState.CONNECTED) {
				doPowerOff();
				doRFOff();
				finalizeCSC();
				mDeviceConnectionState = DeviceConnectionState.NOT_CONNECTED;
			}

		} catch (CSCException e) {
		}
	}

	/**
	 * This method will enable or disable device features like power on, RF on
	 * ,power off, RF off, get ATR etc. See {@link DeviceCommand} enum for
	 * command definitions.
	 * 
	 * @param deviceCommand
	 *            {@link DeviceCommand} value to enable or disable device
	 *            feature.
	 * @return value if any, else NULL.
	 * @throws CSCException
	 *             throws CSCException if an error occurs.
	 * @see DeviceCommand
	 */
	public String sendcommand(DeviceCommand deviceCommand) throws CSCException {
		switch (deviceCommand) {
		case POWER_ON:
			doPowerOn();
			break;
		case POWER_OFF:
			doPowerOff();
			break;
		case RF_ON:
			doRFOn();
			break;
		case RF_OFF:
			doRFOff();
			break;
		case GET_ATR:
			return getATR();
		case FW_VERSION:
			return getFirmwareVersion();

		default:
			break;
		}
		return null;
	}

	/**
	 * Method to switch on device RF.
	 * 
	 * @throws CSCException
	 */
	private void doRFOff() throws CSCException {
		if (isRFOn) {
			setRFOff();
			isRFOn = false;
		}
	}

	/**
	 * Method to switch off device RF.
	 * 
	 * @throws CSCException
	 */
	private void doRFOn() throws CSCException {
		setRFOn();
		isRFOn = true;
	}

	/**
	 * Method to power off device .
	 * 
	 * @throws CSCException
	 */
	private void doPowerOff() throws CSCException {
		if (isPowerOn) {
			setPowerOff();
			isPowerOn = false;
		}

	}

	/**
	 * Method to power on device.
	 * 
	 * @throws CSCException
	 */
	private void doPowerOn() throws CSCException {
		setPowerOn();
		isPowerOn = true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.Communication#write(byte[],
	 * com.tyfone.csc.communication.CSCCallback)
	 */
	@Override
	public void write(byte[] commandBytes, ReadWriteCallback cscCallback) {
		if (!isPowerOn) {
			// Device should be power on to write into.
			cscCallback.onReceiveResponse(null,
					ErrorCodes.ERROR_SDIO_NOT_POWER_ON);
			return;
		}
		if (isRFOn) {
			// Cannot write to a card if RF is on,throw an error;

			cscCallback.onReceiveResponse(null, ErrorCodes.ERROR_SDIO_RF_ON);
			return;
		}

		byte[] responseApdu = null;
		try {
			responseApdu = writeData(commandBytes);
			//Logger.info("Received response:", responseApdu);

		} catch (CSCException e) {
			/*Logger.error(e.getMessage());
			cscCallback
			.onReceiveResponse(responseApdu, e.what);*/
			return;
		}
		cscCallback
		.onReceiveResponse(responseApdu, ErrorCodes.SUCCESS_RESPONSE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.Communication#write(byte[])
	 */
	@Override
	public byte[] write(byte[] dataBytes) throws CSCException {
		if (!isPowerOn) {
			// Device should be power on to write into.
			throw new CSCException(ErrorCodes.ERROR_SDIO_NOT_POWER_ON);
		}
		if (isRFOn) {
			// Cannot write to a card if RF is on,throw an error;
			throw new CSCException(ErrorCodes.ERROR_SDIO_RF_ON);
		}

		byte[] responseApdu = null;
		responseApdu = writeData(dataBytes);
	//	Logger.info("Received response:", responseApdu);
		return responseApdu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.Communication#getConnectionState()
	 */
	@Override
	public DeviceConnectionState getConnectionState() throws CSCException {
		return mDeviceConnectionState;
	}

	/************************* Native_Methods *****************************************/
	/**
	 * Native method to initiaize the Connected SmartCard (CSC)
	 * 
	 * @param cscPath
	 *            The path to the CSC
	 * @return
	 * @throws CSCException
	 *             if initialize(byte[] cscPath) was unsuccessful in native due
	 *             to some reason
	 */
	private native boolean initializeCSC(byte[] cscPath) throws CSCException;

	/**
	 * Native method to get ATR of SDIO Card.
	 * 
	 * @return ATR value of SDIO card.
	 * @throws CSCException
	 *             if getATR() is unsuccessful.
	 */
	public native String getATR() throws CSCException;

	/**
	 * Native method to power on the Secure Element in Connected SmartCard
	 * (SDIO)
	 * 
	 * @throws CSCException
	 *             if setPowerOn() is unsuccessful.
	 */
	public native void setPowerOn() throws CSCException;

	/**
	 * Native method to power off the Secure Element in Connected SmartCard
	 * (SDIO)
	 * 
	 * @throws CSCException
	 *             if setPowerOff() is unsuccessful.
	 */
	public native void setPowerOff() throws CSCException;

	/**
	 * Native method to Activate RF(Contactless interface) in Connected
	 * SmartCard (SDIO).
	 * 
	 * @throws CSCException
	 *             if setRFOn() is unsuccessful.
	 */
	public native void setRFOn() throws CSCException;

	/**
	 * Native method to Deactivate RF (Contactless interface) of SDIO card.
	 * 
	 * @throws CSCException
	 *             if setRFOff() is unsuccessful.
	 */
	public native void setRFOff() throws CSCException;

	/**
	 * Native method to write data to the SDIO Card.
	 * 
	 * @param apdu
	 *            data that is to be written to SDIO.
	 * @return response received from card after writing the data
	 * @throws CSCException
	 *             if writeToCard(String apdu) was unsuccessful in native due to
	 *             some reason
	 */
	private native String writeToCard(String apdu) throws CSCException;

	/**
	 * Native method to write data bytes to the SDIO Card.
	 * 
	 * @param apduData
	 *            data bytes that is to be written to SDIO.
	 * @return byte[] response received from the card after writing the data.
	 * @throws CSCException
	 *             if writeToCard(byte[] apduData) was unsuccessful in native
	 *             due to some reason
	 */

	private native byte[] writeData(byte[] apduData) throws CSCException;

	/**
	 * To get the firmware version of Connected SmartCard (SDIO).
	 * 
	 * @return String firmware version of the card.
	 * @throws CSCException
	 *             if getFirmwareVersion() is unsuccessful.
	 */
	public native String getFirmwareVersion() throws CSCException;

	/**
	 * Native method to clean up the data.
	 * 
	 * @throws CSCException
	 *             if cleanup() was unsuccessful in native due to some reason
	 */
	private native void finalizeCSC() throws CSCException;

	/**
	 * Native method to read bytes from polling file. Polling or continuous file
	 * operation(read in this case) is done to keep the sdcard slot powered on.
	 * 
	 * @throws CSCException
	 *             if readFromPollingFile() was unsuccessful in native due to
	 *             some reason
	 */
	private native boolean readFromPollingFile() throws CSCException;

	/*******************************************************************************************/

}

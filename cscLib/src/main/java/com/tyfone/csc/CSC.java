/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For more information visit: www.tyfone.com 
 */
package com.tyfone.csc;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.tyfone.csc.ble.BLEDevice;
import com.tyfone.csc.bt.BTClassicDevice;
import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;
import com.tyfone.csc.device.ScanListener;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.log.CSCLogger;
import com.tyfone.csc.log.CSCLogger.CSCLoggerCallback;
import com.tyfone.csc.sdio.SDIODevice;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * <p>
 * 
 * This class provides APIs to connect to a {@link Device} and communicate with
 * a secure element ( {@link Smartcard} ) inside a connected device.
 * 
 * </p>
 * 
 * <p>
 * Requires {@link android.Manifest.permission#BLUETOOTH},
 * {@link android.Manifest.permission#BLUETOOTH_ADMIN},
 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE}.
 * </p>
 * 
 * 
 * <p>
 * Use {@link #getInstance(Context)} to get an instance of this class, which
 * internally initializes the CSC Library. Method {@link #finalizeCSC()} should
 * be called to release objects and to close existing connections with the list
 * of attached devices.
 * </p>
 * 
 * 
 * <p>
 * The following are the steps to connect and communicate with {@link Device}
 * and {@link Smartcard}.
 * <ol>
 * <li>Use {@link #getAvailableDevices(DeviceType, ScanListener)} method to get
 * available CSC devices ,which will send the devices information through a
 * callback {@link ScanListener#onDeviceFound(DeviceDetails)}.</li>
 * <li>Connect to the selected device by calling an API
 * {@link #connectToDevice(DeviceDetails, CSCCallback)}, then the status of the
 * device connection is notified through a callback
 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)} .</li>
 * <li>As CSC library supports connection to more than one {@link Device} at a
 * given time,use {@link CSC#getConnectedDevices()} method to get a list of
 * connected devices.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Use {@link #enableLogger(boolean, LogLevel, boolean)} method to enable or
 * disable logging in the library. Logging will happen only if the build is of
 * DEBUG mode.
 * </p>
 * 
 * 
 * <p>
 * Following is an example code.
 * </p>
 * 
 * 
 * <pre>
 * CSC csc = CSC.getInstance(getApplicationContext());
 * csc.getAvailableDevices(DeviceType.BT_CLASSIC, scanListner);
 * 
 * csc.setLogEnabled(true, LogLevel.INFO, true);
 * 
 * csc.setDevice(DeviceDetails, cscCallback);// Call this statement once received
 * // DeviceDetails in ScanListner
 * // callback
 * 
 * ScanListener scanListener = new ScanListener() {
 * 
 * 	&#064;Override
 * 	public void onScanStop() {
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onScanStart() {
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onDeviceFound(DeviceDetails onDeviceFound, int rssi) {
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onDeviceFound(DeviceDetails deviceDetails) {
 * 
 * 	}
 * };
 * 
 * CSCCallback mCSCCallback = new CSCCallback() {
 * 
 * 	&#064;Override
 * 	public void onUpdateRSSI(float value) {
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onSetAppletStatus(Applet applet, boolean selectedState) {
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onReceiveResponse(ResponseAPDU response, Applet applet,
 * 			ErrorCodes error) {
 * 		// Smartcard object is helpful to identify the responding Smartcard and
 * 		// Device when communicating with
 * 		// multiple smartcards and devices.
 * 		Smartcard smartcard = applet.getmSmartcard();
 * 
 * 		byte[] responseData = response.getmResponseAPDU().getBytes();
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onFindAppletResponse(Smartcard smartcard, ErrorCodes errorCodes) {
 * 
 * 		HashMap&lt;String, Boolean&gt; appletsStatusListOnSmartcard = smartcard
 * 				.getFindAppletResponseList();
 * 
 * 	}
 * 
 * 	&#064;Override
 * 	public void onConnectionChange(Device device,
 * 			DeviceConnectionState deviceState) {
 * 		if (deviceState == DeviceConnectionState.CONNECTED) {
 * 			List&lt;Smartcard&gt; smartCards = device.getAvailableCards();
 * 		}
 * 	}
 * };
 * </pre>
 * 
 * @author Suryaprakash and Srikar
 */

public class CSC {
	// TODO: Pradeep_Code_Review - Format Code.
	// TODO: Pradeep_Code_Review - More explanation of the variable UUID. No
	// hard coding
	// serial board connection.
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Holds selected DeviceDetails object.
	private DeviceDetails selectedDeviceDetails;

	// Holds the connected devices object details.
	private static HashMap<String, Device> connectedDevices;

	private static Context context;

	// CSC Singleton instance.
	private static CSC cscInstance;

	private static LogLevel logLevel = LogLevel.NO_LOGS;
	// Flag to determine weather application is acting like a slave or
	// master.Slave if true else master.
	private boolean isSlave;

	// Call back registered by the application to receive CSCCallback events.
	private CSCCallback registeredCallback;

	private final String TAG = CSC.class.getSimpleName();

	/**
	 * Create an instance of the CSC to access the Tyfone CSC Library.
	 * 
	 * @param context
	 *            The Context reference to be provided by the android
	 *            application that wants to use this library.
	 */
	private CSC(Context context) {
		CSC.context = context;
	}

	/**
	 * Gets an instance of the CSC to access the Tyfone CSCLibrary.
	 * 
	 * @param context
	 *            The Context reference to be provided by the android
	 *            application that wants to use this library.
	 * @return object of the CSC
	 */
	public static CSC getInstance(Context context) {
		synchronized (context) {

			if (cscInstance == null) {
				// Initializing Singleton CSC object.
				cscInstance = new CSC(context);

				// Initializing connected device list object.
				connectedDevices = new HashMap<String, Device>();

			}
		}

		return cscInstance;
	}

	/**
	 * This method accepts incoming connection request from tyfone BT classic
	 * device and sends status to {@link CSCCallback}.
	 * 
	 * @param cscCallback
	 *            A callback to receive device connection state.
	 */
	public void acceptIncomingConnection(CSCCallback cscCallback) {
		AcceptThread acceptThread = new AcceptThread(cscCallback);
		acceptThread.start();
	}

	/**
	 * <p>
	 * This method will get the available devices based on deviceType.
	 * </p>
	 * <p>
	 * If any device is found, it will be sent as a callback
	 * {@link ScanListener#onDeviceFound(DeviceDetails)}.
	 * </p>
	 * <p>
	 * <p>
	 * <strong> In case of Bluetooth devices</strong>
	 * </p>
	 * <p>
	 * 1. Requires {@link android.Manifest.permission#BLUETOOTH} and
	 * {@link android.Manifest.permission#BLUETOOTH_ADMIN} permissions. <br>
	 * 2. Make sure device bluetooth is ON before calling this API.</br>
	 * </p>
	 * <p>
	 * <p>
	 * 
	 * Require {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} in
	 * case of SDIO device.
	 * </p>
	 * 
	 * 
	 * @param deviceType
	 *            It will set the type of devices to scan for.
	 * @param scanListner
	 *            It is a callback to receive device details once remote device
	 *            is found.
	 * @return nothing
	 */
	// TODO: Pradeep_Code_Review - Remove unused commented code
	public void getAvailableDevices(final DeviceType deviceType,
			final ScanListener scanListner) throws CSCException {
		//CSCLogger.info(10002, TAG, TAG + "-getAvailableDevices()", null);
		// acceptIncomingConnection();

		// Returns the device details received from Device class
		if (scanListner == null || deviceType == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		else
			Device.getAvailableDevices(deviceType, scanListner);
	}

	/**
	 * <p>
	 * Method to instantiate and return {@link Device} based on
	 * {@link DeviceDetails#getDeviceType()}.
	 * </p>
	 * 
	 * <p>
	 * {@link Device} instance is sent to the application through a registered
	 * callback
	 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
	 * </p>
	 * 
	 * @param deviceDetails
	 *            Details of a device to connect and communicate to.
	 * @param cscCallback
	 *            A callback to receive device connection state.
	 * 
	 * @see CSCCallback#onConnectionChange(Device, DeviceConnectionState).
	 * @throws CSCException
	 *             throws a {@link CSCException} if connection to a device
	 *             fails.
	 */
	public void connectToDevice(final DeviceDetails deviceDetails,
			CSCCallback cscCallback) throws CSCException {
		CSCLogger.info(10003, TAG, TAG + "-connectToDevice()", null);
		// Check for the required params are valid or not
		if (deviceDetails == null || cscCallback == null) {
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		}
		// Sets the selected device with @param passed to this method
		this.registeredCallback = cscCallback;
		setSelectedDevice(deviceDetails);
		if (deviceDetails != null) {
			// Stopping BLE scan
			Device.stopScanning(deviceDetails.getDeviceType());

			// Set Device connection here based on DeviceDetails
			connectDevice(deviceDetails, mCSCCallback);
		}
	}

	/**
	 * To set the selected device details.
	 * 
	 * @param deviceDetails
	 *            Details of a selected device to set.
	 */
	private void setSelectedDevice(DeviceDetails deviceDetails) {
		selectedDeviceDetails = deviceDetails;
	}

	/**
	 * <p>
	 * This method will establish connection with device based on
	 * {@link DeviceDetails} and instantiate and returns {@link Device} based on
	 * {@link DeviceDetails#getDeviceType()}. For Example if the
	 * 
	 * {@link Device#getDeviceType()} is {@link DeviceType#BLE}, it will
	 * establish connection using {@link DeviceDetails#getAddress()}.
	 * </p>
	 * 
	 * @param deviceDetails
	 *            Device details to connect and communicate.
	 * @param cscCallback
	 *            A callback to receive device connection state and
	 *            communication information.
	 * 
	 * @throws CSCException
	 */
	private synchronized void connectDevice(DeviceDetails deviceDetails,
			CSCCallback cscCallback) throws CSCException {

		// Check for null objects
		if (deviceDetails == null || cscCallback == null) {
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		}

		Device device = null;
		// Instantiate the suitable object based on device type from
		// DeviceDetails
		switch (deviceDetails.getDeviceType()) {
		case BT_CLASSIC:
			// Check for this device already connected or not
			if (!isConnectedDevice(deviceDetails)) {
				device = new BTClassicDevice(deviceDetails);
				// TODO: Pradeep_Code_Review - 4 connect function abstractions
				// to finally connect. Hmmm....
				device.connect(deviceDetails, cscCallback);
			}
			break;

		case BLE:
			// Check for this device already connected or not
			if (!isConnectedDevice(deviceDetails)) {
				device = new BLEDevice(deviceDetails);
				device.connect(deviceDetails, cscCallback);
			} else {
				// Logger.info("Device already connected. Using same connection for further  communication.");

			}
			break;

		case SD_IO:
			if (!isConnectedDevice(deviceDetails)) {
				device = new SDIODevice(deviceDetails);
				device.connect(deviceDetails, cscCallback);
			} else {
				// Logger.info("Device already connected. Using same connection for further  communication.");

			}

			break;

		default:
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		}
	}

	/**
	 * This Method checks for {@link Device} with the input
	 * {@link DeviceDetails} is already Connected. Return true if Connected.
	 * <p>
	 * This method is used for checking the device connection is already exist
	 * or not. If device connection already there no need to create new
	 * connection. Application can used the existing connected device object for
	 * further communication
	 * </p>
	 * 
	 * @param deviceDetails
	 *            {@link DeviceDetails} to check the Connection status.
	 * 
	 * @return boolean True if connected.
	 * 
	 */
	private boolean isConnectedDevice(DeviceDetails deviceDetails) {
		boolean isConnected = true;
		// Get the device from connected devices
		Device device = connectedDevices.get(deviceDetails.getAddress());
		//if (device == null)
		//;// Logger.info("Device null on isConnectedDevice");
		//else
		// Logger.info("CSC:isConnectedDevice; get device status:"
		// + device.getConnectionStatus());
		// If device not null and status not connected, send status
		// isConencted=false; because if connection is not established already
		// need to establish new connection for the device
		if (device == null
				|| device.getConnectionStatus() == DeviceConnectionState.NOT_CONNECTED
				|| device.getConnectionStatus() == DeviceConnectionState.ERROR_IN_CONNECTION)
			isConnected = false;
		else {
			// Device has already connected and send the connected status to
			// application level. Here it won't be create new connected object.
			// it will communicate with existing connected device object
			registeredCallback.onConnectionChange(device,
					device.getConnectionStatus());
		}
		return isConnected;
	}

	/**
	 * This method returns {@link Collection} of connected CSC {@link Device}
	 * instances.
	 * 
	 * @return {@link Collection} of connected {@link Device}.
	 */
	public HashMap<String, Device> getConnectedDevices() {
		return connectedDevices;
	}

	/**
	 * Sets the CSCLogger to get logs depending on the log level.
	 * 
	 * @param callBack
	 *            Callback to receive logs
	 * @param logLevel
	 *            the library log level to display different types of
	 *            information.
	 * @throws CSCException
	 *             if the CSCLoggerCallback is null.
	 * 
	 */
	public void setupCSCLogger(final CSCLoggerCallback callBack,
			final LogLevel logLevel) throws CSCException {
		if (callBack != null) {
			CSCLogger.setCSCLoggerCallback(callBack);
		} else {
			throw new CSCException(ErrorCodes.ERROR_NULL_CSCLOGGER);
		}
		if (logLevel == null) {
			CSC.logLevel = LogLevel.NO_LOGS;
		} else {
			CSC.logLevel = logLevel;
		}
	}

	/**
	 * Sets the logLevel.
	 * 
	 * @param logLevel
	 *            the library log level to display different types of
	 *            information.
	 */
	public void setLogLevel(final LogLevel logLevel) {
		if (logLevel == null) {
			CSC.logLevel = LogLevel.NO_LOGS;
		} else {
			CSC.logLevel = logLevel;
		}
	}

	/**
	 * This method returns CSC library {@link #LogLevel}
	 * 
	 * @return log level of CSC library.
	 */
	public static LogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * 
	 * TODO:This method will return CSC library version.
	 * 
	 * @return library version
	 */
	public String getLibVersion(Context context) {
		// TODO get Library version implementation
		return null;
	}

	/**
	 * This method will stop scanning for BLE devices.
	 * 
	 * @throws CSCException
	 *             throws {@link CSCException} with error codes.
	 */
	public void stopScanning() throws CSCException {
		Device.stopScanning(DeviceType.BLE);
	}

	/**
	 * TODO: This method will set CSC library properties.
	 */
	public void setProperties() {
		// TODO
	}

	/**
	 * TODO: This method will return CSC library properties.
	 * 
	 * @return
	 */
	public Collection<?> getProperties() {
		// TODO
		return null;
	}

	/**
	 * This method finalizes the CSC class and clears all configurations and
	 * connections.
	 * 
	 * @throws CSCException
	 */
	public void finalizeCSC() throws CSCException {
		CSCLogger.info(10001, TAG, TAG + "-finalizeCSC()", null);
		// Releasing the objects
		Device.stopScanning(DeviceType.ALL);
		selectedDeviceDetails = null;
		connectedDevices.clear();
		return;// Returns finalization status
	}

	/**
	 * This method returns the context passed in {@link #getInstance(Context)}
	 * call.
	 * 
	 * @return context sets to library.
	 */
	public static Context getContext() {
		// It will return CSC context
		return CSC.context;
	}

	/**
	 * This callback sends to the communication to track communication and
	 * connection state.
	 */
	private CSCCallback mCSCCallback = new CSCCallback() {

		private DeviceDetails deviceDetails;

		@Override
		public void onUpdateRSSI(float value) {
			// send the updated RSSI value to Application level
			registeredCallback.onUpdateRSSI(value);
		}

		@Override
		public void onReceiveResponse(ResponseAPDU response, Applet applet,
				ErrorCodes errorCodes) {
			// send the receive response to Application level
			registeredCallback.onReceiveResponse(response, applet, errorCodes);

		}

		@Override
		public void onReceiveResponse(byte[] response, ErrorCodes error) {
			registeredCallback.onReceiveResponse(response, error);

		}

		@Override
		public void onConnectionChange(Device device,
				DeviceConnectionState deviceState) {
			// Logger.info("CSC: onConnectionChange" + deviceState.toString());
			// Check for device connection state
			if (deviceState == DeviceConnectionState.NOT_CONNECTED) {
				if (connectedDevices != null
						&& connectedDevices.containsKey(device
								.getDeviceDetails()))
					// If disconnected remove from the connectedDevices object
				{

					deviceDetails = device.getDeviceDetails();
					if (!deviceDetails.isSlave()) {
						// Application is no more acting like a slave.
						isSlave = false;
					}
					connectedDevices.remove(deviceDetails);
				}

				registeredCallback.onConnectionChange(device, deviceState);
				// if()

				// Logger.info("Removed connected device");
			} else {
				if (connectedDevices == null)
					connectedDevices = new HashMap<String, Device>();
				// Once connected add to connectedDevices object
				connectedDevices.put(device.getDeviceDetails().getAddress(),
						device);
				// Logger.info("Added connected device" +
				// connectedDevices.size());
				// Send connection change status to application level
				registeredCallback.onConnectionChange(device, deviceState);
			}
		}

		@Override
		public void onFindAppletResponse(Smartcard smartcard,
				ErrorCodes errorCodes) {
			registeredCallback.onFindAppletResponse(smartcard, errorCodes);
		}

		@Override
		public void onSetAppletStatus(Applet applet, boolean selectedState) {
			registeredCallback.onSetAppletStatus(applet, selectedState);
		}

	};

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mServerSocket;
		private final CSCCallback mCSCallback;

		public AcceptThread(CSCCallback cscCallback) {
			mCSCallback = cscCallback;
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {

				tmp = BluetoothAdapter.getDefaultAdapter()
						.listenUsingRfcommWithServiceRecord("CSCLibra",
								SPP_UUID);
			} catch (IOException e) {
			}
			mServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (isSlave) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mServerSocket.accept();
				} catch (IOException e) {
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (CSC.this) {
						isSlave = true;
						BluetoothDevice device = socket.getRemoteDevice();
						String address = device.getAddress();
						// String name=device.getName();
						DeviceDetails deviceDetails = new DeviceDetails(
								address, DeviceType.BT_CLASSIC, false);
						new BTClassicDevice(deviceDetails, socket, mCSCallback);

					}
				}
			}
		}

	}
}

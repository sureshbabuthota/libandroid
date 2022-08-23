/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.ble;

import java.util.HashMap;
import java.util.Set;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.SparseArray;

import com.tyfone.csc.CSC;
import com.tyfone.csc.LogLevel;
import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceCommand;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;
import com.tyfone.csc.device.ScanListener;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.BluetoothHelper;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.resource.ResourceProperties;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;
//import com.tyfone.csc.log.Logger;

/**
 * <p>
 * This class is derived from {@link Device} abstract class. This implementation
 * is specific to Bluetooth Low Energy (BLE).
 * </p>
 * 
 * <p>
 * Requires Android API level 18 {@link Build.VERSION_CODES#JELLY_BEAN_MR2}.
 * </p>
 * 
 * It connects and communicates with a BLE Device.
 * 
 * <p>
 * This class fetches available devices. It connects to a specific device based
 * on {@link DeviceDetails#getAddress()}.
 * </p>
 * 
 * <p>
 * This also provide the BLE device connection status and list of smart cards
 * available in it.
 * </p>
 * 
 * 
 * @author <b>suryaprakash</b> on Jul 30, 2014 1:57:23 PM </br>
 */
@SuppressLint("NewApi")
public final class BLEDevice extends Device {

	private final String TAG = "BLE Device";

	private BLECommunication bleCommunication;

	private DeviceDetails mDeviceDetails;

	private static SparseArray<BluetoothDevice> bluetoothDevices;

	private CSCCallback registeredCallback;

	/**
	 * This sets device details.
	 * 
	 * @param deviceDetails
	 *            sets device details.
	 */
	public BLEDevice(final DeviceDetails deviceDetails) {
		super();
		mDeviceDetails = deviceDetails;
	}

	/**
	 * This method sends all the paired and scanned devices to application using
	 * {@link ScanListener}.
	 * 
	 * @throws CSCException
	 *             with {@link ErrorCodes}
	 */
	public static void getAvailableDevices() throws CSCException {
		scanForDevices();
	}

	/**
	 * This method will fetch all the available BLE devices.
	 * 
	 * @throws CSCException
	 *             through errors {@link ErrorCodes}
	 * 
	 */
	private static void scanForDevices() throws CSCException {

		// Initialize ble adapter
		BluetoothAdapter mBluetoothAdapter = BluetoothHelper.getBleAdapter();
		BluetoothHelper.checkBluetoothStatus(mBluetoothAdapter);
		// Getting all the bonded devices using bluetooth adapter
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		if (pairedDevices == null) {
			throw new CSCException(ErrorCodes.DEVICE_BT_ERR_ON_PAIRED_DEVICES);
		}

		// If any bonded devices send DeviceDetails by callback
		if (pairedDevices.size() > 0 && scanListner != null) {
			// Form DeviceDetails object from BluetoothDevice for further
			// use and send it to ScanListner callback
			for (BluetoothDevice bluetoothDevice : pairedDevices) {
				if (BluetoothHelper.isSidekey(
						bluetoothDevice,
						ResourceProperties.getInstance().getProperty(
								"MAC_ADDRESS_PREFIX_BLE"))) {
					// Send the device found callback to application level
					// with @param Device object
					scanListner.onDeviceFound(BluetoothHelper.getDeviceDetails(
							bluetoothDevice, DeviceType.BLE));
				}
			}
		}

		// Initialize scanning BLE devices along with ScanListner Callback
		if (scanListner == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);

		// Initialize BLE scanning
		initializeScanning(CSC.getContext(), mBluetoothAdapter);
	}

	/**
	 * <p>
	 * This method will initialize BLE scan.
	 * </p>
	 * 
	 * @param context
	 * @param mBluetoothAdapter
	 */
	private static void initializeScanning(final Context context,
			BluetoothAdapter mBluetoothAdapter) {
		bluetoothDevices = new SparseArray<BluetoothDevice>();
		// Sending callback to application that BLE scan started
		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.startLeScan(bleScanCallback);
		} else {
			// If bluetooth not available or turn off sending stop scanning
			// callback
			scanListner.onScanStop();
		}
	}

	/**
	 * This method stops scanning for BLE devices.
	 * 
	 * @throws {@link CSCException}
	 */
	public static void stopDeviceScanning() throws CSCException {
		BluetoothAdapter bluetoothAdapter = BluetoothHelper.getBleAdapter();
		if (bleScanCallback != null && bluetoothAdapter.isEnabled())
			bluetoothAdapter.stopLeScan(bleScanCallback);
		else
			throw new CSCException(ErrorCodes.DEVICE_BT_OFF);
		if (scanListner != null) {
			scanListner.onScanStop();
		}
	}

	/**
	 * <p>
	 * This class will receive BLE device while scanning and also receives RSSI
	 * value.
	 * 
	 * Created by @author <b>suryaprakash</b> on Aug 5, 2014 11:19:22 AM </br>
	 * Project: CSCLibrary </br>
	 * 
	 */
	private static LeScanCallback bleScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice bluetoothDevice, int rssi,
				byte[] scanRecord) {
			// On BLE device discovered setting the DeviceDetails
			if (scanListner != null
					&& bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE
					&& bluetoothDevice.getAddress().contains(
							ResourceProperties.getInstance().getProperty(
									"MAC_ADDRESS_PREFIX_BLE"))) {
				if (bluetoothDevices.get(bluetoothDevice.hashCode()) == null) {
					bluetoothDevices.put(bluetoothDevice.hashCode(),
							bluetoothDevice);
					// Sending callback to application with device
					// details
					DeviceDetails deviceDetails = BluetoothHelper
							.getDeviceDetails(bluetoothDevice, DeviceType.BLE);
					scanListner.onDeviceFound(deviceDetails);

					// Show device info log in Debug mode or setLogEnable(true)
					if (CSC.getLogLevel()==LogLevel.DEBUG) {
						HashMap<String, String> loglist = new HashMap<String, String>();
						loglist.put("Device Name", deviceDetails.getName());
						loglist.put("Device Address",
								deviceDetails.getAddress());
						loglist.put("Device Status", deviceDetails.getStatus()
								.toString());
						loglist.put("Device Type", deviceDetails
								.getDeviceType().toString());
						//Logger.info(loglist.toString());
						loglist = null;
					}
				} else {
					// TODO
					// Sending callback to application with RSSI
					// details
					// scanListner.onRSSIReceive(rssi);
				}
			}
		}
	};

	/**
	 * Connects to BLE device based on {@link DeviceDetails}. The
	 * {@link CSCCallback} sends results to application, such as connection
	 * status as well as response for write operations.
	 * 
	 * @see Device#connect(DeviceDetails, CSCCallback)
	 */
	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback cscCallback)
			throws CSCException {
		setDeviceType(deviceDetails.getDeviceType());
		registeredCallback = cscCallback;
		// Instantiate BLE Communication object here to connect BLE device.
		if (bleCommunication == null) {
			bleCommunication = new BLECommunication(this);
			bleCommunication.connect(deviceDetails, cscCallback);
		}
		return;
	}

	/*
	 * This method is to disconnect the BLE device and terminate the
	 * communication channel.
	 * 
	 * @see com.tyfone.csc.device.Device#disconnect()
	 */
	@Override
	public void disconnect() {
		if (bleCommunication != null)
			try {
				bleCommunication.disconnect();
			} catch (CSCException e) {
				//Logger.error(TAG, e.getMessage());
			}
		return;
	}

	/*
	 * This method enables polling option with particular interval to check the
	 * BLE device status.
	 * 
	 * @see com.tyfone.csc.device.Device#enablePolling(java.lang.Boolean, int)
	 */
	@Override
	public int enablePolling(Boolean isPolling, int interval) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * Get the current connection state of the remote device.
	 * 
	 * @see com.tyfone.csc.device.Device#isConnected()
	 */
	@Override
	public DeviceConnectionState getConnectionStatus() {
		if (bleCommunication != null) {
			return bleCommunication.getConnectionState();
		}
		return DeviceConnectionState.NOT_CONNECTED;
	}

	/*
	 * This method will return device details of {@link BLEDevice}.
	 * 
	 * @see com.tyfone.csc.device.Device#getDeviceDetails()
	 */
	@Override
	public DeviceDetails getDeviceDetails() {
		// TODO Auto-generated method stub
		return this.mDeviceDetails;
	}

	@Override
	public byte[] sendCommand(final byte[] commandBytes) throws CSCException {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			return bleCommunication.write(commandBytes);
		} else {
			throw new CSCException(
					ErrorCodes.DEVICE_BLE_SYNC_WRITE_ON_MAIN_THREAD);
		}
	}

	/*
	 * This method is used for sending {@link DeviceCommands} for knowing device
	 * status.
	 * 
	 * @see
	 * com.tyfone.csc.device.Device#sendCommand(com.tyfone.csc.communication
	 * .DeviceCommands )
	 */
	@Override
	public void sendCommand(final byte[] commandBytes,
			final ReadWriteCallback readWriteCallback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					bleCommunication.write(commandBytes, readWriteCallback);
				} catch (CSCException e) {
					//Logger.error(TAG, e.getMessage());
				}
			}
		}).start();
	}

	@Override
	public void onReceiveResponse(ResponseAPDU response, Applet applet,
			ErrorCodes errorCodes) {
		registeredCallback.onReceiveResponse(response, applet, errorCodes);
	}

	@Override
	public void onReceiveResponse(byte[] response, ErrorCodes error) {
		registeredCallback.onReceiveResponse(response, error);

	}

	@Override
	public void onConnectionChange(Device device,
			DeviceConnectionState deviceState) {
		registeredCallback.onConnectionChange(device, deviceState);

	}

	@Override
	public void onUpdateRSSI(float value) {
		// TODO
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
	public String sendCommand(DeviceCommand deviceCommand) throws CSCException {
		// TODO Auto-generated method stub
		return null;
	}

}

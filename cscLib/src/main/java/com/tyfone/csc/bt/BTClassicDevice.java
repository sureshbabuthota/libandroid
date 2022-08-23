/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc.bt;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.tyfone.csc.CSC;
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
 * 
 * 
 * This class is derived from {@link Device} abstract class. This implementation
 * is specific to Bluetooth classic (BT).
 * 
 * It connects and communicates with a BTClassic Device.
 * 
 * <p>
 * This class fetches available and paired devices. It connects to a specific
 * device based on {@link DeviceDetails#getAddress()}.
 * </p>
 * 
 * 
 * <p>
 * Connection status of a device can be fetched through
 * {@link #getConnectionStatus()}.
 * </p>
 * 
 * @see Device
 * 
 * @author Srikar created on Jul 30, 2014
 * 
 */
public class BTClassicDevice extends Device {
	// TODO This should be a configuration parameter ,application can decide how
	// long the delay should be after connection.

	/**
	 * This can be overlooked if there is a delay in between connecting and
	 * communicating to a BTClassic device. For example if application is using
	 * a event(button event) to send command immediately after connection ,than
	 * delay is not required.
	 */
	private static final int DELAY_AFTER_CONNECTION = 1000;

	private static BluetoothAdapter mBtAdapter;
	/**
	 * {@link DeviceDetails} of this device.
	 */
	private DeviceDetails mDeviceDetails;

	private boolean isMaster;

	/**
	 * Constructor to create a {@link BTClassicDevice} instance with the given
	 * {@link DeviceDetails} object.
	 * 
	 * @param deviceDetails
	 *            {@link DeviceDetails} of the device.
	 */
	public BTClassicDevice(final DeviceDetails deviceDetails) {
		super();
		mDeviceDetails = deviceDetails;
	}

	/**
	 * Constructor to create a {@link BTClassicDevice} instance with the given
	 * {@link DeviceDetails} object.
	 * 
	 * @param deviceDetails
	 *            {@link DeviceDetails} of this device.
	 * @param mBluetoothSocket
	 *            bluetooth socket object.
	 * @param cscCallback
	 *            A callback to receive device connection state and apdu command
	 *            response.
	 */
	public BTClassicDevice(final DeviceDetails deviceDetails,
			final BluetoothSocket mBluetoothSocket,
			final CSCCallback cscCallback) {
		super();
		mDeviceDetails = deviceDetails;
		registeredCallback = cscCallback;
		btClassicCommunication = new BTClassicCommunication(this,
				mBluetoothSocket);
		isMaster = true;

	}

	/**
	 * This method scans for all the available BT classic(Paired and Non-Paired)
	 * devices and sends device details through a callback when a device is
	 * found.
	 * 
	 * @throws CSCException
	 *             with {@link ErrorCodes}
	 * @see ScanListener#onDeviceFound(DeviceDetails)
	 */
	public static void getAvailableDevices() throws CSCException {

		scanForDevices();

	}

	/**
	 * This method will scan for all the available BT classic(Paired and
	 * Non-Paired) devices and send device details through a callback when a
	 * device is found.
	 * 
	 * @throws CSCException
	 *             through errors {@link ErrorCodes}
	 * @see ScanListener#onDeviceFound(DeviceDetails)
	 */
	private static void scanForDevices() throws CSCException {
		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		// Register for broadcasts when discovery has finished
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

		CSC.getContext().registerReceiver(mReceiver, filter);

		// Get the local Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothHelper.checkBluetoothStatus(mBtAdapter);
		Set<BluetoothDevice> pairedDevices = null;

		// Get a set of currently paired devices

		pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices != null) {
			if (!pairedDevices.isEmpty()) {

				for (BluetoothDevice device : pairedDevices) {
					if (BluetoothHelper.isSidekey(
							device,
							ResourceProperties.getInstance().getProperty(
									"MAC_ADDRESS_PREFIX_BTCLASSSIC")))
						scanListner
								.onDeviceFound(BluetoothHelper
										.getDeviceDetails(device,
												DeviceType.BT_CLASSIC));
				}
			} else {
				// TODO If no device is paired.
			}
		}

		// Discovering the available unpaired devices.
		doDiscovery();

	}

	// The BroadcastReceiver that listens for discovered devices and

	private static final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED
						&& BluetoothHelper.isSidekey(
								device,
								ResourceProperties.getInstance().getProperty(
										"MAC_ADDRESS_PREFIX_BTCLASSSIC"))) {

					scanListner.onDeviceFound(BluetoothHelper.getDeviceDetails(
							device, DeviceType.BT_CLASSIC));
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				// TODO Notify to application
				// Unregistering the registered receiver
				CSC.getContext().unregisterReceiver(mReceiver);

			}
		}
	};

	private static final String TAG = "BTClassic Device";

	/**
	 * Start device discover with the BluetoothAdapter.
	 */
	private static void doDiscovery() {

		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}

	private BTClassicCommunication btClassicCommunication;

	private CSCCallback registeredCallback;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#connect()
	 */
	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback btCallback)
			throws CSCException {
		setDeviceType(deviceDetails.getDeviceType());
		registeredCallback = btCallback;
		if (btClassicCommunication == null)
			btClassicCommunication = new BTClassicCommunication(this);
		btClassicCommunication.connect(deviceDetails, btCallback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#disconnect()
	 */
	@Override
	public void disconnect() {
		if (btClassicCommunication != null)
			btClassicCommunication.disconnect();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.tyfone.csc.device.Device#sendCommand(com.tyfone.csc.comm.DeviceCommands
	 * )
	 */
	@Override
	public void sendCommand(byte[] commandBytes, ReadWriteCallback readWriteCallback) {
		try {

			btClassicCommunication.write(commandBytes, readWriteCallback);
		} catch (CSCException e) {
			//Logger.error(TAG, e.getMessage());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#enablePolling(java.lang.Boolean, int)
	 */
	// TODO : Need to discuss the scenario and how to implement.
	@Override
	public int enablePolling(Boolean isPolling, int interval) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * This method will unregister remote devices scanning.
	 * 
	 * @throws CSCException
	 */
	public static void stopDeviceScanning() throws CSCException {
		try {
			CSC.getContext().unregisterReceiver(mReceiver);
		} catch (IllegalArgumentException e) {
			// This exception occurs when receiver is already unregistered.
			// Work around needed.
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.device.Device#getDeviceDetails()
	 */
	// Ainiya_Code_Review - add proper javadoc in parent, describe/link return
	// type
	@Override
	public DeviceDetails getDeviceDetails() {
		// TODO Auto-generated method stub
		return mDeviceDetails;
	}

	// Ainiya_Code_Review - add proper javadoc in parent, describe/link return
	// type
	@Override
	public DeviceConnectionState getConnectionStatus() {
		// TODO Auto-generated method stub
		return btClassicCommunication.getConnectionState();
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
		try {
			// if(device.getDeviceType()==DeviceType.BT_CLASSIC)
			// For BTClassic there should be a delay in between connection and
			// communication.
			Thread.sleep(DELAY_AFTER_CONNECTION);
		} catch (InterruptedException e) {
			//Logger.error(TAG, e.getMessage());
		}
		registeredCallback.onConnectionChange(device, deviceState);

	}

	@Override
	public void onUpdateRSSI(float value) {
		// TODO Auto-generated method stub

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
	public byte[] sendCommand(byte[] commandBytes) throws CSCException {
		return btClassicCommunication.write(commandBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.tyfone.csc.device.Device#sendCommand(com.tyfone.csc.device.DeviceCommand
	 * ) TODO: Need to implement this for Bluetooth devices.
	 */
	@Override
	public String sendCommand(DeviceCommand deviceCommand) throws CSCException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns true if the device acts as master otherwise false.
	 * 
	 * @return device acts as master or slave.
	 */
	public boolean isMaster() {
		return isMaster;
	}
}

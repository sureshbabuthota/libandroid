/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.helper;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import com.tyfone.csc.CSC;
import com.tyfone.csc.device.DeviceBondState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;
import com.tyfone.csc.exception.CSCException;

/**
 * This class provides helper methods for bluetooth module.
 * 
 * 
 * @author Srikar created on  Aug 6, 2014
 */
public class BluetoothHelper {

	private static BluetoothAdapter bleAdapter;

	/**
	 * Checks Bluetooth status such as OFF, not supported, no permissions etc.
	 * 
	 * @param bluetoothAdapter
	 *            instance of {@link BluetoothAdapter}.
	 * @throws CSCException
	 */
	public static void checkBluetoothStatus(
			final BluetoothAdapter bluetoothAdapter) throws CSCException {

		try {
			if (bluetoothAdapter == null) {
				// Bluetooth not supported
				throw new CSCException(ErrorCodes.DEVICE_BT_NOT_SUPPORTED);
			} else if (!bluetoothAdapter.isEnabled()) {
				// Bluetooth Disabled, turn on bluetooth
				throw new CSCException(ErrorCodes.DEVICE_BT_OFF);
			}

		} catch (SecurityException e) {
			// This exception occurs if bluetooth permissions are not specified
			// in manifest.
			throw new CSCException(ErrorCodes.DEVICE_BT_NOT_AUTHERISED);
		}

	}

	/**
	 * Returns the constructed {@link DeviceDetails} object for a given
	 * {@link BluetoothDevice} input.
	 * 
	 * @param device
	 *            received {@link BluetoothDevice} from system.
	 * @param deviceType
	 *            type of device {@link DeviceType}.
	 * 
	 * @return device details object, else NULL.
	 */
	public static DeviceDetails getDeviceDetails(BluetoothDevice device,
			DeviceType deviceType) {
		if (device == null)
			return null;
		DeviceDetails deviceDetails;
		deviceDetails = new DeviceDetails();
		deviceDetails.setDeviceType(deviceType);
		deviceDetails.setAddress(device.getAddress());
		deviceDetails.setName(device.getName());
		deviceDetails.setStatus(getDeviceBondState(device.getBondState()));
		return deviceDetails;

	}

	/**
	 * Gets a {@link BluetoothDevice} object for the given Bluetooth hardware
	 * address.
	 * <p>
	 * Valid Bluetooth hardware addresses must be upper case, in the format "00:11:22:33:AA:BB". 
	 * <p>
	 * A {@link BluetoothDevice} will always be returned for a valid hardware
	 * address, even if this adapter has never seen that device.
	 *
	 * @param macAddress
	 *            valid Bluetooth MAC address
	 * @throws IllegalArgumentException
	 *             if address is invalid
	 */

	public static BluetoothDevice getDeviceByMacAddress(String macAddress) {

		return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
	}

	/**
	 * Return {@link #DeviceBondState} based on the Bond state of
	 * {@link #BluetoothDevice}.
	 * 
	 * @param bondstate
	 *            device bond state.
	 * @return {@link #DeviceBondState}
	 */
	private static DeviceBondState getDeviceBondState(int bondstate) {
		switch (bondstate) {
		case BluetoothDevice.BOND_BONDED:
			return DeviceBondState.BOND_BONDED;
		case BluetoothDevice.BOND_BONDING:
			return DeviceBondState.BOND_BONDING;
		case BluetoothDevice.BOND_NONE:
			return DeviceBondState.BOND_NONE;
		default:
			return DeviceBondState.BOND_NONE;
		}
	}

	/**
	 * This method returns the bluetooth adapter object for low enery device.
	 * 
	 * @param context
	 * 
	 * @return bluetooth Adapter object
	 * @throws CSCException
	 */
	@SuppressLint("NewApi")
	public static BluetoothAdapter getBleAdapter() {
		// Initializing BluetoothManager
		if (bleAdapter == null) {
			bluetoothManager = getBleManager();
			if (bluetoothManager != null)
				bleAdapter = bluetoothManager.getAdapter();
		}
		return bleAdapter;
	}

	private static BluetoothManager bluetoothManager;

	/**
	 * This method returns bluetooth manager for low energy device.
	 * 
	 * @return {@link BluetoothManager} instance.
	 */
	@SuppressLint("InlinedApi")
	public static BluetoothManager getBleManager() {
		if (bluetoothManager == null)
			bluetoothManager = (BluetoothManager) CSC.getContext()
					.getApplicationContext()
					.getSystemService(Context.BLUETOOTH_SERVICE);
		return bluetoothManager;
	}

	/**
	 * This method checks for a device whose MAC address is starting with 00:06:66
	 * in case of BTClassic and 1C:BA:8C in case of BLE.
	 * 
	 * <p>
	 * Example in 00:06:66:46:8B:1F first three bytes represent a vendor, in this
	 * case which is 00:06:66 of Roving Networks.
	 * 
	 * 
	 * @param device
	 *            {@link BluetoothDevice} to check.
	 * @param macAddress
	 *            first 8 characters of the device mac address.
	 * 
	 * @return true if SideKey.
	 */
	public static boolean isSidekey(BluetoothDevice device, String macAddress) {
		String bluetoothAddress = device.getAddress();
		if (bluetoothAddress.length() > 8 && bluetoothAddress.substring(0, 8).equals(macAddress))
				return true;

		return false;
	}

	/**
	 * Checks for Bluetooth LE support.
	 */
	public static boolean hasBLESupport() {
		if (CSC.getContext().getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			return true;
		else
			return false;
	}

}

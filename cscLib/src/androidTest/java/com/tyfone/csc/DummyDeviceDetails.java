/**
 * 
 */
package com.tyfone.csc;

import com.tyfone.csc.device.DeviceBondState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;

/**
 * Created by author <b>suryaprakash</b> on Sep 11, 2014 11:28:39 AM </br>
 * Project: CSCLibraryJUnitTest </br>
 *
 */
public class DummyDeviceDetails {

	public static DeviceDetails getDeviceDetails() {
		DeviceDetails deviceDetails = new DeviceDetails();
		deviceDetails.setAddress("1C:BA:8C:20:F7:A8");
		deviceDetails.setDeviceType(DeviceType.BLE);
		deviceDetails.setName("TyfoneDummy");
		deviceDetails.setStatus(DeviceBondState.BOND_BONDED);
		return deviceDetails;
	}
}

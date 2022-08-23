/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.helper;

import com.tyfone.csc.resource.ResourceProperties;

/**
 * This class contains all possible error types and error codes in the
 * library. This method will fetch the relevant error message based on error
 * key. The error key holding the module information, severity and error code of
 * Particular exception.
 * 
 * @author <b>suryaprakash</b> on Aug 1, 2014 12:12:48 PM </br> Project:
 *         CSCLibrary </br>
 *
 */
/**
 * Created by author <b>suryaprakash</b> on Aug 26, 2014 12:30:42 PM
 * </br>
 * Project: CSCLibrary
 * </br>
 *
 */
/**
 * Created by author <b>suryaprakash</b> on Aug 26, 2014 12:30:45 PM </br>
 * Project: CSCLibrary </br>
 *
 */
/**
 * Created by author <b>suryaprakash</b> on Aug 26, 2014 12:31:08 PM
 * </br>
 * Project: CSCLibrary
 * </br>
 *
 */
/**
 * Created by author <b>suryaprakash</b> on Aug 26, 2014 12:31:11 PM </br>
 * Project: CSCLibrary </br>
 * 
 */
public enum ErrorCodes {

	/**
	 * This is generic error code for sending success status
	 */
	SUCCESS_RESPONSE(0x01010001),
	/**
	 * This is generic error code for sending failure status
	 */
	FAILURE_RESPONSE(0x01010002),

	/**
	 * Error occurs when bluetooth is not supported in the device.
	 */
	DEVICE_BT_NOT_SUPPORTED(0x2001),
	/**
	 * Security exception,bluetooth permissions are not declared in manifest.
	 * file.
	 */
	DEVICE_BT_NOT_AUTHERISED(0x2003),
	/**
	 * This error occur when the Android device bluetooth is switched off.
	 */
	DEVICE_BT_OFF(0x2002),
	/**
	 * This indicates parameters for the method invalid.
	 */
	INVALID_PARAMS(0x02012001),
	/**
	 * This indicates that unable to fetch paired devices information from the
	 * device.
	 */
	DEVICE_BT_ERR_ON_PAIRED_DEVICES(0x03023002),
	/**
	 * This indicates bluetooth generic error.(If any error other than
	 * specified).
	 */
	DEVICE_BT_ERR(0x03013003),
	/**
	 * This indicates that bluetooth api in not found on device.
	 */
	DEVICE_BT_NOT_FOUND(0x2004),
	/**
	 * This indicates device synchronization write operation performing on main
	 * (UI) thread.
	 */
	DEVICE_BLE_SYNC_WRITE_ON_MAIN_THREAD(0x3002),
	/**
	 * This indicates BLE GATT service initialization failed.
	 */
	COMMUNICATION_BLE_GATT_FAILURE(0x3006),

	/**
	 * This indication that device connection error while trying to connect.
	 */
	DEVICE_CONNECTION_ERROR(0x04034001),
	/**
	 * Invalid response received.
	 */
	INVALID_REPOSNSE_DATA(0x04034003),
	/**
	 * This indicates that enabling BLE GATT notification service failed. GATT
	 * error/service error.
	 */
	COMMUNICATION_BLE_ENABLE_NOTIFICATION_FAILED(0x04014004),
	/**
	 * This error occurs when there is an exception while performing IO
	 * operations.This may due to socket is closed.
	 */
	COMMUNICATION_IO_EXCEPTION(0x04014009),
	/**
	 * This error occurs when device not connected.
	 */
	COMMUNICATION_DEVICE_NOT_CONNECTED(0x04014010),
	/**
	 * This indicates that enabling BLE GATT configuration descriptor service
	 * failed. GATT error/service error.
	 */
	COMMUNICATION_BLE_CONFIG_DESCRIPTOR_FAILED(0x04014005),
	/**
	 * This indicates that enabling BLE side key service failed. GATT
	 * error/service error.
	 */
	COMMUNICATION_BLE_SIDEKEY_SERVICE_FAILED(0x04014006),
	/**
	 * Sending BLE packets/data failed.
	 */
	COMMUNICATION_BLE_SENDING_DATA_FAILED(0x04014007),
	/**
	 * Reading packets/data failed from remote device.
	 */
	COMMUNICATION_BLE_RECEIVING_DATA_FAILED(0x04014008),
	/**
	 * Interrupt in writing command
	 */
	COMMUNICATION_BLE_WRITE_INTERRUPTED(0x04014009),
	/**
	 * Packet missing in writing command
	 */
	COMMUNICATION_BLE_PACKET_MISSING_IN_WRITE(0x04014011),

	/**
	 * Resource file not found on specified path.
	 */
	RESOURCE_FILE_NOT_FOUND(0x09039001),
	/**
	 * This indicates that error in reading resource file.
	 */
	RESOURCE_FILE_IO_ERROR(0x09039002),

	// SDIO Specific errors

	ERROR_DEVICE_SDIO_NOT_FOUND(0x1001), ERROR_UNABLE_TO_LOAD_NATIVE_LIBRARY(
			0x1004), ERROR_SDIO_NOT_POWER_ON(0x1006),
	/**
	 * Cannot write to a device in RF mode.
	 */
	ERROR_SDIO_RF_ON(0x1007), APDU_INVALID(0x080308001), /**
	 * The CSCLogger
	 * callback is set as NULL
	 */
	ERROR_NULL_CSCLOGGER(0x02012002);

	private int errorCode;
	private String errorKey;
	private String stackTrace;

	private ErrorCodes(final int errorCode) {
		this.errorCode = errorCode;
		errorKey = Integer.toString(this.errorCode, 16);
	}

	/**
	 * This method will fetch the error message from the resource file.
	 * 
	 * @return error message from resource file
	 */
	public String getMessage() {
		if (errorKey.length() % 2 != 0)
			errorKey = "0".concat(errorKey);

		if (ResourceProperties.getInstance() != null)
			return ResourceProperties.getInstance().getProperty(errorKey);
		else
			return null;
	}

	/**
	 * This method returns the error key received from the exception.
	 * 
	 * @return returns error key received from the exception.
	 */
	public String getErrorKey() {
		return errorKey;
	}

	/**
	 * Is to set a stack trace of an Exception.
	 * 
	 * @param stackTrace
	 *            Stack trace for this error.
	 */
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	/**
	 * This method returns a stack trace for this error.
	 * 
	 * @return String Stack trace for this error,null if it is not set.
	 */
	public String getStackTrace() {
		return stackTrace;
	}
}

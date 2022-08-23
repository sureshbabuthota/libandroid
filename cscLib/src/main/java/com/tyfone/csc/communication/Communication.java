/* 
 * 
 * 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.communication;

import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.exception.CSCException; 
import com.tyfone.csc.helper.ErrorCodes;

/**
 * <p>
 * This class provides an interface to connect and communicate with a device.
 * </p>
 *   
 * <p>
 *  
 * Use {@link Communication#connect(DeviceDetails, CSCCallback)} to connect to a device.
 * 
 * </p>
 * 
 * <p>
 * Connection state of a device is notified through a registered
 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
 * </p>
 * 
 *
 * 
 * <p>
 * 
 * Use {@link #write(byte[], CSCCallback)} method to write a command to a
 * {@link Device}. Response of this write operation is returned through a callback
 * {@link CSCCallback#onReceiveResponse(byte[], ErrorCodes)}
 * </p>
 * <P>
 * Use {@link #write(byte[])} to get back the response as a return type.
 * 
 * </p>
 *  @author Srikar created on Aug 7, 2014
 */
public abstract class Communication {

	/**
	 *  Establishes connection with a selected device and connection state of the
	 * {@link Device} is sent back through a  callback {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
	 * 
	 * @param deviceDetails
	 *            {@link DeviceDetails} to which connection needs to be
	 *            established.
	 * @param cscCallback
	 *             {@link CSCCallback} instance to send connection state of a {@link Device};
	 *             
	 * @throws CSCException
	 *            throws a {@link CSCException} if an error occurs while connecting to a device.
	 */
	public abstract void connect(final DeviceDetails deviceDetails,
			final CSCCallback cscCallback) throws CSCException;

	/**
	 * Method to disconnect the selected
	 * device. Disconnect status is returned through a callback
	 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
	 * @throws CSCException
	 *            throws a {@link CSCException} if an error occurs while disconnecting from a device.
	 */
	public abstract void disconnect() throws CSCException;

	/**
	 * This method executes commandBytes and sends response through {@link CSCCallback#onReceiveResponse(byte[], ErrorCodes)}.
	 * 
	 * @param commandBytes
	 *            bytes of data to write.
	 * @param cscCallback
	 *            callback to receive response bytes and errors.
	 * 
	 * @throws CSCException
	 *         throws a {@link CSCException} if a write operation fails.
	 * @TEE
	 */
	public abstract void write(final byte[] commandBytes,
			final ReadWriteCallback cscCallback) throws CSCException;

	/**
	 * This method writes data in bytes and returns response in bytes.
	 * 
	 * @param dataBytes
	 *            bytes of data to write.
	 * @return response in bytes.
	 * @throws CSCException
	 *         throws a {@link CSCException} if a write operation fails.
	 * @TEE
	 */
	public abstract byte[] write(byte[] dataBytes)throws CSCException;

	/**
	 * This method returns device connection state.
	 * 
	 * @return {@link DeviceConnectionState}
	 * 
	 * @throws CSCException
	 * 	          throws a {@link CSCException} if getConnectionState  fails.

	 */
	public abstract DeviceConnectionState getConnectionState()
			throws CSCException;

}

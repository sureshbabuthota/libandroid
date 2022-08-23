/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.Communication;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.BluetoothHelper;
import com.tyfone.csc.helper.ConversionHelper;
import com.tyfone.csc.helper.ErrorCodes;
//import com.tyfone.csc.log.Logger;

/**
 * Communication module implementation of a Bluetooth classic device. This class
 * is used to connect and communicate with a Bluetooth classic device.
 * 
 * <p>
 * Connection state of a device is notified through a registered
 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
 * </p>
 * 
 * <p>
 * 
 * Use {@link #write(byte[], CSCCallback)} method to write a command to a
 * {@link Device}. Response of this write operation is returned through a
 * callback {@link CSCCallback#onReceiveResponse(byte[], ErrorCodes)}.
 * </p>
 * <P>
 * Use {@link #write(byte[])} to get back the response as a return type.
 * 
 * </p>
 * 
 *
 * @see BTClassicCommunication#connect(DeviceDetails, CSCCallback)
 * @see DeviceConnectionState
 * @see CSCCallback
 * 
 * @author Srikar created on Aug 7, 2014
 */

 class BTClassicCommunication extends Communication {

	// Well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB for Bluetooth
	// serial board connection.
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");// For SPP
	private static final String TAG = "BTClassic communication";

	private DeviceConnectionState mDevConnectionState;
	private ConnectThread mConnectThread;
	private BluetoothAdapter mAdapter;

	private Device btClassicDevice;

	private ConnectedThread mConnectedThread;
	//private CSCCallback mCSCCallback;
	private ReadWriteCallback mReadWriteCallback;

	private byte[] responseBuffer;

	private ReadWriteLock lock;

	/**
	 * Constructs {@link BTClassicCommunication} for a given
	 * {@link BTClassicDevice}.
	 * 
	 * @param device
	 *            {@link BTClassicDevice} instance to connect and communicate
	 *            to.
	 */
	public BTClassicCommunication(BTClassicDevice device) {
		btClassicDevice = device;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mDevConnectionState = DeviceConnectionState.NOT_CONNECTED;
		lock = new ReentrantReadWriteLock();

	}

	/**
	 * Constructs {@link BTClassicCommunication} for a given
	 * {@link BTClassicDevice}.
	 * 
	 * @param device
	 *            {@link BTClassicDevice} instance to connect and communicate
	 *            to.
	 * @param mBluetoothSocket
	 *            {@link BluetoothSocket} instance to perform IO operation.
	 */
	public BTClassicCommunication(BTClassicDevice device,
			BluetoothSocket mBluetoothSocket) {
		btClassicDevice = device;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mDevConnectionState = DeviceConnectionState.CONNECTED;
		mConnectedThread = new ConnectedThread(mBluetoothSocket);
		mConnectedThread.start();
		connected();
		lock = new ReentrantReadWriteLock();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.tyfone.csc.communication.Communication#connect(com.tyfone.csc.device
	 * .DeviceDetails, com.tyfone.csc.communication.BTCallback)
	 */
	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback btCallback)
			throws CSCException {

		connect(BluetoothHelper.getDeviceByMacAddress(deviceDetails
				.getAddress()));

	}

	/**
	 * This method will close the socket connection and do the cleanup operation
	 * of communication module.
	 */
	@Override
	public void disconnect() {
		if (getConnectionState() == DeviceConnectionState.CONNECTED) {
			if (mConnectedThread != null) {
				mConnectedThread.close();
			} else
				setConnectionState(DeviceConnectionState.NOT_CONNECTED);

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.Communication#isConnected()
	 */
	@Override
	public synchronized DeviceConnectionState getConnectionState() {
		// TODO Auto-generated method stub
		return mDevConnectionState;
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The {@link BluetoothDevice} to connect
	 * @throws CSCException
	 */
	private synchronized void connect(BluetoothDevice device)
			throws CSCException {

		// Cancel any thread attempting to make a connection
		if (mDevConnectionState == DeviceConnectionState.CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setConnectionState(DeviceConnectionState.CONNECTING);
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {

		private BluetoothSocket mSocket;

		/**
		 * Constructor for creating ConnectionThread.
		 * 
		 * @param device
		 *            {@link BluetoothDevice} to establish a connection.
		 * @throws CSCException
		 */
		public ConnectThread(BluetoothDevice device) throws CSCException {
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice

			try {
				tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				//Logger.error(TAG, e.getMessage());
				setConnectionState(DeviceConnectionState.NOT_CONNECTED);
				throw new CSCException(ErrorCodes.DEVICE_CONNECTION_ERROR);
			}

			mSocket = tmp;
		}

		public void run() {
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {

				mSocket.connect();

			} catch (IOException e) {
				setConnectionState(DeviceConnectionState.NOT_CONNECTED);
				try {
					mSocket.close();
				} catch (IOException e2) {

				}

				return;
			}

			synchronized (BTClassicCommunication.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			mConnectedThread = new ConnectedThread(mSocket);
			mConnectedThread.start();
			connected();
		}

		/**
		 * This method will close the socket connection.
		 */
		public void cancel() {
			synchronized (BTClassicCommunication.this) {
				try {

					mSocket.close();
				} catch (IOException e) {

				}
			}
		}
	}

	/**
	 * To set the connection state of a BTClassic device and stop the connected
	 * thread.
	 * 
	 */
	private synchronized void connected() {

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		setConnectionState(DeviceConnectionState.CONNECTED);
	}

	/**
	 * Sets the current device connection state and notify
	 * {@link DeviceConnectionState} through
	 * {@link CSCCallback#onConnectionChange(Device, DeviceConnectionState)}.
	 * 
	 * @param state
	 *            A {@link DeviceConnectionState} defining the current
	 *            connection state
	 */
	public synchronized void setConnectionState(DeviceConnectionState state) {
		mDevConnectionState = state;
		//Logger.info(this.toString() + state);
		if (btClassicDevice != null)
			btClassicDevice.onConnectionChange(btClassicDevice, state);

	}

	/**
	 * This method executes commandBytes and sends response through
	 * {@link CSCCallback#onReceiveResponse(byte[], ErrorCodes)}.
	 * 
	 * @param commandBytes
	 *            bytes of data to write.
	 * @param cscCallback
	 *            callback to send response with error codes.
	 * 
	 * @throws CSCException
	 *             is thrown when a write operation fails.
	 * @TEE
	 */
	@Override
	public synchronized void write(final byte[] commandBytes,
			final ReadWriteCallback readWriteCallback) throws CSCException {
		if (commandBytes == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		if (readWriteCallback == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		if (getConnectionState() != DeviceConnectionState.CONNECTED)
			throw new CSCException(
					ErrorCodes.COMMUNICATION_DEVICE_NOT_CONNECTED);
		mReadWriteCallback = readWriteCallback;
		mConnectedThread.write(commandBytes, false);
	}

	/**
	 * This method takes input as bytes and returns response in bytes.
	 * 
	 * @param commandBytes
	 *            bytes of data to write.
	 * @return response in bytes.
	 * @throws CSCException
	 *             is thrown when a write operation fails.
	 * @TEE
	 */
	@Override
	public byte[] write(byte[] commandBytes) throws CSCException {
		if (commandBytes == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);
		if (getConnectionState() != DeviceConnectionState.CONNECTED)
			throw new CSCException(
					ErrorCodes.COMMUNICATION_DEVICE_NOT_CONNECTED);

		if (mConnectedThread != null)
			return mConnectedThread.write(commandBytes, true);

		return null;
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private static final int TIMEOUT_LOOP_COUNT = 50;// 20*50=1000ms waiting
															// for maximum 1
															// second to check
															// for data
															// availability in a
															// Input Stream.
		private static final int GET_AVAILABLE_SLEEP_TIMEOUT = 20;
		private final BluetoothSocket mSocket;
		private final InputStream mInStream;
		private final OutputStream mOutStream;

		public ConnectedThread(BluetoothSocket socket) {

			mSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();

			} catch (IOException e) {
				//Logger.error("temp sockets not created");

			}

			mInStream = tmpIn;
			mOutStream = tmpOut;
		}

		public void run() {
			//Logger.info("BEGIN mConnectedThread");
			// read(INITIAL_MESSAGE_READ);
			// Read out the data from socket after connection is established.
			try {
				read(true);
			} catch (CSCException e) {
			}

		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param commandBytes
		 *            Bytes to write.
		 * @param isSynchronousCall
		 *            If true response is returned by this method else response
		 *            is send through
		 *            {@link CSCCallback#onReceiveResponse(byte[], com.tyfone.csc.smartcard.Applet, ErrorCodes)}
		 * 
		 */
		public final synchronized byte[] write(byte[] commandBytes,
				boolean isSyncronousCall) throws CSCException {
			byte[] outPutBytes = ConversionHelper
					.prependLengthBytes(commandBytes);
			boolean writeSucceded = true;

			if (mConnectedThread != null)
				lock.writeLock().lock();
			try {

				mOutStream.write(outPutBytes);
				mOutStream.flush();
			} catch (IOException e) {
				writeSucceded = false;
				throw new CSCException(ErrorCodes.COMMUNICATION_IO_EXCEPTION);
			} finally {
				lock.writeLock().unlock();
			}
			if (writeSucceded) {
				return read(isSyncronousCall);

			}

			// If is asynchronous call response bytes is sent through
			// CSCCallback onReceiveResponse method.
			return null;

		}

		/**
		 * Read data from connected InputStream.
		 * 
		 * @param isSynchronousCall
		 *            If true response is returned by this method else response
		 *            is send through
		 *            {@link CSCCallback#onReceiveResponse(byte[], com.tyfone.csc.smartcard.Applet, ErrorCodes)}
		 * 
		 */
		private synchronized byte[] read(boolean isSynchronousCall)
				throws CSCException {
			int len = 0, readPointer = 0, temp = -1;
			int respLen = 0;

			try {
				int timeout = 0;
				int maxTimeout = TIMEOUT_LOOP_COUNT; // leads to a timeout of 1
														// second
				int available = 0;

				while ((available = mInStream.available()) == 0
						&& timeout < maxTimeout) {
					timeout++;
					// throws interrupted exception
					try {
						Thread.sleep(GET_AVAILABLE_SLEEP_TIMEOUT);
					} catch (InterruptedException e) {
					}
					Log.v("Inside while", "Time:" + timeout);
				}
				if (available > 0) {

					len = mInStream.read();
					if (len != -1) {
						int lensecondbyte = mInStream.read();

						if (lensecondbyte != -1) {
							// covert length bytes to int
							// Adding two byte to get a length.
							respLen = lensecondbyte | len << 8;
							respLen += 2; // add 2 bytes for the status
											// code.

						/*	Logger.info("In BluetoothCommunication respLen "
									+ respLen);*/
							responseBuffer = new byte[respLen]; // buffer to
																// hold
							// data without len
							// bytes
							for (readPointer = 0; readPointer < respLen; readPointer++) {
								temp = mInStream.read();
								responseBuffer[readPointer] = (byte) temp;

							}

						}
					}
				}
				if (!isSynchronousCall) {
					mReadWriteCallback.onReceiveResponse(responseBuffer,
							ErrorCodes.SUCCESS_RESPONSE);
					// Response is sent through a call back,no
					// need to return response.
					return null;
				}
				/*Logger.info("In BluetoothCommunication read buffer data is "
						+ ConversionHelper.byteArrayToHex(responseBuffer));*/

			} catch (IOException e) {
				mReadWriteCallback.onReceiveResponse(responseBuffer,
						ErrorCodes.COMMUNICATION_IO_EXCEPTION);
				setConnectionState(DeviceConnectionState.NOT_CONNECTED);
			} finally {
				// lock.readLock().unlock();
			}

			return responseBuffer;

		}

		/**
		 * This method will close all the active connections and streams.
		 */
		public void close() {
			try {
				mInStream.close();
				mOutStream.close();
				mSocket.close();
				setConnectionState(DeviceConnectionState.NOT_CONNECTED);
			} catch (IOException e) {
			}
		}
	}

}

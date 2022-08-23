/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.ble;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.tyfone.csc.CSC;
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
import com.tyfone.csc.resource.ResourceProperties;

/**
 * 
 * <p>
 * This class provides an interface to connect and communicate with BLE device.
 * </p>
 * 
 * <p>
 * Using this class, application can connect with BLE device by calling
 * {@link #connect(DeviceDetails, CSCCallback)} and establish communication with
 * the Smart card present in the device by
 * {@link BLECommunication#write(byte[], CSCCallback)} or
 * {@link BLECommunication#write(byte[])}.
 * 
 * <p>
 * {@link #write(byte[], CSCCallback)} internally uses {@link #write(byte[])} to
 * execute commands and sends response as a CSCCallback. Writing a command is
 * handled by the {@link WritePacketListner} and receiving data from BLE device
 * is handled by {@link ReadPacketListner}.
 * </p>
 * 
 * @author Suryaprakash created on Aug 7, 2014
 */
@SuppressLint("NewApi")
  final class BLECommunication extends Communication {

	/**
	 * This class is used for locking find smart card command till receiving
	 * Smart card response or connection status change.
	 * 
	 */
	private class BLEWriteMutex {
		private boolean readyToUse = false;

		public boolean isReadyToUse() {
			return readyToUse;
		}

		public void setReadyToUse(boolean readyToUse) {
			this.readyToUse = readyToUse;
		}
	}

	private enum PacketStatus {
		/**
		 * It indicates that packet is ready to sent.
		 */
		SEND_PACKET,
		/**
		 * It indicates that packet sent.
		 */
		COMMAND_SENT,
		/**
		 * It indicates that packet is failed to sent.
		 */
		COMMAND_FAILURE,
		/**
		 * It indicates that packet missed to receive at remote device end.
		 */
		PACKET_MISSING
	}

	/**
	 * This is used to reading data packets when receiving it from remote BLE
	 * device
	 */
	private interface ReadPacketListner {

		/**
		 * This will return status of receiving packets from remote device.
		 * 
		 * @return return receiving packets status from device
		 */
		boolean isReceiving();

		/**
		 * This method will call when read operation throws an error.
		 * 
		 * @param value
		 *            received packet from remote device
		 */
		void onReadError(byte[] value);

		/**
		 * This will start reading data from remote device.
		 * 
		 * @param bytes
		 *            received packet array information from remote ble device.
		 */
		void onReadStart(byte[] bytes) throws CSCException;

		/**
		 * This method will called once received data from remote device.
		 * 
		 * @param receivedBytes
		 *            response byte array from remote ble device.
		 */
		void readCompleted(byte[] receivedBytes);

		/**
		 * This method will call while reading next packets from remote device.
		 * 
		 * @param bytes
		 *            received byte array information from remote ble device.
		 */
		void readPacket(byte[] bytes);

		/**
		 * This method will reset the last received response byte array.
		 */
		void resetResponseBytes();

		/**
		 * This method returns the received bytes from the remote device.
		 * 
		 * @return returns response byte array.
		 */
		byte[] responseBytes();
	}

	/**
	 * This is used to write {@link APDU} data packets on device.
	 * 
	 */
	private interface WritePacketListner {
		/**
		 * It will return TRUE if writing command on remote device otherwise
		 * FALSE.
		 * 
		 * @return returns the command write status.
		 */
		boolean isWriting();

		/**
		 * This method will called when started writing packet on device.
		 * 
		 * @param bytes
		 *            Writing packet data on remote device.
		 */
		void onWriteStart(byte[] bytes);

		/**
		 * This method will call once write apdu is done.
		 * 
		 * @param packetStatus
		 *            It has status sent packets.
		 */
		void writeCompleted(PacketStatus packetStatus);

		/**
		 * This will called while writing next packets.
		 */
		void writePacket();
	}

	private BluetoothGatt mConnectedGatt;

	private Device bleDevice;

	// Timer for send timeout error
	private Timer timer = new Timer();

	/* UUID Service */
	/** The sidekey service. */
	private static final UUID SIDEKEY_SERVICE = UUID
			.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

	/* UUID Characteristics */
	/** The sidekey response char. */
	private static final UUID SIDEKEY_RESPONSE_CHAR = UUID
			.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

	/** The sidekey write apdu char. */
	private static final UUID SIDEKEY_WRITE_APDU_CHAR = UUID
			.fromString("0000fff5-0000-1000-8000-00805f9b34fb");

	/* Client Configuration Descriptor */
	/** The Constant CONFIG_DESCRIPTOR. */
	private static final UUID CONFIG_DESCRIPTOR = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");

	protected static int timeOut = 0, waitTimeOut = 0;

	private CSCCallback cscCallback;
	/**
	 * This class is used to implement {@link BluetoothGatt} callback.
	 */

	private byte[] sentPacket = null;
	private boolean readDone = false;

	private boolean writeDone = false;

	private ReadPacketListner readPacketListner = new ReadPacketListner() {
		private boolean receivingDataActive = false;
		private int size = 0;
		private byte[] buffer;
		private int copyPointerPosition = 0;
		byte[] receiveBytes;

		@Override
		public boolean isReceiving() {
			// Returns the receiving bytes or not.
			return this.receivingDataActive;
		}

		@Override
		public void onReadError(byte[] value) {
			synchronized (mutex) {
				readDone = true;
				// stop timeout runner
				disbaleTimeout();
				this.receivingDataActive = false;

				// Check if the write call is synchronized call or not. If not
				// send response data in cscCallback.
				if (!isSyncWrite)
					cscCallback.onReceiveResponse(null,
							ErrorCodes.COMMUNICATION_BLE_RECEIVING_DATA_FAILED);
			//	Logger.debug("==============Read Completed with Error==============");
				// On error release the thread for next write operation
				mutex.notify();
				mutex.setReadyToUse(true);
			}
		}

		@Override
		public void onReadStart(byte[] bytes) throws CSCException {
			readDone = false;
			int readStartIndex = 0;
			// Stop timeout runner.
			disbaleTimeout();

			// Enable receiving bytes data flag.
			receivingDataActive = true;

			// Check for the received data size minimum 2 bytes or not.
			if (bytes.length < 2) {
				throw new CSCException(ErrorCodes.INVALID_REPOSNSE_DATA);
			} else if (bytes.length == 2) {
				readStartIndex = 0;
				size = bytes.length;
			} else {
				// Calculate the size of the expected response and 2-bytes
				// status
				// code at end.
				readStartIndex = 2;
				size = (bytes[0] << 8 & 0xFF00 | bytes[1]) + 2;
			}
			//Logger.debug("Received Response Size:", "Size:" + size);

			// If the size of the response is zero, then send the 2 -byte status
			// code.
			buffer = new byte[size];

			// Copy the response data from first received packet.
			byte[] dataBytes = Arrays.copyOfRange(bytes, readStartIndex,
					bytes.length);

			// Send the bytes data to readPacket for response
			readPacket(dataBytes);
		}

		@Override
		public void readCompleted(byte[] receivedBytes) {
			synchronized (mutex) {
				readDone = true;
				// stop timeout runner
				disbaleTimeout();

				// Enable receiving bytes data flag.
				receivingDataActive = false;

				// Reset the copy pointer once write done.
				copyPointerPosition = 0;
				/*Logger.debug("Received Response on device (readCompleted) "
						+ bleDevice.getDeviceDetails().getAddress(),
						receivedBytes);*/
				// Check if the write call is synchronized call or not. If not
				// send response data in cscCallback.
				if (!isSyncWrite)
					cscCallback.onReceiveResponse(receivedBytes,
							ErrorCodes.SUCCESS_RESPONSE);
				this.receiveBytes = receivedBytes;
/*				Logger.debug("==============Read Completed==============");
*/				String sendCommandTimeOutString = ResourceProperties
						.getInstance().getProperty(
								"SEND_COMMAND_WAIT_TIME_IN_MILLIS");
				if (sendCommandTimeOutString != null
						&& sendCommandTimeOutString.matches("\\d+")) {
					waitTimeOut = Integer.parseInt(sendCommandTimeOutString);
				}
				try {
					Thread.sleep(waitTimeOut);
				} catch (InterruptedException e) {
					//Logger.info(getClass().getSimpleName(),
						//	"Exception in Wait for next read:" + e.toString());
				}
				mutex.notify();
				mutex.setReadyToUse(true);
			}
		}

		@Override
		public void readPacket(byte[] receivedBytes) {

			// stop timeout runner
			disbaleTimeout();

			if (copyPointerPosition <= size) {
				if (copyPointerPosition + receivedBytes.length < size) {
					// Copy the newly received bytes into 'buffer' byte array
					// from position 'copyPointerPosition' to 'receivedBytes'
					// array length.
					System.arraycopy(receivedBytes, 0, buffer,
							copyPointerPosition, receivedBytes.length);

					// Change the 'copyPointerPosition' to last byte index
					copyPointerPosition = copyPointerPosition
							+ receivedBytes.length;

					// Enable timer for next response packet to receive.
					enableTimeoutRunner(false);
				} else {
					// Received last packet less than 20 bytes.
					System.arraycopy(receivedBytes, 0, buffer,
							copyPointerPosition, size - copyPointerPosition);

					// Call read completed with response buffer byte array.
					readPacketListner.readCompleted(buffer);
					buffer = null;
				}
			} else {
				// received last packet.
				readPacketListner.readCompleted(buffer);
				buffer = null;
			}
		}

		@Override
		public void resetResponseBytes() {
			this.receiveBytes = null;
		}

		@Override
		public byte[] responseBytes() {
			// returns received bytes, if failed to complete receive bytes
			// returns NULL.
			return this.receiveBytes;
		}
	};

	private WritePacketListner writePacketListner = new WritePacketListner() {
		private PacketStatus packetState;
		private int index = 0;
		private int packetsCount;
		private byte[] commandBytes;

		@Override
		public boolean isWriting() {
			return writeDone;
		}

		@Override
		public void onWriteStart(byte[] bytes) {
		//	Logger.debug("==============Write Started==============");
		/*	Logger.debug("Write Started on device "
					+ bleDevice.getDeviceDetails().getAddress(), bytes);
		*/	index = 0;
			readPacketListner.resetResponseBytes();
			final int commandPacketsCount = ConversionHelper
					.getApduPacketsCount(bytes);
			writeDone = false;
			// Prepend the length bytes to command bytes.
			this.commandBytes = ConversionHelper.prependLengthBytes(bytes);
			this.packetsCount = commandPacketsCount;
			packetState = PacketStatus.SEND_PACKET;

			// Write the packet/packets on remote device.
			writePacket();
		}

		@Override
		public void writeCompleted(PacketStatus packetStatus) {
			synchronized (mutex) {
				readDone = false;
				// Stop timeout runner.
				disbaleTimeout();

				/*Logger.debug(
						"Writing Command Completed on Device "
								+ bleDevice.getDeviceDetails().getAddress(),
						"Write complete with Packet Status="
								+ packetStatus.toString());
*/				if (packetStatus == PacketStatus.COMMAND_FAILURE) {
					// Check if the write call is synchronized call or not. If
					// not send response data in cscCallback.
					if (!isSyncWrite) {
						cscCallback
								.onReceiveResponse(
										null,
										ErrorCodes.COMMUNICATION_BLE_SENDING_DATA_FAILED);
					}
					writeDone = true;

					// If any error in writing command, release the thread from
					// waiting state.
					//Logger.debug("==============Write Completed With Error==============");
					mutex.notify();
					mutex.setReadyToUse(true);
					return;
				} else if (packetStatus == PacketStatus.PACKET_MISSING) {
					// Check if the write call is synchronized call or not. If
					// not send response data in cscCallback.
					if (!isSyncWrite) {
						cscCallback
								.onReceiveResponse(
										null,
										ErrorCodes.COMMUNICATION_BLE_PACKET_MISSING_IN_WRITE);
					}
					writeDone = true;

					// If any error in writing command, release the thread from
					// waiting state.
					//Logger.debug("==============Write Completed With Error==============");
					mutex.notify();
					mutex.setReadyToUse(true);
					return;
				}

				// reset variables
				packetState = null;
				index = 0;
				packetsCount = 0;

				// Set timer for response timeout (Waiting for Characteristic
				// change notification )
				//Logger.debug("Write Completed", "Timer Started");
				enableTimeoutRunner(false);
			}
		}

		@Override
		public void writePacket() {

			// stop timeout runner
			disbaleTimeout();
			if (packetState == PacketStatus.SEND_PACKET && index < packetsCount) {

				// enable packet timeout runner to know device received packet
				// or not.
			//	Logger.debug("Write Packet", "Timer Started");
				enableTimeoutRunner(true);

				// Get the end position of the command bytes to send
				int endPosition = (index + 1) * 20;
				if (endPosition < commandBytes.length) {
					// Copy the command bytes to sentPacket
					sentPacket = Arrays.copyOfRange(commandBytes, index * 20,
							endPosition);
				} else {
					// Copy the command bytes to sentPacket
					sentPacket = Arrays.copyOfRange(commandBytes, index * 20,
							commandBytes.length);
				}
				index++;

				//Logger.debug("Writing Packet " + index + " on device "
					//	+ bleDevice.getDeviceDetails().getAddress(), sentPacket);

				boolean isWriteIntialized = false;
				try {
					// Writing the command and setting it to write
					// characteristic.
					isWriteIntialized = writeCharacteristicBytes(sentPacket);
				} catch (CSCException e) {
					isWriteIntialized = false;
				}
				if (!isWriteIntialized) {
					// If write characteristic failed, call write completed with
					// error status.
					writeCompleted(PacketStatus.COMMAND_FAILURE);
					return;
				}
			} else {
				writeCompleted(PacketStatus.COMMAND_SENT);
				return;
			}
		}
	};

	/**
	 * This method will write commandBytes on remote device and sends the
	 * response as {@link CSCCallback}
	 * 
	 * @param apdu
	 *            Apdu to write on remote device.
	 * 
	 * @param btCallback
	 *            Callback to receive Apdu response from remote device.
	 * 
	 * @throws CSCException
	 *             through errors {@link ErrorCodes}
	 */

	private BLEWriteMutex mutex = new BLEWriteMutex();

	private boolean isSyncWrite = false;

	private int timeleft = 0;

	/**
	 * Constructs a communication channel.
	 * 
	 * @param bleDevice
	 *            BLE device object for communication.
	 */
	public BLECommunication(BLEDevice bleDevice) {
		this.bleDevice = bleDevice;
		// Wait for some time interval (in milliseconds) for
		// every send command operation. Because of BLE side key
		// having limitation that It needs some time gap to
		// send command after received data.
		String packetWaitTimeOutString = ResourceProperties.getInstance()
				.getProperty("PACKET_SEND_OR_RECEIVE_TIMEOUT_IN_SECONDS");
		if (packetWaitTimeOutString != null
				&& packetWaitTimeOutString.matches("\\d+")) {
			timeOut = Integer.parseInt(packetWaitTimeOutString);
		}
		//Logger.info("Packet Timeout :" + timeOut);
	}

	/**
	 * This method establishes a connection with BLE device and sends status as
	 * {@link CSCCallback}. In case of an exception, it throws
	 * {@link CSCException} along with error message.
	 * 
	 * @see Communication#connect(DeviceDetails,
	 *      CSCCallback)
	 */
	@Override
	public void connect(DeviceDetails deviceDetails, CSCCallback mCscCallback)
			throws CSCException {
		if (deviceDetails == null || mCscCallback == null)
			throw new CSCException(ErrorCodes.INVALID_PARAMS);

		this.cscCallback = mCscCallback;
		// Call BLE GATT Connection to establish connection
		connectBLEGatt();
	}

	/**
	 * This method will establish connect if not connected already. And also
	 * enable BLE services to send data and receive notifications
	 * 
	 * @throws CSCException
	 */
	private void connectBLEGatt() throws CSCException {
		// Instantiate BLE Communication object here to connect BLE device.
		BluetoothAdapter mBluetoothAdapter = BluetoothHelper.getBleAdapter();
		if (mBluetoothAdapter != null) {
			final BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(bleDevice.getDeviceDetails().getAddress());
			/*
			 * Make a connection with the device using the special LE-specific
			 * connectGatt() method, passing in a callback for GATT events
			 */
			if (device != null) {
				// If the connection happen before, reconnect the GATT by
				// calling
				// connect() else establish new connection with
				// BluetoothGattCallback
				if (mConnectedGatt != null)
					mConnectedGatt.connect();
				else
					mConnectedGatt = device.connectGatt(CSC.getContext(),
							false, new BluetoothGattCallback() {

								/**
								 * Enable side key notification of a remote ble
								 * device.
								 * 
								 * @param gatt
								 *            Gatt object to enable side key
								 *            notifications.
								 * @throws CSCException
								 */
								private void enableSideKeyNotifications(
										BluetoothGatt gatt) {
									BluetoothGattCharacteristic characteristic = gatt
											.getService(SIDEKEY_SERVICE)
											.getCharacteristic(
													SIDEKEY_RESPONSE_CHAR);
									if (characteristic != null) {
										// Enable local notifications
										gatt.setCharacteristicNotification(
												characteristic, true);
										// Enabled remote notifications
										BluetoothGattDescriptor desc = characteristic
												.getDescriptor(CONFIG_DESCRIPTOR);
										if (desc != null) {
											desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
											gatt.writeDescriptor(desc);
										} else {
											try {
												throw new CSCException(
														ErrorCodes.COMMUNICATION_BLE_CONFIG_DESCRIPTOR_FAILED);
											} catch (CSCException e) {
												//Logger.error(e.getMessage());
											}
										}
									} else {
										try {
											throw new CSCException(
													ErrorCodes.COMMUNICATION_BLE_ENABLE_NOTIFICATION_FAILED);
										} catch (CSCException e) {
											//Logger.error(e.getMessage());
										}
									}
								}

								@Override
								public void onCharacteristicChanged(
										BluetoothGatt gatt,
										BluetoothGattCharacteristic characteristic) {
								//	Logger.debug("Received Packet Response"
									//		+ gatt.getDevice().getAddress(),
										//	characteristic.getValue());
									// Stop the response time out
									disbaleTimeout();

									// Once read done skip flush data if
									// anything received otherwise send
									// response data to application
									if (!readDone) {
										/*Logger.debug(
												"On Received Response(onCharacteristicChanged):"
														+ bleDevice
																.getDeviceDetails()
																.getAddress(),
												characteristic.getValue());
*/
										if (!readPacketListner.isReceiving()) {// If
																				// packets
																				// not
																				// receiving
																				// start
																				// reading
											try {
												// Start reading first packet
												readPacketListner
														.onReadStart(characteristic
																.getValue());
											} catch (CSCException e) {
												Log.i(getClass()
														.getSimpleName(), e
														.getMessage());
												// Error in reading packets
												readPacketListner
														.onReadError(characteristic
																.getValue());
											}
										} else {
											// Reading the data packets from
											// device
											readPacketListner
													.readPacket(characteristic
															.getValue());
										}
									}
								}

								@Override
								public void onCharacteristicRead(
										BluetoothGatt gatt,
										BluetoothGattCharacteristic characteristic,
										int status) {
								}

								@Override
								public void onCharacteristicWrite(
										BluetoothGatt gatt,
										BluetoothGattCharacteristic characteristic,
										int status) {
									//
									if (!writeDone) {
										/*Logger.debug(
												"Received Characterstic Write on (In Hex)"
														+ bleDevice
																.getDeviceDetails()
																.getAddress(),
												characteristic.getValue());
										Logger.debug(
												"Received Characterstic Write on (In Bytes)"
														+ bleDevice
																.getDeviceDetails()
																.getAddress(),
												Arrays.toString(characteristic
														.getValue()));*/
										// Check whether sent packet and
										// received were same. If not same
										// return "packet missing" error, if
										// same send remaining packets to remote
										// device
										if (sentPacket != null
												&& Arrays.equals(sentPacket,
														characteristic
																.getValue())) {
											writePacketListner.writePacket();
										} else {
											// call write complete with status
											// PACKET_MISSING
											writePacketListner
													.writeCompleted(PacketStatus.PACKET_MISSING);
										}
									}
								}

								@Override
								public void onConnectionStateChange(
										BluetoothGatt gatt, int status,
										int newState) {
									// If Gatt status is success and NEW STATE
									// is
									// STATE_CONNECTED do this
									Log.d("****onConnectionStateChange****",
											"Device:"
													+ gatt.getDevice()
															.getAddress()
													+ ", Status:" + status
													+ ", New state:" + newState);
									if (status == BluetoothGatt.GATT_SUCCESS
											&& newState == BluetoothProfile.STATE_CONNECTED) {

										// discover the services present in
										// device
										gatt.discoverServices();
									/*	Logger.info(
												"onConnectionStateChange"
														+ getClass()
																.getSimpleName(),
												"GATT Success and Connection established with "
														+ bleDevice
																.getDeviceDetails()
																.getAddress());
									*/	// Once connection established it will
										// call
										// #onServicesDiscovered() and
										// onDescriptorWrite()
									} else if (status == BluetoothGatt.GATT_SUCCESS
											&& newState == BluetoothProfile.STATE_DISCONNECTED
											&& getConnectionState() != DeviceConnectionState.CONNECTED) {// If
										// Gatt status is success and NEW STATE
										// is STATE_DISCONNECTED do
										// this
										cscCallback
												.onConnectionChange(
														bleDevice,
														DeviceConnectionState.NOT_CONNECTED);
									//	Logger.info("GATT Success and Connection disconnected with "
										//		+ gatt.getDevice().getAddress());
										if (readPacketListner.isReceiving()) {
											readPacketListner.onReadError(null);
										}

										if (!writePacketListner.isWriting()) {
											writePacketListner
													.writeCompleted(PacketStatus.PACKET_MISSING);
										}

										//
										disbaleTimeout();

									} else if (status == BluetoothGatt.GATT_FAILURE) { // If
																						// Gatt
																						// status
																						// is
																						// FAILUE
																						// do
																						// this
										cscCallback
												.onConnectionChange(
														bleDevice,
														DeviceConnectionState.ERROR_IN_CONNECTION);
									/*	Logger.info("GATT Failure "
												+ gatt.getDevice().getAddress());*/
									} else {
										cscCallback
												.onConnectionChange(
														bleDevice,
														DeviceConnectionState.ERROR_IN_CONNECTION);
										/*Logger.info("GATT Failure "
												+ gatt.getDevice().getAddress());*/
									}
								}

								@Override
								public void onDescriptorRead(
										BluetoothGatt gatt,
										BluetoothGattDescriptor descriptor,
										int status) {
								}

								@Override
								public void onDescriptorWrite(
										BluetoothGatt gatt,
										BluetoothGattDescriptor descriptor,
										int status) {
									// Sending the connection callback once
									// configurations done.
									cscCallback.onConnectionChange(bleDevice,
											DeviceConnectionState.CONNECTED);
								}

								@Override
								public void onReadRemoteRssi(
										BluetoothGatt gatt, int rssi, int status) {
								}

								@Override
								public void onServicesDiscovered(
										BluetoothGatt gatt, int status) {
									// Once Service discovered, enable side key
									// notifications
									enableSideKeyNotifications(gatt);
								}
							});
			} else
				throw new CSCException(ErrorCodes.DEVICE_BT_NOT_FOUND);
		} else {
			// throws exception if bluetooth adapter is null
			throw new CSCException(ErrorCodes.DEVICE_BT_NOT_SUPPORTED);
		}
	}

	/**
	 * It will stop the time out runner.
	 */
	private void disbaleTimeout() {
		if (timer != null) {
			// Canceling timer
			timer.cancel();
			// Purge timer object memory
			timer.purge();
			timer = null;
		}
	}

	/**
	 * Disconnects an established BLE connection, or cancels a connection
	 * attempt that is in progress.
	 * 
	 * @throws CSCException
	 * 
	 * @see Communication#disconnect()
	 */
	@Override
	public void disconnect() throws CSCException {
		// check for mConnectedGatt is null, if null skip step otherwise check
		// the connection state before disconnect
		BluetoothManager bluetoothManager = BluetoothHelper.getBleManager();
		BluetoothDevice device = null;
		device = BluetoothHelper.getBleAdapter().getRemoteDevice(
				bleDevice.getDeviceDetails().getAddress());
		if (bluetoothManager != null
				&& device != null
				&& bluetoothManager.getConnectionState(device,
						BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
			mConnectedGatt.disconnect();
			mConnectedGatt.close();
			mConnectedGatt = null;
		} else {
			if (bluetoothManager == null)
				throw new CSCException(ErrorCodes.DEVICE_BT_NOT_SUPPORTED);
			else if (device == null)
				throw new CSCException(ErrorCodes.DEVICE_BT_NOT_FOUND);
		}
	}

	/**
	 * It will starts the time out runner.
	 */
	private void enableTimeoutRunner(final boolean isWrite) {
		timeleft = timeOut;
		timer = new Timer();
		// Run timer at fixed rate (1 second)
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				/*Logger.debug("Timeout Running:"
						+ bleDevice.getDeviceDetails().getAddress() + ":"
						+ timeleft);*/
				if (timeleft > 0) {
					timeleft--;
				} else {
					// Stop time out
					disbaleTimeout();

					if (isWrite) {
						// Call write completed with error status.
						writePacketListner
								.writeCompleted(PacketStatus.COMMAND_FAILURE);
						/*Logger.debug(
								"Write Response Timeout",
								"Write notification not received. Apdu timed out and sending write error to application");*/
					} else {
						// Call read error with null data.
						readPacketListner.onReadError(null);
						/*Logger.debug("Read Response Timeout",
								"Response not received. Apdu timed out and sending read error to application");*/
					}
					return;
				}
			}
		}, 1000, 1000);
	}

	/**
	 * Gets the current connection state of the remote device.
	 * 
	 * @return {@link DeviceConnectionState} of a device.
	 * @throws CSCException
	 * @see Device#getConnectionStatus()
	 */
	@Override
	public DeviceConnectionState getConnectionState() {
		// get the bluetooth adapter and check for connection
		BluetoothDevice device = null;
		device = BluetoothHelper.getBleAdapter().getRemoteDevice(
				bleDevice.getDeviceDetails().getAddress());

		// Get the bluetooth manger to check the device status
		BluetoothManager bluetoothManager = BluetoothHelper.getBleManager();
		DeviceConnectionState deviceConnectionState = DeviceConnectionState.NOT_CONNECTED;
		if (bluetoothManager != null
				&& device != null
				&& bluetoothManager.getConnectionState(device,
						BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
			deviceConnectionState = DeviceConnectionState.CONNECTED;
		} else if (bluetoothManager != null
				&& device != null
				&& bluetoothManager.getConnectionState(device,
						BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTING) {
			deviceConnectionState = DeviceConnectionState.CONNECTING;
		} else {
			deviceConnectionState = DeviceConnectionState.NOT_CONNECTED;
		}

		return deviceConnectionState;

	}

	/**
	 * This method takes input as bytes and returns response in bytes.
	 * 
	 * @param commandBytes
	 *            bytes of data to write.
	 * @return response in bytes.
	 * @throws CSCException
	 *             is thrown when a write operation fails.
	 */
	@Override
	public synchronized byte[] write(final byte[] commandBytes)
			throws CSCException {
		isSyncWrite = true;
		// Writing the command
		writeCommand(commandBytes);
		// return the response bytes received from the device, if any failure
		// returns NULL
		return readPacketListner.responseBytes();
	}

	@Override
	public synchronized void write(final byte[] commandBytes,
			final ReadWriteCallback cscCallback) throws CSCException {
		isSyncWrite = false;
	//	this.cscCallback = cscCallback;
		writeCommand(commandBytes);
	}

	/**
	 * This method used to write characteristic on remote BLE device.
	 * 
	 * @param value
	 *            Apdu packet to write on remote device.
	 * 
	 * @return write status
	 * 
	 * @throws CSCException
	 */
	private boolean writeCharacteristicBytes(byte[] value) throws CSCException {
		boolean writeCharacteristicStatus = false;
		/*Logger.debug("Writting Apdu Packet (set to Writing characteristic) on "
				+ bleDevice.getDeviceDetails().getAddress(), value);*/
		if (mConnectedGatt != null
				&& getConnectionState() == DeviceConnectionState.CONNECTED) {
			// Getting the remote Side key service to write the command on it.
			BluetoothGattCharacteristic characteristic = mConnectedGatt
					.getService(SIDEKEY_SERVICE).getCharacteristic(
							SIDEKEY_WRITE_APDU_CHAR);
			if (characteristic == null)
				throw new CSCException(
						ErrorCodes.COMMUNICATION_BLE_SIDEKEY_SERVICE_FAILED);
			// Setting characteristic value.
			characteristic.setValue(value);
			if (getConnectionState() == DeviceConnectionState.CONNECTED)
				writeCharacteristicStatus = mConnectedGatt
						.writeCharacteristic(characteristic);
			else {
				throw new CSCException(ErrorCodes.DEVICE_CONNECTION_ERROR);
			}
		}
		return writeCharacteristicStatus;
	}

	/**
	 * This method is use to perform write operation asynchronously on device.
	 * 
	 * @throws CSCException
	 *             see {@link ErrorCodes}
	 */
	private synchronized void writeCommand(final byte[] commandBytes)
			throws CSCException {
		synchronized (mutex) {
			// Writing the command
			if (mConnectedGatt != null) {
				// Start writing packets
				// Take the command from Blocking queue
				writePacketListner.onWriteStart(commandBytes);
			} else
				throw new CSCException(
						ErrorCodes.COMMUNICATION_BLE_GATT_FAILURE);

			try {
				// Wait for the command response/error received for writing next
				// command.
				mutex.setReadyToUse(false);
				while (!mutex.isReadyToUse()) {
					mutex.wait();
				}
			} catch (InterruptedException e) {
				throw new CSCException(
						ErrorCodes.COMMUNICATION_BLE_WRITE_INTERRUPTED);
			}
		}
	}
}

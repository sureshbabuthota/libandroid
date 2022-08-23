package com.tyfone.csc.smartcard;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ConversionHelper;
import com.tyfone.csc.helper.ErrorCodes;
//import com.tyfone.csc.log.Logger;
import com.tyfone.csc.resource.ResourceProperties;

/**
 * 
 * A Smartcard represents a Secure Element inside a {@link Device}.Can Store a
 * list of {@link Applet} in it.
 * <p>
 * {@link #findApplet(List)} API is used to find the list of applets loaded
 * inside this Smartcard.Response for finding applets is returned through a
 * callback {@link CSCCallback#onFindAppletResponse(Smartcard, ErrorCodes)}.
 * </p>
 * <p>
 * At a given time only one out of a list of Applets can be in active
 * state.Should call {@link Applet#selectApplet()} to select(activate) an
 * Applet.
 * </p>
 * 
 * @author Srikar created on @date Aug 13, 2014
 */

public class Smartcard implements CSCCallback, ReadWriteCallback {

	enum CommandType {
		FIND_APPLET, SELECT_CARD_MANAGER, GET_CSN, SELECT_APPLET, WRITE
	};

	/** Select card manager apdu JC20. */
	private final static String SELECT_CARD_MANAGER_APDU_JC20 = "00A4040008A000000003000000";

	/** Select card manager apdu JC30. */
	private final static String SELECT_CARD_MANAGER_APDU_JC30 = "00A4040008A000000151000000";

	private final static String GET_CSN = "80CA9F7F00";

	private Device device = null;
	// private int selectAppletIndex = 0;
	// private List<String> listofAppletIDs;

	// Flag to check find applet command is executed?
	// private boolean isFindApplet;

	private HashMap<String, Boolean> findAppletResponseList = null;
	// Applet id of a selected applet.
	// private String appletId;
	// Selected applet inside this SmartCard.
	private Applet selectedApplet = null;

	private CommandType commandType;

	private boolean isJC30Apdu;

	private String CSN;

	private boolean isSelectApplet;

	// TODO lock at smart card level.
	// private ReentrantReadWriteLock lock;

	/**
	 * Constructs a Smart card object.
	 * 
	 * @param device
	 *            Device object in which {@link Smartcard} is resided.
	 */
	public Smartcard(Device device) {
		this.device = device;
		selectCardManager(SELECT_CARD_MANAGER_APDU_JC30);

		// lock = new ReentrantReadWriteLock();

	}

	/**
	 * This method selects this applet out of the list of applets present inside
	 * a {@link Smartcard}.Select applet command will activate communication
	 * with this applet.
	 * 
	 */
	private void processSelectApplet() {
		commandType = CommandType.SELECT_APPLET;
		CommandAPDU commandAPDU = new CommandAPDU(selectedApplet.getAppletID());
		sendApdu(commandAPDU, this);

	}

	private SmartCardMutex mutex = new SmartCardMutex();

	/**
	 * Checks for list of Applets present in SmartCard.List of Applets available
	 * status response can be obtained through
	 * {@link Smartcard#getFindAppletResponseList()}.
	 * 
	 * @see CSCCallback#onFindAppletResponse(Smartcard, ErrorCodes) for applet
	 *      status response.
	 * @param listOfAppletIds
	 *            List of AppletID to find inside a SmartCard.
	 * 
	 */

	public void findApplet(final List<String> listOfAppletIds) {
		synchronized (mutex) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					findAppletsByList(listOfAppletIds);
				}
			}).start();
		}
	}

	private List<String> listOfAppletIds = new ArrayList<String>();
	private String appletId;

	/**
	 * Checks for list of Applets present in SmartCard. Applet present status is
	 * returned as a Hashmap with AppletID as a key and boolean flag as value.
	 * Value true states applet present.
	 * 
	 * @param listOfAppletIds
	 * 
	 * @see CSCCallback#onFindAppletResponse(HashMap) for applet status
	 *      response.
	 * 
	 */

	private synchronized void findAppletsByList(
			final List<String> listOfAppletIds) {
		synchronized (mutex) {
			if (listOfAppletIds != null) {
				this.listOfAppletIds.addAll(listOfAppletIds);
				//Logger.info(listOfAppletIds.toString());
			}
			findAppletByAppletId();
			mutex.setReadyToUse(false);
			try {
				while (!mutex.isReadyToUse())
					mutex.wait();
			} catch (InterruptedException e) {
				//Logger.error("Smart Card", e.getMessage());
			}
		}
	}

	/**
	 * 
	 */
	private void findAppletByAppletId() {
		if (listOfAppletIds.size() > 0) {
			this.appletId = listOfAppletIds.remove(0);
			//Logger.debug("Find Applet:" + appletId);
			commandType = CommandType.FIND_APPLET;
			// String apduLength=ConversionHelper.
			CommandAPDU requestAPDU = new CommandAPDU(appletId);
			//Logger.debug("Find Applet Id:" + appletId);
			if (device != null) {
				// Wait for some time interval (in milliseconds) for
				// every send command operation. Because of BLE side
				// key
				// having limitation that It needs some time gap to
				// send command after received data.
				String sendCommandWaitTimeString = ResourceProperties
						.getInstance().getProperty(
								"SEND_COMMAND_WAIT_TIME_IM_MILLIS");
				if (sendCommandWaitTimeString != null
						&& sendCommandWaitTimeString.matches("\\d+")) {
					Integer sendCommandWaitTime = Integer
							.parseInt(sendCommandWaitTimeString);
					//Logger.info("Wait To Find Applet-->" + sendCommandWaitTime);
				}
				device.sendCommand(requestAPDU.getBytes(), Smartcard.this);
			}
		} else {
			// Sending the list of applet status to the application.
			device.onFindAppletResponse(Smartcard.this,
					ErrorCodes.SUCCESS_RESPONSE);
			mutex.notify();
			mutex.setReadyToUse(true);
		}
	}

	/**
	 * Will send {@link CommandAPDU} to this SmartCard. Response is sent back
	 * through a
	 * {@link CSCCallback#onReceiveResponse(ResponseAPDU, Applet, ErrorCodes)}.
	 * 
	 * @param apdu
	 *            {@link CommandAPDU} instance to write in to SmartCard.
	 * @param btCallback
	 *            {@link CSCCallback} to send back response through
	 *            {@link CSCCallback#onReceiveResponse(APDU, Applet, ErrorCodes)}
	 * @see CommandAPDU
	 * @see CSCCallback
	 */
	public void sendApdu(final CommandAPDU apdu,
			final ReadWriteCallback readWriteCallback) {
		device.sendCommand(apdu.getBytes(), readWriteCallback);
	}

	/**
	 * Takes apdu input in byte[].
	 * 
	 * @param apdu
	 *            apdu data in bytes.
	 * @return byte[] Apdu response in bytes.
	 */
	public byte[] sendApdu(byte[] apduBytes) throws CSCException {
		return device.sendCommand(apduBytes);
	}

	/**
	 * To set a selected Applet in the SmartCard.
	 * 
	 * <br>
	 * Applet selected status is sent back through a
	 * {@link CSCCallback#onSetAppletStatus(Applet, boolean)}.
	 * 
	 * @param applet
	 *            {@link Applet} to be selected.
	 */
	public void setApplet(Applet applet) {
		selectedApplet = applet;
		processSelectApplet();
	}

	@Override
	public void onReceiveResponse(ResponseAPDU response, Applet applet,
			ErrorCodes errorCodes) {
		device.onReceiveResponse(response, applet, errorCodes);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tyfone.csc.communication.CSCCallback#onReceiveResponse(byte[],
	 * com.tyfone.csc.helper.ErrorCodes)
	 */
	@Override
	public void onReceiveResponse(byte[] response, ErrorCodes error) {
		synchronized (mutex) {

			ResponseAPDU responseAPDU;
			switch (commandType) {
			case FIND_APPLET:
				if (findAppletResponseList == null)
					findAppletResponseList = new HashMap<String, Boolean>();
				if (response != null) {
					responseAPDU = new ResponseAPDU(response);
					boolean status = responseAPDU.isSuccessResponse();
					findAppletResponseList.put(appletId, status);
				}
				//Logger.info("On SmartCard, Finding applet " + appletId);
				findAppletByAppletId();

				break;
			case GET_CSN:
				if (response != null) {
					responseAPDU = new ResponseAPDU(response);
					String csn = responseAPDU.getDataString();
					final int firstChar = 26; // counting from 0
					final int lastChar = 42; // counting from 0
					if (csn.length() > lastChar) {
						CSN = csn.substring(firstChar, lastChar);
					}

					//Logger.info("CSN " + CSN);

				}

				break;
			case SELECT_CARD_MANAGER:
				if (response != null) {
					responseAPDU = new ResponseAPDU(response);
					boolean status = responseAPDU.isSuccessResponse();
					//Logger.info("Card manager status " + status);

					if (status) {
						getCardCSN();
					} else if (isJC30Apdu) {
						isJC30Apdu = false;
						selectCardManager(SELECT_CARD_MANAGER_APDU_JC20);

					}
				}
				break;
			case SELECT_APPLET:
				if (response != null) {
					responseAPDU = new ResponseAPDU(response);
					onSetAppletStatus(selectedApplet,
							responseAPDU.isSuccessResponse());
				}

				break;
			case WRITE:
				//Logger.info("On SmartCard, received applet status response");
				// on receive response
				responseAPDU = new ResponseAPDU(response);
				onReceiveResponse(responseAPDU, null, error);

				break;
			}

		}
	}

	/**
	 * Method to retrieve CSN of a SmartCard.
	 * 
	 */
	private void getCardCSN() {
		commandType = CommandType.GET_CSN;
		sendApdu(
				new CommandAPDU(ConversionHelper.hexStringToByteArray(GET_CSN)),
				this);

	}

	/**
	 * Method to select Card manger.
	 * 
	 * @param apdu
	 *            Apdu String to perform select card manger.
	 * @see {@link Smartcard#SELECT_CARD_MANAGER_APDU_JC20}
	 * @see {@link Smartcard#SELECT_CARD_MANAGER_APDU_JC30}
	 */
	private void selectCardManager(final String apdu) {
		commandType = CommandType.SELECT_CARD_MANAGER;
		sendApdu(new CommandAPDU(ConversionHelper.hexStringToByteArray(apdu)),
				this);

	}

	@Override
	public void onConnectionChange(Device device,
			DeviceConnectionState deviceState) {
		//Logger.debug("Smartcatd onConnectionChange");
		synchronized (mutex) {
			mutex.notify();
			mutex.setReadyToUse(true);
		}
	}

	@Override
	public void onUpdateRSSI(float value) {

	}

	@Override
	public void onFindAppletResponse(Smartcard smartcard, ErrorCodes errorCodes) {
	}

	@Override
	public void onSetAppletStatus(Applet applet, boolean selectedState) {
		device.onSetAppletStatus(applet, selectedState);
	}

	/**
	 * This method return the device object that the smart card present.
	 * 
	 * @return device object
	 */
	public Device getDevice() {
		return device;
	}

	/**
	 * Returns Hashmap with appletID as key and status flag as value.Status flag
	 * is true if applet with Key(AppletID) is found else false.
	 * 
	 * 
	 * @return returns applets status map
	 */
	public HashMap<String, Boolean> getFindAppletResponseList() {
		return findAppletResponseList;
	}

	/**
	 * This class is used for locking find smart card command till receiving
	 * Smart card response or connection status change. awq
	 */
	private class SmartCardMutex {
		private boolean readyToUse = false;

		public boolean isReadyToUse() {
			return readyToUse;
		}

		public void setReadyToUse(boolean readyToUse) {
			this.readyToUse = readyToUse;
		}
	}

	/**
	 * Method to retrieve CSN of a SmartCard.
	 * 
	 * @return CSN of this SmartCard.
	 */
	public String getCSN() {
		return CSN;
	}

}

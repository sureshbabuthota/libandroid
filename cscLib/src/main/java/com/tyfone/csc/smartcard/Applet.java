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
package com.tyfone.csc.smartcard;

import java.io.Serializable;

import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.communication.ReadWriteCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ErrorCodes;

/**
 * Applet is an application that is loaded inside a {@link Smartcard}.
 * <p>
 * This is an abstract class to perform applet functionality like writing and
 * reading an APDU.
 * </p>
 * <p>Perform {@link #selectApplet()} to start communicating with this applet. 
 *
 * <p>
 * <b>Note: </b>At a given time only one Applet application will be running in a
 * particular SmartCard.
 *
 * @author Srikar created on @date Aug 13, 2014
 *
 */
public abstract class Applet implements Serializable,ReadWriteCallback {

	private static final long serialVersionUID = 4850786540749055752L;

	private Smartcard mSmartcard;

	// Applet ID of this Applet.
	private String mAppletID;

	private boolean isSelectApplet;

	/**
	 * Constructs {@link Applet} object for a given {@link Smartcard} with a
	 * given appletID.
	 * 
	 * @param smartCard
	 *            {@link Smartcard} in which this applet resides.
	 * @param appletID
	 *            ID of this applet.
	 */
	public Applet(Smartcard smartCard, String appletID) {
		mSmartcard = smartCard;
		mAppletID = appletID;
	}

	/**
	 * Method to send a {@link CommandAPDU} to this {@link Applet}.
	 * 
	 * @param apdu
	 *            {@link CommandAPDU} to be written .
	 */
	public void sendApdu(CommandAPDU apdu) {
		isSelectApplet = false;
		mSmartcard.sendApdu(apdu, this);
	}

	/**
	 * This method sends apdu data in bytes.
	 * 
	 * @param apduBytes apdu command data in bytes.
	 * @return apdu response in byte[].
	 */
	public byte[] sendApdu(byte[] apduBytes) throws CSCException {
		isSelectApplet = false;
		return mSmartcard.sendApdu(apduBytes);
	}

	/**
	 *  Method to select a particular applet out of the list of applets present inside
	 * a {@link Smartcard}. Applet selection status is sent through {@link CSCCallback#onSetAppletStatus(Applet, boolean)}. Select applet command will activate communication
	 * with this applet.
	 * 
	 * <br>
	 * <b> Note:</b> At a given time only one applet will be in active state for
	 * a particular {@link Smartcard}.
	 * 
	 */
	public void selectApplet() {
mSmartcard.setApplet(this);	}

	
	CSCCallback btCallback = new CSCCallback() {

		@Override
		public void onUpdateRSSI(float value) {

		}

		@Override
		public void onReceiveResponse(ResponseAPDU response, Applet applet,
				ErrorCodes errorCodes) {
			if (isSelectApplet && errorCodes == ErrorCodes.SUCCESS_RESPONSE) {
				onSetAppletStatus(Applet.this, response.isSuccessResponse());
			} else
				mSmartcard.onReceiveResponse(response, Applet.this, errorCodes);

		}

		@Override
		public void onFindAppletResponse(Smartcard smartcard,
				ErrorCodes errorCodes) {

		}

		@Override
		public void onConnectionChange(Device device,
				DeviceConnectionState deviceState) {

		}

		@Override
		public void onSetAppletStatus(Applet applet, boolean selectedState) {

			mSmartcard.onSetAppletStatus(applet, selectedState);

		}

		@Override
		public void onReceiveResponse(byte[] response,
				ErrorCodes error) {
			if (error == ErrorCodes.SUCCESS_RESPONSE)
				onReceiveResponse(new ResponseAPDU(response), Applet.this,
						error);
			else
				onReceiveResponse(null, Applet.this, error);
		}
	};

	/**
	 * This method returns the Smartcard object for selected applet.
	 * 
	 * @return Smartcard obejct.
	 */
	public Smartcard getmSmartcard() {
		return mSmartcard;
	}

	/**
	 * This method returns selected applet ID.
	 * 
	 * @return applet ID.
	 */
	public String getAppletID() {
		return mAppletID;
	}
}

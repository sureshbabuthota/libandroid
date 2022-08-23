package com.tyfone.csc;

import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * @author Srikar created on @date Aug 18, 2014
 */
public class LsecurityApplet extends Applet {

	public LsecurityApplet(Smartcard smartCard, String appletID) {
		super(smartCard, appletID);
 	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 737491285996966102L;

	@Override
	public void onReceiveResponse(byte[] response, ErrorCodes error) {

	}

	@Override
	public void onReceiveResponse(ResponseAPDU response, Applet applet, ErrorCodes error) {

	}
}

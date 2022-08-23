package com.tyfone.csc.communication;

import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.CommandAPDU;
import com.tyfone.csc.smartcard.ResponseAPDU;

public interface ReadWriteCallback {
	/**
	 * To be called once a response for a command is received.
	 * 
	 * @param response
	 *            response in bytes.
	 * 
	 * @param error
	 *            error code for this response.
	 * 
	 * 
	 */
	void onReceiveResponse(byte[] response, ErrorCodes error);

	/**
	 * To  be called once the response for a command APDU is received.
	 * 
	 * @param response
	 *            instance of {@link ResponseAPDU}.
	 * @param error
	 *            error code for this response.
	 * 
	 * @see Applet#sendApdu(CommandAPDU)
	 * @see CommandAPDU
	 * @see ResponseAPDU
	 */
	void onReceiveResponse(ResponseAPDU response, Applet applet,
						   ErrorCodes error);




}

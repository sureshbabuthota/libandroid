/*
 * @author Srikar created on @date Aug 15, 2014
 *
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc.smartcard;

import java.util.Arrays;

import com.tyfone.csc.helper.ConversionHelper;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.resource.ResourceProperties;

/**
 * A response APDU as defined in ISO/IEC 7816-4. It consists of a conditional
 * body and a two byte trailer. Trailer indicates the status of a response (in
 * bytes).
 * 
 * <pre>
 * Example of an apdu response :
 * 
 *   |A0098777|9000  |
 *   |Data    |Status|
 * </pre>
 * 
 * @see ComandAPDU
 *
 * 
 */
public final class ResponseAPDU implements java.io.Serializable {

	private static final long serialVersionUID = 6962744978375594225L;

	private byte[] apdu;

	/**
	 * Constructs a ResponseAPDU from a byte array containing the complete APDU
	 * contents.
	 *
	 * <p>
	 * Note that the byte array is cloned to protect against subsequent
	 * modification.
	 * </p>
	 *
	 * @param apdu
	 *            the complete response APDU.
	 *
	 * @throws NullPointerException
	 *             if apdu is null.
	 * @throws IllegalArgumentException
	 *             if <code>apdu.length</code> is less than 2.
	 */
	public ResponseAPDU(byte[] apdu) {
		apdu = apdu.clone();
		check(apdu);
		this.apdu = apdu;
	}

	/**
	 * This method check for apdu is valid or not. If not throws an
	 * {@link IllegalArgumentException}
	 * 
	 * @param apdu
	 *            apdu in bytes.
	 */
	private static void check(byte[] apdu) {
		if (apdu.length < 2) {
			throw new IllegalArgumentException(ResourceProperties.getInstance()
					.getProperty(ErrorCodes.APDU_INVALID.getMessage()));
		}
	}

	/**
	 * Returns the number of data bytes in the response body (Nr) or 0 if this
	 * APDU has no body. This call is equivalent to
	 * <code>getData().length</code>.
	 *
	 * @return the number of data bytes in the response body or 0 if this APDU
	 *         has no body.
	 */
	public int getNr() {
		return apdu.length - 2;
	}

	/**
	 * Returns a copy of the data bytes in the response body. If this APDU has
	 * no body, this method returns a byte array with a length of zero.
	 * <p>
	 * <b>Note: </b> Return value excludes status bytes.
	 * </p>
	 * 
	 * @return a copy of the data bytes in the response body or the empty byte
	 *         array if this APDU has no body.
	 */
	public byte[] getData() {
		byte[] data = new byte[apdu.length - 2];
		System.arraycopy(apdu, 0, data, 0, data.length);
		return data;
	}

	/**
	 * Returns hex string of data bytes in the response body. If this APDU has
	 * no body, this method returns empty string.
	 *
	 * @return string of data bytes in the response body or empty string if this
	 *         APDU has no body.
	 */
	public String getDataString() {

		return ConversionHelper.byteArrayToHex(getData());
	}

	/**
	 * Returns the value of the status byte SW1 as a value between 0 and 255.
	 *
	 * @return the value of the status byte SW1 as a value between 0 and 255.
	 */
	private int getSW1() {
		// Byte to int conversion
		return apdu[apdu.length - 2] & 0xff;
	}

	/**
	 * Returns the value of the status byte SW2 as a value between 0 and 255.
	 *
	 * @return the value of the status byte SW2 as a value between 0 and 255.
	 */
	private int getSW2() {
		// Byte to int conversion
		return apdu[apdu.length - 1] & 0xff;
	}

	/**
	 * Returns the value of the status bytes SW1 and SW2 as a single status word
	 * SW. It is defined as <code>(getSW1() << 8) | getSW2()</code>.
	 *
	 * @return the value of the status word SW.
	 */
	public int getSW() {
		return (getSW1() << 8) | getSW2();
	}

	/**
	 * Returns the hex value of the status bytes.
	 *
	 * @return the hex value of the status bytes.
	 */
	public String getStatus() {
		return ConversionHelper.getHexFromInt(getSW(), 2);
	}

	/**
	 * Returns a copy of the bytes in this APDU. Raw response with status bytes.
	 *
	 * @return a copy of the bytes in this APDU.
	 */
	public byte[] getBytes() {
		return apdu.clone();
	}

	/**
	 * Returns a string representation of this response APDU.
	 *
	 * @return a String representation of this response APDU.
	 */
	public String toString() {
		return "ResponseAPDU: " + apdu.length + " bytes, SW="
				+ Integer.toHexString(getSW());
	}

	/**
	 * Compares the specified object with this response APDU for equality.
	 * Returns true if the given object is also a ResponseAPDU and its bytes are
	 * identical to the bytes in this ResponseAPDU.
	 *
	 * @param obj
	 *            the object to be compared for equality with this response
	 *            APDU.
	 * @return true if the specified object is equal to this response APDU
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ResponseAPDU == false) {
			return false;
		}
		ResponseAPDU other = (ResponseAPDU) obj;
		return Arrays.equals(this.apdu, other.apdu);
	}

	/**
	 * Returns the hash code value for this response APDU.
	 *
	 * @return the hash code value for this response APDU.
	 */
	public int hashCode() {
		return Arrays.hashCode(apdu);
	}

	/**
	 * Checks the status of this Response APDU. Returns true if hex value of
	 * status bytes is equal to "9000".
	 * 
	 * @see ResponseAPDU#getStatus() to get status of this response APDU.
	 * @return true if hex value of status bytes is equal to "9000".
	 * 
	 */
	public boolean isSuccessResponse() {
		String status = getStatus();
		return status.equals("9000");
	}

}

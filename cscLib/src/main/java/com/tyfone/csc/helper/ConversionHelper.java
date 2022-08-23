/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.helper;

/**
 * This class performs all conversion helper functions like converting bytes to
 * hex string, hex to int etc.
 * 
 * @author Srikar created on Aug 6, 2014
 */
public class ConversionHelper {

	/**
	 * This method converts byte array to hex string.
	 * 
	 * @param bytes
	 *            bytes to convert to hex string.
	 * @return hex string for given bytes.
	 */

	public static String byteArrayToHex(byte[] bytes) {
		if(bytes==null)
			return "";
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString();
	}

	/**
	 * This method returns a hex string for a given input decimal.
	 *
	 * @param decimal
	 *            the decimal value to convert to hex string.
	 * @param outByteLength
	 *            number of bytes to represent a decimal value.
	 * @return hex string value of the decimal.
	 */
	public static String getHexFromInt(int decimal, final int outByteLength) {
		String digits = "0123456789ABCDEF";
		if (decimal == 0)
			return "0";
		String hex = "";
		while (decimal > 0) {
			int digit = decimal % 16; // rightmost digit
			hex = digits.charAt(digit) + hex; // string concatenation
			decimal = decimal / 16;
		}
		switch (outByteLength) {
		case 1:
			if (hex.length() == 1)
				return "0" + hex;
			else
				return hex;
		case 2:
			if (hex.length() == 3)
				return "0" + hex;
			else if (hex.length() == 2)
				return "00" + hex;
			else if (hex.length() == 1)
				return "000" + hex;
			else
				return hex;

		default:
			return hex;
		}
	}

	// This method will convert ByteArray to Hex

	/**
	 * Converts hex string to byte array.
	 *
	 * @param hexString
	 *            hex string to convert to bytes.
	 * @return bytes of a given hex string.
	 */

	public static byte[] hexStringToByteArray(final String hexString) {
		if (hexString.length() % 2 != 0)
			throw new IllegalArgumentException(
					"Input string must contain an even number of characters");

		final byte result[] = new byte[hexString.length() / 2];
		final char enc[] = hexString.toCharArray();
		for (int i = 0; i < enc.length; i += 2) {
			StringBuilder curr = new StringBuilder(2);
			curr.append(enc[i]).append(enc[i + 1]);
			result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
		}
		return result;
	}

	/**
	 * Gets the APDU packets for a given APDU.
	 *
	 * @param APDU
	 *            the APDU.
	 * @return APDU packets.
	 */
	public static synchronized String[] getApduPackets(String apdu) {

		int startPacket = 0;
		int endPacket = 40;
		String formattedApdu = new String(ConversionHelper.getHexFromInt(
				apdu.length() / 2, 2)
				+ apdu);
		// String formattedApdu = new String(apdu);

		int formattedApduLength = formattedApdu.length();
		int noOfPackets = (apdu.length() + 4) / 40;
		if (formattedApduLength % 40 != 0) {
			noOfPackets = noOfPackets + 1;
		}
		String[] apduPackets = new String[noOfPackets];
		for (int i = 0; i < noOfPackets; i++) {
			if (endPacket > formattedApduLength)
				endPacket = formattedApduLength;
			apduPackets[i] = formattedApdu.substring(startPacket, endPacket);
			startPacket = endPacket;
			endPacket = endPacket + 40;
		}
		return apduPackets;
	}

	/**
	 * Gets the packets count for a given APDU.
	 *
	 * @param APDU
	 *            the APDU byte array.
	 * @return APDU packets count.
	 */
	public static synchronized int getApduPacketsCount(byte[] apdu) {
		return (apdu.length / 20) + 1;
	}

	/**
	 * Gets integer from bytes.
	 * 
	 * @param bytes
	 *            bytes to convert to integer.
	 * @return integer value of bytes.
	 */
	public static int byteArrayToInt(byte[] bytes) {
		int result = 0;
		for (int i = 0; i < 4; i++) {
			result = (result << 8) - Byte.MIN_VALUE + (int) bytes[i];
		}
		return result;
	}

	/**
	 * This method prepends length bytes to a given byte array.
	 * 
	 * <br>
	 * For example if input is {1,99,0,55} then output will be {0,4,1,99,0,55},
	 * as the length of input bytes is 4.
	 * 
	 * @param commandBytes
	 *            command bytes to write.
	 * @return output byte array with prepended length bytes.
	 */
	public static byte[] prependLengthBytes(byte[] commandBytes) {

		int apduLength = commandBytes.length;
		// Allocating output byte array to accommodate length bytes and apdu
		// bytes.
		byte[] outPutBytes = new byte[apduLength + 2];

		// Storing length(apdu bytes) in two bytes.
		outPutBytes[0] = (byte) ((apduLength >> 8) & 0xff);
		outPutBytes[1] = (byte) (apduLength & 0xff);

		// Copying apdu bytes into output bytes.
		System.arraycopy(commandBytes, 0, outPutBytes, 2, commandBytes.length);
		return outPutBytes;
	}

}

/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.exception;

import com.tyfone.csc.helper.ErrorCodes;

/**
 * This class represents recoverable exceptions. When exceptions are thrown, they
 * may be caught by application code.
 * 
 * <p>
 * This class handles CSC library exceptions and provides error information. Using
 * this class, application can get the information about severity of exception,
 * error code and error description.
 * </p>
 * 
 * <p>
 * The first 2 characters of error key is Module information, next 2 is Severity
 * and last four are Error code.
 * </p>
 * 
 * @author <b>Suryaprakash</b> created on Jul 30, 2014 7:59:25 PM
 *
 */
@SuppressWarnings("serial")
public class CSCException extends Exception {

	// Initialize exception message variable
	private String message = null;

	// initialize error code enum object
	public ErrorCodes what;

	// initialize errorkey
	private String errorKey;
	public CSCException(String error) {
 	}

	public CSCException(ErrorCodes errorCode) {
		super(errorCode.getMessage());
		// assign error message to the message objected received from ErrorCodes
		this.message = errorCode.getMessage();
		this.what = errorCode;
		// get the Error key from ErrorCodes. Error key holds Module, Severity
		// and error code of the particular exception
		this.errorKey = errorCode.getErrorKey();
	}
	
	/**
	 * Creates a new exception using one of the error codes, wrapping a lower level exception.
	 * 
	 * @param errorCode
	 *           defined {@link ErrorCodes}. 
	 * @param throwable
	 *            lower level exception to wrap.
	 */
	public CSCException(ErrorCodes errorCode, Throwable throwable) {
		super(errorCode.getMessage(), throwable);
		// assign error message to the message objected received from ErrorCodes
				this.message = errorCode.getMessage();
				this.what = errorCode;

				// get the Error key from ErrorCodes. Error key holds Module, Severity
				// and error code of the particular exception
				this.errorKey = errorCode.getErrorKey();

	}


	@Override
	public String toString() {
		return message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	/**
	 * This method retrieves error code of a particular exception.
	 * 
	 * @return  the error code of particular exception.
	 */
	public long getErrorCode() {
		// Get the error code from error key and return
		return Long.parseLong(errorKey.substring(errorKey.length() - 4), 16);
	}

	/**
	 * This method retrieves severity of the exception. For example
	 * severity is 0x01=HIGH, 0x02=MEDIUM and 0x03=LOW.
	 * 
	 * @return  severity of exception.
	 */
	public int getSeverity() {
		// get the severity of exception from error key and return
		return Integer.parseInt(errorKey.substring(2, 4), 16);
	}

	/**
	 * This  returns module id of an exception. 
	 * 
	 * @return module segregation id.
	 */
	public int getSegregationID() {
		// get the segregation id from error key and return it. It will
		// indicates the module that was thrown an exception
		return Integer.parseInt(errorKey.substring(0, 2), 16);
	}
}

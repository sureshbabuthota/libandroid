/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.log;

import com.tyfone.csc.CSC;
import com.tyfone.csc.LogLevel;

/**
 * 
 * <p>
 * This class is used to send CSC Library level logs to the application which
 * uses the CSC library.
 * </p>
 * 
 * <p>
 * <strong>Note: </strong> The type of logs sent to the application in
 * {@link LogCallback} depends on the {@linkplain LogLevel} set by the
 * application.
 * </p>
 * 
 * @author <b>Suryaprakash</b> on Jul 30, 2014 5:55:44 PM </br>
 * 
 */
public class CSCLogger {

	/** value of {@link LogLevel#VERBOSE} in bytes. */
	private static int VERBOSE = LogLevel.VERBOSE.getLogLevelValue();

	/** value of {@link LogLevel#DEBUG} in bytes. */
	private static int DEBUG = LogLevel.DEBUG.getLogLevelValue();

	/** value of {@link LogLevel#INFO} in bytes. */
	private static int INFO = LogLevel.INFO.getLogLevelValue();

	/** value of {@link LogLevel#WARN} in bytes. */
	private static int WARN = LogLevel.WARN.getLogLevelValue();

	/** value of {@link LogLevel#ERROR} in bytes. */
	private static int ERROR = LogLevel.ERROR.getLogLevelValue();

	/** value of {@link LogLevel#ERROR} in bytes. */
	private static int CRITICAL = LogLevel.CRITICAL.getLogLevelValue();

	/** {@link CSCLoggerCallback} instance */
	private static CSCLoggerCallback cscLoggerCallback;

	/**
	 * Sets the CSCLoggerCallback
	 * 
	 * @param callback
	 *            CSCLoggerCallback
	 * 
	 */
	public static void setCSCLoggerCallback(CSCLoggerCallback callback) {
		cscLoggerCallback = callback;
	}

	/**
	 * Sends a verbose log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */

	public static void verbose(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & VERBOSE) == VERBOSE) {
			sendLogs(messageId, tag, LogLevel.VERBOSE, methodName, logParams);
		}
	}

	/**
	 * Sends a debug log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */
	public static void debug(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & DEBUG) == DEBUG) {
			sendLogs(messageId, tag, LogLevel.DEBUG, methodName, logParams);
		}
	}

	/**
	 * Sends info log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */
	public static void info(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & INFO) == INFO) {
			sendLogs(messageId, tag, LogLevel.INFO, methodName, logParams);
		}
	}

	/**
	 * Sends a warning log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */
	public static void warn(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & WARN) == WARN) {
			sendLogs(messageId, tag, LogLevel.WARN, methodName, logParams);
		}
	}

	/**
	 * Sends a error log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */
	public static void error(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & ERROR) == ERROR) {
			sendLogs(messageId, tag, LogLevel.ERROR, methodName, logParams);
		}
	}

	/**
	 * Sends a critical log message to the app
	 * 
	 * @param messageId
	 *            the messageId
	 * @param tag
	 *            Used to identify the source of a log message.
	 * @param methodName
	 *            the name of the method from which this function is called
	 * @param logParams
	 *            the log params array
	 */
	public static void critical(int messageId, String tag, String methodName,
			String[] logParams) {
		if ((CSC.getLogLevel().getLogLevelValue() & CRITICAL) == CRITICAL) {
			sendLogs(messageId, tag, LogLevel.CRITICAL, methodName, logParams);
		}
	}

	/**
	 * Sends a log message to the app
	 * 
	 * 
	 * @param messageId
	 *            the message id
	 * @param tag
	 *            the tag
	 * @param loglevel
	 *            the loglevel
	 * @param methodName
	 *            the method name
	 * @param logParams
	 *            the log params
	 */
	private static synchronized void sendLogs(int messageId, String tag,
			LogLevel loglevel, String methodName, String[] logParams) {
		if (cscLoggerCallback != null) {
			cscLoggerCallback.onLogReceived((new LogDetails(messageId, tag,
					loglevel, methodName, Thread.currentThread().getId(),
					android.os.Process.myPid(), logParams)));
		}
	}

	/**
	 * Callback Interface Class to send the logs to the application using the
	 * CSCLibrary
	 **/
	public interface CSCLoggerCallback {
		public void onLogReceived(LogDetails logDetails);
	}
}
/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc;

/**
 * {@link Enum} to define log levels used in library.
 * 
 * @author Suryaprakash created on Aug 23, 2014 5:35:30 PM
 */
public enum LogLevel {

	/**
	 * If log level is NO_LOGS, CSCLibrary will not send any logs to the
	 * application
	 */
	NO_LOGS(0b000000),
	/**
	 * If log level is CRITICAL, CSCLibrary will send all CRITICAL logs to the
	 * application
	 */
	CRITICAL(0b000001),
	/**
	 * If log level is ERROR, CSCLibrary will send all ERROR logs to the
	 * application
	 */
	ERROR(0b000011),

	/**
	 * If log level is WARN, CSCLibrary will send all WARN logs to the
	 * application
	 */
	WARN(0b000111),
	/**
	 * If log level is INFO, CSCLibrary will send all INFO logs to the
	 * application
	 */
	INFO(0b001111),
	/**
	 * If log level is DEBUG, CSCLibrary will send all DEBUG logs to the
	 * application
	 */
	DEBUG(0b011111),
	/**
	 * If log level is VERBOSE, CSCLibrary will send all VERBOSE logs to the
	 * application
	 */
	VERBOSE(0b111111);

	/***/
	private int logLevelValue;

	/**
	 * gets the LogLevel value in binary
	 * 
	 * @return LogLevel value in binary
	 */
	public int getLogLevelValue() {
		return logLevelValue;
	}

	/** Constructor */
	private LogLevel(final int logLevelValue) {
		this.logLevelValue = logLevelValue;
	}

}
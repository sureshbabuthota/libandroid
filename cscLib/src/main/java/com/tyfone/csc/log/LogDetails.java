package com.tyfone.csc.log;

import com.tyfone.csc.LogLevel;
import com.tyfone.csc.resource.ResourceProperties;

/**
 * The Class LogDetails.
 */
public class LogDetails {

	/** The messageID. */
	private int messageID;

	/** The class OR tag. */
	private String cls;

	/** The method. */
	private String method;

	/** The log level. */
	private LogLevel logLevel;

	/** The thread id. */
	private long threadInfo;

	/** The process id. */
	private int processInfo;

	/** The log parameters. */
	private String[] parameters;

	/**
	 * Instantiates a new log details.
	 * 
	 * @param messageID
	 *            the messageID
	 * @param cls
	 *            the class OR tag
	 * @param logLevel
	 *            the log level
	 * @param method
	 *            the method
	 * @param threadInfo
	 *            the thread Info
	 * @param processInfo
	 *            the process Info
	 * @param parameters
	 *            the log parameters
	 */
	public LogDetails(int messageID, String cls, LogLevel logLevel,
			String method, long threadInfo, int processInfo, String[] parameters) {
		super();
		this.messageID = messageID;
		this.cls = cls;
		this.logLevel = logLevel;
		this.method = method;
		this.threadInfo = threadInfo;
		this.processInfo = processInfo;
		this.parameters = parameters;
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public int getMessageID() {
		return messageID;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}

	/**
	 * Gets the class name.
	 * 
	 * @return the class name.
	 */
	public String getTag() {
		return cls;
	}

	/**
	 * Sets the class name..
	 * 
	 * @param cls
	 *            the class name.
	 */
	public void setTag(String cls) {
		this.cls = cls;
	}

	/**
	 * Gets the log level.
	 * 
	 * @return the log level
	 */
	public LogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * Sets the log level.
	 * 
	 * @param logLevel
	 *            the new log level
	 */
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Sets the method.
	 * 
	 * @param method
	 *            the new method
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Gets the thread id.
	 * 
	 * @return the thread id
	 */
	public long getThreadInfo() {
		return threadInfo;
	}

	/**
	 * Sets the thread id.
	 * 
	 * @param threadID
	 *            the new thread id
	 */
	public void setThreadInfo(long threadInfo) {
		this.threadInfo = threadInfo;
	}

	/**
	 * Gets the process id.
	 * 
	 * @return the process id
	 */
	public int getProcessInfo() {
		return processInfo;
	}

	/**
	 * Sets the process id.
	 * 
	 * @param processID
	 *            the new process id
	 */
	public void setProcessInfo(int processInfo) {
		this.processInfo = processInfo;
	}

	/**
	 * Gets the log parameters.
	 * 
	 * @return the log parameters
	 */
	public String[] getParameters() {
		return parameters;
	}

	/**
	 * Sets the log parameters.
	 * 
	 * @param parameters
	 *            the new log parameters
	 */
	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get log message
	 * 
	 * @return logMessage
	 */
	public String getLogMessage() {

		return getFormattedMessage();
	}

	/**
	 * Gets the formatted log message.
	 * 
	 */
	private String getFormattedMessage() {
		String message = ResourceProperties.getInstance().getProperty(
				Integer.valueOf(messageID).toString());
		if (message != null) {
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					message = message.replaceFirst("%s", parameters[i]);
				}
			}
			return message;
		}
		return "Unable to retrieve message from resource file coressponding to Message ID-."
				+ messageID;
	}
}

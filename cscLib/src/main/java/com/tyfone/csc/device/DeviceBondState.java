/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.device;

/**
 * 
 * This enum represents the bonded state of a remote device. 
 * 
 * @author Srikar created on Aug 8, 2014
 */
public enum DeviceBondState {
	/**
	 * Indicates the remote device is not bonded (paired).
	 * <p>
	 * There is no shared link key with the remote device.</p>
	 */
	BOND_NONE,
	/**
	 * Indicates bonding (pairing) is in progress with a remote device.
	 */
	BOND_BONDING,

	/**
	 * Indicates the remote device is bonded (paired).
	 * <p>
	 * A shared link key exists locally for the remote device, so communication
	 * can be authenticated and encrypted.
	 * <p>
	 * <i>Being bonded (paired) with a remote device does not necessarily mean
	 * the device is currently connected. It just means that the pending
	 * procedure was completed at some earlier time, and the link key is still
	 * stored locally, ready to use on the next connection. </i>
	 */
	BOND_BONDED;
}

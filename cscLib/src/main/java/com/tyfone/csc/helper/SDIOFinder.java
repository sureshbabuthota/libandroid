/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */

package com.tyfone.csc.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import com.tyfone.csc.exception.CSCException;

import android.util.Log;


/**
 * This class is used to find the path of Tyfone microSD card.
 * 
 */

public class SDIOFinder {

	/**
	 * Tag(Class name) for logging
	 */
	private final static String TAG = SDIOFinder.class.getSimpleName();

	/**
	 * Card ID found in cards manufactured by Tyfone, Inc.
	 */
	private final static String TYFONE_CID_SIGNATURE = "4e2f41202010";

	/**
	 * Gives the path where a Tyfone Connected Smart Card is mounted. <br>
	 * <br>
	 * Background: <br>
	 * Each SD (MMC) card shows up as a block device on the filesystem such as
	 * /sys/block/mmcblk# <br>
	 * Underneath the folder, there are a bunch of files with identifying
	 * information, such as card ID and manufacturer ID <br>
	 * e.g. /sys/block/mmcblk1/device/cid <br>
	 * <br>
	 * Here are the steps:
	 * <ol>
	 * <li>We will read the card ID for each mmcblk device, and see if it is a
	 * Tyfone card ID.
	 * <li>If we find a match, read the major:minor number for the first
	 * partition <br>
	 * (e.g. /sys/block/mmcblk1/mmcblk1p1/dev)
	 * <li>Read the mount table (/proc/mounts), find the device number, and read
	 * the mount point <br>
	 * (e.g. /dev/block/vold/179:17 /storage/extSdCard vfat rw...)
	 * </ol>
	 * 
	 * @return The path where the Tyfone Connected Smart Card is mounted
	 * @throws CSCException
	 *             If no Connected Smart Card is found, or an I/O exception 
	 *             occurs.
	 */
	public static String findConnectedSmartCard() throws CSCException {

		try {
			/*
			 * For each /sys/block/mmcblk# block device, read device/cid, check
			 * against known Tyfone CID
			 */

			File tyfoneMMCBlockDevice = SDIOFinder.getTyfoneMMCBlockDevice();

			if (tyfoneMMCBlockDevice == null) {
				throw new CSCException(ErrorCodes.ERROR_DEVICE_SDIO_NOT_FOUND);
			}

			/* Read mmcblk#p1/dev */

			String deviceNumbers = SDIOFinder
					.getDeviceNumbers(tyfoneMMCBlockDevice);

			/*
			 * read mount table from /proc/mounts/, find the device numbers in
			 * the file, get the line
			 */

			String mountEntry = SDIOFinder.getMountEntry(deviceNumbers);

			/*
			 * parse the entry from the mount table, get the path where the
			 * device is mounted
			 */

			String mountPoint = SDIOFinder.getMountPoint(mountEntry);

			return mountPoint;
		} catch (IOException e) {
			throw new CSCException(ErrorCodes.ERROR_DEVICE_SDIO_NOT_FOUND, e);
		}
	}

	/**
	 * This function gets all mmcblock devices on the phone and checks for
	 * mmcblock devices with Tyfone CID(Card manufacturer ID). Each SD (MMC)
	 * card shows up as a block device on the filesystem such as
	 * /sys/block/mmcblk#
	 * 
	 * @return File pointing to Tyfone mmcblock device.
	 * 
	 * @throws IOException
	 *             if an error occured while trying to read CID for device
	 */
	private static File getTyfoneMMCBlockDevice() throws IOException {

		File[] mmcBlockDevices = getMMCBlockDevices();

		for (File mmcBlockDevice : mmcBlockDevices) {

			try {
				// read device/cid, check against known Tyfone CID
				String cid = getCID(mmcBlockDevice);
				if (isTyfoneCID(cid)) {
					return mmcBlockDevice;
				}
			} catch (IOException e) {
				Log.e(TAG,
						"An exception occurred while trying to read CID for device "
								+ mmcBlockDevice.getName());
			}
		}
		return null;
	}

	/**
	 * This function gets all mmcblock devices on the phone. Each SD (MMC) card
	 * shows up as a block device on the filesystem such as /sys/block/mmcblk#
	 * 
	 * @return array of files pointing to all mmcblock devices.
	 */
	private static File[] getMMCBlockDevices() {

		// find folders named /sys/block/mmcblk#

		File blockDeviceFolder = new File("/sys/block");
		File[] blockDevices = blockDeviceFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				final String prefix = "mmcblk";
				return (name.startsWith(prefix) && (name.length() == prefix
						.length() + 1));
			}
		});

		if (blockDevices.length == 0) {
			Log.e(TAG, "No SD found");
			return null;
		}
		return blockDevices;
	}

	
	/**
	 * This function gets the card manufacturer ID (CID) from the given file
	 * pointing to a mmcblock device
	 * 
	 * @return CID of mmcblock device
	 */
	private static String getCID(File mmcBlockDevice) throws IOException {
		// read device/cid
		String cid = "";
		File cidFile = new File(mmcBlockDevice, "device/cid");
		try {
			cid = readOneLineFile(cidFile);
		} catch (IOException e) {
			String message = "Error reading CID from " + cidFile + ": "
					+ e.getMessage();
			Log.e(TAG, message, e);
			throw new IOException(message, e);
		}

		return cid;
	}

	
	/**
	 * This function compares the given card manufacturer ID (CID) with the
	 * standard Tyfone CID.
	 * 
	 * @return true if the given CID mathes with the standard Tyfone CID OR
	 *         false if the CID doesnot match.
	 */

	private static boolean isTyfoneCID(String cid) {

		/* Check for a series of bytes in the middle of the CID. */

		/* 0020204e2f4120201083e9ee7600d300 */
		/* 7957524e2f412020105df6832b00ca00 */
		/* ??????4e2f41202010?????????????? */

		if (cid == null) {
			return false;
		}

		if (cid.contains(TYFONE_CID_SIGNATURE)) {
			Log.d(TAG, "cid " + cid + " contains Tyfone Signature\n          "
					+ TYFONE_CID_SIGNATURE);
			return true;
		} else {
			Log.d(TAG, "cid " + cid + " does not contain Tyfone Signature "
					+ TYFONE_CID_SIGNATURE);
			return false;
		}
	}

	/**
	 * This function reads major:minor device numbers from mmcblock device. The
	 * minor device numbers are used to distinguish between different devices
	 * and their controllers. The major number is actually the offset into the
	 * kernel's device driver table, which tells the kernel what kind of device
	 * it is (whether it is a hard disk or a serial terminal).The minor number
	 * tells the kernel special characteristics of the device to be accessed.
	 * 
	 * @return major:minor device numbers of the mmcblock device.
	 * 
	 */
	private static String getDeviceNumbers(File mmcBlockDevice)
			throws IOException {
		/*
		 * The first partition will show up as a subdirectory such as
		 * /sys/block/mmcblk0/mmcblk0p1
		 */
		String firstPartitionName = mmcBlockDevice.getName() + "p1";
		File firstPartitionBlockDevice = new File(mmcBlockDevice,
				firstPartitionName);

		/*
		 * The major:minor device numbers will be in the dev file, e.g.
		 * /sys/block/mmcblk0/mmcblk0p1/dev
		 */
		File devFile = new File(firstPartitionBlockDevice, "dev");

		String deviceNumbers = null;
		try {
			deviceNumbers = readOneLineFile(devFile);
		} catch (IOException e) {
			String message = "Error reading major:minor device numbers from "
					+ devFile + ": " + e.getMessage();
			Log.e(TAG, message, e);
			throw new IOException(message, e);
		}

		return deviceNumbers;
	}

	/**
	 * This function gets the mount entry.
	 * 
	 * mmcblock devices are automatically mounted when you boot up your system.
	 * These are listed in the /proc/mounts file as a mount entry. This file
	 * provides a list of all mounts in use by the system.
	 * 
	 * @return mount entry of the mmcblock device.
	 * 
	 */
	private static String getMountEntry(String deviceNumbers)
			throws IOException {

		File mounts = new File("proc/mounts");

		/*
		 * Read the mounts file and search for the line containing the device
		 * numbers. Append a space to the device numbers, so that 179:1 doesn't
		 * match 179:17
		 */
		String stringToFind = deviceNumbers + " ";

		/*
		 * Some versions of Android mount the card a second time at
		 * /mnt/secure/asec. You can't access the card at that path due to
		 * permissions
		 */
		String stringToAvoid = "asec";

		BufferedReader br = null;
		String mountEntry = null;

		try {
			br = new BufferedReader(new FileReader(mounts));

			String line;
			while ((line = br.readLine()) != null) {

				if (line.contains(stringToFind)
						&& !line.contains(stringToAvoid)) {
					mountEntry = line;
					Log.v(TAG, "Found Mount Entry: " + mountEntry);
					break;
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}

		if (mountEntry == null) {
			throw new IOException("Unable to find device (" + deviceNumbers
					+ ") in mount table.\n" + "Last line is ("
					+ readLastLineOfFile(mounts) + ")");
		}

		return mountEntry;
	}

	/**
	 * This function gets the mount point from the mount entry string.
	 * 
	 * A mount point is a directory to access your data (files and folders)
	 * which is stored in your disks. If you want to access a file or create a
	 * file on a storage device you will need to know its mount point; you
	 * cannot save a file to a device’s path.The first column specifies the
	 * device that is mounted, the second column reveals the mount point.
	 * 
	 * @return mount point of the mmcblock device.
	 * 
	 */
	private static String getMountPoint(String mountEntry) throws IOException {
		String[] tokens = mountEntry.split(" ");
		/*
		 * Technically, could be multiple spaces, but only single space is
		 * expected
		 */
		if (tokens == null || tokens.length < 2) {
			throw new IOException("Unable to parse mount table entry: "
					+ mountEntry);
		}
		return tokens[1];
	}

	/************************************ Utility Functions ******************************************/

	/**
	 * This function reads one line from the given file.
	 * 
	 * @return the contents of the line or null if no characters were read
	 *         before the end of the reader has been reached.
	 * 
	 */
	private static String readOneLineFile(File file) throws IOException {
		BufferedReader br = null;
		String line = null;
		try {
			br = new BufferedReader(new FileReader(file));
			line = br.readLine();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return line;
	}

	/**
	 * This function reads the last line from the given file.
	 * 
	 * @return the contents of the last line or null if no characters were read
	 *         before the end of the reader has been reached.
	 * 
	 */
	private static String readLastLineOfFile(File file) throws IOException {
		BufferedReader br = null;
		String lastLine = null;
		try {
			br = new BufferedReader(new FileReader(file));

			String line;
			while ((line = br.readLine()) != null) {
				lastLine = line;
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return lastLine;
	}

}

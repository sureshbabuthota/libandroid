/**
 * 
 */
package com.tyfone.csc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.os.HandlerThread;
import android.test.AndroidTestCase;
import android.util.Log;
import com.tyfone.csc.communication.CSCCallback;
import com.tyfone.csc.device.Device;
import com.tyfone.csc.device.DeviceConnectionState;
import com.tyfone.csc.device.DeviceDetails;
import com.tyfone.csc.device.DeviceType;
import com.tyfone.csc.device.ScanListener;
import com.tyfone.csc.exception.CSCException;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * Created by author <b>Srikar</b> on Aug 6, 2014 4:46:46 PM </br>
 * Project: CSCLibraryJUnitTest </br>
 *
 */
public class CSCLibraryTest extends AndroidTestCase
		 {
			 public final String ECHO_APPLET_ID = "4563686F4170706C6574";
			 private final String EXTENDED_LEN_APPLET_ID = "747973616D706C6563";
			 private ArrayList<String> findAppletsList1;

			 /**
	 * @param activityClass
	 */
	public CSCLibraryTest() {
		}

	private CSC cscInstance;
	private ScanHandler mHandlerThread;
	ScanListenerImpl scanListenerImpl;
	private Semaphore semaphore, semaphoreConn, semaphoreSendAsynComm;
	private boolean isScanLockActive = false, isDeviceFound = false,
			isConnLockActive = false, isFindAppletsLockActive = false;
	private DeviceDetails deviceDetails;

	private Device conenctedDevice;
	private ConnectionHandler mConnectionHandler;
	private FindAppletHandler mFindAppletHandler;
	private Smartcard mSmartCard;
	private static final String TAG="CSClibraryUnitTest";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		/*
		 * prepare to send key events to the app under test by turning off touch
		 * mode. Must be done before the first call to getActivity()
		 */


		Thread.sleep(5000);
	}

	public void testBLEConnectedSmartCard() throws CSCException,
			InterruptedException {
		Log.i(TAG,"Activity Object created.");

		// Get the instance.
		cscInstance = CSC.getInstance(getContext());
		// Check for the instance is null or not.
		assertNotNull(cscInstance);
		Log.i(TAG,"CSC instance Object created.");

		/*scanListenerImpl = new ScanListenerImpl();
		mHandlerThread = new ScanHandler("ScanHandlerThread");

		semaphore = new Semaphore(0);
		mHandlerThread.start();
		isScanLockActive = true;
		semaphore.acquire();
		mHandlerThread.quit();
		Log.i(TAG,"Scanning task completed");
		assertNotNull(deviceDetails);
		Thread.sleep(5000L);

		semaphoreConn = new Semaphore(0);
		mConnectionHandler = new ConnectionHandler("ConnectionHandlerThread");
		mConnectionHandler.start();
		isConnLockActive = true;
		semaphoreConn.acquire();
		mConnectionHandler.quit();
		Log.i(TAG,"Connection establishment task completed");
		assertNotNull(conenctedDevice);
		Thread.sleep(5000L);

		semaphoreSendAsynComm = new Semaphore(0);
		mFindAppletHandler = new FindAppletHandler("FinadAppletHandlerThread");
		mFindAppletHandler.start();
		isFindAppletsLockActive = true;
		semaphoreSendAsynComm.acquire();
		mFindAppletHandler.quit();

		assertNotNull(conenctedDevice);
*/	}

	private class ScanHandler extends HandlerThread {

		/**
		 * @param name
		 */
		public ScanHandler(String name) {
			super(name);
		}

		public void run() {
			// Get the Available device.
			try {
				Log.i(TAG,"Calling get Available devices.");
				cscInstance.getAvailableDevices(DeviceType.BLE,
						scanListenerImpl);
			} catch (CSCException e) {
				e.printStackTrace();
				assertNotNull(null);
			}
		}
	}

	private class FindAppletHandler extends HandlerThread {

		/**
		 * @param name
		 */
		public FindAppletHandler(String name) {
			super(name);
		}

		public void run() {
			List<Smartcard> availSmartCards = conenctedDevice
					.getAvailableCards();
			mSmartCard = availSmartCards.get(0);
			fillFindAppletsArray1();
			mSmartCard.findApplet(findAppletsList1);
		}

	}

	private class ConnectionHandler extends HandlerThread {

		/**
		 * @param name
		 */
		public ConnectionHandler(String name) {
			super(name);
		}

		public void run() {
			// Connect to device..
			try {
				Log.i(TAG,"Connecting to device...");
				cscInstance.connectToDevice(deviceDetails, cscCallback);
			} catch (CSCException e) {
				e.printStackTrace();
				assertNotNull(null);
			}
		}

	}

	private class ScanListenerImpl implements ScanListener {

		@Override
		public void onScanStop() {
			if (isScanLockActive) {
				semaphore.release();
				isScanLockActive = false;
			}
		}

		@Override
		public void onScanStart() {
		}

		@Override
		public void onDeviceFound(DeviceDetails onDeviceFound, int rssi) {
			Log.i(TAG,"Received device callback from ==>"
					+ onDeviceFound.getAddress());
			if (isScanLockActive) {
				Log.i(TAG,"CSC Device scan stoped.");

				if (DummyDeviceDetails.getDeviceDetails().getAddress()
						.equalsIgnoreCase(onDeviceFound.getAddress())) {
					deviceDetails = onDeviceFound;
					isDeviceFound = true;
				} else if (!isDeviceFound) {
					deviceDetails = null;
				}

				try {
					cscInstance.stopScanning();
				} catch (CSCException e1) {
					e1.printStackTrace();
					assertNotNull(null);
				}
			}
		}

		@Override
		public void onDeviceFound(DeviceDetails onDeviceFound) {
			Log.i(TAG,"Received device callback from ==>"
					+ onDeviceFound.getAddress());
			if (isScanLockActive) {
				Log.i(TAG,"CSC Device scan stoped.");

				if (DummyDeviceDetails.getDeviceDetails().getAddress()
						.equalsIgnoreCase(onDeviceFound.getAddress())) {
					deviceDetails = onDeviceFound;
					isDeviceFound = true;
				} else if (!isDeviceFound) {
					deviceDetails = null;
				}

				try {
					cscInstance.stopScanning();
				} catch (CSCException e1) {
					e1.printStackTrace();
					assertNotNull(null);
				}
			}
		}
	};

	private CSCCallback cscCallback = new CSCCallback() {

		@Override
		public void onUpdateRSSI(float value) {

		}

		@Override
		public void onSetAppletStatus(Applet applet, boolean selectedState) {
			Log.i(TAG,"Test App:Applet setted" + selectedState + ":=>"
					+ applet.getAppletID());
			if (isFindAppletsLockActive) {
				semaphoreSendAsynComm.release();
				isFindAppletsLockActive = false;
			}

		}

		@Override
		public void onReceiveResponse(ResponseAPDU response, Applet applet,
				ErrorCodes error) {

		}

		@Override
		public void onReceiveResponse(byte[] response,
				ErrorCodes error) {

		}

		@Override
		public void onFindAppletResponse(Smartcard smartcard,
				ErrorCodes errorCodes) {
			if (errorCodes == ErrorCodes.SUCCESS_RESPONSE) {
				HashMap<String, Boolean> findAppletResponseList = smartcard
						.getFindAppletResponseList();
				for (String appletID : findAppletsList1) {
					Boolean bool = findAppletResponseList.get(appletID);
					if (bool.booleanValue()) {
						Applet applet = null;
						if (appletID == ECHO_APPLET_ID)
							applet = new EchoApplet(smartcard, appletID);
						else
							applet = new ExtendedLengthApplet(smartcard,
									appletID);

						applet.selectApplet();
					}

				}
			} else {
				Log.i(TAG,"Test App:Error Response received:" + errorCodes);
			}

		}

		@Override
		public void onConnectionChange(Device device,
				DeviceConnectionState deviceState) {
			Log.i(TAG,"Device State:" + deviceState);
			if (deviceState == DeviceConnectionState.CONNECTED) {
				Log.i(TAG,"Device connected to "
						+ device.getDeviceDetails().getAddress());
				conenctedDevice = device;
			} else if (deviceState == DeviceConnectionState.CONNECTING) {
				Log.i(TAG,"Device Conencting...");
			} else {
				Log.i(TAG,"Device Conenction error.");
			}
			Log.i(TAG,"Is locked Connection call:" + isScanLockActive);
			if (isConnLockActive) {
				Log.i(TAG,"Releasing lock...");
				semaphoreConn.release();
				isConnLockActive = false;
			}
		}
	};


			 public void fillFindAppletsArray1() {
				 // TODO Auto-generated method stub
				 findAppletsList1 = new ArrayList<String>();
				 findAppletsList1.add(ECHO_APPLET_ID);
				 findAppletsList1.add(EXTENDED_LEN_APPLET_ID);

				 // findAppletsList1.add("4563686F4170706C6574");
				 // findAppletsList1.add("4563686F4170706C6574");
			 }


		 }

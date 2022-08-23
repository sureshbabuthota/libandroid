package com.tyfone.csc;

import java.util.ArrayList;
import java.util.List;

import com.tyfone.csc.helper.ConversionHelper;
import com.tyfone.csc.helper.ErrorCodes;
import com.tyfone.csc.smartcard.Applet;
import com.tyfone.csc.smartcard.CommandAPDU;
import com.tyfone.csc.smartcard.ResponseAPDU;
import com.tyfone.csc.smartcard.Smartcard;

/**
 * @author Srikar created on @date Sep 9, 2014
 */
public class EchoApplet extends Applet {

	private String appletid;
	private ArrayList<CommandAPDU> commandAPDUList;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EchoApplet(Smartcard smartCard, String appletID) {
		super(smartCard, appletID);
		appletid = appletID;
		commandAPDUList = new ArrayList<CommandAPDU>();
		fillCommandAPDUList();

	}

	private void fillCommandAPDUList() {

		byte[] apduData = ConversionHelper.hexStringToByteArray("b082934200");

		CommandAPDU commandAPDU = new CommandAPDU(apduData);

		commandAPDUList.add(commandAPDU);
		commandAPDU = new CommandAPDU(
				ConversionHelper
						.hexStringToByteArray("b08293420a27bee81af1dc71cbd393"));
		commandAPDUList.add(commandAPDU);

		apduData = ConversionHelper

				.hexStringToByteArray("b082934232329830f19715efb9db68ebb271217b979a97ab94f5c41e774e037b8fde4455dbd6a8836e958fb4c5b3f60e8da0bfd94a03e8");
		commandAPDU = new CommandAPDU(
				ConversionHelper
						.hexStringToByteArray("b0829342646d125d91d939c54dbd4b7a61c94764ee8effc6e37960a09dc10b52d82f554724502441a162f44ea005cbaed31b90f5059633671f3610efeae94fc07a7cf44d81e98e7e792b9475a7378e8ca5681db667a0e2ff16edd799816d6130bd049db8bba62a606a"));

		commandAPDUList.add(commandAPDU);
		apduData = ConversionHelper

				.hexStringToByteArray("b0829342c83c7905263cbd2890e7787b31d28e7a5cd9087073202e21d07ea1276185f3594c8b4f8ebd886b8b496afd8169932e4c4bea0e293526c245903945748eaa0942156cbccfba27852c30d6d9c2c1f7f7aa400b2f527faeb00b0ecd67034f90f3b7f1d4127cc09ce80703fc7572b3a034e89fc80e27f0b88994a4e9958ba481cfa166c3087c79346b2df8efe63774d33f79af1ad3a4bdc6ee38c9fad86d38731c5c7878bcc37f9c7258411534037af76b0ceae63a062e47b67f28de9445a91b50118c041c2e257f8b0a53");
		commandAPDU = new CommandAPDU(0xb0, 0x82, 0x93, 0x42, apduData);
		commandAPDUList.add(commandAPDU);

		apduData = ConversionHelper
				.hexStringToByteArray("b0829342f53c7905263cbd2890e7787b31d28e7a5cd9087073202e21d07ea1276185f3594c8b4f8ebd886b8b496afd8169932e4c4bea0e293526c245903945748eaa0942156cbccfba27852c30d6d9c2c1f7f7aa400b2f527faeb00b0ecd67034f90f3b7f1d4127cc09ce80703fc7572b3a034e89fc80e27f0b88994a4e9958ba481cfa166c3087c79346b2df8efe63774d33f79af1ad3a4bdc6ee38c9fad86d38731c5c7878bcc37f9c7258411534037af76b0ceae63a062e47b67f28de9445a91b50118c041c2e257f8b0a5334037af76b0ceae63a062e47b67f28de9445a91b50118c041c2e257f8b0a5334037af76b0ceae63a062e47b67f");
		commandAPDU = new CommandAPDU(0xb0, 0x82, 0x93, 0x42, apduData);
		commandAPDUList.add(commandAPDU);

		apduData = ConversionHelper
				.hexStringToByteArray("b0829342fa3c7905263cbd2890e7787b31d28e7a5cd9087073202e21d07ea1276185f3594c8b4f8ebd886b8b496afd8169932e4c4bea0e293526c245903945748eaa0942156cbccfba27852c30d6d9c2c1f7f7aa400b2f527faeb00b0ecd67034f90f3b7f1d4127cc09ce80703fc7572b3a034e89fc80e27f0b88994a4e9958ba481cfa166c3087c79346b2df8efe63774d33f79af1ad3a4bdc6ee38c9fad86d38731c5c7878bcc37f9c7258411534037af76b0ceae63a062e47b67f28de9445a91b50118c041c2e257f8b0a5334037af76b0ceae63a062e47b67f28de9445a91b50118c041c2e257f8b0a5334037af76b0ceae63a062e47b67f28de9445a9");
		commandAPDU = new CommandAPDU(0xb0, 0x82, 0x93, 0x42, apduData);
		commandAPDUList.add(commandAPDU);

	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "EchoApplet" + (appletid);
	}

	public List<CommandAPDU> getAPDUList() {
		return commandAPDUList;
	}

	@Override
	public void onReceiveResponse(byte[] response, ErrorCodes error) {

	}

	@Override
	public void onReceiveResponse(ResponseAPDU response, Applet applet, ErrorCodes error) {

	}
}

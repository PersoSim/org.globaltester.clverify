package org.globaltester.clverify.responselistener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globaltester.clverify.ClVerifyConnector;
import org.globaltester.clverify.ResponseListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;

public class SimulatorControlDataListener implements ResponseListener {

	private final ClVerifyConnector clVerify;
	private StringBuffer cmdStr = new StringBuffer();

	public SimulatorControlDataListener(ClVerifyConnector clVerify) {
		this.clVerify = clVerify;
	}

	@Override
	public boolean handleResponse(byte[] tagBuffer, byte[] valueBuffer) {
		if (!"3001".equals(HexString.encode(tagBuffer))) return false;

		String valueString = new String(valueBuffer);
		if (valueString.startsWith("apdu(")) {
			return handleApdu(valueString);
		} else if (valueString.startsWith("field(")) {
			handleFieldChange(valueString);
			return true;
		} else {
			return false;
		}
	}

	private void handleFieldChange(String valueString) {

		BasicLogger.log("handleFieldChange: " + valueString, LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		if (clVerify != null) {
			if (valueString.startsWith("field(on)")) {
				clVerify.fieldOn();
			} else {
				clVerify.fieldOff();
			}
		}
	}

	private boolean handleApdu(String valueString) {
		BasicLogger.log("handleApdu: " + valueString, LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		if (parseApduCommand(valueString)) {
			//let clVerifyConnector handle the APDU
			if (clVerify != null) {
				clVerify.handleApdu(HexString.toByteArray(cmdStr.toString()));
			}
			//clean cmd holding space
			cmdStr = new StringBuffer();
			return true;
		} else {
			return false;
		}
	}

	private boolean parseApduCommand(String valueString) {
		Pattern p = Pattern.compile("apdu\\((\\d+),(\\d+),(\\d+)\\): (.*)");
		Matcher m = p.matcher(valueString);
		if (m.matches()) {
			int nrOfBlocks = Integer.parseInt(m.group(1));
			int curBlock = Integer.parseInt(m.group(2));
			cmdStr.append(m.group(4));

			if (nrOfBlocks >1) {
				System.out.println("Long Cmd-Apdu");
			}

			return curBlock==nrOfBlocks;
		} else {
			return false;
		}
	}

}

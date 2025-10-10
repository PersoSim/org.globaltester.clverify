package org.globaltester.clverify.responselistener;

import java.util.Arrays;

import org.globaltester.clverify.ResponseListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class RemoteCallReturnListener implements ResponseListener {

	int pollingInterval = 50;
	private String commandResponse = null;
	private int nrOfResponses;

	public RemoteCallReturnListener(int nrOfResponses) {
		this.nrOfResponses = nrOfResponses;
	}

	public void waitCommandResponse(int timeout) {
		// busy waiting
		while (timeout > 0) {
			if (commandResponse != null) {
				return;
			}
			try {
				Thread.sleep(pollingInterval);
			} catch (InterruptedException e) { // NOSONAR
				// intentionally ignore (just influences busy waiting timing)
				BasicLogger.logException("OK: Busy waiting", e, LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
			timeout -= pollingInterval;
		}
	}

	@Override
	public boolean handleResponse(byte[] tagBuffer, byte[] valueBuffer) {
		if (!"3100".equals(HexString.encode(tagBuffer))) return false;

		nrOfResponses--;
		if (nrOfResponses > 0) {
			//wait for the remaining responses
			return true;
		}

		int tag = Utils.getIntFromUnsignedByteArray(new byte[] {valueBuffer[1], valueBuffer[0]});

		switch (tag) {
		case 1:
			if (valueBuffer[valueBuffer.length-2] == 1) {
				commandResponse = "Success";
			} else {
				commandResponse = "Error";
			}
			break;
		case 3:
			commandResponse = new String(Arrays.copyOfRange(valueBuffer, 4, valueBuffer.length));
			break;

		default:
			commandResponse = "Unsupported format: " + HexString.encode(valueBuffer);
			break;
		}
		BasicLogger.log("RemoteCallReturn: "+commandResponse, LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		return true;
	}

}

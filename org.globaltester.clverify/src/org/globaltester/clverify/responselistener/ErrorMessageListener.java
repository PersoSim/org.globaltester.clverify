package org.globaltester.clverify.responselistener;

import java.util.Arrays;

import org.globaltester.clverify.ResponseListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;

public class ErrorMessageListener implements ResponseListener {

	@Override
	public boolean handleResponse(byte[] tagBuffer, byte[] valueBuffer) {
		if (!"3110".equals(HexString.encode(tagBuffer))) return false;

		LogLevel level = valueBuffer[0]==0 ? LogLevel.ERROR : LogLevel.WARN;
		BasicLogger.log("ClVerifyResponse message: "+new String(Arrays.copyOfRange(valueBuffer, 1, valueBuffer.length)), level, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

		return true;
	}

}

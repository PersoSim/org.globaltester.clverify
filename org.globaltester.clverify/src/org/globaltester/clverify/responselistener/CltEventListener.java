package org.globaltester.clverify.responselistener;

import org.globaltester.clverify.ResponseListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;

public class CltEventListener implements ResponseListener {

	@Override
	public boolean handleResponse(byte[] tagBuffer, byte[] valueBuffer) {
		if (!"3005".equals(HexString.encode(tagBuffer))) return false;

		BasicLogger.log("CltEvent: " + HexString.encode(valueBuffer), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

		return true;
	}

}

package org.globaltester.clverify;

public interface ResponseListener {

	/**
	 * Handle (or ignore) response received by {@link ResponseServer}
	 * @param tagBuffer
	 * @param valueBuffer
	 * @return true if the response was properly handled and should not be forwarded to other listeners
	 */
	boolean handleResponse(byte[] tagBuffer, byte[] valueBuffer);

}

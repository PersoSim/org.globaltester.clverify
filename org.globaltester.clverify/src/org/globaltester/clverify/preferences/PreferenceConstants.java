package org.globaltester.clverify.preferences;

public class PreferenceConstants {

	public static final String CL_VERIFY_RESPONSE_SERVER_PORT = "clVerifyResponseServerPort";
	public static final String CL_VERIFY_COMMAND_PORT = "clVerifyCommandPort";
	public static final String CL_VERIFY_ADDRESS = "clVerifyAddress";
	public static final String CL_VERIFY_TRACE_OPEN_SOCKET = "clVerifyOpenTraceSocket";
	public static final String CL_VERIFY_TRACE_LOG = "clVerifyLogTrace";

	public static final String CL_VERIFY_PICC_TYPE = "clVerifyPiccType";
	public static final String CL_VERIFY_SPEED = "clVerifySpeed";
	public static final String CL_ADDITIONAL_CONFIG = "clVerifyAdditionalConfig";

	public static final int CL_VERIFY_RESPONSE_SERVER_PORT_DEFAULT = 20003;
	public static final int CL_VERIFY_COMMAND_PORT_DEFAULT = 20004;
	public static final String CL_VERIFY_ADDRESS_DEFAULT = "192.168.1.4";
	public static final boolean CL_VERIFY_TRACE_OPEN_SOCKET_DEFAULT = true;
	public static final int CL_VERIFY_TRACE_SERVER_PORT = 20002;


	private PreferenceConstants() {
		// private constructor to hide the implicit public one
	}
}

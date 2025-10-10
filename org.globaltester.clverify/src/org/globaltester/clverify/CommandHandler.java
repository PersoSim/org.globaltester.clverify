package org.globaltester.clverify;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;

public class CommandHandler {

	private static final int RCTRL_GETBFCMD = 7;

	private ClVerifyConnector clVerify;
	private Socket commandSocket;

	public CommandHandler(ClVerifyConnector connector) {
		super();
		this.clVerify = connector;
	}

	public void connect() throws IOException {
		// connect socket
		commandSocket = new Socket();
		BasicLogger.log("Try to connect to " + clVerify.getCommandAddress().toString().replace("/", "") + "...", LogLevel.INFO, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		commandSocket.connect(clVerify.getCommandAddress());
		commandSocket.setTcpNoDelay(true);

		//send initial command
		DataOutputStream commandStream = new DataOutputStream(commandSocket.getOutputStream());
		commandStream.write(new byte[] { 0, 8, 1, 5 });
	}

	public void stop() {
		if (commandSocket != null) {
			try {
				commandSocket.close();
				commandSocket = null;
			} catch (IOException e) {
				BasicLogger.logException("Unable to close ClVerify command handler socket", e, LogLevel.WARN, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
		}
	}

	public void sendCommand(String cmd) throws IOException
	{
		// command length + tag(32Bit) + length(32Bit)
		int length = cmd.length() + 4 + 4;

        ByteBuffer byteValue = ByteBuffer.allocate(length);
        byteValue.order(ByteOrder.LITTLE_ENDIAN);
        byteValue.putInt(length);
        byteValue.putInt(RCTRL_GETBFCMD);
        byteValue.put(cmd.getBytes());

//        BasicLogger.log("ClVerifyCommand(HEX):   "+HexString.encode(byteValue.array()), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
//        BasicLogger.log("ClVerifyCommand(ASCII): "+new String(byteValue.array()), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
        BasicLogger.log("ClVerifyCommand: "+cmd, LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

        DataOutputStream dOut = new DataOutputStream(commandSocket.getOutputStream());
        dOut.write(byteValue.array());
	}

	public boolean isConnected() {
		return (commandSocket != null) && commandSocket.isConnected();
	}

}

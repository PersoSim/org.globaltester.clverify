package org.globaltester.clverify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.globaltester.base.PreferenceHelper;
import org.globaltester.clverify.preferences.PreferenceConstants;
import org.globaltester.clverify.responselistener.RemoteCallReturnListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;
import org.globaltester.simulator.Simulator;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;

public class ClVerifyConnector {

	private ResponseServer responseServer;
	private TraceServer traceServer;
	private CommandHandler commandHandler;
	private Simulator simulator;

	private final String clVerifyAddress;
	private final int commandPort;
	private final int responseServerPort;

	private static final int CHUNKSIZE = 200;
	private static final String FIRST_CHUNK = ",1";
	private static final String INTERMEDIATE_CHUNK = ",2";
	private static final String LAST_CHUNK = ",0";

	public ClVerifyConnector() {
		clVerifyAddress = PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID,
				PreferenceConstants.CL_VERIFY_ADDRESS,
				org.globaltester.clverify.preferences.PreferenceConstants.CL_VERIFY_ADDRESS_DEFAULT);
		commandPort = Integer.parseInt(PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID,
				PreferenceConstants.CL_VERIFY_COMMAND_PORT, Integer.toString(
						org.globaltester.clverify.preferences.PreferenceConstants.CL_VERIFY_COMMAND_PORT_DEFAULT)));
		responseServerPort = Integer.parseInt(PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID,
				PreferenceConstants.CL_VERIFY_RESPONSE_SERVER_PORT, Integer.toString(
						org.globaltester.clverify.preferences.PreferenceConstants.CL_VERIFY_RESPONSE_SERVER_PORT_DEFAULT)));
	}

	public void connect() throws IOException {
		responseServer = new ResponseServer(this);
		responseServer.start();

		if (Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID,
				PreferenceConstants.CL_VERIFY_TRACE_OPEN_SOCKET, "false"))) {
			traceServer = new TraceServer();
			traceServer.start();
		}
		Thread.yield();

		commandHandler = new CommandHandler(this);
		commandHandler.connect();

		commandHandler.sendCommand("api\r");
	}

	public boolean isConnected() {
		return (commandHandler != null) && commandHandler.isConnected();
	}

	public void stop() {
		if (commandHandler != null && commandHandler.isConnected()) {
			try {
				commandHandler.sendCommand("capi\r");
			} catch (IOException e) {
				BasicLogger.logException("Failure closing CL Verify api", e, LogLevel.WARN,
						new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
			commandHandler.stop();
			commandHandler = null;
		}
		if (responseServer != null) {
			responseServer.stop();
			responseServer = null;
		}
		if (traceServer != null) {
			traceServer.stop();
			traceServer = null;
		}
	}

	public synchronized void sendCommand(String cmd) throws IOException {
		sendCommand(cmd, 1000, 1);
	}

	public synchronized void sendCommand(String cmd, int timeout, int nrOfResponses) throws IOException {
		RemoteCallReturnListener responseListener = new RemoteCallReturnListener(nrOfResponses);
		responseServer.addListener(responseListener);
		commandHandler.sendCommand(cmd);
		responseListener.waitCommandResponse(timeout);
		responseServer.removeListener(responseListener);
	}

	public void fieldOn() {
		if (simulator != null) {
			simulator.cardPowerUp();
		}
	}

	public void fieldOff() {
		if (simulator != null) {
			simulator.cardPowerDown();
		}
	}

	public void handleApdu(byte[] commandApdu) {
		if (simulator != null) {
			byte[] respBytes = simulator.processCommand(commandApdu);
			if (respBytes.length <= CHUNKSIZE) {
				sendResponseApdu(respBytes);
			} else {
				sendResponseApduInChunks(respBytes);
			}
		}
	}

	private void sendResponseApdu(byte[] respApdu) {
		String respString = HexString.encode(respApdu);
		try {
			sendCommand("wcd \"write: " + respString + "\"", 0, 0);
		} catch (IOException e) {
			BasicLogger.logException("Unable to process response APDU", e, LogLevel.ERROR,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		}
	}

	private void sendResponseApduInChunks(byte[] respApdu) {
		String respString = HexString.encode(respApdu);
		try {

			int nrOfBytes = respString.length() / 2;
			int nrOfChunks = (int) Math.ceil((float) nrOfBytes / CHUNKSIZE);
			int lastChunk = nrOfChunks - 1;

			for (int i = 0; i < nrOfChunks; i++) {
				int beginIndex = i * CHUNKSIZE * 2;
				int endIndex = beginIndex + CHUNKSIZE * 2;
				String chunk = respString.substring(beginIndex, Math.min(endIndex, respString.length()));
				BasicLogger.log(getClass(), beginIndex + "," + endIndex, LogLevel.TRACE,
						new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

				String chunkType = INTERMEDIATE_CHUNK;
				if (i == 0)
					chunkType = FIRST_CHUNK;
				if (i == lastChunk)
					chunkType = LAST_CHUNK;

				String cmdString = "wcd \"write: " + chunk + chunkType + "\"";
				BasicLogger.log(cmdString, LogLevel.TRACE,
						new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
				sendCommand(cmdString, 0, 0);
			}
		} catch (IOException e) {
			BasicLogger.logException("Unable to process response APDU", e, LogLevel.ERROR,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		}
	}

	public void setSimulator(Simulator newSim) {
		simulator = newSim;
	}

	public boolean isAvailable() {
		try (Socket commandSocket = new Socket()) {
			commandSocket.connect(getCommandAddress(), 2000);
		} catch (IOException e) {
			BasicLogger.logException("ClVerify unreachable", e, LogLevel.INFO,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			return false;
		}

		return true;
	}

	public SocketAddress getCommandAddress() {
		return new InetSocketAddress(clVerifyAddress, commandPort);
	}

	public int getResponseServerPort() {
		return responseServerPort;
	}

}

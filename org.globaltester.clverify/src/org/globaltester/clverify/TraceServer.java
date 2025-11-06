package org.globaltester.clverify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.globaltester.base.PreferenceHelper;
import org.globaltester.clverify.preferences.PreferenceConstants;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;

public class TraceServer implements Runnable {

	private boolean interrupted = false;
	private Thread serverThread;

	public TraceServer() {
		// do nothing
	}

	public synchronized void start() {
		serverThread = new Thread(this);
		serverThread.setName("CLVerify Trace Server");
		serverThread.start();
	}

	public synchronized void stop() {
		interrupted = true;
		if (curClientSocket != null) {
			try {
				curClientSocket.close();
			} catch (IOException e) {
				BasicLogger.logException("Unable to close trace socket for ClVerify.", e, LogLevel.WARN,
						new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
		}
		if (serverThread != null) {
			serverThread.interrupt();
		}
	}

	Socket curClientSocket = null;

	@Override
	public void run() {
		BasicLogger.log(
				"Starting trace server for ClVerify at port "
						+ org.globaltester.clverify.preferences.PreferenceConstants.CL_VERIFY_TRACE_SERVER_PORT + "...",
				LogLevel.INFO, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

		try (ServerSocket server = new ServerSocket(
				org.globaltester.clverify.preferences.PreferenceConstants.CL_VERIFY_TRACE_SERVER_PORT);
				Socket client = server.accept()) {
			curClientSocket = client;
			while (!interrupted) {
				readAndHandleTraces(new BufferedReader(new InputStreamReader(client.getInputStream())));
			}
		} catch (SocketException e) {
			BasicLogger.log("Trace server for ClVerify stopped.", LogLevel.WARN,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		} catch (Exception e) {
			BasicLogger.logException("Trace server for ClVerify stopped!", e, LogLevel.ERROR,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		} finally {
			curClientSocket = null;
		}

	}

	private void readAndHandleTraces(BufferedReader inputReader) throws IOException {
		String readLine = inputReader.readLine();
		if (readLine == null) {
			this.stop();
			return;
		}

		if (Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID,
				PreferenceConstants.CL_VERIFY_TRACE_LOG, "false"))) {
			BasicLogger.log("CLVerify trace: " + readLine.replaceAll("\\P{Print}", "□"), LogLevel.TRACE,
					new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		}
	}

}

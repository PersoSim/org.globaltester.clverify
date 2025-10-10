package org.globaltester.clverify;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.globaltester.clverify.responselistener.CltEventListener;
import org.globaltester.clverify.responselistener.ErrorMessageListener;
import org.globaltester.clverify.responselistener.SimulatorControlDataListener;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.simulator.log.PersoSimLogTags;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class ResponseServer implements Runnable {

	private boolean interrupted = false;
	private Thread serverThread;
	private final ClVerifyConnector clVerify;

	public ResponseServer(ClVerifyConnector clVerify) {
		this.clVerify = clVerify;
		addListener(new CltEventListener());
		addListener(new ErrorMessageListener());
		addListener(new SimulatorControlDataListener(clVerify));
	}


	public synchronized void start() {
		serverThread = new Thread(this);
		serverThread.start();
	}

	public synchronized void stop() {
		interrupted = true;
		if (curClientSocket != null) {
			try {
				curClientSocket.close();
				curClientSocket = null;
			} catch (IOException e) {
				BasicLogger.logException("Unable to close ResponseSocket to ClVerify", e, LogLevel.WARN, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
		}
		if (curServerSocket != null && !curServerSocket.isClosed()) {
			try {
				curServerSocket.close();
			} catch (IOException e) {
				BasicLogger.logException("Unable to close Server Socket for ClVerify", e, LogLevel.WARN, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			}
			curServerSocket = null;
		}
		if (serverThread != null) {
			serverThread.interrupt();
		}
	}


	private ServerSocket curServerSocket = null;
	private Socket curClientSocket = null;
	@Override
	public void run() {

		BasicLogger.log("Starting response server at port " + clVerify.getResponseServerPort() + "...", LogLevel.INFO, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));

		try (ServerSocket server = new ServerSocket()) {
			curServerSocket = server;
		    server.setReuseAddress(true);
		    server.bind(new InetSocketAddress(clVerify.getResponseServerPort()));

		    try (Socket client = server.accept()) {
		        curClientSocket = client;
		        readInitialBytes(client.getInputStream());
		        while (!interrupted) {
		            readAndHandleResponse(client.getInputStream());
		        }
		    }
		} catch (SocketException e) {
			BasicLogger.log("java.net.SocketException: " + e.getMessage(), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		} catch (Exception e) {
		    BasicLogger.logException("Unable to handle response from ClVerify", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		} finally {
		    stop();
		}
	}


	private void readInitialBytes(InputStream inputStream) throws IOException {
		byte[] lengthBuffer = new byte[4];
		int l = inputStream.read(lengthBuffer);
		if (l<4) {
			BasicLogger.log("Unable to read complete initial bytes ("+l+")", LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			return;
		}
		BasicLogger.log("Initial bytes received ("+ l + "): " + HexString.encode(lengthBuffer), LogLevel.DEBUG, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
	}

	private void readAndHandleResponse(InputStream inputStream) throws IOException {
		byte[] tagBuffer = new byte[2];
		int t = inputStream.read(tagBuffer);
		if (t<2) {
			BasicLogger.log("Unable to read complete tag field ("+t+")", LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			return;
		}
		tagBuffer = Utils.invertByteOrder(tagBuffer);

		byte[] lengthBuffer = new byte[2];
		int l = inputStream.read(lengthBuffer);
		if (l<2) {
			BasicLogger.log("Unable to read complete length field ("+l+")", LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			return;
		}
		lengthBuffer = Utils.invertByteOrder(lengthBuffer);

		int length = Utils.getIntFromUnsignedByteArray(lengthBuffer);

		byte[] valueBuffer = new byte[length];
		int v = inputStream.read(valueBuffer);
		if (v<length) {
			BasicLogger.log("Unable to read complete value field ("+v+")", LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
			return;
		}

		handleResponse(tagBuffer, valueBuffer);
	}


	private synchronized void handleResponse(byte[] tagBuffer, byte[] valueBuffer) {
//		BasicLogger.log("CLVerifyResponse(HEX): " + HexString.encode(tagBuffer) +" " + HexString.encode(valueBuffer), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		BasicLogger.log("CLVerifyResponse: " + HexString.encode(tagBuffer) +" " + new String(valueBuffer).replaceAll("\\P{Print}", "□"), LogLevel.TRACE, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		for (ResponseListener curListener : listeners) {
			if (curListener.handleResponse(tagBuffer, valueBuffer)) break;
		}
	}


	private ArrayList<ResponseListener> listeners = new ArrayList<>();

	public synchronized void addListener(ResponseListener responseListener) {
		listeners.add(responseListener);
	}


	public synchronized void removeListener(ResponseListener responseListener) {
		listeners.remove(responseListener);
	}
}

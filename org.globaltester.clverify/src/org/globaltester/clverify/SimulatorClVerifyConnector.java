package org.globaltester.clverify;

import java.io.IOException;

import org.globaltester.base.PreferenceHelper;
import org.globaltester.clverify.preferences.PreferenceConstants;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;
import org.globaltester.simulator.device.SimulatorDeviceConnector;

import de.persosim.driver.connector.SimulatorManager;
import de.persosim.simulator.log.PersoSimLogTags;

/**
 * Connection for CL Verify A
 *
 *
 * @author may.alexander
 *
 */
public class SimulatorClVerifyConnector implements SimulatorDeviceConnector {

	private static SimulatorClVerifyConnector instance = null;
	ClVerifyConnector clVerify = null;

	public static synchronized SimulatorClVerifyConnector getInstance() {
		if (instance == null) {
			instance = new SimulatorClVerifyConnector();
		}
		return instance;
	}

	private SimulatorClVerifyConnector() {
		SimulatorManager.getSim(); //ensure bundle and ServiceTracker are started
	}


	/* (non-Javadoc)
	 * @see org.globaltester.clverify.SimulatorDeviceConnectorFoo#run()
	 */
	@Override
	public void run() {
		if (clVerify != null) return;
		clVerify = new ClVerifyConnector();
		clVerify.setSimulator(SimulatorManager.getSim());
		try {
			clVerify.connect();

			//configure simulation
			String type = PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID, PreferenceConstants.CL_VERIFY_PICC_TYPE, "a");
			clVerify.sendCommand("sims ISO14443PICC \"create: picc="+type+"\" \"config: loadconf  file=Type"+type.toUpperCase()+"_PICCconf\"\r", 1000, 3);
			String speed = PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID, PreferenceConstants.CL_VERIFY_SPEED, "0");
			clVerify.sendCommand("wcd \"config: target pt"+type+" speed="+speed+"\"\r");

			// allow additional configuration
			for (int i=0;i < 15; i++) {
				String key = String.format(PreferenceConstants.CL_ADDITIONAL_CONFIG + "_%02d", i);
				String additionalConfig = PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID, key);

				if (additionalConfig != null) {
					clVerify.sendCommand(additionalConfig + "\r");
				} else {
					break;
				}
			}

			//start simulation
			clVerify.sendCommand("wcd start:\r");

			//handle edge cases where field is present immediately
			clVerify.fieldOn();
		} catch (IOException e) {
			BasicLogger.logException("Failure starting CL Verify sim", e, LogLevel.ERROR, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		}
	}


	@Override
	public void stop() {
		if (clVerify == null) return;

		try {
			if (clVerify.isConnected()) {
				clVerify.sendCommand("simr\r");
			}
		} catch (IOException e) {
			BasicLogger.logException("Failure closing CL Verify sim", e, LogLevel.WARN, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.CLVERIFYA_TAG_ID));
		}
		clVerify.stop();
		clVerify = null;
	}

	@Override
	public boolean isAvailable() {
		ClVerifyConnector connector;
		if (clVerify != null) {
			connector= clVerify;
		} else {
			connector = new ClVerifyConnector();
		}
		return connector.isAvailable();
	}

	@Override
	public int getPriority() {
		// defaults to 100, which is highest priority currently known
		return Integer.parseInt(PreferenceHelper.getPreferenceValue(Activator.PLUGIN_ID, "SimulatorClVerifyConnectorPriority", "100"));
	}

}

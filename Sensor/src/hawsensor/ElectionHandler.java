package hawsensor;

import java.net.URL;
import java.util.ArrayList;

public class ElectionHandler {
	SensorService ownSensor;

	ElectionHandler(final SensorService sensor) {
		ownSensor = sensor;
	}

	public void electione() {
		boolean voted = false;
		ArrayList<URL> ourCopy = new ArrayList<URL>();
		ArrayList<Sensorproxy.SensorService> ourServicesCopy = new ArrayList<Sensorproxy.SensorService>();
		synchronized (ownSensor.getAllSensors()) {
			for (int i = 0; i < ownSensor.getAllSensors().size(); i++) {
				ourCopy.add(ownSensor.getAllSensors().get(i));
				ourServicesCopy.add(ownSensor.getAllSensorsServices().get(i));
			}
		}
		for (URL url : ourCopy) {
			if (compareString(url.toString(), ownSensor.getOwnURL().toString()) == 1) {
				try {
					ourServicesCopy.get(ourCopy.indexOf(url)).election(
							url.toString());
					voted = true;
				} catch (Exception e) {
					// Connection refused...
				}
			}
		}
		if (!voted) {
			// ownSensor.removeFromAll(ownSensor.askForCoordinator());
			String oldCoordinator = ownSensor.askForCoordinator();
			for (URL url : ourCopy) {
				if (!url.toString().equals(oldCoordinator)) {
					try {
						ourServicesCopy.get(ourCopy.indexOf(url))
								.newCoordinator(
										ownSensor.getOwnURL().toString());
					} catch (Exception e) {
						// Connection refused...
						ownSensor.removeFromAll(url.toString());
					}
				} else {
					ownSensor.removeFromAll(url.toString());
				}
			}

		}

	}

	/**
	 * 
	 * @param url
	 * @param ownURL
	 * @return 0 if equal 1 if url is greater than ownURL -1 if url is smaller
	 *         than ownURL
	 */
	protected int compareString(String url, String ownURL) {
		int result = url.compareTo(ownURL);
		if (result < 0) {
			return -1;
		} else if (result > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}

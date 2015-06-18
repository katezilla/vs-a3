package hawsensor;

import java.net.URL;

public class SensorDBImpl implements SensorDB {

	private URL[] sensors;
	private String coord;

	public SensorDBImpl() {
		sensors = new URL[4];
	}

	@Override
	public synchronized URL[] getSensors() {
		return sensors;
	}

	public synchronized void setCoordinator(String string) {
		this.coord = string;
	}

	@Override
	public synchronized boolean isCoordinator(String sensorName) {
		return sensorName.equals(coord);
	}

	@Override
	public synchronized boolean setSensors(URL[] sensors, String sender) {
		if (isCoordinator(sender)) {
			this.sensors = sensors;
			return true;
		}
		return false;
	}

	public String getCoordinator() {
		return coord;
	}

}

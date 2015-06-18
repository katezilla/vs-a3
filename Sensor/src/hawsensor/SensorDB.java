package hawsensor;

import java.net.URL;

public interface SensorDB {
	public URL[] getSensors();

	public boolean setSensors(URL[] sensors, String sender);

	boolean isCoordinator(String sensorName);
}

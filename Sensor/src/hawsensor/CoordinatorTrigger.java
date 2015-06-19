package hawsensor;

import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class CoordinatorTrigger {

	private Timer trigger;

	public CoordinatorTrigger(final String sensorName,
			                  final SensorDB activeSensors) {
		trigger = new Timer();
		
	}
}

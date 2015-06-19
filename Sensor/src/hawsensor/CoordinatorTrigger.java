package hawsensor;

import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class CoordinatorTrigger {

	private Timer trigger;

	public CoordinatorTrigger(final String sensorName,
			                  final SensorDB activeSensors) {
		trigger = new Timer();
		trigger.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (activeSensors.isCoordinator(sensorName)) {
					for (URL sensorURL : activeSensors.getSensors()) {
						new SensorServiceService(sensorURL, new QName(
								SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
								.getSensorServicePort().sendTrigger(sensorName);
						System.out.println("trigger!");
					}
				}
			}
		}, 2000 - (Calendar.getInstance().getTimeInMillis() % 2000),// delay
				2000); // period
	}
}

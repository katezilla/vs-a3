package hawsensor;

import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class CoordinatorTrigger implements Runnable {

	private String sensorName;
	private Semaphore semBooleanChanged;
	private Timer trigger;
	private SensorDB activeSensors;

	public CoordinatorTrigger(String sensorName, Semaphore semBooleanChanged,
			SensorDB activeSensors) {
		this.sensorName = sensorName;
		this.semBooleanChanged = semBooleanChanged;
		this.activeSensors = activeSensors;
		trigger = new Timer();
	}

	@Override
	public void run() {
		while (true) {
			try {
				semBooleanChanged.acquire();
				trigger.cancel();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (activeSensors.isCoordinator(sensorName)) {
				trigger.schedule(new TimerTask() {
					@Override
					public void run() {
						for (URL sensorURL : activeSensors.getSensors()) {
							getSensorService(sensorURL).sendTrigger(sensorName);
						}
					}
				}, 2000 - (Calendar.getInstance().getTimeInMillis() % 2000),// delay
						2000);// period
			}
			// TODO: shutdown
		}
	}

	private Sensorproxy.SensorService getSensorService(URL sensorURL) {
		return new SensorServiceService(sensorURL, new QName(
				SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
				.getSensorServicePort();
	}

}

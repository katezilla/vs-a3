package hawsensor;

/**
 * TODO: entfernen aus liste, wenn nicht reagiert ( Exception)
 */

import hawmeterproxy.HAWMeteringWebservice;
import hawmeterproxy.HAWMeteringWebserviceService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import Sensorproxy.SensorServiceService;
import Sensorproxy.StringArray;

@WebService
@SOAPBinding(style = Style.RPC)
public class SensorService {
	private static final int DISPLAY_N = 4;
	private static final int ASK_FOR_COORD_TIMEOUT = 500;
	private static final int TIMEOUT_TRIGGER = 2500;
	private static final int timeoutAnswerElection = 1000; // TODO: set good
														   // value
	private static final String IP = "0.0.0.0";
	public static final String NAMESPACE_URI = "http://hawsensor/";
	public static final String LOCAL_PART = "SensorServiceService";
	public static final String DISPLAY_POSITIONS[] = { "nw", "no", "sw", "so" };
	
	private boolean isCoordinator;
	private URL ownURL;
	private URL coordinatorURL;
	
	private String[] allDisplays; // TODO: Send with update
	private String[] ownDisplays; // TODO: Send with update
	private ArrayList<URL> allSensors; // TODO: Send with update
	
	ElectionHandler elect;
	
	private Sensorproxy.ObjectFactory sensorFactory;
	
	private Timer coordinatorTimer;
	private TimerTask coordinatorTask = new TimerTask() {
		@Override
		public void run() {
			if (isCoordinator) {
				synchronized(allSensors) {
					for (URL sensorURL : allSensors) {
						new SensorServiceService(sensorURL, new QName(
								SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
								.getSensorServicePort().sendTrigger(sensorURL.toString());
					}
				}
			}
		}
	};
	
	boolean gotTrigger = false;
	private Timer triggerTimer;
	private TimerTask triggerTimeoutTask = new TimerTask() {
		@Override
		public void run() {
			if(!gotTrigger) {
				elect.electione();
			}
			gotTrigger = false;
		}
	};

	/**
	 * 
	 * @param argumentParser
	 *            .. at Position 0: port ;Position 1: url ; Position 2: name;
	 *            Position 3: display
	 * @throws Exception
	 *             , if sensor shut down after occured error
	 */
	public SensorService(String[] argumentParser) throws Exception {
		// Init
		isCoordinator = false;
		allDisplays = new String[DISPLAY_N];
		ownDisplays = new String[DISPLAY_N];
		for (int i = 0; i < DISPLAY_N; ++i) {
			allDisplays[i] = new String("");
			ownDisplays[i] = new String("");
		}
		ownURL = new URL("http://" + IP + ":" + argumentParser[0]
		         + "/" + argumentParser[2]);
		ownDisplays = parseDisplayArgument(argumentParser[3]);
		Endpoint.publish(ownURL.toString(), this);
		allSensors = new ArrayList<URL>();
		sensorFactory = new Sensorproxy.ObjectFactory();
		// Setup coordinator task...
		coordinatorTimer = new Timer();
		coordinatorTimer.scheduleAtFixedRate(coordinatorTask,
				2000 - (Calendar.getInstance().getTimeInMillis() % 2000),// delay
				2000); // period
		triggerTimer = new Timer();
		triggerTimer.scheduleAtFixedRate(triggerTimeoutTask, TIMEOUT_TRIGGER, TIMEOUT_TRIGGER);
		// Get coordinator URL
		if (argumentParser[1] != null) {
			// We are not the coordinator, ask the given sensor
			// for the coordinator's url.
			coordinatorURL = new URL(getSensorService(argumentParser[1]).askForCoordinator());
			if(!getSensorService(coordinatorURL.toString()).register(
					ownURL.toString(), createStringArray(ownDisplays))) {
				throw new Exception("not allowed to register");
			}
		} else {
			// We are the coordinator.
			isCoordinator = true;
			coordinatorURL = ownURL;
			allDisplays = ownDisplays;
			synchronized(allSensors) {
			    allSensors.add(ownURL);
			}
		}
		elect = new ElectionHandler(this);
	}


	public void updateAll(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "globalDisoplay") String[] globalDisoplay,
			@WebParam(name = "activeSensors") String[] activeSensors) {
		// Alle Daten "updates" zu anderen sensoren senden (!außer selbst!)
		allDisplays = globalDisoplay;
		synchronized(allSensors) {
			allSensors.clear();
			for(int i = 0; i < DISPLAY_N; ++i) {
				URL url = null;
				try {
					url = new URL(globalDisoplay[i]);
				} catch (MalformedURLException e) {
					
				}
				if(url != null && !allSensors.contains(url)) {
					allSensors.add(url);
				}
			}
		}
	}

	/**
	 * coordinator activates this method to trigger each sensor every 2 sec
	 * 
	 * @param sender
	 * @return .. 0, if successfully updated; -1, if update failed
	 */
	public int sendTrigger(@WebParam(name = "sender") String sender) {
		// Send trigger => Wird aufgerufen vom koordinator damit die sensoren
		// ihr display aktualisieren
		gotTrigger = true;
		int val = this.calcValue();
		for(int i = 0; i < DISPLAY_N; ++i ) {
			if(!ownDisplays[i].isEmpty()) {
				try {
					getDisplay(i).setValue(val);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		return 0;
	}

	public boolean reqDisplay(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String[] reqDisplays) {
		// Delete me later!
		return true;
	}

	public boolean register(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String[] reqDisplays) {
		boolean resu = true;
		for (String str : reqDisplays) {
		  System.out.println(str);
		}
		for(int i = 0; i < DISPLAY_N; ++i) {
			if(!reqDisplays[i].isEmpty() && !allDisplays[i].isEmpty()) {
				resu = false;
				break;
			}
		}
		// add new sensor to list
		if(resu) {
			for(int i = 0; i < DISPLAY_N; ++i) {
				if(!reqDisplays[i].isEmpty()) {
					allDisplays[i] = reqDisplays[i];	
				}
			}
			StringArray global = sensorFactory.createStringArray();
			StringArray active = sensorFactory.createStringArray();
			synchronized(allSensors) {
				try {
					allSensors.add(new URL(sender));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				for (int i = 0; i < DISPLAY_N; ++i) {
					global.getItem().add(allDisplays[i]);
				}
				for (URL url : allSensors) {
					active.getItem().add(url.toString());
				}
			}
			// Update all
			for(String url : active.getItem()) {
				if (ownURL.toString().compareTo(url) != 0) {
					getSensorService(url.toString()).updateAll(ownURL.toString(), global, active);
				}
			}
		}
		return resu;
	}
	
	public void election(@WebParam(name = "sender") String sender) {
		System.out.println("Got vote from: " + sender);
		this.elect.electione();
	}

	public void answerElection(@WebParam(name = "sender") String sender) {
		//System.out.println("blub answerd");
	}

	public void newCoordinator(@WebParam(name = "sender") String sender) {
		try {
			coordinatorURL = new URL(sender);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	    this.isCoordinator = sender.compareTo(ownURL.toString()) == 0;
	    if (this.isCoordinator) {
	    	System.out.println("I'm the new coordinator...");
			StringArray global = sensorFactory.createStringArray();
			StringArray active = sensorFactory.createStringArray();
			synchronized(allSensors) {
				try {
					allSensors.add(new URL(sender));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				for (int i = 0; i < DISPLAY_N; ++i) {
					global.getItem().add(allDisplays[i]);
				}
				for (URL url : allSensors) {
					active.getItem().add(url.toString());
				}
			}
			for(String url : active.getItem()) {
				if (ownURL.toString().compareTo(url) != 0) {
					getSensorService(url.toString()).updateAll(ownURL.toString(), global, active);
				}
			}
	    }
	}

	public String askForCoordinator() {
		return coordinatorURL.toString();
	}

	// -----------------------------------------------------------
	
	private int calcValue() {
		long lTicks = new Date().getTime();
		int messwert = ((int) (lTicks % 20000)) / 100;
		if (messwert > 100) {
			messwert = 200 - messwert;
		}
		return messwert;
	}
	

	/**
	 * @param url
	 * @return
	 */
	private Sensorproxy.SensorService getSensorService(String url) {
		try {
			return new SensorServiceService(new URL(url), new QName(
					SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
					.getSensorServicePort();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * @param argument
	 *            expected format: 0 or 1 for each position (ie nw,no,sw,so =>
	 *            1111 or nw,so => 1001)
	 * @return list of own Displays
	 */
	private String[] parseDisplayArgument(String argument) {
		String[] result = new String[4];
		for (int i = 0; i < DISPLAY_N; i++) {
			result[i] = argument.charAt(i) == '1' ? ownURL.toString() : new String("");
		}
		return result;
	}
	

	/**
	 * @param i
	 * @return
	 * @throws MalformedURLException
	 */
	private HAWMeteringWebservice getDisplay(int i)
			throws MalformedURLException {
		return new HAWMeteringWebserviceService(new URL("http://" + "localhost" // TODO
				+ ":9999/hawmetering/" + DISPLAY_POSITIONS[i] + "?wsdl"),
				new QName("http://hawmetering/", "HAWMeteringWebserviceService"))
				.getHAWMeteringWebservicePort();
	}
	
	private StringArray createStringArray(String[] array) {
		StringArray resu = sensorFactory.createStringArray();
		for (String s : array) {
			resu.getItem().add(s);
		}
		return resu;
	}

	public URL getOwnURL() {
		return ownURL;
	}
	
	public void removeFromAll(String url) {
		for (int i = 0; i < DISPLAY_N; ++i) {
			if (this.allDisplays[i].compareTo(url) == 0) {
				this.allDisplays[i] = "";
			}
		}
		try {
			synchronized(allSensors) {
				this.allSensors.remove(new URL(url));
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<URL> getAllSensors() {
		return this.allSensors;
	}
}

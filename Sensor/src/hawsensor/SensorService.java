package hawsensor;

import hawmeterproxy.HAWMeteringWebservice;
import hawmeterproxy.HAWMeteringWebserviceService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import Sensorproxy.SensorServiceService;

@WebService
@SOAPBinding(style = Style.RPC)
public class SensorService {
	private static final int ASK_FOR_COORD_TIMEOUT = 500;
	public static final String NAMESPACE_URI = "http://hawsensor/";
	public static final String LOCAL_PART = "SensorServiceService";
	private URL ownURL;
	private URL coordinatorUrl;
	private boolean coordinator;
	private URL[] activeSensors;
	private int timeout;
	private String[] globalDisplay = new String[4];
	private String[] ownDisplay;
	private ElectionHandler election;
	private CountDownLatch answerElectionTimeout;

	public SensorService(String[] argumentParser) throws Exception {

		try {
			ownURL = new URL("http://localhost:" + argumentParser[0] + "/"
					+ argumentParser[2]);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new Exception("Invalid Port or name entered");
		}
		System.out.println("ownURL: " + ownURL.toString());
		Endpoint.publish(ownURL.toString(), this);

		if (argumentParser[1] != null && argumentParser[1] != "") {
			SensorServiceService service;
			try {
				service = new SensorServiceService(new URL(argumentParser[1]),
						new QName(SensorService.NAMESPACE_URI,
								SensorService.LOCAL_PART));
				Sensorproxy.SensorService sevice_richtig = service
						.getSensorServicePort();

				// bei anderem Sensor nach dem koordinator anfragen
				String koorurl = "";
				koorurl = sevice_richtig.askForCoordinator();
				while (koorurl == "") {
					wait(ASK_FOR_COORD_TIMEOUT);
					koorurl = sevice_richtig.askForCoordinator();
				}

				System.out.println("koorurl: " + koorurl);
				coordinatorUrl = new URL(koorurl);
				service = new SensorServiceService(coordinatorUrl, new QName(
						SensorService.NAMESPACE_URI, SensorService.LOCAL_PART));
				Sensorproxy.SensorService koordi = service
						.getSensorServicePort();
				// TODO:
				// if(koordi.reqDisplay(ownURL.toString(), reqDisplays))
				// if(koordi.register(ownURL.toString(), reqDisplays;)){
				//
				// } else {throw new Exception("No Display acknowledged, shut down.")}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else { // wenn keine sensor url mit gegeben ist dieser der erste ->
					// Koordinator
			coordinatorUrl = ownURL;
			coordinator = true;
			//TODO: set Intervals etc for all Displays
		}
		election = new ElectionHandler(this);
	}

	public void updateAll(@WebParam(name = "sender") String sender,
			@WebParam(name = "globalDisoplay") String[] globalDisoplay,
			@WebParam(name = "activeSensors") String[] activeSensors) {

	}

	public int sendTrigger(@WebParam(name = "sender") String sender) {
		if(sender.equals(coordinatorUrl.toString())){
			try {
				HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(new URL("http://localhost:9999/hawmetering/nw?wsdl"), new QName(
						"http://localhost/","HAWMeteringWebserviceService"));
				HAWMeteringWebservice display = service.getHAWMeteringWebservicePort();
//				display.setValue(val);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	public boolean reqDisplay(@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String[] reqDisplays) {
		return coordinator; // TODO

	}

	public boolean register(@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String[] reqDisplays) {
		return coordinator; // TODO

	}

	public void election(@WebParam(name = "sender") String sender) {
		if (answerElectionTimeout == null
				|| answerElectionTimeout.getCount() <= 0) {
			try {
				election.put(new URL(sender));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	public void answerElection(@WebParam(name = "sender") String sender) {
		if (answerElectionTimeout != null) {
			answerElectionTimeout.countDown();
		}
	}

	public void newCoordinator(@WebParam(name = "sender") String sender) {
		try {
			coordinatorUrl = new URL(sender);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public String askForCoordinator() {
		return coordinatorUrl.toString();
	}

	public URL getOwnURL() {
		return ownURL;
	}

	public URL[] getActiveSensors() {
		return activeSensors;
	}

	public boolean awaitAnswerElectionTimeout() {
		answerElectionTimeout = new CountDownLatch(1);
		try {
			return answerElectionTimeout.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

}

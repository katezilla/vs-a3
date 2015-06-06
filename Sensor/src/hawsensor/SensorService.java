package hawsensor;

import java.net.MalformedURLException;
import java.net.URL;

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
	private URL ownURL;
	private URL coordinatorUrl;
	private boolean  coordinator;
	private URL[] activeSensors;
	private int timeout;
	private String[] globalDisplay = new String[4];
	private String[] ownDisplay;
	private ElectionHandler election;
	
	
	public SensorService(String[] argumentParser) {
		
		try {
			ownURL = new URL("http://localhost:" + argumentParser[0] + "/Sensor");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		System.out.println("ownURL: " + ownURL.toString());
        Endpoint.publish(ownURL.toString(), this);
        
        if (argumentParser[1] != null) {
	        SensorServiceService service;
			try {
				service = new SensorServiceService(new URL(argumentParser[1]), new QName("http://hawsensor/", "SensorServiceService"));
		        Sensorproxy.SensorService sevice_richtig = service.getSensorServicePort();
		        String koorurl = sevice_richtig.askForCoordinator(); // bei anderem Sensor nach dem koordinator anfragen
		        
		        System.out.println("koorurl: " + koorurl);
		        coordinatorUrl = new URL(koorurl);
		        service = new SensorServiceService(coordinatorUrl, new QName("http://hawsensor/", "SensorServiceService"));
		        Sensorproxy.SensorService koordi = service.getSensorServicePort();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
        }else { // wenn keine sensor url mit gegeben ist dieser der erste -> Koordinator
        	coordinatorUrl = ownURL;
        	coordinator = true;
        }
        election = new ElectionHandler();   
	}

	public void updateAll(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "globalDisoplay") String [] globalDisoplay,
			@WebParam(name = "activeSensors") String [] activeSensors) {
		
	}
	
	public int sendTrigger(@WebParam(name = "sender") String sender){
				return 0;
		
	}
	
	public boolean reqDisplay(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String [] reqDisplays) {
				return coordinator; //TODO
		
	}

	
	public boolean register(
			@WebParam(name = "sender") String sender,
			@WebParam(name = "reqDisplays") String [] reqDisplays) {
				return coordinator; //TODO
		
	}
	
	public void election(@WebParam(name = "sender") String sender) {
		
	}
	
	public void answerElection(@WebParam(name = "sender") String sender) {
		
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
	
}

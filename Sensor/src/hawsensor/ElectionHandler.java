package hawsensor;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class ElectionHandler {
	SensorService ownSensor;

    ElectionHandler(final SensorService sensor) {
    	ownSensor= sensor;
    }
    
    public void electione () {
    	boolean voted = false;
    	ArrayList<URL> ourCopy = new ArrayList<URL>();
    	ArrayList<URL> ourServicesCopy = new ArrayList<URL>();
		synchronized(ownSensor.getAllSensors()) {
			for (int i = 0; i < SensorService.DISPLAY_N;i++) {
				ourCopy.add(url);
			}
		}    	
    	for(URL url : ourCopy) {
    		if(compareString(url.toString(), ownSensor.getOwnURL().toString()) == 1) {
    			try {
    				getSensorService(url).election(url.toString());
    			voted = true;
    			} catch(Exception e) {
    				// Connection refused...
    			}
    		}
    	}
    	if(!voted) {
    		ownSensor.removeFromAll(ownSensor.askForCoordinator());    		
    		for(URL url : ourCopy) {
    			try {
    			    getSensorService(url).newCoordinator(ownSensor.getOwnURL().toString());
    			} catch (Exception e) {
    				// Connection refused...
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

    private Sensorproxy.SensorService getSensorService(URL url) throws Exception {
        return new SensorServiceService(url, new QName(
                SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
                .getSensorServicePort();
    }
}

package hawsensor;

/**
 * TODO: entfernen aus liste, wenn nicht reagiert ( Exception)
 */

import hawmeterproxy.HAWMeteringWebservice;
import hawmeterproxy.HAWMeteringWebserviceService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final String IP = "localhost";
    public static final String NAMESPACE_URI = "http://hawsensor/";
    public static final String LOCAL_PART = "SensorServiceService";
    public static final String DISPLAY_POSITIONS[] = { "nw", "no", "sw", "so" };
    private URL ownURL;
    private SensorDBImpl activeSensors;
    private String[] globalDisplay = new String[DISPLAY_N];
    private String[] ownDisplay = new String[DISPLAY_N];
    private String[] requestedDisplays = new String[DISPLAY_N];
    private ElectionHandler election;
    private CountDownLatch timeoutLatchAnswerElection;
    private Thread coordinatorTrigger;
    private Semaphore semBooleanChanged;
    private CountDownLatch timeoutLatchTrigger;

    /**
     * 
     * @param argumentParser
     *            .. at Position 0: port ;Position 1: url ; Position 2: name;
     *            Position 3: display
     * @throws Exception
     *             , if sensor shut down after occured error
     */
    public SensorService(String[] argumentParser) throws Exception {
        semBooleanChanged = new Semaphore(0);
        try {
            ownURL = new URL("http://" + IP + ":" + argumentParser[0] + "/"
                    + argumentParser[2]);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new Exception("Invalid Port or name entered");
        }
        System.out.println("ownURL: " + ownURL.toString());
        Endpoint.publish(ownURL.toString(), this);

        ownDisplay = parseDisplayArgument(argumentParser[3]);
        if (argumentParser[1] != null && argumentParser[1] != "") {
            try {
                Sensorproxy.SensorService sevice_richtig = getSensorService(new URL(
                        argumentParser[1]));

                // bei anderem Sensor nach dem koordinator anfragen
                String koorurl = "";
                koorurl = sevice_richtig.askForCoordinator();
                while (koorurl == "") {
                    wait(ASK_FOR_COORD_TIMEOUT);
                    koorurl = sevice_richtig.askForCoordinator();
                }

                System.out.println("koorurl: " + koorurl);
                URL koor = new URL(koorurl);
                activeSensors.setCoordinator(koorurl);
                semBooleanChanged.release();
                Sensorproxy.SensorService koordi = getSensorService(koor);

                StringArray reqDisplays = getStringArray(ownDisplay);
                if (koordi.reqDisplay(ownURL.toString(), reqDisplays)) {
                    if (koordi.register(ownURL.toString(), reqDisplays)) {
                        // TODO: ready, check if functionality is needed
                    } else {
                        // this exception is being caught by Sensors main
                        throw new Exception(
                                "No Display acknowledged, shut down.");
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new Exception("URL invalid, shut down");
            }
        } else { // wenn keine sensor url mit gegeben ist dieser der erste ->
                 // Koordinator
            // set Intervals etc for all Displays if wanted
            // HAWMeteringWebservice display;
            // for (int i = 0; i < DISPLAY_N; i++) {
            // display = getDisplay(i);
            // display.clearIntervals();
            // display.setIntervals(DISPLAY_POSITIONS[i].toUpperCase(),
            // MIN_VALUE,
            // MAX_VALUE, color);
            // }

            activeSensors.setCoordinator(ownURL.toString());
            // initialize activeSensors and globalDisplay
            URL[] sensorList = activeSensors.getSensors();
            sensorList[0] = ownURL;
            activeSensors.setSensors(sensorList, ownURL.toString());
            globalDisplay = ownDisplay.clone();
        }
        election = new ElectionHandler(this, activeSensors, semBooleanChanged); // TODO:
                                                                                // shutdown
        coordinatorTrigger = new Thread(new CoordinatorTrigger(
                ownURL.toString(), semBooleanChanged, activeSensors));
        coordinatorTrigger.start();// TODO: shutdown
        while (true) {
            timeoutLatchTrigger = new CountDownLatch(1);
            try {
                timeoutLatchTrigger.await(TIMEOUT_TRIGGER,
                        TimeUnit.MILLISECONDS);
                // was not interrupted by a new trigger => start election
                election(getOwnURL().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
            if (argument.charAt(i) == '1') {
                result[i] = ownURL.toString();
            }
        }
        return result;
    }

    /**
     * @param url
     * @return
     */
    private Sensorproxy.SensorService getSensorService(URL url) {
        return new SensorServiceService(url, new QName(
                SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
                .getSensorServicePort();
    }

    public synchronized void updateAll(
            @WebParam(name = "sender") String sender,
            @WebParam(name = "globalDisoplay") String[] globalDisoplay,
            @WebParam(name = "activeSensors") String[] activeSensors) {
        if (this.activeSensors.isCoordinator(sender)) {
            this.globalDisplay = globalDisoplay;
            this.requestedDisplays = new String[DISPLAY_N];
            URL[] urlList = new URL[DISPLAY_N];
            for (int i = 0; i < DISPLAY_N; i++) {
                try {
                    urlList[i] = new URL(activeSensors[i]);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            this.activeSensors.setSensors(urlList, sender);
        } else {
            System.err.println("Received updateAll call from: " + sender);
        }
    }

    /**
     * coordinator activates this method to trigger each sensor every 2 sec
     * 
     * @param sender
     * @return .. 0, if successfully updated; -1, if update failed
     */
    public int sendTrigger(@WebParam(name = "sender") String sender) {
        if (activeSensors.isCoordinator(sender)) {
            timeoutLatchTrigger.countDown();
            HAWMeteringWebservice display;
            for (int i = 0; i < DISPLAY_N; i++) {
                try {
                    display = getDisplay(i);
                    display.setValue(calcValue());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return -1;
                }
            }
        } else {
            System.err.println("Received sendTrigger call from: " + sender);
            return -1;
        }
        return 0;
    }

    /**
     * @param i
     * @return
     * @throws MalformedURLException
     */
    private HAWMeteringWebservice getDisplay(int i)
            throws MalformedURLException {
        return new HAWMeteringWebserviceService(new URL("http://" + IP
                + ":9999/hawmetering/" + DISPLAY_POSITIONS[i] + "?wsdl"),
                new QName("http://" + IP + "/", "HAWMeteringWebserviceService"))
                .getHAWMeteringWebservicePort();
    }

    public synchronized boolean reqDisplay(
            @WebParam(name = "sender") String sender,
            @WebParam(name = "reqDisplays") String[] reqDisplays) {
        boolean result = true;
        if (activeSensors.isCoordinator(ownURL.toString())) {
            for (int i = 0; i < DISPLAY_N; i++) {
                if (reqDisplays[i] != null && reqDisplays[i] != "") {
                    if (globalDisplay[i] != ""
                            || this.requestedDisplays[i] != "") {
                        result = false;
                        System.out.println("Requested display " + i
                                + " already registered by " + globalDisplay[i]
                                + ".");
                        requestedDisplays = cleanDisplays(requestedDisplays,
                                sender);
                        break;
                    } else {
                        requestedDisplays[i] = reqDisplays[i];
                    }
                }
            }
        } else {
            System.err.println("Received reqDisplay call from: " + sender
                    + ", even though I'm not coordinator.");
            result = false;
        }
        return result;
    }

    public synchronized boolean register(
            @WebParam(name = "sender") String sender,
            @WebParam(name = "reqDisplays") String[] reqDisplays) {
        boolean result = true;
        if (activeSensors.isCoordinator(ownURL.toString())) {
            for (int i = 0; i < DISPLAY_N; i++) {
                if (reqDisplays[i] != null && reqDisplays[i] != "") {
                    if (globalDisplay[i] != ""
                            || !(this.requestedDisplays[i]
                                    .equals(reqDisplays[i]))) {
                        System.out.println("Requested display " + i
                                + " registered by " + globalDisplay[i]
                                + " or requested by " + requestedDisplays[i]
                                + ".");
                        requestedDisplays = cleanDisplays(requestedDisplays,
                                sender);
                        globalDisplay = cleanDisplays(globalDisplay, sender);
                        result = false;
                        break;
                    } else {
                        globalDisplay[i] = reqDisplays[i];
                    }
                }
            }
        } else {
            System.err.println("Received reqDisplay call from: " + sender
                    + ", even though I'm not coordinator.");
            result = false;
        }
        if (result) {
            StringArray globalDisplays = getStringArray(globalDisplay);
            StringArray activeSensors = getStringArray(this.activeSensors
                    .getSensors());
            for (URL sensor : this.activeSensors.getSensors()) {
                getSensorService(sensor).updateAll(ownURL.toString(),
                        globalDisplays, activeSensors);
            }
        }
        return result;
    }

    /**
     * @param displays
     * @param sender
     */
    private String[] cleanDisplays(String[] displays, String sender) {
        // clean up requestedDisplays
        for (int i = 0; i < DISPLAY_N; i++) {
            if (displays[i].equals(sender)) {
                displays[i] = "";
            }
        }
        return displays;
    }

    public void election(@WebParam(name = "sender") String sender) {
        if (timeoutLatchAnswerElection.getCount() <= 0
                || timeoutLatchAnswerElection == null) {
            try {
                election.put(new URL(sender));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void answerElection(@WebParam(name = "sender") String sender) {
        if (timeoutLatchAnswerElection != null) {
            timeoutLatchAnswerElection.countDown();
        }
    }

    public void newCoordinator(@WebParam(name = "sender") String sender) {
        try {
            new URL(sender);
            activeSensors.setCoordinator(sender);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public String askForCoordinator() {
        return activeSensors.getCoordinator();
    }

    public URL getOwnURL() {
        return ownURL;
    }

    public boolean awaitAnswerElectionTimeout() {
        timeoutLatchAnswerElection = new CountDownLatch(1);
        try {
            return timeoutLatchAnswerElection.await(timeoutAnswerElection,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int calcValue() {
        long lTicks = new Date().getTime();
        int messwert = ((int) (lTicks % 20000)) / 100;
        if (messwert > 100) {
            messwert = 200 - messwert;
        }
        return messwert;
    }

    private StringArray getStringArray(String[] strings) {
        StringArray reqDisplays = new StringArray();
        for (String string : strings) {
            reqDisplays.getItem().add(string);
        }
        return reqDisplays;
    }

    private StringArray getStringArray(URL[] sensors) {
        StringArray reqDisplays = new StringArray();
        for (URL url : sensors) {
            reqDisplays.getItem().add(url.toString());
        }
        return reqDisplays;
    }
}

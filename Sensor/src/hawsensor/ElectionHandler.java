package hawsensor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class ElectionHandler {

    private Queue<URL> messages;

    Thread worker;

    private Semaphore sem_queue = new Semaphore(0);

    ElectionHandler(final SensorService ownSensor, final String [] allSensors) {
        messages = new LinkedList<URL>();
        worker = new Thread() {
            @Override
            public void run() {
                URL message = null;
                Sensorproxy.SensorService messageService;
                Sensorproxy.SensorService sensor = null;
                boolean noLargerUrl = true;
                while (true) {
                    try {
                        sem_queue.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    message = messages.poll();
                    if (message != null) {
                        if (compareString(message.toString(), ownSensor.getOwnURL().toString()) == 0) {
                            // election started by me
                        } else {
                            messageService = getSensorService(message);
                            messageService.answerElection(ownSensor.getOwnURL()
                                    .toString());
                        }
                        // HOLD_ELECTION
                        for (String url : allSensors) {
                            if (compareString(url, ownSensor.getOwnURL().toString()) == 1) {
                                try {
									sensor = getSensorService(new URL(url));
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
                                sensor.election(ownSensor.getOwnURL()
                                        .toString());
                                noLargerUrl = false;
                            }
                        }
                        if (!noLargerUrl) {
                            // timeout
                            if (!ownSensor.awaitAnswerElectionTimeout()) {
                                //semBooleanChanged.release();
                                // signal coordinatorTrigger, that he can work
								for (String url : allSensors) {
									try {
										sensor = getSensorService(new URL(url));
									} catch (MalformedURLException e) {
										e.printStackTrace();
									}
									sensor.newCoordinator(ownSensor.getOwnURL()
											.toString());

								}
                            } else {
                                // there is another Sensor that is more capable
                            }
                        } // if !noLargerUrl

                    } // if message != null
                    else {
                        break;
                    }
                } // while true
            }

            private Sensorproxy.SensorService getSensorService(URL url) {
                return new SensorServiceService(url, new QName(
                        SensorService.NAMESPACE_URI, SensorService.LOCAL_PART))
                        .getSensorServicePort();
            }
        };
        worker.start();

    }

    public void put(URL url) {
        messages.offer(url);
        sem_queue.release();
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

    public void shutdown() {
        sem_queue.release();
    }
}

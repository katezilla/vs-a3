package hawsensor;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.xml.namespace.QName;

import Sensorproxy.SensorServiceService;

public class ElectionHandler {

    private Queue<URL> messages;

    Thread worker;

    private Semaphore sem_queue;

    ElectionHandler(final SensorService ownSensor,
            final SensorDBImpl activeSensors, final Semaphore semBooleanChanged) {
        messages = new LinkedList<URL>();
        worker = new Thread() {
            @Override
            public void run() {
                URL message = null;
                Sensorproxy.SensorService messageService;
                Sensorproxy.SensorService sensor;
                boolean noLargerUrl = true;
                while (true) {
                    try {
                        sem_queue.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    message = messages.poll();
                    if (message != null) {
                        if (compareUrl(message, ownSensor.getOwnURL()) == 0) {
                            // election started by me
                        } else {
                            messageService = getSensorService(message);
                            messageService.answerElection(ownSensor.getOwnURL()
                                    .toString());
                        }
                        // HOLD_ELECTION
                        for (URL url : activeSensors.getSensors()) {
                            if (compareUrl(url, ownSensor.getOwnURL()) == 1) {
                                sensor = getSensorService(url);
                                sensor.election(ownSensor.getOwnURL()
                                        .toString());
                                noLargerUrl = false;
                            }
                        }
                        if (!noLargerUrl) {
                            // timeout
                            if (!ownSensor.awaitAnswerElectionTimeout()) {
                                // I'm the Coordinator, bow down!
                                activeSensors.setCoordinator(ownSensor
                                        .getOwnURL().toString());
                                semBooleanChanged.release();
                                // signal coordinatorTrigger, that he can work
                                for (URL url : activeSensors.getSensors()) {
                                    if (compareUrl(url, ownSensor.getOwnURL()) == -1) {
                                        sensor = getSensorService(url);
                                        sensor.newCoordinator(ownSensor
                                                .getOwnURL().toString());
                                    }
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
    protected int compareUrl(URL url, URL ownURL) {
        int result = url.toString().compareTo(ownURL.toString());
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

package hawsensor;

import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

public class ElectionHandler {
	
	private LinkedBlockingQueue<URL> messages = null;
	
	Thread worker;
	
	ElectionHandler() {
		messages = new LinkedBlockingQueue<URL>();
		worker = new Thread() {
			@Override
			public void run() {
				try {
					messages.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		worker.start();
		
		
	}
	
	public void put(URL url) {
		try {
			messages.put(url);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}

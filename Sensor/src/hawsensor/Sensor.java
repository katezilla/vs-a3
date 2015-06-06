package hawsensor;

import javax.xml.ws.Endpoint;

public class Sensor {

	public static void main(String[] args) {
		SensorService sensor = new SensorService(argumentParser(args));
		
		while(true) {
			
		}
	}
	
	private static String[] argumentParser(String[] args) {
		String [] resu = new String[2];
		for (int i = 0; i < args.length; ++i) {
			if (args[i].contains("--port=")) {
				resu[0] = readArgument(args[i]);
			}
			if (args[i].contains("--url=")) {
				resu[1] = readArgument(args[i]);
			}

			if (args[i].contains("--help")) {
				printHelpMessage();
				System.exit(0);
			}
		}
		return resu;
		
	}

	private static String readArgument(final String line) {
		String[] splitted = line.split("=");
		return splitted.length == 2 ? splitted[1] : "";
	}

	private static void printHelpMessage() {
		StringBuilder str = new StringBuilder();
		str.append("Usage: java -cp . Sensor [Options...]\n");
		str.append("Arguments:\n");
		str.append("--port=arg          Set the port for new Sensor\n");
		str.append("--url=arg           Set url for Sensor that will be asked\n");
		str.append("--help              Print this help message\n");
		System.out.println(str);
	}
}

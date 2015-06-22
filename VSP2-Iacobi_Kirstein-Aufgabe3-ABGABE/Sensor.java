package hawsensor;

public class Sensor {

	public static void main(String[] args) {
		try {
			String[] arguments = argumentParser(args);
			if (arguments[0] == null || arguments[2] == null
					|| arguments[3] == null) {
				System.out.println("Arguments are not valid.");
				printHelpMessage();
				return;
			}
			SensorService sensor = new SensorService(arguments);
			while (true) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String[] argumentParser(String[] args) {
		String[] resu = new String[4];
		for (int i = 0; i < args.length; ++i) {
			if (args[i].contains("--name=")) {
				resu[2] = readArgument(args[i]);
			}
			if (args[i].contains("--port=")) {
				resu[0] = readArgument(args[i]);
			}
			if (args[i].contains("--url=")) {
				resu[1] = readArgument(args[i]);
			}
			if (args[i].contains("--display=")) {
				resu[3] = readArgument(args[i]);
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
		str.append("--display=arg       Set to expected format: 0 or 1 for each position (ie nw,no,sw,so => 1111 or nw,so => 1001)\n");
		str.append("--help              Print this help message\n");
		System.out.println(str);
	}
}

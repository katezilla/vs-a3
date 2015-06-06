/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hawmeterclient;

import hawmeterproxy.HAWMeteringWebservice;
import hawmeterproxy.HAWMeteringWebserviceService;
import hawmeterproxy.WebColor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

/**
 *
 * @author heitmann
 */
public class HAWMeterClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            URL url = null;
            URL baseUrl;
            baseUrl = HAWMeteringWebserviceService.class.getResource(".");
            boolean success = false;
            if (args.length >= 2) {
                url = new URL(baseUrl, args[0]);

                HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(url, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
                HAWMeteringWebservice metering = service.getHAWMeteringWebservicePort();
                String cmd = args[1];
                success = true;
                if ("setRange".equals(cmd) && args.length == 4) {
                    double min = Double.parseDouble(args[2]);
                    double max = Double.parseDouble(args[3]);
                    metering.setRange(min, max);
                } else if ("setValue".equals(cmd) && args.length == 3) {
                    double val = Double.parseDouble(args[2]);
                    metering.setValue(val);
                } else if ("setIntervals".equals(cmd) && args.length == 9) {
                    String label = args[2];
                    double min = Double.parseDouble(args[3]);
                    double max = Double.parseDouble(args[4]);

                    WebColor color = new WebColor();
                    color.setRed(Integer.parseInt(args[5]));
                    color.setGreen(Integer.parseInt(args[6]));
                    color.setBlue(Integer.parseInt(args[7]));
                    color.setAlpha(Integer.parseInt(args[8]));

                    metering.setIntervals(label, min, max, color);
                } else if ("clearIntervals".equals(cmd) && args.length == 2) {
                    metering.clearIntervals();
                } else if ("setTitle".equals(cmd) && args.length == 3) {
                    String title = args[2];
                    metering.setTitle(title);
                } else {
                    success = false;
                }
            }

            if (!success) {
                System.out.println("Usage:");
                System.out.println("   <url> setRange <min> <max>");
                System.out.println("   <url> setValue <val>");
                System.out.println("   <url> setIntervals <label> <min> <max> <red> <green> <blue> <alpha>");
                System.out.println("   <url> clearIntervals");
                System.out.println("   <url> setTitle <title>");
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(HAWMeterClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hawmetering;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.xml.ws.Endpoint;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class HAWMetering extends ApplicationFrame {

    private DialChart chart[];

    public HAWMetering(String s) {
        super(s);
        URL url = HAWMetering.class.getResource("/favicon.png");
        ImageIcon im = new ImageIcon(url);
        this.setIconImage(im.getImage());
        this.setTitle("HAW Metering");

        JPanel jpanel = createMeteringPanel();
        jpanel.setPreferredSize(new Dimension(700, 500));
        setContentPane(jpanel);
    }

    public JPanel createMeteringPanel() {
        chart = new DialChart[4];
        JPanel jpanel = new JPanel(new GridLayout(2, 2));
        chart[0] = new DialChart();
        chart[0].createChart("NW");
        jpanel.add(chart[0].getChartPanel());
        chart[1] = new DialChart();
        chart[1].createChart("NO");
        jpanel.add(chart[1].getChartPanel());
        chart[2] = new DialChart();
        chart[2].createChart("SW");
        jpanel.add(chart[2].getChartPanel());
        chart[3] = new DialChart();
        chart[3].createChart("SO");
        jpanel.add(chart[3].getChartPanel());
        return jpanel;
    }

    private void startWebservice() {
        String names[] = {"nw", "no", "sw", "so"};
        for (int i = 0; i < chart.length; i++) {
            HAWMeteringWebservice webservice = new HAWMeteringWebservice(chart[i]);
            Endpoint.publish("http://lab26:9999/hawmetering/" + names[i], webservice);
        }
    }

    public static void main(String args[]) {
        HAWMetering dialdemo = new HAWMetering("HAW Metering");
        dialdemo.pack();
        RefineryUtilities.centerFrameOnScreen(dialdemo);
        dialdemo.setVisible(true);

        dialdemo.startWebservice();
    }
}

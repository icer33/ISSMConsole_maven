/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Icer
 */
public class IPUtil {

    public static String getMyIP() {
        for (int i = 0; i < 3; i++) {
            try {
                return getUrlSource("http://checkip.dyndns.org/").split("Address:")[1].split("</body")[0].trim();
            } catch (IOException ex) {
                Logger.getLogger(IPUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "{Unable to get IP}";
    }

    private static String getUrlSource(String url) throws IOException {
        URL yahoo = new URL(url);
        URLConnection yc = yahoo.openConnection();
        yc.setConnectTimeout(60000);
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            a.append(inputLine);
        }
        in.close();

        return a.toString();
    }
}

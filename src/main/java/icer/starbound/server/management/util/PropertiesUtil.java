/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Icer
 */
public class PropertiesUtil {

    private final static Properties prop = new Properties();
    public final static String SERVER_LOCATION_KEY = "SERVER_FILE_LOCATION";
    public final static String TCP_REDIRECT_KEY = "ALLOW_TCP_REDIRECT";

    public static void save() {
        try {
            prop.store(new FileOutputStream("ISSMConsole.properties"), null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void load() {
        try {
            prop.load(new FileInputStream("ISSMConsole.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized static String getProperty(String key) {
        return prop.getProperty(key);
    }

    public synchronized static Object setProperty(String key, String value) {
        return prop.setProperty(key, value);
    }
}

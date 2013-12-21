/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eclipse.jetty.servlets;

import icer.starbound.server.management.jetty.servlets.MyEventSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Icer
 */
public class EventSouceRegistry {
    static List<MyEventSource> list = new ArrayList<>();

    public static boolean add(MyEventSource e) {
        System.out.println("Added Event Source");
        return list.add(e);
    }

    public static boolean remove(MyEventSource o) {
        System.out.println("Removed Event Source");
        return list.remove(o);
    }
    
    public void sendEvent(String str){
        for (MyEventSource eventSource : list) {
            try {
                eventSource.emitEvent(str);
            } catch (IOException ex) {
                Logger.getLogger(EventSouceRegistry.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

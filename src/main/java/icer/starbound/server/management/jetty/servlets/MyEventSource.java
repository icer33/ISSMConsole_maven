/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty.servlets;

import java.io.IOException;
import org.eclipse.jetty.servlets.EventSouceRegistry;

/**
 *
 * @author Icer
 */
public class MyEventSource implements org.eclipse.jetty.servlets.EventSource {

    private Emitter emitter;

    
    @Override
    public void onOpen(Emitter emitter) throws IOException {
        this.emitter = emitter;
        EventSouceRegistry.add(this);
    }

    public void emitEvent(String dataToSend) throws IOException {
        this.emitter.data(dataToSend);
    }

    @Override
    public void onClose() {
        EventSouceRegistry.remove(this);
    }
}

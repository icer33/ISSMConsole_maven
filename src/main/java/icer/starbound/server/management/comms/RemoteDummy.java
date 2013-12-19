/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.comms;

import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.client.StarboundServerGUI;
import java.io.IOException;

/**
 *
 * @author Icer
 */
public class RemoteDummy extends RemoteManagement{

    public RemoteDummy(boolean server) throws IOException {
        super(server);
    }

    @Override
    public <T> T sendRequest(String method, Object... args) {
        return null;
    }

    @Override
    public void start(String ip) {
    }

    @Override
    public void setServer(StarboundServer server) {
    }

    @Override
    public void setGui(StarboundServerGUI gui) {
    }
}

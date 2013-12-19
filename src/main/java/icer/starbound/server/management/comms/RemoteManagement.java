/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.comms;

import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.client.StarboundServerGUI;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Icer
 */
public class RemoteManagement {

    RemoteAccess local;
    RemoteAccess remote;
    boolean server = false;

    public RemoteManagement(boolean server) throws IOException {
        this.server = server;
        if (server) {
            local = new RemoteAccess("localhost", RemoteAccess.REMOTE_ACCESS_PORT_SERVER);
            remote = new RemoteAccess(null, RemoteAccess.REMOTE_ACCESS_PORT_CLIENT);
        } else {
            local = new RemoteAccess("localhost", RemoteAccess.REMOTE_ACCESS_PORT_CLIENT);
            remote = new RemoteAccess(null, RemoteAccess.REMOTE_ACCESS_PORT_SERVER);
        }

//
//        new Thread() {
//            @Override
//            public void run() {
//                while (!remote.isConnected()) {
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                System.out.println("--------------------------------------------------------------");
//                int n = 30;
//                ExecutorService es = Executors.newFixedThreadPool(n);
//                Future[] f = new Future[n];
//                for (int i = 0; i < n; i++) {
//                    final int a = i;
//                    f[i] = es.submit(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                local.sendRequest("getA");
//                                System.out.println(a);
//                            } catch (IOException ex) {
//                                Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        }
//                    });
//                }
//                for (Future future : f) {
//                    try {
//                        future.get();
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (ExecutionException ex) {
//                        Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                System.out.println("I'm out!");
//            }
//        }.start();
    }

    public <T> T sendRequest(String method, Object... args) {
        try {
            if (local.isConnected()) {
                local.sendRequest(method, args);
            }
        } catch (IOException ex) {
            Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void start(String ip) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (server) {
                        local.startClient("localhost", RemoteAccess.REMOTE_ACCESS_PORT_SERVER);
                        remote.startServer(RemoteAccess.REMOTE_ACCESS_PORT_CLIENT);
                    } else {
                        local.startClient("localhost", RemoteAccess.REMOTE_ACCESS_PORT_CLIENT);
                        remote.startServer(RemoteAccess.REMOTE_ACCESS_PORT_SERVER);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(RemoteManagement.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    public void setServer(StarboundServer server) {
        remote.setServer(server);
        local.setServer(server);
    }

    public void setGui(StarboundServerGUI gui) {
        remote.setGui(gui);
        remote.setGui(gui);
    }

    public static void main(String[] args) throws IOException {
        RemoteManagement rm1 = new RemoteManagement(true);
        RemoteManagement rm2 = new RemoteManagement(false);
        rm1.start("localhost");
        rm2.start("localhost");
    }
}

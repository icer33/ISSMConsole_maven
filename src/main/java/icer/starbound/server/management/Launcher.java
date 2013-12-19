/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management;

import icer.starbound.server.management.jetty.ConsoleServlet;
import icer.starbound.server.management.jetty.HelloServlet;
import icer.starbound.server.management.jetty.MainPage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author Icer
 */
public class Launcher {

    public static void main(String[] args) {

        final StarboundServer starboundServer = new StarboundServer();
        new Thread() {
            public void run() {
                try {
                    starboundServer.startServer("D:\\SteamLibrary\\SteamApps\\common\\Starbound\\win32\\starbound_server.exe");
                } catch (Exception ex) {
                    Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();

        Server server = new Server(21005);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
 
        context.addServlet(new ServletHolder(new MainPage(starboundServer)),"/*");
        context.addServlet(new ServletHolder(new ConsoleServlet(starboundServer)),"/console/*");
        context.addServlet(new ServletHolder(new HelloServlet("Buongiorno Mondo")),"/it/*");
        context.addServlet(new ServletHolder(new HelloServlet("Bonjour le Monde")),"/fr/*");
 
        
        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }


//        new Thread() {
//            public void run() {
//                try {
//                    Thread.sleep(100000);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                final JFrame frame = new JFrame();
//
//                StarboundServerGUI starboundServerGUI = new StarboundServerGUI(frame);
//                frame.add(starboundServerGUI);
//                frame.pack();
//                frame.setTitle("Icer's Starbound Server Management Console v" + StarboundServer.MANAGEMENT_VERSION_NUMBER);
//                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//                frame.setVisible(true);
//
//                starboundServerGUI.init();
//            }
//        }.start();
    }
}

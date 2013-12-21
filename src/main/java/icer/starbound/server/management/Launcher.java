/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management;

import icer.starbound.server.management.jetty.servlets.ConsoleServlet;
import icer.starbound.server.management.jetty.servlets.CurrentStateServlet;
import icer.starbound.server.management.jetty.servlets.HelloServlet;
import icer.starbound.server.management.jetty.servlets.MyEventSourceServlet;
import icer.starbound.server.management.jetty.servlets.SSEServlet;
import icer.starbound.server.management.jetty.servlets.ServerListenerServlet;
import icer.starbound.server.management.jetty.servlets.ServerTimeServlet;
import icer.starbound.server.management.util.PropertiesUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.EventSourceServlet;

/**
 *
 * @author Icer
 */
public class Launcher {

    public static void main(String[] args) {
        PropertiesUtil.load();

        final StarboundServer starboundServer = new StarboundServer();
        new Thread() {
            public void run() {
                try {
                    starboundServer.startServer();
                } catch (Exception ex) {
                    Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();

        Server server = new Server(21005);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");




//WebAppContext webcontext = new WebAppContext();
//webcontext.setContextPath("/");
//ResourceCollection resources = new ResourceCollection(new String[] {
//            "project/webapp/folder",
//    "/root/static/folder/A",    
//    "/root/static/folder/B",    
//});
//webcontext.setBaseResource(resources);
//server.setHandler(webcontext);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase("./src/main/webapp/");

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(resourceHandler);
        handlerList.addHandler(context);
        server.setHandler(handlerList);
//        server.setHandler(context);

        final MyEventSourceServlet ess = new MyEventSourceServlet();
        final ServerListenerServlet serverListenerServlet = new ServerListenerServlet();
        starboundServer.addServerListener(serverListenerServlet);

//        context.addServlet(new ServletHolder(new MainPage(starboundServer)), "/*");
        context.addServlet(new ServletHolder(new ConsoleServlet(starboundServer)), "/console/*");
        context.addServlet(new ServletHolder(new CurrentStateServlet(starboundServer)), "/state/*");
        context.addServlet(new ServletHolder(serverListenerServlet), "/sse/*");
        context.addServlet(new ServletHolder(new HelloServlet("Buongiorno Mondo")), "/it/*");
        context.addServlet(new ServletHolder(new HelloServlet("Bonjour le Monde")), "/fr/*");

        System.out.println(System.currentTimeMillis());

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

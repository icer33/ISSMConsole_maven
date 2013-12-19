/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty;

import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.pojos.ServerState;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author Icer
 */
public class ServerStateHandler extends AbstractHandler {

    StarboundServer server;

    public ServerStateHandler(StarboundServer server) {
        this.server = server;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<h1>Hello World</h1>");

        StringBuilder sb = new StringBuilder();
        ServerState si = server.getStartingInformation();
        sb.append("<h1> Starbound Server Console</h1>");
        sb.append(createH3("IP", si.getIp()));
        sb.append(createH3("Status", si.getStatus().toString()));
        sb.append(createH3("location", si.getLocation()));
        sb.append(createH3("version", si.getVersion()));
        sb.append("<h2> Console</h2>");
        for (String str : si.getConsoleMessages()) {
            sb.append("<h5>" + str + "</h5>");
        }


        sb.append("<script>setTimeout(\"location.reload(true);\", 1000);</script>\n");

        response.getWriter().println(sb.toString());
    }

    private String createH3(String a, String b) {
        return "<h3>" + a + " = " + b + "</h3>\n";
    }
}
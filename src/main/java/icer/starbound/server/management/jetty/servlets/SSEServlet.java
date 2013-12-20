/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty.servlets;

import icer.starbound.server.management.StarboundServer;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Chipmonk
 */
public class SSEServlet extends HttpServlet {

    StarboundServer server;

    public SSEServlet(StarboundServer server) {
        this.server = server;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/event-stream");
        response.setStatus(HttpServletResponse.SC_OK);
        

        response.getWriter().println(System.currentTimeMillis() + "<br />");
    }
}

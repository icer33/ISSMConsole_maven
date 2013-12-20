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
 * @author Icer
 */
public class CurrentStateServlet extends HttpServlet {

    StarboundServer server;

    public CurrentStateServlet(StarboundServer server) {
        this.server = server;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        ObjectMapper mapper = new ObjectMapper();
        
        // convert user object to json string, and save to a file
        mapper.writeValue(response.getOutputStream(), server.getStartingInformation());
    }
}

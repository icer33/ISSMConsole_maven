/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty.servlets;

import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.pojos.ServerState;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Icer
 */
public class ConsoleServlet extends HttpServlet {

    StarboundServer server;

    public ConsoleServlet(StarboundServer server) {
        this.server = server;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        StringBuilder sb = new StringBuilder();
        ServerState si = server.getStartingInformation();
        for (String string : si.getConsoleMessages()) {
            sb.append(string+"\n");
        }
        response.getWriter().println(sb.toString());
    }

    private String createH3(String a, String b) {
        return "<h3>" + a + " = " + b + "</h3>\n";
    }

    private String createDiv(String a, String b) {
        return "<div>" + a + " = " + b + "</div>\n";
    }
}

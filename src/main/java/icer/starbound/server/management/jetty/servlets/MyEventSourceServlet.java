/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty.servlets;

import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.servlets.EventSource;

/**
 *
 * @author Icer
 */
public class MyEventSourceServlet extends org.eclipse.jetty.servlets.EventSourceServlet
{
    @Override
    protected EventSource newEventSource(HttpServletRequest request)
    {
        return new MyEventSource();
    }
}
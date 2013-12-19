/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management;

import icer.starbound.server.management.proxy.ProxyActions;

/**
 *
 * @author Icer
 */
public interface ServerCommandListener {

    public void banPlayer(String ip, ProxyActions action);
}

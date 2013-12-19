/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.proxy;

/**
 *
 * @author Icer
 */
public enum ProxyActions {

    NOTHING("----"),
    KICK("Kick"),
    BAN_1_HOUR("Ban for 1 hour"),
    BAN_1_DAY("Ban for 1 day"),
    BAN_FOR_X("Ban for ..."),
    PERMANENT_BAN("Permanent Ban");
    String text = "";

    private ProxyActions(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

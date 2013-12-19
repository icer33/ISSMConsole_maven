/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.proxy;

/**
 *
 * @author Icer
 */
public class BannedPlayer {
    String ip;
    long bannedUntil;

    public BannedPlayer(String ip, long bannedUntil) {
        this.ip = ip;
        this.bannedUntil = bannedUntil;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(long bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    @Override
    public String toString() {
        return "BannedPlayer{" + "ip=" + ip + ", bannedUntil=" + bannedUntil + '}';
    }
}

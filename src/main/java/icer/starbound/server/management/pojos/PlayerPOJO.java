/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.pojos;

import icer.starbound.server.management.JoinFailed;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Icer
 */
public class PlayerPOJO implements Serializable{
    String name;
    String ip;
    long timeConnected;
    long bannedUntil;
    JoinFailed failedReason;
//    List<ChatPOJO> chats = new ArrayList<>();

    public PlayerPOJO(String name, String ip) {
        this.name = name;
        this.ip = ip;
        timeConnected = System.currentTimeMillis();
    }

    public long getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(long bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public JoinFailed getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(JoinFailed failedReason) {
        this.failedReason = failedReason;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public long getTimeConnected() {
        return timeConnected;
    }

//    public List<ChatPOJO> getChats() {
//        return chats;
//    }
//
//    public boolean addChat(ChatPOJO e) {
//        return chats.add(e);
//    }
}

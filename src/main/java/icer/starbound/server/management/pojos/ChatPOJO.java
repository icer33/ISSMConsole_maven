/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.pojos;

import java.io.Serializable;

/**
 *
 * @author Icer
 */
public class ChatPOJO implements Serializable{

    String text;
    String user;
    long timeStamp;

    public ChatPOJO(String text, String user) {
        this.text = text;
        this.user = user;
        timeStamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public String getUser() {
        return user;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}

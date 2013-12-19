/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.pojos;

import icer.starbound.server.management.ServerStatus;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Icer
 */
public class ServerState implements Serializable{

    String location="";
    String config = "";
    String version="";
    ServerStatus status= ServerStatus.Loading;
    String ip="";
    List<WorldPOJO> worlds;
    List<String> whiteList;
    List<PlayerPOJO> bannedPlayers;
    List<String> admins;
    boolean redirectActive = true;
    List<PlayerPOJO> activePlayers;
    List<PlayerPOJO> failedToJoin;
    List<String> consoleMessages;

    public ServerState() {
        worlds = new ArrayList<>();
        whiteList = new ArrayList<>();
        bannedPlayers = new ArrayList<>();
        admins = new ArrayList<>();
        activePlayers = new ArrayList<>();
        failedToJoin = new ArrayList<>();
        consoleMessages = new ArrayList<>();
    }
    
    public void clearConsoleMessages(){
        consoleMessages.clear();
    }

    public List<String> getConsoleMessages() {
        return consoleMessages;
    }

    public List<WorldPOJO> getWorlds() {
        return worlds;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public List<PlayerPOJO> getBannedPlayers() {
        return bannedPlayers;
    }

    public List<String> getAdmins() {
        return admins;
    }

    public List<PlayerPOJO> getActivePlayers() {
        return activePlayers;
    }

    public List<PlayerPOJO> getFailedToJoin() {
        return failedToJoin;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isRedirectActive() {
        return redirectActive;
    }

    public void setRedirectActive(boolean redirectActive) {
        this.redirectActive = redirectActive;
    }    
}

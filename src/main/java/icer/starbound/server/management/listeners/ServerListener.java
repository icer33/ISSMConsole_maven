/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.listeners;

import icer.starbound.server.management.enums.JoinFailed;
import icer.starbound.server.management.enums.ServerStatus;
import icer.starbound.server.management.pojos.PlayerPOJO;
import icer.starbound.server.management.pojos.ChatPOJO;
import icer.starbound.server.management.pojos.ServerState;
import java.util.List;

/**
 *
 * @author Icer
 */
public interface ServerListener {

    public void newLine(String line);

    public void playerJoined(PlayerPOJO player);

    public void playerJoinFailed(PlayerPOJO player, JoinFailed failed);

    public void playerLeft(PlayerPOJO player);

    public void serverLocation(String location);

    public void serverVersion(String version);

    public void serverStatus(ServerStatus status);

    public void serverIP(String ip);

    public void chatMessage(ChatPOJO chat);

    public void worldLoaded(String sector, String coordinates);

    public void worldUnloaded(String sector, String coordinates);

    public void playerBanned(String ip, long until);

    public void playerKicked(String ip);

    public void playerAddedtoWhitelist(String ip);

    public void playerAddedToAdmins(String ip);

    public void tcpRedirectActive(boolean active);

    public void recieveStartingInformation(ServerState serverState);

    public ServerState getStartingInformation();
}

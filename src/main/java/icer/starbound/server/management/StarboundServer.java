/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management;

import icer.starbound.server.management.enums.ServerStatus;
import icer.starbound.server.management.enums.JoinFailed;
import icer.starbound.server.management.listeners.ServerCommandListener;
import icer.starbound.server.management.listeners.ServerListener;
import icer.starbound.server.management.listeners.ConsoleMessageListener;
import icer.starbound.server.management.util.IPUtil;
import icer.starbound.server.management.pojos.PlayerPOJO;
import icer.starbound.server.management.pojos.ChatPOJO;
import icer.starbound.server.management.pojos.ServerState;
import icer.starbound.server.management.pojos.WorldPOJO;
import icer.starbound.server.management.proxy.ProxyActions;
import icer.starbound.server.management.proxy.ProxyManager;
import icer.starbound.server.management.proxy.TCPForwardServer;
import icer.starbound.server.management.util.PropertiesUtil;
import icer.starbound.server.management.util.StartupUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Icer
 */
public class StarboundServer implements ServerListener, ServerCommandListener, ConsoleMessageListener {

    List<ServerListener> serverListeners = new ArrayList<>();
    List<ServerCommandListener> commandListeners = new ArrayList<>();
    private Process serverProcess;
    private String lastIPAddress = "{Unknown IP}";
    Pattern ipAddressPattern =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}:\\d{1,5}\\b");
    List<PlayerPOJO> currentPlayers = new ArrayList<>();
    public static final double MANAGEMENT_VERSION_NUMBER = 0.33;
    boolean headless = true;
    private TCPForwardServer tcpForwardServer;
    private ServerState currentState = new ServerState();

    public StarboundServer() {
        addServerListener(this);
        addCommandListener(this);
        
        loadBanList();
    }

    private void loadBanList() {
        ProxyManager.loadBanList();

//        lblBannedPlayers.setText("Banned Players (0)");
//        lblWhitelistPlayers.setText("Whitelist Players (0)  -  INACTIVE");
//        lblServerAdmins.setText("Server Admins (0)");
//        lblAdminPP.setText("No password set - Admin disabled");

        for (String ip : ProxyManager.getBannedIpAddresses()) {
            PlayerPOJO player = new PlayerPOJO("", ip);
            player.setBannedUntil(ProxyManager.getBannedPlayer(ip).getBannedUntil());
            currentState.getBannedPlayers().add(player);
        }
        for (String string : ProxyManager.getWhiteList()) {
            currentState.getWhiteList().add(string);
        }
        for (String string : ProxyManager.getServerAdmins()) {
            currentState.getAdmins().add(string);
        }
        if (ProxyManager.getAdminPassword() != null) {
            currentState.setServerPassword(ProxyManager.getAdminPassword());
        }
    }

    private void startProxy() {
        PropertiesUtil.load();
        String str = PropertiesUtil.getProperty(PropertiesUtil.TCP_REDIRECT_KEY);
        if (str == null || str.equals("1")) {
            PropertiesUtil.setProperty(PropertiesUtil.TCP_REDIRECT_KEY, "1");
            ProxyManager.addListener(this);
            new Thread() {
                @Override
                public void run() {
                    sendConsoleMessageToListeners("TCP Redirect starting");
                    try {
                        tcpForwardServer = new TCPForwardServer();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        sendConsoleMessageToListeners("Error starting TCP Redirect, is there another copy of ISSMConsole running?");
                    }
                }
            }.start();
        } else {
            PropertiesUtil.setProperty(PropertiesUtil.TCP_REDIRECT_KEY, "1");
        }
        PropertiesUtil.save();
    }

    public synchronized boolean addServerListener(ServerListener e) {
        return serverListeners.add(e);
    }

    public synchronized boolean removeServerListener(ServerListener e) {
        return serverListeners.remove(e);
    }

    public boolean addCommandListener(ServerCommandListener e) {
        return commandListeners.add(e);
    }

    public boolean removeCommandListener(ServerCommandListener o) {
        return commandListeners.remove(o);
    }

    public void startServer() throws Exception {
        startProxy();

        new Thread() {
            @Override
            public void run() {
                String ip = IPUtil.getMyIP();
//                String ip = getClientIP("").split(":")[0];
                currentState.setIp(ip);
                for (ServerListener serverListener : serverListeners) {
                    serverListener.serverIP(ip);
                }
            }
        }.start();

        List<ConsoleMessageListener> cmls = new ArrayList<>();
        cmls.add(this);
        StartupUtil.startServer(headless, null, serverListeners, cmls, currentState);

        killRunningServers();

        String serverLocation= PropertiesUtil.getProperty(PropertiesUtil.SERVER_LOCATION_KEY);
        ProcessBuilder builder = new ProcessBuilder(serverLocation);
        builder.redirectErrorStream(true);
        serverProcess = builder.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });

        final ExecutorService consumers = Executors.newFixedThreadPool(1);
        InputStream is = serverProcess.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(isr);
        String line;
        sendConsoleMessageToListeners("Server Started");

        while (true) {
            if ((line = in.readLine()) == null) {
                break;
            }
            final String str = line;
            consumers.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        currentState.getConsoleMessages().add(str);
                        for (ServerListener serverListener : serverListeners) {
                            serverListener.newLine(str);
                        }
                        processLine(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        consumers.shutdown();
        consumers.awaitTermination(10, TimeUnit.SECONDS);
        in.close();
        serverProcess.destroy();
        sendConsoleMessageToListeners("Server Shutdown");
        currentState.setStatus(ServerStatus.Shutdown);
        for (ServerListener serverListener : serverListeners) {
            serverListener.serverStatus(ServerStatus.Shutdown);
        }
    }

    public String convertNameToIP(String name) {
        for (PlayerPOJO playerPOJO : currentPlayers) {
            if (playerPOJO.getName().equals(name)) {
                return playerPOJO.getIp();
            }
        }
        return null;
    }

    private synchronized void processLine(String line) {
        if (line.startsWith("Info: accept from ")) {//127.0.0.1:56623 (488)")) {
            String sub = line.split(" from ")[1].split(" ")[0].trim();
            ProxyManager.mapNewIP(sub);
        } else if (line.startsWith("Info: Client")) {
            if (line.endsWith(" connected")) {
                String name = getClientName(line);
                String ip = ProxyManager.getRealIP(getClientIP(line)).getIp();

                PlayerPOJO player = new PlayerPOJO(name, ip);
                currentState.getActivePlayers().add(player);
                for (ServerListener serverListener : serverListeners) {
                    serverListener.playerJoined(player);
                }
            } else if (line.endsWith(" disconnected")) {
                String name = getClientName(line);
                for (int i = 0; i < currentState.getActivePlayers().size(); i++) {
                    PlayerPOJO playerPOJO = currentState.getActivePlayers().get(i);
                    if (playerPOJO.getName().equals(name)) {
                        for (ServerListener serverListener : serverListeners) {
                            serverListener.playerLeft(playerPOJO);
                        }
                        currentState.getActivePlayers().remove(playerPOJO);
                        break;
                    }
                }
            }
        } else if (line.startsWith("Info: Reaping client ")) {
            if (line.contains("'")) {
                String name = getClientName(line);
                for (int i = 0; i < currentState.getActivePlayers().size(); i++) {
                    PlayerPOJO playerPOJO = currentState.getActivePlayers().get(i);
                    if (playerPOJO.getName().equals(name)) {
                        for (ServerListener serverListener : serverListeners) {
                            serverListener.playerLeft(playerPOJO);
                        }
                        currentState.getActivePlayers().remove(playerPOJO);
                        break;
                    }
                }
            }
        } else if (line.contains("client connection made from")) {
            lastIPAddress = line.split("from")[1].trim();
        } else if (line.startsWith("Info: Server version")) {
            String version = line.split("version")[1];
            currentState.setVersion(version);
            for (ServerListener serverListener : serverListeners) {
                serverListener.serverVersion(version);
            }
        } else if (line.startsWith("Info: TcpServer listening")) {
            currentState.setStatus(ServerStatus.Running);
            for (ServerListener serverListener : serverListeners) {
                serverListener.serverStatus(ServerStatus.Running);
            }
        } else if (line.contains("client connect failed")) {
            String ip = ProxyManager.getRealIP(getIpAddress(line)).getIp();
            if (line.contains("wrong password")) {
                PlayerPOJO player = new PlayerPOJO("Unknown", ip);
                player.setFailedReason(JoinFailed.Bad_Password);
                currentState.getFailedToJoin().add(player);
                for (ServerListener serverListener : serverListeners) {
                    serverListener.playerJoinFailed(player, JoinFailed.Bad_Password);
                }
            }
        } else if (line.startsWith("Info:  <")) {
            for (PlayerPOJO playerPOJO : currentPlayers) {
                if (line.startsWith("Info:  <" + playerPOJO.getName() + "> ")) {
                    ChatPOJO chat = new ChatPOJO(line.split("> ")[1], playerPOJO.getName());
//                    playerPOJO.addChat(chat);
                    for (ServerListener serverListener : serverListeners) {
                        serverListener.chatMessage(chat);
                    }
                    processCommand(chat);
                }
            }
        } else if (line.startsWith("Info: Loading world db for world ")) {
            String[] worldStr = line.split(" for world ")[1].split(":", 2);
            String sector = worldStr[0].trim();
            String coordinates = worldStr[1].trim();
            WorldPOJO world = new WorldPOJO(sector, coordinates);
            currentState.getWorlds().add(world);
            for (ServerListener serverListener : serverListeners) {
                serverListener.worldLoaded(sector, coordinates);
            }
        } else if (line.startsWith("Info: Shutting down world ")) {
            String[] worldStr = line.split(" world ")[1].split(":", 2);
            String sector = worldStr[0].trim();
            String coordinates = worldStr[1].trim();
            WorldPOJO world = new WorldPOJO(sector, coordinates);
            currentState.getWorlds().remove(world);
            for (ServerListener serverListener : serverListeners) {
                serverListener.worldUnloaded(sector, coordinates);
            }
        }
    }

    private String getIpAddress(String line) {
        Matcher matcher =
                ipAddressPattern.matcher(line);

        while (matcher.find()) {
            return matcher.group();
        }
        return "{Unknown IP}";
//        return getClientIP(line);
    }

    private String getClientIP(String str) {
        return str.split("\\(")[1].split("\\)")[0];
//        Random r = new Random();
//        return (r.nextInt(60) + 50) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10)+":" + (r.nextInt(200)+50742);
    }

    private String getClientName(String str) {
        return str.split("'")[1];
    }

    public void stopServer() {
        if (serverProcess != null) {
            serverProcess.destroy();
            try {
                serverProcess.waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            currentState.setStatus(ServerStatus.Shutdown);
        }
    }

    private void killRunningServers() {
        try {
            String os = System.getProperty("os.name");
            ProcessBuilder builder;
            if (os.startsWith("Linux")) {
                builder = new ProcessBuilder(new String[]{"pkill starbound_server"});
            } else if (os.startsWith("Windows")) {
                builder = new ProcessBuilder(new String[]{"cmd.exe", "/C", "taskkill /F /IM starbound_server.exe"});
            } else {
                return;
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();

            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            String line;
            while ((line = in.readLine()) == null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        sendConsoleMessageToListeners("Starting server in 5 seconds");
        for (int i = 5; i >= 1; i--) {
            sendConsoleMessageToListeners(i + "");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void sendConsoleMessageToListeners(String str) {
        String msg = "<ISSMConsole> " + str;
        currentState.getConsoleMessages().add(msg);
        for (ServerListener serverListener : serverListeners) {
            serverListener.newLine(msg);
        }
        if (headless) {
            System.out.println(msg);
        }
    }

    private boolean commandAuthorized(ChatPOJO chat) {
        if (!ProxyManager.isPlayerAdmin(convertNameToIP(chat.getUser()))) {
            sendConsoleMessageToListeners("Unauthorized use of server admin command by " + chat.getUser());
            return false;
        }
        if (!isPassphraseValid(chat.getText())) {
            sendConsoleMessageToListeners("Invalid password used in command by " + chat.getUser());
            return false;
        }
        return true;
    }

    private void processCommand(ChatPOJO chat) {
        String txt = chat.getText();
        String ip;
        if (txt.startsWith("/ban ")) {
            if ((ip = getPlayerNameFromCommand(txt)) != null) {
                if (commandAuthorized(chat)) {
                    for (ServerCommandListener serverCommandListener : commandListeners) {
                        serverCommandListener.banPlayer(ip, ProxyActions.PERMANENT_BAN);
                    }
                }
            }
        } else if (txt.startsWith("/banhour ")) {
            if ((ip = getPlayerNameFromCommand(txt)) != null) {
                if (commandAuthorized(chat)) {
                    for (ServerCommandListener serverCommandListener : commandListeners) {
                        serverCommandListener.banPlayer(ip, ProxyActions.BAN_1_HOUR);
                    }
                }
            }
        } else if (txt.startsWith("/banday ")) {
            if ((ip = getPlayerNameFromCommand(txt)) != null) {
                if (commandAuthorized(chat)) {
                    for (ServerCommandListener serverCommandListener : commandListeners) {
                        serverCommandListener.banPlayer(ip, ProxyActions.BAN_1_DAY);
                    }
                }
            }
        } else if (txt.startsWith("/kick ")) {
            if ((ip = getPlayerNameFromCommand(txt)) != null) {
                if (commandAuthorized(chat)) {
                    for (ServerCommandListener serverCommandListener : commandListeners) {
                        serverCommandListener.banPlayer(ip, ProxyActions.KICK);
                    }
                }
            }
        }
    }

    private String getPlayerNameFromCommand(String txt) {
        String[] splt = txt.split(" ");
        if (splt.length == 3) {
            return convertNameToIP(splt[1]);
        }
        return null;
    }

    private boolean isPassphraseValid(String txt) {
        String[] splt = txt.split(" ");
        return ProxyManager.isPasswordCorrect(splt[splt.length - 1]);
    }

    @Override
    public void newLine(String line) {
//        remoteManagement.sendRequest("newLine", line);
    }

    @Override
    public void playerJoined(PlayerPOJO player) {
//        remoteManagement.sendRequest("playerJoined", player);
    }

    @Override
    public void playerJoinFailed(PlayerPOJO player, JoinFailed failed) {
//        remoteManagement.sendRequest("playerJoinFailed", player, failed);
    }

    @Override
    public void playerLeft(PlayerPOJO player) {
//        remoteManagement.sendRequest("playerLeft", player);
    }

    @Override
    public void serverVersion(String version) {
//        remoteManagement.sendRequest("serverVersion", version);
    }

    @Override
    public void serverStatus(ServerStatus status) {
//        remoteManagement.sendRequest("serverStatus", status);
    }

    @Override
    public void serverIP(String ip) {
//        remoteManagement.sendRequest("serverIP", ip);
    }

    @Override
    public void chatMessage(ChatPOJO chat) {
//        remoteManagement.sendRequest("chatMessage", chat);
    }

    @Override
    public void worldLoaded(String sector, String coordinates) {
//        remoteManagement.sendRequest("worldLoaded", sector, coordinates);
    }

    @Override
    public void worldUnloaded(String sector, String coordinates) {
//        remoteManagement.sendRequest("worldUnloaded", sector, coordinates);
    }

    @Override
    public void playerBanned(String ip, long until) {
//        remoteManagement.sendRequest("playerBanned", ip, until);
        ProxyManager.banPlayer(ip, System.currentTimeMillis() + 60 * 60 * 1000);
        try {
            ProxyManager.closeSocket(ip);
        } catch (IOException ex) {
            Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void playerAddedtoWhitelist(String ip) {
//        remoteManagement.sendRequest("playerAddedtoWhitelist", ip);
    }

    @Override
    public void playerAddedToAdmins(String ip) {
//        remoteManagement.sendRequest("playerAddedToAdmins", ip);
    }

    @Override
    public void banPlayer(String ip, ProxyActions action) {
//        remoteManagement.sendRequest("banPlayer", ip, action);
    }

    @Override
    public void serverLocation(String location) {
//        remoteManagement.sendRequest("serverLocation", location);
    }

    @Override
    public void newConsoleMessage(String message) {
        sendConsoleMessageToListeners(message);
    }

    @Override
    public void tcpRedirectActive(boolean active) {
    }

    @Override
    public void recieveStartingInformation(ServerState serverState) {
    }

    @Override
    public ServerState getStartingInformation() {
        return currentState;
    }

    @Override
    public void playerKicked(String ip) {
        try {
            ProxyManager.closeSocket(ip);
        } catch (IOException ex) {
            Logger.getLogger(StarboundServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.proxy;

import icer.starbound.server.management.enums.JoinFailed;
import icer.starbound.server.management.listeners.ServerListener;
import icer.starbound.server.management.pojos.PlayerPOJO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Icer
 */
public class ProxyManager {

    private static Map<ClientAddress, ClientAddress> addressMap = new HashMap<>();
    private static Map<String, ClientAddress> localIPMap = new HashMap<>();
    private static ClientAddress lastClient = null;
    private static Map<String, BannedPlayer> banned = new LinkedHashMap<>();
    private static final String BAN_FILE_NAME = "ISSMConsolePlayerList.xml";
    private static List<ServerListener> serverListeners = new ArrayList<>();
    private static Set<String> whiteList = new LinkedHashSet<>();
    private static Set<String> adminList = new LinkedHashSet<>();
    private static String adminPassword = null;

    public static boolean addListener(ServerListener e) {
        return serverListeners.add(e);
    }

    public static boolean removeListener(ServerListener o) {
        return serverListeners.remove(o);
    }

    public static void loadBanList() {
        banned.clear();
        whiteList.clear();
        adminList.clear();

        File playerFile = new File(BAN_FILE_NAME);
        if (playerFile.exists()) {
            try {
                DocumentBuilderFactory factory =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                builder = factory.newDocumentBuilder();
                Document document = builder.parse(playerFile);


                NodeList nlw = document.getElementsByTagName("AllowedPlayer");
                for (int i = 0; i < nlw.getLength(); i++) {
                    Element item = (Element) nlw.item(i);
                    String name = item.getTextContent();
                    whiteList.add(name);
                }

                NodeList nla = document.getElementsByTagName("ServerAdmin");
                for (int i = 0; i < nla.getLength(); i++) {
                    Element item = (Element) nla.item(i);
                    String name = item.getTextContent();
                    adminList.add(name);
                }
                NodeList pp = document.getElementsByTagName("AdminPassword");
                if (pp.getLength() > 0) {
                    adminPassword = pp.item(0).getTextContent();
                    if (adminPassword != null && adminPassword.isEmpty()) {
                        adminPassword = null;
                    }
                } else {
                    adminPassword = null;
                }

                NodeList nlb = document.getElementsByTagName("BannedPlayer");
                for (int i = 0; i < nlb.getLength(); i++) {
                    String name = null;
                    String length = null;
                    Element item = (Element) nlb.item(i);
                    NodeList names = item.getElementsByTagName("IPAddress");
                    if (names.getLength() > 0) {
                        name = names.item(0).getTextContent();
                    }
                    NodeList lengths = item.getElementsByTagName("BannedUntil");
                    if (lengths.getLength() > 0) {
                        length = lengths.item(0).getTextContent();
                    }
                    if (name != null && length != null) {
                        try {
                            long time = Long.parseLong(length);
                            if (time <= 0 || time > System.currentTimeMillis()) {
                                BannedPlayer bannedPlayer = new BannedPlayer(name, time);
                                banned.put(name, bannedPlayer);
                            } else {
                                System.out.println("Ban expired on " + name);
                            }
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                Logger.getLogger(ProxyManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        saveBanList();
    }

    public static void saveBanList() {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();


            Element root = (Element) document.createElement("root");
            document.appendChild(root);

            Element rootwhitelist = (Element) document.createElement("Whitelist");
            root.appendChild(rootwhitelist);
            Comment whiteComment = document.createComment("To activate the white list, add any IP to an AllowedPlayer field, ie <AllowedPlayer>1.2.3.4</AllowedPlayer>");
            rootwhitelist.appendChild(whiteComment);
            for (String string : whiteList) {
                Element playerNode = document.createElement("AllowedPlayer");
                rootwhitelist.appendChild(playerNode);
                playerNode.setTextContent(string);
            }

            Element rootAdmins = (Element) document.createElement("Adminlist");
            root.appendChild(rootAdmins);

            Comment ppComment = document.createComment("To set the admin password add a AdminPassword field, ie <AdminPassword>abc</AdminPassword>");
            rootAdmins.appendChild(ppComment);

            Element ppNode = document.createElement("AdminPassword");
            rootAdmins.appendChild(ppNode);
            ppNode.setTextContent(adminPassword);

            Comment adminComment = document.createComment("To make a player an Administrator simply add their IP to a ServerAdmin field, ie <ServerAdmin>1.2.3.4</ServerAdmin>");
            rootAdmins.appendChild(adminComment);

            for (String string : adminList) {
                Element playerNode = document.createElement("ServerAdmin");
                rootAdmins.appendChild(playerNode);
                playerNode.setTextContent(string);
            }

            Element rootBanned = (Element) document.createElement("Banned");
            root.appendChild(rootBanned);
            Comment banComment = document.createComment("To ban a player, create a <BannedPlayer><IPAddress>1.2.3.4</IPAddress><BannedUntil>date-in-milliseconds</BannedUntil></BannedPlayer>");
            rootBanned.appendChild(banComment);
            for (BannedPlayer bannedPlayer : banned.values()) {
                Element playerNode = document.createElement("BannedPlayer");
                rootBanned.appendChild(playerNode);

                Element playerName = document.createElement("IPAddress");
                playerName.setTextContent(bannedPlayer.getIp());
                playerNode.appendChild(playerName);

                Element bannedUntil = document.createElement("BannedUntil");
                bannedUntil.setTextContent(bannedPlayer.getBannedUntil() + "");
                playerNode.appendChild(bannedUntil);
            }

            File file = new File(BAN_FILE_NAME);
            file.createNewFile();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new FileOutputStream(file));
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
        } catch (TransformerException | ParserConfigurationException | IOException ex) {
            Logger.getLogger(ProxyManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Set<String> getBannedIpAddresses() {
        return banned.keySet();
    }

    public static BannedPlayer getBannedPlayer(String ip) {
        return banned.get(ip);
    }

    public static Set<String> getWhiteList() {
        return whiteList;
    }

    public static Set<String> getServerAdmins() {
        return adminList;
    }

    public static boolean isPlayerAdmin(String player) {
        for (String string : adminList) {
            if (string.equals(player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPasswordCorrect(String pass) {
        System.out.println("comparing " + adminPassword + pass);
        return adminPassword != null && adminPassword.equals(pass);
    }

    public static String getAdminPassword() {
        return adminPassword;
    }

    public static boolean addWhitelist(String ip) {
        for (ServerListener serverListener : serverListeners) {
            serverListener.playerAddedtoWhitelist(ip);
        }
        return whiteList.add(ip);
    }

    public static boolean removeWhitelist(String ip) {
        return whiteList.remove(ip);
    }

    public static void banPlayer(String ip, long until) {
        banned.put(ip, new BannedPlayer(ip, until));
        saveBanList();
        for (ServerListener serverListener : serverListeners) {
            serverListener.playerBanned(ip, until);
        }
    }

    protected static boolean allowConnection(String ip) {
        if (!whiteList.isEmpty()) {
            if (!whiteList.contains(ip)) {
                sendFailedLoginEvent(ip, JoinFailed.Not_on_whitelist);
                return false;
            }
        } else {
            BannedPlayer bp = banned.get(ip);
            if (bp != null) {
                if (bp.getBannedUntil() == -1) {
                    System.out.println(ip + " permanently banned");
                    sendFailedLoginEvent(ip, JoinFailed.Banned);
                    return false;
                } else if (bp.getBannedUntil() > System.currentTimeMillis()) {
                    System.out.println(ip + " banned for another " + ((bp.getBannedUntil() - System.currentTimeMillis()) / 1000));
                    sendFailedLoginEvent(ip, JoinFailed.Banned);
                    return false;
                }
            }
        }
        return true;
    }

    private static void sendFailedLoginEvent(String ip, JoinFailed reason) {
        for (ServerListener serverListener : serverListeners) {
            serverListener.playerJoinFailed(new PlayerPOJO("Unknown", ip), reason);
        }
    }

    public static void addClient(ClientAddress c1, ClientAddress c2) {
//        System.out.println("\t" + c1 + " mapped to " + c2);
        System.out.println("-- AddClient --");
        addressMap.put(c1, c2);
        lastClient = c1;
//        System.out.println("t1 " + System.currentTimeMillis());
    }

    public static synchronized void mapNewIP(String ip) {
        System.out.println("-- MapNewIP --");
        System.out.println("\tip = " + ip);
        System.out.println("\tlastClient = " + lastClient);
        localIPMap.put(ip, lastClient);
    }

    public static synchronized ClientAddress getRealIP(String key) {

//        Random r = new Random();
//        String ip = (r.nextInt(60) + 50) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10);
//        int port = (r.nextInt(200) + 50742);
//        return new ClientAddress(ip, port);
        ClientAddress realIp = localIPMap.get(key);
        if (realIp == null) {
            if (key.contains(":")) {
                String[] split = key.split(":");
                try {
                    return new ClientAddress(split[0], Integer.parseInt(split[1]));
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            return new ClientAddress(key, 0);
        }
        return realIp;
    }

    public static ClientAddress getForwardedClient(String ip) {
        for (ClientAddress clientAddress : addressMap.keySet()) {
            if (clientAddress.getIp().equals(ip)) {
                return clientAddress;
            }
        }
        return null;
    }

    public static void closeSocket(String ip) throws IOException {
        ClientAddress remove = null;
        for (ClientAddress clientAddress : addressMap.keySet()) {
            if (clientAddress.getIp().equals(ip)) {
                System.out.println("Closing connection " + clientAddress.getIp() + ":" + clientAddress.getPort());
                clientAddress.getSocket().close();
                remove = clientAddress;
//                break;
            }
        }
        if (remove != null) {
            addressMap.remove(remove);
        }
    }
}

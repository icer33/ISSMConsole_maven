/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.util;

import icer.starbound.server.management.ConsoleMessageListener;
import icer.starbound.server.management.ServerListener;
import icer.starbound.server.management.ServerStatus;
import icer.starbound.server.management.client.StarboundServerGUI;
import icer.starbound.server.management.pojos.ServerState;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 *
 * @author Icer
 */
public class StartupUtil {

    public static ServerState startServer(final boolean headless, final JComponent comp, final List<ServerListener> sls, List<ConsoleMessageListener> cmls, ServerState serverState) {
//        new Thread() {
//            @Override
//            public void run() {
        try {
            PropertiesUtil.load();
            String serverLocation = PropertiesUtil.getProperty(PropertiesUtil.SERVER_LOCATION_KEY);
            System.out.println("serverLocation = " + serverLocation);
            if (serverLocation == null || !new File(serverLocation).exists()) {
                File autoFind = new File("starbound_server.exe");
                if (autoFind.exists()) {
                    serverLocation = autoFind.getAbsolutePath();
                } else {
                    String comment = "Please enter the file path to your installation of the Starbound Server.\nThis path is typically located for most people in their steam games directory.\n";
                    String example = "{Replace-with-directory-path}\\SteamApps\\common\\Starbound\\win32\\starbound_server.exe\n";
                    if (headless) {
                        System.out.println(comment);
                        System.out.println("ie. " + example);
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        serverLocation = br.readLine();
                    } else {
                        serverLocation = (String) JOptionPane.showInputDialog(comp,
                                comment + "The most common path ending is already prefilled in for you.\n ",
                                "Starbound server location",
                                JOptionPane.QUESTION_MESSAGE,
                                null, null,
                                example);
                    }
                    if (serverLocation == null || serverLocation.isEmpty() || !new File(serverLocation).exists()) {
                        String comment2 = "File location was not valid\n'" + serverLocation + "'\n\nExiting application";
                        if (headless) {
                            System.out.println(comment2);
                        } else {
                            JOptionPane.showMessageDialog(comp, comment2, "Title", JOptionPane.ERROR_MESSAGE);
                        }
                        System.exit(0);
                    }
                }
                PropertiesUtil.setProperty(PropertiesUtil.SERVER_LOCATION_KEY, serverLocation);
                String s = PropertiesUtil.getProperty(PropertiesUtil.SERVER_LOCATION_KEY);
                PropertiesUtil.save();
            }
            serverState.setLocation(serverLocation);


            File file = new File(serverLocation);
            System.out.println("Server set to " + file.getAbsolutePath());
            boolean configFound = false;
            for (int i = 0; i < 4; i++) {
                if (file.getParentFile() != null) {
                    file = file.getParentFile();
                    for (ConsoleMessageListener consoleMessageListener : cmls) {
                        consoleMessageListener.newConsoleMessage("Checking for config file in " + file.getAbsolutePath());
                    }
                    String configSr = file.getAbsolutePath() + File.separatorChar + "starbound.config";
                    File config = new File(configSr);
                    if (config.exists()) {
                        serverState.setConfig(configSr);
                        try {
                            for (ConsoleMessageListener consoleMessageListener : cmls) {
                                consoleMessageListener.newConsoleMessage("   Config file found");
                            }
                            configFound = true;
                            boolean update = false;
                            System.out.println(config.getAbsolutePath());
                            BufferedReader br = new BufferedReader(new FileReader(config));
                            String line;
                            StringBuilder sb = new StringBuilder();
                            while ((line = br.readLine()) != null) {
                                if (line.contains("\"gamePort\"")) {
                                    String pString = line.split(":")[1].split(",")[0].trim();
                                    try {
                                        int port = Integer.parseInt(pString);
                                        if (port != 21024) {
                                            String comment = "Server port currently set to " + port + ".\nWould you like to automatically adjust the port to 21024?\n\n"
                                                    + "This is required for TCP redirect and player banning.";
                                            if (headless) {
                                                System.out.print(comment + "  [y/n] ");
                                                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                                                String answer = input.readLine();
                                                System.out.println("Done already?");
                                                if (answer.equalsIgnoreCase("y")) {
                                                    update = true;
                                                    System.out.println("Port set to 21024");
                                                }
                                            } else {
                                                int adjustPort = JOptionPane.showConfirmDialog(
                                                        comp,
                                                        comment,
                                                        "Automatically adjust port",
                                                        JOptionPane.YES_NO_OPTION,
                                                        JOptionPane.WARNING_MESSAGE);
                                                if (adjustPort == JOptionPane.YES_OPTION) {
                                                    update = true;
                                                }
                                            }
                                            if (update) {
                                                line = "  \"gamePort\" : 21024,";
                                            }
                                        }
                                        if (port == 21024 || update == true) {
                                            for (ConsoleMessageListener consoleMessageListener : cmls) {
                                                consoleMessageListener.newConsoleMessage("   Port correctly set to 21024");
                                            }
                                        }
                                    } catch (NumberFormatException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                sb.append(line).append("\n");
                            }
                            br.close();

                            if (update) {
                                PrintWriter pw = new PrintWriter(new FileWriter(config));
                                pw.println(sb.toString());
                                pw.close();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        break;
                    }
                }
            }
            if (!configFound) {
                for (ConsoleMessageListener consoleMessageListener : cmls) {
                    consoleMessageListener.newConsoleMessage("No config file found, are you sure you have the right server executable?");
                }
            }
            for (ServerListener listener : sls) {
                listener.serverLocation(serverLocation);
            }

            serverState.setStatus(ServerStatus.Loading);
            for (ServerListener serverListener : sls) {
                serverListener.serverStatus(ServerStatus.Loading);
            }
            for (ConsoleMessageListener consoleMessageListener : cmls) {
                consoleMessageListener.newConsoleMessage("Server executable set to '" + serverLocation + "'");
            }
//                    starboundServer.startServer(serverLocation);


        } catch (Exception ex) {
            Logger.getLogger(StarboundServerGUI.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
//            }
//        }.start();
        return serverState;
    }
}

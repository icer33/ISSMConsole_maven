/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.client;

import icer.starbound.server.management.JoinFailed;
import icer.starbound.server.management.ServerCommandListener;
import icer.starbound.server.management.ServerListener;
import icer.starbound.server.management.ServerStatus;
import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.comms.RemoteManagement;
import icer.starbound.server.management.pojos.PlayerPOJO;
import icer.starbound.server.management.pojos.ChatPOJO;
import icer.starbound.server.management.pojos.ServerState;
import icer.starbound.server.management.pojos.WorldPOJO;
import icer.starbound.server.management.proxy.BannedPlayer;
import icer.starbound.server.management.proxy.ProxyActions;
import static icer.starbound.server.management.proxy.ProxyActions.BAN_1_DAY;
import static icer.starbound.server.management.proxy.ProxyActions.BAN_1_HOUR;
import static icer.starbound.server.management.proxy.ProxyActions.KICK;
import static icer.starbound.server.management.proxy.ProxyActions.NOTHING;
import static icer.starbound.server.management.proxy.ProxyActions.PERMANENT_BAN;
import icer.starbound.server.management.proxy.ProxyManager;
import icer.starbound.server.management.proxy.TCPForwardServer;
import icer.starbound.server.management.util.PropertiesUtil;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Icer
 */
public class StarboundServerGUI extends javax.swing.JPanel implements ServerListener, ServerCommandListener {

//    final StarboundServer starboundServer = new StarboundServer();
    private long serverStartTime = 0;
    private String lastLine = "";
    private int lineCount = 0;
    private int logCount = 0;
    private static final Logger logger = Logger.getLogger(StarboundServerGUI.class.getName());
    private JComboBox playerActions = new JComboBox();
    private TCPForwardServer tcpForwardServer;
    Timer uptimer = new Timer("Server time thread");
    RemoteManagement remoteManagement;
    private ServerState currentState = new ServerState();

    public StarboundServerGUI() {
        initComponents();
//        new Thread() {
//            public void run() {
        try {
            remoteManagement = new RemoteManagement(false);
        } catch (IOException ex) {
            Logger.getLogger(StarboundServerGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
//
//            }
//        }.start();
    }

    public StarboundServerGUI(final JFrame frame) {
        this();
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int c = JOptionPane.showConfirmDialog(frame,
                        "Are you sure to close down the server?", "Really Close?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (c == JOptionPane.YES_OPTION) {
//                    StarboundServerGUI.this.starboundServer.stopServer();
                    remoteManagement.sendRequest("stopServer");
                    System.exit(0);
                }
            }
        });
        setUpBanColumn(tblPlayers, tblPlayers.getColumn("Action"));
        remoteManagement.setGui(this);
//        try {
        remoteManagement.start("localhost");
//        } catch (IOException ex) {
//            Logger.getLogger(StarboundServerGUI.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void init() {
        PropertiesUtil.load();
//        startServer();
//        startProxy();
        updatePlayerCount();
        loadBanList();

//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                remoteManagement.sendRequest("sendConsoleMessageToListeners", "This is a test message every .5 seconds");
//            }
//        }, 500, 500);
    }

    private void loadBanList() {
        ProxyManager.loadBanList();

        lblBannedPlayers.setText("Banned Players (0)");
        lblWhitelistPlayers.setText("Whitelist Players (0)  -  INACTIVE");
        lblServerAdmins.setText("Server Admins (0)");
        lblAdminPP.setText("No password set - Admin disabled");

        for (String ip : ProxyManager.getBannedIpAddresses()) {
            playerBanned(ip, ProxyManager.getBannedPlayer(ip).getBannedUntil());
        }
        for (String string : ProxyManager.getWhiteList()) {
            playerAddedtoWhitelist(string);
        }
        for (String string : ProxyManager.getServerAdmins()) {
            playerAddedToAdmins(string);
        }
        if (ProxyManager.getAdminPassword() != null) {
            lblAdminPP.setText("Password = '" + ProxyManager.getAdminPassword() + "'");
        }
    }

    public void setUpBanColumn(JTable table,
            TableColumn sportColumn) {
        playerActions.addItem(ProxyActions.NOTHING);
        playerActions.addItem(ProxyActions.KICK);
        playerActions.addItem(ProxyActions.BAN_1_HOUR);
        playerActions.addItem(ProxyActions.BAN_1_DAY);
//        playerActions.addItem(ProxyActions.BAN_FOR_X);
        playerActions.addItem(ProxyActions.PERMANENT_BAN);
        playerActions.addItemListener(new ItemListener() {
            boolean ignoreNext = true;

            @Override
            public void itemStateChanged(ItemEvent e) {
                System.out.println(e.getItem());
                if (!ignoreNext) {
                    System.out.println(playerActions.getSelectedItem());
                    int row = tblPlayers.getSelectedRow();
                    DefaultTableModel dtm = (DefaultTableModel) tblPlayers.getModel();
                    String ip = (String) dtm.getValueAt(row, 1);
                    if (e.getItem() instanceof ProxyActions) {
                        try {
                            ProxyActions proxyActions = (ProxyActions) e.getItem();
                            performPlayerAction(ip, proxyActions, true);
                        } catch (IOException ex) {
                            Logger.getLogger(StarboundServerGUI.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    StarboundServerGUI.this.playerActions.removeItemListener(this);
                    playerActions.setSelectedIndex(0);
                    StarboundServerGUI.this.playerActions.addItemListener(this);
                }
                ignoreNext = !ignoreNext;
            }
        });
        sportColumn.setCellEditor(new DefaultCellEditor(playerActions));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        sportColumn.setCellRenderer(renderer);
    }

    private void reloadPlayersFile() {
        DefaultTableModel model = (DefaultTableModel) tblFailedLogins1.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            model.removeRow(i);
        }

        DefaultTableModel modelWhite = (DefaultTableModel) tblWhitelist.getModel();
        for (int i = modelWhite.getRowCount() - 1; i >= 0; i--) {
            modelWhite.removeRow(i);
        }

        DefaultTableModel modelAdmins = (DefaultTableModel) tblServerAdmins.getModel();
        for (int i = modelAdmins.getRowCount() - 1; i >= 0; i--) {
            modelAdmins.removeRow(i);
        }

        loadBanList();
    }

    private void performPlayerAction(String ip, ProxyActions proxyActions, boolean confirm) throws IOException {
        switch (proxyActions) {
            case NOTHING:
                System.out.println("Doing nothing");
                break;
            case KICK:
                if (!confirm || JOptionPane.showConfirmDialog(StarboundServerGUI.this,
                        "Are you sure to KICK " + ip + " from the server?", "Really Kick?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    
//                    ProxyManager.closeSocket(ip);
                    remoteManagement.sendRequest("playerKicked", ip);
//                    starboundServer.sendConsoleMessageToListeners("Kicked " + ip + " from the server");
                    remoteManagement.sendRequest("sendConsoleMessageToListeners", "Kicked " + ip + " from the server");
                }
                break;
            case BAN_1_HOUR:
                if (!confirm || JOptionPane.showConfirmDialog(StarboundServerGUI.this,
                        "Are you sure to BAN " + ip + " from the server for 1 HOUR?", "Really Kick?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
//                    ProxyManager.banPlayer(ip, System.currentTimeMillis() + 60 * 60 * 1000);
//                    ProxyManager.closeSocket(ip);
                    remoteManagement.sendRequest("playerBanned", ip, System.currentTimeMillis() + 60 * 60 * 1000);
//                    starboundServer.sendConsoleMessageToListeners("Banned " + ip + " from the server for one hour");
                    remoteManagement.sendRequest("sendConsoleMessageToListeners", "Banned " + ip + " from the server for one hour");
                }
                break;
            case BAN_1_DAY:
                if (!confirm || JOptionPane.showConfirmDialog(StarboundServerGUI.this,
                        "Are you sure to BAN " + ip + " from the server for 1 DAY?", "Really Kick?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    ProxyManager.banPlayer(ip, System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                    ProxyManager.closeSocket(ip);
//                    starboundServer.sendConsoleMessageToListeners("Banned " + ip + " from the server for one hour");
                    remoteManagement.sendRequest("sendConsoleMessageToListeners", "Banned " + ip + " from the server for one hour");
                }
                break;
            case PERMANENT_BAN:
                if (!confirm || JOptionPane.showConfirmDialog(StarboundServerGUI.this,
                        "Are you sure to BAN " + ip + " from the server PERMANENTLY?", "Really Kick?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    ProxyManager.banPlayer(ip, -1);
                    ProxyManager.closeSocket(ip);
//                    starboundServer.sendConsoleMessageToListeners("Banned " + ip + " from the server for one hour");
                    remoteManagement.sendRequest("sendConsoleMessageToListeners", "Banned " + ip + " from the server for one hour");
                }
                break;
            default:
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        PlayerPanel = new javax.swing.JPanel();
        lblPlayer = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        ServerInfoPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtIP = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtServerVersion = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtServerStatus = new javax.swing.JTextField();
        txtServerLocation = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        chkTCPRedirect = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        failedLoginsPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        lblPlayer1 = new javax.swing.JLabel();
        bannedPlayersPanel = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        lblBannedPlayers = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        whitelistPlayersPanel = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        lblWhitelistPlayers = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lblServerLogTitle = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        lblServerAdmins = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        jButton3 = new javax.swing.JButton();
        lblAdminPP = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());

        jSplitPane1.setDividerLocation(500);

        PlayerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lblPlayer.setText("Players");

        tblPlayers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Connected", "IP Address", "Name", "Action"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPlayers.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane3.setViewportView(tblPlayers);

        javax.swing.GroupLayout PlayerPanelLayout = new javax.swing.GroupLayout(PlayerPanel);
        PlayerPanel.setLayout(PlayerPanelLayout);
        PlayerPanelLayout.setHorizontalGroup(
            PlayerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PlayerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblPlayer, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE))
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        PlayerPanelLayout.setVerticalGroup(
            PlayerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PlayerPanelLayout.createSequentialGroup()
                .addComponent(lblPlayer, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE))
        );

        ServerInfoPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        ServerInfoPanel.setPreferredSize(new java.awt.Dimension(210, 107));

        jLabel3.setText("Server Information");

        jLabel4.setText("IP");

        txtIP.setEditable(false);

        jLabel5.setText("Server Version");

        txtServerVersion.setEditable(false);

        jLabel6.setText("Server Status");

        txtServerStatus.setEditable(false);

        txtServerLocation.setEditable(false);
        txtServerLocation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtServerLocationActionPerformed(evt);
            }
        });

        jLabel8.setText("Server Path");

        chkTCPRedirect.setSelected(true);
        chkTCPRedirect.setText("Enable TCP redirect (Required to ban and kick players)");
        chkTCPRedirect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkTCPRedirectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ServerInfoPanelLayout = new javax.swing.GroupLayout(ServerInfoPanel);
        ServerInfoPanel.setLayout(ServerInfoPanelLayout);
        ServerInfoPanelLayout.setHorizontalGroup(
            ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ServerInfoPanelLayout.createSequentialGroup()
                .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(ServerInfoPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(ServerInfoPanelLayout.createSequentialGroup()
                                .addComponent(chkTCPRedirect)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(txtServerLocation)
                            .addComponent(txtIP)
                            .addComponent(txtServerVersion)
                            .addComponent(txtServerStatus))))
                .addContainerGap())
        );
        ServerInfoPanelLayout.setVerticalGroup(
            ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ServerInfoPanelLayout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(txtServerLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(txtServerVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ServerInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(txtServerStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkTCPRedirect)
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel5.setLayout(new java.awt.GridLayout(3, 1));

        failedLoginsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        failedLoginsPanel.setPreferredSize(new java.awt.Dimension(539, 141));

        tblFailedLogins.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Timestamp", "IP Address", "Reason"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFailedLogins.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane4.setViewportView(tblFailedLogins);

        lblPlayer1.setText("Failed logins");

        javax.swing.GroupLayout failedLoginsPanelLayout = new javax.swing.GroupLayout(failedLoginsPanel);
        failedLoginsPanel.setLayout(failedLoginsPanelLayout);
        failedLoginsPanelLayout.setHorizontalGroup(
            failedLoginsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblPlayer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)
        );
        failedLoginsPanelLayout.setVerticalGroup(
            failedLoginsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(failedLoginsPanelLayout.createSequentialGroup()
                .addComponent(lblPlayer1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE))
        );

        jPanel5.add(failedLoginsPanel);

        bannedPlayersPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bannedPlayersPanel.setPreferredSize(new java.awt.Dimension(539, 141));

        tblFailedLogins1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "IP Address", "Banned until"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFailedLogins1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane6.setViewportView(tblFailedLogins1);

        lblBannedPlayers.setText("Banned Players");

        jButton1.setText("Reload from file");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout bannedPlayersPanelLayout = new javax.swing.GroupLayout(bannedPlayersPanel);
        bannedPlayersPanel.setLayout(bannedPlayersPanelLayout);
        bannedPlayersPanelLayout.setHorizontalGroup(
            bannedPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bannedPlayersPanelLayout.createSequentialGroup()
                .addComponent(lblBannedPlayers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1))
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)
        );
        bannedPlayersPanelLayout.setVerticalGroup(
            bannedPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bannedPlayersPanelLayout.createSequentialGroup()
                .addGroup(bannedPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblBannedPlayers)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
        );

        jPanel5.add(bannedPlayersPanel);

        whitelistPlayersPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        whitelistPlayersPanel.setPreferredSize(new java.awt.Dimension(539, 141));

        tblWhitelist.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "IP Address"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblWhitelist.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane7.setViewportView(tblWhitelist);

        lblWhitelistPlayers.setText("Whitelist Players (0)  -  INACTIVE");

        jButton2.setText("Reload from file");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout whitelistPlayersPanelLayout = new javax.swing.GroupLayout(whitelistPlayersPanel);
        whitelistPlayersPanel.setLayout(whitelistPlayersPanelLayout);
        whitelistPlayersPanelLayout.setHorizontalGroup(
            whitelistPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(whitelistPlayersPanelLayout.createSequentialGroup()
                .addComponent(lblWhitelistPlayers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2))
            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)
        );
        whitelistPlayersPanelLayout.setVerticalGroup(
            whitelistPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(whitelistPlayersPanelLayout.createSequentialGroup()
                .addGroup(whitelistPlayersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblWhitelistPlayers)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
        );

        jPanel5.add(whitelistPlayersPanel);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ServerInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
            .addComponent(PlayerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(ServerInfoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PlayerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jSplitPane2.setDividerLocation(415);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        txtServerLog.setEditable(false);
        jScrollPane1.setViewportView(txtServerLog);

        lblServerLogTitle.setText("Server Log");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblServerLogTitle, javax.swing.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE))
            .addComponent(jScrollPane1)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(lblServerLogTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE))
        );

        jSplitPane2.setLeftComponent(jPanel3);

        jLabel1.setText("Chat");

        txtChat.setEditable(false);
        jScrollPane2.setViewportView(txtChat);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE))
            .addComponent(jScrollPane2)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel4);

        jPanel2.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jPanel7.setLayout(new java.awt.GridLayout(2, 0));

        jPanel8.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel8.setPreferredSize(new java.awt.Dimension(539, 141));

        lblServerAdmins.setText("Server Admins");

        tblServerAdmins.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "IP Address"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblServerAdmins.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane8.setViewportView(tblServerAdmins);

        jButton3.setText("Reload from file");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        lblAdminPP.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(lblServerAdmins)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblAdminPP, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton3))
            .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblServerAdmins)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAdminPP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
        );

        jPanel7.add(jPanel8);

        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel7.setText("Currently Loaded Worlds");

        tblLoadedWorlds.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Sector", "Coordinates"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblLoadedWorlds.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane5.setViewportView(tblLoadedWorlds);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE))
        );

        jPanel7.add(jPanel6);

        jPanel2.add(jPanel7, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setRightComponent(jPanel2);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void txtServerLocationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtServerLocationActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtServerLocationActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        reloadPlayersFile();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        reloadPlayersFile();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void chkTCPRedirectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTCPRedirectActionPerformed
        PropertiesUtil.setProperty(PropertiesUtil.TCP_REDIRECT_KEY, chkTCPRedirect.isSelected() ? "1" : "0");
        PropertiesUtil.save();
    }//GEN-LAST:event_chkTCPRedirectActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        reloadPlayersFile();
    }//GEN-LAST:event_jButton3ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PlayerPanel;
    private javax.swing.JPanel ServerInfoPanel;
    private javax.swing.JPanel bannedPlayersPanel;
    private javax.swing.JCheckBox chkTCPRedirect;
    private javax.swing.JPanel failedLoginsPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JLabel lblAdminPP;
    private javax.swing.JLabel lblBannedPlayers;
    private javax.swing.JLabel lblPlayer;
    private javax.swing.JLabel lblPlayer1;
    private javax.swing.JLabel lblServerAdmins;
    private javax.swing.JLabel lblServerLogTitle;
    private javax.swing.JLabel lblWhitelistPlayers;
    private final javax.swing.JTable tblFailedLogins = new javax.swing.JTable();
    private final javax.swing.JTable tblFailedLogins1 = new javax.swing.JTable();
    private final javax.swing.JTable tblLoadedWorlds = new javax.swing.JTable();
    private final javax.swing.JTable tblPlayers = new javax.swing.JTable();
    private final javax.swing.JTable tblServerAdmins = new javax.swing.JTable();
    private final javax.swing.JTable tblWhitelist = new javax.swing.JTable();
    private final javax.swing.JTextPane txtChat = new javax.swing.JTextPane();
    private javax.swing.JTextField txtIP;
    private javax.swing.JTextField txtServerLocation;
    private final javax.swing.JTextPane txtServerLog = new javax.swing.JTextPane();
    private javax.swing.JTextField txtServerStatus;
    private javax.swing.JTextField txtServerVersion;
    private javax.swing.JPanel whitelistPlayersPanel;
    // End of variables declaration//GEN-END:variables

    private void startProxy() {
        String str = PropertiesUtil.getProperty(PropertiesUtil.TCP_REDIRECT_KEY);
        if (str == null || str.equals("1")) {
            PropertiesUtil.setProperty(PropertiesUtil.TCP_REDIRECT_KEY, "1");
            chkTCPRedirect.setSelected(true);
            ProxyManager.addListener(this);
            new Thread() {
                @Override
                public void run() {
//                    starboundServer.sendConsoleMessageToListeners("TCP Redirect starting");
                    try {
                        tcpForwardServer = new TCPForwardServer();
                    } catch (IOException ex) {
                        Logger.getLogger(StarboundServerGUI.class.getName()).log(Level.SEVERE, null, ex);
//                        starboundServer.sendConsoleMessageToListeners("Error starting TCP Redirect, is there another copy of ISSMConsole running?");
                    }
                }
            }.start();
        } else {
            chkTCPRedirect.setSelected(false);
            PropertiesUtil.setProperty(PropertiesUtil.TCP_REDIRECT_KEY, "1");
        }
        PropertiesUtil.save();
    }

    private void startServer() {
//        starboundServer.addServerListener(this);
//        starboundServer.addCommandListener(this);
        new Thread() {
            @Override
            public void run() {
                try {
                    String serverLocation = PropertiesUtil.getProperty(PropertiesUtil.SERVER_LOCATION_KEY);
                    if (serverLocation == null || !new File(serverLocation).exists()) {
                        File autoFind = new File("starbound_server.exe");
                        if (autoFind.exists()) {
//                            serverLocation = "./starbound_server.exe";
                            serverLocation = autoFind.getAbsolutePath();
                        } else {
                            serverLocation = (String) JOptionPane.showInputDialog(StarboundServerGUI.this,
                                    "Please enter the file path to your installation of the Starbound Server.\nThis path is typically located for most people in their steam games directory.\n The most common path ending is already prefilled in for you.\n ",
                                    "Starbound server location",
                                    JOptionPane.QUESTION_MESSAGE,
                                    null, null,
                                    "{Replace-with-directory-path}\\SteamApps\\common\\Starbound\\win32\\starbound_server.exe");
                            if (serverLocation == null || serverLocation.isEmpty() || !new File(serverLocation).exists()) {
                                JOptionPane.showMessageDialog(StarboundServerGUI.this, "File location was not valid\n'" + serverLocation + "'\n\nExiting application", "Title", JOptionPane.ERROR_MESSAGE);
                                System.exit(0);
                            }
                        }
                        PropertiesUtil.setProperty(PropertiesUtil.SERVER_LOCATION_KEY, serverLocation);
                        PropertiesUtil.save();
                    }
                    txtServerLocation.setText(serverLocation);

                    File file = new File(serverLocation);
                    System.out.println(file.getAbsolutePath());
                    boolean configFound = false;
                    for (int i = 0; i < 4; i++) {
                        if (file.getParentFile() != null) {
                            file = file.getParentFile();
//                            starboundServer.sendConsoleMessageToListeners("Checking for config file in " + file.getAbsolutePath());
                            String configSr = file.getAbsolutePath() + File.separatorChar + "starbound.config";
                            File config = new File(configSr);
                            if (config.exists()) {
                                try {
//                                    starboundServer.sendConsoleMessageToListeners("   Config file found");
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
                                                    int adjustPort = JOptionPane.showConfirmDialog(
                                                            StarboundServerGUI.this,
                                                            "Server port currently set to " + port + ".\nWould you like to automatically adjust the port to 21024?\n\n"
                                                            + "This is required for TCP redirect and player banning.",
                                                            "Automatically adjust port",
                                                            JOptionPane.YES_NO_OPTION,
                                                            JOptionPane.WARNING_MESSAGE);
                                                    if (adjustPort == JOptionPane.YES_OPTION) {
                                                        line = "  \"gamePort\" : 21024,";
                                                        update = true;
                                                    }
                                                }
                                                if (port == 21024 || update == true) {
//                                                    starboundServer.sendConsoleMessageToListeners("   Port correctly set to 21024");
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
//                        starboundServer.sendConsoleMessageToListeners("No config file found, are you sure you have the right server executable?");
                    }

                    txtServerStatus.setText("Loading...");
//                    starboundServer.sendConsoleMessageToListeners("Server executable set to '" + serverLocation + "'");
//                    starboundServer.startServer(serverLocation);


                } catch (Exception ex) {
                    Logger.getLogger(StarboundServerGUI.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    @Override
    public void newLine(String line) {
        synchronized (txtServerLog) {
            StyledDocument doc = txtServerLog.getStyledDocument();
            try {
                if (lastLine.equals(line)) {
                    lineCount++;
                    int len = doc.getLength();
                    int ind = doc.getText(0, len).lastIndexOf(lastLine);
//                    int dif = len - txtServerLog.getText().length();
                    int tot = ind + lastLine.length();
//                    System.err.println(tot + " : " + len + " : " + (len - tot)+" : "+dif);
                    doc.remove(tot, len - tot);
                    doc.insertString(doc.getLength(), " x" + lineCount + " \n", getStyle(lastLine));
                } else {
                    if (lineCount > 0) {
//                        doc.insertString(doc.getLength() - 1, " x" + lineCount + " ", getStyle(lastLine));
                        lineCount = 1;
                    }
                    doc.insertString(doc.getLength(), line + " \n", getStyle(line));
                    lblServerLogTitle.setText("Server Log (" + ++logCount + " entries)");
                    if (logCount > 1000) {
                        int indexOf = txtServerLog.getText().indexOf("\n");
                        doc.remove(0, indexOf);
                    }
                }

                txtServerLog.setCaretPosition(txtServerLog.getDocument().getLength());

            } catch (BadLocationException ex) {
                Logger.getLogger(StarboundServerGUI.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            lastLine = line;
        }
    }

    private void updatePlayerCount() {
        lblPlayer.setText("Players: " + tblPlayers.getModel().getRowCount());
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame();

        StarboundServerGUI starboundServerGUI = new StarboundServerGUI(frame);
        frame.add(starboundServerGUI);
        frame.pack();
        frame.setTitle("Icer's Starbound Server Management Console v" + StarboundServer.MANAGEMENT_VERSION_NUMBER);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setVisible(true);

        starboundServerGUI.init();

    }

    @Override
    public void playerJoined(PlayerPOJO player) {
        synchronized (tblPlayers) {
            DefaultTableModel model = (DefaultTableModel) tblPlayers.getModel();
            model.addRow(new String[]{currentDateString(), player.getIp(), player.getName(), "----"});
            updatePlayerCount();
//            ProxyManager.mapNewIP(player.getIp());
        }
    }

    @Override
    public void playerLeft(PlayerPOJO player) {
        synchronized (tblPlayers) {
            DefaultTableModel model = (DefaultTableModel) tblPlayers.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object value = model.getValueAt(i, 2);
                if (value instanceof String) {
                    String string = (String) value;
                    if (player.getName().equals(string)) {
                        model.removeRow(i);
                        break;
                    }
                }
            }
            updatePlayerCount();
        }
    }

    @Override
    public void serverVersion(String version) {
        txtServerVersion.setText(version);
    }

    @Override
    public void serverStatus(ServerStatus status) {
        if (status.equals(ServerStatus.Running) && serverStartTime == 0) {
            serverStartTime = System.currentTimeMillis();
            uptimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long time = (System.currentTimeMillis() - serverStartTime) / 1000;
                    String format;
                    if (time >= 86400) {
                        format = String.format("%dd:%dh:%02dm:%02ds", time / 86400, (time % 86400) / 3600, (time % 3600) / 60, (time % 60));
                    } else if (time >= 3600) {
                        format = String.format("%dh:%02dm:%02ds", time / 3600, (time % 3600) / 60, (time % 60));
                    } else {
                        format = String.format("%02dm:%02ds", (time % 3600) / 60, (time % 60));
                    }
                    txtServerStatus.setText("Running for " + format);
                }
            }, 0, 5000);
        } else if (status.equals(ServerStatus.Shutdown)) {
            uptimer.cancel();
            txtServerStatus.setText("Server shutdown");
        }
    }

    @Override
    public void serverIP(String ip) {
        txtIP.setText(ip);
    }

    @Override
    public void playerJoinFailed(PlayerPOJO player, JoinFailed failed) {
        synchronized (tblFailedLogins) {
            System.out.println("Player failed to join from " + player.getIp() + " because of " + failed);

            DefaultTableModel model = (DefaultTableModel) tblFailedLogins.getModel();

//            Random r = new Random();
//            String ip = (r.nextInt(60) + 50) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10) + "." + (r.nextInt(200) + 10);

            model.addRow(new String[]{currentDateString(), player.getIp(), failed.toString()});
//            model.addRow(new String[]{currentDateString(), ip, failed.toString()});
        }
    }

    @Override
    public void chatMessage(ChatPOJO chat) {
        synchronized (txtChat) {
            StyledDocument doc = txtChat.getStyledDocument();
            try {
                Style styleName = txtChat.addStyle("userName Style", null);
                StyleConstants.setForeground(styleName, Color.BLUE);

                Style styleText = txtChat.addStyle("userName Style", null);
                StyleConstants.setForeground(styleText, Color.DARK_GRAY);

                doc.insertString(doc.getLength(), currentDateString() + " ", null);
                doc.insertString(doc.getLength(), chat.getUser() + ": ", styleName);
                doc.insertString(doc.getLength(), chat.getText() + "\n", styleText);
                txtChat.setCaretPosition(txtChat.getDocument().getLength());


            } catch (BadLocationException ex) {
                Logger.getLogger(StarboundServerGUI.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String formatDateString(long time) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date;
        if (time <= 0) {
            date = new Date();
        } else {
            date = new Date(time);
        }
        return dateFormat.format(date);
    }

    private String currentDateString() {
        return formatDateString(0);
    }

    @Override
    public void worldLoaded(String sector, String coordinates) {
        synchronized (tblLoadedWorlds) {
            DefaultTableModel model = (DefaultTableModel) tblLoadedWorlds.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String tSector = (String) model.getValueAt(i, 0);
                String tCoord = (String) model.getValueAt(i, 1);
                if (sector.equals(tSector) && coordinates.equals(tCoord)) {
                    return;
                }
            }
            model.addRow(new String[]{sector, coordinates});
        }
    }

    @Override
    public void worldUnloaded(String sector, String coordinates) {
        synchronized (tblLoadedWorlds) {
            DefaultTableModel model = (DefaultTableModel) tblLoadedWorlds.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String tSector = (String) model.getValueAt(i, 0);
                String tCoord = (String) model.getValueAt(i, 1);
                if (sector.equals(tSector) && coordinates.equals(tCoord)) {
                    model.removeRow(i);
                    break;
                }
            }
        }
    }

    private Style getStyle(String line) {
        Style styleText = null;
        if (line.startsWith("<ISSMConsole> ")) {
            styleText = txtServerLog.addStyle("Info_Style_Console", null);
            StyleConstants.setBackground(styleText, new Color(230, 230, 250));
        } else if (line.startsWith("Info")) {
            styleText = txtServerLog.addStyle("Info_Style_Info", null);
            StyleConstants.setBackground(styleText, Color.GREEN);
        } else if (line.startsWith("Warn: ")) {
            styleText = txtServerLog.addStyle("Info_Style_Warn", null);
            StyleConstants.setBackground(styleText, Color.lightGray);
        } else if (line.startsWith("Error: ") || line.contains("error") || line.contains("Error")) {
            styleText = txtServerLog.addStyle("Info_Style_Error", null);
            StyleConstants.setBackground(styleText, Color.RED);
        }
        return styleText;
    }

    @Override
    public void playerBanned(String ip, long until) {
        BannedPlayer bannedPlayer = ProxyManager.getBannedPlayer(ip);

        DefaultTableModel dtm = (DefaultTableModel) tblFailedLogins1.getModel();
        for (int i = dtm.getRowCount() - 1; i >= 0; i--) {
            String ipTbl = (String) dtm.getValueAt(i, 0);
            if (ipTbl.equals(ip)) {
                dtm.removeRow(i);
                break;
            }
        }

        String bannedUntil = (bannedPlayer.getBannedUntil() <= 0 ? "Permanently" : formatDateString(bannedPlayer.getBannedUntil()));
        dtm.addRow(new String[]{bannedPlayer.getIp(), bannedUntil});
        lblBannedPlayers.setText("Banned Players (" + dtm.getRowCount() + ")");
    }

    @Override
    public void playerAddedtoWhitelist(String ip) {
        DefaultTableModel dtm = (DefaultTableModel) tblWhitelist.getModel();
        for (int i = dtm.getRowCount() - 1; i >= 0; i--) {
            String ipTbl = (String) dtm.getValueAt(i, 0);
            if (ipTbl.equals(ip)) {
                dtm.removeRow(i);
                break;
            }
        }
        dtm.addRow(new String[]{ip});
        lblWhitelistPlayers.setText("Whitelist Players (" + dtm.getRowCount() + ")  -  " + (dtm.getRowCount() > 0 ? "ACTIVE" : "INACTIVE"));
    }

    @Override
    public void playerAddedToAdmins(String ip) {
        DefaultTableModel dtm = (DefaultTableModel) tblServerAdmins.getModel();
        for (int i = dtm.getRowCount() - 1; i >= 0; i--) {
            String ipTbl = (String) dtm.getValueAt(i, 0);
            if (ipTbl.equals(ip)) {
                dtm.removeRow(i);
                break;
            }
        }
        dtm.addRow(new String[]{ip});
        lblServerAdmins.setText("Server Admins (" + dtm.getRowCount() + ")");
    }

    @Override
    public void banPlayer(String ip, ProxyActions action) {
        try {
            performPlayerAction(ip, action, false);
        } catch (IOException ex) {
            Logger.getLogger(StarboundServerGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void serverLocation(String location) {
        txtServerLocation.setText(location);
    }

    @Override
    public void tcpRedirectActive(boolean active) {
        chkTCPRedirect.setSelected(active);
    }

    @Override
    public void recieveStartingInformation(ServerState serverState) {
        System.out.println("recieving on client");
        currentState = serverState;
        serverIP(currentState.getIp());
        serverLocation(currentState.getLocation());
        serverStatus(currentState.getStatus());
        serverVersion(currentState.getVersion());
        tcpRedirectActive(currentState.isRedirectActive());
        for (PlayerPOJO playerPOJO : currentState.getActivePlayers()) {
            playerJoined(playerPOJO);
        }
        for (String string : currentState.getAdmins()) {
            playerAddedToAdmins(string);
        }
        for (PlayerPOJO playerPOJO : currentState.getBannedPlayers()) {
            playerBanned(playerPOJO.getIp(), playerPOJO.getBannedUntil());
        }
        for (PlayerPOJO playerPOJO : currentState.getFailedToJoin()) {
            playerJoinFailed(playerPOJO, playerPOJO.getFailedReason());
        }
        for (String string : currentState.getWhiteList()) {
            playerAddedtoWhitelist(string);
        }
        for (WorldPOJO worldPOJO : currentState.getWorlds()) {
            worldLoaded(worldPOJO.getSector(), worldPOJO.getCoordinates());
        }
    }

    @Override
    public ServerState getStartingInformation() {
        return currentState;
    }

    @Override
    public void playerKicked(String ip) {
        
    }
}

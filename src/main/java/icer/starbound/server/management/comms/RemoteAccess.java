/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.comms;

import icer.starbound.server.management.ServerListener;
import icer.starbound.server.management.ServerStatus;
import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.client.StarboundServerGUI;
import icer.starbound.server.management.pojos.ServerState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Icer
 */
public class RemoteAccess {

    private Socket socket;
    private ServerSocket ss;
    public final static int REMOTE_ACCESS_PORT_SERVER = 21005;
    public final static int REMOTE_ACCESS_PORT_CLIENT = 21006;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isServer = false;
    private final static String GREETINGS_VERSION = "I'm a ISSMConsole, " + StarboundServer.MANAGEMENT_VERSION_NUMBER;
    private final static String ACCEPTABLE_CONNECTION = "lets talk";
    private final static String UNACCEPTABLE_CONNECTION = "sorry";
    private final static String REFLECTION_COMMAND = "ReflectionCommand incoming";
    private final static String REFLECTION_COMMAND_AND_WAIT = "ReflectionCommand incoming, and I want an answer";
    private final static String REFLECTION_RESULT = "ReflectionResult incoming";
    private final static String RELEASE_LOCK = "Release the read lock";
    private final static String SEPERATOR = "6:9@*)'a";
    private int port;
    private final Object lockTransmit = new Object();
    private final Object lockRecieve1 = new Object();
    private final Object lockRecieve2 = new Object();
    ServerListener reflectObject;
    private volatile Object responseObject = null;
    private final Object responseNotifier = new Object();
    private boolean printTransactions = false;

    public static void main(String[] args) throws IOException {
        RemoteAccess local = new RemoteAccess("localhost", RemoteAccess.REMOTE_ACCESS_PORT_SERVER);
        RemoteAccess remote = new RemoteAccess(null, RemoteAccess.REMOTE_ACCESS_PORT_SERVER);

    }

    public RemoteAccess(String ip, int port) throws IOException {
//        this.port = port;
//        if (ip == null) {
        isServer = ip == null;
//            startServer(port);
//        } else {
//            startClient(ip, port);
//        }
    }

    public void setServer(StarboundServer server) {
        reflectObject = server;
    }

    public void setGui(StarboundServerGUI gui) {
        reflectObject = gui;
    }

    public void startClient(final String ip, final int port) throws IOException {
        new Thread("Client listening thread") {
            @Override
            public void run() {
                while (socket == null) {
                    try {
                        socket = new Socket(ip, port);
                    } catch (IOException ex) {
                        System.out.println("error connecting socket");
                        try {
                            sleep(1000);
                        } catch (InterruptedException ex1) {
                            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex1);
                            return;
                        }
                    }
                }
                try {
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());
                    send(GREETINGS_VERSION);
                    startListening();
                } catch (IOException ex) {
                    Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    public void startServer(int port) throws IOException {
        ss = new ServerSocket(port);
        new Thread("Server listening thread") {
            @Override
            public void run() {
                try {
                    socket = ss.accept();
                    System.out.println("server accept");
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());
                    startListening();
                } catch (IOException ex) {
                    Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private void sendInt(int num) {
        send(num);
    }

    private void send(Object str) {
        if (printTransactions) {
            System.out.println(port + "\t" + "t\t" + (isServer ? "s" : "c") + "\t" + str);
        }
        try {
            out.writeObject(str);
        } catch (IOException ex) {
            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Object sendRequestAndWait(String method, Object... args) throws IOException {
        synchronized (lockTransmit) {
            send(REFLECTION_COMMAND_AND_WAIT);
            send(method);
            sendInt(args.length);
            for (Object object : args) {
                send(object);
            }
            try {
                synchronized (responseNotifier) {
                    responseNotifier.wait();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
            return responseObject;
        }
    }

    public void sendRequest(String method, Object... args) throws IOException {
        synchronized (lockTransmit) {
            send(REFLECTION_COMMAND);
            send(method);
            sendInt(args.length);
            for (Object object : args) {
                send(object);
            }
        }
    }

    private void startListening() {
        String inputLine, outputLine;
        Object inputObject = "";
        try {
            System.out.println("listening");
            Object lastObject = null;
            while (!"asd".equals("qwe")) {
                inputObject = readObject();
                if (printTransactions) {
                    System.out.println(port + "\t" + "r\t" + (isServer ? "s" : "c") + "\tcomm " + inputObject.toString());
                }
                lastObject = inputObject;
                if (inputObject instanceof String) {
                    inputLine = (String) inputObject;
                    if (inputLine.equals(GREETINGS_VERSION)) {
                        double ver = getVersion(inputLine);
                        if (isServer) {
                            send(GREETINGS_VERSION);
                            if (ver == StarboundServer.MANAGEMENT_VERSION_NUMBER) {
                                send(ACCEPTABLE_CONNECTION);
                            } else {
                                send(UNACCEPTABLE_CONNECTION);
                            }
                        } else {
                            if (readLine().equals(ACCEPTABLE_CONNECTION)) {
                                if (printTransactions) {
                                    System.out.println(port + "\t" + "-\tc\tI'm accepted!");
                                }
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (reflectObject != null) {
                                                ServerState startingInformation = reflectObject.getStartingInformation();
                                                sendRequest("recieveStartingInformation", startingInformation);
                                            }
                                        } catch (IOException ex) {
                                            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                }.start();
//                                    sendRequestB("getB");
//                                    sendRequestB("setB", "abc");
//                                    sendRequestB("getB");
//                                    Object o = sendRequestB("getA");
//                                    System.out.println(o.getClass().getCanonicalName());
                            } else {
                                if (printTransactions) {
                                    System.out.println(port + "\t" + "-\tc\tI'm rejected!");
                                }
                            }
                        }
                    } else if (inputLine.equals(REFLECTION_COMMAND) || inputLine.equals(REFLECTION_COMMAND_AND_WAIT)) {
                        String method = readLine();
                        int i = readInt();
                        Object[] args = new Object[i];
                        Class[] classes = new Class[i];
                        for (int j = 0; j < i; j++) {
                            args[j] = readObject();
                            classes[j] = args[j].getClass();
                        }
                        if (isServer) {
                            try {
                                Method meth = reflectObject.getClass().getMethod(method, classes);
                                Object o = meth.invoke(reflectObject, (Object[]) args);
                                if (inputLine.equals(REFLECTION_COMMAND_AND_WAIT)) {
                                    send(REFLECTION_RESULT);
                                    send(o);
                                }
                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } else if (inputLine.equals(REFLECTION_RESULT)) {
                        Object result = readObject();
                        responseObject = result;
                        synchronized (responseNotifier) {
                            responseNotifier.notifyAll();
                        }
                    }
//                    else if (inputLine.equals(RELEASE_LOCK)) {
//                        System.out.println("released from 2");
//                    }
//                    }
                }
            }
            System.out.println("out of loop");
            System.exit(0);
        } catch (IOException ex) {
            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int readInt() throws IOException {
        int line = 0;
        try {
            line = (Integer) in.readObject();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (printTransactions) {
            System.out.println(port + "\t" + "r\t" + (isServer ? "s" : "c") + "\t" + line);
        }
        return line;
    }

    private String readLine() throws IOException {
        String line = null;
        try {
            line = (String) in.readObject();
            if (printTransactions) {
                System.out.println(port + "\t" + "r\t" + (isServer ? "s" : "c") + "\t" + line);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }

    private Object readObject() throws IOException {
        Object line = null;
        try {
            line = in.readObject();
            if (printTransactions) {
                System.out.println(port + "\t" + "r\t" + (isServer ? "s" : "c") + "\t" + line);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RemoteAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }

    private double getVersion(String str) {
        String trim = str.split(",")[1].trim();
        try {
            return Double.parseDouble(trim);
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    class A {

        ServerStatus a = ServerStatus.Loading;
        String b = "bb";

        public ServerStatus getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }
    }

    class B {

        String c = "cc";
        String d = "dd";

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }

        public String getD() {
            return d;
        }

        public void setD(String d) {
            this.d = d;
        }
    }
}

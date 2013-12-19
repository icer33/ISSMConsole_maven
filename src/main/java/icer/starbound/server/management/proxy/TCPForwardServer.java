package icer.starbound.server.management.proxy;

/**
 * This program is an example from the book "Internet programming with Java" by
 * Svetlin Nakov. It is freeware. For more information:
 * http://www.nakov.com/books/inetjava/
 */
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCPForwardServer is a simple TCP bridging software that allows a TCP port on
 * some host to be transparently forwarded to some other TCP port on some other
 * host. TCPForwardServer continuously accepts client connections on the
 * listening TCP port (source port) and starts a thread (ClientThread) that
 * connects to the destination host and starts forwarding the data between the
 * client socket and destination socket.
 */
public class TCPForwardServer {

    public static final int SOURCE_PORT = 21025;
    public static final String DESTINATION_HOST = "127.0.0.1";
    public static final int DESTINATION_PORT = 21024;

    public static void main(String[] args) throws IOException {
        new TCPForwardServer(SOURCE_PORT, DESTINATION_PORT, DESTINATION_HOST);
    }

    public TCPForwardServer() throws IOException {
        this(SOURCE_PORT, DESTINATION_PORT, DESTINATION_HOST);
    }

    public TCPForwardServer(int sourcePort, int destPort, String hostname) throws IOException {
        ServerSocket serverSocket =
                new ServerSocket(sourcePort);
        System.out.println("Proxy listening");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                boolean allow = ProxyManager.allowConnection(clientSocket.getInetAddress().getHostAddress());
                if (!allow) {
                    System.out.println("Connection not allowed - closing");
                    clientSocket.close();
                    continue;
                }
                ClientThread clientThread =
                        new ClientThread(clientSocket);
                clientThread.start();
            } catch (IOException ex) {
                Logger.getLogger(TCPForwardServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

/**
 * ClientThread is responsible for starting forwarding between the client and
 * the server. It keeps track of the client and servers sockets that are both
 * closed on input/output error during the forwarding. The forwarding is
 * bidirectional and is performed by two ForwardThread instances.
 */
class ClientThread extends Thread {

    private Socket mClientSocket;
    private Socket mServerSocket;
    private boolean mForwardingActive = false;

    ClientThread(Socket aClientSocket) {
        mClientSocket = aClientSocket;
    }

    /**
     * Establishes connection to the destination server and starts bidirectional
     * forwarding of data between the client and the server.
     */
    @Override
    public void run() {
        InputStream clientIn;
        OutputStream clientOut;
        InputStream serverIn;
        OutputStream serverOut;
        try {
            // Connect to the destination server 

            ClientAddress serverAddress = new ClientAddress(TCPForwardServer.DESTINATION_HOST, TCPForwardServer.DESTINATION_PORT);
            ProxyManager.addClient(new ClientAddress(mClientSocket), serverAddress);
            mServerSocket = new Socket(
                    TCPForwardServer.DESTINATION_HOST,
                    TCPForwardServer.DESTINATION_PORT);
            serverAddress.setSocket(mServerSocket);

            // Turn on keep-alive for both the sockets 
            mServerSocket.setKeepAlive(true);
            mClientSocket.setKeepAlive(true);

            // Obtain client & server input & output streams 
            clientIn = mClientSocket.getInputStream();
            clientOut = mClientSocket.getOutputStream();
            serverIn = mServerSocket.getInputStream();
            serverOut = mServerSocket.getOutputStream();
        } catch (IOException ioe) {
            System.err.println("Can not connect to "
                    + TCPForwardServer.DESTINATION_HOST + ":"
                    + TCPForwardServer.DESTINATION_PORT);
            connectionBroken();
            return;
        }

        // Start forwarding data between server and client 
        mForwardingActive = true;
        ForwardThread clientForward =
                new ForwardThread(this, clientIn, serverOut);
        clientForward.start();
        ForwardThread serverForward =
                new ForwardThread(this, serverIn, clientOut);
        serverForward.start();

        System.out.println("TCP Forwarding "
                + mClientSocket.getInetAddress().getHostAddress()
                + ":" + mClientSocket.getPort() + " <--> "
                + mServerSocket.getInetAddress().getHostAddress()
                + ":" + mServerSocket.getPort() + " started.");

    }

    /**
     * Called by some of the forwarding threads to indicate that its socket
     * connection is broken and both client and server sockets should be closed.
     * Closing the client and server sockets causes all threads blocked on
     * reading or writing to these sockets to get an exception and to finish
     * their execution.
     */
    public synchronized void connectionBroken() {
        try {
            mServerSocket.close();
        } catch (Exception e) {
        }
        try {
            mClientSocket.close();
        } catch (Exception e) {
        }

        if (mForwardingActive) {
            System.out.println("TCP Forwarding "
                    + mClientSocket.getInetAddress().getHostAddress()
                    + ":" + mClientSocket.getPort() + " <--> "
                    + mServerSocket.getInetAddress().getHostAddress()
                    + ":" + mServerSocket.getPort() + " stopped.");
            mForwardingActive = false;
        }
    }
}

/**
 * ForwardThread handles the TCP forwarding between a socket input stream
 * (source) and a socket output stream (dest). It reads the input stream and
 * forwards everything to the output stream. If some of the streams fails, the
 * forwarding stops and the parent is notified to close all its sockets.
 */
class ForwardThread extends Thread {

    private static final int BUFFER_SIZE = 8192;
    InputStream mInputStream;
    OutputStream mOutputStream;
    ClientThread mParent;

    /**
     * Creates a new traffic redirection thread specifying its parent, input
     * stream and output stream.
     */
    public ForwardThread(ClientThread aParent, InputStream aInputStream, OutputStream aOutputStream) {
        mParent = aParent;
        mInputStream = aInputStream;
        mOutputStream = aOutputStream;
    }

    /**
     * Runs the thread. Continuously reads the input stream and writes the read
     * data to the output stream. If reading or writing fail, exits the thread
     * and notifies the parent about the failure.
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (true) {
                int bytesRead = mInputStream.read(buffer);
                if (bytesRead == -1) {
                    break; // End of stream is reached --> exit 
                }
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
            }
        } catch (IOException e) {
            // Read/write failed --> connection is broken 
        }

        // Notify parent thread that the connection is broken 
        mParent.connectionBroken();
    }
}
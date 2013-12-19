/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.proxy;

import java.net.Socket;
import java.util.Objects;

/**
 *
 * @author Icer
 */
public class ClientAddress {

    private String ip;
    private int port;
    private Socket socket;

    public ClientAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    
    public ClientAddress(Socket socket) {
        ip = socket.getInetAddress().getHostAddress();
        port = socket.getPort();
        this.socket = socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.ip);
        hash = 53 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClientAddress other = (ClientAddress) obj;
        if (!Objects.equals(this.ip, other.ip)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ClientAddress{" + "ip=" + ip + ", port=" + port + '}';
    }
}

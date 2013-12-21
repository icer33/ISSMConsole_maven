package icer.starbound.server.management.jetty.servlets;

import icer.starbound.server.management.enums.ServerStatus;
import icer.starbound.server.management.listeners.ServerListener;
import icer.starbound.server.management.pojos.ChatPOJO;
import icer.starbound.server.management.pojos.PlayerPOJO;
import icer.starbound.server.management.pojos.ServerState;
import icer.starbound.server.management.pojos.WorldPOJO;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.codehaus.jackson.map.ObjectMapper;

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSource.Emitter;
import org.eclipse.jetty.servlets.EventSourceServlet;

public class ServerListenerServlet extends EventSourceServlet implements ServerListener {

    private static final Logger LOG = Logger.getLogger("event-source-sample");
    private final Set<Emitter> emitters = new CopyOnWriteArraySet<>();
    private volatile ScheduledExecutorService executor;
    ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void destroy() {
        this.executor.shutdown();
        this.emitters.clear();
        super.destroy();
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest request) {
        return new ListenerEventSource();
    }

    @Override
    public void newLine(String line) {
        sendToEmitters("newLine:" + line);
    }

    @Override
    public void playerJoined(PlayerPOJO player) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, player);
            sendToEmitters("playerJoined:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void playerJoinFailed(PlayerPOJO player) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, player);
            sendToEmitters("playerJoinFailed:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void playerLeft(PlayerPOJO player) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, player);
            sendToEmitters("playerLeft:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void serverLocation(String location) {
        sendToEmitters("serverLocation:" + location);
    }

    @Override
    public void serverVersion(String version) {
        sendToEmitters("serverVersion:" + version);
    }

    @Override
    public void serverStatus(ServerStatus status) {
        sendToEmitters("serverStatus:" + status.toString());
    }

    @Override
    public void serverIP(String ip) {
        sendToEmitters("serverIP:" + ip);
    }

    @Override
    public void chatMessage(ChatPOJO chat) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, chat);
            sendToEmitters("chatMessage:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void playerKicked(String ip) {
        sendToEmitters("playerKicked:" + ip);
    }

    @Override
    public void playerAddedtoWhitelist(String ip) {
        sendToEmitters("playerAddedtoWhitelist:" + ip);
    }

    @Override
    public void playerAddedToAdmins(String ip) {
        sendToEmitters("playerAddedToAdmins:" + ip);
    }

    @Override
    public void tcpRedirectActive(boolean active) {
    }

    @Override
    public void recieveStartingInformation(ServerState serverState) {
    }

    @Override
    public ServerState getStartingInformation() {
        return null;
    }

    @Override
    public void worldLoaded(WorldPOJO world) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, world);
            sendToEmitters("worldLoaded:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void worldUnloaded(WorldPOJO world) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, world);
            sendToEmitters("worldUnloaded:" + sw.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerListenerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void playerBanned(PlayerPOJO player) {
    }
    private void sendToEmitters(String data) {
        for (Emitter emitter : emitters) {
            try {
                emitter.data(data);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "could not send update to client", e);
            }
        }
    }
    
    final class ListenerEventSource implements EventSource {

        private volatile Emitter emitter;

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            this.emitter = emitter;
            emitters.add(emitter);
        }

        public void onResume(Emitter emitter, String lastEventId) throws IOException {
            onOpen(emitter);
        }

        @Override
        public void onClose() {
            emitters.remove(this.emitter);
        }
    }
}

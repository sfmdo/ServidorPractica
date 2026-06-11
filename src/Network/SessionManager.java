package Network;

import Messages.MessagePacket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManager {
    private static SessionManager instance;
    private final ConcurrentHashMap<String, ClientConnection> activeSessions;
    
    private SessionManager() { 
        activeSessions = new ConcurrentHashMap<>();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    public void registerSession(String userId, ClientConnection connection){
        activeSessions.put(userId, connection);
    }
    
    public void removeSession(String userId){
        activeSessions.remove(userId);
    }
    
    public ClientConnection getSession(String userId){
        return activeSessions.get(userId);
    }

    public Collection<ClientConnection> getOnlineConnections() {
        return activeSessions.values();
    }

    public void broadcastToAll(MessagePacket packet) {
        activeSessions.values().forEach(connection -> {
            connection.sendPacket(packet);
        });
    }
    
    public List<String> getConnectedUsers(String excludeUserId){
        return activeSessions.keySet().stream()
            .filter(key -> !key.equals(excludeUserId))
            .collect(Collectors.toList());
    }
    
    public void broadcast(MessagePacket packet, String excludeUserId) {
        activeSessions.forEach((userId, connection) -> {
            if (!userId.equals(excludeUserId)) {
                connection.sendPacket(packet);
            }
        });
    }
}
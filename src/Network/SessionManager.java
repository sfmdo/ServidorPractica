package Network;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author sfmdo
 */
public class SessionManager {
    private static SessionManager instance;
    private ConcurrentHashMap<String, ClientConnection> activeSessions;
    
     private SessionManager() { 
        activeSessions = new ConcurrentHashMap<>();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public synchronized void registerSession(String userId, ClientConnection connection){
        activeSessions.put(userId, connection);
    }
    
    public synchronized void removeSession(String userId){
        activeSessions.remove(userId);
    }
    
    public ClientConnection getSession(String userId){
        return activeSessions.get(userId);
    }
    
    public List<String> getConnectedUsers(String excludeUserId){
        List<String> filteredKeys = activeSessions.keySet().stream()
    .filter(key -> !key.equals(excludeUserId))
    .collect(Collectors.toList());
        return filteredKeys;
    }
    
    
}

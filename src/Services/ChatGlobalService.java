package Services;

import Messages.MessagePacket;
import Models.GlobalMessage;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;
import java.lang.System.Logger; 
import java.lang.System.Logger.Level; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGlobalService {
    private static ChatGlobalService instance;
    private static final Logger LOGGER = System.getLogger(ChatGlobalService.class.getName());
    
    // Nuestra "Tabla SQL" en RAM: Key = "ID_MENOR-ID_MAYOR", Value = Fila de mensajes
    private ConcurrentHashMap<String, List<GlobalMessage>> localHistory;

    private ChatGlobalService() {
        localHistory = new ConcurrentHashMap<>();
    }

    public static synchronized ChatGlobalService getInstance() {
        if (instance == null) instance = new ChatGlobalService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        switch (packet.getAction()) {
            case Protocol.GLOBAL_MSG:
                processMessage(packet, client);
                break;
            case Protocol.GLOBAL_FETCH_HISTORY: 
                handleFetchHistory(packet, client);
                break;
        }
    }

    private void processMessage(MessagePacket packet, ClientConnection client) {
        String myId = client.getCurrentUserId();
        String targetId = packet.getParam("targetUserId");
        String text = packet.getParam("text");
        LOGGER.log(Level.INFO, "Mensaje local de {0}, para {1}, contenido: {2}",myId,targetId, text);
        if (targetId == null || text == null) return;

        ClientConnection targetConn = SessionManager.getInstance().getSession(targetId);
        
        if (targetConn != null) {
            String chatKey = generateKey(myId, targetId);
            GlobalMessage msg = new GlobalMessage(myId, targetId, text);
            
            // Insertar en nuestra tabla RAM
            localHistory.computeIfAbsent(chatKey, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
            
            // Reenvío al destinatario
            targetConn.sendPacket(MessagePacket.event(Protocol.GLOBAL_MSG)
                    .add("from", myId)
                    .add("text", text)
                    .add("timestamp", msg.getTimestamp()));
            
            // Confirmar al emisor
            client.sendPacket(MessagePacket.response(Protocol.GLOBAL_MSG, packet.getToken()).add("status", "success"));
            LOGGER.log(Level.INFO, "Mensaje guardado con exito de {0}, para {1}",myId,targetId);
        } else {
            LOGGER.log(Level.INFO, "Mensaje no guardado de {0}, para {1}, DESCONECTADO",myId,targetId);
            client.sendPacket(MessagePacket.response(Protocol.GLOBAL_MSG, packet.getToken())
                .add("status", "error").add("reason", "Usuario offline."));
        }
    }

    private void handleFetchHistory(MessagePacket packet, ClientConnection client) {
        String myId = client.getCurrentUserId();
        String targetId = packet.getParam("targetUserId");
        String chatKey = generateKey(myId, targetId);

        // "SELECT * FROM localHistory WHERE key = chatKey"
        List<GlobalMessage> history = localHistory.getOrDefault(chatKey, new ArrayList<>());

        client.sendPacket(MessagePacket.response(Protocol.GLOBAL_FETCH_HISTORY, packet.getToken())
                .add("status", "success")
                .add("targetUserId", targetId)
                .add("history", history));
        
        LOGGER.log(Level.INFO, "Enviando historial RAM ({0} msgs) a usuario {1}", history.size(), myId);
    }

    public void clearUserHistory(String userId) {
        localHistory.keySet().removeIf(key -> key.startsWith(userId + "-") || key.endsWith("-" + userId));
        LOGGER.log(Level.INFO, "Tabla RAM: Limpieza para usuario {0} completada.", userId);
    }

    private String generateKey(String id1, String id2) {
        if (id1.compareTo(id2) < 0) return id1 + "-" + id2;
        else return id2 + "-" + id1;
    }
}
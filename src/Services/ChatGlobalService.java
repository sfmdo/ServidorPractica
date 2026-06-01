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
    
    // Simulación de "Tabla": Key = "ID1-ID2" (ordenados), Value = Lista de mensajes
    private ConcurrentHashMap<String, List<GlobalMessage>> localHistory;

    private ChatGlobalService() {
        localHistory = new ConcurrentHashMap<>();
    }

    public static synchronized ChatGlobalService getInstance() {
        if (instance == null) instance = new ChatGlobalService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        String myId = client.getCurrentUserId();
        String targetId = packet.getParam("targetUserId");
        String text = packet.getParam("text");
        
        if (targetId == null || text == null) {
            LOGGER.log(Level.WARNING, "Petición de Chat Global incompleta de usuario: {0}", myId);
            return;
        }
        // 1. Generar la llave única para la pareja (ej: "1-2" siempre será igual a "2-1")
        String chatKey = generateKey(myId, targetId);

        // 2. Guardar el mensaje en la "Tabla Local" (RAM)
        GlobalMessage msg = new GlobalMessage(myId, targetId, text);
        localHistory.computeIfAbsent(chatKey, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        LOGGER.log(Level.INFO, "Mensaje Global guardado en RAM. Llave: {0} | De: {1}", chatKey, myId);
        
        // 3. Entrega inmediata si el destinatario está online
        ClientConnection targetConn = SessionManager.getInstance().getSession(targetId);
        if (targetConn != null) {
            targetConn.sendPacket(MessagePacket.event(Protocol.GLOBAL_MSG)
                    .add("from", myId)
                    .add("text", text)
                    .add("timestamp", msg.getTimestamp()));
            LOGGER.log(Level.INFO, "Mensaje Global entregado en tiempo real a: {0}", targetId);
        }
        
        LOGGER.log(Level.INFO, "Destinatario {0} offline. Mensaje conservado en RAM.", targetId);
    }

    
    // Borra todas las conversaciones donde participe el usuario que se desconecta.
     
    public void clearUserHistory(String userId) {
        // Contamos cuántas conversaciones se eliminan para el log
        int sizeBefore = localHistory.size();
        
        localHistory.keySet().removeIf(key -> key.contains("-" + userId) || key.contains(userId + "-"));
        
        int sizeAfter = localHistory.size();
        int deleted = sizeBefore - sizeAfter;

        if (deleted > 0) {
            LOGGER.log(Level.INFO, "Memoria RAM liberada: {0} conversaciones eliminadas para usuario {1}", deleted, userId);
        } else {
            LOGGER.log(Level.INFO, "No había conversaciones activas en RAM para el usuario {1}", deleted, userId);
        }
    }

    // Genera una llave consistente sin importar quién envíe primero.
     
    private String generateKey(String id1, String id2) {
        if (id1.compareTo(id2) < 0) return id1 + "-" + id2;
        else return id2 + "-" + id1;
    }
}
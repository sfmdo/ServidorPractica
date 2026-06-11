package Services;

import Messages.MessagePacket;
import Models.GlobalMessage;
import Network.ClientConnection;
import Network.Router;
import Network.SessionManager;
import java.lang.System.Logger; 
import java.lang.System.Logger.Level; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGlobalService {
    private static final Logger LOGGER = System.getLogger(ChatGlobalService.class.getName());
    private static ChatGlobalService instance;

    private static final String ACTION_MSG = "GLOBAL_MSG";
    private static final String ACTION_FETCH = "GLOBAL_FETCH_HISTORY";

    private final Router serviceRouter = new Router();
    
    private final ConcurrentHashMap<String, List<GlobalMessage>> localHistory;

    private ChatGlobalService() {
        this.localHistory = new ConcurrentHashMap<>();
        serviceRouter.add(ACTION_MSG,   this::processMessage);
        serviceRouter.add(ACTION_FETCH, this::handleFetchHistory);
    }

    public static synchronized ChatGlobalService getInstance() {
        if (instance == null) instance = new ChatGlobalService();
        return instance;
    }

    public Router getRouter() {
        return serviceRouter;
    }



    private void processMessage(MessagePacket packet, ClientConnection client) {
        String myId = client.getCurrentUserId();
        String targetId = packet.getParam("targetUserId");
        String text = packet.getParam("text");

        if (targetId == null || text == null) return;

        ClientConnection targetConn = SessionManager.getInstance().getSession(targetId);
        
        if (targetConn != null) {
            String chatKey = generateKey(myId, targetId);
            GlobalMessage msg = new GlobalMessage(myId, targetId, text);

            localHistory.computeIfAbsent(chatKey, k -> 
                Collections.synchronizedList(new ArrayList<>())
            ).add(msg);

            targetConn.sendPacket(MessagePacket.event(ACTION_MSG)
                    .add("from", myId)
                    .add("text", text)
                    .add("timestamp", msg.getTimestamp()));

            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success"));
            
            LOGGER.log(Level.INFO, "Mensaje RAM: {0} -> {1}", myId, targetId);
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                .add("status", "error")
                .add("reason", "Usuario offline."));
        }
    }

    private void handleFetchHistory(MessagePacket packet, ClientConnection client) {
        String myId = client.getCurrentUserId();
        String targetId = packet.getParam("targetUserId");
        
        if (targetId == null) return;

        String chatKey = generateKey(myId, targetId);
        List<GlobalMessage> history = localHistory.getOrDefault(chatKey, new ArrayList<>());

        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                .add("status", "success")
                .add("targetUserId", targetId)
                .add("history", history));
        
        LOGGER.log(Level.INFO, "Historial RAM enviado a {0} (Key: {1})", myId, chatKey);
    }

    public void clearUserHistory(String userId) {
        localHistory.keySet().removeIf(key -> 
            key.startsWith(userId + "-") || key.endsWith("-" + userId)
        );
        LOGGER.log(Level.INFO, "Limpieza de RAM para usuario: {0}", userId);
    }

    private String generateKey(String id1, String id2) {
        return (id1.compareTo(id2) < 0) ? (id1 + "-" + id2) : (id2 + "-" + id1);
    }
}
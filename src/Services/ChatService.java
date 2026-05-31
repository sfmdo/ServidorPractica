package Services;

import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;

public class ChatService {
    private static ChatService instance;

    public void handle(MessagePacket packet, ClientConnection client) {
        switch (packet.getAction()) {
            case Protocol.GLOBAL_MSG -> handleGlobal(packet, client);
            case Protocol.FRIEND_MSG -> handlePrivate(packet, client);
            case Protocol.GROUP_MSG -> handleGroup(packet, client);
        }
    }

    private void handleGlobal(MessagePacket packet, ClientConnection client) {
        String target = packet.getParam("targetUser");
        String text = packet.getParam("text");

        // Chat volátil: Buscamos en RAM, no en DB
        ClientConnection targetConn = SessionManager.getInstance().getSession(target);
        
        if (targetConn != null) {
            targetConn.sendPacket(MessagePacket.event(Protocol.GLOBAL_MSG)
                    .add("from", client.getCurrentUserId())
                    .add("text", text));
        } else {
            client.sendPacket(MessagePacket.event(Protocol.GLOBAL_MSG)
                    .add("error", "El usuario no está conectado."));
        }
    }

    private void handlePrivate(MessagePacket packet, ClientConnection client) {
        // 1. Guardar en DB (DAO private_messages)
        // 2. Si el amigo está online, enviar EVENTO en tiempo real
    }

    private void handleGroup(MessagePacket packet, ClientConnection client) {
        // 1. Guardar en DB (DAO group_messages)
        // 2. Obtener lista de miembros del grupo en DB
        // 3. Por cada miembro online (SessionManager), enviar EVENTO
    }
    
    public static synchronized ChatService getInstance() {
        if (instance == null) instance = new ChatService();
        return instance;
    }
}
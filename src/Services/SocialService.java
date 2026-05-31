/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Services;

import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;

public class SocialService {
    private static SocialService instance;

    public void handle(MessagePacket packet, ClientConnection client) {
        switch (packet.getAction()) {
            case Protocol.FRIEND_REQUEST: handleFriendRequest(packet, client); break;
            case Protocol.GROUP_CREATE: handleGroupCreate(packet, client); break;
        }
    }

    private void handleFriendRequest(MessagePacket packet, ClientConnection client) {
        String fromId = client.getCurrentUserId();
        String targetName = packet.getParam("targetUser");

        // 1. Guardar en DB la solicitud (FriendshipDAO)
        // 2. Crear notificación persistente en DB (NotificationDAO)
        
        // 3. BIDIRECCIONALIDAD: ¿Está el objetivo online ahora mismo?
        // String targetId = UserDAO.getIdByName(targetName);
        String targetId = "456"; // Simulación
        ClientConnection targetConn = SessionManager.getInstance().getSession(targetId);

        if (targetConn != null) {
            // Se la enviamos en tiempo real como un EVENTO
            targetConn.sendPacket(MessagePacket.event(Protocol.NOTIFICATION)
                    .add("type", "FRIEND_REQ")
                    .add("fromUser", fromId));
        }
        
        client.sendPacket(MessagePacket.response(Protocol.FRIEND_REQUEST, packet.getToken())
                .add("status", "sent"));
    }

    public static synchronized SocialService getInstance() {
        if (instance == null) instance = new SocialService();
        return instance;
    }
}
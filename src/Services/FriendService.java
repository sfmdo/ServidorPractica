package Services;

import DAOlayer.FriendDAO;
import DAOlayer.MessageDAO;
import Models.PrivateMessages;
import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class FriendService {
    private static final Logger LOGGER = System.getLogger(FriendService.class.getName());
    private static FriendService instance;
    private FriendDAO friendDAO = new FriendDAO();
    private MessageDAO messageDAO = new MessageDAO();

    private FriendService() {}

    public static synchronized FriendService getInstance() {
        if (instance == null) instance = new FriendService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        switch (packet.getAction()) {
            case Protocol.FRIEND_REQUEST: handleRequest(packet, client); break;
            case Protocol.FRIEND_ACCEPT: handleAccept(packet, client); break;
            case Protocol.FRIEND_MSG: handlePrivateMsg(packet, client); break;
        }
    }

    private void handleRequest(MessagePacket packet, ClientConnection client) {
        int fromId = Integer.parseInt(client.getCurrentUserId());
        int targetId = packet.getIntParam("targetUserId");
        LOGGER.log(Level.INFO, "Procesando solicitud de amistad: De {0} para {1}", fromId, targetId);
        // 1. Crear registro PENDING en la DB
        if (friendDAO.sendFriendRequest(fromId, targetId)) {
            
            // 2. Crear notificación persistente para el receptor
            NotificationService.getInstance().createNotification(
                targetId, fromId, "FRIEND_REQUEST", 0, "Nueva solicitud de amistad"
            );

            // 3. Bidireccionalidad: Si el objetivo está online, avisarle YA
            ClientConnection targetConn = SessionManager.getInstance().getSession(String.valueOf(targetId));
            if (targetConn != null) {
                targetConn.sendPacket(MessagePacket.event(Protocol.FRIEND_REQUEST)
                    .add("fromId", fromId)
                    .add("fromName", client.getSocket().getInetAddress().toString())); // O el nombre de usuario
                LOGGER.log(Level.INFO, "Notificación de solicitud enviada en tiempo real al usuario {0}", targetId);
            }

            client.sendPacket(MessagePacket.response(Protocol.FRIEND_REQUEST, packet.getToken()).add("status", "success"));
        } else {
            LOGGER.log(Level.WARNING, "No se pudo crear la solicitud de {0} para {1}. Posible duplicado.", fromId, targetId);
            client.sendPacket(MessagePacket.response(Protocol.FRIEND_REQUEST, packet.getToken()).add("status", "error"));
        }
    }

    private void handleAccept(MessagePacket packet, ClientConnection client) {
        int friendshipId = packet.getIntParam("friendshipId");
        int myId = Integer.parseInt(client.getCurrentUserId());
        int requesterId = packet.getIntParam("requesterId");
        
        LOGGER.log(Level.INFO, "Usuario {0} aceptando solicitud de amistad de {1}", myId, requesterId);
        
        // 1. Cambiar estado a ACCEPTED en DB
        if (friendDAO.acceptFriendRequest(friendshipId)) {
            
            // 2. Notificar al que envió la solicitud original
            // (Necesitaríamos buscar quién era el otro en la DB, asumamos que viene en el packet
            
            NotificationService.getInstance().createNotification(
                requesterId, myId, "SYSTEM", friendshipId, "Solicitud de amistad aceptada"
            );

            // 3. Si el otro está online, avisarle del evento
            ClientConnection requesterConn = SessionManager.getInstance().getSession(String.valueOf(requesterId));
            if (requesterConn != null) {
                requesterConn.sendPacket(MessagePacket.event(Protocol.FRIEND_ACCEPT)
                    .add("friendId", myId)
                    .add("msg", "¡Ahora son amigos!"));
                LOGGER.log(Level.INFO, "Evento de amistad aceptada entregado a {0}", requesterId);
            }

            client.sendPacket(MessagePacket.response(Protocol.FRIEND_ACCEPT, packet.getToken()).add("status", "success"));
        } else {
            LOGGER.log(Level.ERROR, "Fallo al actualizar estado de amistad en DB para ID: {0}", friendshipId);
        }
    }

    private void handlePrivateMsg(MessagePacket packet, ClientConnection client) {
        int senderId = Integer.parseInt(client.getCurrentUserId());
        int targetUserId = packet.getIntParam("targetUserId");
        String text = packet.getParam("text");

        // 1. Obtener ID de relación
        int fId = friendDAO.getFriendshipId(senderId, targetUserId);
        
        if (fId != -1) {
            // 2. Persistencia en DB
            PrivateMessages dbMsg = new PrivateMessages();
            dbMsg.setFriendshipId(fId);
            dbMsg.setSenderId(senderId);
            dbMsg.setMessage(text);
            messageDAO.saveMessage(dbMsg);

            LOGGER.log(Level.INFO, "Mensaje privado persistido: De {0} para {1} (Friendship: {2})", senderId, targetUserId, fId);
            
            // 3. Entrega en tiempo real si el amigo está conectado
            ClientConnection friendConn = SessionManager.getInstance().getSession(String.valueOf(targetUserId));
            if (friendConn != null) {
                friendConn.sendPacket(MessagePacket.event(Protocol.FRIEND_MSG)
                    .add("from", senderId)
                    .add("text", text));
                LOGGER.log(Level.INFO, "Mensaje privado entregado vía socket a {0}", targetUserId);
            }
            
            client.sendPacket(MessagePacket.response(Protocol.FRIEND_MSG, packet.getToken()).add("status", "sent"));
        } else {
            LOGGER.log(Level.WARNING, "BLOQUEO: El usuario {0} intentó mensajear a {1} sin tener amistad.", senderId, targetUserId);
            client.sendPacket(MessagePacket.response(Protocol.FRIEND_MSG, packet.getToken()).add("status", "error").add("reason", "No son amigos"));
        }
    }
}
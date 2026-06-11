package Services;

import DAOlayer.FriendDAO;
import DAOlayer.MessageDAO;
import Models.PrivateMessages;
import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Router;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

public class FriendService {
    private static final Logger LOGGER = System.getLogger(FriendService.class.getName());
    private static FriendService instance;
    private static final String EVENT_UPDATE_LIST = "FRIEND_LIST_UPDATE";

    private final Router serviceRouter = new Router();
    private FriendDAO friendDAO = new FriendDAO();
    private MessageDAO messageDAO = new MessageDAO();

    private FriendService() {
        serviceRouter.add("FRIEND_REQ",     this::handleRequest);
        serviceRouter.add("FRIEND_ACCEPT",  this::handleAccept);
        serviceRouter.add("FRIEND_MSG",     this::handlePrivateMsg);
        serviceRouter.add("FRIEND_DECLINE", this::handleDecline);
        serviceRouter.add("FRIEND_HISTORY", this::handleFetchHistory);
        serviceRouter.add("FRIEND_LIST",    this::handleFriendList);
    }

    public static synchronized FriendService getInstance() {
        if (instance == null) instance = new FriendService();
        return instance;
    }

    public Router getRouter() {
        return serviceRouter;
    }

    private void handleRequest(MessagePacket packet, ClientConnection client) {
        int fromId = Integer.parseInt(client.getCurrentUserId());
        int targetId = packet.getIntParam("targetUserId");

        if (friendDAO.sendFriendRequest(fromId, targetId)) {
            int fId = friendDAO.getFriendshipId(fromId, targetId);
            
            NotificationService.getInstance().createNotification(
                targetId, fromId, "FRIEND_REQUEST", fId, "Nueva solicitud de amistad"
            );

            ClientConnection targetConn = SessionManager.getInstance().getSession(String.valueOf(targetId));
            if (targetConn != null) {
                targetConn.sendPacket(MessagePacket.event(packet.getAction())
                    .add("fromId", fromId)
                    .add("fromName", client.getSocket().getInetAddress().toString())); 
            }

            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "success"));
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "La solicitud ya existe o el usuario no existe"));
        }
    }

    private void handleAccept(MessagePacket packet, ClientConnection client) {
        int myId = Integer.parseInt(client.getCurrentUserId());
        int requesterId = packet.getIntParam("targetUserId"); 
        
        int fId = friendDAO.getFriendshipId(myId, requesterId);
        if (friendDAO.acceptFriendRequest(fId)) {
            NotificationService.getInstance().cleanNotificationFriends(myId, requesterId, "FRIEND_REQUEST");
            pushFriendList(myId, client);
            ClientConnection requesterConn = SessionManager.getInstance().getSession(String.valueOf(requesterId));
            if (requesterConn != null) {
                requesterConn.sendPacket(MessagePacket.event(packet.getAction())
                    .add("friendId", myId)
                    .add("msg", "¡Ahora son amigos!"));
                requesterConn.sendPacket(MessagePacket.event(EVENT_UPDATE_LIST));
                pushFriendList(requesterId, requesterConn);
            }
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "success"));
            LOGGER.log(Level.INFO, "Amistad aceptada entre {0} y {1}. Actualización enviada a ambos.", myId, requesterId);
        } else {
            LOGGER.log(Level.ERROR, "Fallo al aceptar amistad ID: {0}", fId);
        }
    }
    
    private void handleDecline(MessagePacket packet, ClientConnection client) {
        int myId = Integer.parseInt(client.getCurrentUserId());
        int fId = packet.getIntParam("friendshipId"); 

        LOGGER.log(Level.INFO, "Usuario {0} rechazando solicitud de amistad ID: {1}", myId, fId);
        if (friendDAO.declineFriendRequest(fId)) {
            NotificationService.getInstance().cleanNotificationFriends(myId, fId, "FRIEND_REQUEST");
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success"));
            
            LOGGER.log(Level.INFO, "Solicitud {0} rechazada con éxito por el usuario {1}", fId, myId);
        } else {
            LOGGER.log(Level.WARNING, "No se pudo rechazar la solicitud {0}", fId);
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo actualizar el estado de la solicitud."));
        }
    }
    
    private void pushFriendList(int userId, ClientConnection conn) {
        Models.User me = new Models.User();
        me.setId(userId);
        ArrayList<Models.User> friends = friendDAO.getFriends(me);

        conn.sendPacket(MessagePacket.event("FRIEND_LIST") 
                .add("status", "success")
                .add("friends", friends));
    }

    private void handlePrivateMsg(MessagePacket packet, ClientConnection client) {
        int senderId = Integer.parseInt(client.getCurrentUserId());
        int targetUserId = packet.getIntParam("targetUserId");
        String text = packet.getParam("text");

        int fId = friendDAO.getFriendshipId(senderId, targetUserId);
        
        if (fId != -1) {
            PrivateMessages dbMsg = new PrivateMessages();
            dbMsg.setFriendshipId(fId);
            dbMsg.setSenderId(senderId);
            dbMsg.setMessage(text);
            messageDAO.saveMessage(dbMsg);
 
            ClientConnection friendConn = SessionManager.getInstance().getSession(String.valueOf(targetUserId));
            if (friendConn != null) {
                friendConn.sendPacket(MessagePacket.event(packet.getAction())
                    .add("from", senderId)
                    .add("text", text));
            }
            
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "sent"));
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "error").add("reason", "No son amigos"));
        }
    }
    
    private void handleFetchHistory(MessagePacket packet, ClientConnection client) {
        int myId = Integer.parseInt(client.getCurrentUserId());
        int targetUserId = packet.getIntParam("targetUserId");
        int fId = friendDAO.getFriendshipId(myId, targetUserId);

        if (fId != -1) {
            ArrayList<PrivateMessages> historial = messageDAO.getHistoryChat(fId);
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("history", historial));
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "Acceso denegado."));
        }
    }
    
    private void handleFriendList(MessagePacket packet, ClientConnection client) {
        int myId = Integer.parseInt(client.getCurrentUserId());
        Models.User me = new Models.User();
        me.setId(myId);

        ArrayList<Models.User> friends = friendDAO.getFriends(me);

        if (friends != null) {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("friends", friends));
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo recuperar la lista."));
        }
    }
}
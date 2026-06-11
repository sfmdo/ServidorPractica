package Services;

import DAOlayer.NotificationDAO;
import Messages.MessagePacket;
import Models.Notifications;
import Network.ClientConnection;
import Network.Router;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

public class NotificationService {
    private static final Logger LOGGER = System.getLogger(NotificationService.class.getName());
    private static NotificationService instance;

    private static final String ACTION_FETCH = "NOTIF_REQ";     
    private static final String EVENT_NOTIF  = "NOTIFICATION";  

    private final Router serviceRouter = new Router();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    private NotificationService() {
        serviceRouter.add(ACTION_FETCH, this::handleFetchRequest);
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    public Router getRouter() {
        return serviceRouter;
    }

    private void handleFetchRequest(MessagePacket packet, ClientConnection client) {
        sendPendingToUser(client.getCurrentUserId(), client);
    }


    public void sendPendingToUser(String userIdStr, ClientConnection client) {
        try {
            int userId = Integer.parseInt(userIdStr);
            ArrayList<Notifications> lista = notificationDAO.getPendingByUserId(userId);
        
            if (lista.isEmpty()){
                LOGGER.log(Level.INFO, "Usuario {0} sin notificaciones pendientes.", userId);
                return;
            }

            client.sendPacket(MessagePacket.event(EVENT_NOTIF)
                    .add("notifications", lista));
        
            LOGGER.log(Level.INFO, "Enviadas {0} notificaciones al usuario {1}", lista.size(), userId);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.ERROR, "ID de usuario inválido: {0}", userIdStr);
        }
    }

    public void createNotification(int target, int from, String type, int related, String content) {
        Notifications n = new Notifications();
        n.setTarget_user_id(target);
        n.setFrom_user_id(from);
        n.setType(type);
        n.setRelated_id(related);
        n.setContent(content);
        n.setStatus("PENDING");

        if (notificationDAO.create(n)) {
            LOGGER.log(Level.INFO, "Notificación persistida: {0} -> {1}", from, target);
            ClientConnection targetConn = SessionManager.getInstance().getSession(String.valueOf(target));
            if (targetConn != null) {
                ArrayList<Notifications> singleList = new ArrayList<>();
                singleList.add(n);
                
                targetConn.sendPacket(MessagePacket.event(EVENT_NOTIF)
                        .add("notifications", singleList));
                
                LOGGER.log(Level.INFO, "Notificación entregada instantáneamente al usuario {0}", target);
            }
        } else {
            LOGGER.log(Level.ERROR, "Error al guardar notificación para {0}", target);
        }
    }

    public void cleanNotification(int userId, int relatedId, String type) {
        notificationDAO.markAsRead(userId, relatedId, type);
        LOGGER.log(Level.INFO, "Notificación {0} leída por {1}", type, userId);
    }
    
    public void cleanNotificationFriends(int userId, int relatedId, String type) {
        notificationDAO.markAsReadFriends(userId, relatedId, type);
        LOGGER.log(Level.INFO, "Notificación de amistad {0} leída por {1}", type, userId);
    }
}
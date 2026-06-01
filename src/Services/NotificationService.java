package Services;

import DAOlayer.NotificationDAO;
import Messages.MessagePacket;
import Models.Notifications;
import Network.ClientConnection;
import Network.Protocol;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;


 // Servicio encargado de gestionar las notificaciones.
 
public class NotificationService {
    private static final Logger LOGGER = System.getLogger(NotificationService.class.getName());
    private static NotificationService instance;

    // Constructor privado para Singleton
    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

     //Se ejecuta tras un Login exitoso. Busca notificaciones que el usuario
     //recibió mientras estaba desconectado y se las envía.
     
    public void sendPendingToUser(String userIdStr, ClientConnection client) {
        try {
            int userId = Integer.parseInt(userIdStr);
            NotificationDAO dao = new NotificationDAO();
            
            // 1. Obtener de la DB las pendientes
            ArrayList<Notifications> lista = dao.getPendingByUserId(userId);
            
            if (lista.isEmpty()){
                LOGGER.log(Level.INFO, "El usuario {0} no tiene notificaciones pendientes.", userId);
                return;
            }
            
            LOGGER.log(Level.INFO, "Sincronizando {0} notificaciones pendientes para el usuario {1}", lista.size(), userId);

            // 2. Iterar y enviar cada una como un EVENTO de red
            for (Notifications n : lista) {
                MessagePacket p = MessagePacket.event(Protocol.FETCH_NOTIFICATIONS)
                        .add("from", n.getFrom_user_id())
                        .add("type", n.getType())
                        .add("content", n.getContent())
                        .add("relatedId", n.getRelated_id());

                client.sendPacket(p);
            }
            
        } catch (NumberFormatException e) {
            LOGGER.log(Level.ERROR, "Fallo al procesar notificaciones: ID de usuario inválido ({0})", userIdStr);
        }
    }

    // Método para que otros servicios (SocialService) creen notificaciones fácilmente.
    // Ejemplo: SocialService llama a esto cuando alguien manda una Friend Request.
    public void createNotification(int target, int from, String type, int related, String content) {
        Notifications n = new Notifications();
        n.setTarget_user_id(target);
        n.setFrom_user_id(from);
        n.setType(type);
        n.setRelated_id(related);
        n.setContent(content);
        n.setStatus("PENDING");

        NotificationDAO dao = new NotificationDAO();
        if (dao.create(n)) {
            LOGGER.log(Level.INFO, "Notificación guardada en DB: {0} -> {1} (Tipo: {2})", from, target, type);
        } else {
            LOGGER.log(Level.ERROR, "Error crítico al intentar persistir notificación para el usuario {0}", target);
        }
    }
}
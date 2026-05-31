/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Services;

import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;
import java.util.List;

public class NotificationService {
    private static NotificationsService instance;

    public void sendPendingToUser(String userId, ClientConnection client) {
        // 1. Consultar en DB (NotificationDAO) las que tengan status 'PENDING'
        // List<Notification> lista = NotificationDAO.getPending(userId);
        
        // 2. Si hay notificaciones, enviarlas en un solo paquete o varios
        /* 
        for(Notification n : lista) {
            client.sendPacket(MessagePacket.event(Protocol.NOTIFICATION)
                .add("id", n.getId())
                .add("type", n.getType())
                .add("content", n.getContent()));
        }
        */
    }

    public static synchronized NotificationsService getInstance() {
        if (instance == null) instance = new NotificationsService();
        return instance;
    }
}
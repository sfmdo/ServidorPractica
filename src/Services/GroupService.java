package Services;

import DAOlayer.*;
import Messages.MessagePacket;
import Models.Group;
import Models.GroupMessages;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

public class GroupService {
    private static final Logger LOGGER = System.getLogger(GroupService.class.getName());
    private static GroupService instance;
    private GroupDAO groupDAO = new GroupDAO();
    private GroupMemberDAO memberDAO = new GroupMemberDAO();
    private GroupMessageDAO messageDAO = new GroupMessageDAO();

    private GroupService() {}

    public static synchronized GroupService getInstance() {
        if (instance == null) instance = new GroupService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        switch (packet.getAction()) {
            case Protocol.GROUP_CREATE: handleCreate(packet, client); break;
            case Protocol.GROUP_MSG: handleMsg(packet, client); break;
            case Protocol.GROUP_INVITE: handleInvite(packet, client); break;
            case Protocol.GROUP_INVITE_ACCEPT: handleAcceptInvitation(packet, client); break;
            case Protocol.GROUP_HISTORY: handleFetchHistory(packet, client); break;
            case Protocol.GROUP_LEAVE: handleLeave(packet, client); break;
        }
    }

    private void handleCreate(MessagePacket packet, ClientConnection client) {
        String groupName = packet.getParam("groupName");
        int creatorId = Integer.parseInt(client.getCurrentUserId());
        
        LOGGER.log(Level.INFO, "Usuario {0} solicitó crear el grupo: {1}", creatorId, groupName);
        
        Group group = new Group();
        group.setGroupName(groupName);
        
        // 1. Crear grupo y obtener el ID real
        int groupId = groupDAO.createGroupEntities(group, creatorId);

        if (groupId != -1) {
            // 2. El creador se une automáticamente
            memberDAO.addMemberWithStatus(groupId, creatorId, "ACCEPTED");
            LOGGER.log(Level.INFO, "Grupo creado exitosamente. ID: {0}, Nombre: {1}", groupId, groupName);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_CREATE, packet.getToken())
                    .add("status", "success").add("groupId", groupId));
        } else {
            LOGGER.log(Level.ERROR, "Error en base de datos al intentar crear el grupo: {0}", groupName);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_CREATE, packet.getToken())
                    .add("status", "error").add("reason", "Error al crear el grupo en DB"));
        }
    }

    private void handleInvite(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int targetUserId = packet.getIntParam("targetUserId");
        int inviterId = Integer.parseInt(client.getCurrentUserId());

        // 1. Seguridad: Solo miembros pueden invitar
        if (!memberDAO.isMember(groupId, inviterId)){
            LOGGER.log(Level.WARNING, "SEGURIDAD: Usuario {0} intentó invitar a {1} al grupo {2} sin ser miembro.", inviterId, targetUserId, groupId);
            return;
        }
        memberDAO.addMemberWithStatus(groupId, targetUserId, "PENDING");

        // 2. Crear notificación persistente (Offline)
        // Usamos el relatedId para guardar el ID del grupo
        NotificationService.getInstance().createNotification(
            targetUserId, 
            inviterId, 
            "GROUP_INVITE", 
            groupId, 
            "Has sido invitado al grupo: " + groupId
        );

        // 3. Bidireccionalidad: Si el invitado está ONLINE, avisarle ahora
        ClientConnection targetConn = SessionManager.getInstance().getSession(String.valueOf(targetUserId));
        if (targetConn != null) {
            targetConn.sendPacket(MessagePacket.event(Protocol.NOTIFICATION)
                .add("type", "GROUP_INVITE")
                .add("groupId", String.valueOf(groupId))
                .add("from", String.valueOf(inviterId))
                .add("content", "Te han invitado al grupo " + groupId));
            LOGGER.log(Level.INFO, "Invitación al grupo {0} entregada en tiempo real a {1}", groupId, targetUserId);
        }

        client.sendPacket(MessagePacket.response(Protocol.GROUP_INVITE, packet.getToken()).add("status", "sent"));
    }
    
    private void handleAcceptInvitation(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());

        LOGGER.log(Level.INFO, "Usuario {0} aceptando invitación al grupo {1}", userId, groupId);

        // 1. Actualizar el estado en la base de datos
        if (memberDAO.updateMemberStatus(groupId, userId, "ACCEPTED")) {
            NotificationService.getInstance().cleanNotification(userId, groupId, "GROUP_INVITE");
            // 2. Responder éxito al usuario
            client.sendPacket(MessagePacket.response(Protocol.GROUP_INVITE_ACCEPT, packet.getToken())
                    .add("status", "success")
                    .add("groupId", groupId));
        
            // 3. (Opcional) Notificar al resto del grupo que alguien se unió
            ArrayList<Integer> memberIds = messageDAO.getMemberIds(groupId);
            for (Integer id : memberIds) {
                if (id == userId) continue;
                ClientConnection conn = SessionManager.getInstance().getSession(String.valueOf(id));
                if (conn != null) {
                    conn.sendPacket(MessagePacket.event(Protocol.NOTIFICATION)
                        .add("type", "SYSTEM")
                        .add("content", "El usuario " + userId + " se ha unido al grupo."));
                }
            }
        } else {
            client.sendPacket(MessagePacket.response(Protocol.GROUP_INVITE_ACCEPT, packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo procesar la aceptación."));
        }
    }

    private void handleFetchHistory(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());

        // Seguridad: Solo miembros ven el historial
        if (!memberDAO.isMember(groupId, userId)){
            LOGGER.log(Level.WARNING, "Acceso Denegado: Usuario {0} intentó ver historial del grupo {1}", userId, groupId);
            return;
        }

        // 1. Obtener lista del DAO
        ArrayList<GroupMessages> historial = messageDAO.getMessagesByGroupId(groupId);
        LOGGER.log(Level.INFO, "Enviando historial del grupo {0} al usuario {1} ({2} mensajes)", groupId, userId, historial.size());

        // 2. Enviar el paquete con la lista (Gson se encarga de convertir el ArrayList a JSON)
        client.sendPacket(MessagePacket.response(Protocol.GROUP_HISTORY, packet.getToken())
                .add("history", historial));
    }

    private void handleMsg(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        String text = packet.getParam("text");
        int senderId = Integer.parseInt(client.getCurrentUserId());

        // 1. Validar seguridad: ¿Pertenece al grupo?
        if (!memberDAO.hasAccepted(groupId, senderId)) {
            LOGGER.log(Level.WARNING, "Intento de mensaje ilegal: Usuario {0} en grupo {1}", senderId, groupId);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken())
                    .add("status", "error").add("reason", "No eres miembro del grupo"));
            return;
        }
        
        // REGLA 1: Mínimo 3 personas (Invitadas o Aceptadas) para poder enviar mensajes
        int totalMembers = memberDAO.getTotalMemberCount(groupId);
        if (totalMembers < 3) {
            LOGGER.log(Level.WARNING, "El grupo requiere mínimo 3 integrantes para chatear: Usuario {0} en grupo {1}", senderId, groupId);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken())
                    .add("status", "error")
                    .add("reason", "El grupo requiere mínimo 3 integrantes para chatear (faltan " + (3 - totalMembers) + ")"));
            return;
        }

        // 2. Persistir
        GroupMessages msg = new GroupMessages();
        msg.setGroupId(groupId);
        msg.setSenderId(senderId);
        msg.setMessage(text);
        messageDAO.addMessage(msg);

        // 3. Distribuir a los miembros conectados
        ArrayList<Integer> memberIds = messageDAO.getMemberIds(groupId);
        int onlineCount = 0;
        for (Integer id : memberIds) {
            // No enviar al emisor
            if (id == senderId) continue; 

            ClientConnection conn = SessionManager.getInstance().getSession(String.valueOf(id));
            if (conn != null) {
                conn.sendPacket(MessagePacket.event(Protocol.GROUP_MSG)
                        .add("groupId", groupId)
                        .add("from", senderId)
                        .add("text", text));
                onlineCount++;
            }
        }
        LOGGER.log(Level.INFO, "Mensaje de grupo {0} procesado. Distribuido a {1} miembros online.", groupId, onlineCount);
    }

    private void handleLeave(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());

        if (memberDAO.removeMember(groupId, userId)) {
            LOGGER.log(Level.INFO, "Usuario {0} abandonó el grupo {1}", userId, groupId);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_LEAVE, packet.getToken())
                    .add("status", "success"));
        }
    }
}
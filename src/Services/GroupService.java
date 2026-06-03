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
            case Protocol.GROUP_LIST: handleGroupList(packet, client); break;
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
                .add("groupId", groupId)
                .add("from", inviterId)
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
            LOGGER.log(Level.WARNING, "Acceso Denegado: Usuario {0} intentó ver historial del grupo {1}(No es miembro)", userId, groupId);
            return;
        }
        
        if (!memberDAO.hasAccepted(groupId, userId)) {
            LOGGER.log(Level.WARNING, "Intento de mensaje ilegal: Usuario {0} no ha aceptado solicitud en grupo {1}", userId, groupId);
            client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken())
                    .add("status", "error").add("reason", "No has aceptado la solicitud del grupo"));
            return;
        }

        // 1. Obtener lista del DAO
        ArrayList<GroupMessages> historial = messageDAO.getMessagesByGroupId(groupId);
        LOGGER.log(Level.INFO, "Enviando historial del grupo {0} al usuario {1} ({2} mensajes)", groupId, userId, historial.size());

        // 2. Enviar el paquete con la lista (Gson se encarga de convertir el ArrayList a JSON)
        client.sendPacket(MessagePacket.response(Protocol.GROUP_HISTORY, packet.getToken())
                .add("status", "success")
                .add("history", historial));
    }

    private void handleMsg(MessagePacket packet, ClientConnection client) {
    int groupId = packet.getIntParam("groupId");
    String text = packet.getParam("text");
    int senderId = Integer.parseInt(client.getCurrentUserId());

    if (!memberDAO.hasAccepted(groupId, senderId)) {
        client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken())
                .add("status", "error").add("reason", "No has aceptado la invitación"));
        return;
    }

    int totalMembers = memberDAO.getTotalMemberCount(groupId);
    if (totalMembers < 3) {
        client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken())
                .add("status", "error")
                .add("reason", "Faltan integrantes (Mínimo 3)"));
        return;
    }

    // Guardar en DB
    GroupMessages msg = new GroupMessages();
    msg.setGroupId(groupId);
    msg.setSenderId(senderId);
    msg.setMessage(text);
    messageDAO.addMessage(msg);

    // BROADCAST
    ArrayList<Integer> memberIds = messageDAO.getMemberIds(groupId);
    int onlineCount = 0;
    
    // Ver quiénes están en el mapa de sesiones para depurar
    LOGGER.log(Level.INFO, "Sesiones activas en este momento: {0}", 
               SessionManager.getInstance().getConnectedUsers(""));

    for (Integer id : memberIds) {
        if (id.intValue() == senderId) continue; 

        String sid = String.valueOf(id);
        ClientConnection conn = SessionManager.getInstance().getSession(sid);
        
        if (conn != null) {
            conn.sendPacket(MessagePacket.event(Protocol.GROUP_MSG)
                    .add("groupId", groupId)
                    .add("from", senderId)
                    .add("text", text));
            onlineCount++;
        }
    }
    LOGGER.log(Level.INFO, "Broadcast Grupo {0}: Se encontró a {1} de {2} miembros en línea.", 
               new Object[]{groupId, onlineCount, memberIds.size() - 1});
    
    // IMPORTANTE: Responder éxito al emisor para que su UI sepa que se envió
    client.sendPacket(MessagePacket.response(Protocol.GROUP_MSG, packet.getToken()).add("status", "success"));
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
    private void handleGroupList(MessagePacket packet, ClientConnection client) {
        // 1. Obtener mi ID desde la conexión autenticada
        int myId = Integer.parseInt(client.getCurrentUserId());
    
        LOGGER.log(Level.INFO, "Usuario {0} solicitó su lista de grupos.", myId );

        // 2. Llamar al DAO que creamos anteriormente (getUserGroups)
        // Nota: memberDAO es una instancia de GroupMemberDAO dentro de esta clase
        ArrayList<Models.Group> groups = memberDAO.getUserGroups(myId);

        if (groups != null) {
            // 3. Responder con éxito y la lista de objetos Group
            // Gson convertirá el ArrayList automáticamente
            client.sendPacket(MessagePacket.response(Protocol.GROUP_LIST, packet.getToken())
                    .add("status", "success")
                    .add("groups", groups));
        
            LOGGER.log(Level.INFO, "Enviada lista de {0} grupos al usuario {1}", new Object[]{groups.size(), myId});
        } else {
            client.sendPacket(MessagePacket.response(Protocol.GROUP_LIST, packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo recuperar la lista de grupos."));
        }
    }
}
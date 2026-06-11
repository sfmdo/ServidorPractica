package Services;

import DAOlayer.*;
import Messages.MessagePacket;
import Models.Group;
import Models.GroupMessages;
import Network.ClientConnection;
import Network.Router;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

public class GroupService {
    private static final Logger LOGGER = System.getLogger(GroupService.class.getName());
    private static GroupService instance;

    private static final String ACTION_CREATE = "GROUP_CREATE";
    private static final String ACTION_INVITE = "GROUP_INVITE";
    private static final String ACTION_ACCEPT = "GROUP_ACCEPT";
    private static final String ACTION_MSG    = "GROUP_MSG";
    private static final String ACTION_HISTORY = "GROUP_HISTORY";
    private static final String ACTION_LEAVE   = "GROUP_LEAVE";
    private static final String ACTION_LIST    = "GROUP_LIST";
    private static final String ACTION_DECLINE = "GROUP_DECLINE"; 
    
    
    private static final String EVENT_UPDATE_LIST = "GROUP_LIST_UPDATE";

    private final Router serviceRouter = new Router();
    private final GroupDAO groupDAO = new GroupDAO();
    private final GroupMemberDAO memberDAO = new GroupMemberDAO();
    private final GroupMessageDAO messageDAO = new GroupMessageDAO();

    private GroupService() {
        serviceRouter.add(ACTION_CREATE,  this::handleCreate);
        serviceRouter.add(ACTION_INVITE,  this::handleInvite);
        serviceRouter.add(ACTION_ACCEPT,  this::handleAcceptInvitation);
        serviceRouter.add(ACTION_MSG,     this::handleMsg);
        serviceRouter.add(ACTION_HISTORY, this::handleFetchHistory);
        serviceRouter.add(ACTION_LEAVE,   this::handleLeave);
        serviceRouter.add(ACTION_LIST,    this::handleGroupList);
        serviceRouter.add(ACTION_DECLINE, this::handleDeclineInvitation);
    }

    public static synchronized GroupService getInstance() {
        if (instance == null) instance = new GroupService();
        return instance;
    }

    public Router getRouter() {
        return serviceRouter;
    }

    private void handleCreate(MessagePacket packet, ClientConnection client) {
        String groupName = packet.getParam("groupName");
        int creatorId = Integer.parseInt(client.getCurrentUserId());
        
        Group group = new Group();
        group.setGroupName(groupName);
        int groupId = groupDAO.createGroupEntities(group, creatorId);

        if (groupId != -1) {
            memberDAO.addMemberWithStatus(groupId, creatorId, "ACCEPTED");
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success").add("groupId", groupId));

            pushGroupListToUser(creatorId, client);
            
            LOGGER.log(Level.INFO, "Grupo {0} creado por {1}", groupId, creatorId);
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error").add("reason", "Error en DB"));
        }
    }

    private void handleInvite(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int targetUserId = packet.getIntParam("targetUserId");
        int inviterId = Integer.parseInt(client.getCurrentUserId());

        if (!memberDAO.isMember(groupId, inviterId)) return;

        memberDAO.addMemberWithStatus(groupId, targetUserId, "PENDING");

        NotificationService.getInstance().createNotification(
            targetUserId, inviterId, "GROUP_INVITE", groupId, "Invitación a grupo"
        );

        notifyUserUpdate(targetUserId);

        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "sent"));
    }
    
    private void handleAcceptInvitation(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());

        if (memberDAO.updateMemberStatus(groupId, userId, "ACCEPTED")) {
            NotificationService.getInstance().cleanNotification(userId, groupId, "GROUP_INVITE");
            
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("groupId", groupId));
        
            client.sendPacket(MessagePacket.event(EVENT_UPDATE_LIST));

            broadcastToGroup(groupId, userId, MessagePacket.event("NOTIFICATION")
                .add("type", "SYSTEM")
                .add("content", "Un nuevo usuario se ha unido al grupo."));
            
            broadcastUpdateToGroup(groupId, userId);

        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error").add("reason", "Error al procesar"));
        }
    }
    
    private void handleDeclineInvitation(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());

        LOGGER.log(Level.INFO, "Usuario {0} rechazando invitación al grupo {1}", userId, groupId);
        if (memberDAO.updateMemberStatus(groupId, userId, "DECLINE")) {
            NotificationService.getInstance().cleanNotification(userId, groupId, "GROUP_INVITE");
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("groupId", groupId));
            validateGroupThreshold(groupId);
            client.sendPacket(MessagePacket.event("GROUP_LIST_UPDATE"));
            LOGGER.log(Level.INFO, "Invitación rechazada con éxito por usuario {0} para grupo {1}", userId, groupId);
        } else {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo rechazar la invitación."));
        }
    }
    

    private void handleMsg(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        String text = packet.getParam("text");
        int senderId = Integer.parseInt(client.getCurrentUserId());

        if (!memberDAO.hasAccepted(groupId, senderId)) {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error").add("reason", "No has aceptado la invitación"));
            return;
        }

        if (memberDAO.getTotalMemberCount(groupId) < 3) {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error").add("reason", "Mínimo 3 integrantes"));
            return;
        }

        GroupMessages msg = new GroupMessages();
        msg.setGroupId(groupId);
        msg.setSenderId(senderId);
        msg.setMessage(text);
        messageDAO.addMessage(msg);

        broadcastToGroup(groupId, senderId, MessagePacket.event(ACTION_MSG)
                .add("groupId", groupId)
                .add("from", senderId)
                .add("text", text));

        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken()).add("status", "success"));
    }

    private void handleLeave(MessagePacket packet, ClientConnection client) {
    int groupId = packet.getIntParam("groupId");
    int userId = Integer.parseInt(client.getCurrentUserId());
    if (memberDAO.removeMember(groupId, userId)) {
        LOGGER.log(Level.INFO, "Usuario {0} abandonó el grupo {1}", userId, groupId);
        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                .add("status", "success"));
        validateGroupThreshold(groupId);
        client.sendPacket(MessagePacket.event("GROUP_LIST_UPDATE"));
    } else {
        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "No se pudo abandonar el grupo."));
        }
    }

    private void handleGroupList(MessagePacket packet, ClientConnection client) {
        int myId = Integer.parseInt(client.getCurrentUserId());
        ArrayList<Models.Group> groups = memberDAO.getUserGroups(myId);

        if (groups != null) {
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("groups", groups));
        }
    }

    private void handleFetchHistory(MessagePacket packet, ClientConnection client) {
        int groupId = packet.getIntParam("groupId");
        int userId = Integer.parseInt(client.getCurrentUserId());
        if (!memberDAO.isMember(groupId, userId)) return;
        
        ArrayList<GroupMessages> historial = messageDAO.getMessagesByGroupId(groupId);
        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                .add("status", "success")
                .add("history", historial));
    }


    private void notifyUserUpdate(int userId) {
        ClientConnection conn = SessionManager.getInstance().getSession(String.valueOf(userId));
        if (conn != null) {
            conn.sendPacket(MessagePacket.event(EVENT_UPDATE_LIST));
        }
    }

    private void broadcastToGroup(int groupId, int excludeId, MessagePacket packet) {
        ArrayList<Integer> memberIds = messageDAO.getMemberIds(groupId);
        for (Integer id : memberIds) {
            if (id == excludeId) continue;
            ClientConnection conn = SessionManager.getInstance().getSession(String.valueOf(id));
            if (conn != null) conn.sendPacket(packet);
        }
    }
    
    private void validateGroupThreshold(int groupId) {
        int currentMembers = memberDAO.getTotalMemberCount(groupId);

        if (currentMembers < 3 && currentMembers > 0) {
            LOGGER.log(Level.INFO, "Grupo {0} disuelto por falta de miembros (Quedan: {1})", groupId, currentMembers);
            broadcastUpdateToGroup(groupId, -1);
            groupDAO.deleteGroup(groupId);
        } else if (currentMembers >= 3) {
            broadcastUpdateToGroup(groupId, -1);
        }
    }

    private void broadcastUpdateToGroup(int groupId, int excludeId) {
        ArrayList<Integer> memberIds = messageDAO.getMemberIds(groupId);
        for (Integer id : memberIds) {
            if (id == excludeId) continue;
            ClientConnection conn = SessionManager.getInstance().getSession(String.valueOf(id));
            if (conn != null) {
                conn.sendPacket(MessagePacket.event("GROUP_LIST_UPDATE"));
            }
        }
    }
    
    private void pushGroupListToUser(int userId, ClientConnection conn) {
        ArrayList<Models.Group> groups = memberDAO.getUserGroups(userId);
        conn.sendPacket(MessagePacket.event("GROUP_LIST")
                .add("status", "success")
                .add("groups", groups));
    }

}
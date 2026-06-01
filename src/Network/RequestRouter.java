package Network;

import Messages.MessagePacket;
import Services.AuthService;
import Services.ChatGlobalService;
import Services.FriendService;
import Services.GroupService;
import Services.NotificationService;


// Clase encargada de redirigir los paquetes a los servicios correspondientes.

public class RequestRouter {
    
    private static final System.Logger LOGGER = System.getLogger(RequestRouter.class.getName());

    public static void route(MessagePacket packet, ClientConnection client) {
        
    
        String action = packet.getAction();
        LOGGER.log(System.Logger.Level.INFO, "Peticion de tipo {0} del usuario {1}", 
                new Object[]{action, client.getCurrentUserId()});
        // 1. Validar si el paquete es nulo o no tiene acción
        if (action == null) return;

        // 2. Acciones de Autenticación (No requieren sesión activa)
        if (action.equals(Protocol.LOGIN) || action.equals(Protocol.REGISTER)) {
            AuthService.getInstance().handle(packet, client);
            return;
        }

        // 3. FILTRO DE SEGURIDAD: Si no está "AUTHENTICATED", rebotar cualquier otra acción
        if (!client.getCurrentState().equals("AUTHENTICATED")) {
            client.sendPacket(MessagePacket.response(action, packet.getToken())
                    .add("status", "error")
                    .add("reason", "Acceso denegado. Por favor, inicie sesión."));
            return;
        }

        // 4. ENRUTAMIENTO MODULAR
        switch (action) {
            
            // --- CONTEXTO DE AMISTAD ---
            case Protocol.FRIEND_REQUEST:
            case Protocol.FRIEND_ACCEPT:
            case Protocol.FRIEND_DECLINE:
            case Protocol.FRIEND_MSG:
                FriendService.getInstance().handle(packet, client);
                break;

            // --- CONTEXTO DE GRUPOS ---
            case Protocol.GROUP_CREATE:
            case Protocol.GROUP_MSG:
            case Protocol.GROUP_INVITE:
            case Protocol.GROUP_LEAVE:
            case Protocol.GROUP_HISTORY:
                GroupService.getInstance().handle(packet, client);
                break;

            // --- CHAT GLOBAL (VOLÁTIL EN RAM) ---
            case Protocol.GLOBAL_MSG:
                ChatGlobalService.getInstance().handle(packet, client);
                break;

            // --- NOTIFICACIONES ---
            case Protocol.FETCH_NOTIFICATIONS:
                // Llamamos directamente al servicio de notificaciones para refrescar pendientes
                NotificationService.getInstance().sendPendingToUser(client.getCurrentUserId(), client);
                break;

            default:
                LOGGER.log(System.Logger.Level.INFO, "Acción no reconocida enviada por {0}, Accion: {1}", 
                new Object[]{action, client.getCurrentUserId()});
                client.sendPacket(MessagePacket.response(action, packet.getToken())
                        .add("status", "error")
                        .add("reason", "Acción no soportada por el servidor."));
                break;
        }
    }
}
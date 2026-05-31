/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Services;
import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;

/**
 *
 * @author sfmdo
 */
public class RequestRouter {

    public static void route(MessagePacket packet, ClientConnection client) {
        String action = packet.getAction();

        // 1. Acciones que NO requieren estar logueado
        if (action.equals(Protocol.LOGIN) || action.equals(Protocol.REGISTER)) {
            AuthService.getInstance().handle(packet, client);
            return;
        }

        // 2. Validar Seguridad: Si no está logueado, rebotar el resto
        if (!client.getState().equals("AUTHENTICATED")) {
            client.sendPacket(MessagePacket.createResponse(action, "ERROR")
                    .add("reason", "Debes iniciar sesión primero."));
            return;
        }

        // 3. Enrutar según la naturaleza de la acción
        switch (action) {
            case Protocol.GLOBAL_MSG:
            case Protocol.FRIEND_MSG:
            case Protocol.GROUP_MSG:
                ChatService.getInstance().handle(packet, client);
                break;

            case Protocol.FRIEND_REQUEST:
            case Protocol.FRIEND_ACCEPT:
            case Protocol.GROUP_CREATE:
            case Protocol.GROUP_LEAVE:
            case Protocol.FETCH_NOTIFICATIONS:
                SocialService.getInstance().handle(packet, client);
                break;

            default:
                System.out.println("Acción no reconocida: " + action);
        }
    }
}

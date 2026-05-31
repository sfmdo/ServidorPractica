/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Services;

import Messages.MessagePacket;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;
/**
 *
 * @author sfmdo
 */
public class AuthService {
    private static AuthService instance;

    public void handle(MessagePacket packet, ClientConnection client) {
        String action = packet.getAction();
        if (action.equals(Protocol.LOGIN)) handleLogin(packet, client);
        else if (action.equals(Protocol.REGISTER)) handleRegister(packet, client);
    }

    private void handleLogin(MessagePacket packet, ClientConnection client) {
        String user = packet.getParam("user");
        String pass = packet.getParam("pass");

        // 1. Validar en DB (UserDAO)
        // User userDb = UserDAO.validate(user, pass);
        boolean isValid = true; // Simulación

        if (isValid) {
            String userId = "123"; // ID obtenida de la DB
            
            // 2. Actualizar estado de la conexión
            client.setAuthenticated(userId);
            
            // 3. Registrar en el mapa global de sesiones
            SessionManager.getInstance().registerSession(userId, client);

            client.sendPacket(MessagePacket.response(Protocol.LOGIN, packet.getToken())
                    .add("status", "success").add("userId", userId));
            
            // 4. Trigger automático: Mandar notificaciones pendientes
            NotificationsService.getInstance().sendPendingToUser(userId, client);
        } else {
            client.sendPacket(MessagePacket.response(Protocol.LOGIN, packet.getToken())
                    .add("status", "error").add("reason", "Credenciales incorrectas"));
        }
    }

    private void handleRegister(MessagePacket packet, ClientConnection client) {
        // Lógica para UserDAO.insert(...)
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }
}

package Services;

import DAOlayer.UserDAO;
import Messages.MessagePacket;
import Models.User;
import Network.ClientConnection;
import Network.Protocol;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level; 

public class AuthService {
    private static AuthService instance;
    private UserDAO userDAO = new UserDAO();
    private static final Logger LOGGER = System.getLogger(AuthService.class.getName());

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        String action = packet.getAction();
        if (action.equals(Protocol.LOGIN)) handleLogin(packet, client);
        else if (action.equals(Protocol.REGISTER)) handleRegister(packet, client);
    }

    private void handleLogin(MessagePacket packet, ClientConnection client) {
        String username = packet.getParam("user");
        String pass = packet.getParam("pass");
        
        LOGGER.log(Level.INFO, "Intento de inicio de sesión: {0}", username);
        // 1. Validar en DB
        User userDb = userDAO.validateCredentials(username, pass);

        if (userDb != null) {
            String userId = String.valueOf(userDb.getId());
            
            // 2. Elevar estado de la conexión en memoria
            client.setAuthenticated(userId);
            
            // 3. Registrar en el mapa de sesiones activas
            SessionManager.getInstance().registerSession(userId, client);
            
            // 4. Actualizar status en DB a ONLINE
            userDAO.updateOnlineStatus(userDb.getId(), "ONLINE");

            // 5. Responder éxito al cliente (devolvemos el token que él mandó)
            client.sendPacket(MessagePacket.response(Protocol.LOGIN, packet.getToken())
                    .add("status", "success")
                    .add("userId", userId)
                    .add("username", userDb.getUsername()));
            
            LOGGER.log(Level.INFO, "Usuario autenticado con éxito: {0} (ID: {1})", username, userId);

            // 6. Enviar notificaciones que recibió mientras estaba offline
            NotificationService.getInstance().sendPendingToUser(userId, client);
            
        } else {
            LOGGER.log(Level.WARNING, "Fallo de autenticación para el usuario: {0}", username);
            client.sendPacket(MessagePacket.response(Protocol.LOGIN, packet.getToken())
                    .add("status", "error")
                    .add("reason", "Usuario o contraseña incorrectos"));
        }
    }

    private void handleRegister(MessagePacket packet, ClientConnection client) {
        String username = packet.getParam("user");
        String pass = packet.getParam("pass");

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(pass);

        if (userDAO.addUser(newUser)) {
            LOGGER.log(Level.INFO, "Nuevo usuario registrado: {0}", username);
            client.sendPacket(MessagePacket.response(Protocol.REGISTER, packet.getToken())
                    .add("status", "success")
                    .add("message", "Usuario creado correctamente"));
        } else {
            LOGGER.log(Level.ERROR, "Error al registrar usuario: {0}. Posible duplicado.", username);
            client.sendPacket(MessagePacket.response(Protocol.REGISTER, packet.getToken())
                    .add("status", "error")
                    .add("reason", "El nombre de usuario ya existe o error de DB"));
        }
    }
}
package Services;

import DAOlayer.UserDAO;
import Messages.MessagePacket;
import Models.User;
import Network.ClientConnection;
import Network.Router;
import Network.SessionManager;
import java.lang.System.Logger;
import java.lang.System.Logger.Level; 

public class AuthService {
    private static final Logger LOGGER = System.getLogger(AuthService.class.getName());
    private static AuthService instance;

    private static final String ACTION_LOGIN = "LOGIN";
    private static final String ACTION_REGISTER = "REGISTER";
    

    private final Router serviceRouter = new Router();
    private final UserDAO userDAO = new UserDAO();

    private AuthService() {
        serviceRouter.add(ACTION_LOGIN,    this::handleLogin);
        serviceRouter.add(ACTION_REGISTER, this::handleRegister);
        serviceRouter.add("LOGOUT",   this::handleLogout);
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public Router getRouter() {
        return serviceRouter;
    }

    private void handleLogin(MessagePacket packet, ClientConnection client) {
        String username = packet.getParam("user");
        String pass = packet.getParam("pass");
        
        LOGGER.log(Level.INFO, "Intento de login: {0}", username);
        User userDb = userDAO.validateCredentials(username, pass);

        if (userDb != null) {
            String userId = String.valueOf(userDb.getId());
            client.setAuthenticated(userId);
            SessionManager.getInstance().registerSession(userId, client);
            userDAO.updateOnlineStatus(userDb.getId(), "ONLINE");
            UserService.getInstance().broadcastAllUsers();

            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("userId", userId)
                    .add("username", userDb.getUsername()));
            
            LOGGER.log(Level.INFO, "Login exitoso: {0}", username);

            NotificationService.getInstance().sendPendingToUser(userId, client);
            
        } else {
            LOGGER.log(Level.WARNING, "Login fallido: {0}", username);
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
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
            LOGGER.log(Level.INFO, "Registro exitoso: {0}", username);
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "success")
                    .add("message", "Usuario creado correctamente"));
        } else {
            LOGGER.log(Level.ERROR, "Registro fallido: {0}", username);
            client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                    .add("status", "error")
                    .add("reason", "El nombre de usuario ya existe"));
        }
    }
    private void handleLogout(MessagePacket packet, ClientConnection client) {
        String userId = client.getCurrentUserId();
        if (userId != null) {
            LOGGER.log(Level.INFO, "Logout voluntario del usuario: {0}", userId);
            SessionManager.getInstance().removeSession(userId);
            userDAO.updateOnlineStatus(Integer.parseInt(userId), "OFFLINE");
            Services.ChatGlobalService.getInstance().clearUserHistory(client.getCurrentUserId());
            client.setCurrentUserId(null);
            client.setCurrentState("CONNECTED");
            UserService.getInstance().broadcastAllUsers();
        
            client.sendPacket(MessagePacket.response("LOGOUT", packet.getToken()).add("status", "success"));
        }
    }
}
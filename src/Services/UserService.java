package Services;

import DAOlayer.UserDAO;
import Messages.MessagePacket;
import Models.User;
import Network.ClientConnection;
import Network.Router; 
import Network.SessionManager;
import java.util.ArrayList;

public class UserService {
    private static final System.Logger LOGGER = System.getLogger(UserService.class.getName());
    private static UserService instance;

    private static final String ACTION_FETCH = "FETCH_USERS";

    private final Router serviceRouter = new Router();
    private final UserDAO userDAO = new UserDAO();
    
    private UserService() {
        serviceRouter.add(ACTION_FETCH, this::handleFetchUsers);
    }

    public static synchronized UserService getInstance() {
        if (instance == null) instance = new UserService();
        return instance;
    }
    
    public Router getRouter() {
        return serviceRouter;
    }

    private void handleFetchUsers(MessagePacket packet, ClientConnection client) {
        ArrayList<User> allUsers = userDAO.getAllUsers();
        
        LOGGER.log(System.Logger.Level.INFO, "Enviando lista completa de usuarios al ID: {0}", client.getCurrentUserId());

        client.sendPacket(MessagePacket.response(packet.getAction(), packet.getToken())
                .add("status", "success")
                .add("users", allUsers));
    }
    
    public void broadcastAllUsers() {
        ArrayList<User> allUsers = userDAO.getAllUsers();
        MessagePacket packet = MessagePacket.event(ACTION_FETCH) 
                .add("status", "success")
                .add("users", allUsers);

        SessionManager.getInstance().getOnlineConnections().forEach(conn -> {
            conn.sendPacket(packet);
        });
    
        LOGGER.log(System.Logger.Level.INFO, "Broadcast: Lista de usuarios actualizada enviada a todos.");
    }
}
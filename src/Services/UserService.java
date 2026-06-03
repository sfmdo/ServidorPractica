package Services;

import DAOlayer.UserDAO;
import Messages.MessagePacket;
import Models.User;
import Network.ClientConnection;
import Network.Protocol;
import java.util.ArrayList;

public class UserService {
    private static UserService instance;
    private UserDAO userDAO = new UserDAO();
    private static final System.Logger LOGGER = System.getLogger(AuthService.class.getName());
    
    private UserService() {}

    public static synchronized UserService getInstance() {
        if (instance == null) instance = new UserService();
        return instance;
    }

    public void handle(MessagePacket packet, ClientConnection client) {
        if (Protocol.FETCH_USERS.equals(packet.getAction())) {
            handleFetchUsers(packet, client);
        }
    }

    private void handleFetchUsers(MessagePacket packet, ClientConnection client) {
        // 1. Obtener todos los usuarios de la DB
        // Necesitas añadir este método a tu UserDAO (ver paso 2)
        ArrayList<User> allUsers = userDAO.getAllUsers();
        LOGGER.log(System.Logger.Level.INFO, "Mandando Todos los usuarios");
        // 2. Responder al cliente
        client.sendPacket(MessagePacket.response(Protocol.FETCH_USERS, packet.getToken())
                .add("status", "success")
                .add("users", allUsers));
    }
}
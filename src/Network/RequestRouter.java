package Network;

import Messages.MessagePacket;
import Services.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestRouter {
    private static final Map<String, RequestHandler> masterRoutes = new HashMap<>();

    private static final Set<String> PUBLIC_ACTIONS = Set.of("LOGIN", "REGISTER");

    static {
        mount(AuthService.getInstance().getRouter());
        mount(FriendService.getInstance().getRouter());
        mount(ChatGlobalService.getInstance().getRouter());
        mount(GroupService.getInstance().getRouter());
        mount(NotificationService.getInstance().getRouter());
        mount(UserService.getInstance().getRouter());
    }

    private static void mount(Router serviceRouter) {
        masterRoutes.putAll(serviceRouter.getAllRoutes());
    }

    public static void route(MessagePacket packet, ClientConnection client) {
        String action = packet.getAction();
        if (action == null) return;

        RequestHandler handler = masterRoutes.get(action);

        if (handler == null) {
            client.sendPacket(MessagePacket.response(action, packet.getToken())
                    .add("status", "error")
                    .add("reason", "Acción no reconocida por el servidor."));
            return;
        }

        if (!PUBLIC_ACTIONS.contains(action) && !"AUTHENTICATED".equals(client.getCurrentState())) {
            client.sendPacket(MessagePacket.response(action, packet.getToken())
                    .add("status", "error")
                    .add("reason", "Acceso denegado. Inicie sesión."));
            return;
        }

        handler.handle(packet, client);
    }
}
package Network;

import java.util.HashMap;
import java.util.Map;

public class Router {
    private final Map<String, RequestHandler> routes = new HashMap<>();

    public void add(String action, RequestHandler handler) {
        routes.put(action, handler);
    }

    public RequestHandler getHandler(String action) {
        return routes.get(action);
    }

    public Map<String, RequestHandler> getAllRoutes() {
        return routes;
    }
}
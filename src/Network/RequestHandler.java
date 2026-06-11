package Network;

import Messages.MessagePacket;
import Network.ClientConnection;

@FunctionalInterface
public interface RequestHandler {
    void handle(MessagePacket packet, ClientConnection client);
}
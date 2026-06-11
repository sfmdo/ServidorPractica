package Network;

import Messages.Comprimir;
import Messages.MessagePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class ClientConnection implements Runnable {
    private static final Logger LOGGER = System.getLogger(ClientConnection.class.getName());
    private static final Gson GSON = new GsonBuilder().create();
    
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private final BlockingQueue<MessagePacket> packetQueue = new LinkedBlockingQueue<>();
    private final Thread processorThread;
    private volatile boolean running = true;
    // -------------------------

    private String currentState = "CONNECTED";
    private String currentUserId = null;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        this.processorThread = new Thread(this::processQueue, "Processor-" + socket.getRemoteSocketAddress());
        this.processorThread.start();
    }

    @Override
    public void run() {
        try {
            String lineaRecibida;
            while (running && (lineaRecibida = reader.readLine()) != null) {
                enqueuePacket(lineaRecibida);
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Socket cerrado para usuario: {0}", currentUserId);
        } finally {
            closeConnection();
        }
    }

    private void enqueuePacket(String lineaRecibida) {
        try {
            byte[] dataData = Base64.getDecoder().decode(lineaRecibida);
            char[] tokensComprimidos = bytesToChars(dataData);
            String jsonDescomprimido = Comprimir.descomprimir(tokensComprimidos);
            MessagePacket packet = GSON.fromJson(jsonDescomprimido, MessagePacket.class);

            if (packet != null) {
                packetQueue.offer(packet);
            }
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Error al encolar paquete: {0}", e.getMessage());
        }
    }

    private void processQueue() {
        while (running) {
            try {
                MessagePacket packet = packetQueue.take();
                RequestRouter.route(packet, this);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.ERROR, "Error en el procesador de ruteo: {0}", e.getMessage());
            }
        }
    }
    
    private void closeConnection() {
        running = false;
        try {
            if (currentUserId != null) {
                cleanupUserSession();
            }
           
            processorThread.interrupt();
            
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Error al cerrar conexión: {0}", e.getMessage());
        }
    }

    private void cleanupUserSession() {
        SessionManager.getInstance().removeSession(currentUserId);
        Services.ChatGlobalService.getInstance().clearUserHistory(currentUserId);
        MessagePacket disconnectNotif = MessagePacket.event("GLOBAL_MSG")
            .add("subAction", "USER_DISCONNECTED")
            .add("from", currentUserId);
        SessionManager.getInstance().broadcast(disconnectNotif, currentUserId);
        new DAOlayer.UserDAO().updateOnlineStatus(Integer.parseInt(currentUserId), "OFFLINE");
        Services.UserService.getInstance().broadcastAllUsers();
    }

    public void sendPacket(MessagePacket packet) {
        try {
            String json = GSON.toJson(packet);
            char[] tokens = new Comprimir().compresion(json);
            byte[] bytesParaEnviar = charsToBytes(tokens);
            String stringBase64 = Base64.getEncoder().encodeToString(bytesParaEnviar);
            writer.println(stringBase64);
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Error enviando: {0}", e.getMessage());
        }
    }

    private byte[] charsToBytes(char[] chars) {
        ByteBuffer bb = ByteBuffer.allocate(chars.length * 2);
        for (char c : chars) bb.putChar(c);
        return bb.array();
    }

    private char[] bytesToChars(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        char[] chars = new char[bytes.length / 2];
        for (int i = 0; i < chars.length; i++) chars[i] = bb.getChar();
        return chars;
    }
    public Socket getSocket() { return socket; }
    public String getCurrentState() { return currentState; }
    public String getCurrentUserId() { return currentUserId; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public void setCurrentUserId(String currentUserId) { this.currentUserId = currentUserId; }
    public void setAuthenticated(String userId) {
        this.currentUserId = userId;
        this.currentState = "AUTHENTICATED";
    }
}
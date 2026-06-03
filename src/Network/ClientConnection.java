package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import Messages.Comprimir;
import Messages.MessagePacket;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 *
 * @author sfmdo
 */
public class ClientConnection implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson = new Gson();
    private Comprimir compresor = new Comprimir();
    private String currentState = "CONNECTED";
    private String currentUserId = null; 
    private static final Logger LOGGER = System.getLogger(ClientConnection.class.getName());

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String lineaRecibida;
            while ((lineaRecibida = reader.readLine()) != null) {
                byte[] dataData = Base64.getDecoder().decode(lineaRecibida);
                char[] tokensComprimidos = bytesToChars(dataData);

                String jsonDescomprimido = Comprimir.descomprimir(tokensComprimidos);

                MessagePacket packet = gson.fromJson(jsonDescomprimido, MessagePacket.class);

                // 4. Enviar al Router (Capa 2)

                LOGGER.log(Level.INFO, "Accion Recibida {0} de {1}", packet.getAction(),currentUserId);
                RequestRouter.route(packet, this);
            }
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Error en el protocolo de comunicación", e);
        } finally {
            closeConnection(); 
        }
    }
    
    private void closeConnection() {
        try {
            // Solo hacemos limpieza si el usuario llegó a estar logueado
            if (currentUserId != null) {
                
                
                // 1. Quitar de la lista de sesiones activas (RAM)
                SessionManager.getInstance().removeSession(currentUserId);
                // 2. Limpiar chats globales 1-a-1
                Services.ChatGlobalService.getInstance().clearUserHistory(currentUserId);
                MessagePacket disconnectNotif = MessagePacket.event(Protocol.GLOBAL_MSG)
                    .add("subAction", "USER_DISCONNECTED") // Acción interna
                    .add("from", currentUserId);
                
                SessionManager.getInstance().broadcast(disconnectNotif, currentUserId);
                // 3. Actualizar estado en la Base de Datos 
                new DAOlayer.UserDAO().updateOnlineStatus(Integer.parseInt(currentUserId), "OFFLINE");
                LOGGER.log(Level.INFO, "Limpieza completa para el usuario: {0}", currentUserId);
            }
            
            // 4. Cerrar recursos físicos
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
            LOGGER.log(Level.ERROR, "Error al cerrar la conexion", e);
        }
    }
    
    public void sendPacket(MessagePacket packet) {
        String json = gson.toJson(packet);
        char[] tokens = compresor.compresion(json);
        byte[] bytesParaEnviar = charsToBytes(tokens);
        String stringBase64 = Base64.getEncoder().encodeToString(bytesParaEnviar);

        writer.println(stringBase64);
    }

    // Funciones auxiliares para convertir char[] (16bit) a byte[] (8bit)
    private byte[] charsToBytes(char[] chars) {
        byte[] b = new byte[chars.length * 2];
        for (int i = 0; i < chars.length; i++) {
            b[i * 2] = (byte) (chars[i] >> 8);
            b[i * 2 + 1] = (byte) (chars[i]);
        }
        return b;
    }

    private char[] bytesToChars(byte[] bytes) {
        char[] c = new char[bytes.length / 2];
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) ((bytes[i * 2] << 8) | (bytes[i * 2 + 1] & 0xFF));
        }
        return c;
    }
    
    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setAuthenticated(String userId) {
        this.currentUserId = userId;
        this.currentState = "AUTHENTICATED";
    }

    public Socket getSocket() {
        return socket;
    }
    
    
}

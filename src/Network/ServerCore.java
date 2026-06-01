/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Network;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author sfmdo
 */
public class ServerCore {
    private static final int PORT = 6767;
    private ServerSocket serverSocket;
    private ExecutorService threadPool; 
    private boolean isRunning;
    private static final System.Logger LOGGER = System.getLogger(ServerCore.class.getName());

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool(); 
            isRunning = true;
            
            System.out.println("Server started on port " + PORT);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.log(Level.INFO, "Usuario se conectó desde la IP {1} usando el puerto {2}", 
                new Object[]{clientSocket.getInetAddress(), clientSocket.getPort() });
            }
        } catch (IOException e) {
            if (isRunning) LOGGER.log(Level.INFO, "Error en el ServerCore", e);
        }
    }

    public void stop() {
        isRunning = false;
        LOGGER.log(Level.INFO, "Apagando Servidor");
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Error en el ServerCore", e);
        }
    }
}

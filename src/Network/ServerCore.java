/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author sfmdo
 */
public class ServerCore {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private ExecutorService threadPool; 
    private boolean isRunning;

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool(); 
            isRunning = true;
            
            System.out.println("Server started on port " + PORT);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                
                threadPool.execute(new ClientConnection(clientSocket));
            }
        } catch (IOException e) {
            if (isRunning) e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown(); 
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package servidorpractica;

import DAOlayer.DatabaseConnection;
import Network.ServerCore;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 *
 * @author sfmdo
 */
public class ServidorPractica {
    private static final Logger LOGGER = System.getLogger("MainServer");
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Iniciando sistema de mensajería multihilo...");
        try {
            // 1. Inicializar la conexión a la base de datos
            // Esto asegura que si la DB está caída, el servidor no arranque por gusto
            DatabaseConnection db = new DatabaseConnection();
            if (db.getDbpointer() != null) {
                LOGGER.log(Level.INFO, "Conexión a MySQL establecida correctamente.");
            } else {
                LOGGER.log(Level.ERROR, "No se pudo establecer conexión con la base de datos. Abortando...");
                return;
            }

            // 2. Instanciar y arrancar el núcleo del servidor
            ServerCore server = new ServerCore();
            
            // Añadir un "Hook" de apagado para cerrar recursos al detener el programa (Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.log(Level.INFO, "Apagando servidor de forma segura...");
                server.stop();
            }));

            // 3. Iniciar el bucle de escucha de sockets
            // Este método se queda bloqueado aceptando clientes
            server.start();

        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Error fatal durante el arranque: " + e.getMessage(), e);
            System.exit(1);
        }
        
    }
    
}

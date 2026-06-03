package DAOlayer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    private static Connection dbPointer;
    // 1. Definir el Logger para la conexión
    private static final Logger LOGGER = System.getLogger(DatabaseConnection.class.getName());
    
    public DatabaseConnection() {

        
        try {
             if (dbPointer == null || dbPointer.isClosed()) {
                String dir = "jdbc:mysql://localhost:3306/chat";
                String Usr = "root";
                String Cont = "";
                
                Class.forName("com.mysql.cj.jdbc.Driver");
                dbPointer = DriverManager.getConnection(dir, Usr, Cont);
                
                LOGGER.log(Level.INFO, "Conexión física establecida con MySQL.");
            }
            
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.ERROR, "Error: No se encontró el driver de MySQL (Connector/J). Asegúrate de añadir el JAR al proyecto.");
            LOGGER.log(Level.DEBUG, "Detalle técnico: {0}", e.getMessage());
            
        } catch (SQLException e) {
            LOGGER.log(Level.ERROR, "Error de SQL: No se pudo conectar a la base de datos en {0}. Verifica que MySQL esté encendido y el usuario/password sean correctos.");
            LOGGER.log(Level.DEBUG, "SQL State: {0}, Error Code: {1}", e.getSQLState(), e.getErrorCode());
        }
    }

    public Connection getDbpointer() {
        return dbPointer;
    }

    public void setDbpointer(Connection dbpointer) {
        this.dbPointer = dbpointer;
    }
}
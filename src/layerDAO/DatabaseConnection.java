
package layerDAO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    private Connection dbpointer;
    
    public DatabaseConnection(){
        try{ //Recuerden, sin el trycatch no funciona este rollo
            
          Class.forName("com.mysql.cj.jdbc.Driver"); //Esta clase me permite tomar un objeto externo y montarlo en la maquina virtual de Java (se instancia)
          dbpointer =  DriverManager.getConnection("jdbc:mysql://localhost:3306/ejemplococo","root",""); //Este sujeto devuelve el apuntador
        }catch(ClassNotFoundException pepe1){
            System.out.println("Error"+pepe1.getMessage());
        }catch(SQLException pepe){
            System.out.println("Error: "+pepe.getMessage());
        }
    }

    public Connection getDbpointer() {
        return dbpointer;
    }

    public void setDbpointer(Connection dbpointer) {
        this.dbpointer = dbpointer;
    }
    
    
}

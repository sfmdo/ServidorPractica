
package layerDAO;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;

import models.User;
public class UserDAO extends DatabaseConnection{ //Los DAO tienen que heredar a la conexión muchachos
    
    public UserDAO(){
        super();
    }
    public void addUser(User user){ //Los parametros son el modelo de la tabla
        
        try{
           PreparedStatement ps;
           ps = getDbpointer().prepareStatement("INSERT INTO users(username,password,status,created_at) "
                   + "values (?,?,?,?)");
           ps.setString(1, user.getUsername());
           ps.setString(2,user.getPassword());
           ps.setString(3, user.getStatus());
           ps.setString(4, user.getCreated_at());
           
           ps.executeUpdate(); 
           
           
        }catch(SQLException e){
            System.out.println("Error al insterar: " + e.getMessage());
        }   
    }
    
    
    //Get a todos los usuarios, por si sirve, nose
    public ArrayList<User> getAll(User ej){
        try{
            PreparedStatement ps;
            ResultSet rs;
            ps = getDbpointer().prepareStatement("SELECT Id, username, status FROM users");
            rs = ps.executeQuery(); 
            ArrayList<User> datos = new ArrayList<>();

            while(rs.next()){ 
                datos.add(new User(rs.getInt(1),rs.getString(2),rs.getString(3)));
            }
            return datos;
            
           
        }catch(SQLException e){
            System.out.println("Error: "+e.getMessage());
            return null; //va a devolver nulo si no jala.
        }
    }
}


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
    public void addUser(User user) {
        try {
            PreparedStatement ps = getDbpointer().prepareStatement(
                    "INSERT INTO users(username,password_hash,status,created_at) VALUES (?,?,?,?)" );
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getStatus());
            ps.setString(4, user.getCreated_at());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error al insertar usuario: " + e.getMessage());
        }
    }

    
    public User validateCredentials(String username, String password) {
        try {
            PreparedStatement ps = getDbpointer().prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password_hash = ?" );
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password_hash"));
                user.setStatus(rs.getString("status"));
                user.setCreated_at(rs.getString("created_at"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Error al validar usuario: " + e.getMessage());
        }
        return null; //Si no jalo, devuelve null :v
    }
    
    
 
}

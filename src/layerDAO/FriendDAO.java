
package layerDAO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import models.User;

public class FriendDAO extends DatabaseConnection {

    public FriendDAO() {
        super();
    }

    public ArrayList<User> getFriends(User user) { //El seven; ete six
        try {
            PreparedStatement ps = getDbpointer().prepareStatement("SELECT u.id, u.username, u.status " +
                    "FROM users u " +
                    "INNER JOIN friendships f " +
                    "ON (u.id = f.user_id1 OR u.id = f.user_id2) " +
                    "WHERE (f.user_id1 = ? OR f.user_id2 = ?) " +
                    "AND u.id <> ? " +
                    "AND f.status = 'ACCEPTED'");
            ps.setInt(1, user.getId());
            ps.setInt(2, user.getId());
            ps.setInt(3, user.getId());
            ResultSet rs = ps.executeQuery();
            ArrayList<User> amigos = new ArrayList<>();
            while (rs.next()) {
                User amigo = new User();
                amigo.setId(rs.getInt("id"));
                amigo.setUsername(rs.getString("username"));
                amigo.setStatus(rs.getString("status"));
                amigos.add(amigo);
            }
            return amigos;

        } catch (SQLException e) {
            System.out.println("Error al obtener amigos: " + e.getMessage());
            return null; 
        }
    }
}

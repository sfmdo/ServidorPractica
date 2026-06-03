/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAOlayer;

import Models.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class FriendDAO extends DatabaseConnection {

    public FriendDAO() { super(); }

    // 1. Enviar solicitud de amistad (PENDING)
    public boolean sendFriendRequest(int user1, int user2) {
        // Ordenamos los IDs para evitar duplicados (ej: 1-2 y 2-1)
        int id1 = Math.min(user1, user2);
        int id2 = Math.max(user1, user2);
        
        String sql = "INSERT INTO friendships (user_id1, user_id2, status) VALUES (?, ?, 'PENDING')";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("ERROR SQL en FriendDAO: " + e.getMessage()); return false; }
    }

    // 2. Aceptar solicitud (Pasar a ACCEPTED)
    public boolean acceptFriendRequest(int friendshipId) {
        String sql = "UPDATE friendships SET status = 'ACCEPTED' WHERE id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, friendshipId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("ERROR SQL en FriendDAO: " + e.getMessage()); return false; }
    }

    // 3. Obtener el ID de la relación entre dos usuarios (Vital para los mensajes)
    public int getFriendshipId(int user1, int user2) {
        int id1 = Math.min(user1, user2);
        int id2 = Math.max(user1, user2);
        String sql = "SELECT id FROM friendships WHERE user_id1 = ? AND user_id2 = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1; // No existe relación
    }

    // 4. Tu método getFriends
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

package DAOlayer;

import Models.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO extends DatabaseConnection {

    public UserDAO() {
        super();
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar usuario: " + e.getMessage());
            return false;
        }
    }

    public User validateCredentials(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setStatus(rs.getString("status"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error al validar usuario: " + e.getMessage());
        }
        return null;
    }

    public void updateOnlineStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar status: " + e.getMessage());
        }
    }
}
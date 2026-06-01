package DAOlayer;

import Models.Notifications;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

public class NotificationDAO extends DatabaseConnection {

    public NotificationDAO() {
        super();
    }

     // Crea una nueva notificación en la base de datos.
     // Se usa cuando alguien envía una solicitud de amistad o invitación a grupo.

    public boolean create(Notifications n) {
        String sql = "INSERT INTO notifications (target_user_id, from_user_id, type, related_id, content, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, n.getTarget_user_id());
            ps.setInt(2, n.getFrom_user_id());
            ps.setString(3, n.getType()); 
            
            // related_id puede ser nulo en la DB
            if (n.getRelated_id() > 0) {
                ps.setInt(4, n.getRelated_id());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            
            ps.setString(5, n.getContent());
            ps.setString(6, n.getStatus() != null ? n.getStatus() : "PENDING");

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al crear notificación: " + e.getMessage());
            return false;
        }
    }


     // Obtiene todas las notificaciones pendientes para un usuario.
     // Ideal para llamar justo después del Login.

    public ArrayList<Notifications> getPendingByUserId(int userId) {
        ArrayList<Notifications> lista = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE target_user_id = ? AND status = 'PENDING' ORDER BY created_at DESC";
        
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener notificaciones: " + e.getMessage());
        }
        return lista;
    }

    
    // Actualiza el estado de una notificación (ej. de PENDING a READ).
     
    public boolean updateStatus(int notificationId, String newStatus) {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar notificación: " + e.getMessage());
            return false;
        }
    }

    
     // Elimina notificaciones relacionadas a una acción que ya no es válida.
     // Ejemplo: Si alguien cancela una solicitud de amistad antes de que la acepten.
     
    public boolean deleteByRelatedId(int relatedId, String type) {
        String sql = "DELETE FROM notifications WHERE related_id = ? AND type = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, relatedId);
            ps.setString(2, type);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar notificación: " + e.getMessage());
            return false;
        }
    }

    // Método auxiliar para convertir un ResultSet en un objeto Notifications.
     
    private Notifications mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notifications n = new Notifications();
        n.setTarget_user_id(rs.getInt("target_user_id"));
        n.setFrom_user_id(rs.getInt("from_user_id"));
        n.setType(rs.getString("type"));
        n.setRelated_id(rs.getInt("related_id"));
        n.setContent(rs.getString("content"));
        n.setStatus(rs.getString("status"));
        return n;
    }
}

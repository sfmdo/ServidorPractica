package DAOlayer;

import Models.GroupMessages;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class GroupMessageDAO extends DatabaseConnection {

    public GroupMessageDAO() {
        super();
    }

     // Guarda un nuevo mensaje de grupo en la base de datos.
     
    public boolean addMessage(GroupMessages msg) {
        String sql = "INSERT INTO group_messages (group_id, sender_id, message) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, msg.getGroupId());
            ps.setInt(2, msg.getSenderId());
            ps.setString(3, msg.getMessage());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar mensaje de grupo: " + e.getMessage());
            return false;
        }
    }

    //Recupera todo el historial de mensajes de un grupo específico.
    //Los ordena por fecha para que aparezcan en orden cronológico.
    
    public ArrayList<GroupMessages> getMessagesByGroupId(int groupId) {
        ArrayList<GroupMessages> historial = new ArrayList<>();
        String sql = "SELECT * FROM group_messages WHERE group_id = ? ORDER BY sent_at ASC";
        
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                GroupMessages m = new GroupMessages();
                m.setId(rs.getInt("id"));
                m.setGroupId(rs.getInt("group_id"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setMessage(rs.getString("message"));
                m.setSentAt(rs.getString("sent_at")); // Usando tu setter con el typo 'SenAt'
                historial.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar historial de grupo: " + e.getMessage());
        }
        return historial;
    }

    // Obtiene los IDs de todos los miembros de un grupo.
    // Es vital para que el ChatService sepa a quiénes reenviarles el mensaje.
     
    public ArrayList<Integer> getMemberIds(int groupId) {
        ArrayList<Integer> miembros = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
        
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                miembros.add(rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener miembros del grupo: " + e.getMessage());
        }
        return miembros;
    }
}
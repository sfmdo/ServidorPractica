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
                m.setSentAt(rs.getString("sent_at")); 
                historial.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar historial de grupo: " + e.getMessage());
        }
        return historial;
    }

     
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
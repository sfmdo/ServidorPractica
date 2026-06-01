package DAOlayer;

import Models.PrivateMessages;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MessageDAO extends DatabaseConnection {
    public MessageDAO() { super(); }
    
    // Guardar mensaje
    public void saveMessage(PrivateMessages message) {
        try {
            PreparedStatement ps = getDbpointer().prepareStatement(
                "INSERT INTO private_messages(friendship_id, sender_id, message) VALUES (?,?,?)" );
            ps.setInt(1, message.getFriendshipId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getMessage());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al guardar mensaje: " + e.getMessage());
        }
    }

    // Obtener historial de chat entre dos amigos
    public ArrayList<PrivateMessages> getChatHistory(int friendshipId) {
        ArrayList<PrivateMessages> msgs = new ArrayList<>();
        String sql = "SELECT * FROM private_messages WHERE friendship_id = ? ORDER BY sent_at ASC";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, friendshipId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PrivateMessages m = new PrivateMessages();
                m.setFriendshipId(rs.getInt("friendship_id"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setMessage(rs.getString("message"));
                m.setSentAt(rs.getString("sent_at"));
                msgs.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return msgs;
    }
}
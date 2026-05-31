
package layerDAO;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import models.PrivateMessages;

public class MessageDAO extends DatabaseConnection{
    public MessageDAO(){
        super();
    }
    
    public void saveMessage(PrivateMessages message) {
        try {
            PreparedStatement ps = getDbpointer().prepareStatement(
                    "INSERT INTO private_messages(friendship_id,sender_id,message,sent_at) VALUES (?,?,?,?)" );
            ps.setInt(1, message.getFriendshipId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getMessage());
            ps.setString(4, message.getSentAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al guardar mensaje: " + e.getMessage());
        }
    }
    
}

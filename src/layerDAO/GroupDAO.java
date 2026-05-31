package layerDAO;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import models.Group;

public class GroupDAO extends DatabaseConnection {

    public GroupDAO() {
        super();
    }
    public void createGroupEntities(Group group) {
        try {
            PreparedStatement ps = getDbpointer().prepareStatement(
                    "INSERT INTO chat_groups(group_name, created_at) VALUES (?,?)" );
            ps.setString(1, group.getGroupName());
            ps.setString(2, group.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al crear grupo: " + e.getMessage());
        }
    }
}

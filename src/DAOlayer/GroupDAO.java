/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAOlayer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;    // <--- ESTE ES EL QUE FALTA
import java.sql.Statement;

import Models.Group;

public class GroupDAO extends DatabaseConnection {

    public GroupDAO() {
        super();
    }
    public int createGroupEntities(Group group, int creatorId) { 
        String sql = "INSERT INTO chat_groups(group_name, creator_id) VALUES (?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getGroupName());
            ps.setInt(2, creatorId);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error al crear grupo: " + e.getMessage());
        }
        return -1;
    }
}

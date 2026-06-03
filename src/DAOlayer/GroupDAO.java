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
import java.util.ArrayList;

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
    
    public ArrayList<Group> getAllGroups() {
        ArrayList<Group> lista = new ArrayList<>();
        String sql = "SELECT * FROM chat_groups";
    
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
        
            while (rs.next()) {
                Group g = new Group();
                g.setId(rs.getInt("id"));
                g.setGroupName(rs.getString("group_name"));
                // g.setCreatedAt(rs.getString("created_at"));
                lista.add(g);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los grupos: " + e.getMessage());
        }
        return lista;
    }
}

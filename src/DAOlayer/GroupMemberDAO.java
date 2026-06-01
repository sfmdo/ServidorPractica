package DAOlayer;

import Models.GroupMembers;
import Models.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class GroupMemberDAO extends DatabaseConnection {

    public GroupMemberDAO() {
        super();
    }

    //Agrega un usuario a un grupo.
    //Se usa al crear un grupo o al aceptar una invitación.
    
    public boolean addMember(int groupId, int userId) {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al agregar miembro al grupo: " + e.getMessage());
            return false;
        }
    }

 
    //Elimina a un usuario de un grupo.
    //Se usa para la acción GROUP_LEAVE.
    //Nota: El Trigger borrará el grupo si este era el último miembro.
    
    public boolean removeMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar miembro del grupo: " + e.getMessage());
            return false;
        }
    }

    //Verifica si un usuario pertenece a un grupo específico.
    // Seguridad: Úsalo antes de permitir que alguien envíe un mensaje a un grupo.
     
    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    //Obtiene la lista de usuarios (con nombre y status) que pertenecen a un grupo.
     
    public ArrayList<User> getGroupMembersInfo(int groupId) {
        ArrayList<User> miembros = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.status FROM users u " +
                     "INNER JOIN group_members gm ON u.id = gm.user_id " +
                     "WHERE gm.group_id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setStatus(rs.getString("status"));
                miembros.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener info de miembros: " + e.getMessage());
        }
        return miembros;
    }
}
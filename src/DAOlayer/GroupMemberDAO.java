package DAOlayer;

import Models.Group;
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

    //Cuenta el total de personas relacionadas al grupo (Invitados + Aceptados).
    //Se usa para la regla de "Mínimo 3 para chatear".
 
    public int getTotalMemberCount(int groupId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
}


    //Verifica si un usuario específico ya aceptó la invitación.
    //Se usa para la regla de "Solo miembros aceptados ven el historial".

    public boolean hasAccepted(int groupId, int userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ? AND status = 'ACCEPTED'";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }
    
    //Modifica el addMember para que el creador entre como ACCEPTED y los invitados como PENDING.
    public boolean addMemberWithStatus(int groupId, int userId, String status) {
        String sql = "INSERT INTO group_members (group_id, user_id, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
    
    public boolean updateMemberStatus(int groupId, int userId, String newStatus) {
        String sql = "UPDATE group_members SET status = ? WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, groupId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar estado del miembro: " + e.getMessage());
            return false;
        }
    }
    
    public ArrayList<Group> getUserGroups(int userId) {
        ArrayList<Group> lista = new ArrayList<>();
        // Unimos la tabla de grupos con la de miembros para obtener los nombres
        String sql = "SELECT g.id, g.group_name, g.creator_id " +
                     "FROM chat_groups g " +
                     "INNER JOIN group_members gm ON g.id = gm.group_id " +
                     "WHERE gm.user_id = ?";
    
        try (PreparedStatement ps = getDbpointer().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
        
            while (rs.next()) {
                Group g = new Group();
                g.setId(rs.getInt("id"));
                g.setGroupName(rs.getString("group_name"));
                lista.add(g);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener grupos del usuario: " + e.getMessage());
        }
        return lista;
    }
}
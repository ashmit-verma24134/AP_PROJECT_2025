package dao;
import models.User;

import java.sql.*;
import java.time.LocalDateTime;

import edu.univ.erp.util.DBConnection;
public class UserDAOImpl implements UserDAO {
    @Override
    public User findByUsername(String username) throws Exception{
        String sql = "SELECT * FROM auth_db.users WHERE username = ?";
        try (Connection c =DBConnection.getConnection();
             PreparedStatement ps =c.prepareStatement(sql)){
            ps.setString(1,username);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()) return null;
                User u=new User();
                 u.setUserId(rs.getLong("user_id"));
                u.setUsername(rs.getString("username"));
                 u.setPassHash(rs.getString("pass_hash"));
                  u.setRoleId(rs.getInt("role_id"));
                u.setStatus(rs.getString("status"));
                Timestamp lockedTs=rs.getTimestamp("locked_until");
                  if (lockedTs!=null) u.setLockedUntil(lockedTs.toLocalDateTime());
                 Timestamp lastLogin=rs.getTimestamp("last_login");
                  if (lastLogin!=null) u.setLastLogin(lastLogin.toLocalDateTime());
                Timestamp createdAt=rs.getTimestamp("created_at");
                  if (createdAt!=null) u.setCreatedAt(createdAt.toLocalDateTime());
                    Timestamp updatedAt=rs.getTimestamp("updated_at");
                  if (updatedAt!=null) u.setUpdatedAt(updatedAt.toLocalDateTime());
                  u.setFailedAttempts(rs.getInt("failed_attempts"));
                return u;
            }
      }
}

    @Override
    public long createUser(User u) throws Exception{
        String sql="INSERT INTO auth_db.users (username, pass_hash, role_id) VALUES (?, ?, ?)";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,u.getUsername());
            ps.setString(2,u.getPassHash());
            ps.setInt(3,u.getRoleId());
            ps.executeUpdate();
            try (ResultSet rs=ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                return -1;
              }
          }
      }
 }

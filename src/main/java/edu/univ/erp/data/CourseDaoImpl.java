package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;
import java.sql.*;
import java.util.*;

public class CourseDaoImpl implements CourseDao {

    @Override
    public long createCourse(String code, String title, int credits) throws Exception {
        String sql = "INSERT INTO courses (code, title, credits) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new Exception("Failed to insert course");
    }

    @Override
    public void updateCourse(long id, String code, String title, int credits) throws Exception {
        String sql = "UPDATE courses SET code=?, title=?, credits=? WHERE id=?";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Map<String,Object>> listCourses() throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        String sql = "SELECT id, code, title, credits FROM courses ORDER BY code";

        try (Connection con = DBConnection.getErpConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("code", rs.getString("code"));
                row.put("title", rs.getString("title"));
                row.put("credits", rs.getInt("credits"));
                result.add(row);
            }
        }
        return result;
    }
}

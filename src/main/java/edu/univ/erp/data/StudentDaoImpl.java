package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDaoImpl implements StudentDao {
    private final Connection conn;
    public StudentDaoImpl(Connection conn) { this.conn = conn; }

    @Override
    public Long getStudentIdByRoll(String rollNo) throws Exception {
        String sql = "SELECT student_id FROM students WHERE roll_no = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("student_id");
                } else {
                    return null;
                }
            }
        }
    }
}

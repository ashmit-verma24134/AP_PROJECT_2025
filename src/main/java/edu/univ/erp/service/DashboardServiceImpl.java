package edu.univ.erp.service;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class DashboardServiceImpl implements DashboardService {

    @Override
    public Map<String, Object> loadOverview(String studentId) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            return dao.getStudentOverview(studentId);
        }
    }

    @Override
    public List<Map<String, Object>> loadUpcomingSchedule(String studentId, int limit) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            return dao.getUpcomingSchedule(studentId, limit);
        }
    }

    @Override
    public List<Map<String, Object>> loadRecentGrades(String studentId, int limit) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            return dao.getRecentGrades(studentId, limit);
        }
    }

    @Override
    public String loadDisplayName(String studentId) throws Exception {
        if (studentId == null) return "—";

        try (Connection conn = DBConnection.getErpConnection()) {
            String sql = "SELECT s.full_name, s.roll_no, u.username " +
                    "FROM students s LEFT JOIN auth_db.users u ON s.user_id = u.user_id " +
                    "WHERE s.student_id = ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uname = rs.getString("username");
                        String full = rs.getString("full_name");
                        String roll = rs.getString("roll_no");

                        if (uname != null && !uname.isEmpty()) return uname;
                        if (full != null && !full.isEmpty()) return full;
                        if (roll != null && !roll.isEmpty()) return roll;
                    }
                }
            }
        }
        return studentId; // fallback
    }
}

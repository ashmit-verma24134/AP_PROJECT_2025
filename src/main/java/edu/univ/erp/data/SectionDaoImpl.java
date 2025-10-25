package edu.univ.erp.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SectionDaoImpl implements SectionDao {
    private final Connection conn;
    public SectionDaoImpl(Connection conn) { this.conn = conn; }

    @Override
    public List<SectionRow> searchOpenSections(String query) throws SQLException {
        String sql = """
            SELECT s.section_id, c.code, c.title, c.credits,
                   IFNULL(i.full_name, 'TBA') AS instructor,
                   s.capacity,
                   (s.capacity - IFNULL((SELECT COUNT(*) 
                       FROM enrollments e 
                       WHERE e.section_id = s.section_id AND e.status='ENROLLED'),0)) AS seats_left,
                   s.semester, s.year, s.day_time
            FROM sections s
            JOIN courses c ON s.course_id = c.course_id
            LEFT JOIN instructors i ON s.instructor_id = i.instructor_id
            WHERE (c.code LIKE ? OR c.title LIKE ?)
            ORDER BY c.code
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + (query == null ? "" : query) + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                List<SectionRow> list = new ArrayList<>();
                while (rs.next()) {
                    SectionRow row = new SectionRow();
                    row.sectionId = rs.getLong("section_id");
                    row.courseCode = rs.getString("code");
                    row.title = rs.getString("title");
                    row.credits = rs.getInt("credits");
                    row.instructorName = rs.getString("instructor");
                    row.capacity = rs.getInt("capacity");
                    row.seatsLeft = rs.getInt("seats_left");
                    row.semester = rs.getString("semester");
                    row.year = rs.getInt("year");
                    row.dayTime = rs.getString("day_time");
                    list.add(row);
                }
                return list;
            }
        }
    }

    @Override
    public boolean isStudentEnrolled(long studentId, long sectionId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE student_id=? AND section_id=? AND status='ENROLLED' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int getSeatsLeft(long sectionId) throws SQLException {
        String sql = """
            SELECT (capacity - IFNULL((SELECT COUNT(*) 
                FROM enrollments e 
                WHERE e.section_id=? AND e.status='ENROLLED'),0)) AS seats_left
            FROM sections WHERE section_id=?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            ps.setLong(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("seats_left") : 0;
            }
        }
    }

    @Override
    public boolean registerStudentInSection(long studentId, long sectionId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'ENROLLED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean isMaintenanceOn() throws SQLException {
        SettingsDao settingsDao = new SettingsDaoImpl(conn);
        return settingsDao.isMaintenanceOn();
    }

    @Override
    public boolean isDropDeadlineOver(long sectionId) throws SQLException {
        String sql = "SELECT drop_deadline FROM sections WHERE section_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date deadline = rs.getDate("drop_deadline");
                    return deadline != null && deadline.before(new java.util.Date());
                }
                return false;
            }
        }
    }

}

package edu.univ.erp.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SectionDaoImpl - concrete DAO for sections.
 * Uses an injected Connection (provided in ctor) so callers control transactions.
 *
 * Note: SQL here has been made tolerant to different DB layouts by avoiding references
 * to non-standard columns like s.cterm or s.schedule. We read the canonical columns:
 * - semester (may be varchar), term (varchar), year (int), day_time (preferred)
 */
public class SectionDaoImpl implements SectionDao {
    private final Connection conn;

    public SectionDaoImpl(Connection conn) {
        this.conn = conn;
    }

    // -------------------- searchOpenSections --------------------
    @Override
    public List<SectionRow> searchOpenSections(String query) throws SQLException {
        String sql = """
            SELECT s.section_id,
                   s.course_id,
                   c.code,
                   c.title,
                   c.credits,
                   IFNULL(i.full_name, 'TBA') AS instructor,
                   s.capacity,
                   (s.capacity - IFNULL((SELECT COUNT(*)
                       FROM enrollments e
                       WHERE e.section_id = s.section_id AND e.status='ENROLLED'),0)) AS seats_left,
                   -- prefer explicit semester/term; if semester NULL use term plus year (safe concat)
                   COALESCE(s.semester, s.term, CONCAT(IFNULL(s.term,''), ' ', IFNULL(CAST(s.year AS CHAR),''))) AS semester,
                   s.year,
                   s.day_time AS day_time,
                   s.room,
                   NULL AS section_no
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
                    SectionRow row = mapRow(rs);
                    row.instructorName = rs.getString("instructor");
                    list.add(row);
                }
                return list;
            }
        }
    }

    // -------------------- getSectionsByInstructor --------------------
    @Override
    public List<SectionRow> getSectionsByInstructor(long instructorId, String term) throws SQLException {
        String sql = """
            SELECT s.section_id,
                   s.course_id,
                   c.code,
                   c.title,
                   c.credits,
                   s.instructor_id,
                   IFNULL(i.full_name, 'TBA') AS instructor,
                   s.capacity,
                   (s.capacity - IFNULL((SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id AND e.status='ENROLLED'),0)) AS seats_left,
                   -- tolerant semester/term
                   COALESCE(s.semester, s.term, CONCAT(IFNULL(s.term,''), ' ', IFNULL(CAST(s.year AS CHAR),''))) AS semester,
                   s.year,
                   s.day_time AS day_time,
                   s.room,
                   s.drop_deadline,
                   s.created_at,
                   s.updated_at,
                   NULL AS section_no
            FROM sections s
            JOIN courses c ON s.course_id = c.course_id
            LEFT JOIN instructors i ON s.instructor_id = i.instructor_id
            WHERE (? IS NULL OR s.instructor_id = ?)
              AND (? IS NULL OR CONCAT(COALESCE(s.semester, s.term, ''), ' ', COALESCE(CAST(s.year AS CHAR), '')) = ?)
            ORDER BY s.year DESC, COALESCE(s.semester, s.term, '') DESC, c.code
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // params 1,2 => instructor filter
            if (instructorId <= 0) {
                ps.setNull(1, Types.BIGINT);
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(1, instructorId);
                ps.setLong(2, instructorId);
            }

            // params 3,4 => term filter (like "Spring 2025")
            if (term == null || term.trim().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(3, term.trim());
                ps.setString(4, term.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<SectionRow> list = new ArrayList<>();
                while (rs.next()) {
                    SectionRow r = mapRow(rs);
                    r.instructorName = rs.getString("instructor");
                    r.instructorId = rs.getLong("instructor_id");
                    // include timestamps if present
                    try {
                        r.dropDeadline = rs.getDate("drop_deadline");
                    } catch (SQLException ignored) {}
                    try {
                        r.createdAt = rs.getTimestamp("created_at");
                        r.updatedAt = rs.getTimestamp("updated_at");
                    } catch (SQLException ignored) {}
                    list.add(r);
                }
                return list;
            }
        }
    }

    // -------------------- isStudentEnrolled --------------------
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

    // -------------------- getSeatsLeft --------------------
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

    // -------------------- registerStudentInSection --------------------
    @Override
    public boolean registerStudentInSection(long studentId, long sectionId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'ENROLLED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------- isMaintenanceOn --------------------
    @Override
    public boolean isMaintenanceOn() throws SQLException {
        SettingsDao settingsDao = new SettingsDaoImpl(conn);
        return settingsDao.isMaintenanceOn();
    }

    // -------------------- isDropDeadlineOver --------------------
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

    // -------------------- helper: mapRow --------------------
    private SectionRow mapRow(ResultSet rs) throws SQLException {
        SectionRow row = new SectionRow();
        row.sectionId = rs.getLong("section_id");
        row.courseId = rs.getLong("course_id");

        // try both possible column names used above (code OR course_code)
        try {
            row.courseCode = rs.getString("code");
        } catch (SQLException e) {
            row.courseCode = safeGetString(rs, "course_code");
        }

        // try both possible title column names
        try {
            row.title = rs.getString("title");
        } catch (SQLException e) {
            row.title = safeGetString(rs, "course_title");
        }

        row.credits = safeGetInt(rs, "credits");
        row.capacity = safeGetInt(rs, "capacity");
        row.seatsLeft = safeGetInt(rs, "seats_left");
        row.enrolled = row.capacity - row.seatsLeft;
        row.semester = safeGetString(rs, "semester");
        row.year = safeGetInt(rs, "year");
        row.dayTime = safeGetString(rs, "day_time");
        row.room = safeGetString(rs, "room");
        row.sectionNo = safeGetString(rs, "section_no");
        return row;
    }

    // small helpers to avoid repeating try/catch
    private static String safeGetString(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (SQLException e) {
            return null;
        }
    }
    private static int safeGetInt(ResultSet rs, String col) {
        try {
            return rs.getInt(col);
        } catch (SQLException e) {
            return 0;
        }
    }
}

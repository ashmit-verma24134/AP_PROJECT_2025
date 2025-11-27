package edu.univ.erp.data;

import edu.univ.erp.model.Section;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Concrete JDBC implementation of SectionDao.
 * Implements ALL methods declared in SectionDao.
 */
public class SectionDaoImpl implements SectionDao {

    private final Connection conn;

    public SectionDaoImpl(Connection conn) {
        this.conn = conn;
    }

    // --------------------------------------------------------
    //  SEARCH OPEN SECTIONS
    // --------------------------------------------------------
    @Override
    public List<SectionRow> searchOpenSections(String query) throws SQLException {
        List<SectionRow> list = new ArrayList<>();

        String sql = """
                SELECT s.section_id, s.course_id, c.code, c.title, c.credits,
                       s.instructor_id,
                       IFNULL(i.full_name, 'TBA') AS instructor,
                       s.capacity,
                       (s.capacity - IFNULL(
                            (SELECT COUNT(*) FROM enrollments e 
                             WHERE e.section_id = s.section_id AND e.status='ENROLLED')
                       ,0)) AS seats_left,
                       COALESCE(s.semester, '') AS semester
                FROM sections s
                JOIN courses c ON c.course_id = s.course_id
                LEFT JOIN instructors i ON i.instructor_id = s.instructor_id
                WHERE c.code LIKE ? OR c.title LIKE ?
                ORDER BY c.code
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SectionRow row = new SectionRow(
                            rs.getLong("section_id"),
                            rs.getLong("course_id"),
                            rs.getString("code"),
                            rs.getString("title"),
                            rs.getDouble("credits"),
                            rs.getLong("instructor_id"),
                            rs.getString("instructor"),
                            rs.getInt("capacity"),
                            rs.getInt("seats_left"),
                            rs.getString("semester")
                    );
                    list.add(row);
                }
            }
        }
        return list;
    }

    // --------------------------------------------------------
    //  GET SECTIONS BY INSTRUCTOR
    // --------------------------------------------------------
    @Override
    public List<SectionRow> getSectionsByInstructor(long instructorId, String term) throws SQLException {
        List<SectionRow> list = new ArrayList<>();

        String sql = """
                SELECT s.section_id, s.course_id, c.code, c.title, c.credits,
                       s.instructor_id,
                       IFNULL(i.full_name, 'TBA') AS instructor,
                       s.capacity,
                       (s.capacity - IFNULL(
                            (SELECT COUNT(*) FROM enrollments e 
                             WHERE e.section_id = s.section_id AND e.status='ENROLLED')
                       ,0)) AS seats_left,
                       COALESCE(s.semester, '') AS semester
                FROM sections s
                JOIN courses c ON c.course_id = s.course_id
                LEFT JOIN instructors i ON i.instructor_id = s.instructor_id
                WHERE s.instructor_id = ? AND s.semester = ?
                ORDER BY c.code
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, instructorId);
            ps.setString(2, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SectionRow(
                            rs.getLong("section_id"),
                            rs.getLong("course_id"),
                            rs.getString("code"),
                            rs.getString("title"),
                            rs.getDouble("credits"),
                            rs.getLong("instructor_id"),
                            rs.getString("instructor"),
                            rs.getInt("capacity"),
                            rs.getInt("seats_left"),
                            rs.getString("semester")
                    ));
                }
            }
        }
        return list;
    }

    // --------------------------------------------------------
    //  MAINTENANCE MODE
    // --------------------------------------------------------
    @Override
    public boolean isMaintenanceOn() throws SQLException {
        String sql = "SELECT maintenance FROM settings LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getBoolean("maintenance");
            }
        }
        return false;
    }

    // --------------------------------------------------------
    //  DROP DEADLINE PASSED
    // --------------------------------------------------------
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
            }
        }
        return false;
    }

    // --------------------------------------------------------
    //  SEATS LEFT
    // --------------------------------------------------------
    @Override
    public int getSeatsLeft(long sectionId) throws SQLException {
        String sql = """
                SELECT 
                    (s.capacity - IFNULL(
                        (SELECT COUNT(*) FROM enrollments e 
                         WHERE e.section_id = s.section_id AND e.status='ENROLLED')
                    ,0)) AS seats_left
                FROM sections s
                WHERE s.section_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("seats_left");
            }
        }
        return 0;
    }

    // --------------------------------------------------------
    //  BASIC CRUD
    // --------------------------------------------------------
    @Override
    public List<Section> findFiltered(String courseId, String instructorId) {
        List<Section> list = new ArrayList<>();
        String sql = "SELECT * FROM sections WHERE course_id=? AND instructor_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, instructorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception ignore) {}
        return list;
    }

    @Override
    public Optional<Section> findById(long id) {
        String sql = "SELECT * FROM sections WHERE section_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    @Override
    public void insert(Section s) {
        String sql = """
                INSERT INTO sections(course_id, instructor_id, capacity, semester)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, s.getCourseId());
            ps.setLong(2, s.getInstructorId());
            ps.setInt(3, s.getCapacity());
            ps.setString(4, s.getSemester());
            ps.executeUpdate();
        } catch (Exception ignore) {}
    }

    @Override
    public void update(Section s) {
        String sql = """
                UPDATE sections SET course_id=?, instructor_id=?, capacity=?, semester=?
                WHERE section_id=?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, s.getCourseId());
            ps.setLong(2, s.getInstructorId());
            ps.setInt(3, s.getCapacity());
            ps.setString(4, s.getSemester());
            ps.setLong(5, s.getSectionId());
            ps.executeUpdate();
        } catch (Exception ignore) {}
    }

    // --------------------------------------------------------
    //  HELPER
    // --------------------------------------------------------
    private Section mapRow(ResultSet rs) throws SQLException {
        return new Section(
                rs.getLong("section_id"),
                rs.getLong("course_id"),
                rs.getLong("instructor_id"),
                rs.getInt("capacity"),
                rs.getString("semester")
        );
    }

    // --------------------------------------------------------
    //  DROP DEADLINE OPERATIONS
    @Override
    public java.time.LocalDate getDropDeadline(long sectionId) throws SQLException {
        String sql = "SELECT drop_deadline FROM sections WHERE section_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date deadline = rs.getDate("drop_deadline");
                    return deadline != null ? deadline.toLocalDate() : null;
                }
            }
        }
        return null;
    }
    @Override
    public boolean updateDropDeadline(long sectionId, java.time.LocalDate date) throws SQLException {
        String sql = "UPDATE sections SET drop_deadline=? WHERE section_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }
    @Override
    public boolean clearDropDeadline(long sectionId) throws SQLException {
        String sql = "UPDATE sections SET drop_deadline=NULL WHERE section_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            return ps.executeUpdate() > 0;
        }
    }
}

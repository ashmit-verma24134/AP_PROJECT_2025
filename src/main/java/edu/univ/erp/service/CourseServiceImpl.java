package edu.univ.erp.service;

import edu.univ.erp.data.AdminDao;
import edu.univ.erp.data.CourseDao;
import edu.univ.erp.model.Course;
import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.*;

import edu.univ.erp.data.CourseDao;
import edu.univ.erp.data.CourseDaoImpl;
import edu.univ.erp.model.Course;

/**
 * JDBC-based CourseService implementation.
 * Keeps all DB access here. The UI will not import any SQL/DB code.
 */
public class CourseServiceImpl implements CourseService {

    private final CourseDao dao;

    public CourseServiceImpl(CourseDao dao) {
        this.dao = dao;
    }

    @Override
    public CourseResult loadCourseWithSections(long courseId) throws Exception {
        CourseResult out = new CourseResult();
        out.courseId = courseId;
        out.sections = new ArrayList<>();

    


        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();

            // detect columns in courses table (fallback to common names)
            String courseTbl = "courses";
            String codeCol = detectColumn(md, courseTbl, new String[]{"course_code", "code", "courseid", "course"});
            String titleCol = detectColumn(md, courseTbl, new String[]{"title", "course_title", "name"});
            String creditsCol = detectColumn(md, courseTbl, new String[]{"credits","credit"});

            String sqlCourse = "SELECT * FROM " + courseTbl + " WHERE " + detectCourseIdCol(md) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCourse)) {
                ps.setLong(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        out.code = safeGetString(rs, codeCol);
                        out.title = safeGetString(rs, titleCol);
                        out.credits = safeGetString(rs, creditsCol);
                    }
                }
            }

            // sections table detection and select
            String secTbl = detectSectionsTable(md);
            if (secTbl != null) {
                String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                String secCodeCol = detectColumn(md, secTbl, new String[]{"section_code","code","sec_code"});
                String courseIdColInSec = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
                String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","schedule","slot"});
                String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
                String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
                String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
                String yearCol = detectColumn(md, secTbl, new String[]{"year"});
                String instrCol = detectColumn(md, secTbl, new String[]{"instructor_id","instructorid","teacher_id"});

                // build select
                List<String> sel = new ArrayList<>();
                sel.add((secIdCol != null ? secIdCol : "NULL") + " AS section_id");
                sel.add((secCodeCol != null ? secCodeCol : "NULL") + " AS section_code");
                sel.add((timeCol != null ? timeCol : "NULL") + " AS day_time");
                sel.add((roomCol != null ? roomCol : "NULL") + " AS room");
                sel.add((capCol != null ? capCol : "NULL") + " AS capacity");
                sel.add((semCol != null ? semCol : "NULL") + " AS semester");
                sel.add((yearCol != null ? yearCol : "NULL") + " AS year");
                sel.add((instrCol != null ? instrCol : "NULL") + " AS instructor_id");

                String q = "SELECT " + String.join(", ", sel) + " FROM " + secTbl + " WHERE " + courseIdColInSec + " = ?";
                if (secIdCol != null) q += " ORDER BY " + secIdCol;

                try (PreparedStatement ps = conn.prepareStatement(q)) {
                    ps.setLong(1, courseId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            SectionRow sr = new SectionRow();
                            Object sid = rs.getObject("section_id");
                            sr.sectionId = sid == null ? null : ((sid instanceof Number) ? ((Number) sid).longValue() : Long.parseLong(String.valueOf(sid)));
                            sr.sectionCode = safeGetString(rs, "section_code");
                            sr.dayTime = safeGetString(rs, "day_time");
                            sr.room = safeGetString(rs, "room");
                            Object cap = rs.getObject("capacity");
                            if (cap instanceof Number) sr.capacity = ((Number)cap).intValue();
                            else if (cap != null) { try { sr.capacity = Integer.parseInt(String.valueOf(cap)); } catch (Exception ignored) {} }
                            sr.semester = safeGetString(rs, "semester");
                            Object y = rs.getObject("year");
                            if (y instanceof Number) sr.year = ((Number)y).intValue();
                            else if (y != null) { try { sr.year = Integer.parseInt(String.valueOf(y)); } catch (Exception ignored) {} }
                            Object iid = rs.getObject("instructor_id");
                            if (iid instanceof Number) sr.instructorId = ((Number) iid).longValue();
                            else if (iid != null) { try { sr.instructorId = Long.parseLong(String.valueOf(iid)); } catch (Exception ignored) {} }
                            out.sections.add(sr);
                        }
                    }
                }
            }
        }

        return out;
    }

    @Override
    public void saveCourseBasic(long courseId, String code, String title, String credits) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String courseTbl = "courses";
            String courseIdCol = detectCourseIdCol(md);
            String codeCol = detectColumn(md, courseTbl, new String[]{"course_code", "code", "courseid", "course"});
            String titleCol = detectColumn(md, courseTbl, new String[]{"title","course_title","name"});
            String creditsCol = detectColumn(md, courseTbl, new String[]{"credits","credit"});

            List<String> sets = new ArrayList<>();
            if (codeCol != null) sets.add(codeCol + " = ?");
            if (titleCol != null) sets.add(titleCol + " = ?");
            if (creditsCol != null) sets.add(creditsCol + " = ?");
            if (sets.isEmpty()) throw new SQLException("No writable columns detected on courses table");

            String sql = "UPDATE " + courseTbl + " SET " + String.join(", ", sets) + " WHERE " + courseIdCol + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                if (codeCol != null) ps.setString(idx++, code.isEmpty() ? null : code);
                if (titleCol != null) ps.setString(idx++, title.isEmpty() ? null : title);
                if (creditsCol != null) {
                    if (credits == null || credits.isEmpty()) ps.setNull(idx++, Types.DECIMAL);
                    else ps.setBigDecimal(idx++, new java.math.BigDecimal(credits));
                }
                ps.setLong(idx, courseId);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public long createSection(long courseId, SectionRow row) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String secTbl = detectSectionsTable(md);
            if (secTbl == null) throw new SQLException("Sections table not found");

            String courseIdCol = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
            String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
            String codeCol = detectColumn(md, secTbl, new String[]{"section_code","code"});
            String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","schedule","slot"});
            String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
            String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
            String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
            String yearCol = detectColumn(md, secTbl, new String[]{"year"});
            String instrCol = detectColumn(md, secTbl, new String[]{"instructor_id","instructorid","teacher_id"});

            StringBuilder cols = new StringBuilder();
            StringBuilder vals = new StringBuilder();
            List<Object> params = new ArrayList<>();

            cols.append(courseIdCol); vals.append("?"); params.add(courseId);
            if (codeCol != null) { cols.append(", ").append(codeCol); vals.append(", ?"); params.add(nullIfEmpty(row.sectionCode)); }
            if (timeCol != null) { cols.append(", ").append(timeCol); vals.append(", ?"); params.add(nullIfEmpty(row.dayTime)); }
            if (roomCol != null) { cols.append(", ").append(roomCol); vals.append(", ?"); params.add(nullIfEmpty(row.room)); }
            if (capCol != null) { cols.append(", ").append(capCol); vals.append(", ?"); params.add(row.capacity); }
            if (semCol != null) { cols.append(", ").append(semCol); vals.append(", ?"); params.add(nullIfEmpty(row.semester)); }
            if (yearCol != null) { cols.append(", ").append(yearCol); vals.append(", ?"); params.add(row.year); }
            if (instrCol != null) { cols.append(", ").append(instrCol); vals.append(", ?"); params.add(row.instructorId); }

            String sql = "INSERT INTO " + secTbl + " (" + cols.toString() + ") VALUES (" + vals.toString() + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i=0;i<params.size();++i) {
                    Object p = params.get(i);
                    if (p == null) ps.setNull(i+1, Types.VARCHAR);
                    else if (p instanceof Integer) ps.setInt(i+1, (Integer)p);
                    else if (p instanceof Long) ps.setLong(i+1, (Long)p);
                    else ps.setString(i+1, String.valueOf(p));
                }
                ps.executeUpdate();
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) return gk.getLong(1);
                }
            }
            return -1L;
        }
    }

    @Override
    public boolean updateSection(long sectionId, SectionRow row) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String secTbl = detectSectionsTable(md);
            if (secTbl == null) throw new SQLException("Sections table not found");
            String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
            String codeCol = detectColumn(md, secTbl, new String[]{"section_code","code"});
            String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","schedule","slot"});
            String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
            String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
            String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
            String yearCol = detectColumn(md, secTbl, new String[]{"year"});
            String instrCol = detectColumn(md, secTbl, new String[]{"instructor_id","instructorid","teacher_id"});

            List<String> sets = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            if (codeCol != null) { sets.add(codeCol + " = ?"); params.add(nullIfEmpty(row.sectionCode)); }
            if (timeCol != null) { sets.add(timeCol + " = ?"); params.add(nullIfEmpty(row.dayTime)); }
            if (roomCol != null) { sets.add(roomCol + " = ?"); params.add(nullIfEmpty(row.room)); }
            if (capCol != null) { sets.add(capCol + " = ?"); params.add(row.capacity); }
            if (semCol != null) { sets.add(semCol + " = ?"); params.add(nullIfEmpty(row.semester)); }
            if (yearCol != null) { sets.add(yearCol + " = ?"); params.add(row.year); }
            if (instrCol != null) { sets.add(instrCol + " = ?"); params.add(row.instructorId); }

            if (sets.isEmpty()) throw new SQLException("No writable columns on sections table");

            String sql = "UPDATE " + secTbl + " SET " + String.join(", ", sets) + " WHERE " + secIdCol + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params) {
                    if (p == null) ps.setNull(idx++, Types.VARCHAR);
                    else if (p instanceof Integer) ps.setInt(idx++, (Integer)p);
                    else if (p instanceof Long) ps.setLong(idx++, (Long)p);
                    else ps.setString(idx++, String.valueOf(p));
                }
                ps.setLong(idx, sectionId);
                int updated = ps.executeUpdate();
                return updated > 0;
            }
        }
    }

    @Override
    public boolean deleteSection(long sectionId) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String secTbl = detectSectionsTable(md);
            String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
            if (secTbl == null || secIdCol == null) throw new SQLException("Cannot find sections table or id column");
            String delSql = "DELETE FROM " + secTbl + " WHERE " + secIdCol + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                ps.setLong(1, sectionId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    @Override
    public List<InstructorItem> listInstructors() throws Exception {
        List<InstructorItem> out = new ArrayList<>();
        try (Connection conn = DBConnection.getErpConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String instrTbl = null;
            try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String name = tables.getString("TABLE_NAME").toLowerCase();
                    if ("instructors".equals(name) || "instructor".equals(name) || "teachers".equals(name)) {
                        instrTbl = tables.getString("TABLE_NAME");
                        break;
                    }
                }
            }
            if (instrTbl == null) { out.add(new InstructorItem(null, "<No Instructor>")); return out; }

            String idCol = detectColumn(md, instrTbl, new String[]{"instructor_id","id"});
            String nameCol = detectColumn(md, instrTbl, new String[]{"full_name","name","first_name"});
            String sql = "SELECT " + idCol + ", " + (nameCol != null ? nameCol : idCol) + " FROM " + instrTbl + " ORDER BY " + (nameCol != null ? nameCol : idCol);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                out.add(new InstructorItem(null, "<No Instructor>"));
                while (rs.next()) {
                    Long id = rs.getObject(1) == null ? null : rs.getLong(1);
                    String nm = rs.getString(2);
                    out.add(new InstructorItem(id, nm == null ? ("Instructor " + id) : nm));
                }
            }
        }
        return out;
    }

    // ---------- helpers (same logic as earlier) ----------
    private static String safeGetString(ResultSet rs, String col) {
        if (col == null) return null;
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }

    private static String detectCourseIdCol(DatabaseMetaData md) throws SQLException {
        try (ResultSet rs = md.getColumns(null, null, "courses", null)) {
            while (rs.next()) {
                String c = rs.getString("COLUMN_NAME").toLowerCase();
                if ("course_id".equals(c) || "id".equals(c)) return rs.getString("COLUMN_NAME");
            }
        } catch (SQLException ignored) {}
        return "course_id";
    }

    private static String detectSectionsTable(DatabaseMetaData md) throws SQLException {
        try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME").toLowerCase();
                if ("sections".equals(name) || "course_sections".equals(name) || "sections_tbl".equals(name)) return tables.getString("TABLE_NAME");
            }
        } catch (SQLException ignored) {}
        return "sections";
    }

    private static String detectColumn(DatabaseMetaData md, String table, String[] candidates) throws SQLException {
        if (table == null) return null;
        try (ResultSet rs = md.getColumns(null, null, table, null)) {
            while (rs.next()) {
                String c = rs.getString("COLUMN_NAME").toLowerCase();
                for (String s : candidates) if (s.equals(c)) return rs.getString("COLUMN_NAME");
            }
        } catch (SQLException ignored) {}
        return null;
    }

    private static Object nullIfEmpty(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return s.isBlank() ? null : o;
    }

    @Override
    public List<Course> listAllCourses() throws ServiceException {
        try (Connection conn = DBConnection.getErpConnection()) {
            CourseDao dao = new CourseDaoImpl(conn);
            // adjust method name if your DAO uses a different name (e.g. findAllCourses)
            return dao.listAll(); 
        } catch (Exception ex) {
            throw new ServiceException("Unable to list courses", ex);
        }
    }

}
package edu.univ.erp.service;

import edu.univ.erp.data.*;
import edu.univ.erp.model.GradeDetail;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default StudentService implementation.
 * All JDBC/DAO usage stays here; UI calls this service.
 */
public class StudentServiceImpl implements StudentService {

    public StudentServiceImpl() {
        // default ctor
    }

    /**
     * Return grade-detail rows adapted to GradeDetail DTOs.
     */
    @Override
    public List<GradeDetail> getGradeDetails(String studentId) throws ServiceException {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            List<Map<String, Object>> rows = dao.getGradeDetails(studentId);
            List<GradeDetail> out = new ArrayList<>();
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    GradeDetail g = new GradeDetail();
                    g.setEnrollmentId(r.get("enrollment_id"));
                    g.setCourseCode(r.get("course_code") == null ? null : String.valueOf(r.get("course_code")));
                    g.setCourseName(r.get("course_name") == null ? null : String.valueOf(r.get("course_name")));
                    Object c = r.get("credits");
                    if (c instanceof Number) g.setCredits(((Number) c).intValue());
                    else if (c != null) {
                        try { g.setCredits(Integer.parseInt(String.valueOf(c))); } catch (Exception ignored) {}
                    }
                    g.setFinalGrade(r.get("final_grade") == null ? null : String.valueOf(r.get("final_grade")));
                    Object gp = r.get("grade_point");
                    if (gp instanceof Number) g.setGradePoint(((Number) gp).doubleValue());
                    else if (gp != null) {
                        try { g.setGradePoint(Double.parseDouble(String.valueOf(gp))); } catch (Exception ignored) {}
                    }
                    g.setSemester(r.get("semester") == null ? null : String.valueOf(r.get("semester")));
                    Object y = r.get("year");
                    if (y instanceof Number) g.setYear(((Number) y).intValue());
                    else if (y != null) {
                        try { g.setYear(Integer.parseInt(String.valueOf(y))); } catch (Exception ignored) {}
                    }
                    out.add(g);
                }
            }
            return out;
        } catch (Exception ex) {
            throw new ServiceException("Unable to load grade details", ex);
        }
    }

    // in edu.univ.erp.service.StudentServiceImpl
@Override
public Map<String, Object> getStudentOverview(String studentId) {
    // example: fetch Student entity and return a Map
    Student s = studentDao.findById(Long.parseLong(studentId)); // adapt to your DAO API
    if (s == null) return null;
    Map<String, Object> m = new HashMap<>();
    m.put("username", s.getUsername());
    m.put("full_name", s.getFullName());
    m.put("roll_no", s.getRollNo());
    return m;
}


    /**
     * Return current courses for student. Delegates to StudentDao.
     */
    @Override
    public List<Map<String, Object>> getCurrentCourses(String studentId, String query) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            return dao.getCurrentCourses(studentId, query);
        }
    }

    /**
     * Register the student into the given section.
     * Performs maintenance check, existence checks, seat check and creates enrollment inside a transaction.
     */
    @Override
    public Result registerForSection(String studentId, long sectionId) {
        final long sid;
        try {
            sid = Long.parseLong(studentId);
        } catch (NumberFormatException e) {
            return Result.error("Invalid student id");
        }

        try (Connection conn = DBConnection.getErpConnection()) {
            try {
                conn.setAutoCommit(false);

                SettingsDao settingsDao = new SettingsDaoImpl(conn);
                if (settingsDao.isMaintenanceOn()) {
                    conn.rollback();
                    return Result.error("System in maintenance mode — registration disabled.");
                }

                // ensure student exists
                try (var ps = conn.prepareStatement("SELECT 1 FROM students WHERE student_id = ? LIMIT 1")) {
                    ps.setLong(1, sid);
                    try (var rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Result.error("Student record not found. Please login with a valid student account.");
                        }
                    }
                }

                EnrollmentDao enrollmentDao = new EnrollmentDaoImpl(conn);
                if (enrollmentDao.isStudentEnrolled(sid, sectionId)) {
                    conn.rollback();
                    return Result.error("You are already enrolled in this section.");
                }

                SectionDao sectionDao = new SectionDaoImpl(conn);
                int seatsLeft = sectionDao.getSeatsLeft(sectionId);
                if (seatsLeft <= 0) {
                    conn.rollback();
                    return Result.error("This section is already full.");
                }

                long enrollmentId;
                try {
                    enrollmentId = enrollmentDao.createEnrollment(sid, sectionId);
                } catch (Exception daoEx) {
                    // attempt to map FK/constraint SQL errors to friendly message
                    Throwable cause = daoEx;
                    while (cause != null && !(cause instanceof java.sql.SQLException)) {
                        cause = cause.getCause();
                    }
                    if (cause instanceof java.sql.SQLException) {
                        java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                        if (sqlEx.getErrorCode() == 1452 || (sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("23"))) {
                            conn.rollback();
                            return Result.error("Registration failed: student or section does not exist (foreign key).");
                        }
                    }
                    throw daoEx;
                }

                if (enrollmentId <= 0) {
                    conn.rollback();
                    return Result.error("Unexpected database error during registration.");
                }

                conn.commit();
                return Result.ok("Registered successfully! (enrollment id: " + enrollmentId + ")");
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Exception ignore) {}
                if (ex instanceof java.sql.SQLException) {
                    java.sql.SQLException sqlEx = (java.sql.SQLException) ex;
                    if (sqlEx.getErrorCode() == 1452 || (sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("23"))) {
                        return Result.error("Registration failed: student or section does not exist (foreign key).");
                    }
                }
                return Result.error("Database error: " + ex.getMessage());
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
            }
        } catch (Exception ex) {
            return Result.error("Connection error: " + ex.getMessage());
        }
    }

    /**
     * Drop a section for a student.
     * Performs maintenance check and drop-deadline check inside a transaction.
     */
    @Override
    public Result dropSection(String studentId, long sectionId) {
        final long sid;
        try {
            sid = Long.parseLong(studentId);
        } catch (NumberFormatException e) {
            return Result.error("Invalid student ID.");
        }

        try (Connection conn = DBConnection.getErpConnection()) {
            try {
                conn.setAutoCommit(false);

                SettingsDao settingsDao = new SettingsDaoImpl(conn);
                if (settingsDao.isMaintenanceOn()) {
                    conn.rollback();
                    return Result.error("System in maintenance mode — drop disabled.");
                }

                SectionDao sectionDao = new SectionDaoImpl(conn);
                if (sectionDao.isDropDeadlineOver(sectionId)) {
                    conn.rollback();
                    return Result.error("Drop deadline has passed; cannot drop.");
                }

                EnrollmentDao enrollmentDao = new EnrollmentDaoImpl(conn);
                boolean ok = enrollmentDao.dropEnrollment(sid, sectionId);
                if (!ok) {
                    conn.rollback();
                    return Result.error("Not enrolled or already dropped.");
                }

                conn.commit();
                return Result.ok("Dropped successfully!");
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Exception ignore) {}
                return Result.error("Database error: " + ex.getMessage());
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
            }
        } catch (Exception ex) {
            return Result.error("Connection error: " + ex.getMessage());
        }
    }
}
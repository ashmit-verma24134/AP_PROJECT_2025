package edu.univ.erp.service;

import edu.univ.erp.data.*;
import edu.univ.erp.util.DBConnection;

// imports at top of StudentService.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import edu.univ.erp.service.StudentSummary;
import edu.univ.erp.service.SemesterRecord;

/**
 * Business logic for student actions (register/drop).
 * Centralizes maintenance & access checks.
 */
public class StudentService {

    /**
     * Attempt to register the student in a section.
     * All checks are performed here in a transaction-like flow.
     */
public Result registerForSection(String studentId, long sectionId) {
    // convert studentId to long once
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

            // Optional defensive check: ensure student exists (gives clearer error than FK failure)
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

            if (sectionDao.isDropDeadlineOver(sectionId)) {
                conn.rollback();
                return Result.error("Registration deadline has passed.");
            }

            boolean ok = false;
            try {
                ok = enrollmentDao.createEnrollment(sid, sectionId);
            } catch (Exception daoEx) {
                // If DAO throws SQLException, try to detect FK/constraint issues and convert to friendly message
                Throwable cause = daoEx;
                while (cause != null && !(cause instanceof java.sql.SQLException)) {
                    cause = cause.getCause();
                }
                if (cause instanceof java.sql.SQLException) {
                    java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                    // MySQL FK violation commonly has error code 1452 and SQLState starting with "23"
                    if (sqlEx.getErrorCode() == 1452 || (sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("23"))) {
                        conn.rollback();
                        return Result.error("Registration failed: student or section does not exist (foreign key).");
                    }
                }
                // otherwise rethrow to be handled below
                throw daoEx;
            }

            if (!ok) {
                conn.rollback();
                return Result.error("Unexpected database error during registration.");
            }

            conn.commit();
            return Result.ok(" Registered successfully!");
        } catch (Exception ex) {                       // catch Exception because DAOs can throw SQLException or other
            try { conn.rollback(); } catch (Exception ignore) {}
            // Map common SQL issues to friendly messages where possible
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
 * Fetch student header info for dashboard: name, program, inferred current sem, current cgpa.
 * Accepts student_id (long).
 */
public StudentSummary getStudentSummaryById(long studentId) throws Exception {
    String sql = """
        SELECT s.student_id, s.full_name, s.roll_no, s.program,
          -- infer current semester as the max section.semester the student has enrollment for
          (SELECT MAX(CAST(sec.semester AS SIGNED)) FROM sections sec
             JOIN enrollments e ON e.section_id = sec.section_id
             WHERE e.student_id = s.student_id AND e.status IN ('ENROLLED','COMPLETED','WAITLISTED')
          ) AS current_sem,
          -- try to read precomputed cgpa view, otherwise compute on the fly
          ( SELECT v.cgpa FROM erp_db.v_student_cgpa v WHERE v.student_id = s.student_id LIMIT 1 ) AS view_cgpa
        FROM erp_db.students s
        WHERE s.student_id = ?
    """;

    try (Connection conn = DBConnection.getErpConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, studentId);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            StudentSummary out = new StudentSummary();
            out.setStudentId(rs.getLong("student_id"));
            out.setFullName(rs.getString("full_name"));
            out.setRollNo(rs.getString("roll_no"));
            out.setProgram(rs.getString("program"));

            int cs = rs.getInt("current_sem");
            if (rs.wasNull()) out.setCurrentSem(null);
            else out.setCurrentSem(cs);

            Double cg = null;
            try {
                cg = rs.getDouble("view_cgpa");
                if (rs.wasNull()) cg = null;
            } catch (Exception ignore) { cg = null; }

            // if view didn't exist / returned null, compute cgpa on the fly
            if (cg == null) {
String calc = 
    "SELECT ROUND(SUM((g.score / NULLIF(g.max_score,0)) * 10.0 * c.credits) / NULLIF(SUM(c.credits),0), 2) AS cgpa "
  + "FROM grades g "
  + "JOIN enrollments e ON g.enrollment_id = e.enrollment_id "
  + "JOIN sections sec ON e.section_id = sec.section_id "
  + "JOIN courses c ON sec.course_id = c.course_id "
  + "WHERE e.student_id = ? "
  + "  AND g.score IS NOT NULL AND g.max_score IS NOT NULL";

                try (PreparedStatement ps2 = conn.prepareStatement(calc)) {
                    ps2.setLong(1, studentId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            cg = rs2.getDouble("cgpa");
                            if (rs2.wasNull()) cg = null;
                        }
                    }
                }
            }
            out.setCurrentCgpa(cg);
            return out;
        }
    }
}

/**
 * Fetch semester-wise SGPA/CGPA rows for a student up to inferred current semester.
 * Relies on your v_student_performance view (if present) or computes by grouping.
 */
public List<SemesterRecord> getSemestersUpToCurrent(long studentId) throws Exception {
    List<SemesterRecord> list = new ArrayList<>();
String sqlView =
  "SELECT sem_no, year, sgpa, cgpa FROM ( "
+ " SELECT CAST(sec.semester AS SIGNED) AS sem_no, sec.year, "
+ " ROUND(SUM((g.score / NULLIF(g.max_score,0)) * 10.0 * c.credits) / NULLIF(SUM(c.credits),0),2) AS sgpa, NULL AS cgpa "
+ " FROM grades g "
+ " JOIN enrollments e ON g.enrollment_id = e.enrollment_id "
+ " JOIN sections sec ON e.section_id = sec.section_id "
+ " JOIN courses c ON sec.course_id = c.course_id "
+ " WHERE e.student_id = ? AND g.score IS NOT NULL AND g.max_score IS NOT NULL "
+ " GROUP BY CAST(sec.semester AS SIGNED), sec.year "
+ " ORDER BY CAST(sec.semester AS SIGNED) "
+ ") t";


    try (Connection conn = DBConnection.getErpConnection();
         PreparedStatement ps = conn.prepareStatement(sqlView)) {
        ps.setLong(1, studentId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SemesterRecord r = new SemesterRecord();
                r.setSemNo(rs.getInt("sem_no"));
                r.setYear(rs.getInt("year"));
                double sg = rs.getDouble("sgpa");
                r.setSgpa(rs.wasNull() ? null : sg);
                // cgpa per sem can be computed client-side from sgpa & credits; leaving cgpa null or compute cumulative below
                r.setCgpa(null);
                list.add(r);
            }
        }
    }

    // compute cumulative CGPA per sem (weighted) if you want it stored in each row
    // We'll compute using a simple loop over semesters using SQL for credits and points per semester
    double totalPoints = 0.0;
    double totalCredits = 0.0;
    for (SemesterRecord sr : list) {
        String semCalc = """
            SELECT SUM(g.points * c.credits) AS pts, SUM(c.credits) AS creds
            FROM grades g JOIN enrollments e ON g.enrollment_id = e.enrollment_id
            JOIN sections sec ON e.section_id = sec.section_id
            JOIN courses c ON sec.course_id = c.course_id
            WHERE e.student_id = ? AND CAST(sec.semester AS SIGNED) = ? AND g.letter_grade NOT IN ('I','W','S','X')
        """;
        try (Connection conn = DBConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(semCalc)) {
            ps.setLong(1, studentId);
            ps.setInt(2, sr.getSemNo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double pts = rs.getDouble("pts");
                    if (rs.wasNull()) pts = 0.0;
                    double creds = rs.getDouble("creds");
                    if (rs.wasNull()) creds = 0.0;
                    totalPoints += pts;
                    totalCredits += creds;
                    if (totalCredits > 0) {
                        sr.setCgpa(Math.round((totalPoints / totalCredits) * 100.0) / 100.0);
                    } else {
                        sr.setCgpa(null);
                    }
                }
            }
        }
    }

    return list;
}



    /**
     * Drop a section for a student.
     */
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

                // --- Maintenance check ---
                SettingsDao settingsDao = new SettingsDaoImpl(conn);
                if (settingsDao.isMaintenanceOn()) {
                    conn.rollback();
                    return Result.error(" System in maintenance mode — drop disabled.");
                }

                // --- Drop deadline check ---
                SectionDao sectionDao = new SectionDaoImpl(conn);
                if (sectionDao.isDropDeadlineOver(sectionId)) {
                    conn.rollback();
                    return Result.error("Drop deadline has passed; cannot drop.");
                }

                // --- Perform drop ---
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

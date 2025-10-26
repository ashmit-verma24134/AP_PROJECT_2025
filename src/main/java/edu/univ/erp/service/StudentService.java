package edu.univ.erp.service;

import edu.univ.erp.data.*;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;

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

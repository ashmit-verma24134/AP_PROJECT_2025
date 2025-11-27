package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-based implementation of TranscriptService.
 * All SQL / DBConnection lives here; UI will not touch DB directly.
 */
public class TranscriptServiceImpl implements TranscriptService {

    public TranscriptServiceImpl() {}

    @Override
    public TranscriptResult loadTranscriptForStudent(String studentId) throws Exception {
        TranscriptResult res = new TranscriptResult();
        res.studentId = studentId;
        res.rows = new ArrayList<>();

        try (Connection conn = DBConnection.getErpConnection()) {

            // fetch basic metadata (if present)
            try (PreparedStatement psInfo = conn.prepareStatement(
                    "SELECT full_name, department, year AS batch FROM students WHERE student_id = ?")) {
                try { psInfo.setLong(1, Long.parseLong(studentId)); }
                catch (NumberFormatException ex) { psInfo.setString(1, studentId); }
                try (ResultSet rsInfo = psInfo.executeQuery()) {
                    if (rsInfo.next()) {
                        res.studentName = rsInfo.getString("full_name");
                        res.department = rsInfo.getString("department");
                        Object b = rsInfo.getObject("batch");
                        res.batch = b == null ? null : String.valueOf(b);
                    }
                } catch (Exception ignore) {}
            } catch (Exception ignore) {}

            // fetch transcript rows
            String sql = """
                SELECT c.code, c.title, COALESCE(c.credits,0) AS credits,
                       s.semester, s.year,
                       g2.final_grade
                FROM enrollments e
                JOIN sections s ON s.section_id = e.section_id
                JOIN courses c ON c.course_id = s.course_id
                LEFT JOIN (
                    SELECT gr.enrollment_id,
                           gr.final_grade,
                           COALESCE(gr.computed_at, gr.created_at) AS ts
                    FROM grades gr
                    WHERE gr.final_grade IS NOT NULL
                      AND gr.enrollment_id IS NOT NULL
                ) g2 ON g2.enrollment_id = e.enrollment_id
                WHERE e.student_id = ?
                  AND e.status IN ('ENROLLED','COMPLETED')
                ORDER BY s.year DESC, s.semester DESC, c.code ASC
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try { ps.setLong(1, Long.parseLong(studentId)); }
                catch (NumberFormatException ex) { ps.setString(1, studentId); }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TranscriptRow r = new TranscriptRow(
                                rs.getString("code"),
                                rs.getString("title"),
                                rs.getInt("credits"),
                                rs.getString("semester"),
                                rs.getInt("year"),
                                rs.getString("final_grade")
                        );
                        res.rows.add(r);
                    }
                }
            }

            // dedupe / pick latest per course code (same logic as previous panel)
            if (res.rows.size() > 1) {
                java.util.Map<String, TranscriptRow> best = new java.util.LinkedHashMap<>();
                // Build semester order map (best-effort)
                java.util.Map<String, Integer> semOrder = new java.util.HashMap<>();
                try (PreparedStatement psSem = conn.prepareStatement(
                        "SELECT semester, MIN(year) AS first_year FROM sections GROUP BY semester ORDER BY first_year ASC")) {
                    try (ResultSet rsSem = psSem.executeQuery()) {
                        int idx = 0;
                        while (rsSem.next()) {
                            String sem = rsSem.getString("semester");
                            if (sem == null) sem = "";
                            semOrder.put(sem, idx++);
                        }
                    }
                } catch (Exception ignore) {}

                for (TranscriptRow r : res.rows) {
                    String key = r.code == null ? r.title : r.code;
                    TranscriptRow existing = best.get(key);
                    if (existing == null) { best.put(key, r); continue; }

                    if (r.year > existing.year) { best.put(key, r); continue; }
                    if (r.year < existing.year) continue;

                    Integer o1 = semOrder.get(r.semester);
                    Integer o2 = semOrder.get(existing.semester);
                    if (o1 != null && o2 != null) {
                        if (o1 > o2) { best.put(key, r); }
                        continue;
                    }

                    if ((r.semester == null ? "" : r.semester).compareTo(existing.semester == null ? "" : existing.semester) > 0) {
                        best.put(key, r);
                        continue;
                    }

                    boolean existingHasGrade = existing.finalGrade != null && !existing.finalGrade.isBlank();
                    boolean newHasGrade = r.finalGrade != null && !r.finalGrade.isBlank();
                    if (!existingHasGrade && newHasGrade) best.put(key, r);
                }
                res.rows = new ArrayList<>(best.values());
            }

            // compute CGPA weighted by credits (ignore ungraded rows)
            double sumPointsTimesCredits = 0.0;
            double sumCreditsForGraded = 0.0;
            for (TranscriptRow rr : res.rows) {
                Double pts = gradeToPoints(rr.finalGrade);
                if (pts != null) {
                    sumPointsTimesCredits += pts * (double) rr.credits;
                    sumCreditsForGraded += (double) rr.credits;
                }
            }
            if (sumCreditsForGraded > 0.0) {
                double cgpa = sumPointsTimesCredits / sumCreditsForGraded;
                cgpa = Math.round(cgpa * 100.0) / 100.0;
                res.cgpa = cgpa;
            } else {
                res.cgpa = null;
            }

        } // conn auto-close

        return res;
    }

    // same mapping as before (10-point scale)
    private static Double gradeToPoints(String grade) {
        if (grade == null) return null;
        String g = grade.trim().toUpperCase();
        switch (g) {
            case "A+": case "A": return 10.0;
            case "A-": return 9.0;
            case "B+": return 8.0;
            case "B": return 7.0;
            case "B-": return 6.0;
            case "C+": return 5.0;
            case "C": return 4.0;
            case "C-": return 3.0;
            case "D": return 2.0;
            case "F": return 0.0;
            default:
                try { return Double.parseDouble(g); } catch (Exception e) { return null; }
        }
    }
}
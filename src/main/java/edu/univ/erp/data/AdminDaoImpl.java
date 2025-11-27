package edu.univ.erp.data;

import edu.univ.erp.model.Instructor;
import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDaoImpl implements AdminDao {

    @Override
    public void deleteCourseCascade(long courseId) {
        try (Connection conn = DBConnection.getErpConnection()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM sections WHERE course_id=?");
            ps1.setLong(1, courseId);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM courses WHERE course_id=?");
            ps2.setLong(1, courseId);
            ps2.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteSectionCascade(long sectionId) {
        try (Connection conn = DBConnection.getErpConnection()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM enrollments WHERE section_id=?");
            ps1.setLong(1, sectionId);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM sections WHERE section_id=?");
            ps2.setLong(1, sectionId);
            ps2.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countEnrolled(long sectionId) {
        try (Connection conn = DBConnection.getErpConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM enrollments WHERE section_id=? AND status='ENROLLED'"
            );
            ps.setLong(1, sectionId);

            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);

            return 0;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Instructor> findAllInstructors() {
        List<Instructor> result = new ArrayList<>();
        try (Connection conn = DBConnection.getErpConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                "SELECT instructor_id, full_name, email FROM instructors ORDER BY instructor_id"
            );
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Instructor ins = new Instructor(
                        rs.getLong("instructor_id"),
                        rs.getString("full_name"),
                        rs.getString("email")
                );
                result.add(ins);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}

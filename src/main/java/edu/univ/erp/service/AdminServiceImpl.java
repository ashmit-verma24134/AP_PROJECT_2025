package edu.univ.erp.service;

import edu.univ.erp.data.AdminDao;
import edu.univ.erp.model.Instructor;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class AdminServiceImpl implements AdminService {
    private final AdminDao dao;

    public AdminServiceImpl(AdminDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Instructor> listAllInstructors() {
        try {
            return dao.findAllInstructors();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load instructors", ex);
        }
    }

    @Override
    public void deleteCourseCascade(long courseId) {
        try {
            dao.deleteCourseCascade(courseId);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete course cascade", ex);
        }
    }

    @Override
    public void deleteSectionCascade(long sectionId) {
        try {
            dao.deleteSectionCascade(sectionId);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete section cascade", ex);
        }
    }

    @Override
    public int countEnrolled(long sectionId) {
        try {
            return dao.countEnrolled(sectionId);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to count enrolled students", ex);
        }
    }

     @Override
    public String getAdminUsername(long userId) throws Exception {
        String sql = "SELECT username FROM users WHERE user_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }

        return null;
    }

    @Override
    public String getUsernameById(long userId) throws Exception {
        String sql = "SELECT username FROM users WHERE user_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }

        return null;
    }
    
}

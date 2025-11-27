package edu.univ.erp.data;

import edu.univ.erp.model.Instructor;
import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;

public class InstructorDaoImpl implements InstructorDao {

    private final Connection conn;

    public InstructorDaoImpl(Connection conn) {
        this.conn = conn;
    }

    private Instructor mapRow(ResultSet rs) throws SQLException {
        Instructor i = new Instructor();
        i.setInstructorId(rs.getLong("instructor_id"));
        i.setFirstName(rs.getString("first_name"));
        i.setLastName(rs.getString("last_name"));
        i.setEmail(rs.getString("email"));
        i.setUsername(rs.getString("username"));
        i.setDepartment(rs.getString("department"));
        return i;
    }

    @Override
    public Optional<Instructor> findById(long instructorId) throws Exception {
        String sql = """
            SELECT instructor_id, first_name, last_name, email, username, department
            FROM instructors
            WHERE instructor_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Instructor> findByUsername(String username) throws Exception {
        String sql = """
            SELECT instructor_id, first_name, last_name, email, username, department
            FROM instructors
            WHERE username = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Instructor> listAll() throws Exception {
        String sql = """
            SELECT instructor_id, first_name, last_name, email, username, department
            FROM instructors
            ORDER BY last_name, first_name
        """;

        List<Instructor> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    @Override
    public void insert(Instructor instructor) throws Exception {
        String sql = """
            INSERT INTO instructors(first_name, last_name, email, username, department)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, instructor.getFirstName());
            ps.setString(2, instructor.getLastName());
            ps.setString(3, instructor.getEmail());
            ps.setString(4, instructor.getUsername());
            ps.setString(5, instructor.getDepartment());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    instructor.setInstructorId(keys.getLong(1));
                }
            }
        }
    }

    @Override
    public void update(Instructor instructor) throws Exception {
        String sql = """
            UPDATE instructors
            SET first_name = ?, last_name = ?, email = ?, username = ?, department = ?
            WHERE instructor_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructor.getFirstName());
            ps.setString(2, instructor.getLastName());
            ps.setString(3, instructor.getEmail());
            ps.setString(4, instructor.getUsername());
            ps.setString(5, instructor.getDepartment());
            ps.setLong(6, instructor.getInstructorId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(long instructorId) throws Exception {
        String sql = "DELETE FROM instructors WHERE instructor_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, instructorId);
            ps.executeUpdate();
        }
    }
}


package edu.univ.erp.data;

import edu.univ.erp.model.Course;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple DAO implementation for courses table.
 * Adjust SQL column names if your schema uses different names.
 */
public class CourseDaoImpl implements CourseDao {

     private final Connection conn;

    // DEFAULT CONSTRUCTOR (no throws)
    public CourseDaoImpl() {
        Connection c = null;
        try {
            c = DBConnection.getErpConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            // Optional: log or handle error
        }
        this.conn = c;
    }

    // Constructor if someone wants to inject custom connection
    public CourseDaoImpl(Connection conn) {
        this.conn = conn;
    }
    

    @Override
    public List<Course> listAll() throws Exception {
        String sql = "SELECT course_id, course_code, title, credits FROM courses ORDER BY course_code";

        List<Course> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Course c = new Course();

                // id
                try { c.setCourseId(rs.getLong("course_id")); } catch (Throwable ignore) {}

                // code/title
                try { c.setCode(rs.getString("course_code")); } catch (Throwable ignore) {}
                try { c.setTitle(rs.getString("title")); } catch (Throwable ignore) {}

                // credits -> your model expects Double, handle nulls safely
                try {
                    Object cr = rs.getObject("credits");
                    if (cr == null) {
                        c.setCredits(null);
                    } else {
                        // read as double for numeric or parse if string
                        try { c.setCredits(rs.getDouble("credits")); }
                        catch (Throwable t) {
                            try { c.setCredits(Double.parseDouble(String.valueOf(cr))); }
                            catch (Throwable tt) { c.setCredits(null); }
                        }
                    }
                } catch (Throwable ignore) {
                    // tolerate missing column / incompatible type
                    try { c.setCredits(null); } catch (Throwable ignore2) {}
                }

                out.add(c);
            }
        }

        return out;
    }
}
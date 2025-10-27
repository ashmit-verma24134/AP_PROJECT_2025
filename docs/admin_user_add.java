import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

import edu.univ.erp.util.DBConnection;

public class admin_user_add {

    public static boolean create_user(String us_n, String pw, int role, String student_roll_num, String program, Integer year, String instr_dept, int admin_userId ) throws Exception{
        Connection conn = null;
        PreparedStatement pstadmin_user=null;
        PreparedStatement pstStudent=null;
        PreparedStatement pstInstr=null;
        PreparedStatement pstlog=null;

        try{
            conn = DBConnection.getConnection();

            pstadmin_user = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            pstadmin_user.setString(1, us_n);
            ResultSet rs = pstadmin_user.executeQuery();

            if(rs.next()){
                throw new Exception("Username already exists.");
                conn.rollback();
                return false;
            }
            pstadmin_user.close();

            String hashed_pw = BCrypt.hashpw(pw, BCrypt.gensalt(11));

            pstadmin_user = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            pstadmin_user.setString(1, us_n);
            pstadmin_user.setString(2, hashed_pw);
            pstadmin_user.setInt(3, role);
            
            pstadmin_user.executeUpdate();

            ResultSet generatedKeys = pstadmin_user.getGeneratedKeys();
            if(generatedKeys.next()){
                int new_userid = generatedKeys.getInt(1);
                
                Connection conn2 = DBConnection.getConnection("jdbc:mysql://localhost:3306/erp_db?serverTimezone=UTC",
                    "root",
                    "Ash1234");

                try{
                    if(role == 1){              // 1 - student ke liye..I didn't what number to assign
                        pstStudent = conn2.prepareStatement("INSERT INTO students (user_id, roll_number, program, year) VALUES (?, ?, ?, ?)");
                        pstStudent.setInt(1, new_userid);
                        pstStudent.setString(2, student_roll_num);
                        pstStudent.setString(3, program);
                        pstStudent.setInt(4, year);
                        pstStudent.executeUpdate();

                    } else if(role == 2){           // 2 - instructor ke liye
                        pstInstr = conn2.prepareStatement("INSERT INTO instructors (user_id, department) VALUES (?, ?)");
                        pstInstr.setInt(1, new_userid);
                        pstInstr.setString(2, instr_dept);
                        pstInstr.executeUpdate();
                    }

                    conn2.commit();
                } catch(Exception e){
                    conn.rollback();
                    conn2.rollback();
                    throw e;
                } finally{
                    //if(pstStudent != null) pstStudent.close();
                    //if(pstInstr != null) pstInstr.close();
                    conn2.close();
                }
            }
            else if(!generatedKeys.next()){
                conn.rollback();
                throw new Exception("Failed to retrieve user ID.");
                return false;
            }
            // Logging the admin action

            pstlog = conn.prepareStatement("INSERT INTO admin_logs (admin_user_id, action, target_user_id, timestamp) VALUES (?, 'CREATE_USER', ?, ?)");        //ek baar yeh dekh lena.. mujhe itna idea nhi hai 
            pstlog.setInt(1, admin_userId);
            pstlog.setInt(2, generatedKeys.getInt(1));
            pstlog.setString(3, "Created user with ID: " + generatedKeys.getInt(1));
            pstlog.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstlog.executeUpdate();

            conn.commit();
            return true;

        } catch(Exception e){
            if(conn != null){
                conn.rollback();
            }
            throw e;
            return false;
        } finally{
            if(pstadmin_user != null) pstadmin_user.close();
            if(pstStudent != null) pstStudent.close();
            if(pstInstr != null) pstInstr.close();
            if(pstlog != null) pstlog.close();
            if(conn != null) conn.close();

        }
    }
    
}

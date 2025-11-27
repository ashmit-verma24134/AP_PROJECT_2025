package edu.univ.erp.service;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * TimetableService loads a student's weekly schedule.
 * Returns List<Map<String,Object>> identical to StudentDao.getStudentSchedule().
 */
public class TimetableService {

    public List<Map<String,Object>> getStudentSchedule(String studentId) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            StudentDao dao = new StudentDaoImpl(conn);
            return dao.getStudentSchedule(studentId);
        }
    }
}
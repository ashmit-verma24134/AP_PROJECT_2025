package edu.univ.erp.data;

import edu.univ.erp.model.Instructor;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface InstructorDao {

    Optional<Instructor> findById(long instructorId) throws Exception;

    Optional<Instructor> findByUsername(String username) throws Exception;

    List<Instructor> listAll() throws Exception;
    void insert(Instructor instructor) throws Exception;
    void update(Instructor instructor) throws Exception;

    void delete(long instructorId) throws Exception;
}

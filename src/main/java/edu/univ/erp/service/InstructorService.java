package edu.univ.erp.service;

import edu.univ.erp.model.Instructor;

import java.util.Optional;
import java.util.List;

public interface InstructorService {
    
    Optional<Instructor> getById(long instructorId) throws Exception;

    Optional<Instructor> getByUsername(String username) throws Exception;

    List<Instructor> listAll() throws Exception;
}

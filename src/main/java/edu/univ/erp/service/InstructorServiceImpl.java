package edu.univ.erp.service;

import edu.univ.erp.data.InstructorDao;
import edu.univ.erp.model.Instructor;

import java.util.List;
import java.util.Optional;

public class InstructorServiceImpl implements InstructorService {

    private final InstructorDao dao;

    public InstructorServiceImpl(InstructorDao dao) {
        this.dao = dao;
    }

    @Override
    public Optional<Instructor> getById(long instructorId) throws Exception {
        return dao.findById(instructorId);
    }

    @Override
    public Optional<Instructor> getByUsername(String username) throws Exception {
        return dao.findByUsername(username);
    }

    @Override
    public List<Instructor> listAll() throws Exception {
        return dao.listAll();
    }
}

package edu.univ.erp.service;

import java.util.List;

/**
 * Service interface to fetch transcript rows + metadata for a student.
 */
public interface TranscriptService {

    /**
     * Result object returned by the service.
     */
    class TranscriptResult {
        public String studentId;
        public String studentName;
        public String department;
        public String batch;
        public Double cgpa; // may be null
        public List<TranscriptRow> rows;

        public TranscriptResult() {}
    }

    /**
     * Lightweight DTO representing a single transcript row.
     */
    class TranscriptRow {
        public String code;
        public String title;
        public int credits;
        public String semester;
        public int year;
        public String finalGrade;

        public TranscriptRow() {}

        public TranscriptRow(String code, String title, int credits, String semester, int year, String finalGrade) {
            this.code = code;
            this.title = title;
            this.credits = credits;
            this.semester = semester;
            this.year = year;
            this.finalGrade = finalGrade;
        }
    }

    /**
     * Load transcript for a given student id (string). Implementations may parse numeric id.
     */
    TranscriptResult loadTranscriptForStudent(String studentId) throws Exception;
}
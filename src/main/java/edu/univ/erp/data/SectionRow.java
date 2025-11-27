package edu.univ.erp.data;

public class SectionRow {

    public long sectionId;
    public long courseId;
    public String code;
    public String title;
    public double credits;
    public long instructorId;
    public String instructorName;
    public int capacity;
    public int seatsLeft;
    public String semester;

    public SectionRow(
            long sectionId,
            long courseId,
            String code,
            String title,
            double credits,
            long instructorId,
            String instructorName,
            int capacity,
            int seatsLeft,
            String semester
    ) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.capacity = capacity;
        this.seatsLeft = seatsLeft;
        this.semester = semester;
    }

    @Override
    public String toString() {
        return code + " - " + title + " (" + semester + ")";
    }
}

package edu.univ.erp.data;

/**
 * POJO representing one assessment component row (one row = one component for one enrollment).
 */
public class AssessmentComponent {
    private long id;
    private long sectionId;
    private String name;
    private Integer weight;
    private Double maxScore;
    private Boolean published;
    private Double studentScore;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSectionId() { return sectionId; }
    public void setSectionId(long sectionId) { this.sectionId = sectionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public Double getStudentScore() { return studentScore; }
    public void setStudentScore(Double studentScore) { this.studentScore = studentScore; }
}

package edu.univ.erp.service;

/**
 * Simple data holder for an assessment component and a student's score on it.
 * Fields chosen to match what AssessmentsDialog and the DAO expect.
 */
public class AssessmentComponent {

    private Long componentId;
    private String name;
    private Integer weight;       // percentage integer, e.g. 20
    private Double maxScore;      // maximum points for component
    private Double studentScore;  // student's points for this component
    private Boolean published;    // whether score is released

    public AssessmentComponent() {}

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long componentId) { this.componentId = componentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public Double getStudentScore() { return studentScore; }
    public void setStudentScore(Double studentScore) { this.studentScore = studentScore; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    @Override
    public String toString() {
        return "AssessmentComponent{" +
                "componentId=" + componentId +
                ", name='" + name + '\'' +
                ", weight=" + weight +
                ", maxScore=" + maxScore +
                ", studentScore=" + studentScore +
                ", published=" + published +
                '}';
    }
}
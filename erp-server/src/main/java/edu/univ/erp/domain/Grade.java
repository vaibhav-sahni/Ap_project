package edu.univ.erp.domain;

import java.io.Serializable;
import java.util.List;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String courseName;
    private String finalGrade; // Letter grade (A, B+, F)
    private List<AssessmentComponent> components; 
    /**
     * Optional numeric score or points associated with this grade.
     * Historically the server used Double.NaN as a sentinel; to avoid
     * emitting invalid JSON tokens (NaN) we use a nullable Double and
     * leave it null when no numeric score is available.
     */
    private Double numericScore = null;

    // Constructor used by StudentService for final aggregation
    public Grade(String courseName, String finalGrade, List<AssessmentComponent> components) {
        this.courseName = courseName;
        this.finalGrade = finalGrade;
        this.components = components;
    }

    /**
     * Backward-compatible constructor that accepts an extra numeric score.
     */
    public Grade(String courseName, String finalGrade, List<AssessmentComponent> components, Double numericScore) {
        this.courseName = courseName;
        this.finalGrade = finalGrade;
        this.components = components;
        this.numericScore = numericScore;
    }
    
    // Default constructor required by Gson
    public Grade() { }

    // Getters and Setters (Required by Gson)
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
    public List<AssessmentComponent> getComponents() { return components; }
    public void setComponents(List<AssessmentComponent> components) { this.components = components; }
    public Double getNumericScore() { return numericScore; }
    public void setNumericScore(Double numericScore) { this.numericScore = numericScore; }
}
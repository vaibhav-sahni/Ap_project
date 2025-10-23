package edu.univ.erp.domain;

import java.io.Serializable;
import java.util.List;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String courseName;
    private String finalGrade; // Letter grade (A, B+, F)
    private double credits;
    private List<AssessmentComponent> components; 

    // Constructor used by StudentService for final aggregation
    public Grade(String courseName, String finalGrade, List<AssessmentComponent> components) {
        this.courseName = courseName;
        this.finalGrade = finalGrade;
        this.components = components;
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
    public double getCredits() { return credits; }
    public void setCredits(double credits) { this.credits = credits; }
}
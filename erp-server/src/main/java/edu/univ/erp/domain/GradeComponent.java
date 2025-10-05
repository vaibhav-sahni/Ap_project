package edu.univ.erp.domain;

/**
 * Represents a single score component (e.g., Quiz 1, Midterm Exam) 
 * within a course grade record.
 */
public class GradeComponent {
    private String componentName;
    private double score;

    // Getters and Setters (or public fields, depending on your style)

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}

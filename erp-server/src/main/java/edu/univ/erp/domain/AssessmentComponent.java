package edu.univ.erp.domain;

import java.io.Serializable;

public class AssessmentComponent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String componentName; 
    private double score;         
    
    public AssessmentComponent(String componentName, double score) {
        this.componentName = componentName;
        this.score = score;
    }
    
    public AssessmentComponent() {} //default constructor
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
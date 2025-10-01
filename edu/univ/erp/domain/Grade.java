package edu.univ.erp.domain;

public class Grade {
    private final int grade_id;
    private final int enrollment_id; // FK to Enrollment
    private final String component; // e.g., "Quiz1", "Midterm", "EndSem"
    private final double score; // Raw score
    private final String final_grade; // e.g., "A", "B+", "F" (can be null until computed)

    public Grade(int grade_id, int enrollment_id, String component, double score, String final_grade) {
        this.grade_id = grade_id;
        this.enrollment_id = enrollment_id;
        this.component = component;
        this.score = score;
        this.final_grade = final_grade;
    }

    public int getgrade_id() { return this.grade_id; }
    public int getenrollment_id() { return this.enrollment_id; }
    public String getComponent() { return this.component; }
    public double getScore() { return this.score; }
    public String getfinal_grade() { return this.final_grade; }
}
package edu.univ.erp.domain;

public class Enrollment {
    private final int enrollment_id;
    private final int student_id; // FK to Student (UserAuth ID)
    private final int section_id; // FK to Section
    private final String status; // e.g., "Registered", "Dropped", "Completed"

    public Enrollment(int enrollment_id, int student_id, int section_id, String status) {
        this.enrollment_id = enrollment_id;
        this.student_id = student_id;
        this.section_id = section_id;
        this.status = status;
    }

    public int getenrollment_id() { return this.enrollment_id; }
    public int getstudent_id() { return this.student_id; }
    public int getsection_id() { return this.section_id; }
    public String getStatus() { return this.status; }
}
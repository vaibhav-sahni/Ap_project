package edu.univ.erp.domain;

import java.io.Serializable;

/**
 * Domain model representing a course section.
 * Contains the constructors necessary for various DAO operations.
 */
public class Section implements Serializable {
    private static final long serialVersionUID = 1L;

    private int sectionId;
    private String courseCode;
    private String courseName;
    private int capacity;
    private int instructorId; // ID of the assigned instructor

    public Section() {}

    /**
     * Required constructor used by the InstructorDAO to populate basic section details.
     */
    public Section(int sectionId, String courseCode, String courseName, int capacity) {
        this.sectionId = sectionId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.capacity = capacity;
    }
    
    /** Full Constructor. */
    public Section(int sectionId, String courseCode, String courseName, int capacity, int instructorId) {
        this.sectionId = sectionId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.capacity = capacity;
        this.instructorId = instructorId;
    }

    // --- Getters and Setters ---
    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }
}
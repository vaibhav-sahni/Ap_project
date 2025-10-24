package edu.univ.erp.domain;

// LocalTime is NOT needed here because your schema uses VARCHAR(50) for day_time
// and does not use separate start_time/end_time columns.

public class CourseCatalog {
    
    // Course Details
    private String courseCode; // e.g., CSCI 301 (PRIMARY KEY from your schema)
    private String courseTitle; 
    private int credits; // New field from your schema
    
    // Section Details
    private int sectionId;
    private String dayTime; // e.g., MWF 10:00-11:00 (Matches your schema)
    private String room;
    private int capacity;
    private int enrolledCount; // Fetched via COUNT in the query
    private String semester;
    private int year;
    
    // Instructor Details
    private int instructorId;
    private String instructorName; // Fetched from instructors.name
    private String enrollmentStatus; // e.g., Registered, Dropped, Completed (nullable)

    // Default Constructor (required by Gson)
    public CourseCatalog() {}

    // Parameterized Constructor (for the DAO to use)
    public CourseCatalog(String courseCode, String courseTitle, int credits, int sectionId, String dayTime, 
                         String room, int capacity, int enrolledCount, String semester, int year,
                         int instructorId, String instructorName, String enrollmentStatus) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.sectionId = sectionId;
        this.dayTime = dayTime;
        this.room = room;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.semester = semester;
        this.year = year;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.enrollmentStatus = enrollmentStatus;
    }

    /**
     * Backward-compatible constructor used in some DAO call sites that do not
     * provide the enrollmentStatus field. Keeps behavior identical and sets
     * enrollmentStatus to null.
     */
    public CourseCatalog(String courseCode, String courseTitle, int credits, int sectionId, String dayTime,
                         String room, int capacity, int enrolledCount, String semester, int year,
                         int instructorId, String instructorName) {
        this(courseCode, courseTitle, credits, sectionId, dayTime, room, capacity, enrolledCount, semester, year, instructorId, instructorName, null);
    }

    // --- GETTERS (Required for Client UI and JSON Serialization) ---
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public int getCredits() { return credits; }
    public int getSectionId() { return sectionId; }
    public String getDayTime() { return dayTime; }
    public String getRoom() { return room; }
    public int getCapacity() { return capacity; }
    public int getEnrolledCount() { return enrolledCount; }
    public String getSemester() { return semester; }
    public int getYear() { return year; }
    public int getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    
    @Override
    public String toString() {
        return courseCode + ": " + courseTitle + " (Section ID: " + sectionId + ", Instructor: " + instructorName + ", Enrolled: " + enrolledCount + "/" + capacity + ")";
    }
}
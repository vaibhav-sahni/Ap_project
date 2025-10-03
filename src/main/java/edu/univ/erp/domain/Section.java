package edu.univ.erp.domain;

public class Section {
    private final int section_id;
    private final String courseCode;
    private final int instructor_id; 
    private final String day_time; // e.g., "M/W 10:00-11:30"
    private final String room;
    private final int capacity;
    private final String semester;
    private final int year;

    public Section(int section_id, String courseCode, int instructor_id, String day_time, 
                   String room, int capacity, String semester, int year) {
        this.section_id = section_id;
        this.courseCode = courseCode;
        this.instructor_id = instructor_id;
        this.day_time = day_time;
        this.room = room;
        this.capacity = capacity;
        this.semester = semester;
        this.year = year;
    }

    public int getsection_id() { return this.section_id; }
    public String getCourseCode() { return this.courseCode; }
    public int getinstructor_id() { return this.instructor_id; }
    public String getday_time() { return this.day_time; } //class timings and days
    public String getRoom() { return this.room; }
    public int getCapacity() { return this.capacity; }
    public String getSemester() { return this.semester; }
    public int getYear() { return this.year; }
}
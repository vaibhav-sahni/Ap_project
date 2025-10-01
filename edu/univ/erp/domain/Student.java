package edu.univ.erp.domain;

public class Student {
    private final int user_id;
    private final String roll_no;
    private final String program;
    private final int year;

    public Student(int user_id, String roll_no, String program, int year) {
        this.user_id = user_id;
        this.roll_no = roll_no;
        this.program = program;
        this.year = year;
    }

    public int getuser_id() { return this.user_id; }
    public String getRollNo() { return this.roll_no; }
    public String getProgram() { return this.program; }
    public int getYear() { return this.year; }
}
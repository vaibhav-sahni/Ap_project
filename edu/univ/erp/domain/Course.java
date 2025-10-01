package edu.univ.erp.domain;

public class Course {
    private final String code;
    private final String title;
    private final int credits;

    public Course(String code, String title, int credits) {
        this.code = code;
        this.title = title;
        this.credits = credits;
    }

    public String getCode() { return this.code; }
    public String getTitle() { return this.title; }
    public int getCredits() { return this.credits; }
}
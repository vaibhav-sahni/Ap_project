package edu.univ.erp.domain;

public class Instructor {
    private final int user_id; 
    private final String name; 
    private final String dept;

    public Instructor(int user_id, String name, String dept) {
        this.user_id = user_id;
        this.name = name;
        this.dept = dept;
    }

    public int getuser_id() { return this.user_id; }
    public String getName() { return this.name; }
    public String getdept() { return this.dept; }
}
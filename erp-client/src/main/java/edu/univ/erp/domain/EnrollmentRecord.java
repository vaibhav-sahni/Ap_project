package edu.univ.erp.domain;

/**
 * Domain object representing a single student's enrollment in a section, 
 * including their component scores and final grade.
 */
public class EnrollmentRecord {

    // Identifiers
    private int enrollmentId;
    private int studentId;
    private String studentName;
    private String rollNo; 
    
    // Component Scores (raw scores out of 100 or as entered)
    private Double quizScore;        
    private Double assignmentScore;  
    private Double midtermScore;     
    private Double endtermScore;     

    // Final Grade (calculated or directly stored letter grade)
    private String finalGrade;
    
    // --- Constructor ---

    public EnrollmentRecord() {
        // Initialize scores to null to distinguish between a zero score and no score recorded
        this.quizScore = null;
        this.assignmentScore = null;
        this.midtermScore = null;
        this.endtermScore = null;
        this.finalGrade = null;
        this.rollNo = null; 
    }

    // --- Getters and Setters ---

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public Double getQuizScore() {
        return quizScore;
    }

    // *** NEW: Safe getter for primitive double, returning 0.0 if null ***
    public double getQuizScoreSafe() {
        return quizScore == null ? 0.0 : quizScore.doubleValue();
    }
    // *******************************************************************

    public void setQuizScore(Double quizScore) {
        this.quizScore = quizScore;
    }

    public Double getAssignmentScore() {
        return assignmentScore;
    }
    
    // *** NEW: Safe getter for primitive double, returning 0.0 if null ***
    public double getAssignmentScoreSafe() {
        return assignmentScore == null ? 0.0 : assignmentScore.doubleValue();
    }
    // *******************************************************************

    public void setAssignmentScore(Double assignmentScore) {
        this.assignmentScore = assignmentScore;
    }

    public Double getMidtermScore() {
        return midtermScore;
    }

    // *** NEW: Safe getter for primitive double, returning 0.0 if null ***
    public double getMidtermScoreSafe() {
        return midtermScore == null ? 0.0 : midtermScore.doubleValue();
    }
    // *******************************************************************

    public void setMidtermScore(Double midtermScore) {
        this.midtermScore = midtermScore;
    }

    public Double getEndtermScore() {
        return endtermScore;
    }

    // *** NEW: Safe getter for primitive double, returning 0.0 if null ***
    public double getEndtermScoreSafe() {
        return endtermScore == null ? 0.0 : endtermScore.doubleValue();
    }
    // *******************************************************************

    public void setEndtermScore(Double endtermScore) {
        this.endtermScore = endtermScore;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade = finalGrade;
    }

    @Override
    public String toString() {
        return "EnrollmentRecord{" +
                "enrollmentId=" + enrollmentId +
                ", studentName='" + studentName + '\'' +
                ", rollNo='" + rollNo + '\'' +
                ", Quiz=" + (quizScore != null ? String.format("%.2f", quizScore) : "N/A") +
                ", Assignment=" + (assignmentScore != null ? String.format("%.2f", assignmentScore) : "N/A") +
                ", Midterm=" + (midtermScore != null ? String.format("%.2f", midtermScore) : "N/A") +
                ", Endterm=" + (endtermScore != null ? String.format("%.2f", endtermScore) : "N/A") +
                ", FinalGrade='" + (finalGrade != null ? finalGrade : "N/A") + '\'' +
                '}';
    }
}

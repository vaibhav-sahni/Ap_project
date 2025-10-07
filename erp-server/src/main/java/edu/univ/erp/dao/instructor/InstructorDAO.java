package edu.univ.erp.dao.instructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Section;

/**
 * Data Access Object (DAO) for Instructor-specific queries.
 * Handles fetching sections taught by an instructor, student rosters, and grade recording.
 */
public class InstructorDAO {

    // SQL to fetch all sections assigned to a specific instructor.
    private static final String GET_SECTIONS_SQL = 
        "SELECT S.section_id, S.course_code, C.title AS course_name, S.capacity " +
        "FROM Sections S " +
        "JOIN Courses C ON S.course_code = C.code " +
        "WHERE S.instructor_id = ?";

    // SQL to fetch the full student roster for a given section, including existing grades.
    // FIX 1: Removed leading spaces on the concatenated line for G.component.
    private static final String GET_ROSTER_SQL = 
        "SELECT E.enrollment_id, E.student_id, U.username AS student_name, ST.roll_no, " + 
        "G.component, G.score, G.final_grade " + 
        "FROM Enrollments E " +
        "JOIN Students ST ON E.student_id = ST.user_id " +
        "JOIN auth_db.users_auth U ON ST.user_id = U.user_id " + // Cross-database access
        "LEFT JOIN Grades G ON E.enrollment_id = G.enrollment_id " +
        "WHERE E.section_id = ? AND E.status = 'Registered'";

    // SQL to fetch the component grades for a single enrollment record.
    // FIX 2: Removed leading spaces on the concatenated line for G.component.
    private static final String GET_SINGLE_ENROLLMENT_GRADES_SQL = 
        "SELECT E.enrollment_id, E.student_id, U.username AS student_name, ST.roll_no, " +
        "G.component, G.score, G.final_grade " + 
        "FROM Enrollments E " +
        "JOIN auth_db.users_auth U ON E.student_id = U.user_id " + 
        "JOIN Students ST ON E.student_id = ST.user_id " + 
        "LEFT JOIN Grades G ON E.enrollment_id = G.enrollment_id " +
        "WHERE E.enrollment_id = ?";

    private static final String UPDATE_ENROLLMENT_STATUS_SQL =
        "UPDATE enrollments SET status = ? WHERE enrollment_id = ?";


    /**
     * Retrieves all course sections taught by a given instructor.
     */
    public List<Section> getSectionsByInstructorId(int instructorId) throws SQLException {
        List<Section> sections = new ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_SECTIONS_SQL)) {
            
            stmt.setInt(1, instructorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sections.add(new Section(
                        rs.getInt("section_id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"), 
                        rs.getInt("capacity"),
                        instructorId
                    ));
                }
            }
        } 
        return sections; 
    }

    /**
     * Retrieves the complete roster for a section, aggregating component grades per student.
     */
    public List<EnrollmentRecord> getEnrollmentRoster(int sectionId) throws SQLException {
        // Map to hold EnrollmentRecord objects keyed by enrollmentId for aggregation
        Map<Integer, EnrollmentRecord> recordsMap = new HashMap<>();
        
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ROSTER_SQL)) {
            
            stmt.setInt(1, sectionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int enrollmentId = rs.getInt("enrollment_id");
                    EnrollmentRecord record = recordsMap.getOrDefault(enrollmentId, new EnrollmentRecord());
                    
                    if (record.getEnrollmentId() == 0) { // New record, initialize base data
                        record.setEnrollmentId(enrollmentId);
                        record.setStudentId(rs.getInt("student_id"));
                        // *** CHANGE: Populate student identification data ***
                        record.setStudentName(rs.getString("student_name")); 
                        record.setRollNo(rs.getString("roll_no")); // NEW: set the roll number
                        recordsMap.put(enrollmentId, record);
                    }
                    
                    // Map component scores to specific fields in the EnrollmentRecord
                    this.mapComponentScore(record, rs.getString("component"), rs.getDouble("score"));
                    
                    // Check if the row contains the final_grade (letter grade) and set it if not already set.
                    if (rs.getString("final_grade") != null && record.getFinalGrade() == null) {
                        record.setFinalGrade(rs.getString("final_grade"));
                    }
                }
            }
        } 
        // Return the aggregated list of records
        return new ArrayList<>(recordsMap.values());
    }

    /**
     * Retrieves a single EnrollmentRecord with all component scores for final grade calculation.
     */
    public EnrollmentRecord getSingleEnrollmentRecord(int enrollmentId) throws SQLException {
        EnrollmentRecord record = null;

        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_SINGLE_ENROLLMENT_GRADES_SQL)) {
            
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (record == null) {
                        record = new EnrollmentRecord();
                        record.setEnrollmentId(enrollmentId);
                        record.setStudentId(rs.getInt("student_id"));
                        // *** CHANGE: Populate student identification data ***
                        record.setStudentName(rs.getString("student_name")); 
                        record.setRollNo(rs.getString("roll_no")); // NEW: set the roll number
                    }
                    
                    // Map component scores to specific fields in the EnrollmentRecord
                    this.mapComponentScore(record, rs.getString("component"), rs.getDouble("score"));
                    
                    // Check if the row contains the final_grade (letter grade) and set it if not already set.
                    if (rs.getString("final_grade") != null && record.getFinalGrade() == null) {
                        record.setFinalGrade(rs.getString("final_grade"));
                    }
                }
            }
        } 
        return record;
    }

    /** * Helper method to map raw database component names to EnrollmentRecord fields.
     * Uses a switch statement for clean, exact mapping.
     */
    private void mapComponentScore(EnrollmentRecord record, String component, double score) {
        if (component == null) return;
        
        switch (component.toLowerCase()) {
            case "quiz":
                record.setQuizScore(score);
                break;
            case "assignment":
            case "project": // Include 'project' if used elsewhere
                record.setAssignmentScore(score);
                break;
            case "midterm":
            case "midterm exam": // Include 'midterm exam' if used elsewhere
                record.setMidtermScore(score);
                break;
            case "endterm":
            case "endsem": 
                record.setEndtermScore(score);
                break;
            default:
                // Ignore other components (like 'finalgrade' which is handled separately)
                break;
        }
    }


    /**
     * Records or updates a score for a single assessment component (UPSERT logic).
     */
    public void recordGradeComponent(int enrollmentId, String componentName, double score) throws SQLException {
        // Inserts/updates component scores (Quiz, Midterm, etc.) where final_grade is NULL.
        String UPSERT_GRADE_SQL = 
            "INSERT INTO Grades (enrollment_id, component, score, final_grade) " +
            "VALUES (?, ?, ?, NULL) " +
            "ON DUPLICATE KEY UPDATE score = VALUES(score)"; 

        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPSERT_GRADE_SQL)) {
            
            stmt.setInt(1, enrollmentId);
            stmt.setString(2, componentName);
            stmt.setDouble(3, score);
            stmt.executeUpdate();
        } 
    }
    
    /**
     * Updates the final letter grade for a course on a specific enrollment record.
     */
    public void updateFinalGrade(int enrollmentId, String finalGrade) throws SQLException {
        // Targets the Grades table, using 'FinalGrade' as the component name to store the letter grade.
        String UPSERT_FINAL_GRADE_SQL = 
            "INSERT INTO Grades (enrollment_id, component, score, final_grade) " +
            "VALUES (?, 'FinalGrade', NULL, ?) " +
            "ON DUPLICATE KEY UPDATE final_grade = VALUES(final_grade)";
            
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPSERT_FINAL_GRADE_SQL)) {
            
            stmt.setInt(1, enrollmentId);
            stmt.setString(2, finalGrade);
            stmt.executeUpdate();
        } 
    }
    
    // --- Access Checker Dependency Methods ---

    /**
     * Security check: Determines if the instructor is assigned to the given section.
     */
    public boolean isInstructorAssigned(int instructorId, int sectionId) throws SQLException {
        String SQL = "SELECT COUNT(*) FROM Sections WHERE section_id = ? AND instructor_id = ?";
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setInt(1, sectionId);
            stmt.setInt(2, instructorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Security check: Determines if the instructor teaches the section linked to the enrollment.
     */
    public boolean isInstructorOfEnrollmentRecord(int instructorId, int enrollmentId) throws SQLException {
        String SQL = "SELECT COUNT(*) FROM Enrollments E JOIN Sections S ON E.section_id = S.section_id WHERE E.enrollment_id = ? AND S.instructor_id = ?";
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setInt(1, enrollmentId);
            stmt.setInt(2, instructorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Updates the status of a student enrollment.
     * @param enrollmentId The enrollment record ID.
     * @param newStatus The new status, e.g., "Completed".
     * @throws SQLException
     */
    public void updateEnrollmentStatus(int enrollmentId, String newStatus) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ENROLLMENT_STATUS_SQL)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, enrollmentId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No enrollment record updated; check the enrollment ID.");
            }
        }
    }
}



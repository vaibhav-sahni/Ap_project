package edu.univ.erp.dao.grade;

import java.sql.Connection; // Assuming this exists
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.AssessmentComponent;

public class GradeDAO {
    
    // 1. Record to hold initial results (used by StudentService)
    public static record RawGradeResult(
        int enrollmentId,
        String courseTitle,
        String finalGrade
    ) {}

    // SQL to fetch course title, final letter grade, AND the enrollment ID
    private static final String FIND_RAW_GRADES_SQL = 
        "SELECT " +
        "e.enrollment_id, " + 
        "c.title AS course_title, " +
        "g.final_grade " +
        "FROM enrollments e " + // **START HERE: Get ALL enrollments first**
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN courses c ON s.course_code = c.code " +
        // **LEFT JOIN: Attach the grade ONLY IF it exists, otherwise g.final_grade is NULL**
        "LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id AND g.component = 'FinalGrade' " +
        "WHERE e.student_id = ?"; 

    // SQL to fetch all assessment components (raw scores)
    private static final String FIND_COMPONENTS_SQL = 
        "SELECT component, score FROM grades WHERE enrollment_id = ? AND component != 'FinalGrade' AND score IS NOT NULL";

    
    public List<RawGradeResult> getRawGradeResultsByUserId(int userId) {
        List<RawGradeResult> rawResults = new ArrayList<>();
        
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_RAW_GRADES_SQL)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rawResults.add(new RawGradeResult(
                        rs.getInt("enrollment_id"),
                        rs.getString("course_title"),
                        rs.getString("final_grade")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving raw grades: " + e.getMessage());
        }
        return rawResults;
    }
    
    public List<AssessmentComponent> getComponentsByEnrollmentId(int enrollmentId) {
        List<AssessmentComponent> components = new ArrayList<>();
        
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_COMPONENTS_SQL)) {
            
            stmt.setInt(1, enrollmentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    components.add(new AssessmentComponent(
                        rs.getString("component"),
                        rs.getDouble("score")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error retrieving assessment components: " + e.getMessage());
        }
        return components;
    }
}
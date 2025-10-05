package edu.univ.erp.dao.enrollment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.univ.erp.dao.db.DBConnector;

public class EnrollmentDAO {

    // 1. Check if the section is full
    private static final String GET_CAPACITY_SQL = 
        "SELECT s.capacity, COUNT(e.student_id) AS enrolled_count " +
        "FROM sections s " +
        "LEFT JOIN enrollments e ON s.section_id = e.section_id AND e.status = 'Registered' " +
        "WHERE s.section_id = ? " +
        "GROUP BY s.section_id";

    // 2. Check if the student is already enrolled (Registered)
    private static final String CHECK_EXISTING_ENROLLMENT_SQL =
        "SELECT status FROM enrollments WHERE student_id = ? AND section_id = ?";
    
    // 3. Get the student's current enrollment schedule (for conflict checking)
    private static final String GET_STUDENT_SCHEDULE_SQL =
        "SELECT s.day_time FROM enrollments e " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "WHERE e.student_id = ? AND e.status = 'Registered'";

    // 4. Register the student (Insert a new active enrollment)
    private static final String REGISTER_COURSE_SQL =
        "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'Registered')";
        
    // --- NEW SQL QUERY: Update status from 'Registered' to 'Dropped' (Rule #2) ---
    private static final String DROP_COURSE_SQL =
        "UPDATE enrollments SET status = 'Dropped' WHERE student_id = ? AND section_id = ? AND status = 'Registered'";


    /**
     * Checks if a section has available capacity.
     * @return The number of remaining seats, or -1 on error.
     */
    public int getRemainingCapacity(int sectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_CAPACITY_SQL)) {
            
            stmt.setInt(1, sectionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int capacity = rs.getInt("capacity");
                    int enrolledCount = rs.getInt("enrolled_count");
                    return capacity - enrolledCount;
                }
                throw new SQLException("Section ID not found: " + sectionId);
            }
        }
    }

    /**
     * Checks if a student is already enrolled in the section (Registered status).
     * @return true if status is 'Registered', false otherwise.
     */
    public boolean isStudentRegistered(int studentId, int sectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_EXISTING_ENROLLMENT_SQL)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "Registered".equalsIgnoreCase(rs.getString("status"));
                }
                return false;
            }
        }
    }

    /**
     * Fetches the day_time string for all courses a student is currently Registered in.
     */
    public List<String> getStudentScheduleDayTimes(int studentId) throws SQLException {
        List<String> scheduleTimes = new ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_STUDENT_SCHEDULE_SQL)) {
            
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    scheduleTimes.add(rs.getString("day_time"));
                }
            }
        }
        return scheduleTimes;
    }

    /**
     * Executes the enrollment transaction.
     */
    public void registerStudent(int studentId, int sectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(REGISTER_COURSE_SQL)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                 throw new SQLException("Enrollment failed, possibly due to invalid IDs.");
            }
        }
    }
    
    // --- NEW METHOD: dropStudent ---
    /**
     * Executes the course drop transaction by updating the enrollment status to 'Dropped'.
     * This query only succeeds if the status is currently 'Registered'.
     * @return The number of affected rows (should be 1 for a successful drop, 0 otherwise).
     */
    public int dropStudent(int studentId, int sectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(DROP_COURSE_SQL)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            
            // This method returns the number of rows updated (0 or 1)
            return stmt.executeUpdate(); 
        }
    }
}
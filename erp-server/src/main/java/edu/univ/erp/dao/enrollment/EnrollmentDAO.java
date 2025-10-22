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
    
    // Select capacity while acquiring a row lock to prevent concurrent changes
    private static final String SELECT_SECTION_FOR_UPDATE_SQL =
        "SELECT capacity FROM sections WHERE section_id = ? FOR UPDATE";

    // Count current registered students for a section
    private static final String COUNT_REGISTERED_SQL =
        "SELECT COUNT(*) AS enrolled_count FROM enrollments WHERE section_id = ? AND status = 'Registered'";

    // Check whether student is registered in any other section of the same course
    private static final String CHECK_REGISTERED_SAME_COURSE_SQL =
        "SELECT 1 FROM enrollments e " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN course_catalog c ON s.course_code = c.course_code " +
        "WHERE e.student_id = ? AND e.status = 'Registered' AND c.course_code = (" +
        "    SELECT c2.course_code FROM sections s2 JOIN course_catalog c2 ON s2.course_code = c2.course_code WHERE s2.section_id = ?" +
        ") AND s.section_id <> ? LIMIT 1";
        
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
        Connection conn = null;
        PreparedStatement selectSectionStmt = null;
        PreparedStatement countStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnector.getErpConnection();
            conn.setAutoCommit(false);

            // Lock the section row to prevent concurrent capacity reads
            selectSectionStmt = conn.prepareStatement(SELECT_SECTION_FOR_UPDATE_SQL);
            selectSectionStmt.setInt(1, sectionId);
            rs = selectSectionStmt.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                throw new SQLException("Section ID not found: " + sectionId);
            }
            int capacity = rs.getInt("capacity");
            rs.close(); rs = null;

            // Count currently registered students
            countStmt = conn.prepareStatement(COUNT_REGISTERED_SQL);
            countStmt.setInt(1, sectionId);
            rs = countStmt.executeQuery();
            int enrolledCount = 0;
            if (rs.next()) enrolledCount = rs.getInt("enrolled_count");
            rs.close(); rs = null;

            if (enrolledCount >= capacity) {
                conn.rollback();
                throw new SQLException("Section is full. Registration failed.");
            }

            // Insert enrollment
            insertStmt = conn.prepareStatement(REGISTER_COURSE_SQL);
            insertStmt.setInt(1, studentId);
            insertStmt.setInt(2, sectionId);
            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                throw new SQLException("Enrollment failed, possibly due to invalid IDs.");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) { /* ignore */ }
            if (selectSectionStmt != null) try { selectSectionStmt.close(); } catch (SQLException ex) { /* ignore */ }
            if (countStmt != null) try { countStmt.close(); } catch (SQLException ex) { /* ignore */ }
            if (insertStmt != null) try { insertStmt.close(); } catch (SQLException ex) { /* ignore */ }
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
        }
    }

    /**
     * Returns true if the student is already registered in another section of the same course.
     */
    public boolean isRegisteredForSameCourseInAnotherSection(int studentId, int targetSectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_REGISTERED_SAME_COURSE_SQL)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, targetSectionId);
            stmt.setInt(3, targetSectionId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
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
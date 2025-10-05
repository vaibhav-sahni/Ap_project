package edu.univ.erp.data;

import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.UserAuth; // Assuming Student is defined separately
import java.sql.*;

/**
 * Data Access Object (DAO) for retrieving Student profile information 
 * from the ERP database (students table).
 */
public class StudentDAO {

    private static final String FIND_STUDENT_PROFILE_SQL = 
        "SELECT roll_no, program, year FROM students WHERE user_id = ?";

    /**
     * Loads the full Student profile details from the ERP database.
     * * @param userAuth The basic identity object retrieved during the authentication phase.
     * @return A complete Student object, or null if the profile is not found.
     */
    public Student findStudentProfile(UserAuth userAuth) {
        
        // This DAO uses the ERP database connection
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_STUDENT_PROFILE_SQL)) {
            
            // 1. Link Auth identity to ERP profile using the user_id
            stmt.setInt(1, userAuth.getUserId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 2. Build the Student object using the identity data (from UserAuth) 
                    //    and the profile data (from the ResultSet)
                    return new Student(
                        userAuth.getUserId(),
                        userAuth.getUsername(),
                        userAuth.getRole(),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("year")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error during Student profile lookup for ID: " + userAuth.getUserId());
            e.printStackTrace();
        }
        return null; // Profile not found or a database error occurred
    }
}
package edu.univ.erp.dao.student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet; // Assuming Student is defined separately
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.UserAuth;

/**
 * Data Access Object (DAO) for retrieving Student profile information 
 * from the ERP database (students table).
 */
public class StudentDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

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
            LOGGER.log(Level.SEVERE, "DB Error during Student profile lookup for ID: " + userAuth.getUserId(), e);
        }
        return null; // Profile not found or a database error occurred
    }

    private static final String GET_ROLL_NO_SQL = 
        "SELECT roll_no FROM students WHERE user_id = ?";

    public static String getStudentRollNo(int userId) {
         try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ROLL_NO_SQL)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("roll_no");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DB Error during Roll No lookup for ID: " + userId, e);
        }
        return null;
    }
}
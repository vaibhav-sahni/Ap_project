package edu.univ.erp.data;

import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.UserAuth;
import java.sql.*;

public class InstructorDAO {

    // SQL to fetch profile details using the foreign key (user_id)
    private static final String FIND_INSTR_PROFILE_SQL = 
        "SELECT name, department FROM instructors WHERE user_id = ?"; // <-- CORRECTED SQL

    public Instructor findInstructorProfile(UserAuth userAuth) {
        
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_INSTR_PROFILE_SQL)) {
            
            stmt.setInt(1, userAuth.getUserId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Build the Instructor object using the two correct fields
                    return new Instructor(
                        userAuth.getUserId(),
                        userAuth.getUsername(),
                        userAuth.getRole(),
                        rs.getString("name"),         // <-- CORRECTED field retrieval
                        rs.getString("department")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error during Instructor profile lookup for ID: " + userAuth.getUserId());
            e.printStackTrace();
        }
        return null;
    }
}
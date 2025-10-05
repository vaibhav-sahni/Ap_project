package edu.univ.erp.dao.auth;
import edu.univ.erp.dao.db.DBConnector;

import java.sql.*;

public class AuthDAO {
    
    public static record AuthDetails (
    int userID,
    String passwordHash,
    String role
){}

    private static final String FIND_USER_SQL = "SELECT user_id, password_hash, role FROM users_auth WHERE username = ?";

    /**
     * Retrieves authentication details from the Auth DB.
     * @param username The username provided during login.
     * @return AuthDetails object if user is found, otherwise null.
     */
    public AuthDetails findUserByUsername(String username) {
        
        try (Connection conn = DBConnector.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_USER_SQL)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Use the AuthDetails record to return the structured data
                    return new AuthDetails( 
                        rs.getInt("user_id"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            // Log the error and fail securely (return null)
            System.err.println("DB Error during auth lookup for user: " + username + ". " + e.getMessage());
        }
        return null;
    }
}
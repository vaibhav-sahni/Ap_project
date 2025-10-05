package edu.univ.erp.dao.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.univ.erp.dao.db.DBConnector;

public class SettingDAO {

    private static final String GET_MAINTENANCE_SQL = 
        "SELECT setting_value FROM settings WHERE setting_key = 'maintenance_on'";

    /**
     * Checks the current status of the maintenance mode flag in the ERP DB.
     * @return true if maintenance mode is 'true', false otherwise.
     */
    public boolean isMaintenanceModeOn() {
        try (Connection conn = DBConnector.getErpConnection(); // Use ERP Connection
             PreparedStatement stmt = conn.prepareStatement(GET_MAINTENANCE_SQL);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String status = rs.getString("setting_value");
                return "true".equalsIgnoreCase(status);
            }
        } catch (SQLException e) {
            System.err.println("DB Error accessing settings table: " + e.getMessage());
            // Fail safe: If the table or setting isn't found, assume maintenance is OFF
        }
        return false; 
    }
}
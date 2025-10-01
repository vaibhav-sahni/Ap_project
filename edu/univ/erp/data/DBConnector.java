package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    // Configuration for AUTH DB
    private static final String AUTH_DB_URL = "jdbc:mysql://localhost:3306/auth_db";
    private static final String AUTH_DB_USER = "dbuser_auth";
    private static final String AUTH_DB_PASS = "authpass";

    // Configuration for ERP DB
    private static final String ERP_DB_URL = "jdbc:mysql://localhost:3306/erp_db";
    private static final String ERP_DB_USER = "dbuser_erp";
    private static final String ERP_DB_PASS = "erppass";

    /**
     * Gets a connection to the Auth Database.
     */
    public static Connection getAuthConnection() throws SQLException {
        return DriverManager.getConnection(AUTH_DB_URL, AUTH_DB_USER, AUTH_DB_PASS);
    }

    /**
     * Gets a connection to the ERP Database.
     */
    public static Connection getErpConnection() throws SQLException {
        return DriverManager.getConnection(ERP_DB_URL, ERP_DB_USER, ERP_DB_PASS);
    }
}
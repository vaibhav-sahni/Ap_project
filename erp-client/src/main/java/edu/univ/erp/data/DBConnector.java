package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    // Configuration for AUTH DB (login as root)
    //create user 'auth_user'@'localhost' identified by 'auth_pass';
    //grant all privileges on auth_db.* to 'auth_user'@'localhost';
    //create user 'erp_user'@'localhost' identified by 'erp_pass';
    //grant all privileges on erp_db.* to 'erp_user'@'localhost';
    //flush privileges;

    private static final String AUTH_DB_URL = "jdbc:mysql://localhost:3306/auth_db";
    private static final String AUTH_DB_USER = "auth_user";
    private static final String AUTH_DB_PASS = "auth_pass";

    // Configuration for ERP DB
    private static final String ERP_DB_URL = "jdbc:mysql://localhost:3306/erp_db";
    private static final String ERP_DB_USER = "erp_user";
    private static final String ERP_DB_PASS = "erp_pass";

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
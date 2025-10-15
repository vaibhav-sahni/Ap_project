package edu.univ.erp.dao.db;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DBConnector provides pooled JDBC connections for the server.
 * It preserves the existing static API (getAuthConnection/getErpConnection)
 * so DAO code does not need to change.
 */
public final class DBConnector {

    private static final HikariDataSource authDs;
    private static final HikariDataSource erpDs;

    static {
        // Auth DB pool
        HikariConfig authCfg = new HikariConfig();
        authCfg.setJdbcUrl(System.getProperty("erp.auth.jdbcUrl", "jdbc:mysql://localhost:3306/auth_db"));
        authCfg.setUsername(System.getProperty("erp.auth.user", "auth_user"));
        authCfg.setPassword(System.getProperty("erp.auth.pass", "auth_pass"));
        authCfg.setMaximumPoolSize(Integer.parseInt(System.getProperty("erp.auth.maxPool", "5")));
        authCfg.setMinimumIdle(Integer.parseInt(System.getProperty("erp.auth.minIdle", "1")));
        authCfg.setConnectionTimeout(Long.parseLong(System.getProperty("erp.auth.connTimeout", "30000")));
        authCfg.setIdleTimeout(Long.parseLong(System.getProperty("erp.auth.idleTimeout", "300000")));
        authCfg.setMaxLifetime(Long.parseLong(System.getProperty("erp.auth.maxLifetime", "1800000")));
        authCfg.setLeakDetectionThreshold(Long.parseLong(System.getProperty("erp.auth.leakThreshold", "5000")));
        authDs = new HikariDataSource(authCfg);

        // ERP DB pool
        HikariConfig erpCfg = new HikariConfig();
        erpCfg.setJdbcUrl(System.getProperty("erp.jdbcUrl", "jdbc:mysql://localhost:3306/erp_db"));
        erpCfg.setUsername(System.getProperty("erp.user", "erp_user"));
        erpCfg.setPassword(System.getProperty("erp.pass", "erp_pass"));
        erpCfg.setMaximumPoolSize(Integer.parseInt(System.getProperty("erp.maxPool", "10")));
        erpCfg.setMinimumIdle(Integer.parseInt(System.getProperty("erp.minIdle", "2")));
        erpCfg.setConnectionTimeout(Long.parseLong(System.getProperty("erp.connTimeout", "30000")));
        erpCfg.setIdleTimeout(Long.parseLong(System.getProperty("erp.idleTimeout", "300000")));
        erpCfg.setMaxLifetime(Long.parseLong(System.getProperty("erp.maxLifetime", "1800000")));
        erpCfg.setLeakDetectionThreshold(Long.parseLong(System.getProperty("erp.leakThreshold", "5000")));
        // driver tuning
        erpCfg.addDataSourceProperty("cachePrepStmts", "true");
        erpCfg.addDataSourceProperty("prepStmtCacheSize", "250");
        erpCfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        erpDs = new HikariDataSource(erpCfg);
    }

    private DBConnector() {}

    public static Connection getAuthConnection() throws SQLException {
        return authDs.getConnection();
    }

    public static Connection getErpConnection() throws SQLException {
        return erpDs.getConnection();
    }

    public static void shutdown() {
        try {
            if (erpDs != null) erpDs.close();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(DBConnector.class.getName()).warning("Failed to close ERP datasource: " + e.getMessage());
        }
        try {
            if (authDs != null) authDs.close();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(DBConnector.class.getName()).warning("Failed to close AUTH datasource: " + e.getMessage());
        }
    }

    // Expose configured ERP DB connection details for external utilities
    public static String getErpJdbcUrl() {
        return System.getProperty("erp.jdbcUrl", "jdbc:mysql://localhost:3306/erp_db");
    }

    public static String getErpUsername() {
        return System.getProperty("erp.user", "erp_user");
    }

    public static String getErpPassword() {
        return System.getProperty("erp.pass", "erp_pass");
    }
}
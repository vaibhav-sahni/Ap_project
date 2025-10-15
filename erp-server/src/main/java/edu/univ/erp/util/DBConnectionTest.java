package edu.univ.erp.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.univ.erp.dao.db.DBConnector;
// java -cp ".;..\..\..\lib\mysql-connector-j-9.4.0.jar" edu/univ/erp/util/DBConnectionTest.java
public class DBConnectionTest {

    private static final Logger LOGGER = Logger.getLogger(DBConnectionTest.class.getName());

    public static void main(String[] args) {
        LOGGER.info("--- Starting Database Connection Tests ---");
        testConnection("Auth DB", DBConnectionTest::testAuthConnection);
        LOGGER.info("-----------------------------------------");
        testConnection("ERP DB", DBConnectionTest::testErpConnection);
        LOGGER.info("--- Connection Tests Complete ---");
    }

    private static void testConnection(String dbName, ThrowingRunnable testMethod) {
        LOGGER.info(() -> "\nAttempting connection to " + dbName + "...");
        try {
            testMethod.run(); 
            LOGGER.info(() -> "✅ SUCCESS: " + dbName + " connection established successfully.");
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "❌ FAILURE: Could not connect to " + dbName + ". Error: " + e.getMessage(), e);
            LOGGER.severe("   --- CHECK LIST ---");
            LOGGER.severe("   1. Is the DB server running?");
            LOGGER.severe("   2. Are DBs and Users created?");
            LOGGER.severe("   3. Are credentials in DbConnector.java correct?");
        }
    }

    private static void testAuthConnection() throws SQLException {
        try (Connection conn = DBConnector.getAuthConnection()) { 
            // The connection is implicitly closed by try-with-resources
        }
    }

    private static void testErpConnection() throws SQLException {
        try (Connection conn = DBConnector.getErpConnection()) { 
            // The connection is implicitly closed by try-with-resources
        }
    }
}
package edu.univ.erp.util;

import edu.univ.erp.data.DBConnector;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Database Connection Tests ---");
        testConnection("Auth DB", DBConnectionTest::testAuthConnection);
        System.out.println("-----------------------------------------");
        testConnection("ERP DB", DBConnectionTest::testErpConnection);
        System.out.println("--- Connection Tests Complete ---");
    }

    private static void testConnection(String dbName, ThrowingRunnable testMethod) {
        System.out.println("\nAttempting connection to " + dbName + "...");
        try {
            testMethod.run(); 
            System.out.println("✅ SUCCESS: " + dbName + " connection established successfully.");
        } catch (Exception e) { 
            System.err.println("❌ FAILURE: Could not connect to " + dbName + ".");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("   --- CHECK LIST ---");
            System.err.println("   1. Is the DB server running?");
            System.err.println("   2. Are DBs and Users created?");
            System.err.println("   3. Are credentials in DbConnector.java correct?");
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
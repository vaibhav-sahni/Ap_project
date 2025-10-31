package edu.univ.erp.net;

/**
 * Holds the active persistent client connection for the running client app.
 */
public class ClientSession {
    private static ClientConnection conn = null;
    // When true, suppress the session-lost modal/redirect that other network
    // error handlers may fire. This is used for intentional client actions
    // such as explicit logout so the user doesn't see a confusing "connection
    // lost" message during normal logout flows.
    private static volatile boolean suppressSessionLost = false;

    public static synchronized void setConnection(ClientConnection c) {
        conn = c;
    }

    public static synchronized ClientConnection getConnection() {
        return conn;
    }

    public static synchronized void clear() {
        if (conn != null) {
            try { conn.close(); } catch (Exception e) { /* ignore */ }
            conn = null;
        }
    }

    public static void setSuppressSessionLost(boolean s) {
        suppressSessionLost = s;
    }

    public static boolean isSuppressSessionLost() {
        return suppressSessionLost;
    }
}

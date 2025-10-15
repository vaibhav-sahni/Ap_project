package edu.univ.erp.net;

/**
 * Holds the active persistent client connection for the running client app.
 */
public class ClientSession {
    private static ClientConnection conn = null;

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
}

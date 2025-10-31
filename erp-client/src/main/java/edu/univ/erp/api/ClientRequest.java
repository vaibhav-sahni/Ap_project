package edu.univ.erp.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import edu.univ.erp.net.ClientConnection;
import edu.univ.erp.net.ClientSession;
import edu.univ.erp.ui.util.SessionLostNotifier;

public class ClientRequest {
    private static final String SERVER_HOST = "localhost"; 
    private static final int SERVER_PORT = 9090;
    // gson was previously declared for convenience but is not required in this helper

    /**
     * Sends a request string to the server and returns the raw response string.
     * @param request The full request string (e.g., "LOGIN:user:pass").
     * @return The raw response string (e.g., "SUCCESS:{json}").
     * @throws Exception If a network error or server-side error occurs.
     */
    public static String send(String request) throws Exception {
        // If a persistent connection exists (created during login), reuse it
        ClientConnection conn = ClientSession.getConnection();
        if (conn != null) {
            try {
                return conn.send(request);
            } catch (Exception e) {
                // Decide whether this was a network/connection failure (clear session) or a server-side application error (don't clear)
                String msg = e.getMessage() == null ? "" : e.getMessage();
                boolean isNetworkFailure = false;
                // Common network/connection failure messages produced by ClientConnection
                if (msg.contains("Server closed connection") || msg.toLowerCase().contains("connection reset") || msg.toLowerCase().contains("broken pipe")) {
                    isNetworkFailure = true;
                }
                // Also consider underlying IOExceptions
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof java.io.IOException || cause instanceof java.net.SocketException) { isNetworkFailure = true; break; }
                    cause = cause.getCause();
                }

                if (isNetworkFailure) {
                    try { ClientSession.clear(); } catch (Exception ignore) {}
                    System.err.println("CLIENT WARN: persistent connection failed, session cleared: " + e.getMessage());
                    // Only show the session-lost notifier if not suppressed by an intentional action
                    try {
                        if (!ClientSession.isSuppressSessionLost()) SessionLostNotifier.notifyAndRedirect();
                    } catch (Exception ignore) {}
                    throw new Exception("Persistent session lost. Please login again before retrying this operation.", e);
                }

                // If the server signaled maintenance or authentication loss, treat this as session loss.
                String serverMsg = e.getMessage() == null ? "" : e.getMessage();
                if (serverMsg.contains("MAINTENANCE_ON") || serverMsg.contains("NOT_AUTHENTICATED") || serverMsg.contains("NOT_AUTH")) {
                    try { ClientSession.clear(); } catch (Exception ignore) {}
                    System.err.println("CLIENT WARN: server requested session clear due to maintenance/auth: " + serverMsg);
                    // Only show the session-lost notifier if not suppressed by an intentional action
                    try {
                        if (!ClientSession.isSuppressSessionLost()) SessionLostNotifier.notifyAndRedirect();
                    } catch (Exception ignore) {}
                    throw new Exception("Session invalidated: " + serverMsg, e);
                }

                // Server-side application error; do not clear the persistent session. Surface the server message to caller.
                System.err.println("CLIENT ERROR: server-side error (session kept): " + e.getMessage());
                throw e;
            }
        }

       try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
           PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
           BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
       ) {
          // ensure we don't block forever waiting for a server response
          try { socket.setSoTimeout(10000); } catch (Exception ignore) {}
            out.println(request);
            String response = in.readLine();

            if (response == null) {
                throw new Exception("Server did not respond or connection closed.");
            }

            // Handle SUCCESS or ERROR protocol here
            if (response.startsWith("ERROR:")) {
                String errorMessage = response.substring("ERROR:".length());
                // If server indicates maintenance or unauthenticated, clear persistent session and redirect to login
                if (errorMessage.startsWith("NOT_AUTHENTICATED") || errorMessage.startsWith("NOT_AUTH")) {
                    try { ClientSession.clear(); } catch (Exception ignore) {}
                    try { SessionLostNotifier.notifyAndRedirect(); } catch (Exception ignore) {}
                    throw new Exception(errorMessage);
                }
                throw new Exception(errorMessage);
            }
            // Returns raw SUCCESS:{json} or SUCCESS:OK
            return response; 

        } catch (IOException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (msg.toLowerCase().contains("timed out") || msg.toLowerCase().contains("sockettimeout")) {
                throw new Exception("Server did not respond in time (timeout). Try again.", e);
            }
            throw new Exception("Could not connect to ERP Server. Is the server running?", e);
        }
    }
}
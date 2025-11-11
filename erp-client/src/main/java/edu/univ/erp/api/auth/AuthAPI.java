package edu.univ.erp.api.auth;

import com.google.gson.Gson;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.net.ClientConnection;
import edu.univ.erp.net.ClientSession; 

public class AuthAPI {
    private final Gson gson = new Gson();

    public UserAuth login(String username, String password) throws Exception {
        // 1. Build the specific command for this API
        String request = "LOGIN:" + username + ":" + password;

        // For per-connection session model, open a persistent connection and send LOGIN over it.
        ClientConnection conn = null;
        String response;
        // Show a simple progress dialog during the blocking login call
        final edu.univ.erp.ui.components.ProgressDialog[] dlgRef = new edu.univ.erp.ui.components.ProgressDialog[1];
        try {
            // Create and show dialog on EDT. invokeAndWait throws if we are already on EDT,
            // so handle both cases.
            final java.awt.Frame owner = new java.awt.Frame();
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                dlgRef[0] = new edu.univ.erp.ui.components.ProgressDialog(owner, "Logging in...", "Connecting to server and authenticating...");
                dlgRef[0].showDialog();
            } else {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    dlgRef[0] = new edu.univ.erp.ui.components.ProgressDialog(owner, "Logging in...", "Connecting to server and authenticating...");
                    dlgRef[0].showDialog();
                });
            }

            conn = new ClientConnection("localhost", 9090);
            response = conn.send(request);
        } catch (Exception e) {
            if (conn != null) try { conn.close(); } catch (Exception ex) { /* ignore */ }
            throw e;
        } finally {
            // ensure the dialog is closed on EDT
            final edu.univ.erp.ui.components.ProgressDialog finalDlg = dlgRef[0];
            try {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (finalDlg != null) finalDlg.close();
                });
            } catch (Exception ignore) {}
        }

        // 3. Process the SUCCESS response
        if (response.startsWith("SUCCESS:")) {
            String userJson = response.substring("SUCCESS:".length());
            // Deserialize the JSON string back into a UserAuth object
            UserAuth user = gson.fromJson(userJson, UserAuth.class);
            // Persist the connection for this client session
            ClientSession.setConnection(conn);
            return user;
        } 
        
        // any unexpected path: close connection if opened
        if (conn != null) try { conn.close(); } catch (Exception ex) { /* ignore */ }
        throw new Exception("Unexpected response during login.");
    }

    /**
     *  Sends a request to the server to change the user's password.
     * @param userId The ID of the currently logged-in user.
     * @param oldPassword The current raw password.
     * @param newPassword The new raw password.
     * @return The success message from the server.
     * @throws Exception If the server returns an ERROR (e.g., old password mismatch, maintenance mode, etc.).
     */
    public String changePassword(int userId, String oldPassword, String newPassword) throws Exception {
        // Command format: CHANGE_PASSWORD:userId:oldPassword:newPassword
        String request = String.format("CHANGE_PASSWORD:%d:%s:%s", userId, oldPassword, newPassword);
        
        // Use the generic sender, which throws an Exception on ERROR
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length());
        } 
        
        // This is a safety net, as ClientRequest.send() should handle errors
        throw new Exception("Password change failed due to an unexpected client error."); 
    }

    /**
     * Request an unauthenticated password reset: sends the username and desired new password
     * to the server which will notify the configured admin.
     */
    public String requestPasswordReset(String username, String newPassword) throws Exception {
        if (username == null || username.trim().isEmpty()) throw new Exception("Username required.");
        if (newPassword == null || newPassword.length() < 6) throw new Exception("New password must be at least 6 characters.");
        String request = String.format("RESET_PASSWORD:%s:%s", username, newPassword);
        String response = ClientRequest.send(request);
        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length());
        }
        throw new Exception("Unexpected response from server.");
    }

    /**
     * Logout: closes persistent connection and clears client-side session.
     */
    public String logout() throws Exception {
        // Send LOGOUT over persistent connection if present
        edu.univ.erp.net.ClientConnection conn = edu.univ.erp.net.ClientSession.getConnection();
        if (conn != null) {
            // Suppress the session-lost notifier while we perform an intentional
            // logout so the user does not see the "Connection lost" modal.
            edu.univ.erp.net.ClientSession.setSuppressSessionLost(true);
            try {
                String resp = conn.send("LOGOUT");
                // clear local session
                edu.univ.erp.net.ClientSession.clear();
                return resp.startsWith("SUCCESS:") ? resp.substring("SUCCESS:".length()) : resp;
            } finally {
                // Re-enable notifier for subsequent unexpected disconnects
                edu.univ.erp.net.ClientSession.setSuppressSessionLost(false);
            }
        }
        return "SUCCESS:No active session";
    }
}
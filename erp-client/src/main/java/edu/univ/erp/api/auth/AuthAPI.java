package edu.univ.erp.api.auth;

import com.google.gson.Gson;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.UserAuth; 

public class AuthAPI {
    private final Gson gson = new Gson();

    public UserAuth login(String username, String password) throws Exception {
        // 1. Build the specific command for this API
        String request = "LOGIN:" + username + ":" + password;

        // 2. Use the generic sender
        String response = ClientRequest.send(request);

        // 3. Process the SUCCESS response
        if (response.startsWith("SUCCESS:")) {
            String userJson = response.substring("SUCCESS:".length());
            // Deserialize the JSON string back into a UserAuth object
            return gson.fromJson(userJson, UserAuth.class); 
        } 
        
        // Note: ClientRequest.send() handles throwing the 'ERROR:' exception.
        throw new Exception("Unexpected response during login.");
    }

    /**
     * NEW: Sends a request to the server to change the user's password.
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
}
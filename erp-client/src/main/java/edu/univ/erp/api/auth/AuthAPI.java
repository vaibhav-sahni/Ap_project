package edu.univ.erp.api.auth;

import com.google.gson.Gson;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.UserAuth; // Import the new utility

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
        // If we reach here, something unexpected happened.
        throw new Exception("Unexpected response during login.");
    }
}
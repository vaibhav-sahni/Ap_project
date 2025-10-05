package edu.univ.erp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.service.auth.AuthService;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override // <-- This is the required method signature!
    public void run() { 
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true) 
        ) {
            String request = in.readLine(); 
            String response = processRequest(request); // Route the request
            out.println(response);

        } catch (IOException e) {
            System.err.println("SERVER LOG: ClientHandler network error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* ignored */ }
        }
    }
    
    private String processRequest(String request) {
        try {
            if (request == null) return "ERROR:NO_REQUEST";
            
            String[] parts = request.split(":");
            String command = parts[0].toUpperCase();
            
            switch (command) {
                case "LOGIN":
                    return handleLogin(parts); // Handle Auth
                // FUTURE: case "GET_GRADES": return handleGetGrades(parts);
                default:
                    return "ERROR:UNKNOWN_COMMAND";
            }
        } catch (Exception e) {
            // Catch service-layer errors (like "Invalid password.")
            return "ERROR:" + e.getMessage();
        }
    }
    
    private String handleLogin(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing username or password.");

        String username = parts[1];
        String password = parts[2];
        
        AuthService authService = new AuthService();
        UserAuth user = authService.authenticate(username, password);
        
        // Convert the UserAuth object to JSON (using Gson)
        String userJson = gson.toJson(user);
        return "SUCCESS:" + userJson;
    }
}
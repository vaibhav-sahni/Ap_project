package edu.univ.erp.server;

import com.google.gson.Gson;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.service.auth.AuthService;
import edu.univ.erp.service.student.StudentService;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) { this.clientSocket = socket; }

    @Override
    public void run() { 
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true) 
        ) {
            String request = in.readLine(); 
            String response = processRequest(request);
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
            
            // CENTRAL REQUEST ROUTER
            switch (command) {
                case "LOGIN":
                    return handleLogin(parts);
                case "GET_GRADES":
                    return handleGetGrades(parts);
                default:
                    return "ERROR:UNKNOWN_COMMAND";
            }
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
    
    private String handleLogin(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing username or password.");
        String username = parts[1];
        String password = parts[2];
        
        AuthService authService = new AuthService();
        UserAuth user = authService.authenticate(username, password);
        
        String userJson = gson.toJson(user);
        return "SUCCESS:" + userJson;
    }
    
    private String handleGetGrades(String[] parts) throws Exception {
        if (parts.length < 2) throw new Exception("Missing user ID for grades request.");
        
        // Ensure user ID is passed as an integer from the client
        int userId = Integer.parseInt(parts[1]); 

        StudentService studentService = new StudentService();
        List<Grade> grades = studentService.fetchGrades(userId);
        
        String gradesJson = gson.toJson(grades); 
        return "SUCCESS:" + gradesJson;
    }
}
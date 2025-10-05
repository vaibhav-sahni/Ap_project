package edu.univ.erp.api;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class ClientRequest {
    private static final String SERVER_HOST = "localhost"; 
    private static final int SERVER_PORT = 9090;
    private static final Gson gson = new Gson();

    /**
     * Sends a request string to the server and returns the raw response string.
     * @param request The full request string (e.g., "LOGIN:user:pass").
     * @return The raw response string (e.g., "SUCCESS:{json}").
     * @throws Exception If a network error or server-side error occurs.
     */
    public static String send(String request) throws Exception {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(request);
            String response = in.readLine();
            
            if (response == null) {
                throw new Exception("Server did not respond or connection closed.");
            }
            
            // Handle SUCCESS or ERROR protocol here
            if (response.startsWith("ERROR:")) {
                String errorMessage = response.substring("ERROR:".length());
                throw new Exception(errorMessage);
            }
            // Returns raw SUCCESS:{json} or SUCCESS:OK
            return response; 

        } catch (IOException e) {
            throw new Exception("Could not connect to ERP Server. Is the server running?", e);
        }
    }
}
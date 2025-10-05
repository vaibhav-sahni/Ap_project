package edu.univ.erp.server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 9090;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ ERP SERVER STARTED. Listening on port " + PORT + "...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("SERVER LOG: New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Handle the client request in a new thread
                new Thread(new ClientHandler(clientSocket)).start(); 
            }
        } catch (Exception e) {
            System.err.println("❌ Server critical exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
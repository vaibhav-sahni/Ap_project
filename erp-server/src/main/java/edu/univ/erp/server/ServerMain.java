package edu.univ.erp.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {
    private static final int PORT = 9090;
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        // register shutdown hook to close pooled datasources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("SERVER LOG: Shutting down, closing DB pools...");
            try { edu.univ.erp.dao.db.DBConnector.shutdown(); } catch (Exception ex) { LOGGER.log(Level.WARNING, "Failed to shutdown DBConnector", ex); }
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("ERP SERVER STARTED. Listening on port " + PORT + "...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept(); 
                LOGGER.info("SERVER LOG: New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Handle the client request in a new thread
                new Thread(new ClientHandler(clientSocket)).start(); 
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server critical exception: " + e.getMessage(), e);
        }
    }
}
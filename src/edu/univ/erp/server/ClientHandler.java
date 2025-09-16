package edu.univ.erp.server;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;

    // Constructor: assign client socket
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            // Streams to read/write Java objects
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ) {
            Object request;

            // Keep reading requests from client until they disconnect
            while ((request = in.readObject()) != null) {
                System.out.println("Received request: " + request);

                // TODO: call service layer based on request
                // For now, just echo back
                String response = "Server received: " + request.toString();
                out.writeObject(response);
                out.flush();
            }

        } catch (Exception e) {
            // Client disconnected or error occurred
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
        } finally {
            try {
                clientSocket.close(); // Close socket
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}

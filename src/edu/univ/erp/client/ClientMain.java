package edu.univ.erp.client;

import java.io.*;
import java.net.Socket;

public class ClientMain {

    private static final String SERVER_HOST = "localhost"; // server IP
    private static final int SERVER_PORT = 6000;           // must match server

    public static void main(String[] args) {
        try (
            // Connect to server
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ) {
            System.out.println("Connected to server.");

            // Send a simple test request
            out.writeObject("Hello server!");
            out.flush();

            // Receive response from server
            Object response = in.readObject();
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package edu.univ.erp.server;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 6000;
    public static void main(String[] args) {
        try {
            ServerSocket socket = new ServerSocket(PORT);
            System.out.printf("Server started on port %d%n",PORT);
            while (true) { 
                Socket client = socket.accept();
                System.out.println("New client connected : "+client.getInetAddress());
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

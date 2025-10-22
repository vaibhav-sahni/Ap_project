package edu.univ.erp.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Simple persistent client connection for per-connection session model.
 */
public class ClientConnection implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    public ClientConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        // Set a socket read timeout so blocking reads do not hang indefinitely (10s)
        try {
            this.socket.setSoTimeout(10000); // 10 seconds
        } catch (IOException ignore) {
            // Best-effort; the socket may be closed or unsupported in some envs
        }
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public synchronized String send(String request) throws Exception {
        out.println(request);
        String response = in.readLine();
        if (response == null) throw new Exception("Server closed connection");
        if (response.startsWith("ERROR:")) {
            throw new Exception(response.substring("ERROR:".length()));
        }
        return response;
    }

    @Override
    public void close() throws Exception {
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}

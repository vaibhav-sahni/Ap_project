package edu.univ.erp.tools;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.api.auth.AuthAPI;

/**
 * Simple CLI tool to download or upload DB backups via the socket protocol.
 * Usage:
 *  java ClientDbBackupTool download <outfile.gz>
 *  java ClientDbBackupTool restore <infile.gz>
 */
public class ClientDbBackupTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: download|restore ...");
            System.exit(2);
        }
        String cmd = args[0];
        AuthAPI auth = new AuthAPI();

        // Optionally login first - assumes credentials are provided via env for convenience
        String username = System.getenv("ERP_USER");
        String password = System.getenv("ERP_PASS");
        if (username != null && password != null) {
            try { auth.login(username, password); } catch (Exception e) { System.err.println("Login failed: " + e.getMessage()); }
        }

        if ("download".equalsIgnoreCase(cmd)) {
            if (args.length < 2) { System.err.println("download <outfile.gz>"); System.exit(2); }
            String outfile = args[1];
            String resp = ClientRequest.send("DB_BACKUP");
            if (!resp.startsWith("FILE_DOWNLOAD:")) throw new Exception("Unexpected response: " + resp);
            // Format: FILE_DOWNLOAD:contentType:filename:BASE64:<payload>
            String[] parts = resp.split(":", 5);
            if (parts.length < 5) throw new Exception("Malformed FILE_DOWNLOAD response: " + resp);
            String payload = parts[4];
            byte[] decoded = Base64.getDecoder().decode(payload);
            Files.write(Paths.get(outfile), decoded);
            System.out.println("Saved DB backup to " + outfile);

        } else if ("restore".equalsIgnoreCase(cmd)) {
            if (args.length < 2) { System.err.println("restore <infile.gz>"); System.exit(2); }
            String infile = args[1];
            byte[] data = Files.readAllBytes(Paths.get(infile));
            String b64 = Base64.getEncoder().encodeToString(data);
            String request = "DB_RESTORE:BASE64:" + b64;
            String resp = ClientRequest.send(request);
            System.out.println(resp);
        } else {
            System.err.println("Unknown command: " + cmd);
        }

        try { auth.logout(); } catch (Exception e) { /* ignore */ }
    }
}

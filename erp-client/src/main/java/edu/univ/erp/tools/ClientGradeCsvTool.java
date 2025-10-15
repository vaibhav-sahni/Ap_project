package edu.univ.erp.tools;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.api.auth.AuthAPI;

/**
 * Simple CLI tool to export or import grades CSV via the socket protocol.
 * Usage:
 *  java ClientGradeCsvTool export <instructorId> <sectionId> <outfile>
 *  java ClientGradeCsvTool import <instructorId> <sectionId> <infile>
 */
public class ClientGradeCsvTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: export|import ...");
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

        if ("export".equalsIgnoreCase(cmd)) {
            if (args.length < 4) { System.err.println("export <instructorId> <sectionId> <outfile>"); System.exit(2); }
            String instructorId = args[1];
            String sectionId = args[2];
            String outfile = args[3];
            String resp = ClientRequest.send("EXPORT_GRADES:" + instructorId + ":" + sectionId);
            if (!resp.startsWith("FILE_DOWNLOAD:")) throw new Exception("Unexpected response: " + resp);
            // Format: FILE_DOWNLOAD:contentType:filename:BASE64:<payload>
            String[] parts = resp.split(":", 5);
            String payload = parts[4];
            byte[] decoded = Base64.getDecoder().decode(payload);
            Files.write(Paths.get(outfile), decoded);
            System.out.println("Saved CSV to " + outfile);
        } else if ("import".equalsIgnoreCase(cmd)) {
            if (args.length < 4) { System.err.println("import <instructorId> <sectionId> <infile>"); System.exit(2); }
            String instructorId = args[1];
            String sectionId = args[2];
            String infile = args[3];
            byte[] data = Files.readAllBytes(Paths.get(infile));
            String b64 = Base64.getEncoder().encodeToString(data);
            String request = "IMPORT_GRADES:" + instructorId + ":" + sectionId + ":BASE64:" + b64;
            String resp = ClientRequest.send(request);
            System.out.println(resp);
        } else {
            System.err.println("Unknown command: " + cmd);
        }

        try { auth.logout(); } catch (Exception e) { /* ignore */ }
    }
}

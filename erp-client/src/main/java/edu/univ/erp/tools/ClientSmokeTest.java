package edu.univ.erp.tools;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.api.auth.AuthAPI;

public class ClientSmokeTest {
    public static void main(String[] args) {
        AuthAPI auth = new AuthAPI();
        try {
            System.out.println("--- CHECK_MAINTENANCE ---");
            String status = ClientRequest.send("CHECK_MAINTENANCE");
            System.out.println("Response: " + status);

            System.out.println("--- ATTEMPT CREATE_STUDENT (test payload) ---");
            // userId: 99999, username: smoke_test, role: Admin (server will accept any role string here), rollNo, program, year, password
            String createReq = "CREATE_STUDENT:1234:smoke_test:Student:RT-000:TestProg:1:tmpPass";
            try {
                String createResp = ClientRequest.send(createReq);
                System.out.println("CREATE Response: " + createResp);
            } catch (Exception e) {
                System.out.println("CREATE Error (caught): " + e.getMessage());
            }

            // Clean up any persistent connection by logging out if present
            try {
                String out = auth.logout();
                System.out.println("Logout: " + out);
            } catch (Exception le) {
                // Not fatal for the smoke test
                System.err.println("Logout failed: " + le.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Smoke test failed: " + e.getMessage());
            java.util.logging.Logger.getLogger(ClientSmokeTest.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
        }
    }
}

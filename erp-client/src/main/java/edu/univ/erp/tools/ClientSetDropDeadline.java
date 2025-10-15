package edu.univ.erp.tools;

import edu.univ.erp.api.admin.AdminAPI;
import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.domain.UserAuth;

public class ClientSetDropDeadline {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ClientSetDropDeadline <adminUserId> YYYY-MM-DD");
            System.exit(2);
        }

        String username = args[0];
        String password = args[1];
        String iso = args[2];

        AuthAPI auth = new AuthAPI();
        try {
            UserAuth user = auth.login(username, password);
            System.out.println("Logged in as " + user.getUsername() + " (role=" + user.getRole() + ")");

            AdminAPI admin = new AdminAPI();
            String resp = admin.setDropDeadline(iso);
            System.out.println("OK: " + resp);
            // Logout to clean up persistent connection
            try {
                String out = auth.logout();
                System.out.println("Logout: " + out);
            } catch (Exception le) {
                System.err.println("Logout failed: " + le.getMessage());
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}

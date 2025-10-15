package edu.univ.erp.tools;

import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.api.instructor.InstructorAPI;

/**
 * Quick test to fetch assigned sections for an instructor.
 * Usage: java QuickInstructorSectionsTest [instructorId]
 * It will attempt to login using ERP_USER / ERP_PASS env vars if present.
 */
public class QuickInstructorSectionsTest {
    public static void main(String[] args) {
        int instructorId = 1;
        if (args.length > 0) {
            try { instructorId = Integer.parseInt(args[0]); } catch (Exception e) { /* use default */ }
        }

        AuthAPI auth = new AuthAPI();
        String user = System.getenv("ERP_USER");
        String pass = System.getenv("ERP_PASS");
        try {
            if (user != null && pass != null) {
                System.out.println("Attempting login as " + user);
                auth.login(user, pass);
                System.out.println("Login succeeded (via env vars)");
            } else {
                System.out.println("No ERP_USER/ERP_PASS env vars set — proceeding without login (will likely fail authorization)");
            }

            InstructorAPI api = new InstructorAPI();
            System.out.println("Requesting assigned sections for instructorId=" + instructorId);
            java.util.List<?> sections = api.getAssignedSections(instructorId);
            System.out.println("SUCCESS: Retrieved sections: " + sections);

        } catch (Exception e) {
            System.err.println("ERROR calling GET_INSTRUCTOR_SECTIONS: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            try { auth.logout(); } catch (Exception ignore) {}
        }
    }
}

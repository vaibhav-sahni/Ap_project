package edu.univ.erp.tools;

import javax.swing.SwingUtilities;

public class StudentDashboardPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // demo UserAuth (id, username, role)
            edu.univ.erp.domain.UserAuth user = new edu.univ.erp.domain.UserAuth(1, "demoStudent", "Student");
            edu.univ.erp.ui.preview.StudentDashboardFrame frame = new edu.univ.erp.ui.preview.StudentDashboardFrame(user);
            frame.setVisible(true);
        });
    }
}

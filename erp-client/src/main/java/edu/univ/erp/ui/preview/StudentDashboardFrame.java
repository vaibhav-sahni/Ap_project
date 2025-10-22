package edu.univ.erp.ui.preview;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;

public class StudentDashboardFrame extends JFrame {
    private DashboardController controller;

    public StudentDashboardFrame(UserAuth user) {
        super("Student Dashboard");
        setLayout(new FlowLayout());
        setSize(800, 120);
        // Perform graceful logout on window close to avoid abrupt socket resets
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (controller != null) controller.handleLogoutClick();
                else {
                    try { new edu.univ.erp.api.auth.AuthAPI().logout(); } catch (Exception ignore) {}
                    dispose();
                }
            }
        });

    JButton catalog = new JButton("Catalog");
        JButton grades = new JButton("My Grades");
        JButton register = new JButton("Register Course");
        JButton drop = new JButton("Drop Course");
        JButton timetable = new JButton("Timetable");
        JButton transcript = new JButton("Download Transcript");
        JButton exit = new JButton("Logout");

    catalog.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
        grades.addActionListener(e -> new GradesPreviewFrame(user).setVisible(true));
        timetable.addActionListener(e -> {
            if (controller != null) controller.fetchAndDisplayTimetable();
            else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).displayTimetable();
        });

        // Register Course: show dropdown of available sections (catalog) and register selected
        register.addActionListener(e -> {
            try {
                java.util.List<edu.univ.erp.domain.CourseCatalog> catalogList = new edu.univ.erp.ui.handlers.StudentUiHandlers(user).fetchCatalog();

                if (catalogList == null || catalogList.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "No available courses to register.", "Register", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                String[] options = new String[catalogList.size()];
                for (int i = 0; i < catalogList.size(); i++) {
                    edu.univ.erp.domain.CourseCatalog c = catalogList.get(i);
                    options[i] = c.getSectionId() + "  - " + c.getCourseCode() + " : " + c.getCourseTitle() + " (" + c.getInstructorName() + ")";
                }

                String chosen = (String) javax.swing.JOptionPane.showInputDialog(StudentDashboardFrame.this, "Select section to register:", "Register Course", javax.swing.JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                if (chosen == null) return; // cancelled

                // parse leading section id
                String[] parts = chosen.split("\\s+", 2);
                int sectionId = Integer.parseInt(parts[0].trim());

                int confirm = javax.swing.JOptionPane.showConfirmDialog(StudentDashboardFrame.this, "Register for " + chosen + "?", "Confirm Register", javax.swing.JOptionPane.YES_NO_OPTION);
                if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

                try {
                    if (controller != null) controller.handleRegisterCourseClick(sectionId);
                    else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).handleRegisterCourseClick(sectionId);

                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "Registration completed.", "Register", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    if (controller != null) controller.fetchAndDisplayTimetable();
                    else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).refreshCatalogAndTimetable();
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, ex.getMessage(), "Register Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, ex.getMessage(), "Register Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        // Drop Course: present a dropdown of currently registered sections (student timetable)
        drop.addActionListener(e -> {
            try {
                java.util.List<edu.univ.erp.domain.CourseCatalog> timetableList = null;
                if (controller != null) {
                    timetableList = new edu.univ.erp.ui.handlers.StudentUiHandlers(user).fetchTimetable();
                } else {
                    timetableList = new edu.univ.erp.ui.handlers.StudentUiHandlers(user).fetchTimetable();
                }

                if (timetableList == null || timetableList.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "You are not registered for any courses.", "Drop Course", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                String[] options = new String[timetableList.size()];
                for (int i = 0; i < timetableList.size(); i++) {
                    edu.univ.erp.domain.CourseCatalog c = timetableList.get(i);
                    options[i] = c.getSectionId() + "  - " + c.getCourseCode() + " : " + c.getCourseTitle() + " (" + c.getInstructorName() + ")";
                }

                String chosen = (String) javax.swing.JOptionPane.showInputDialog(StudentDashboardFrame.this, "Select section to drop:", "Drop Course", javax.swing.JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                if (chosen == null) return; // cancelled

                String[] parts = chosen.split("\\s+", 2);
                int sectionId = Integer.parseInt(parts[0].trim());

                int confirm = javax.swing.JOptionPane.showConfirmDialog(StudentDashboardFrame.this, "Are you sure you want to drop " + chosen + "? This action is final.", "Confirm Drop", javax.swing.JOptionPane.YES_NO_OPTION);
                if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

                try {
                    if (controller != null) {
                        controller.handleDropCourseClick(sectionId);
                    } else {
                        new edu.univ.erp.ui.handlers.StudentUiHandlers(user).handleDropCourseClick(sectionId);
                    }
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "Course drop completed.", "Drop Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    if (controller != null) controller.fetchAndDisplayTimetable();
                    else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).refreshCatalogAndTimetable();
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, ex.getMessage(), "Drop Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, ex.getMessage(), "Drop Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        transcript.addActionListener(e -> new edu.univ.erp.ui.handlers.StudentUiHandlers(user).downloadTranscriptAndSave(StudentDashboardFrame.this));
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

    // Last login label (if available on the UserAuth object)
    if (user != null && user.getLastLogin() != null) {
        javax.swing.JLabel lastLoginLabel = new javax.swing.JLabel("Last login: " + user.getLastLogin());
        add(lastLoginLabel);
    }
    // CGPA and credits label (will be populated asynchronously)
    javax.swing.JLabel cgpaLabel = new javax.swing.JLabel("CGPA: N/A  | Credits Earned: 0");
    add(cgpaLabel);
    if (user != null) {
        // Fetch CGPA off-EDT to avoid blocking UI
        new Thread(() -> {
            try {
                edu.univ.erp.ui.actions.StudentActions actions = new edu.univ.erp.ui.actions.StudentActions();
                edu.univ.erp.api.student.StudentAPI.CgpaResponse resp = actions.getCgpa(user.getUserId());
                String displayCgpa = resp.cgpa == null ? "N/A" : String.format("%.2f", resp.cgpa);
                String creds = String.format("%.2f", resp.totalCreditsEarned == null ? 0.0 : resp.totalCreditsEarned);
                javax.swing.SwingUtilities.invokeLater(() -> cgpaLabel.setText("CGPA: " + displayCgpa + "  | Credits Earned: " + creds));
            } catch (Exception ex) {
                // Log exception and show N/A
                ex.printStackTrace(System.err);
                javax.swing.SwingUtilities.invokeLater(() -> cgpaLabel.setText("CGPA: N/A  | Credits Earned: 0"));
            }
        }).start();
    }
    add(catalog); add(grades); add(register); add(drop); add(timetable); add(transcript); add(exit);
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

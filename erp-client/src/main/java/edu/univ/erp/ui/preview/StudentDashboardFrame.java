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
        drop.addActionListener(e -> {
            // Prompt for section ID and call the drop handler
            String input = javax.swing.JOptionPane.showInputDialog(StudentDashboardFrame.this, "Enter section ID to drop:", "Drop Course", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (input == null) return; // cancelled
            int sectionId;
            try {
                sectionId = Integer.parseInt(input.trim());
            } catch (NumberFormatException nfe) {
                javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "Invalid section ID.", "Input Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirm final action
            int confirm = javax.swing.JOptionPane.showConfirmDialog(StudentDashboardFrame.this, "Are you sure you want to drop section " + sectionId + "? This action is final.", "Confirm Drop", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

            try {
                if (controller != null) {
                    controller.handleDropCourseClick(sectionId);
                } else {
                    new edu.univ.erp.ui.handlers.StudentUiHandlers(user).handleDropCourseClick(sectionId);
                }
                javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "Course drop request completed.", "Drop Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                // Refresh timetable/catalog display if controller is present
                if (controller != null) controller.fetchAndDisplayTimetable();
                else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).refreshCatalogAndTimetable();
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, ex.getMessage(), "Drop Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        transcript.addActionListener(e -> new edu.univ.erp.ui.handlers.StudentUiHandlers(user).downloadTranscriptAndSave(StudentDashboardFrame.this));
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

    add(catalog); add(grades); add(drop); add(timetable); add(transcript); add(exit);
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

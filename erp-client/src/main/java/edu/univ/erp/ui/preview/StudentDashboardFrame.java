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
        JButton timetable = new JButton("Timetable");
        JButton transcript = new JButton("Download Transcript");
        JButton exit = new JButton("Logout");

        catalog.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
        grades.addActionListener(e -> new GradesPreviewFrame(user).setVisible(true));
        timetable.addActionListener(e -> {
            if (controller != null) controller.fetchAndDisplayTimetable();
            else new edu.univ.erp.ui.handlers.StudentUiHandlers(user).displayTimetable();
        });
        transcript.addActionListener(e -> new edu.univ.erp.ui.handlers.StudentUiHandlers(user).downloadTranscriptAndSave(StudentDashboardFrame.this));
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

        add(catalog); add(grades); add(timetable); add(transcript); add(exit);
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

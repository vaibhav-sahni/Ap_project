package edu.univ.erp.ui.preview;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.univ.erp.domain.UserAuth;

public class DashboardPreviewFrame extends JFrame {
    public DashboardPreviewFrame(UserAuth user) {
        super("ERP Dashboard Preview");
        setLayout(new FlowLayout());
        setSize(400, 120);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        String role = user == null ? "" : user.getRole();

        // Student dashboard
        if ("Student".equals(role)) {
            JButton catalog = new JButton("Catalog");
            JButton grades = new JButton("My Grades");
            JButton timetable = new JButton("Timetable");
            JButton transcript = new JButton("Download Transcript");
            JButton exit = new JButton("Logout");

            catalog.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
            grades.addActionListener(e -> new GradesPreviewFrame(user).setVisible(true));
            timetable.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true)); // reuse catalog for timetable preview
            transcript.addActionListener(e -> new edu.univ.erp.ui.handlers.StudentUiHandlers(user).downloadTranscriptAndSave(DashboardPreviewFrame.this));
            exit.addActionListener(e -> dispose());

            add(catalog); add(grades); add(timetable); add(transcript); add(exit);
            return;
        }

        // Instructor dashboard
        if ("Instructor".equals(role)) {
            JButton assigned = new JButton("Assigned Sections");
            JButton roster = new JButton("View Roster (section 1)");
            JButton finalize = new JButton("Compute Final Grades (section 1)");
            JButton exit = new JButton("Logout");
            final int uid = user == null ? -1 : user.getUserId();
            assigned.addActionListener(e -> {
                // display assigned sections via handler (prints/returns list)
                new edu.univ.erp.ui.handlers.InstructorUiHandlers(user).displayAssignedSections(uid);
            });
            roster.addActionListener(e -> new RosterPreviewFrame(user, 1).setVisible(true));
            finalize.addActionListener(e -> new edu.univ.erp.ui.handlers.InstructorUiHandlers(user).computeFinalGradesWithUi(uid, 1));
            exit.addActionListener(e -> dispose());

            add(assigned); add(roster); add(finalize); add(exit);
            return;
        }

        // Admin dashboard
        if ("Admin".equals(role)) {
            JButton allStudents = new JButton("All Students");
            JButton allCourses = new JButton("All Courses");
            JButton createStudent = new JButton("Create Student");
            JButton toggleMaintenance = new JButton("Toggle Maintenance");
            JButton setDrop = new JButton("Set Drop Deadline");
            JButton exit = new JButton("Logout");

            edu.univ.erp.ui.handlers.AdminUiHandlers adminHandlers = new edu.univ.erp.ui.handlers.AdminUiHandlers(user);
            allStudents.addActionListener(e -> adminHandlers.displayAllStudents());
            allCourses.addActionListener(e -> adminHandlers.displayAllCourses());
            createStudent.addActionListener(e -> adminHandlers.handleCreateStudentClick());
            toggleMaintenance.addActionListener(e -> adminHandlers.handleToggleMaintenanceClick());
            setDrop.addActionListener(e -> adminHandlers.handleSetDropDeadlineClick());
            exit.addActionListener(e -> dispose());

            add(allStudents); add(allCourses); add(createStudent); add(toggleMaintenance); add(setDrop); add(exit);
            return;
        }

        // Default: show catalog and exit
        JButton catalog = new JButton("Catalog");
        JButton exit = new JButton("Exit");
        catalog.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
        exit.addActionListener(e -> dispose());
        add(catalog); add(exit);
        setSize(400, 120);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}

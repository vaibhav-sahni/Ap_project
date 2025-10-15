package edu.univ.erp.ui.preview;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;

public class AdminDashboardFrame extends JFrame {
    private DashboardController controller;

    public AdminDashboardFrame(UserAuth user) {
        super("Admin Dashboard");
        setLayout(new FlowLayout());
        setSize(900, 140);
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

        JButton allStudents = new JButton("All Students");
        JButton allCourses = new JButton("All Courses");
        JButton createStudent = new JButton("Create Student");
        JButton toggleMaintenance = new JButton("Toggle Maintenance");
        JButton setDrop = new JButton("Set Drop Deadline");
    JButton downloadBackup = new JButton("Download DB Backup");
    JButton restoreBackup = new JButton("Restore DB Backup");
        JButton exit = new JButton("Logout");

        edu.univ.erp.ui.handlers.AdminUiHandlers adminHandlers = new edu.univ.erp.ui.handlers.AdminUiHandlers(user);
        allStudents.addActionListener(e -> adminHandlers.displayAllStudents());
        allCourses.addActionListener(e -> adminHandlers.displayAllCourses());
        createStudent.addActionListener(e -> adminHandlers.handleCreateStudentClick());
        toggleMaintenance.addActionListener(e -> adminHandlers.handleToggleMaintenanceClick());
        setDrop.addActionListener(e -> adminHandlers.handleSetDropDeadlineClick());
        downloadBackup.addActionListener(e -> adminHandlers.handleDownloadBackupClick());
        restoreBackup.addActionListener(e -> adminHandlers.handleRestoreBackupClick());
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

        add(allStudents); add(allCourses); add(createStudent); add(toggleMaintenance); add(setDrop); add(downloadBackup); add(restoreBackup); add(exit);
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

package edu.univ.erp.ui.preview;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;

/**
 * Minimal dashboard panel with a few buttons to open previews and call handlers.
 */
public class DashboardMainPanel extends JPanel {
    private final UserAuth user;
    private final DashboardController controller;

    public DashboardMainPanel(UserAuth user, DashboardController controller) {
        super(new BorderLayout());
        this.user = user;
        this.controller = controller;

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCatalog = new JButton("Open Catalog Preview");
        JButton btnTimetable = new JButton("View Timetable");
        JButton btnGrades = new JButton("View Grades");
        JButton btnLogout = new JButton("Logout");

        top.add(btnCatalog);
        top.add(btnTimetable);
        top.add(btnGrades);
        top.add(btnLogout);

        add(top, BorderLayout.NORTH);

        btnCatalog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new edu.univ.erp.ui.preview.CatalogPreviewFrame(user).setVisible(true);
            }
        });

        btnTimetable.addActionListener(ae -> controller.fetchAndDisplayTimetable());
        btnGrades.addActionListener(ae -> controller.fetchAndDisplayAllStudents()); // reuse admin view for now if needed
        btnLogout.addActionListener(ae -> controller.handleLogoutClick());
    }
}

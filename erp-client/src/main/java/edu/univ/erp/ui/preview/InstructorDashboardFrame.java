package edu.univ.erp.ui.preview;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;

public class InstructorDashboardFrame extends JFrame {
    private DashboardController controller;

    public InstructorDashboardFrame(UserAuth user) {
        super("Instructor Dashboard");
        setLayout(new FlowLayout());
        setSize(800, 120);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton assigned = new JButton("Assigned Sections");
        JButton roster = new JButton("View Roster (section 1)");
        JButton finalize = new JButton("Compute Final Grades (section 1)");
        JButton exit = new JButton("Logout");

        final int uid = user == null ? -1 : user.getUserId();
        assigned.addActionListener(e -> new edu.univ.erp.ui.handlers.InstructorUiHandlers(user).displayAssignedSections(uid));
        roster.addActionListener(e -> new RosterPreviewFrame(user, 1).setVisible(true));
        finalize.addActionListener(e -> new edu.univ.erp.ui.handlers.InstructorUiHandlers(user).computeFinalGradesWithUi(uid, 1));
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

        add(assigned); add(roster); add(finalize); add(exit);
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

package edu.univ.erp.ui.preview;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;
import edu.univ.erp.ui.handlers.InstructorUiHandlers;

public class InstructorDashboardFrame extends JFrame {
    private DashboardController controller;

    public InstructorDashboardFrame(UserAuth user) {
        super("Instructor Dashboard");
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

        JButton assigned = new JButton("Assigned Sections");
        JButton roster = new JButton("View Roster (section 1)");
        JButton finalize = new JButton("Compute Final Grades (section 1)");
        JButton export = new JButton("Export Grades (section 1)");
    JButton importBtn = new JButton("Import Grades (section 1)");
    JButton enterGrade = new JButton("Enter Grade");
        JButton exit = new JButton("Logout");

        final int uid = user == null ? -1 : user.getUserId();
    InstructorUiHandlers handlers = new InstructorUiHandlers(user);

        if (user != null && user.getLastLogin() != null) {
            javax.swing.JLabel lastLoginLabel = new javax.swing.JLabel("Last login: " + user.getLastLogin());
            add(lastLoginLabel);
        }

        assigned.addActionListener(e -> handlers.displayAssignedSections(uid));
        roster.addActionListener(e -> {
            Integer sid = chooseSectionDialog(handlers, uid);
            if (sid != null) new RosterPreviewFrame(user, sid).setVisible(true);
        });
        finalize.addActionListener(e -> {
            Integer sid = chooseSectionDialog(handlers, uid);
            if (sid != null) handlers.computeFinalGradesWithUi(uid, sid);
        });
        export.addActionListener(e -> {
            Integer sid = chooseSectionDialog(handlers, uid);
            if (sid != null) handlers.exportGradesToFile(uid, sid);
        });
        importBtn.addActionListener(e -> {
            Integer sid = chooseSectionDialog(handlers, uid);
            if (sid != null) handlers.importGradesFromFile(uid, sid);
        });
        enterGrade.addActionListener(e -> {
            Integer sid = chooseSectionDialog(handlers, uid);
            if (sid != null) handlers.enterGradeForStudentUi(uid, sid);
        });
        exit.addActionListener(e -> {
            if (controller != null) controller.handleLogoutClick();
            else dispose();
        });

        add(assigned); add(roster); add(finalize); add(export); add(importBtn); add(enterGrade); add(exit);
    }

    /**
     * Simple dialog that fetches assigned sections and lets the user pick one.
     * Returns the sectionId or null if cancelled.
     */
    private Integer chooseSectionDialog(edu.univ.erp.ui.handlers.InstructorUiHandlers handlers, int instructorId) {
        try {
            java.util.List<edu.univ.erp.domain.Section> list = handlers.displayAssignedSections(instructorId);
            if (list == null || list.isEmpty()) return null;
            String[] options = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                edu.univ.erp.domain.Section s = list.get(i);
                options[i] = s.getSectionId() + " - " + s.getCourseName() + " (" + s.getCourseCode() + ")";
            }
            String sel = (String) javax.swing.JOptionPane.showInputDialog(this, "Choose section:", "Section Selection", javax.swing.JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (sel == null) return null;
            // parse the chosen section id
            String idStr = sel.split(" - ")[0];
            return Integer.parseInt(idStr);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Failed to fetch sections: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void setController(DashboardController controller) {
        this.controller = controller;
    }
}

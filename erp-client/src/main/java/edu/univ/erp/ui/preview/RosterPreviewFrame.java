package edu.univ.erp.ui.preview;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.handlers.InstructorUiHandlers;

public class RosterPreviewFrame extends JFrame {
    private final RosterTableModel model;
    private final JTable table;
    private final InstructorUiHandlers handlers;
    private final javax.swing.JPanel centerPanel;

    public RosterPreviewFrame(UserAuth user, int sectionId) {
        super("Roster Preview");
        this.handlers = new InstructorUiHandlers(user);
        this.model = new RosterTableModel(null);
        this.table = new JTable(model);
    setLayout(new BorderLayout());
    // Card layout with table view and an empty message view
    this.centerPanel = new javax.swing.JPanel(new java.awt.CardLayout());
    javax.swing.JScrollPane tableScroll = new JScrollPane(table);
    javax.swing.JLabel emptyLabel = new javax.swing.JLabel("No students enrolled", javax.swing.SwingConstants.CENTER);
    this.centerPanel.add(tableScroll, "table");
    this.centerPanel.add(emptyLabel, "empty");
    add(this.centerPanel, BorderLayout.CENTER);
        // Add export/import buttons in a small top panel
        javax.swing.JPanel top = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportBtn = new JButton("Export Grades");
        JButton importBtn = new JButton("Import Grades");
        exportBtn.addActionListener(e -> handlers.exportGradesToFile(user.getUserId(), sectionId));
        importBtn.addActionListener(e -> handlers.importGradesFromFile(user.getUserId(), sectionId));
        top.add(exportBtn); top.add(importBtn);
        add(top, BorderLayout.NORTH);
        setSize(900, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loadAsync(user.getUserId(), sectionId);
    }

    private void loadAsync(int instructorId, int sectionId) {
        new SwingWorker<List<EnrollmentRecord>, Void>(){
            @Override protected List<EnrollmentRecord> doInBackground() throws Exception { return handlers.fetchRoster(instructorId, sectionId); }
            @Override protected void done() {
                try {
                    List<EnrollmentRecord> r = get();
                    java.awt.CardLayout cl = (java.awt.CardLayout) RosterPreviewFrame.this.centerPanel.getLayout();
                    if (r == null || r.isEmpty()) {
                        model.setData(java.util.Collections.emptyList());
                        cl.show(RosterPreviewFrame.this.centerPanel, "empty");
                    } else {
                        model.setData(r);
                        cl.show(RosterPreviewFrame.this.centerPanel, "table");
                    }
                } catch (Exception e) {
                    // On error, show empty roster and log for debugging
                    model.setData(java.util.Collections.emptyList());
                    java.awt.CardLayout cl = (java.awt.CardLayout) RosterPreviewFrame.this.centerPanel.getLayout();
                    cl.show(RosterPreviewFrame.this.centerPanel, "empty");
                    System.err.println("CLIENT ERROR: Failed to load roster: " + e.getMessage());
                }
            }
        }.execute();
    }
}

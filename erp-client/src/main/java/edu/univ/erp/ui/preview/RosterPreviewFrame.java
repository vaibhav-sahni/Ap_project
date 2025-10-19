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
    // Add export/import buttons and Enter Grades in a small top panel
    javax.swing.JPanel top = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton exportBtn = new JButton("Export Grades");
    JButton importBtn = new JButton("Import Grades");
    JButton enterBtn = new JButton("Enter Grades");
    exportBtn.addActionListener(e -> handlers.exportGradesToFile(user.getUserId(), sectionId));
    importBtn.addActionListener(e -> handlers.importGradesFromFile(user.getUserId(), sectionId));
    enterBtn.addActionListener(e -> openEnterGradesDialog(user.getUserId(), sectionId));
    top.add(exportBtn); top.add(importBtn); top.add(enterBtn);
        add(top, BorderLayout.NORTH);
        setSize(900, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loadAsync(user.getUserId(), sectionId);
    }

    public void openEnterGradesDialog(int instructorId, int sectionId) {
        try {
            java.util.List<EnrollmentRecord> roster = handlers.fetchRoster(instructorId, sectionId);
            if (roster == null || roster.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "No students to grade.", "Enter Grades", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] cols = new String[] {"Enrollment ID", "Student", "Roll No", "Quiz", "Assignment", "Midterm", "Endterm"};
            Object[][] data = new Object[roster.size()][cols.length];
            for (int i = 0; i < roster.size(); i++) {
                EnrollmentRecord r = roster.get(i);
                data[i][0] = r.getEnrollmentId();
                data[i][1] = r.getStudentName();
                data[i][2] = r.getRollNo();
                data[i][3] = r.getQuizScore() == null ? "" : r.getQuizScore();
                data[i][4] = r.getAssignmentScore() == null ? "" : r.getAssignmentScore();
                data[i][5] = r.getMidtermScore() == null ? "" : r.getMidtermScore();
                data[i][6] = r.getEndtermScore() == null ? "" : r.getEndtermScore();
            }

            javax.swing.table.DefaultTableModel tm = new javax.swing.table.DefaultTableModel(data, cols) {
                @Override public boolean isCellEditable(int row, int col) { return col >= 3; }
            };

            javax.swing.JTable editTable = new javax.swing.JTable(tm);
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(editTable);
            scroll.setPreferredSize(new java.awt.Dimension(900, 400));

            int choice = javax.swing.JOptionPane.showConfirmDialog(this, scroll, "Enter component scores for section " + sectionId, javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (choice != javax.swing.JOptionPane.OK_OPTION) return;

            // Validate and send updates
            InstructorUiHandlers uiHandlers = this.handlers;
            edu.univ.erp.ui.actions.InstructorActions actions = new edu.univ.erp.ui.actions.InstructorActions();

            for (int r = 0; r < tm.getRowCount(); r++) {
                int enrollmentId = Integer.parseInt(tm.getValueAt(r, 0).toString());
                // for each component, if value non-empty, parse and send
                String[] comps = new String[] {"quiz", "assignment", "midterm", "endterm"};
                for (int ci = 0; ci < comps.length; ci++) {
                    Object v = tm.getValueAt(r, 3 + ci);
                    if (v == null) continue;
                    String vs = v.toString().trim();
                    if (vs.isEmpty()) continue;
                    double score;
                    try { score = Double.parseDouble(vs); }
                    catch (NumberFormatException nfe) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Invalid numeric value for enrollment " + enrollmentId + " in column " + comps[ci], "Input Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (score < 0.0 || score > 100.0) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Score must be between 0 and 100 for enrollment " + enrollmentId + ".", "Input Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        String resp = actions.recordComponentScore(instructorId, enrollmentId, comps[ci], score);
                        System.out.println("CLIENT LOG: Recorded " + comps[ci] + "=" + score + " for EID " + enrollmentId + ": " + resp);
                    } catch (Exception ex) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Failed to record score for enrollment " + enrollmentId + ": " + ex.getMessage(), "Record Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            javax.swing.JOptionPane.showMessageDialog(this, "All provided component scores recorded successfully.", "Enter Grades", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            // refresh roster table
            loadAsync(instructorId, sectionId);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
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

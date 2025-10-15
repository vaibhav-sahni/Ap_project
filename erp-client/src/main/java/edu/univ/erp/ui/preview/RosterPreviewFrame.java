package edu.univ.erp.ui.preview;

import java.awt.BorderLayout;
import java.util.List;

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

    public RosterPreviewFrame(UserAuth user, int sectionId) {
        super("Roster Preview");
        this.handlers = new InstructorUiHandlers(user);
        this.model = new RosterTableModel(null);
        this.table = new JTable(model);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(900, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loadAsync(user.getUserId(), sectionId);
    }

    private void loadAsync(int instructorId, int sectionId) {
        new SwingWorker<List<EnrollmentRecord>, Void>(){
            @Override protected List<EnrollmentRecord> doInBackground() throws Exception { return handlers.fetchRoster(instructorId, sectionId); }
            @Override protected void done() {
                try { List<EnrollmentRecord> r = get(); if (r==null||r.isEmpty()) {
                            // sample
                            EnrollmentRecord e = new EnrollmentRecord(); e.setEnrollmentId(1); e.setStudentName("Alice"); e.setRollNo("R001"); e.setFinalGrade("A");
                            model.setData(java.util.Arrays.asList(e));
                        } else model.setData(r);
                } catch (Exception e) { EnrollmentRecord s = new EnrollmentRecord(); s.setEnrollmentId(1); s.setStudentName("Alice"); s.setRollNo("R001"); s.setFinalGrade("A"); model.setData(java.util.Arrays.asList(s)); }
            }
        }.execute();
    }
}

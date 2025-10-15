package edu.univ.erp.ui.preview;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.handlers.StudentUiHandlers;

public class GradesPreviewFrame extends JFrame {
    private final GradeTableModel model;
    private final JTable table;
    private final StudentUiHandlers handlers;

    public GradesPreviewFrame(UserAuth user) {
        super("Grades Preview");
        this.handlers = new StudentUiHandlers(user);
        this.model = new GradeTableModel(null);
        this.table = new JTable(model);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loadAsync();
    }

    private void loadAsync() {
        new SwingWorker<List<Grade>, Void>(){
            @Override protected List<Grade> doInBackground() throws Exception { return handlers.fetchGrades(); }
            @Override protected void done() {
                try { List<Grade> g = get(); if (g==null||g.isEmpty()) {
                            model.setData(java.util.Arrays.asList(new Grade("Sample Course","A",null)));
                        } else model.setData(g);
                } catch (Exception e) {
                    model.setData(java.util.Arrays.asList(new Grade("Sample Course","A",null)));
                }
            }
        }.execute();
    }
}

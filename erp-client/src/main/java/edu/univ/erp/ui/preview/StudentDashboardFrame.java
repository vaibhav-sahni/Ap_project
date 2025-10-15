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
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton catalog = new JButton("Catalog");
        JButton grades = new JButton("My Grades");
        JButton timetable = new JButton("Timetable");
        JButton transcript = new JButton("Download Transcript");
        JButton exit = new JButton("Logout");

        catalog.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
        grades.addActionListener(e -> new GradesPreviewFrame(user).setVisible(true));
        timetable.addActionListener(e -> new CatalogPreviewFrame(user).setVisible(true));
        transcript.addActionListener(e -> new javax.swing.SwingWorker<String, Void>(){
            @Override protected String doInBackground() throws Exception { return new edu.univ.erp.ui.handlers.StudentUiHandlers(user).fetchTranscriptHtml(); }
            @Override protected void done() {
                try {
                    String html = get();
                    javax.swing.JTextArea ta = new javax.swing.JTextArea(html);
                    ta.setEditable(false);
                    javax.swing.JScrollPane sp = new javax.swing.JScrollPane(ta);
                    sp.setPreferredSize(new java.awt.Dimension(800, 600));
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, sp, "Transcript (HTML)", javax.swing.JOptionPane.PLAIN_MESSAGE);
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    javax.swing.JOptionPane.showMessageDialog(StudentDashboardFrame.this, "Failed to fetch transcript: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        }.execute());
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

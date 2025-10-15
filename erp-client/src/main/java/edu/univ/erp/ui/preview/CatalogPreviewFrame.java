package edu.univ.erp.ui.preview;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.handlers.StudentUiHandlers;

/**
 * Minimal Swing preview window that demonstrates consuming the headless handler fetch methods.
 */
public class CatalogPreviewFrame extends JFrame {
    private final CourseCatalogTableModel model;
    private final JTable table;
    private final StudentUiHandlers handlers;

    public CatalogPreviewFrame(UserAuth user) {
        super("Course Catalog Preview");
        this.handlers = new StudentUiHandlers(user);
        // Start with sample data so the UI is never blank while async load happens
        java.util.List<CourseCatalog> initial = java.util.Arrays.asList(
                new CourseCatalog("CSCI101", "Intro to CS", 3, 1, "Mon 9-11", "R101", 40, 12, "Fall", 2025, 10, "Dr. Alice"),
                new CourseCatalog("MATH201", "Linear Algebra", 3, 2, "Tue 10-12", "R202", 30, 25, "Fall", 2025, 11, "Dr. Bob")
        );
        this.model = new CourseCatalogTableModel(initial);
        this.table = new JTable(model);
        // make columns visible and readable by setting a few preferred widths
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {80, 100, 300, 140, 70, 70, 140};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        setLayout(new BorderLayout());
    JLabel header = new JLabel("Catalog (loaded asynchronously)");
    add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        loadCatalogAsync();
    }

    private void loadCatalogAsync() {
        SwingWorker<List<CourseCatalog>, Void> w = new SwingWorker<>() {
            @Override
            protected List<CourseCatalog> doInBackground() throws Exception {
                return handlers.fetchCatalog();
            }

            @Override
            protected void done() {
                try {
                    List<CourseCatalog> data = get();
                    if (data == null || data.isEmpty()) {
                        // populate sample data for UI development
                        data = java.util.Arrays.asList(
                                new CourseCatalog("CSCI101", "Intro to CS", 3, 1, "Mon 9-11", "R101", 40, 12, "Fall", 2025, 10, "Dr. Alice"),
                                new CourseCatalog("MATH201", "Linear Algebra", 3, 2, "Tue 10-12", "R202", 30, 25, "Fall", 2025, 11, "Dr. Bob")
                        );
                    }
                    model.setData(data);
                } catch (Exception e) {
                    // fallback to sample data if any exception occurs (e.g., server unreachable or missing deps)
                    List<CourseCatalog> sample = java.util.Arrays.asList(
                            new CourseCatalog("CSCI101", "Intro to CS", 3, 1, "Mon 9-11", "R101", 40, 12, "Fall", 2025, 10, "Dr. Alice"),
                            new CourseCatalog("MATH201", "Linear Algebra", 3, 2, "Tue 10-12", "R202", 30, 25, "Fall", 2025, 11, "Dr. Bob")
                    );
                    model.setData(sample);
                    // also show a non-fatal warning to the user
                    JOptionPane.showMessageDialog(CatalogPreviewFrame.this, "Warning: could not fetch live catalog — showing sample data.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        w.execute();
    }

}

// Runnable entry for manual testing
class _CatalogPreviewRunner {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            edu.univ.erp.domain.UserAuth user = new edu.univ.erp.domain.UserAuth(1, "demoStudent", "Student");
            CatalogPreviewFrame f = new CatalogPreviewFrame(user);
            f.setVisible(true);
        });
    }
}

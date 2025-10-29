package edu.univ.erp.ui.instructordashboard.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.Timer;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.domain.Section;
import edu.univ.erp.ui.instructordashboard.components.SimpleForm;
import edu.univ.erp.ui.instructordashboard.menu.FormManager;
import edu.univ.erp.ui.utils.UIHelper;
import net.miginfocom.swing.MigLayout;

/**
 * Instructor Course Detail Form - Title: Course Name - Subtitle: course code
 * and section - Search bar + buttons: Export, Import, Save, Grade/Finalized -
 * Stats card: 4-color ring gauge + Average/Min/Max/Median - After finalized:
 * grade distribution graph (A/B/C/D/F) - Table: editable grades until
 * finalized; locked after Theme: honors light/dark via FlatLaf
 */
public class MyCourseForm extends SimpleForm {

    // Data
    private final Section section;
    private final List<GradeEntry> gradeEntries = new ArrayList<>();
    // Use server-aligned component names so save/import/export map 1:1
    private final List<String> assessments = List.of("Quiz", "Assignment", "Midterm", "Endterm");

    // UI
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JTextField searchField;
    private JButton exportBtn, importBtn, saveBtn, finalizeBtn, statsBtn;
    private ThemeableButton backBtn;
    private JTable gradeTable;
    private GradeTableModel tableModel;
    private TableRowSorter<GradeTableModel> sorter;
    private JPanel statsNumbersPanel;

    // Card that contains the roster table or an empty-state message
    private CardPanel tableCard;
    private JScrollPane tableScrollPane;

    private boolean isFinalized = false;

    public MyCourseForm(Section section) {
        if (section == null) {
            this.section = new Section(0, "N/A", "No section selected", 0);
        } else {
            this.section = section;
        }
        initUI();
    }

    // --- Theme helpers ---
    private Color textColor() {
        return FlatLaf.isLafDark() ? new Color(234, 234, 234) : new Color(30, 30, 30);
    }

    private Color secondaryTextColor() {
        return FlatLaf.isLafDark() ? new Color(153, 153, 153) : new Color(100, 100, 100);
    }

    private Color borderColor() {
        return FlatLaf.isLafDark() ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0");
    }

    private Color inputBg() {
        return FlatLaf.isLafDark() ? Color.decode("#272727") : Color.WHITE;
    }

    private Color getCalButtonBg() {
        return FlatLaf.isLafDark() ? new Color(42, 42, 42) : new Color(245, 245, 245);
    }

    private Color getCalButtonFg() {
        return FlatLaf.isLafDark() ? new Color(234, 234, 234) : new Color(30, 30, 30);
    }

    private Color getCalButtonBorder() {
        return FlatLaf.isLafDark() ? new Color(74, 74, 74) : new Color(200, 200, 200);
    }

    private void initUI() {
        setOpaque(false);
        setLayout(new MigLayout("fill, insets 24 28 24 28", "[grow]", "[]16[]16[grow]"));

        // Title + subtitle + Back button row
        JPanel titleRow = new JPanel(new MigLayout("insets 0, fillx", "[grow]12[]", "[]"));
        titleRow.setOpaque(false);

        JPanel titles = new JPanel(new MigLayout("insets 0, gapy 4", "[grow]", "[]4[]"));
        titles.setOpaque(false);

        titleLabel = new JLabel(section.getCourseName()) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        titles.add(titleLabel, "wrap");

        subtitleLabel = new JLabel(section.getCourseCode() + "  •  Section " + section.getSectionId()) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        titles.add(subtitleLabel);

        backBtn = new ThemeableButton("Back to Dashboard");
        backBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            FormManager.showForm(new DashboardForm());
            // Ensure the destination form is refreshed to avoid UI staleness
            SwingUtilities.invokeLater(() -> {
                try {
                    FormManager.refresh();
                } catch (Throwable ignore) {
                }
            });
        });

        titleRow.add(titles, "grow");
        titleRow.add(backBtn, "aligny center");
        add(titleRow, "growx, wrap");

        // Search + action buttons bar
        add(createActionBar(), "growx, wrap");

        // Table wrapped in CardPanel like student dashboard
        tableModel = new GradeTableModel();
        gradeTable = new ModernTable(tableModel);
        styleTable();
        sorter = new TableRowSorter<>(tableModel);
        gradeTable.setRowSorter(sorter);

        tableScrollPane = new JScrollPane(gradeTable);
        tableScrollPane.setOpaque(false);
        tableScrollPane.getViewport().setOpaque(false);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        tableScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        tableScrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        add(tableCard, "grow, push");

        // Load roster from server (falls back to demo data on error)
        loadRosterFromServer();

        // Theme change listener: repaint
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    repaint();
                });
            }
        });
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[grow]8[]8[]8[]8[]8[]", "[]"));
        bar.setOpaque(false);

        // Professional Search Field with icon (styled like MyGradesForm)
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setBackground(dark ? Color.decode("#272727") : Color.WHITE);
                setBorder(new RoundedBorder(10, dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0")));
                super.paintComponent(g);
            }
        };
        searchPanel.setOpaque(false);

        // Search icon (visual only)
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        searchPanel.add(searchIcon, BorderLayout.WEST);

        final String placeholder = "Search by name...";
        searchField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                Color bgColor = dark ? Color.decode("#272727") : Color.WHITE;
                Color textCol = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
                Color placeholderColor = dark ? Color.decode("#666666") : Color.decode("#94A3B8");
                setBackground(bgColor);
                if (getText().equals(placeholder)) {
                    setForeground(placeholderColor);
                } else {
                    setForeground(textCol);
                }
                setCaretColor(textCol);
                super.paintComponent(g);
            }
        };
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 10));
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        boolean dark = FlatLaf.isLafDark();
        searchField.setForeground(dark ? Color.decode("#666666") : Color.decode("#94A3B8"));
        searchField.setText(placeholder);

        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(placeholder)) {
                    searchField.setText("");
                    boolean dark = FlatLaf.isLafDark();
                    searchField.setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    boolean dark = FlatLaf.isLafDark();
                    searchField.setForeground(dark ? Color.decode("#666666") : Color.decode("#94A3B8"));
                    searchField.setText(placeholder);
                }
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applySearchFilter();
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        bar.add(searchPanel, "h 40!, growx");

        // Stats button with RoundButton styling
        statsBtn = new RoundButton("Stats", 12);
        statsBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statsBtn.setForeground(Color.WHITE);
        statsBtn.setBackground(new Color(139, 92, 246)); // Purple color
        statsBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        statsBtn.setPreferredSize(new Dimension(100, 40));
        statsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        statsBtn.addActionListener(e -> showStats());

        // Action buttons with RoundButton styling
        saveBtn = new RoundButton("Save", 12);
        saveBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(new Color(59, 130, 246));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        saveBtn.setPreferredSize(new Dimension(100, 40));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> doSave());

        exportBtn = new RoundButton("Export", 12);
        exportBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setBackground(new Color(16, 185, 129));
        exportBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        exportBtn.setPreferredSize(new Dimension(100, 40));
        exportBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportBtn.addActionListener(e -> doExport());

        importBtn = new RoundButton("Import", 12);
        importBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        importBtn.setForeground(Color.WHITE);
        importBtn.setBackground(new Color(234, 179, 8));
        importBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        importBtn.setPreferredSize(new Dimension(100, 40));
        importBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        importBtn.addActionListener(e -> doImport());

        finalizeBtn = new RoundButton("Finalize Grades", 12);
        finalizeBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        finalizeBtn.setForeground(Color.WHITE);
        finalizeBtn.setBackground(new Color(220, 38, 38));
        finalizeBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        finalizeBtn.setPreferredSize(new Dimension(160, 40));
        finalizeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        finalizeBtn.addActionListener(e -> doFinalize());

        bar.add(statsBtn, "h 40!, aligny center");
        bar.add(saveBtn, "h 40!, aligny center");
        bar.add(exportBtn, "h 40!, aligny center");
        bar.add(importBtn, "h 40!, aligny center");
        bar.add(finalizeBtn, "h 40!, aligny center");
        return bar;
    }

    private void styleTable() {
        gradeTable.setRowHeight(46);
        gradeTable.setFillsViewportHeight(true);
        gradeTable.setIntercellSpacing(new Dimension(0, 0));
        gradeTable.setShowGrid(false);

        JTableHeader header = gradeTable.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        header.setDefaultRenderer(new SortHeaderRenderer(gradeTable));

        // Center numeric columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (tableModel.getColumnCount() > 2) {
            gradeTable.getColumnModel().getColumn(2).setCellRenderer(center);
        }
        if (tableModel.getColumnCount() > 3) {
            gradeTable.getColumnModel().getColumn(3).setCellRenderer(center);
        }
        if (tableModel.getColumnCount() > 4) {
            gradeTable.getColumnModel().getColumn(4).setCellRenderer(center);
        }
        if (tableModel.getColumnCount() > 5) {
            gradeTable.getColumnModel().getColumn(5).setCellRenderer(center);
        }
    }

    // RoundButton - Custom button with rounded corners (from MyGradesForm)
    private static class RoundButton extends JButton {

        private final int radius;

        public RoundButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalTextPosition(SwingConstants.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void applySearchFilter() {
        String q = searchField.getText();
        final String placeholder = "Search by name...";
        if (q == null || q.isBlank() || q.equals(placeholder)) {
            sorter.setRowFilter(null);
            return;
        }
        final String s = q.trim().toLowerCase();
        sorter.setRowFilter(new javax.swing.RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(javax.swing.RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
                Object name = entry.getValue(0);
                Object roll = entry.getValue(1);
                return (name != null && name.toString().toLowerCase().contains(s))
                        || (roll != null && roll.toString().toLowerCase().contains(s));
            }
        });
    }

    private void doSave() {
        if (isFinalized) {
            JOptionPane.showMessageDialog(this, "Grading is finalized. Cannot save changes.", "Save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Persist changes via InstructorActions.recordScore
        edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Not logged in.", "Save Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean hadError = false;
        try {
            edu.univ.erp.ui.actions.InstructorActions a = new edu.univ.erp.ui.actions.InstructorActions();
            int instructorId = u.getUserId();
            for (GradeEntry ge : gradeEntries) {
                if (ge.enrollmentId <= 0) {
                    continue;
                }
                for (String comp : assessments) {
                    Double v = ge.scores.get(comp);
                    if (v != null) {
                        try {
                            String compId = switch (comp.toLowerCase()) {
                                case "quiz" ->
                                    "quiz";
                                case "assignment" ->
                                    "assignment";
                                case "midterm" ->
                                    "midterm";
                                case "endterm" ->
                                    "endterm";
                                default ->
                                    comp.toLowerCase().replaceAll("\\s+", "");
                            };
                            a.recordScore(instructorId, ge.enrollmentId, compId, v);
                        } catch (Exception ex) {
                            hadError = true;
                            System.err.println("Failed to save " + ge.studentName + " comp=" + comp + ": " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            hadError = true;
            System.err.println("Save failed (handler missing or error): " + ex.getMessage());
        }

        if (!hadError) {
            JOptionPane.showMessageDialog(this, "Grades saved to server.", "Save", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Fallback: offer to save locally as CSV
        int choice = JOptionPane.showConfirmDialog(this, "Some or all grades failed to save to server. Save a local CSV copy instead?", "Save Locally", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save grades as CSV");
        int rv = fc.showSaveDialog(this);
        if (rv == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                exportToCsv(f);
                JOptionPane.showMessageDialog(this, "Saved local copy to " + f.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException io) {
                JOptionPane.showMessageDialog(this, "Failed to save local CSV: " + io.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doExport() {
        try {
            edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
            if (u == null) {
                throw new Exception("Not logged in");
            }
            edu.univ.erp.ui.handlers.InstructorUiHandlers handlers = new edu.univ.erp.ui.handlers.InstructorUiHandlers(u);
            boolean ok = handlers.exportGradesToFile(u.getUserId(), section.getSectionId());
            if (ok) {
                return;
            }
            // if handler returned false -> fallthrough to local export
        } catch (Exception ex) {
            // fallback to local CSV export
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export grades to CSV");
        int rv = fc.showSaveDialog(this);
        if (rv == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                exportToCsv(f);
                JOptionPane.showMessageDialog(this, "Exported grades to " + f.getAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException io) {
                JOptionPane.showMessageDialog(this, "Export failed: " + io.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private void doImport() {
        if (isFinalized) {
            JOptionPane.showMessageDialog(this, "Grading is finalized. Cannot import.", "Import", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
            if (u == null) {
                throw new Exception("Not logged in");
            }
            edu.univ.erp.ui.handlers.InstructorUiHandlers handlers = new edu.univ.erp.ui.handlers.InstructorUiHandlers(u);
            String summary = handlers.importGradesFromFile(u.getUserId(), section.getSectionId());
            if (summary != null) {
                JOptionPane.showMessageDialog(this, "Import result:\n" + summary, "Import", JOptionPane.INFORMATION_MESSAGE);
                // refresh roster from server after successful import
                loadRosterFromServer();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Import Failed", JOptionPane.ERROR_MESSAGE);
            // fallback to local CSV import below
        }

        // Local import via file chooser
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import grades from CSV");
        int rv = fc.showOpenDialog(this);
        if (rv == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                importFromCsv(f);
                JOptionPane.showMessageDialog(this, "Imported grades from " + f.getAbsolutePath(), "Import", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException io) {
                JOptionPane.showMessageDialog(this, "Import failed: " + io.getMessage(), "Import Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private void doFinalize() {
        if (isFinalized) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Finalize grades? This will lock editing.",
                "Finalize Grades",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        // Delegate finalization to server via handlers which also updates final grades
        try {
            edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
            if (u == null) {
                throw new Exception("Not logged in");
            }
            edu.univ.erp.ui.handlers.InstructorUiHandlers handlers = new edu.univ.erp.ui.handlers.InstructorUiHandlers(u);
            // This will show preview/confirm dialogs and perform server-side compute
            handlers.computeFinalGradesWithUi(u.getUserId(), section.getSectionId());
            // Refresh roster (server will mark enrollments finalized)
            loadRosterFromServer();
            isFinalized = true;
            tableModel.setEditable(false);
            finalizeBtn.setText("Finalized");
            // keep theme/default color; do not change background
            saveBtn.setEnabled(false);
            importBtn.setEnabled(false);
            return;
        } catch (Exception ex) {
            // fallback to local finalize
            System.err.println("Finalize failed server-side: " + ex.getMessage());
        }

        // Local finalize: compute simple letter grades from averages
        for (GradeEntry ge : gradeEntries) {
            String letter = computeFinalLetterGrade(ge);
            ge.finalGrade = letter;
        }
        isFinalized = true;
        tableModel.setEditable(false);
        finalizeBtn.setText("Finalized");
        // keep theme/default color; do not change background
        saveBtn.setEnabled(false);
        importBtn.setEnabled(false);
        tableModel.fireTableDataChanged();
        JOptionPane.showMessageDialog(this, "Grades finalized locally. Consider exporting a CSV copy or saving to server.", "Finalized", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show an empty-state message inside the table card.
     */
    private void showEmptyMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            tableCard.removeAll();
            JLabel lbl = new JLabel(message, SwingConstants.CENTER);
            lbl.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
            lbl.setForeground(secondaryTextColor());
            tableCard.add(lbl, BorderLayout.CENTER);
            tableCard.revalidate();
            tableCard.repaint();
        });
    }

    /**
     * Restore the table view inside the card.
     */
    private void showTableView() {
        SwingUtilities.invokeLater(() -> {
            tableCard.removeAll();
            tableCard.add(tableScrollPane, BorderLayout.CENTER);
            tableCard.revalidate();
            tableCard.repaint();
        });
    }

    // Load roster using InstructorUiHandlers; fall back to dummy data if any error occurs
    private void loadRosterFromServer() {
        gradeEntries.clear();
        edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
        if (u == null) {
            // No logged-in user: show empty roster and leave UI in a neutral state.
            gradeEntries.clear();
            tableModel.updateData(gradeEntries);
            isFinalized = false;
            tableModel.setEditable(true);
            finalizeBtn.setText("Finalize Grades");
            finalizeBtn.setBackground(new Color(220, 38, 38));
            saveBtn.setEnabled(true);
            importBtn.setEnabled(true);
            showEmptyMessage("No students enrolled.");
            return;
        }
        UIHelper.runAsync(() -> {
            try {
                edu.univ.erp.ui.handlers.InstructorUiHandlers handlers = new edu.univ.erp.ui.handlers.InstructorUiHandlers(u);
                java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = handlers.fetchRoster(u.getUserId(), section.getSectionId());
                if (roster == null || roster.isEmpty()) {
                    // Return an empty list so UI shows an empty table instead of demo data.
                    return new ArrayList<GradeEntry>();
                }
                java.util.List<GradeEntry> out = new ArrayList<>();
                for (edu.univ.erp.domain.EnrollmentRecord r : roster) {
                    Map<String, Double> map = new HashMap<>();
                    // Map component names to match 'assessments'
                    map.put("Quiz", r.getQuizScore());
                    map.put("Assignment", r.getAssignmentScore());
                    map.put("Midterm", r.getMidtermScore());
                    map.put("Endterm", r.getEndtermScore());
                    GradeEntry ge = new GradeEntry(r.getStudentName(), r.getRollNo(), map, r.getFinalGrade());
                    ge.enrollmentId = r.getEnrollmentId();
                    out.add(ge);
                }
                return out;
            } catch (Exception ex) {
                return null;
            }
        }, (Object result) -> {
            try {
                java.util.List<GradeEntry> list = (java.util.List<GradeEntry>) result;
                // Show empty table when server returns no data; do NOT populate demo rows.
                if (list == null || list.isEmpty()) {
                    gradeEntries.clear();
                    tableModel.updateData(gradeEntries);
                    isFinalized = false;
                    tableModel.setEditable(true);
                    finalizeBtn.setText("Finalize Grades");
                    finalizeBtn.setBackground(new Color(220, 38, 38));
                    saveBtn.setEnabled(true);
                    importBtn.setEnabled(true);
                    showEmptyMessage("No students enrolled.");
                } else {
                    gradeEntries.clear();
                    gradeEntries.addAll(list);
                    tableModel.updateData(gradeEntries);
                    showTableView();
                    // Auto-lock if server indicates final grades already present for all
                    boolean allFinal = gradeEntries.stream().allMatch(e -> e.finalGrade != null && !e.finalGrade.isBlank());
                    isFinalized = allFinal;
                    tableModel.setEditable(!isFinalized);
                    if (isFinalized) {
                        finalizeBtn.setText("Finalized");
                        // keep theme/default color; do not change background
                        saveBtn.setEnabled(false);
                        importBtn.setEnabled(false);
                    } else {
                        finalizeBtn.setText("Finalize Grades");
                        finalizeBtn.setBackground(new Color(220, 38, 38));
                        saveBtn.setEnabled(true);
                        importBtn.setEnabled(true);
                    }
                }
            } catch (Exception ex) {
                // On unexpected exception, show empty table and log the error.
                gradeEntries.clear();
                tableModel.updateData(gradeEntries);
                isFinalized = false;
                tableModel.setEditable(true);
                finalizeBtn.setText("Finalize Grades");
                finalizeBtn.setBackground(new Color(220, 38, 38));
                saveBtn.setEnabled(true);
                importBtn.setEnabled(true);
                showEmptyMessage("No students enrolled.");
                System.err.println("Client WARN: Failed to parse roster response: " + ex.getMessage());
            }
        }, (Exception ex) -> {
            // On async error, show empty table and surface a UI message
            gradeEntries.clear();
            tableModel.updateData(gradeEntries);
            isFinalized = false;
            tableModel.setEditable(true);
            finalizeBtn.setText("Finalize Grades");
            finalizeBtn.setBackground(new Color(220, 38, 38));
            saveBtn.setEnabled(true);
            importBtn.setEnabled(true);
            showEmptyMessage("No students enrolled.");
            System.err.println("Client WARN: Failed to load roster from server: " + ex.getMessage());
        });
    }

    private void showStats() {
        // Build a live stats panel and show in a non-modal dialog.
        // Computes per-student averages from current `gradeEntries` and updates the gauge + numbers every second.
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Statistics", java.awt.Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new MigLayout("fill, insets 12", "[pref!]8[grow]", "[grow]"));
        content.setOpaque(false);

        StatsGaugePanel gauge = new StatsGaugePanel();
        gauge.setPreferredSize(new Dimension(260, 260));

        // numbers panel on the right
        statsNumbersPanel = new JPanel(new MigLayout("wrap 1, insets 6", "[grow]", "[]8[]8[]8[]8[]"));
        statsNumbersPanel.setOpaque(false);
        JLabel avgLabel = new JLabel("Average: --");
        JLabel minLabel = new JLabel("Min: --");
        JLabel maxLabel = new JLabel("Max: --");
        JLabel medLabel = new JLabel("Median: --");
        Font nf = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        avgLabel.setFont(nf);
        minLabel.setFont(nf);
        maxLabel.setFont(nf);
        medLabel.setFont(nf);
        statsNumbersPanel.add(avgLabel);
        statsNumbersPanel.add(minLabel);
        statsNumbersPanel.add(maxLabel);
        statsNumbersPanel.add(medLabel);

        // small distribution preview under numbers
        GradeDistributionPanel dist = new GradeDistributionPanel();
        dist.setPreferredSize(new Dimension(200, 120));
        statsNumbersPanel.add(dist, "growx");

        content.add(gauge);
        content.add(statsNumbersPanel, "growx");

        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setLocationRelativeTo(this);

        // Timer to recalc stats every 1s
        Timer t = new Timer(1000, ev -> {
            java.util.List<Double> averages = new ArrayList<>();
            for (GradeEntry ge : gradeEntries) {
                double sum = 0;
                int cnt = 0;
                for (String a : assessments) {
                    Double v = ge.scores.get(a);
                    if (v != null) {
                        sum += v;
                        cnt++;
                    }
                }
                if (cnt > 0) {
                    averages.add(sum / cnt);
                }
            }
            double avg = 0, min = 0, max = 0, median = 0;
            if (!averages.isEmpty()) {
                java.util.Collections.sort(averages);
                double sum = 0;
                for (double d : averages) {
                    sum += d;
                }
                avg = sum / averages.size();
                min = averages.get(0);
                max = averages.get(averages.size() - 1);
                int m = averages.size() / 2;
                if (averages.size() % 2 == 1) {
                    median = averages.get(m);
                } else {
                    median = (averages.get(m - 1) + averages.get(m)) / 2.0;
                }
            }

            // update gauge normalized to 100
            gauge.setValues(avg / 100.0, min / 100.0, max / 100.0, median / 100.0);
            avgLabel.setText(String.format("Average: %.2f", avg));
            minLabel.setText(String.format("Min: %.2f", min));
            maxLabel.setText(String.format("Max: %.2f", max));
            medLabel.setText(String.format("Median: %.2f", median));

            // distribution by final grade (use existing finalGrade if present, else compute local letter)
            Map<String, Long> counts = new HashMap<>();
            String[] grades = new String[]{"A", "B", "C", "D", "F"};
            for (String g : grades) {
                counts.put(g, 0L);
            }
            for (GradeEntry ge : gradeEntries) {
                String fg = ge.finalGrade;
                if (fg == null || fg.isBlank()) {
                    fg = computeFinalLetterGrade(ge);
                }
                if (fg == null || fg.isBlank()) {
                    continue;
                }
                counts.put(fg, counts.getOrDefault(fg, 0L) + 1);
            }
            dist.setData(counts);
        });

        // clicking the gauge opens a larger popup
        gauge.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Create a larger dialog with full-size gauge and distribution
                JDialog big = new JDialog(SwingUtilities.getWindowAncestor(MyCourseForm.this), "Stats — Detailed", java.awt.Dialog.ModalityType.MODELESS);
                JPanel p = new JPanel(new MigLayout("fill, insets 12", "[grow]", "[grow][]"));
                StatsGaugePanel bigGauge = new StatsGaugePanel();
                bigGauge.setPreferredSize(new Dimension(420, 420));
                GradeDistributionPanel bigDist = new GradeDistributionPanel();
                bigDist.setPreferredSize(new Dimension(420, 140));
                p.add(bigGauge, "growx, wrap");
                p.add(bigDist, "growx");
                big.getContentPane().add(p);
                big.pack();
                big.setLocationRelativeTo(MyCourseForm.this);

                // sync values immediately and while shown
                Timer tt = new Timer(500, ev2 -> {
                    // reuse same calculation code quickly
                    java.util.List<Double> avgs = new ArrayList<>();
                    for (GradeEntry ge : gradeEntries) {
                        double s = 0;
                        int c = 0;
                        for (String a : assessments) {
                            Double v = ge.scores.get(a);
                            if (v != null) {
                                s += v;
                                c++;
                            }
                        }
                        if (c > 0) {
                            avgs.add(s / c);
                        }
                    }
                    double A = 0, mN = 0, mX = 0, md = 0;
                    if (!avgs.isEmpty()) {
                        java.util.Collections.sort(avgs);
                        double s = 0;
                        for (double vv : avgs) {
                            s += vv;
                        }
                        A = s / avgs.size();
                        mN = avgs.get(0);
                        mX = avgs.get(avgs.size() - 1);
                        int mm = avgs.size() / 2;
                        md = (avgs.size() % 2 == 1) ? avgs.get(mm) : (avgs.get(mm - 1) + avgs.get(mm)) / 2.0;
                    }
                    bigGauge.setValues(A / 100.0, mN / 100.0, mX / 100.0, md / 100.0);
                    Map<String, Long> cnts = new HashMap<>();
                    for (String g : new String[]{"A", "B", "C", "D", "F"}) {
                        cnts.put(g, 0L);
                    }
                    for (GradeEntry ge : gradeEntries) {
                        String fg = ge.finalGrade;
                        if (fg == null || fg.isBlank()) {
                            fg = computeFinalLetterGrade(ge);
                        
                        }if (fg == null || fg.isBlank()) {
                            continue;
                        }
                        cnts.put(fg, cnts.getOrDefault(fg, 0L) + 1);
                    }
                    bigDist.setData(cnts);
                });

                big.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        tt.stop();
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        tt.stop();
                    }
                });
                tt.start();
                big.setVisible(true);
            }
        });

        dlg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                t.stop();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                t.stop();
            }
        });

        t.setInitialDelay(0);
        t.start();
        dlg.setVisible(true);
    }

    @Override
    public void formRefresh() {
        // Just repaint
        gradeTable.repaint();
    }

    // --- Data classes ---
    private static class GradeEntry {

        int enrollmentId = -1;
        String studentName;
        String rollNo;
        Map<String, Double> scores;
        String finalGrade;

        GradeEntry(String name, String roll, Map<String, Double> scores, String finalGrade) {
            this.studentName = name;
            this.rollNo = roll;
            this.scores = scores != null ? new HashMap<>(scores) : new HashMap<>();
            this.finalGrade = finalGrade;
        }
    }

    private class GradeTableModel extends AbstractTableModel {

        private final String[] BASE = {"Student Name", "Roll No"};
        private boolean editable = true;

        void setEditable(boolean e) {
            this.editable = e;
            fireTableDataChanged();
        }

        void updateData(List<GradeEntry> list) {
            // Keep the master list in the outer form so edits in the table
            // reflect directly on `gradeEntries` used by save/export/import.
            // copy incoming list to avoid aliasing issues when caller passes the
            // same list reference as our backing list.
            gradeEntries.clear();
            if (list != null) {
                gradeEntries.addAll(new ArrayList<>(list));
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return gradeEntries.size();
        }

        @Override
        public int getColumnCount() {
            return BASE.length + assessments.size() + 1;
        }

        @Override
        public String getColumnName(int col) {
            if (col < BASE.length) {
                return BASE[col];
            }
            if (col < BASE.length + assessments.size()) {
                return assessments.get(col - BASE.length);
            }
            return "Final Grade";
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col < BASE.length) {
                return String.class;
            }
            if (col < BASE.length + assessments.size()) {
                return Double.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return editable && col >= BASE.length && col < BASE.length + assessments.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            GradeEntry e = gradeEntries.get(row);
            if (col == 0) {
                return e.studentName;
            }
            if (col == 1) {
                return e.rollNo;
            }
            if (col < BASE.length + assessments.size()) {
                String key = assessments.get(col - BASE.length);
                return e.scores.get(key);
            }
            return e.finalGrade;
        }

        @Override
        public void setValueAt(Object aValue, int row, int col) {
            if (!isCellEditable(row, col)) {
                return;
            }
            GradeEntry e = gradeEntries.get(row);
            String key = assessments.get(col - BASE.length);
            try {
                Double v = null;
                if (aValue != null && !aValue.toString().trim().isEmpty()) {
                    v = Double.parseDouble(aValue.toString());
                    if (v < 0) {
                        throw new NumberFormatException();
                    }
                }
                e.scores.put(key, v);
                fireTableCellUpdated(row, col);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(MyCourseForm.this, "Enter a valid non-negative number.", "Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- CSV Import/Export helpers (local fallback) ---
    private void exportToCsv(File file) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            // Header
            String header = "Student Name,Roll No";
            for (String a : assessments) {
                header += "," + a;
            }
            header += ",Final Grade";
            w.write(header);
            w.newLine();
            for (GradeEntry ge : gradeEntries) {
                StringBuilder sb = new StringBuilder();
                sb.append(escapeCsv(ge.studentName)).append(',').append(escapeCsv(ge.rollNo));
                for (String a : assessments) {
                    Double v = ge.scores.get(a);
                    sb.append(',');
                    if (v != null) {
                        sb.append(v);
                    }
                }
                sb.append(',').append(escapeCsv(ge.finalGrade == null ? "" : ge.finalGrade));
                w.write(sb.toString());
                w.newLine();
            }
        }
    }

    private void importFromCsv(File file) throws IOException {
        List<GradeEntry> imported = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line = r.readLine();
            if (line == null) {
                return;
            }
            String[] headers = splitCsv(line);
            // Map column indices for known headers
            int nameIdx = -1, rollIdx = -1;
            int[] compIdx = new int[assessments.size()];
            for (int i = 0; i < compIdx.length; i++) {
                compIdx[i] = -1;
            }
            int finalIdx = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim();
                if (h.equalsIgnoreCase("Student Name")) {
                    nameIdx = i;
                } else if (h.equalsIgnoreCase("Roll No")) {
                    rollIdx = i;
                } else if (h.equalsIgnoreCase("Final Grade")) {
                    finalIdx = i;
                } else {
                    for (int j = 0; j < assessments.size(); j++) {
                        if (h.equalsIgnoreCase(assessments.get(j))) {
                            compIdx[j] = i;
                            break;
                        }
                    }
                }
            }
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] cols = splitCsv(line);
                String name = nameIdx >= 0 && nameIdx < cols.length ? cols[nameIdx] : "";
                String roll = rollIdx >= 0 && rollIdx < cols.length ? cols[rollIdx] : "";
                Map<String, Double> map = new HashMap<>();
                for (int j = 0; j < assessments.size(); j++) {
                    int idx = compIdx[j];
                    Double v = null;
                    if (idx >= 0 && idx < cols.length) {
                        String s = cols[idx].trim();
                        if (!s.isEmpty()) {
                            try {
                                v = Double.parseDouble(s);
                            } catch (Exception ignore) {
                                v = null;
                            }
                        }
                    }
                    map.put(assessments.get(j), v);
                }
                String finalG = finalIdx >= 0 && finalIdx < cols.length ? cols[finalIdx] : null;
                GradeEntry ge = new GradeEntry(name, roll, map, finalG);
                imported.add(ge);
            }
        }
        gradeEntries.clear();
        gradeEntries.addAll(imported);
        tableModel.updateData(gradeEntries);
        showTableView();
    }

    private static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // Local finalize: average available assessment scores and map to letter grade
    private String computeFinalLetterGrade(GradeEntry ge) {
        double sum = 0;
        int count = 0;
        for (String a : assessments) {
            Double v = ge.scores.get(a);
            if (v != null) {
                sum += v;
                count++;
            }
        }
        if (count == 0) {
            return "";
        }
        double avg = sum / count;
        if (avg >= 90) {
            return "A";
        }
        if (avg >= 80) {
            return "B";
        }
        if (avg >= 70) {
            return "C";
        }
        if (avg >= 60) {
            return "D";
        }
        return "F";
    }

    // --- Styled components ---
    private static class RoundedBorder extends javax.swing.border.AbstractBorder {

        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public Insets getBorderInsets(java.awt.Component c) {
            return new Insets(radius / 2 + 2, radius / 2 + 2, radius / 2 + 2, radius / 2 + 2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, radius, radius));
            g2.dispose();
        }
    }

    private static class CardPanel extends JPanel {

        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = FlatLaf.isLafDark();
            Color bg = dark ? Color.decode("#272727") : Color.WHITE;
            Color border = dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0");
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
            g2.setColor(border);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ModernTable extends JTable {

        ModernTable(TableModel m) {
            super(m);
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean dark = FlatLaf.isLafDark();
            setBackground(dark ? Color.decode("#272727") : Color.WHITE);
            setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
            setSelectionBackground(dark ? Color.decode("#4A4A4A") : Color.decode("#CCE0FF"));
            setSelectionForeground(dark ? Color.WHITE : Color.decode("#0F172A"));
            setGridColor(dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB"));
            JTableHeader h = getTableHeader();
            if (h != null) {
                h.setBackground(dark ? Color.decode("#1E1E1E") : Color.decode("#F1F5F9"));
                h.setForeground(dark ? Color.decode("#B3B3B3") : Color.decode("#475569"));
            }
            super.paintComponent(g);
        }
    }

    private static class SortHeaderRenderer implements TableCellRenderer {

        private final JTable table;

        SortHeaderRenderer(JTable table) {
            this.table = table;
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JTableHeader header = table.getTableHeader();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(header.getBackground());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 10));
            JLabel text = new JLabel(value == null ? "" : value.toString());
            text.setFont(header.getFont());
            text.setForeground(header.getForeground());
            panel.add(text, BorderLayout.WEST);
            return panel;
        }
    }

    // 4-ring gauge panel
    private static class StatsGaugePanel extends JPanel {

        private double a, b, c, d; // 0..1 for avg,min,max,median

        void setValues(double a, double b, double c, double d) {
            this.a = clamp(a);
            this.b = clamp(b);
            this.c = clamp(c);
            this.d = clamp(d);
            repaint();
        }

        private double clamp(double v) {
            return Math.max(0, Math.min(1, v));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 20;
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int base = size;
            int rings = 4;
            int ringW = Math.max(10, size / 14);
            Color[] cols = new Color[]{new Color(59, 130, 246), new Color(34, 197, 94), new Color(245, 158, 11), new Color(99, 102, 241)};
            double[] vals = new double[]{a, b, c, d};
            for (int i = 0; i < rings; i++) {
                int r = base - i * (ringW + 8);
                int x = cx - r / 2;
                int y = cy - r / 2;
                // background ring
                g2.setColor(new Color(200, 210, 230, 60));
                g2.setStroke(new java.awt.BasicStroke(ringW, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(x, y, r, r, 90, -360, Arc2D.OPEN));
                // value arc
                g2.setColor(cols[i]);
                g2.draw(new Arc2D.Double(x, y, r, r, 90, -360 * vals[i], Arc2D.OPEN));
            }
            // Center label
            String label = "Stats";
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g2.setColor(FlatLaf.isLafDark() ? Color.WHITE : Color.decode("#0F172A"));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + fm.getAscent() / 2);
            g2.dispose();
        }
    }

    // Simple bar chart panel for grade distribution
    private static class GradeDistributionPanel extends JPanel {

        private Map<String, Long> counts = new HashMap<>();

        void setData(Map<String, Long> c) {
            this.counts = new HashMap<>(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String[] grades = {"A", "B", "C", "D", "F"};
            int w = getWidth() - 40, h = getHeight() - 40;
            int x = 20, y = 20;
            long max = 1;
            for (String gr : grades) {
                max = Math.max(max, counts.getOrDefault(gr, 0L));
            }
            int barW = Math.max(20, w / (grades.length * 2));
            int gap = barW;
            for (int i = 0; i < grades.length; i++) {
                long v = counts.getOrDefault(grades[i], 0L);
                int bh = (int) (h * (v / (double) max));
                int bx = x + i * (barW + gap) + gap / 2;
                int by = y + h - bh;
                g2.setColor(new Color(59, 130, 246));
                g2.fillRoundRect(bx, by, barW, bh, 8, 8);
                g2.setColor(FlatLaf.isLafDark() ? Color.WHITE : Color.decode("#0F172A"));
                g2.drawString(grades[i], bx + barW / 2 - g2.getFontMetrics().stringWidth(grades[i]) / 2, y + h + 16);
            }
            g2.dispose();
        }
    }

    // Custom ScrollBar UI from student dashboard
    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

        private final Color thumb;
        private final Color track;

        public CustomScrollBarUI() {
            boolean dark = FlatLaf.isLafDark();
            this.thumb = dark ? Color.decode("#4A4A4A") : Color.decode("#C7C7CC");
            this.track = dark ? Color.decode("#272727") : Color.decode("#F2F2F7");
        }

        public CustomScrollBarUI(Color thumb, Color track) {
            this.thumb = thumb;
            this.track = track;
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = thumb;
            this.trackColor = track;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }

    // ThemeableButton - Professional styled button with rounded corners and theme-aware colors
    private class ThemeableButton extends JButton {

        public ThemeableButton(String text) {
            super(text);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg;
            if (getModel().isPressed()) {
                bg = getCalButtonBg().darker();
            } else if (getModel().isRollover()) {
                // Brighter in dark mode, darker in light mode
                bg = FlatLaf.isLafDark() ? getCalButtonBg().brighter() : getCalButtonBg().darker();
            } else {
                bg = getCalButtonBg();
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.setColor(getCalButtonBorder());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();

            setForeground(getCalButtonFg());
            super.paintComponent(g);
        }
    }
}

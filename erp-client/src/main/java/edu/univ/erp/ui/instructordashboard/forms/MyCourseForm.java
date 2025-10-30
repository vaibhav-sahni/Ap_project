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
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
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
        try {
            edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
            if (u == null) {
                throw new Exception("Not logged in");
            }
            edu.univ.erp.ui.actions.InstructorActions a = new edu.univ.erp.ui.actions.InstructorActions();
            int instructorId = u.getUserId();
            for (GradeEntry ge : gradeEntries) {
                // each GradeEntry now contains enrollmentId
                if (ge.enrollmentId <= 0) {
                    continue;
                }
                for (String comp : assessments) {
                    Double v = ge.scores.get(comp);
                    if (v != null) {
                        try {
                            // Map UI assessment names to server component identifiers
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
                            // log and continue
                            System.err.println("Failed to save " + ge.studentName + " comp=" + comp + ": " + ex.getMessage());
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Grades saved.", "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save grades: " + ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
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
            if (!ok) {
                // user probably cancelled
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
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
            finalizeBtn.setBackground(Color.decode("#6B7280"));
            saveBtn.setEnabled(false);
            importBtn.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Finalize failed: " + ex.getMessage(), "Finalize Failed", JOptionPane.ERROR_MESSAGE);
        }
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
                    // Diagnostic logging to help debug why rows are not visible
                    System.out.println("Client LOG: Roster fetch callback received list size=" + list.size());
                    System.out.println("Client LOG: Table model rows before update=" + tableModel.getRowCount());

                    gradeEntries.clear();
                    gradeEntries.addAll(list);
                    tableModel.updateData(gradeEntries);

                    // Force a UI refresh of the table and card so Swing paints the new rows
                    SwingUtilities.invokeLater(() -> {
                        showTableView();
                        gradeTable.revalidate();
                        gradeTable.repaint();
                        tableCard.revalidate();
                        tableCard.repaint();
                        System.out.println("Client LOG: Table model rows after update=" + tableModel.getRowCount());
                        try {
                            if (tableModel.getRowCount() > 0) {
                                // select the first row to make it visually apparent
                                if (gradeTable.getRowCount() > 0) {
                                    gradeTable.setRowSelectionInterval(0, 0);
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    });

                    // Auto-lock if server indicates final grades already present for all
                    boolean allFinal = gradeEntries.stream().allMatch(e -> e.finalGrade != null && !e.finalGrade.isBlank());
                    isFinalized = allFinal;
                    tableModel.setEditable(!isFinalized);
                    if (isFinalized) {
                        finalizeBtn.setText("Finalized");
                        finalizeBtn.setBackground(Color.decode("#6B7280"));
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
        if (gradeEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No grades available to compute stats.", "Stats", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Compute per-student weighted average using server weightings
        // FINAL GRADE WEIGHTING RULE: Quiz 15%, Assignment 20%, Midterm 30%, Endterm 35%
        final double W_QUIZ = 0.15;
        final double W_ASSIGNMENT = 0.20;
        final double W_MIDTERM = 0.30;
        final double W_ENDTERM = 0.35;

        java.util.List<Double> values = new ArrayList<>();
        for (GradeEntry ge : gradeEntries) {
            // If a component is missing, treat as 0.0 (same as server calculation)
            double quiz = ge.scores.getOrDefault("Quiz", 0.0);
            double assignment = ge.scores.getOrDefault("Assignment", 0.0);
            double midterm = ge.scores.getOrDefault("Midterm", 0.0);
            double endterm = ge.scores.getOrDefault("Endterm", 0.0);
            double weighted = quiz * W_QUIZ + assignment * W_ASSIGNMENT + midterm * W_MIDTERM + endterm * W_ENDTERM;
            values.add(weighted);
        }

        if (values.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No numeric grades available to compute stats.", "Stats", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Calculate average, min, max, median
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        java.util.List<Double> sorted = new ArrayList<>(values);
        java.util.Collections.sort(sorted);
        double median;
        int n = sorted.size();
        if (n % 2 == 1) median = sorted.get(n / 2);
        else median = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;

        // Prepare distribution counts (use finalGrade if present, otherwise infer from weighted numeric average)
    // Use server letter buckets including minus grades: A, A-, B, B-, C, C-, D, F
    Map<String, Long> dist = new HashMap<>();
    dist.put("A", 0L);
    dist.put("A-", 0L);
    dist.put("B", 0L);
    dist.put("B-", 0L);
    dist.put("C", 0L);
    dist.put("C-", 0L);
    dist.put("D", 0L);
    dist.put("F", 0L);
        for (int i = 0; i < gradeEntries.size(); i++) {
            GradeEntry ge = gradeEntries.get(i);
            String grade = ge.finalGrade;
            if (grade != null && !grade.isBlank()) {
                grade = grade.trim().toUpperCase();
                // If server returned a letter like "A-" or "B+" map to the base bucket
                if (dist.containsKey(grade)) {
                    // exact match (e.g. A-)
                    dist.put(grade, dist.get(grade) + 1);
                    continue;
                } else {
                    // fallback: map by prefix to the nearest base bucket
                    if (grade.startsWith("A")) {
                        dist.put("A", dist.get("A") + 1);
                        continue;
                    } else if (grade.startsWith("B")) {
                        dist.put("B", dist.get("B") + 1);
                        continue;
                    } else if (grade.startsWith("C")) {
                        dist.put("C", dist.get("C") + 1);
                        continue;
                    } else if (grade.startsWith("D")) {
                        dist.put("D", dist.get("D") + 1);
                        continue;
                    }
                }
            }
            // infer from weighted numeric average for this entry (align with server weights)
            double quiz = ge.scores.getOrDefault("Quiz", 0.0);
            double assignment = ge.scores.getOrDefault("Assignment", 0.0);
            double midterm = ge.scores.getOrDefault("Midterm", 0.0);
            double endterm = ge.scores.getOrDefault("Endterm", 0.0);
            double weightedVal = quiz * W_QUIZ + assignment * W_ASSIGNMENT + midterm * W_MIDTERM + endterm * W_ENDTERM;
            // Mirror server thresholds including minus grades
            String letter;
            if (weightedVal >= 90.0) letter = "A";
            else if (weightedVal >= 80.0) letter = "A-";
            else if (weightedVal >= 70.0) letter = "B";
            else if (weightedVal >= 60.0) letter = "B-";
            else if (weightedVal >= 50.0) letter = "C";
            else if (weightedVal >= 40.0) letter = "C-";
            else if (weightedVal >= 30.0) letter = "D";
            else letter = "F";
            dist.put(letter, dist.get(letter) + 1);
        }

        // Create visual components
        StatsGaugePanel gauge = new StatsGaugePanel();
        gauge.setPreferredSize(new Dimension(240, 240));
        // StatsGaugePanel expects 0..1 values
        gauge.setValues(avg / 100.0, min / 100.0, max / 100.0, median / 100.0);

    GradeDistributionPanel chart = new GradeDistributionPanel();
        chart.setPreferredSize(new Dimension(340, 240));
        chart.setData(dist);

    // Legend panel (show colors for gauge values + distribution)
    JPanel legend = new JPanel(new java.awt.GridLayout(6, 1, 6, 6));
    legend.setOpaque(false);
    legend.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    // Colors used by the gauge and chart (match the rendering in the panels)
    Color colAvg = new Color(59, 130, 246);
    Color colMin = new Color(34, 197, 94);
    Color colMax = new Color(255, 215, 0); //gold
    Color colMed = new Color(99, 102, 241);
    Color colBar = new Color(255, 36, 0); //scarlet

    // Apply requested colors to the gauge and chart (set after colors are defined)
    gauge.setColors(new Color[]{colAvg, colMin, colMax, colMed});
    chart.setBarColor(colBar);

    legend.add(makeLegendRow(colAvg, "Average (gauge)"));
    legend.add(makeLegendRow(colMin, "Min (gauge)"));
    legend.add(makeLegendRow(colMax, "Max (gauge)"));
    legend.add(makeLegendRow(colMed, "Median (gauge)"));
    legend.add(makeLegendRow(colBar, "Grade distribution (bars)"));

    JLabel note = new JLabel("Using server weights; prefers server final grade when present.");
    note.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
    note.setForeground(secondaryTextColor());
    legend.add(note);

        // Numeric summary panel
        JPanel nums = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        nums.setOpaque(false);
        JLabel avgL = new JLabel(String.format("Average: %.2f", avg));
        JLabel minL = new JLabel(String.format("Min: %.2f", min));
        JLabel maxL = new JLabel(String.format("Max: %.2f", max));
        JLabel medL = new JLabel(String.format("Median: %.2f", median));
        avgL.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        minL.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        maxL.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        medL.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        avgL.setForeground(textColor());
        minL.setForeground(secondaryTextColor());
        maxL.setForeground(secondaryTextColor());
        medL.setForeground(secondaryTextColor());
        nums.add(avgL);
        nums.add(minL);
        nums.add(maxL);
        nums.add(medL);

        // Assemble dialog
        javax.swing.JDialog dlg = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(this), "Course Stats", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.setOpaque(false);

    JPanel top = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 8));
    top.setOpaque(false);
    top.add(gauge);
    top.add(chart);
    // Add the legend to the top area so it's visible next to the chart
    top.add(legend);

        content.add(top, BorderLayout.CENTER);
        content.add(nums, BorderLayout.SOUTH);

        dlg.setContentPane(content);
    dlg.pack();
    // Increase default size so legend and charts are visible comfortably
    // Increase default size vertically so legend and charts are comfortably visible
    dlg.setSize(820, 580);
    // Prevent too-small resizing that would hide legend/chart
    dlg.setMinimumSize(new Dimension(700, 480));
        dlg.setLocationRelativeTo(this);
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
        private List<GradeEntry> rows = new ArrayList<>();

        void setEditable(boolean e) {
            this.editable = e;
            fireTableDataChanged();
        }

        void updateData(List<GradeEntry> list) {
            this.rows = new ArrayList<>(list);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
            GradeEntry e = rows.get(row);
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
            GradeEntry e = rows.get(row);
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
        private Color[] cols = new Color[]{new Color(59, 130, 246), new Color(34, 197, 94), new Color(245, 158, 11), new Color(99, 102, 241)};

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
            // use instance-level colors (may be overridden)
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

        // Allow runtime override of ring colors (avg, min, max, median)
        public void setColors(Color[] colors) {
            if (colors != null && colors.length >= 4) {
                this.cols = new Color[]{colors[0], colors[1], colors[2], colors[3]};
                repaint();
            }
        }
    }

    // Simple bar chart panel for grade distribution
    private static class GradeDistributionPanel extends JPanel {

        private Map<String, Long> counts = new HashMap<>();
        private Color barColor = new Color(59, 130, 246);

        void setData(Map<String, Long> c) {
            this.counts = new HashMap<>(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String[] grades = {"A", "A-", "B", "B-", "C", "C-", "D", "F"};
            int w = getWidth() - 40, h = getHeight() - 40;
            int x = 20, y = 20;
            long max = 1;
            for (String gr : grades) {
                max = Math.max(max, counts.getOrDefault(gr, 0L));
            }
            int barW = Math.max(14, w / (grades.length * 2));
            int gap = barW;
            for (int i = 0; i < grades.length; i++) {
                long v = counts.getOrDefault(grades[i], 0L);
                int bh = (int) (h * (v / (double) max));
                int bx = x + i * (barW + gap) + gap / 2;
                int by = y + h - bh;
                g2.setColor(barColor);
                g2.fillRoundRect(bx, by, barW, bh, 8, 8);
                g2.setColor(FlatLaf.isLafDark() ? Color.WHITE : Color.decode("#0F172A"));
                g2.drawString(grades[i], bx + barW / 2 - g2.getFontMetrics().stringWidth(grades[i]) / 2, y + h + 16);
            }
            g2.dispose();
        }

        public void setBarColor(Color c) {
            if (c != null) {
                this.barColor = c;
                repaint();
            }
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

    // Helper to create a legend row with a small color box and label
    private JPanel makeLegendRow(Color col, String text) {
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 2));
        row.setOpaque(false);
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(14, 12));
        box.setBackground(col);
        box.setBorder(BorderFactory.createLineBorder(borderColor(), 1));
        row.add(box);
        JLabel lbl = new JLabel(text);
        lbl.setForeground(textColor());
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        row.add(lbl);
        return row;
    }
}

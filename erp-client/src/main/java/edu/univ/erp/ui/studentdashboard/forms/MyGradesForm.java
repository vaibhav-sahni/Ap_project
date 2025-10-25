package edu.univ.erp.ui.studentdashboard.forms;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import net.miginfocom.swing.MigLayout;

/**
 * A form for students to view their grades for all registered courses. This
 * view is read-only, as students cannot change their grades. It includes a
 * "Download Transcript" button as required by the project brief. [cite: 30, 33]
 */
public class MyGradesForm extends SimpleForm {

    private JTable gradesTable;
    private GradesTableModel tableModel;
    private JTextField searchField;
    // semester column removed — we no longer display or filter by semester here
    private JButton clearBtn;
    private JButton downloadButton;
    private JLabel statsLabel; // shows "Viewing grades for X courses"
    private JLabel titleLabel;
    private JPanel filterPanel;
    private JPanel searchPanelContainer; // holds the rounded search box
    private CardPanel tablePanel;
    private FadePanel contentFadePanel; // wraps the table for fade-in

    private boolean openAnimationPlayed = false;

    // Data storage: master list of grade entries
    private List<GradeEntry> gradeEntries;

    /**
     * Data model for a student's grade in one section. Includes scores for
     * components and a final grade.
     */
    private static class GradeEntry {

        String courseCode;
        String courseTitle;
        String quiz;
        String midterm;
        String assignment;
        String endTerm;
        String finalGrade; // e.g., "A", "B+", "F", or "IP" (In Progress)

        public GradeEntry(String code, String title, String quiz, String mid, String assign, String end, String finalG) {
            this.courseCode = code;
            this.courseTitle = title;
            // Handle null scores gracefully
            this.quiz = (quiz == null) ? "N/A" : quiz;
            this.midterm = (mid == null) ? "N/A" : mid;
            this.assignment = (assign == null) ? "N/A" : assign;
            this.endTerm = (end == null) ? "N/A" : end;
            this.finalGrade = (finalG == null) ? "IP" : finalG; // IP = In Progress
        }
    }

    // Custom TableModel to manage the grade data
    private static class GradesTableModel extends AbstractTableModel {

        // Columns updated to show grade components 
    private final String[] COLUMN_NAMES = {"Code", "Title", "Quiz", "Midterm", "Assignment", "End-Term", "Final Grade"};
        private List<GradeEntry> entries;

        public GradesTableModel(List<GradeEntry> entries) {
            this.entries = entries;
        }

        public void updateEntries(List<GradeEntry> newEntries) {
            this.entries = newEntries;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            // Student view is read-only [cite: 18]
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GradeEntry entry = entries.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return entry.courseCode;
                case 1:
                    return entry.courseTitle;
                case 2:
                    return entry.quiz;
                case 3:
                    return entry.midterm;
                case 4:
                    return entry.assignment;
                case 5:
                    return entry.endTerm;
                case 6:
                    return entry.finalGrade; // Pass the grade string to the renderer
                default:
                    return null;
            }
        }
    }

    public MyGradesForm() {
        init();
    }

    @Override
    public void formInitAndOpen() {
        // Fetch student grades from server and populate the table
        edu.univ.erp.domain.UserAuth cu = null;
        try {
            cu = edu.univ.erp.ClientContext.getCurrentUser();
        } catch (Throwable ignore) {}
        if (cu == null) return;
        final int uid = cu.getUserId();
        edu.univ.erp.api.student.StudentAPI api = new edu.univ.erp.api.student.StudentAPI();
        edu.univ.erp.ui.utils.UIHelper.runAsync(() -> api.getMyGrades(uid), (java.util.List<edu.univ.erp.domain.Grade> grades) -> {
            if (grades == null) return;
            java.util.List<GradeEntry> converted = new java.util.ArrayList<>();
            for (edu.univ.erp.domain.Grade g : grades) {
                String course = g.getCourseName() == null ? "Unknown Course" : g.getCourseName();
                // Map assessment components into the common slots if names match
                String quiz = "N/A", mid = "N/A", assign = "N/A", end = "N/A";
                if (g.getComponents() != null) {
                    for (edu.univ.erp.domain.AssessmentComponent ac : g.getComponents()) {
                        String n = ac.getComponentName() == null ? "" : ac.getComponentName().toLowerCase();
                        String val = String.valueOf(ac.getScore());
                        if (n.contains("quiz")) quiz = val;
                        else if (n.contains("mid")) mid = val;
                        else if (n.contains("assign")) assign = val;
                        else if (n.contains("end") || n.contains("final")) end = val;
                    }
                }
                GradeEntry e = new GradeEntry(course, course, quiz, mid, assign, end, g.getFinalGrade());
                converted.add(e);
            }
            gradeEntries = converted;
            tableModel.updateEntries(new ArrayList<>(gradeEntries));
            updateStats();
            if (contentFadePanel != null) contentFadePanel.startFadeIn(0);
        }, (Exception ex) -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    gradeEntries.clear();
                    tableModel.updateEntries(new ArrayList<>(gradeEntries));
                    updateStats();
                } catch (Throwable ignore) {}
                javax.swing.JOptionPane.showMessageDialog(this, "Failed to load grades: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            });
        });
    }

    private void init() {
        gradeEntries = new ArrayList<>();
        // start empty; will fetch authoritative grades from server when opened

        setLayout(new MigLayout("fill, insets 20", "[fill]", "[]20[]20[grow,fill]"));

        // Title with stats
        add(createTitlePanel(), "wrap");

        // Enhanced Action/Filter Bar
        add(createActionFilterBar(), "wrap");

        // Grades Table
        tableModel = new GradesTableModel(new ArrayList<>(gradeEntries));
        gradesTable = new ModernTable(tableModel);
        setupTableStyles();

        JScrollPane scrollPane = new JScrollPane(gradesTable);
        styleScrollPane(scrollPane);

        tablePanel = new CardPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentFadePanel = new FadePanel(1f);
        contentFadePanel.setOpaque(false);
        contentFadePanel.setLayout(new BorderLayout());
        contentFadePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(contentFadePanel, BorderLayout.CENTER);

        add(tablePanel, "grow");

        applyThemeColors();

        // Theme change listener
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        javax.swing.SwingUtilities.updateComponentTreeUI(w);
                        w.invalidate();
                        w.validate();
                        w.repaint();
                    } else {
                        javax.swing.SwingUtilities.updateComponentTreeUI(this);
                        revalidate();
                        repaint();
                    }
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // Repaint all custom components
                        if (titleLabel != null) {
                            titleLabel.repaint();
                        }
                        if (statsLabel != null) {
                            statsLabel.repaint();
                        }
                        if (gradesTable != null) {
                            gradesTable.repaint();
                            if (gradesTable.getTableHeader() != null) {
                                gradesTable.getTableHeader().repaint();
                            }
                        }
                        if (searchPanelContainer != null) {
                            searchPanelContainer.repaint();
                        }
                        if (searchField != null) {
                            searchField.repaint();
                        }
                        if (tablePanel != null) {
                            tablePanel.repaint();
                        }
                        revalidate();
                        repaint();
                    });
                });
            }
        });

        // Open animation
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !openAnimationPlayed) {
                openAnimationPlayed = true;
                formRefresh();
            }
        });
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        titlePanel.setOpaque(false);

        titleLabel = new JLabel("My Grades") {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        titlePanel.add(titleLabel);

        statsLabel = new JLabel(gradeEntries.size() + " courses found") {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setForeground(dark ? Color.decode("#999999") : Color.decode("#5C5C5C"));
                super.paintComponent(g);
            }
        };
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        titlePanel.add(statsLabel, "alignx right");

        return titlePanel;
    }

    private void loadDummyData() {
        // Dummy data removed. Form will fetch grades from server.
    }

    private void setupTableStyles() {
        gradesTable.setRowHeight(50);
        gradesTable.setGridColor(Color.decode("#3A3A3A"));
        gradesTable.setBackground(Color.decode("#272727"));
        gradesTable.setForeground(Color.decode("#EAEAEA"));
        gradesTable.setSelectionBackground(Color.decode("#4A4A4A"));
        gradesTable.setSelectionForeground(Color.WHITE);
        gradesTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        gradesTable.setBorder(null);
        gradesTable.setIntercellSpacing(new Dimension(0, 0));
        gradesTable.setRowMargin(0);
        gradesTable.setFillsViewportHeight(true);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());

        // Style Header
        JTableHeader header = new JTableHeader(gradesTable.getColumnModel()) {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setBackground(dark ? Color.decode("#1E1E1E") : Color.decode("#F1F5F9"));
                setForeground(dark ? Color.decode("#B3B3B3") : Color.decode("#475569"));
                super.paintComponent(g);
            }
        };
        gradesTable.setTableHeader(header);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 10));
        header.setDefaultRenderer(new SortHeaderRenderer(gradesTable));

        gradesTable.setShowHorizontalLines(false);
        gradesTable.setShowVerticalLines(false);
        gradesTable.setGridColor(Color.decode("#3C3C3C"));

        // Default cell renderers
        LeftPaddedCellRenderer padded = new LeftPaddedCellRenderer();
        gradesTable.setDefaultRenderer(Object.class, padded);
        gradesTable.setDefaultRenderer(Integer.class, new LeftPaddedCellRenderer());

    // *** NEW: Custom renderer for the "Final Grade" column ***
    gradesTable.getColumnModel().getColumn(6).setCellRenderer(new GradeRenderer());

        // Column sizing
        TableColumnModel columns = gradesTable.getColumnModel();
    columns.getColumn(0).setPreferredWidth(90);  // Code
    columns.getColumn(1).setPreferredWidth(260); // Title
    columns.getColumn(2).setPreferredWidth(90);  // Quiz
    columns.getColumn(3).setPreferredWidth(90);  // Midterm
    columns.getColumn(4).setPreferredWidth(90);  // Assignment
    columns.getColumn(5).setPreferredWidth(90);  // End-Term
    columns.getColumn(6).setPreferredWidth(110); // Final Grade

        // Sorting
        TableRowSorter<GradesTableModel> sorter = new TableRowSorter<>(tableModel);
        gradesTable.setRowSorter(sorter);

        // Sorter for grades (custom comparator can be added if needed)
    sorter.setComparator(6, Comparator.comparing(grade -> (String) grade));
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class LeftPaddedCellRenderer extends DefaultTableCellRenderer {

        // ... (No changes needed, code is identical to your provided file) ...
        private static final Border PAD = BorderFactory.createEmptyBorder(0, 11, 0, 12);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column); // ignore focus
            setBorder(PAD);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class ModernTable extends JTable {

        // ... (No changes needed, code is identical to your provided file) ...
        public ModernTable(TableModel model) {
            super(model);
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

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getGridColor());
            int rows = getRowCount();
            for (int r = 0; r < rows; r++) {
                Rectangle rect = getCellRect(r, 0, true);
                int y = rect.y + rect.height - 1;
                g2.drawLine(0, y, getWidth(), y);
            }
            g2.dispose();
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private void styleScrollPane(JScrollPane scrollPane) {
        // ... (No changes needed, code is identical to your provided file) ...
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
    }

    private JPanel createActionFilterBar() {
        filterPanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Professional Search Field with icon
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
        this.searchPanelContainer = searchPanel;

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        searchPanel.add(searchIcon, BorderLayout.WEST);

        final String placeholder = "Search by code or title...";
        searchField = new JTextField() {
            // ... (paintComponent logic identical to your MyCoursesForm) ...
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                Color bgColor = dark ? Color.decode("#272727") : Color.WHITE;
                Color textColor = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
                Color placeholderColor = dark ? Color.decode("#666666") : Color.decode("#94A3B8");
                setBackground(bgColor);
                if (getText().equals(placeholder)) {
                    setForeground(placeholderColor); 
                }else {
                    setForeground(textColor);
                }
                setCaretColor(textColor);
                super.paintComponent(g);
            }
        };
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 10));
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        boolean dark = FlatLaf.isLafDark();
        searchField.setForeground(dark ? Color.decode("#666666") : Color.decode("#94A3B8"));
        searchField.setText(placeholder);

        searchField.addFocusListener(new FocusAdapter() {
            // ... (focusGained/focusLost logic identical to your MyCoursesForm) ...
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
                applyFilters();
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        filterPanel.add(searchPanel, "h 40!, growx");

    // Right cluster: Clear, Download Transcript
        JPanel rightCluster = new JPanel(new MigLayout("insets 0", "[]8[]8[]", "[]"));
        rightCluster.setOpaque(false);

    // (semester filter removed)

        // Clear Filters Button
        clearBtn = new RoundButton("Clear", 12);
        // ... (Styling and action listener identical to your MyCoursesForm) ...
        clearBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        clearBtn.setForeground(Color.decode("#CCCCCC"));
        clearBtn.setBackground(Color.decode("#555555"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        clearBtn.setPreferredSize(new Dimension(100, 40));
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.setVisible(true);
        clearBtn.setEnabled(false);
        clearBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (clearBtn.isEnabled()) {
                    clearBtn.setBackground(Color.decode("#DC2626"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearBtn.setBackground(clearBtn.isEnabled() ? Color.decode("#EF4444") : Color.decode("#555555"));
            }
        });
        clearBtn.addActionListener(e -> clearFilters());
        rightCluster.add(clearBtn, "h 40!, aligny center");

        // *** NEW: Download Transcript Button  ***
        downloadButton = new RoundButton("Download Transcript", 12);
        downloadButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setBackground(Color.decode("#3B82F6")); // Blue primary color
        downloadButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        downloadButton.setPreferredSize(new Dimension(160, 40));
        downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadButton.addActionListener(e -> downloadTranscript());

        rightCluster.add(downloadButton, "h 40!, aligny center");

        filterPanel.add(rightCluster, "h 40!, alignx right");

        return filterPanel;
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String placeholder = "Search by code or title...";

    // no semester filter; only search affects active filters
    boolean hasActiveFilters = (!searchText.isEmpty() && !searchText.equals(placeholder));

        clearBtn.setEnabled(hasActiveFilters);
        clearBtn.setBackground(hasActiveFilters ? Color.decode("#EF4444") : Color.decode("#555555"));
        clearBtn.setForeground(hasActiveFilters ? Color.WHITE : Color.decode("#CCCCCC"));
        filterPanel.revalidate();
        filterPanel.repaint();

        List<GradeEntry> filtered = gradeEntries.stream()
                .filter(entry -> {
                    // Search filter
                    if (!searchText.isEmpty() && !searchText.equals(placeholder)) {
                        String search = searchText.toLowerCase();
                        boolean matches = entry.courseCode.toLowerCase().contains(search)
                                || entry.courseTitle.toLowerCase().contains(search);
                        if (!matches) {
                            return false;
                        }
                    }

                    // No semester filtering in this view
                    return true;
                })
                .collect(Collectors.toList());

        tableModel.updateEntries(filtered);
        updateStats();
    }

    private void clearFilters() {
        String placeholder = "Search by code or title...";
        searchField.setText(placeholder);
        searchField.setForeground(Color.decode("#666666"));

    // semester filter removed

        clearBtn.setEnabled(false);
        clearBtn.setBackground(Color.decode("#555555"));
        clearBtn.setForeground(Color.decode("#CCCCCC"));
        filterPanel.revalidate();
        filterPanel.repaint();

        tableModel.updateEntries(new ArrayList<>(gradeEntries));
        updateStats();
    }

    /**
     * Placeholder action for the Download Transcript button.
     */
    private void downloadTranscript() {
        // Wire to real transcript download logic: use StudentUiHandlers to download and prompt save
        try {
            edu.univ.erp.domain.UserAuth cu = null;
            try { cu = edu.univ.erp.ClientContext.getCurrentUser(); } catch (Throwable ignore) {}
            if (cu == null) {
                JOptionPane.showMessageDialog(this, "No authenticated user available.", "Download Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Delegate to existing handler which performs background download and file-save on EDT
            new edu.univ.erp.ui.handlers.StudentUiHandlers(cu).downloadTranscriptAndSave(this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to start transcript download: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // (This method is copied directly from your MyCoursesForm)
    private void styleComboBox(JComboBox<String> combo) {
        // ... (No changes needed, code is identical to your provided file) ...
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        combo.addHierarchyListener(e -> {
            boolean dark = FlatLaf.isLafDark();
            combo.setBackground(dark ? Color.decode("#272727") : Color.WHITE);
            combo.setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
            combo.setBorder(new RoundedBorder(10, dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0")));
        });
        boolean dark = FlatLaf.isLafDark();
        combo.setBackground(dark ? Color.decode("#272727") : Color.WHITE);
        combo.setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
        combo.setBorder(new RoundedBorder(10, dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0")));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean dark = FlatLaf.isLafDark();
                Color inputBg = dark ? Color.decode("#272727") : Color.WHITE;
                setBackground(isSelected ? Color.decode("#3B82F6") : inputBg);
                setForeground(dark ? Color.WHITE : Color.decode("#1E1E1E"));
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                return this;
            }
        });
    }

    // (This method is copied directly from your MyCoursesForm)
    private void applyThemeColors() {
        // ... (No changes needed, code is identical to your provided file) ...
        // ... (This method updates all custom component colors) ...
        boolean dark = FlatLaf.isLafDark();
        Color bgWindow = dark ? Color.decode("#1E1E1E") : Color.WHITE;
        Color textPrimary = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
        Color textSecondary = dark ? Color.decode("#999999") : Color.decode("#5C5C5C");
        Color inputBg = dark ? Color.decode("#272727") : Color.WHITE;
        Color border = dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0");
        Color tableBg = dark ? Color.decode("#272727") : Color.WHITE;
        Color tableFg = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
        Color selectionBg = dark ? Color.decode("#4A4A4A") : Color.decode("#CCE0FF");
        Color selectionFg = dark ? Color.WHITE : Color.decode("#0F172A");
        Color grid = dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB");
        Color headerBg = dark ? Color.decode("#1E1E1E") : Color.decode("#F1F5F9");
        Color headerFg = dark ? Color.decode("#B3B3B3") : Color.decode("#475569");
        Color scrollbarThumb = dark ? Color.decode("#4A4A4A") : Color.decode("#C7C7CC");
        Color scrollbarTrack = dark ? Color.decode("#272727") : Color.decode("#F2F2F7");

        setBackground(bgWindow);
        if (titleLabel != null) {
            titleLabel.setForeground(textPrimary);
        }
        if (statsLabel != null) {
            statsLabel.setForeground(textSecondary);
        }

        if (gradesTable != null) {
            gradesTable.setBackground(tableBg);
            gradesTable.setForeground(tableFg);
            gradesTable.setSelectionBackground(selectionBg);
            gradesTable.setSelectionForeground(selectionFg);
            gradesTable.setGridColor(grid);
            JTableHeader h = gradesTable.getTableHeader();
            if (h != null) {
                h.setBackground(headerBg);
                h.setForeground(headerFg);
                h.repaint();
            }
            JScrollPane sp = (JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, gradesTable);
            if (sp != null) {
                sp.getVerticalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
                sp.getHorizontalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
                sp.repaint();
            }
            gradesTable.repaint();
        }

        if (searchPanelContainer != null) {
            searchPanelContainer.setBorder(new RoundedBorder(10, border));
            searchPanelContainer.setBackground(inputBg);
            searchPanelContainer.repaint();
        }
        if (searchField != null) {
            searchField.setBackground(inputBg);
            searchField.setForeground(textPrimary);
            searchField.setCaretColor(textPrimary);
            searchField.repaint();
        }

        // semester filter removed from UI

        if (tablePanel != null) {
            tablePanel.repaint();
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class SortHeaderRenderer implements TableCellRenderer {

        // ... (No changes needed, code is identical to your provided file) ...
        // ... (Small tweak: center the "Final Grade" header) ...
        private final JTable table;

        public SortHeaderRenderer(JTable table) {
            this.table = table;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JTableHeader header = table.getTableHeader();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(header.getBackground());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 10));

            JLabel text = new JLabel(value == null ? "" : value.toString());
            text.setFont(header.getFont());
            text.setForeground(header.getForeground());

            // Center only the Final Grade header (model column 7)
            int modelIndexForThis = table.convertColumnIndexToModel(column);
            boolean centerGrade = (modelIndexForThis == 6);
            text.setHorizontalAlignment(centerGrade ? SwingConstants.CENTER : SwingConstants.LEFT);
            panel.add(text, centerGrade ? BorderLayout.CENTER : BorderLayout.WEST);

            Icon icon = null;
            RowSorter<? extends TableModel> sorter = table.getRowSorter();
            if (sorter != null) {
                List<? extends RowSorter.SortKey> keys = sorter.getSortKeys();
                if (!keys.isEmpty()) {
                    int modelIndex = table.convertColumnIndexToModel(column);
                    for (RowSorter.SortKey k : keys) {
                        if (k.getColumn() == modelIndex) {
                            icon = switch (k.getSortOrder()) {
                                case ASCENDING ->
                                    UIManager.getIcon("Table.ascendingSortIcon");
                                case DESCENDING ->
                                    UIManager.getIcon("Table.descendingSortIcon");
                                default ->
                                    null;
                            };
                            break;
                        }
                    }
                }
            }
            JLabel arrow = new JLabel(icon);
            arrow.setHorizontalAlignment(SwingConstants.RIGHT);
            panel.add(arrow, BorderLayout.EAST);
            return panel;
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class RoundButton extends JButton {

        // ... (No changes needed, code is identical to your provided file) ...
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

    // Update the stats label
    private void updateStats() {
        if (statsLabel == null) {
            return;
        }
        int count = (tableModel != null) ? tableModel.getRowCount() : 0;
        statsLabel.setText(count + " courses found");
    }

    // (This method is copied directly from your MyCoursesForm)
    @Override
    public void formRefresh() {
        // ... (No changes needed, code is identical to your provided file) ...
        if (contentFadePanel != null) {
            contentFadePanel.startFadeIn(0);
        }
        if (gradesTable != null) {
            gradesTable.repaint();
        }
        revalidate();
        repaint();
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class CardPanel extends JPanel {

        // ... (No changes needed, code is identical to your provided file) ...
        private float fadeAlpha = 0f;
        private javax.swing.Timer fadeTimer;

        public CardPanel() {
            setOpaque(false);
        }

        public void playFade() {
            if (fadeTimer != null && fadeTimer.isRunning()) {
                fadeTimer.stop();
            }
            fadeAlpha = 0.35f;
            fadeTimer = new javax.swing.Timer(20, e -> {
                fadeAlpha -= 0.05f;
                if (fadeAlpha <= 0f) {
                    fadeAlpha = 0f;
                    ((javax.swing.Timer) e.getSource()).stop();
                }
                repaint();
            });
            fadeTimer.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = FlatLaf.isLafDark();
            Color bgColor = dark ? Color.decode("#272727") : Color.WHITE;
            Color borderColor = dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0");
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 15, 15));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (fadeAlpha > 0f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.SrcOver.derive(fadeAlpha));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class RoundedBorder implements Border {

        // ... (No changes needed, code is identical to your provided file) ...
        private int radius;
        private Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius / 2 + 2, this.radius / 2 + 2, this.radius / 2 + 2, this.radius / 2 + 2);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class FadePanel extends JPanel {

        // ... (No changes needed, code is identical to your provided file) ...
        private float alpha;
        private javax.swing.Timer fadeTimer;

        public FadePanel(float initialAlpha) {
            this.alpha = initialAlpha;
            setOpaque(false);
        }

        public void startFadeIn(int delayMs) {
            Runnable starter = () -> {
                if (fadeTimer != null && fadeTimer.isRunning()) {
                    fadeTimer.stop();
                }
                alpha = 0f;
                fadeTimer = new javax.swing.Timer(16, ev -> {
                    alpha += (1f - alpha) * 0.18f;
                    if (1f - alpha < 0.02f) {
                        alpha = 1f;
                        ((javax.swing.Timer) ev.getSource()).stop();
                    }
                    repaint();
                });
                fadeTimer.start();
            };
            if (delayMs > 0) {
                javax.swing.Timer d = new javax.swing.Timer(delayMs, e -> {
                    ((javax.swing.Timer) e.getSource()).stop();
                    starter.run();
                });
                d.setRepeats(false);
                d.start();
            } else {
                starter.run();
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }
    }

    // (This class is copied directly from your MyCoursesForm)
    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

        // ... (No changes needed, code is identical to your provided file) ...
        private final Color thumb;
        private final Color track;

        public CustomScrollBarUI() {
            this.thumb = Color.decode("#4A4A4A");
            this.track = Color.decode("#272727");
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

    /**
     * *** NEW AESTHETIC RENDERER *** Renders the "Final Grade" as a colored
     * "pill" or "tag". This provides a much cleaner, more modern UI look.
     */
    private static class GradeRenderer extends JPanel implements TableCellRenderer {

        private JLabel label;

        public GradeRenderer() {
            super(new GridBagLayout());
            label = new JLabel();
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10)); // Padding
            add(label, new GridBagConstraints());
            setOpaque(false); // Panel is transparent
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String grade = (String) value;
            label.setText(grade);

            Color bgColor, fgColor;

            // Determine colors based on grade
            if (grade.startsWith("A") || grade.startsWith("B")) {
                bgColor = FlatLaf.isLafDark() ? new Color(22, 101, 52, 150) : new Color(22, 163, 74, 150); // Green
                fgColor = FlatLaf.isLafDark() ? Color.decode("#BBF7D0") : Color.decode("#166534");
            } else if (grade.startsWith("C") || grade.startsWith("D")) {
                bgColor = FlatLaf.isLafDark() ? new Color(120, 53, 15, 150) : new Color(249, 115, 22, 150); // Orange
                fgColor = FlatLaf.isLafDark() ? Color.decode("#FED7AA") : Color.decode("#9A3412");
            } else if (grade.startsWith("F")) {
                bgColor = FlatLaf.isLafDark() ? new Color(127, 29, 29, 150) : new Color(239, 68, 68, 150); // Red
                fgColor = FlatLaf.isLafDark() ? Color.decode("#FECACA") : Color.decode("#991B1B");
            } else { // "IP" (In Progress) or "N/A"
                bgColor = FlatLaf.isLafDark() ? new Color(51, 65, 85, 150) : new Color(100, 116, 139, 150); // Gray
                fgColor = FlatLaf.isLafDark() ? Color.decode("#E2E8F0") : Color.decode("#F1F5F9");
            }

            // Set colors for the label
            label.setForeground(fgColor);

            // Set background color for the pill (which will be painted by this JPanel)
            setBackground(bgColor);

            // Handle selection
            if (isSelected) {
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                }
            }

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Paint the rounded background "pill"
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            // Center the pill in the cell
            int pillWidth = label.getPreferredSize().width;
            int pillHeight = label.getPreferredSize().height;
            int x = (getWidth() - pillWidth) / 2;
            int y = (getHeight() - pillHeight) / 2;
            g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight); // Fully rounded
            g2.dispose();

            // The super.paintComponent(g) will paint the label on top
            super.paintComponent(g);
        }
    }
}
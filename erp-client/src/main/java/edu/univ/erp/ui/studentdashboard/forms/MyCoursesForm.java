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
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractCellEditor;
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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import net.miginfocom.swing.MigLayout;

/**
 * MyCoursesForm
 * <p>
 * Student-facing view showing the courses the current user has taken or is
 * currently registered for. This form merges three data sources:
 * - timetable (registered/currently enrolled sections),
 * - course catalog (metadata like credits, section, instructor), and
 * - grades/transcript (to mark completed courses).
 *
 * The UI displays a searchable, filterable table with columns for code,
 * title, credits, section, schedule, room, instructor, status and actions.
 */
public class MyCoursesForm extends SimpleForm {

    // Main table showing courses (backed by CourseTableModel)
    private JTable courseTable;
    private CourseTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> deptFilter;
    private JComboBox<String> creditsFilter;
    private JButton clearBtn;
    /** Label used to show current count, e.g. "5 courses registered" */
    private JLabel statsLabel;
    /** Title label for the form */
    private JLabel titleLabel;
    private JPanel filterPanel;
    private JPanel searchPanelContainer; // holds the rounded search box for re-theming
    private CardPanel tablePanel;
    private FadePanel contentFadePanel; // wraps the table to enable fade-in like Dashboard notifications
    // status renderer reference (unused but kept for symmetry)
    private TableCellRenderer statusRenderer; // renderer for Status column
    private boolean openAnimationPlayed = false; // ensure open animation runs once
    // theme-driven state
    private Color capacityTrackColor = Color.decode("#3A3A3A");

    // Hover tracking for capacity cell behavior
    private int hoverRow = -1;
    private int hoverCol = -1;

    // Data storage: master list of courses for this simplified view
    private List<CourseSection> courses;

    // Data model for a course section
    private static class CourseSection {
        /** Short unique course identifier shown in table (e.g., CS101) */
        String courseCode;
        /** Human-readable course title */
        String courseTitle;
        /** Number of credits for the course (may be 0 if unknown) */
        int credits;
        /** Section id as string (may be "N/A" for unknown) */
        String sectionId;
        /** Day/time string (display-only) */
        String dayTime;
        /** Room / location */
        String room;
        /** Instructor name */
        String instructorName;
        /** Number of students currently enrolled in this section */
        int enrolledCount;
        /** Maximum capacity for the section */
        int capacity;
        /** Whether the current user is currently registered in this section */
        boolean isRegistered;
        /** Whether the student is allowed to drop this section (deadline checks) */
        boolean dropAllowed = true;
        /** Whether this course/section is completed (final grade recorded) */
        boolean isCompleted = false;

        /**
         * Construct a lightweight CourseSection used by the UI. Fields are
         * intentionally simple and mostly display-oriented; the authoritative
         * data comes from server APIs (timetable/catalog/grades).
         */
        public CourseSection(String code, String title, int credits, String secId, String time, String room, String instr, int enrolled, int cap, boolean registered) {
            this.courseCode = code;
            this.courseTitle = title;
            this.credits = credits;
            this.sectionId = secId;
            this.dayTime = time;
            this.room = room;
            this.instructorName = instr;
            this.enrolledCount = enrolled;
            this.capacity = cap;
            this.isRegistered = registered;
        }

        /**
         * Compute a simple availability metric used for UI hints. Note:
         * capacity may be 0 (unknown) which would cause division by zero; the
         * surrounding code never relies on this method for correctness.
         */
        public double getAvailability() {
            if (capacity <= 0) return 0.0;
            return (double) enrolledCount / capacity;
        }
    }

    // Custom TableModel to manage the course data
    private static class CourseTableModel extends AbstractTableModel {

        private final String[] COLUMN_NAMES = {"Code", "Title", "Credits", "Section", "Schedule", "Room", "Instructor", "Status", "Actions"};
        private List<CourseSection> courses;

        public CourseTableModel(List<CourseSection> courses) {
            this.courses = courses;
        }

        public void updateCourses(List<CourseSection> newCourses) {
            this.courses = newCourses;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return courses.size();
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
            return col == 8; // Only Actions column is editable
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CourseSection course = courses.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return course.courseCode;
                case 1:
                    return course.courseTitle;
                case 2:
                    return course.credits;
                case 3:
                    return course.sectionId;
                case 4:
                    return course.dayTime;
                case 5:
                    return course.room;
                case 6:
                    return course.instructorName;
                case 7:
                    return course; // Status column - renderer will read flags and show text
                case 8:
                    return course; // Pass the whole object to the editor/renderer
                default:
                    return null;
            }
        }
    }

    public MyCoursesForm() {
        init();
    }

    private void init() {
        // Initialize master course list
        courses = new ArrayList<>();

        // Load simple dummy data into courses (some registered, some completed)
        loadDummyData();

        // Background will be set by applyThemeColors()
        setLayout(new MigLayout("fill, insets 20", "[fill]", "[]20[]20[grow,fill]"));

        // Title with stats
        add(createTitlePanel(), "wrap");

        // Enhanced Filter Bar
        add(createFilterBar(), "wrap");

        // Course Table
        tableModel = new CourseTableModel(new ArrayList<>(courses));
        courseTable = new ModernTable(tableModel);
        setupTableStyles();

        JScrollPane scrollPane = new JScrollPane(courseTable);
        styleScrollPane(scrollPane);

        // Add table to a CardPanel for consistent styling
        tablePanel = new CardPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Wrap the scroll pane in a FadePanel to replicate Dashboard notifications fade-in
        contentFadePanel = new FadePanel(1f); // start fully visible on first render
        contentFadePanel.setOpaque(false);
        contentFadePanel.setLayout(new BorderLayout());
        contentFadePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(contentFadePanel, BorderLayout.CENTER);

        add(tablePanel, "grow");

        // Apply current theme colors once
        applyThemeColors();

        // Refresh the entire window when Look & Feel (mode) changes
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
                    // Force repaint of all custom components after a short delay
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if (titleLabel != null) {
                            titleLabel.repaint();
                        }
                        if (statsLabel != null) {
                            statsLabel.repaint();
                        }
                        if (courseTable != null) {
                            courseTable.repaint();
                            if (courseTable.getTableHeader() != null) {
                                courseTable.getTableHeader().repaint();
                            }
                        }
                        if (searchPanelContainer != null) {
                            searchPanelContainer.repaint();
                        }
                        if (searchField != null) {
                            searchField.repaint();
                        }
                        if (deptFilter != null) {
                            deptFilter.repaint();
                        }
                        if (creditsFilter != null) {
                            creditsFilter.repaint();
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

        // Trigger the same animations as refresh when the form first becomes visible
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !openAnimationPlayed) {
                openAnimationPlayed = true;
                formRefresh();
                // Fetch the real timetable from server on first open (replace dummy data)
                try {
                    // Fetch completed/recorded enrollments from grades/transcript API instead of timetable.
                    // GET_GRADES will return all enrollment rows with final grades when available.
                    edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                    if (cu != null) {
                        final int uid = cu.getUserId();
                        edu.univ.erp.api.student.StudentAPI studentAPI = new edu.univ.erp.api.student.StudentAPI();

                        // We'll fetch three things: the student's timetable (registered sections),
                        // the full catalog (to map credits/section metadata), and grades (completed).
                        // We'll merge timetable (registered) and completed (grades) while avoiding duplicates by sectionId.
                        courses.clear();

                        edu.univ.erp.ui.utils.UIHelper.runAsync(() -> {
                            // Parallel-friendly fetch sequence executed synchronously inside the task
                            java.util.List<edu.univ.erp.domain.CourseCatalog> timetable = null;
                            java.util.List<edu.univ.erp.domain.CourseCatalog> catalog = null;
                            java.util.List<edu.univ.erp.domain.Grade> grades = null;
                            try {
                                timetable = studentAPI.getTimetable(uid);
                            } catch (Exception ignore) {
                            }
                            try {
                                catalog = studentAPI.getCourseCatalog();
                            } catch (Exception ignore) {
                            }
                            try {
                                grades = studentAPI.getMyGrades(uid);
                            } catch (Exception ignore) {
                            }
                            // Return a small wrapper object containing all three
                            java.util.Map<String, Object> map = new java.util.HashMap<>();
                            map.put("timetable", timetable);
                            map.put("catalog", catalog);
                            map.put("grades", grades);
                            return map;
                        }, (@SuppressWarnings("unchecked") java.util.Map<String, Object> results) -> {
                            @SuppressWarnings("unchecked") java.util.List<edu.univ.erp.domain.CourseCatalog> timetable = (java.util.List<edu.univ.erp.domain.CourseCatalog>) results.get("timetable");
                            @SuppressWarnings("unchecked") java.util.List<edu.univ.erp.domain.CourseCatalog> catalog = (java.util.List<edu.univ.erp.domain.CourseCatalog>) results.get("catalog");
                            @SuppressWarnings("unchecked") java.util.List<edu.univ.erp.domain.Grade> grades = (java.util.List<edu.univ.erp.domain.Grade>) results.get("grades");

                            // Use a set of sectionIds to avoid duplicates; some CourseCatalog entries might not have numeric section id
                            java.util.Set<String> seenSections = new java.util.HashSet<>();

                            // Add registered courses from timetable first
                            if (timetable != null) {
                                for (edu.univ.erp.domain.CourseCatalog cc : timetable) {
                                    String secId = String.valueOf(cc.getSectionId());
                                    if (secId == null) secId = "N/A";
                                    if (seenSections.contains(secId)) continue;
                                    seenSections.add(secId);
                                    CourseSection cs = new CourseSection(cc.getCourseCode() == null ? cc.getCourseTitle() : cc.getCourseCode(),
                                            cc.getCourseTitle(), cc.getCredits(), secId, cc.getDayTime(), cc.getRoom(), cc.getInstructorName(), cc.getEnrolledCount(), cc.getCapacity(), true);
                                    cs.dropAllowed = true; // assume timetable entries are registered and droppable unless server marks otherwise
                                    cs.isCompleted = false;
                                    courses.add(cs);
                                }
                            }

                            // Add completed courses from grades, mapping to catalog when possible
                            if (grades != null) {
                                for (edu.univ.erp.domain.Grade g : grades) {
                                    String finalGrade = g.getFinalGrade();
                                    if (finalGrade == null || finalGrade.trim().isEmpty()) continue;
                                    String courseName = g.getCourseName() == null ? "Unknown Course" : g.getCourseName();

                                    // Try to find a matching catalog entry (either in catalog or timetable)
                                    edu.univ.erp.domain.CourseCatalog matched = null;
                                    if (catalog != null) {
                                        for (edu.univ.erp.domain.CourseCatalog cc : catalog) {
                                            if (cc.getCourseTitle() != null && cc.getCourseTitle().equalsIgnoreCase(courseName)) {
                                                matched = cc;
                                                break;
                                            }
                                            if (cc.getCourseCode() != null && cc.getCourseCode().equalsIgnoreCase(courseName)) {
                                                matched = cc;
                                                break;
                                            }
                                        }
                                    }
                                    if (matched == null && timetable != null) {
                                        for (edu.univ.erp.domain.CourseCatalog cc : timetable) {
                                            if (cc.getCourseTitle() != null && cc.getCourseTitle().equalsIgnoreCase(courseName)) {
                                                matched = cc;
                                                break;
                                            }
                                        }
                                    }

                                    String secId = matched != null ? String.valueOf(matched.getSectionId()) : "N/A";
                                    if (seenSections.contains(secId)) {
                                        // If the student is already registered in this section (seen via timetable), mark that row as completed if appropriate
                                        for (CourseSection csExisting : courses) {
                                            if (csExisting.sectionId.equals(secId)) {
                                                csExisting.isCompleted = true;
                                                csExisting.dropAllowed = false; // completed -> not droppable
                                            }
                                        }
                                        continue;
                                    }

                                    String code = matched != null ? matched.getCourseCode() : courseName;
                                    String title = matched != null ? matched.getCourseTitle() : courseName;
                                    int credits = matched != null ? matched.getCredits() : 0;
                                    int enrolled = matched != null ? matched.getEnrolledCount() : 0;
                                    int capacity = matched != null ? matched.getCapacity() : 0;

                                    CourseSection cs = new CourseSection(code, title, credits, secId, "", "", "", enrolled, capacity, false);
                                    cs.dropAllowed = false;
                                    cs.isCompleted = true;
                                    seenSections.add(secId);
                                    courses.add(cs);
                                }
                            }

                            tableModel.updateCourses(new ArrayList<>(courses));
                            updateStats();
                        }, (Exception ex) -> {
                            // On any failure in the async task, keep dummy data and inform user
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(this, "Failed to fetch courses: " + ex.getMessage(), "Fetch Error", javax.swing.JOptionPane.WARNING_MESSAGE);
                            });
                        });
                    }
                } catch (Exception ignore) {
                    // ignore; leave dummy data if ClientContext not available
                }
            }
        });
    }

    private JPanel createTitlePanel() {
        // Title area: shows the page title and a compact stats label
        JPanel titlePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        titlePanel.setOpaque(false);

        titleLabel = new JLabel("My Courses") {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        titlePanel.add(titleLabel);

        // Stats label (promoted to field) - updated via updateStats()
        statsLabel = new JLabel(courses.size() + " courses registered") {
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
        // Some items are marked registered or completed for demo
        CourseSection cs1 = new CourseSection("CS101", "Introduction to Programming", 4, "A", "MWF 9:00-10:00", "A-101", "Dr. Johnson", 95, 120, false);
        CourseSection cs2 = new CourseSection("MATH201", "Calculus II", 4, "B", "TTh 11:00-12:30", "B-205", "Prof. Chen", 72, 80, true); // registered
        CourseSection cs3 = new CourseSection("PHYS150", "General Physics", 3, "A", "MWF 2:00-3:00", "C-112", "Dr. Rodriguez", 60, 60, false); // completed (full)
        CourseSection cs4 = new CourseSection("ENG102", "Academic Writing", 2, "C", "TTh 9:30-10:45", "D-301", "Prof. Wilson", 88, 100, false);
        CourseSection cs5 = new CourseSection("CS250", "Data Structures", 3, "A", "MWF 1:00-2:00", "A-102", "Dr. Lisa Park", 90, 90, true); // registered but full
        CourseSection cs6 = new CourseSection("MATH301", "Linear Algebra", 3, "A", "TTh 2:00-3:30", "B-205", "Prof. Chen", 58, 70, false);
        // Set dropAllowed according to dummy deadlines: cs2 allowed, cs5 drop locked (past deadline), completed courses not droppable
        cs1.dropAllowed = true;
        cs2.dropAllowed = true;
        cs3.dropAllowed = false; // completed/full - no drops
        cs4.dropAllowed = true;
        cs5.dropAllowed = false; // registered but past deadline - cannot drop
        cs6.dropAllowed = true;

        // For MyCoursesForm we only want to show courses the student has taken: registered OR completed (full)
        if (cs1.isRegistered || cs1.enrolledCount >= cs1.capacity) {
            courses.add(cs1);
        }
        if (cs2.isRegistered || cs2.enrolledCount >= cs2.capacity) {
            courses.add(cs2);
        }
        if (cs3.isRegistered || cs3.enrolledCount >= cs3.capacity) {
            courses.add(cs3);
        }
        if (cs4.isRegistered || cs4.enrolledCount >= cs4.capacity) {
            courses.add(cs4);
        }
        if (cs5.isRegistered || cs5.enrolledCount >= cs5.capacity) {
            courses.add(cs5);
        }
        if (cs6.isRegistered || cs6.enrolledCount >= cs6.capacity) {
            courses.add(cs6);
        }
    }

    private void setupTableStyles() {
        courseTable.setRowHeight(50);
        courseTable.setGridColor(Color.decode("#3A3A3A"));
        courseTable.setBackground(Color.decode("#272727"));
        courseTable.setForeground(Color.decode("#EAEAEA"));
        courseTable.setSelectionBackground(Color.decode("#4A4A4A"));
        courseTable.setSelectionForeground(Color.WHITE);
        courseTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        courseTable.setBorder(null);
        // Make header/content alignment exact by removing cell gaps and global focus border
        courseTable.setIntercellSpacing(new Dimension(0, 0));
        courseTable.setRowMargin(0);
        courseTable.setFillsViewportHeight(true);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());

        // Style Header with dynamic theme-aware colors
        JTableHeader header = new JTableHeader(courseTable.getColumnModel()) {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setBackground(dark ? Color.decode("#1E1E1E") : Color.decode("#F1F5F9"));
                setForeground(dark ? Color.decode("#B3B3B3") : Color.decode("#475569"));
                super.paintComponent(g);
            }
        };
        courseTable.setTableHeader(header);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 10));
        header.setDefaultRenderer(new SortHeaderRenderer(courseTable));

    // Track hover for advanced render behaviors (capacity cell shows text on hover)
        courseTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = courseTable.rowAtPoint(e.getPoint());
                int c = courseTable.columnAtPoint(e.getPoint());
                if (r != hoverRow || c != hoverCol) {
                    hoverRow = r;
                    hoverCol = c;
                    courseTable.repaint();
                }
            }
        });
        courseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                hoverCol = -1;
                courseTable.repaint();
            }
        });

        // Draw our own full-width row divider under each course (including under custom cells)
        courseTable.setShowHorizontalLines(false);
        courseTable.setShowVerticalLines(false);
        courseTable.setGridColor(Color.decode("#3C3C3C"));

        // Default cell renderers with consistent left padding and no focus box
        LeftPaddedCellRenderer padded = new LeftPaddedCellRenderer();
        courseTable.setDefaultRenderer(Object.class, padded);
        courseTable.setDefaultRenderer(Integer.class, new LeftPaddedCellRenderer());

        // Set custom renderer for Status column
        statusRenderer = new DefaultTableCellRenderer();
        courseTable.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());

        ActionsCellEditor actionsEditor = new ActionsCellEditor();
        courseTable.getColumnModel().getColumn(8).setCellRenderer(actionsEditor);
        courseTable.getColumnModel().getColumn(8).setCellEditor(actionsEditor);

        // Column sizing for a cleaner look
        TableColumnModel columns = courseTable.getColumnModel();
        columns.getColumn(0).setPreferredWidth(90);   // Code
        columns.getColumn(1).setPreferredWidth(260);  // Title
        columns.getColumn(2).setPreferredWidth(70);   // Credits
        columns.getColumn(4).setPreferredWidth(160);  // Schedule
        columns.getColumn(6).setPreferredWidth(160);  // Instructor
        columns.getColumn(7).setPreferredWidth(120);  // Status
        columns.getColumn(8).setPreferredWidth(140);  // Actions

        // Sorting
        TableRowSorter<CourseTableModel> sorter = new TableRowSorter<>(tableModel);
        courseTable.setRowSorter(sorter);

        // Sorter for credits (as numbers)
        sorter.setComparator(2, Comparator.comparingInt(credits -> (Integer) credits));
        // No special sorter for status column
    }

    // Simple left-padded, no-focus-border renderer for all regular text/number cells
    private static class LeftPaddedCellRenderer extends DefaultTableCellRenderer {

        private static final Border PAD = BorderFactory.createEmptyBorder(0, 11, 0, 12);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column); // ignore focus
            setBorder(PAD);

            // Ensure we use the table's current colors (they update dynamically in ModernTable.paintComponent)
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

    // JTable that draws a full-width horizontal separator line under each row so
    // the divider extends beneath Capacity and Actions custom renderers as well.
    private static class ModernTable extends JTable {

        public ModernTable(TableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Set colors based on current theme before painting
            boolean dark = FlatLaf.isLafDark();
            setBackground(dark ? Color.decode("#272727") : Color.WHITE);
            setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
            setSelectionBackground(dark ? Color.decode("#4A4A4A") : Color.decode("#CCE0FF"));
            setSelectionForeground(dark ? Color.WHITE : Color.decode("#0F172A"));
            setGridColor(dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB"));

            // Update header colors
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
            // draw separators after cells so they appear on top consistently
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

    private void styleScrollPane(JScrollPane scrollPane) {
        // Add a small top padding to prevent any visual overlap with the filter bar borders
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
    }

    private JPanel createFilterBar() {
        // Two-column bar: left = search (grows), right = compact cluster (dept, credits, clear) right-aligned
        filterPanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        filterPanel.setOpaque(false);
        // Ensure the rounded borders (search box / combos) are not visually clipped by the table below
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

    // Search icon (visual only)
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        searchPanel.add(searchIcon, BorderLayout.WEST);

    // Placeholder text used when field is empty
        final String placeholder = "Search by code, title, or instructor...";

        // Search field with dynamic theme colors
        searchField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                Color bgColor = dark ? Color.decode("#272727") : Color.WHITE;
                Color textColor = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
                Color placeholderColor = dark ? Color.decode("#666666") : Color.decode("#94A3B8");

                setBackground(bgColor);
                if (getText().equals(placeholder)) {
                    // Placeholder text
                    setForeground(placeholderColor);
                } else {
                    // Regular text
                    setForeground(textColor);
                    setCaretColor(textColor);
                }
                super.paintComponent(g);
            }
        };
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 10));
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        // Set initial placeholder with theme-aware color
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

        // Real-time search: apply filters as the user types
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        filterPanel.add(searchPanel, "h 40!, growx");

        // Right cluster panel to keep filters close together and vertically centered
        JPanel rightCluster = new JPanel(new MigLayout("insets 0", "[]8[]8[]", "[]"));
        rightCluster.setOpaque(false);

        // Professional Department Filter (same height as search)
        deptFilter = new JComboBox<>(new String[]{"All Departments", "CS", "MATH", "PHYS", "ENG"});
        styleComboBox(deptFilter);
        deptFilter.setPreferredSize(new Dimension(160, 40));
        deptFilter.addActionListener(e -> applyFilters());
        rightCluster.add(deptFilter, "h 40!, aligny center");

        // Professional Credits Filter (same height as search)
        creditsFilter = new JComboBox<>(new String[]{"All Credits", "2 Credits", "3 Credits", "4 Credits"});
        styleComboBox(creditsFilter);
        creditsFilter.setPreferredSize(new Dimension(140, 40));
        creditsFilter.addActionListener(e -> applyFilters());
        // Note: We'll add credits after clearBtn to keep clear at extreme right
        // Clear Filters Button - Neutral grey on open; turns red only when filters are active
        clearBtn = new RoundButton("Clear", 12);
        clearBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        clearBtn.setForeground(Color.decode("#CCCCCC"));
        clearBtn.setBackground(Color.decode("#555555"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        clearBtn.setPreferredSize(new Dimension(100, 40));
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.setVisible(true); // Always visible
        clearBtn.setEnabled(false); // Disabled until any filter is active

        // Hover effect
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
    // Place controls: Department | Credits | Clear (clear at extreme right)
        rightCluster.add(creditsFilter, "h 40!, aligny center");
        rightCluster.add(clearBtn, "h 40!, aligny center");

        // Add the right cluster as a single unit, right-aligned
        filterPanel.add(rightCluster, "h 40!, alignx right");

        return filterPanel;
    }

    private void applyFilters() {
        // Build filter parameters from UI controls
        String searchText = searchField.getText().trim();
        String placeholder = "Search by code, title, or instructor...";

        // Get filter values
        String selectedDept = (String) deptFilter.getSelectedItem();
        String selectedCredits = (String) creditsFilter.getSelectedItem();

        // Check if any filter is active
        boolean hasActiveFilters = (!searchText.isEmpty() && !searchText.equals(placeholder))
                || (selectedDept != null && !selectedDept.equals("All Departments"))
                || (selectedCredits != null && !selectedCredits.equals("All Credits"));

        // Enable/disable clear button based on filter status (always visible)
        clearBtn.setEnabled(hasActiveFilters);
        clearBtn.setBackground(hasActiveFilters ? Color.decode("#EF4444") : Color.decode("#555555"));
        clearBtn.setForeground(hasActiveFilters ? Color.WHITE : Color.decode("#CCCCCC"));
        filterPanel.revalidate();
        filterPanel.repaint();

    // Filter the master courses list using the selected filters and search text
        List<CourseSection> filtered = courses.stream()
                .filter(course -> {
                    // Search filter
                    if (!searchText.isEmpty() && !searchText.equals(placeholder)) {
                        String search = searchText.toLowerCase();
                        boolean matches = course.courseCode.toLowerCase().contains(search)
                                || course.courseTitle.toLowerCase().contains(search)
                                || course.instructorName.toLowerCase().contains(search);
                        if (!matches) {
                            return false;
                        }
                    }

                    // Department filter
                    if (selectedDept != null && !selectedDept.equals("All Departments")) {
                        if (!course.courseCode.startsWith(selectedDept)) {
                            return false;
                        }
                    }

                    // Credits filter
                    if (selectedCredits != null && !selectedCredits.equals("All Credits")) {
                        int filterCredits = Integer.parseInt(selectedCredits.split(" ")[0]);
                        if (course.credits != filterCredits) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Update table
        tableModel.updateCourses(filtered);
        updateStats();
    }

    private void clearFilters() {
        // Reset search field to placeholder and UI combos to defaults
        String placeholder = "Search by code, title, or instructor...";
        searchField.setText(placeholder);
        searchField.setForeground(Color.decode("#666666"));

        // Reset combo boxes
        deptFilter.setSelectedIndex(0);
        creditsFilter.setSelectedIndex(0);

        // Disable clear button (keep visible)
        clearBtn.setEnabled(false);
        clearBtn.setBackground(Color.decode("#555555"));
        clearBtn.setForeground(Color.decode("#CCCCCC"));
        filterPanel.revalidate();
        filterPanel.repaint();

        // Refresh table to show all courses (reset filters)
        tableModel.updateCourses(new ArrayList<>(courses));
        updateStats();
    }

    // Trigger a subtle fade overlay on the table panel (used by Refresh)
    private void playRefreshFade() {
        if (tablePanel != null) {
            tablePanel.playFade();
        }
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Wrap in a panel that repaints with theme colors
        combo.addHierarchyListener(e -> {
            boolean dark = FlatLaf.isLafDark();
            combo.setBackground(dark ? Color.decode("#272727") : Color.WHITE);
            combo.setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
            combo.setBorder(new RoundedBorder(10, dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0")));
        });

        // Set initial colors
        boolean dark = FlatLaf.isLafDark();
        combo.setBackground(dark ? Color.decode("#272727") : Color.WHITE);
        combo.setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
        combo.setBorder(new RoundedBorder(10, dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0")));

        // Custom renderer for dropdown items
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

    // Apply the current Light/Dark theme to all parts of this form
    private void applyThemeColors() {
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
        capacityTrackColor = dark ? Color.decode("#3A3A3A") : Color.decode("#E2E8F0");

        setBackground(bgWindow);
        if (titleLabel != null) {
            titleLabel.setForeground(textPrimary);
        }
        if (statsLabel != null) {
            statsLabel.setForeground(textSecondary);
        }

        if (courseTable != null) {
            courseTable.setBackground(tableBg);
            courseTable.setForeground(tableFg);
            courseTable.setSelectionBackground(selectionBg);
            courseTable.setSelectionForeground(selectionFg);
            courseTable.setGridColor(grid);
            JTableHeader h = courseTable.getTableHeader();
            if (h != null) {
                h.setBackground(headerBg);
                h.setForeground(headerFg);
                h.repaint();
            }
            // Refresh scrollbars
            JScrollPane sp = (JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, courseTable);
            if (sp != null) {
                sp.getVerticalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
                sp.getHorizontalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
                sp.repaint();
            }
            courseTable.repaint();
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

        if (deptFilter != null) {
            deptFilter.setBackground(inputBg);
            deptFilter.setForeground(textPrimary);
            deptFilter.setBorder(new RoundedBorder(10, border));
            deptFilter.repaint();
        }
        if (creditsFilter != null) {
            creditsFilter.setBackground(inputBg);
            creditsFilter.setForeground(textPrimary);
            creditsFilter.setBorder(new RoundedBorder(10, border));
            creditsFilter.repaint();
        }

        // Repaint table panel to pick up new theme colors
        if (tablePanel != null) {
            tablePanel.repaint();
        }
    }

    // Header renderer that keeps text left and pushes sort arrow to extreme right
    private static class SortHeaderRenderer implements TableCellRenderer {

        private final JTable table;
        private final TableCellRenderer base = new DefaultTableCellRenderer();

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
            // Center only the Actions header (model column 8); others stay left-aligned
            int modelIndexForThis = table.convertColumnIndexToModel(column);
            boolean centerActions = (modelIndexForThis == 8);
            text.setHorizontalAlignment(centerActions ? SwingConstants.CENTER : SwingConstants.LEFT);
            panel.add(text, centerActions ? BorderLayout.CENTER : BorderLayout.WEST);

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

    // A pill/rounded button that paints its own rounded background
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

    private List<CourseSection> getDummyCourses() {
        return new ArrayList<>(courses);
    }

    // Getter methods for data management
    // Keep legacy getters but map to the single 'courses' list
    public List<CourseSection> getUnregisteredCourses() {
        return courses;
    }

    public List<CourseSection> getRegisteredCourses() {
        return courses.stream().filter(c -> c.isRegistered).collect(Collectors.toList());
    }

    public void setUnregisteredCourses(List<CourseSection> courses) {
        this.courses = courses;
        tableModel.updateCourses(new ArrayList<>(courses));
        updateStats();
    }

    // Simple helper to mark a course registered (no movement between lists)
    public void addToRegistered(CourseSection course) {
        if (course.enrolledCount < course.capacity) {
            course.enrolledCount += 1;
        }
        course.isRegistered = true;
        tableModel.updateCourses(new ArrayList<>(courses));
        updateStats();
    }

    // Update the stats label to reflect current available courses (filtered rows if table exists)
    private void updateStats() {
        if (statsLabel == null) {
            return;
        }
        int count = 0;
        if (tableModel != null) {
            count = tableModel.getRowCount();
        } else if (courses != null) {
            count = courses.size();
        }
        statsLabel.setText(count + " courses registered");
    }

    @Override
    public void formRefresh() {
        // Keep current filters/search intact; fade text/content like the notification panel
        if (contentFadePanel != null) {
            contentFadePanel.startFadeIn(0);
        }
        // No capacity animations for status column; just repaint
        if (courseTable != null) {
            courseTable.repaint();
        }
        revalidate();
        repaint();
    }

    // On window open handled via HierarchyListener above to avoid framework-specific overrides
    // Custom Components as Inner Classes
    private static class CardPanel extends JPanel {

        private float fadeAlpha = 0f;
        private javax.swing.Timer fadeTimer;

        public CardPanel() {
            setOpaque(false);
        }

        public void playFade() {
            if (fadeTimer != null && fadeTimer.isRunning()) {
                fadeTimer.stop();
            }
            fadeAlpha = 0.35f; // start slightly visible
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

            // Fetch colors dynamically based on current theme
            boolean dark = FlatLaf.isLafDark();
            Color bgColor = dark ? Color.decode("#272727") : Color.WHITE;
            Color borderColor = dark ? Color.decode("#4A4A4A") : Color.decode("#D0D0D0");

            // Fill the background with theme-aware color
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));

            // Draw the border with theme-aware color
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

    private static class RoundedBorder implements Border {

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

    // Generic fading container (replicates DashboardForm's notification fade)
    private static class FadePanel extends JPanel {

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
                // Restart fade from zero for a visible effect every time
                alpha = 0f;
                fadeTimer = new javax.swing.Timer(16, ev -> {
                    alpha += (1f - alpha) * 0.18f; // ease-out
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

    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

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

    // Simple renderer for Status column
    /**
     * Renderer for the Status column. Reads flags from CourseSection and
     * renders a centered text label: "Registered" or "Completed".
     */
    private class StatusRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            CourseSection course = (CourseSection) value;
            if (course == null) {
                setText("");
                return this;
            }
            // Determine status: Registered, Completed, or blank. Use explicit isCompleted flag first.
            String statusText = "";
            if (course.isRegistered) {
                statusText = "Registered";
            } else if (course.isCompleted) {
                statusText = "Completed";
            } else if (course.enrolledCount >= course.capacity) {
                statusText = "Completed"; // legacy fallback: treat full as completed
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            boolean dark = FlatLaf.isLafDark();
            setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#0F172A"));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setText(statusText);
            return this;
        }
    }

    // Custom Renderer/Editor for the Actions column
    /**
     * Renderer + Editor for the Actions column. Displays a contextual
     * button (e.g., Drop) when applicable. This class implements both the
     * TableCellRenderer and TableCellEditor interfaces so that the same UI
     * component is shown while editing.
     */
    private class ActionsCellEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JButton actionButton = new RoundButton("", 12);

        public ActionsCellEditor() {
            panel.setOpaque(true);
            actionButton.setVisible(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(actionButton, gbc);
        }

        private void styleButton(JButton button, Color color) {
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(color);
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        private void updateButton(JTable table, Object value, boolean isSelected) {
            CourseSection course = (CourseSection) value;
            panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            // Only allow drop if the student is registered and dropAllowed is true
            if (course.isRegistered && course.dropAllowed) {
                actionButton.setText("Drop");
                styleButton(actionButton, Color.decode("#EF4444"));
                actionButton.setVisible(true);
            } else {
                // No actions for completed or non-registered courses in this view
                actionButton.setVisible(false);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            updateButton(table, value, isSelected);
            return panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            updateButton(table, value, isSelected);
            // remove existing listeners to avoid stacking
            for (java.awt.event.ActionListener al : actionButton.getActionListeners()) {
                actionButton.removeActionListener(al);
            }

            actionButton.addActionListener(e -> {
                fireEditingStopped();
                CourseSection cs = (CourseSection) value;
                // Only Drop path should be reachable here (button only visible then)
                if (cs.isRegistered && cs.dropAllowed) {
                    // Perform real drop via StudentAPI
                    edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                    if (cu == null) {
                        JOptionPane.showMessageDialog(table, "Not authenticated.", "Drop Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    final int uid = cu.getUserId();
                    final int sectionId;
                    try {
                        sectionId = Integer.parseInt(cs.sectionId);
                    } catch (NumberFormatException nfe) {
                        // If section id isn't numeric, attempt to use 0 and show error
                        JOptionPane.showMessageDialog(table, "Invalid section id for drop.", "Drop Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    edu.univ.erp.api.student.StudentAPI studentAPI = new edu.univ.erp.api.student.StudentAPI();
                    actionButton.setEnabled(false);
                    actionButton.setText("Dropping...");
                    edu.univ.erp.ui.utils.UIHelper.runAsync(() -> studentAPI.dropCourse(uid, sectionId), (String msg) -> {
                        // On success, update UI (remove from taken list)
                        cs.isRegistered = false;
                        if (cs.enrolledCount > 0) cs.enrolledCount -= 1;
                        cs.dropAllowed = false;
                        courses.remove(cs);
                        tableModel.updateCourses(new ArrayList<>(courses));
                        updateStats();
                        JOptionPane.showMessageDialog(table, msg == null || msg.isEmpty() ? "Dropped successfully." : msg, "Drop", JOptionPane.INFORMATION_MESSAGE);
                    }, (Exception ex) -> {
                        // Re-enable button and show error
                        actionButton.setEnabled(true);
                        actionButton.setText("Drop");
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(table, "Failed to drop course: " + ex.getMessage(), "Drop Error", JOptionPane.ERROR_MESSAGE);
                        });
                    });
                }
                // Refresh table from master list (in case early exit)
                tableModel.updateCourses(new ArrayList<>(courses));
                updateStats();
            });

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }

        @Override
        public boolean isCellEditable(java.util.EventObject e) {
            return true;
        }
    }
}

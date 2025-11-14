package edu.univ.erp.ui.studentdashboard.forms;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.ClientContext;
import edu.univ.erp.api.student.StudentAPI;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import edu.univ.erp.util.UIHelper;
import net.miginfocom.swing.MigLayout;

/**
 * A form to display the student's weekly timetable in a grid view, similar to
 * the target design (image_2086d4.png).
 */
public class TimetableForm extends SimpleForm {

    private TimetableGridPanel gridPanel;
    private JScrollPane scrollPane;
    private FadePanel contentFadePanel; // for refresh animation like MyCoursesForm
    private JLabel titleLabel;
    private javax.swing.Timer titleFadeTimer;

    // --- Data Model Classes ---
    // NOTE: This is copied from MyCoursesForm.java for this example.
    // In a real app, this would be in a shared 'domain' package.
    private static class CourseSection {

        String courseCode;
        String courseTitle;
        int credits;
        String sectionId;
        String dayTime;
        String room;
        String instructorName;
        int enrolledCount;
        int capacity;
        boolean isRegistered;
        boolean dropAllowed = true;
        boolean isComplete;
        public CourseSection(String code, String title, int credits, String secId, String time, String room, String instr, int enrolled, int cap, boolean registered, boolean complete) {
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
            this.isComplete = complete;
        }
    }

    /**
     * A processed entry for the timetable grid, representing a single class on
     * a specific day with start/end times.
     */
    private static class TimetableEntry {

        String courseCode;
        String courseTitle;
        String room;
        String instructorName;
        LocalTime startTime;
        LocalTime endTime;
        DayOfWeek day;
        Color color;

        public TimetableEntry(String code, String title, String room, String instr, LocalTime start, LocalTime end, DayOfWeek day, Color color) {
            this.courseCode = code;
            this.courseTitle = title;
            this.room = room;
            this.instructorName = instr;
            this.startTime = start;
            this.endTime = end;
            this.day = day;
            this.color = color;
        }
    }

    // --- End Data Model Classes ---
    public TimetableForm() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[fill]", "[][grow,fill]"));

        // 1. Initialize empty entries; fetch and populate asynchronously from server
        List<TimetableEntry> timetableEntries = new ArrayList<>();
        // Will asynchronously populate the grid and animate when done
        fetchTimetableAndPopulate();

        // 2. Create Title
        add(createTitlePanel(), "wrap");

        // 3. Create Timetable Grid
    gridPanel = new TimetableGridPanel(timetableEntries);
        scrollPane = new JScrollPane(gridPanel);
        styleScrollPane(scrollPane);

        // Wrap the scroll pane in a FadePanel so Refresh triggers the same
        // subtle fade animation used in MyCoursesForm
        contentFadePanel = new FadePanel(1f);
        contentFadePanel.setOpaque(false);
        contentFadePanel.setLayout(new java.awt.BorderLayout());
        contentFadePanel.add(scrollPane, java.awt.BorderLayout.CENTER);
        add(contentFadePanel, "grow");

        // 4. Apply theme and add listener. When LookAndFeel changes we must
        // update the whole window UI tree (not only colors) so components
        // re-create any LAF-dependent decorations. This mirrors the fix used
        // in MyCoursesForm and DashboardForm: updateComponentTreeUI on the
        // top-level window and then revalidate/repaint.
        applyThemeColors();
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
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
                        if (gridPanel != null) {
                            gridPanel.applyThemeColors();
                            gridPanel.revalidate();
                            gridPanel.repaint();
                        }
                        if (contentFadePanel != null) {
                            contentFadePanel.repaint();
                        }
                        if (scrollPane != null) {
                            scrollPane.repaint();
                        }
                    });
                });
            }
        });
    }

    /**
     * Creates the main title panel for the form.
     */
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        titlePanel.setOpaque(false);

        titleLabel = new JLabel("My Timetable") {
            @Override
            protected void paintComponent(Graphics g) {
                boolean dark = FlatLaf.isLafDark();
                setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        titlePanel.add(titleLabel);
        return titlePanel;
    }

    /**
     * Applies the current FlatLaf theme colors to the form components.
     */
    private void applyThemeColors() {
        boolean dark = FlatLaf.isLafDark();
        setBackground(dark ? Color.decode("#1E1E1E") : Color.WHITE);
        if (titleLabel != null) {
            titleLabel.repaint();
        }
        if (gridPanel != null) {
            gridPanel.applyThemeColors();
        }
        if (scrollPane != null) {
            styleScrollPane(scrollPane); // Re-style scrollbars for theme
        }
    }

    /**
     * Styles the JScrollPane to match the application's theme.
     */
    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        boolean dark = FlatLaf.isLafDark();
        Color scrollbarThumb = dark ? Color.decode("#4A4A4A") : Color.decode("#C7C7CC");
        Color scrollbarTrack = dark ? Color.decode("#272727") : Color.decode("#F2F2F7");

        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI(scrollbarThumb, scrollbarTrack));
    }

    /**
     * Parses a list of CourseSection objects into a flat list of TimetableEntry
     * objects. One CourseSection with "MWF" will become three TimetableEntry
     * objects.
     */
    private List<TimetableEntry> parseCourses(List<CourseSection> courses) {
        List<TimetableEntry> entries = new ArrayList<>();
        // Regex to capture day groups (e.g., "MWF", "TTh") and times (e.g., "11:00-12:30")
        Pattern pattern = Pattern.compile("([MTWThF]+)\\s+([0-9]{1,2}:[0-9]{2})-([0-9]{1,2}:[0-9]{2})");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        // Define a color palette for the course blocks. We'll assign colors
        // to course codes via a map so the same course always gets the same
        // color. Colors are picked pseudo-randomly from the palette when a
        // course is first encountered.
        Color[] colors = new Color[]{
            Color.decode("#8B5CF6"), // Purple
            Color.decode("#3B82F6"), // Blue
            Color.decode("#EF4444"), // Red
            Color.decode("#10B981"), // Emerald
            Color.decode("#F59E0B"), // Amber
            Color.decode("#06B6D4"), // Cyan
            Color.decode("#F472B6"), // Pink
            Color.decode("#A3E635"), // Lime
            Color.decode("#60A5FA"), // Light Blue
            Color.decode("#F97316") // Orange
        };

        // Each timetable block will pick an independent color from the
        // palette so colors are not tied to course codes.
        Random rng = new Random();

        for (CourseSection course : courses) {
            Matcher matcher = pattern.matcher(course.dayTime);
            if (matcher.matches()) {
                String daysStr = matcher.group(1);
                String startStr = matcher.group(2);
                String endStr = matcher.group(3);

                try {
                    LocalTime startTime = LocalTime.parse(startStr, timeFormatter);
                    LocalTime endTime = LocalTime.parse(endStr, timeFormatter);

                    // Pick a color independently for this block (not tied to course code)
                    Color blockColor = colors[rng.nextInt(colors.length)];

                    // Handle day string parsing (e.g., "TTh")
                    for (int i = 0; i < daysStr.length(); i++) {
                        DayOfWeek day = null;
                        char c = daysStr.charAt(i);
                        if (c == 'M') {
                            day = DayOfWeek.MONDAY;
                        } else if (c == 'W') {
                            day = DayOfWeek.WEDNESDAY;
                        } else if (c == 'F') {
                            day = DayOfWeek.FRIDAY;
                        } else if (c == 'T') {
                            if (i + 1 < daysStr.length() && daysStr.charAt(i + 1) == 'h') {
                                day = DayOfWeek.THURSDAY;
                                i++; // Skip the 'h'
                            } else {
                                day = DayOfWeek.TUESDAY;
                            }
                        }

                        if (day != null) {
                            entries.add(new TimetableEntry(
                                    course.courseCode,
                                    course.courseTitle,
                                    course.room,
                                    course.instructorName,
                                    startTime,
                                    endTime,
                                    day,
                                    blockColor
                            ));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse timetable entry: " + course.dayTime);
                    e.printStackTrace();
                }
            }
        }
        return entries;
    }

    /**
     * Fetches the student's timetable from the server and updates the grid.
     * Falls back to leaving the current content if the fetch fails.
     */
    private void fetchTimetableAndPopulate() {
        try {
            UserAuth cu = ClientContext.getCurrentUser();
            if (cu == null) return;
            final int uid = cu.getUserId();
            StudentAPI studentAPI = new StudentAPI();

            UIHelper.runAsync(() -> studentAPI.getTimetable(uid), (java.util.List<CourseCatalog> timetable) -> {
                if (timetable == null) return;
                java.util.List<CourseSection> sections = new ArrayList<>();
                for (CourseCatalog cc : timetable) {
                    String secId = String.valueOf(cc.getSectionId());
                    boolean registered = true; // server timetable only returns registered rows
                    CourseSection cs = new CourseSection(
                            cc.getCourseCode() == null ? cc.getCourseTitle() : cc.getCourseCode(),
                            cc.getCourseTitle(),
                            cc.getCredits(),
                            secId,
                            cc.getDayTime(),
                            cc.getRoom(),
                            cc.getInstructorName(),
                            cc.getEnrolledCount(),
                            cc.getCapacity(),
                            registered,
                            false
                    );
                    sections.add(cs);
                }
                java.util.List<TimetableEntry> entries = parseCourses(sections);
                if (gridPanel != null) {
                    gridPanel.updateEntries(entries);
                    gridPanel.applyThemeColors();
                    gridPanel.revalidate();
                    gridPanel.repaint();
                    if (contentFadePanel != null) contentFadePanel.startFadeIn(0);
                    playTitleFade();
                }
            }, (Exception ex) -> {
                // silent fallback; keep existing content (dummy)
            });
        } catch (Exception ignore) {
            // leave dummy data
        }
    }

    // Dummy timetable data removed. Timetable now relies on server fetch only.

    @Override
    public void formRefresh() {
        // On refresh, fetch the authoritative timetable from server and
        // update the grid asynchronously. This avoids falling back to the
        // hardcoded dummy data which was used only for initial design-time view.
        fetchTimetableAndPopulate();
    }

    /**
     * Subtle fade animation for the title text to match the content fade.
     */
    private void playTitleFade() {
        if (titleLabel == null) {
            return;
        }
        // Determine base RGB color depending on theme
        boolean dark = FlatLaf.isLafDark();
        Color base = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
        final int r = base.getRed();
        final int g = base.getGreen();
        final int b = base.getBlue();

        // Stop any existing title fade
        if (titleFadeTimer != null && titleFadeTimer.isRunning()) {
            titleFadeTimer.stop();
        }
        // Start with fully transparent label so fade is visible
        titleLabel.setForeground(new Color(r, g, b, 0));
        titleFadeTimer = new javax.swing.Timer(16, null);
        final long start = System.currentTimeMillis();
        final int duration = 360; // ms
        titleFadeTimer.addActionListener(ev -> {
            long now = System.currentTimeMillis();
            float p = Math.min(1f, (now - start) / (float) duration);
            // ease-out interpolation
            float ease = 1f - (1f - p) * (1f - p);
            int alpha = Math.max(0, Math.min(255, (int) (255 * ease)));
            titleLabel.setForeground(new Color(r, g, b, alpha));
            titleLabel.repaint();
            if (p >= 1f) {
                ((javax.swing.Timer) ev.getSource()).stop();
            }
        });
        titleFadeTimer.setRepeats(true);
        titleFadeTimer.start();
    }

    /**
     * The main panel that draws the timetable grid and places the course
     * blocks. Uses a null layout to manually position course blocks.
     */
    private class TimetableGridPanel extends JPanel {

        private static final LocalTime START_TIME = LocalTime.of(8, 0);
        private static final LocalTime END_TIME = LocalTime.of(20, 0); // 8 PM
        private static final int HEADER_HEIGHT = 50;
        private static final int TIME_COL_WIDTH = 80;
        // Reduce vertical density so the timetable fits typical 768px-high frames.
        // Each 30-min block is now 30px tall (was 60px) which halves total height.
        private static final int ROW_HEIGHT_PER_30_MINS = 30; // Each 30-min block is 30px tall
        private static final DayOfWeek[] DAYS_TO_SHOW = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        private List<TimetableEntry> entries;

        // Theme-aware colors
        private Color gridColor;
        private Color headerBg;
        private Color headerFg;
        private Color timeFg;
        private Color panelBg;

        public TimetableGridPanel(List<TimetableEntry> entries) {
            this.entries = entries;

            setLayout(null); // We will position course blocks manually
            initCourseBlocks();
            applyThemeColors();

            // Recalculate block positions on resize
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    layoutCourseBlocks();
                }
            });
        }

        /**
         * Applies current theme colors to the grid.
         */
        public void applyThemeColors() {
            boolean dark = FlatLaf.isLafDark();
            panelBg = dark ? Color.decode("#1E1E1E") : Color.WHITE;
            gridColor = dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB");
            headerBg = dark ? Color.decode("#272727") : Color.decode("#F9FAFB");
            headerFg = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
            timeFg = dark ? Color.decode("#999999") : Color.decode("#5C5C5C");
            setBackground(panelBg);

            // Update colors of all child CourseBlockPanel components
            for (Component comp : getComponents()) {
                if (comp instanceof CourseBlockPanel) {
                    ((CourseBlockPanel) comp).applyThemeColors();
                }
            }
            repaint();
        }

        /**
         * Creates and adds all CourseBlockPanel components to this panel.
         */
        private void initCourseBlocks() {
            removeAll(); // Clear old blocks if any
            for (TimetableEntry entry : entries) {
                add(new CourseBlockPanel(entry));
            }
            layoutCourseBlocks(); // Set initial positions
        }

        /**
         * Replace the current entries and rebuild the block components.
         */
        public void updateEntries(List<TimetableEntry> newEntries) {
            this.entries = newEntries;
            initCourseBlocks();
        }

        /**
         * Calculates and sets the bounds (x, y, width, height) of all
         * CourseBlockPanel components based on the panel's current size.
         */
        private void layoutCourseBlocks() {
            if (getWidth() == 0) {
                return; // Not visible yet
            }
            double colWidth = (double) (getWidth() - TIME_COL_WIDTH) / DAYS_TO_SHOW.length;

            for (Component comp : getComponents()) {
                if (comp instanceof CourseBlockPanel) {
                    CourseBlockPanel block = (CourseBlockPanel) comp;
                    TimetableEntry entry = block.getEntry();

                    int dayIndex = -1;
                    for (int i = 0; i < DAYS_TO_SHOW.length; i++) {
                        if (DAYS_TO_SHOW[i] == entry.day) {
                            dayIndex = i;
                            break;
                        }
                    }
                    if (dayIndex == -1) {
                        continue; // Not a day we are showing
                    }
                    double x = TIME_COL_WIDTH + (dayIndex * colWidth);
                    double y = timeToY(entry.startTime);
                    double height = timeToY(entry.endTime) - y;

                    // Use the exact time-slot height so the block fits the timetable grid.
                    // (Do not expand to preferred height -- this keeps blocks aligned with time.)

                    // Apply a small margin
                    block.setBounds((int) x + 2, (int) y + 2, (int) colWidth - 4, (int) height - 4);
                }
            }
        }

        /**
         * Helper method to convert a LocalTime to a Y-coordinate on the panel.
         */
        private double timeToY(LocalTime time) {
            double minutesFromStart = START_TIME.until(time, ChronoUnit.MINUTES);
            // Calculate Y based on 30-minute row height
            double y = HEADER_HEIGHT + (minutesFromStart / 30.0) * ROW_HEIGHT_PER_30_MINS;
            return y;
        }

        /**
         * Overridden to draw the grid lines, time labels, and day headers.
         */
        @Override
        protected void paintComponent(Graphics g) {
            // Compute theme-aware colors here so painting always reflects
            // the current LookAndFeel even if applyThemeColors hasn't run.
            boolean dark = FlatLaf.isLafDark();
            Color localPanelBg = dark ? Color.decode("#1E1E1E") : Color.WHITE;
            Color localGridColor = dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB");
            Color localHeaderBg = dark ? Color.decode("#272727") : Color.decode("#F9FAFB");
            Color localHeaderFg = dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E");
            Color localTimeFg = dark ? Color.decode("#999999") : Color.decode("#5C5C5C");

            // Ensure background matches theme
            if (!getBackground().equals(localPanelBg)) {
                setBackground(localPanelBg);
            }

            super.paintComponent(g); // fills background with localPanelBg
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            long totalMinutes = START_TIME.until(END_TIME, ChronoUnit.MINUTES);
            int totalRows = (int) (totalMinutes / 30);
            int totalHeight = HEADER_HEIGHT + (totalRows * ROW_HEIGHT_PER_30_MINS);

            double colWidth = (double) (width - TIME_COL_WIDTH) / DAYS_TO_SHOW.length;

            // --- Draw Day Headers ---
            g2.setColor(localHeaderBg);
            g2.fillRect(0, 0, width, HEADER_HEIGHT);
            g2.setColor(localHeaderFg);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            // Draw the time header in the left column (centered like day headers)
            String timeLabel = "Time";
            int timeLabelWidth = g2.getFontMetrics().stringWidth(timeLabel);
            // Center vertically to match day header baseline
            g2.drawString(timeLabel, (TIME_COL_WIDTH - timeLabelWidth) / 2, HEADER_HEIGHT / 2 + 5);
            for (int i = 0; i < DAYS_TO_SHOW.length; i++) {
                String dayName = DAYS_TO_SHOW[i].toString(); // e.g., MONDAY
                dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase(); // e.g., Monday
                int x = (int) (TIME_COL_WIDTH + (i * colWidth));
                // Center text in the column header
                int textWidth = g2.getFontMetrics().stringWidth(dayName);
                g2.drawString(dayName, x + ((int) colWidth - textWidth) / 2, HEADER_HEIGHT / 2 + 5);
            }

            // --- Draw Time Labels and Horizontal Grid Lines ---
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            LocalTime currentTime = START_TIME;
            for (int i = 0; i <= totalRows; i++) {
                int y = HEADER_HEIGHT + (i * ROW_HEIGHT_PER_30_MINS);

                // Draw horizontal line
                g2.setColor(localGridColor);
                g2.drawLine(TIME_COL_WIDTH, y, width, y);

                // Draw time label every hour (on the hour)
                if (currentTime.getMinute() == 0) {
                    g2.setColor(localTimeFg);
                    String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                    int strWidth = g2.getFontMetrics().stringWidth(timeStr);
                    g2.drawString(timeStr, (TIME_COL_WIDTH - strWidth) / 2, y + 15); // Draw just below the line
                }

                currentTime = currentTime.plusMinutes(30);
            }

            // --- Draw Vertical Grid Lines ---
            g2.setColor(localGridColor);
            for (int i = 0; i <= DAYS_TO_SHOW.length; i++) {
                int x = (int) (TIME_COL_WIDTH + (i * colWidth));
                g2.drawLine(x, HEADER_HEIGHT, x, totalHeight);
            }

            // Draw vertical line for time column
            g2.drawLine(TIME_COL_WIDTH, 0, TIME_COL_WIDTH, totalHeight);
            // Draw horizontal line under header
            g2.drawLine(0, HEADER_HEIGHT, width, HEADER_HEIGHT);

            g2.dispose();
        }

        /**
         * Ensures the panel is tall enough to display the full timetable.
         */
        @Override
        public Dimension getPreferredSize() {
            long totalMinutes = START_TIME.until(END_TIME, ChronoUnit.MINUTES);
            int totalRows = (int) (totalMinutes / 30);
            int height = HEADER_HEIGHT + (totalRows * ROW_HEIGHT_PER_30_MINS);
            // Width of 800 is a sensible default, height is calculated
            return new Dimension(800, height);
        }
    }

    /**
     * A custom panel to display a single course block on the timetable. It
     * paints its own colored, rounded-rectangle background.
     */
    private class CourseBlockPanel extends JPanel {

        private TimetableEntry entry;
        private Color blockBg;
        private Color blockFg;

        public CourseBlockPanel(TimetableEntry entry) {
            this.entry = entry;
            // Use MigLayout to position labels inside the block
            setLayout(new MigLayout("fill, insets 8 10 8 10", "[fill]", "[]5[]push[]"));
            setOpaque(false); // We will paint our own rounded rect

            // Use a wrapping JTextArea for the title so long course titles
            // will wrap to multiple lines and remain visible inside the block.
            JTextArea titleArea = new JTextArea(entry.courseTitle);
            titleArea.setLineWrap(true);
            titleArea.setWrapStyleWord(true);
            titleArea.setOpaque(false);
            titleArea.setEditable(false);
            titleArea.setFocusable(false);
            titleArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            titleArea.setBorder(null);

            JLabel codeLabel = new JLabel(entry.courseCode);
            codeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            JLabel profLabel = new JLabel(entry.instructorName);
            profLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

            JLabel roomLabel = new JLabel(entry.room);
            roomLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

            add(titleArea, "wrap, gaptop 2");
            add(codeLabel, "wrap");
            add(profLabel, "wrap");
            add(roomLabel, "wrap"); // 'push' moves this to the bottom

            // Tooltip showing exact timings and the requested format.
            try {
                java.time.format.DateTimeFormatter tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                String times = entry.startTime.format(tf) + " - " + entry.endTime.format(tf);
                // Use HTML for line breaks and simple styling.
                String tip = "<html><b>" + times + "</b><br/>" +
                        escapeHtml(entry.courseTitle) + " - " + escapeHtml(entry.courseCode) + "<br/>" +
                        escapeHtml(entry.instructorName) + " - " + escapeHtml(entry.room) + "</html>";
                setToolTipText(tip);
            } catch (Throwable ignore) {
            }

            // avoid calling overridable methods from ctor; colors are
            // applied dynamically in paintComponent so an explicit
            // initial call isn't required here.
        }

        private String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        }

        public TimetableEntry getEntry() {
            return entry;
        }

        /**
         * Applies theme colors to the block. The background is the course
         * color, text is white.
         */
        public void applyThemeColors() {
            // The design from the image uses solid, bright colors
            blockBg = entry.color;
            blockFg = Color.WHITE; // Text is always white on colored blocks

            // Set text color for all labels
            for (Component c : getComponents()) {
                c.setForeground(blockFg);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Recompute theme-dependent colors each paint so LAF switches
            // are reflected immediately (labels/colors updated on repaint).
            blockBg = entry.color;
            blockFg = Color.WHITE;

            // Ensure child labels use the correct foreground and are non-opaque
            for (Component c : getComponents()) {
                c.setForeground(blockFg);
                if (c instanceof javax.swing.JComponent) {
                    ((javax.swing.JComponent) c).setOpaque(false);
                }
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fill background with the course color
            g2.setColor(blockBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            // Draw a slightly darker border
            try {
                g2.setColor(blockBg.darker());
            } catch (Exception ex) {
                g2.setColor(blockBg);
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.dispose();

            super.paintComponent(g); // Paint labels on top of our custom background
        }
    }

    /**
     * A custom ScrollBar UI. Copied from MyCoursesForm.
     */
    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

        private final Color thumb;
        private final Color track;

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
     * Generic fading container (replicates MyCoursesForm's FadePanel)
     */
    private static class FadePanel extends javax.swing.JPanel {

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
        public void paint(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }
    }
}
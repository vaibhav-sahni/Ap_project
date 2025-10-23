package edu.univ.erp.ui.studentdashboard.forms;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Window;

import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import com.formdev.flatlaf.FlatLaf;
// theme classes imported where needed elsewhere; remove unused imports here
import net.miginfocom.swing.MigLayout;

public class DashboardForm extends SimpleForm {

    private GlassCardPanel cgpaCard;
    private GlassCardPanel creditsCard;
    private GlassCardPanel coursesCard;
    private JPanel notificationListPanel;
    private InteractiveCalendarPanel calendarPanel;
    private Timer resizeSyncTimer; // debounce timer for resize sync
    private boolean windowListenersInstalled = false; // ensure we install once
    private Timer stateTransitionTimer; // fade overlay timer during window state changes
    private float stateTransitionAlpha = 0f; // 0..1 overlay alpha for transition

    // UI theme helpers (match template behavior by using UI defaults)
    private Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private Color panelBg() {
        return uiColor("Panel.background", getBackground());
    }

    private Color textColor() {
        boolean isDark = FlatLaf.isLafDark();
        return isDark ? new Color(234, 234, 234) : new Color(30, 30, 30);
    }

    private Color secondaryTextColor() {
        boolean isDark = FlatLaf.isLafDark();
        return isDark ? new Color(153, 153, 153) : new Color(100, 100, 100);
    }

    private Color accentColor() {
        Color c = UIManager.getColor("Component.accentColor");
        if (c == null) {
            c = UIManager.getColor("Button.startBackground");
        }
        if (c == null) {
            c = UIManager.getColor("Component.focusColor");
        }
        return c != null ? c : Color.decode("#5856D6");
    }

    // helper color methods were removed to keep the class focused; calendar-specific helpers remain

    private Color todayColor() {
        return Color.decode("#30CC72"); // Green in both modes
    }

    private Color deadlineColor() {
        return Color.decode("#EF4444");
    }

    // Notification class to hold notification data
    private static class Notification {

        LocalDateTime timestamp;
        String title;
        String message;

        public Notification(LocalDateTime timestamp, String title, String message) {
            this.timestamp = timestamp;
            this.title = title;
            this.message = message;
        }
    }

    // Dummy notification data
    private List<Notification> getDummyNotifications() {
        List<Notification> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Today notifications
        notifications.add(new Notification(now.minusMinutes(30), "Prelim payment due", "Lorem ipsum dolor sit amet, consectetur adipiscing elit."));
        notifications.add(new Notification(now.minusHours(2), "Exam schedule", "Norem ipsum dolor sit amet, consectetur adipiscing elit. Nunc vulputate libero et velit."));
        notifications.add(new Notification(now.minusHours(5), "Assignment reminder", "Your CS101 assignment is due in 2 days."));

        // Yesterday notifications
        notifications.add(new Notification(now.minusDays(1).minusHours(3), "Library book reminder", "Your borrowed book 'Java Fundamentals' is due tomorrow."));
        notifications.add(new Notification(now.minusDays(1).minusHours(8), "Grade posted", "Your grade for Database Systems midterm has been posted."));

        return notifications;
    }

    public DashboardForm() {
        init();
    }

    /**
     * DashboardForm initializer. SimpleForm defines a private init() which is not
     * visible to subclasses, so provide a local init() to perform component
     * initialization and kick off the usual refresh.
     */
    private void init() {
        // keep panel transparent by default and let formRefresh set layout/content
        setOpaque(false);
        // Ensure any subclass-specific initialization runs via formRefresh
        formRefresh();
    }

    @Override
    public void formRefresh() {
        // Update background color
        setBackground(panelBg());

        // Refresh calendar to update text colors and reset to today
        if (calendarPanel != null) {
            calendarPanel.resetToToday();
            calendarPanel.updateCalendar();
        }

        // Refresh notifications to update text colors
        refreshNotificationList();

        // Force repaint of all components to pick up new theme colors
        revalidate();
        repaint();
        // Use theme-aware background
        setBackground(panelBg());
        setLayout(new MigLayout("fill,insets 20", "[fill,grow]20[340!]", "[]30[]20[]20[grow,fill]"));

        String username = "";
        try {
            edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
            if (cu != null) username = cu.getUsername();
        } catch (Throwable ignore) {}

        JLabel welcomeLabel = new JLabel("Welcome back" + (username.isEmpty() ? ", Student!" : ", " + username + "!")) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        welcomeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));

        // Wrap welcome label in fade panel for smooth appearance
        FadePanel welcomeWrapper = new FadePanel(0f);
        welcomeWrapper.setOpaque(false);
        welcomeWrapper.setLayout(new BorderLayout());
        welcomeWrapper.add(welcomeLabel, BorderLayout.CENTER);
        add(welcomeWrapper, "span,wrap");

        // Start fade after layout
        SwingUtilities.invokeLater(() -> welcomeWrapper.startFadeIn(0));

    // Create placeholder cards; actual values will be filled by an async fetch
    cgpaCard = createGaugeCard("CGPA", 0.0, 10, new Color(48, 204, 114));
    creditsCard = createGaugeCard("Credits", 0, 120, new Color(52, 152, 219));
    coursesCard = createGaugeCard("My Courses", 0, 20, new Color(241, 196, 15));

        // Create and store calendar reference
        calendarPanel = (InteractiveCalendarPanel) createCalendarPanel();

        // Top row: 3 gauge cards on left, calendar on right (spans 2 rows)
        add(cgpaCard, "split 3,sg card,height 200!");
        add(creditsCard, "sg card,height 200!");
        add(coursesCard, "sg card,height 200!");
        add(calendarPanel, "spany 2,grow,wrap");

        // Second row: notifications panel below the charts
        add(createNotificationsPanel(), "grow,height 280!");

        // Refresh the entire window when Look & Feel (mode) changes
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        SwingUtilities.updateComponentTreeUI(w);
                        w.invalidate();
                        w.validate();
                        w.repaint();
                    } else {
                        SwingUtilities.updateComponentTreeUI(this);
                        revalidate();
                        repaint();
                    }
                    // Also refresh dashboard-specific dynamic content
                    formRefresh();
                });
            }
        });

        // One-time refresh and start animations when dashboard is first shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            private boolean done = false;

            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (done) {
                    return;
                }
                done = true;
                SwingUtilities.invokeLater(() -> {
                    // Attach listeners to the host window to catch maximize/restore transitions
                    installWindowListeners();
                    refreshNotificationList();
                    if (calendarPanel != null) {
                        calendarPanel.updateCalendar();
                    }
                    if (cgpaCard != null) {
                        cgpaCard.startAnimation();
                        // fetch fresh CGPA & courses asynchronously
                        fetchAndPopulateStudentMetrics();
                    }
                    if (creditsCard != null) {
                        creditsCard.startAnimation();
                    }
                    if (coursesCard != null) {
                        coursesCard.startAnimation();
                    }
                    revalidate();
                    repaint();
                });
            }
        });

        // Auto-refresh when theme (Look & Feel) changes
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    formRefresh();
                });
            }
        });

        // Debounced relayout when the dashboard is resized (e.g., maximize/restore)
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                scheduleResizeSync();
            }
        });
    }

    // Fetch CGPA, credits and course count from the server and update UI
    private void fetchAndPopulateStudentMetrics() {
        edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
        if (u == null) return;
        edu.univ.erp.ui.utils.UIHelper.runAsync(() -> {
            edu.univ.erp.ui.actions.StudentActions actions = new edu.univ.erp.ui.actions.StudentActions();
            edu.univ.erp.api.student.StudentAPI.CgpaResponse resp = null;
            int courseCount = 0;
            try {
                resp = actions.getCgpa(u.getUserId());
            } catch (Exception ex) {
                throw ex;
            }
            try {
                java.util.List<edu.univ.erp.domain.CourseCatalog> list = actions.getCourseCatalog();
                if (list != null) courseCount = list.size();
            } catch (Exception ex) {
                // propagate the error so MessagePresenter can show it
                throw ex;
            }
            // Pack results into a small array: [cgpa, credits, courseCount]
            Double cg = resp == null ? null : resp.cgpa;
            Double cr = resp == null ? null : resp.totalCreditsEarned;
            return new Object[]{cg, cr, Integer.valueOf(courseCount)};
        }, (Object result) -> {
            try {
                Object[] arr = (Object[]) result;
                Double cg = (Double) arr[0];
                Double cr = (Double) arr[1];
                Integer cc = (Integer) arr[2];
                if (cg != null || cr != null || (cc != null && cc > 0)) {
                    double cgVal = cg != null ? cg : 0.0;
                    double crVal = cr != null ? cr : 0.0;
                    int ccVal = cc != null ? cc.intValue() : 0;
                    try {
                        if (cgpaCard != null) cgpaCard.updateGauge(cgVal, 10);
                        if (creditsCard != null) creditsCard.updateGauge(crVal, 120);
                        if (coursesCard != null) coursesCard.updateGauge(ccVal, 20);
                    } catch (Throwable t) {
                        edu.univ.erp.ui.utils.MessagePresenter.showError(this, "Failed to update dashboard UI: " + t.getMessage());
                    }
                }
            } catch (Throwable t) {
                edu.univ.erp.ui.utils.MessagePresenter.showError(this, t.getMessage());
            }
        }, (Exception ex) -> {
            edu.univ.erp.ui.utils.MessagePresenter.showError(this, ex.getMessage());
        });
    }

    // Subtle fade overlay when window is transitioning (maximize/restore)
    private void startWindowTransition() {
        // Start from a small overlay and fade out quickly
        stateTransitionAlpha = 0.22f; // subtle
        if (stateTransitionTimer != null && stateTransitionTimer.isRunning()) {
            stateTransitionTimer.stop();
        }
        stateTransitionTimer = new Timer(16, ev -> {
            // ease-out fade
            stateTransitionAlpha *= 0.82f;
            if (stateTransitionAlpha < 0.02f) {
                stateTransitionAlpha = 0f;
                stateTransitionTimer.stop();
            }
            repaint();
        });
        stateTransitionTimer.start();
    }

    // Install window-level listeners once to reliably handle maximize/restore
    private void installWindowListeners() {
        if (windowListenersInstalled) {
            return;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null) {
            return; // not yet attached
        }
        windowListenersInstalled = true;

        // Listen to window size changes (covers manual resize and OS-driven toggles)
        w.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                scheduleResizeSync();
            }
        });

        // Listen explicitly for state changes like MAXIMIZED_BOTH <-> NORMAL
        if (w instanceof java.awt.Frame) {
            ((java.awt.Frame) w).addWindowStateListener(evt -> {
                // Just debounce sync; avoid any size nudge to prevent accidental re-maximize
                scheduleResizeSync();
                // Kick a quick fade overlay to make the transition feel smoother
                startWindowTransition();
            });
        }
    }

    // Schedules a relayout after a short delay to avoid repeated heavy work while resizing
    private void scheduleResizeSync() {
        if (resizeSyncTimer != null && resizeSyncTimer.isRunning()) {
            resizeSyncTimer.restart();
            return;
        }
        resizeSyncTimer = new Timer(120, ev -> {
            performResizeSync();
            resizeSyncTimer.stop();
        });
        resizeSyncTimer.setRepeats(false);
        resizeSyncTimer.start();
    }

    // Performs the sync: update dynamic sections and force a validate/repaint
    private void performResizeSync() {
        if (calendarPanel != null) {
            calendarPanel.updateCalendar();
        }
        refreshNotificationList();
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.doLayout();
            w.validate();
            w.repaint();
        } else {
            revalidate();
            repaint();
        }
    }

    // Nudge layout system to recompute sizes fully after maximize/restore
    private void forceRelayout() {
        // forceRelayout removed - layout nudges handled in scheduleResizeSync/performResizeSync
    }

    // Draw a transient overlay after children to create a smooth transition feel
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (stateTransitionAlpha > 0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Lighten in dark mode; darken in light mode
            boolean isDark = FlatLaf.isLafDark();
            Color overlay = isDark ? Color.WHITE : Color.BLACK;
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.min(0.35f, stateTransitionAlpha)));
            g2.setColor(overlay);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private JPanel createNoticeItem(Notification notif) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));

        // Title and time in one line
        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BorderLayout(10, 0));

        JLabel titleLabel = new JLabel(notif.title) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        JLabel timeLabel = new JLabel(getTimeAgo(notif.timestamp)) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(timeLabel, BorderLayout.EAST);

        // Message
        JTextArea messageLabel = new JTextArea(notif.message);
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);
        messageLabel.setOpaque(false);
        messageLabel.setEditable(false);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageLabel.setForeground(secondaryTextColor());
        messageLabel.setBorder(null);
        messageLabel.setFocusable(false);

        item.add(titleRow);
        item.add(Box.createVerticalStrut(5));
        item.add(messageLabel);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        return item;
    }

    private String getTimeAgo(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) {
            return "now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    private JPanel createCalendarPanel() {
        return new InteractiveCalendarPanel();
    }

    // Reintroduced helper to create a gauge card matching earlier usage.
    // Keeps behavior consistent with template by delegating to GlassCardPanel.
    private GlassCardPanel createGaugeCard(String title, double value, double maxValue, Color color) {
        return new GlassCardPanel(title, value, maxValue, color);
    }

    private JPanel createNotificationsPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));

        // Header row with title and "Show all" link
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel header = new JLabel("Notifications") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        JLabel showAllLink = new JLabel("Show all") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        showAllLink.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        showAllLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showAllLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // TODO: Navigate to full notifications page
                JOptionPane.showMessageDialog(DashboardForm.this, "Show all notifications feature coming soon!");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                showAllLink.setForeground(accentColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showAllLink.repaint(); // Let paintComponent restore the color
            }
        });

        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(showAllLink, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        notificationListPanel = new JPanel();
        notificationListPanel.setOpaque(false);
        notificationListPanel.setLayout(new BoxLayout(notificationListPanel, BoxLayout.Y_AXIS));
        card.add(notificationListPanel, BorderLayout.CENTER);
        // Populate after adding to avoid first-render emptiness
        SwingUtilities.invokeLater(this::refreshNotificationList);
        return card;
    }

    private void refreshNotificationList() {
        if (notificationListPanel == null) {
            return;
        }

        notificationListPanel.removeAll();
        List<Notification> all = getDummyNotifications();
        int count = Math.min(3, all.size());
        for (int i = 0; i < count; i++) {
            Notification n = all.get(i);
            JPanel item = createNoticeItem(n);
            // Wrap in fade panel for smooth appearance; stagger slightly
            FadePanel wrapper = new FadePanel(0f);
            wrapper.setOpaque(false);
            wrapper.setLayout(new BorderLayout());
            wrapper.add(item, BorderLayout.CENTER);
            notificationListPanel.add(wrapper);
            final int delay = i * 60; // stagger by 60ms each
            SwingUtilities.invokeLater(() -> wrapper.startFadeIn(delay));
            if (i < count - 1) {
                notificationListPanel.add(Box.createVerticalStrut(12));
            }
        }
        notificationListPanel.revalidate();
        notificationListPanel.repaint();
    }

    // Custom Components as Inner Classes
    // Generic fading container used to reveal refreshed content smoothly
    private class FadePanel extends JPanel {

        private float alpha;
        private Timer fadeTimer;

        public FadePanel(float initialAlpha) {
            this.alpha = initialAlpha;
            setOpaque(false);
        }

        public void startFadeIn(int delayMs) {
            Runnable starter = () -> {
                if (fadeTimer != null && fadeTimer.isRunning()) {
                    fadeTimer.stop();
                }
                fadeTimer = new Timer(16, ev -> {
                    alpha += (1f - alpha) * 0.18f; // ease-out
                    if (1f - alpha < 0.02f) {
                        alpha = 1f;
                        fadeTimer.stop();
                    }
                    repaint();
                });
                fadeTimer.start();
            };
            if (delayMs > 0) {
                Timer d = new Timer(delayMs, e -> {
                    ((Timer) e.getSource()).stop();
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

    private class CardPanel extends JPanel {

        public CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Use theme-aware colors
            boolean isDark = FlatLaf.isLafDark();
            Color cardBg = isDark ? new Color(39, 39, 39) : new Color(255, 255, 255);
            Color cardBorder = isDark ? new Color(255, 255, 255, 25) : new Color(0, 0, 0, 15);

            // Soft, subtle shadow to lift cards slightly from the background
            int arc = 20;
            int shadowLayers = 6;
            for (int i = shadowLayers; i >= 1; i--) {
                float alpha = (isDark ? 8 : 10) / 255f; // lighter in dark mode
                int blur = i * 2;
                Color shadow = new Color(0, 0, 0, Math.round(alpha * 255));
                g2.setColor(shadow);
                g2.fill(new RoundRectangle2D.Float(0 + blur, 2 + blur, getWidth() - blur * 2, getHeight() - blur * 2, arc, arc));
            }

            g2.setColor(cardBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));

            g2.setColor(cardBorder);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class GlassCardPanel extends JPanel {

        private final AnimatedGaugeChart gauge;
    // hover state not stored separately; hoverProgress drives visuals
        private float hoverProgress = 0f; // 0..1
        private float hoverTarget = 0f;
        private Timer hoverTimer;

        public GlassCardPanel(String title, double value, double maxValue, Color color) {
            this.gauge = new AnimatedGaugeChart(title, value, maxValue, color);
            setOpaque(false);
            setLayout(new BorderLayout());
            add(gauge, BorderLayout.CENTER);

            // Start animation when component becomes visible
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    startAnimation();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverTo(1f);
                    startAnimation();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverTo(0f);
                }
            });
        }

        private void hoverTo(float target) {
            hoverTarget = target;
            if (hoverTimer != null && hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            hoverTimer = new Timer(16, ev -> {
                // smooth approach easing
                hoverProgress += (hoverTarget - hoverProgress) * 0.18f;
                if (Math.abs(hoverTarget - hoverProgress) < 0.01f) {
                    hoverProgress = hoverTarget;
                    hoverTimer.stop();
                }
                repaint();
            });
            hoverTimer.start();
        }

        public void startAnimation() {
            gauge.startAnimation();
        }

        // stopAnimation removed (unused)

        /** Update the gauge displayed in this card (double value). */
        public void updateGauge(double value, double max) {
            try {
                gauge.updateValue(value, max);
            } catch (Throwable t) {
                System.err.println("CLIENT WARN: updateGauge(double) failed: " + t.getMessage());
            }
        }

        /** Update the gauge displayed in this card (int value). */
        public void updateGauge(int value, int max) {
            updateGauge((double) value, (double) max);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Use theme-aware colors
            boolean isDark = FlatLaf.isLafDark();
            Color cardBg = isDark ? new Color(39, 39, 39) : new Color(255, 255, 255);
            Color cardBorder = isDark ? new Color(255, 255, 255, 25) : new Color(0, 0, 0, 15);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(cardBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));

            g2.setColor(cardBorder);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 20, 20));

            // Hover overlay (subtle lightening) - no scale, just color overlay
            if (hoverProgress > 0f) {
                int alpha = (int) (hoverProgress * (isDark ? 50 : 30));
                Color hoverOverlay = isDark ? new Color(255, 255, 255, alpha) : new Color(0, 0, 0, alpha);
                g2.setColor(hoverOverlay);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class AnimatedGaugeChart extends JComponent {

        private final String title;
        private double value;
        private double maxValue;
        private final Color color;
        private float animatedValue = 0f;
        private Timer timer;

        public AnimatedGaugeChart(String title, double value, double maxValue, Color color) {
            this.title = title;
            this.value = value;
            this.maxValue = maxValue;
            this.color = color;
            setOpaque(false);
            // start from 0 to animate on load
            this.animatedValue = 0f;
        }

        /**
         * Update the target value and optionally max value, then start animation towards it.
         */
        public synchronized void updateValue(double newValue, double newMax) {
            this.value = newValue;
            this.maxValue = newMax;
            // restart animation towards the new target
            startAnimation();
        }

        public void startAnimation() {
            float target = (float) value;
            // Reset to 0 to restart animation (for refresh functionality)
            animatedValue = 0f;

            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new Timer(15, null);
            timer.addActionListener(evt -> {
                float diff = target - animatedValue;
                animatedValue += diff * 0.12f; // easing
                if (Math.abs(diff) < 0.5f) {
                    animatedValue = target;
                    timer.stop();
                }
                repaint();
            });
            timer.start();
        }

        // stopAnimation removed (unused) - animation is controlled via startAnimation/updateValue

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply fade-out on the entire content during animation start
            float contentAlpha = Math.min(1f, Math.max(0f, animatedValue / (float) value));
            if (contentAlpha < 1f) {
                g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.3f + 0.7f * contentAlpha));
            }

            int size = Math.min(getWidth(), getHeight()) - 30;
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // make stroke ~25% thinner than before; keep a reasonable minimum
            float stroke = Math.max(6f, size * 0.09f);
            float radius = size / 2f - stroke / 2f - 5f;

            // Draw track arc (background)
            g2.setStroke(new java.awt.BasicStroke(stroke, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            boolean isDark = FlatLaf.isLafDark();
            Color trackColor = isDark ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 20);
            g2.setColor(trackColor);
            Arc2D track = new Arc2D.Double(cx - radius, cy - radius, radius * 2, radius * 2, 90, -360, Arc2D.OPEN);
            g2.draw(track);

            // Draw animated arc in solid accent color with a subtle glow shadow
            float startAngle = 90f;
            float extent = (float) -((animatedValue / (float) maxValue) * 360f);
            java.awt.geom.Arc2D arc = new java.awt.geom.Arc2D.Double(cx - radius, cy - radius, radius * 2, radius * 2, startAngle, extent, Arc2D.OPEN);
            // shadow (glow)
            g2.setStroke(new java.awt.BasicStroke(stroke + 4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
            g2.setColor(glow);
            g2.draw(arc);
            // main arc
            g2.setStroke(new java.awt.BasicStroke(stroke, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            g2.draw(arc);

            // Inner circle (fill) to create gauge look
            float innerRadius = radius - stroke / 2f - 6f;
            Ellipse2D inner = new Ellipse2D.Float(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);
            Color innerBg = isDark ? new Color(39, 39, 39) : new Color(255, 255, 255);
            g2.setColor(innerBg);
            g2.fill(inner);

            // subtle inner ring
            Color innerRing = isDark ? new Color(255, 255, 255, 18) : new Color(0, 0, 0, 12);
            g2.setColor(innerRing);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Float(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2));

            // Draw title and value centered
            Color textColor = isDark ? new Color(234, 234, 234) : new Color(30, 30, 30);
            g2.setColor(textColor);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, cx - fm.stringWidth(title) / 2, cy - 8);

            // Draw value with larger font and "/max" suffix on same baseline
            g2.setColor(textColor);
            String valueText = (value % 1 == 0) ? String.valueOf((int) value) : String.format("%.1f", value);
            String maxText = "/" + ((maxValue % 1 == 0) ? String.valueOf((int) maxValue) : String.format("%.1f", maxValue));

            // Measure both parts
            Font valueFont = new Font(Font.SANS_SERIF, Font.BOLD, 22);
            Font maxFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

            g2.setFont(valueFont);
            FontMetrics valueFm = g2.getFontMetrics();
            int valueWidth = valueFm.stringWidth(valueText);

            g2.setFont(maxFont);
            FontMetrics maxFm = g2.getFontMetrics();
            int maxWidth = maxFm.stringWidth(maxText);

            int spacing = 4; // space between value and "/max"

            // Calculate total width and starting x position to center both
            int totalWidth = valueWidth + spacing + maxWidth;
            int startX = cx - totalWidth / 2;
            int baselineY = cy + 22;

            // Draw value in large font
            g2.setFont(valueFont);
            g2.drawString(valueText, startX, baselineY);

            // Draw "/max" in smaller font on same baseline (greyish color)
            g2.setFont(maxFont);
            Color secondaryText = isDark ? new Color(153, 153, 153) : new Color(120, 120, 120);
            g2.setColor(secondaryText);
            g2.drawString(maxText, startX + valueWidth + spacing, baselineY);

            g2.dispose();
        }
    }

    // Course Deadline class
    private static class CourseDeadline {

        LocalDate date;
        String courseName;

        CourseDeadline(LocalDate date, String courseName) {
            this.date = date;
            this.courseName = courseName;
        }
    }

    private class InteractiveCalendarPanel extends CardPanel {

        private LocalDate currentDate;
        private final List<CourseDeadline> courseDeadlines;
        private int viewMode = 0; // 0=days, 1=months, 2=years
        private float animationProgress = 1.0f;
        private Timer animationTimer;

        // Reset calendar to today's date and day view
        public void resetToToday() {
            currentDate = LocalDate.now();
            viewMode = 0; // Reset to day view
        }

        // Theme-aware colors for calendar
        private Color getCalButtonBg() {
            return FlatLaf.isLafDark() ? new Color(42, 42, 42) : new Color(245, 245, 245);
        }

        private Color getCalButtonFg() {
            return FlatLaf.isLafDark() ? new Color(234, 234, 234) : new Color(30, 30, 30);
        }

        private Color getCalButtonBorder() {
            return FlatLaf.isLafDark() ? new Color(74, 74, 74) : new Color(200, 200, 200);
        }

        private Color getCalDayHeaderFg() {
            return FlatLaf.isLafDark() ? new Color(153, 153, 153) : new Color(120, 120, 120);
        }

        public InteractiveCalendarPanel() {
            this.currentDate = LocalDate.now();
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));

            // Initialize course deadlines with exact dates (DD-MM-YYYY)
            courseDeadlines = new ArrayList<>();
            courseDeadlines.add(new CourseDeadline(LocalDate.of(2025, 10, 18), "Advanced Algorithms"));
            courseDeadlines.add(new CourseDeadline(LocalDate.of(2025, 10, 25), "Database Systems"));
            courseDeadlines.add(new CourseDeadline(LocalDate.of(2025, 11, 5), "Machine Learning"));
            courseDeadlines.add(new CourseDeadline(LocalDate.of(2025, 11, 15), "Operating Systems"));
            courseDeadlines.add(new CourseDeadline(LocalDate.of(2025, 12, 1), "Computer Networks"));

            updateCalendar();
        }

        private void updateCalendar() {
            removeAll();
            add(createHeader(), BorderLayout.NORTH);

            // Build mode-specific center, then fade it in for a smooth refresh
            JPanel centerPanel;
            if (viewMode == 0) {
                centerPanel = createDayView();
            } else if (viewMode == 1) {
                centerPanel = createMonthView();
            } else {
                centerPanel = createYearView();
            }

            FadePanel fadeCenter = new FadePanel(0f);
            fadeCenter.setOpaque(false);
            fadeCenter.setLayout(new BorderLayout());
            fadeCenter.add(centerPanel, BorderLayout.CENTER);
            add(fadeCenter, BorderLayout.CENTER);

            add(createFooter(), BorderLayout.SOUTH);

            revalidate();
            repaint();
            // Start fade after layout to avoid size jumps
            SwingUtilities.invokeLater(() -> fadeCenter.startFadeIn(0));
        }

        private void animateViewChange(int newMode) {
            if (animationTimer != null && animationTimer.isRunning()) {
                return;
            }

            viewMode = newMode;
            animationProgress = 0.0f;

            animationTimer = new Timer(10, new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    animationProgress += 0.08f;
                    if (animationProgress >= 1.0f) {
                        animationProgress = 1.0f;
                        ((Timer) e.getSource()).stop();
                    }
                    updateCalendar();
                }
            });
            animationTimer.start();
        }

        private JPanel createHeader() {
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            // Month/Year label that can be clicked
            String headerText;
            if (viewMode == 0) {
                headerText = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            } else if (viewMode == 1) {
                headerText = String.valueOf(currentDate.getYear());
            } else {
                int decade = (currentDate.getYear() / 10) * 10;
                headerText = decade + " - " + (decade + 9);
            }

            JButton monthYearButton = new JButton(headerText) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Draw rounded background - compute color dynamically
                    g2.setColor(getCalButtonBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    // Draw border
                    g2.setColor(getCalButtonBorder());
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                    g2.dispose();

                    // Set dynamic foreground before painting text
                    setForeground(getCalButtonFg());
                    super.paintComponent(g);
                }
            };
            monthYearButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            monthYearButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            monthYearButton.setFocusPainted(false);
            monthYearButton.setOpaque(false);
            monthYearButton.setContentAreaFilled(false);
            monthYearButton.setBorderPainted(false);
            monthYearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            monthYearButton.addActionListener(e -> {
                if (viewMode == 0) {
                    animateViewChange(1); // Days -> Months
                } else if (viewMode == 1) {
                    animateViewChange(2); // Months -> Years
                }
            });

            header.add(monthYearButton, BorderLayout.CENTER);
            return header;
        }

        private JPanel createFooter() {
            JPanel footer = new JPanel(new GridLayout(1, 3, 6, 0));
            footer.setOpaque(false);

            // Navigation buttons with custom styling
            JButton prevButton = new JButton("<") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(getCalButtonBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    g2.setColor(getCalButtonBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                    g2.dispose();

                    // Set dynamic foreground before painting text
                    setForeground(getCalButtonFg());
                    super.paintComponent(g);
                }
            };
            prevButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            prevButton.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            prevButton.setFocusPainted(false);
            prevButton.setOpaque(false);
            prevButton.setContentAreaFilled(false);
            prevButton.setBorderPainted(false);
            prevButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            prevButton.addActionListener(e -> {
                if (viewMode == 0) {
                    currentDate = currentDate.minusMonths(1);
                } else if (viewMode == 1) {
                    currentDate = currentDate.minusYears(1);
                } else {
                    currentDate = currentDate.minusYears(10);
                }
                updateCalendar();
            });

            JButton nextButton = new JButton(">") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(getCalButtonBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    g2.setColor(getCalButtonBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                    g2.dispose();

                    // Set dynamic foreground before painting text
                    setForeground(getCalButtonFg());
                    super.paintComponent(g);
                }
            };
            nextButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            nextButton.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            nextButton.setFocusPainted(false);
            nextButton.setOpaque(false);
            nextButton.setContentAreaFilled(false);
            nextButton.setBorderPainted(false);
            nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            nextButton.addActionListener(e -> {
                if (viewMode == 0) {
                    currentDate = currentDate.plusMonths(1);
                } else if (viewMode == 1) {
                    currentDate = currentDate.plusYears(1);
                } else {
                    currentDate = currentDate.plusYears(10);
                }
                updateCalendar();
            });

            // Today button with custom styling
            JButton todayButton = new JButton("Today") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(getCalButtonBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    g2.setColor(getCalButtonBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                    g2.dispose();

                    // Set dynamic foreground before painting text
                    setForeground(getCalButtonFg());
                    super.paintComponent(g);
                }
            };
            todayButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            todayButton.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            todayButton.setFocusPainted(false);
            todayButton.setOpaque(false);
            todayButton.setContentAreaFilled(false);
            todayButton.setBorderPainted(false);
            todayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            todayButton.addActionListener(e -> {
                currentDate = LocalDate.now();
                if (viewMode != 0) {
                    animateViewChange(0);
                } else {
                    updateCalendar();
                }
            });

            footer.add(prevButton);
            footer.add(todayButton);
            footer.add(nextButton);

            return footer;
        }

        // createNavButton removed - calendar buttons use inline styling

        private JPanel createDayView() {
            JPanel panel = new JPanel(new GridLayout(0, 7, 5, 5));
            panel.setOpaque(false);

            // Day headers
            String[] daysOfWeek = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
            for (String day : daysOfWeek) {
                JLabel dayLabel = new JLabel(day, SwingConstants.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        setForeground(getCalDayHeaderFg());
                        super.paintComponent(g);
                    }
                };
                dayLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                dayLabel.setOpaque(false);
                panel.add(dayLabel);
            }

            YearMonth yearMonth = YearMonth.from(currentDate);
            int firstDayOfMonth = currentDate.withDayOfMonth(1).getDayOfWeek().getValue(); // 1=Mon
            int daysInMonth = yearMonth.lengthOfMonth();

            // Add empty slots for the first week
            for (int i = 1; i < firstDayOfMonth; i++) {
                JLabel emptyLabel = new JLabel("");
                emptyLabel.setOpaque(false);
                panel.add(emptyLabel);
            }

            // Add day cells
            for (int day = 1; day <= daysInMonth; day++) {
                JPanel dayCell = createDayCell(day);
                panel.add(dayCell);
            }

            return panel;
        }

        private JPanel createMonthView() {
            JPanel panel = new JPanel(new GridLayout(4, 3, 8, 8));
            panel.setOpaque(false);

            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            for (int i = 0; i < 12; i++) {
                final int monthIndex = i;
                final boolean isCurrentMonth = (i + 1 == LocalDate.now().getMonthValue() && currentDate.getYear() == LocalDate.now().getYear());

                JButton monthBtn = new JButton(months[i]) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        // Always use fresh theme colors
                        if (isCurrentMonth) {
                            setBackground(todayColor());
                            setForeground(Color.WHITE);
                        } else {
                            setBackground(getCalButtonBg());
                            setForeground(getCalButtonFg());
                        }
                        super.paintComponent(g);
                    }
                };
                monthBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                monthBtn.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));
                monthBtn.setFocusPainted(false);
                monthBtn.setOpaque(true);
                monthBtn.setBorderPainted(false);
                monthBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                monthBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!isCurrentMonth) {
                            Color hoverBg = FlatLaf.isLafDark() ? new Color(58, 58, 58) : new Color(230, 230, 230);
                            monthBtn.setBackground(hoverBg);
                            monthBtn.repaint();
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (!isCurrentMonth) {
                            monthBtn.repaint(); // Let paintComponent set the right color
                        }
                    }
                });

                monthBtn.addActionListener(e -> {
                    currentDate = currentDate.withMonth(monthIndex + 1);
                    animateViewChange(0);
                });

                panel.add(monthBtn);
            }

            return panel;
        }

        private JPanel createYearView() {
            JPanel panel = new JPanel(new GridLayout(4, 5, 8, 8));
            panel.setOpaque(false);

            int decade = (currentDate.getYear() / 10) * 10;
            int startYear = decade - 4;

            for (int i = 0; i < 20; i++) {
                final int year = startYear + i;
                final boolean isCurrentYear = (year == LocalDate.now().getYear());

                JButton yearBtn = new JButton(String.valueOf(year)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        // Always use fresh theme colors
                        if (isCurrentYear) {
                            setBackground(todayColor());
                            setForeground(Color.WHITE);
                        } else {
                            setBackground(getCalButtonBg());
                            setForeground(getCalButtonFg());
                        }
                        super.paintComponent(g);
                    }
                };
                yearBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                yearBtn.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
                yearBtn.setFocusPainted(false);
                yearBtn.setOpaque(true);
                yearBtn.setBorderPainted(false);
                yearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                yearBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!isCurrentYear) {
                            Color hoverBg = FlatLaf.isLafDark() ? new Color(58, 58, 58) : new Color(230, 230, 230);
                            yearBtn.setBackground(hoverBg);
                            yearBtn.repaint();
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (!isCurrentYear) {
                            yearBtn.repaint(); // Let paintComponent set the right color
                        }
                    }
                });

                yearBtn.addActionListener(e -> {
                    currentDate = currentDate.withYear(year);
                    animateViewChange(1);
                });

                panel.add(yearBtn);
            }

            return panel;
        }

        private JPanel createDayCell(int day) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(false);

            // Determine date and whether it's today or deadline
            LocalDate cellDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), day);
            boolean isToday = cellDate.equals(LocalDate.now());
            CourseDeadline foundDeadline = null;
            for (CourseDeadline cd : courseDeadlines) {
                if (cd.date.equals(cellDate)) {
                    foundDeadline = cd;
                    break;
                }
            }
            final CourseDeadline deadline = foundDeadline;
            final boolean isDeadline = (deadline != null);

            JLabel dayLabel = new JLabel(String.valueOf(day), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Color highlight = isToday ? todayColor() : (isDeadline ? deadlineColor() : null);
                    if (highlight != null) {
                        int w = getWidth();
                        int h = getHeight();
                        int pad = 8;
                        int diameter = Math.min(w, h) - pad;
                        if (diameter < 8) diameter = Math.min(w, h);
                        int x = (w - diameter) / 2;
                        int y = (h - diameter) / 2;
                        g2.setColor(highlight);
                        g2.fillOval(x, y, diameter, diameter);
                        g2.setColor(highlight.darker());
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawOval(x, y, diameter, diameter);
                        setForeground(Color.WHITE);
                    } else {
                        setForeground(textColor());
                    }

                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            dayLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            dayLabel.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));

            if (isDeadline && deadline != null) {
                dayLabel.setToolTipText("Registration of \"" + deadline.courseName + "\" ends");
                // optional click to show details
                dayLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(DashboardForm.this,
                                    "Deadline for: " + deadline.courseName + " on " + deadline.date,
                                    "Deadline Details", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                });
            }

            cell.add(dayLabel, BorderLayout.CENTER);
            return cell;
        }
    }

    // Helper class for rounded borders
    private static class RoundedBorder implements Border {

        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
    }
}

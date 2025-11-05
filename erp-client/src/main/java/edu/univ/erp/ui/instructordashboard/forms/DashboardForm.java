package edu.univ.erp.ui.instructordashboard.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.api.NotificationAPI;
import edu.univ.erp.domain.Section;
import edu.univ.erp.ui.instructordashboard.components.SimpleForm;
import edu.univ.erp.ui.instructordashboard.menu.FormManager;
import edu.univ.erp.util.UIHelper;
import net.miginfocom.swing.MigLayout;

public class DashboardForm extends SimpleForm {

    // Removed gauge card fields
    private JPanel notificationListPanel;
    private JPanel courseCardsContainer; // Panel to hold the new CourseCardPanels
    private javax.swing.JScrollPane courseListScrollPane;
    private javax.swing.JScrollPane notifScrollPane;
    private JPanel courseWrapperPanel;
    private JPanel notifWrapperPanel;

    private volatile long notificationsFetchSeq = 0L;
    // private InteractiveCalendarPanel calendarPanel; // REMOVED
    private Timer resizeSyncTimer; // debounce timer for resize sync
    private boolean windowListenersInstalled = false; // ensure we install once
    private Window attachedWindow = null; // the window we attached listeners to
    private boolean lafListenerInstalled = false; // ensure we add Look&Feel listener once
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

    // REMOVED calendar-specific color helpers (todayColor, deadlineColor)
    public DashboardForm() {
        init();
    }

    /**
     * DashboardForm initializer. SimpleForm defines a private init() which is
     * not visible to subclasses, so provide a local init() to perform component
     * initialization and kick off the usual refresh.
     */
    private void init() {
        // keep panel transparent by default and let formRefresh set layout/content
        setOpaque(false);
        // Ensure any subclass-specific initialization runs via formRefresh
        formRefresh();

        // When the panel's showing state changes (e.g., after PanelSlider transitions),
        // schedule a relayout so the scroll panes and cards size correctly.
        addHierarchyListener(evt -> {
            if ((evt.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(() -> {
                    installWindowListeners(); // idempotent
                    scheduleResizeSync();
                });
            }
        });
    }

    @Override
    public void formRefresh() {
        // Clear any previously-built components to avoid duplicate children when
        // formRefresh is called multiple times (refresh button, theme change, etc.)
        removeAll();

        // Update background color
        setBackground(panelBg());

        // REFRESH CALENDAR BLOCK - REMOVED
        // Refresh notifications to update text colors
        refreshNotificationList();

        // Refresh course cards (clear and re-fetch)
        if (courseCardsContainer != null) {
            courseCardsContainer.removeAll();
        }
        fetchAndPopulateInstructorCourses(); // Re-fetch courses on theme change

        // Force repaint of all components to pick up new theme colors
        revalidate();
        repaint();

        // Use theme-aware background
        setBackground(panelBg());

        // Layout: welcome row, section headers, then main content row with two columns
        // Left column grows, right column uses a fixed width for notifications for visual balance
        setLayout(new MigLayout("fill,insets 18", "[grow][360!]", "[]12[]8[grow]"));

        String username = "";
        try {
            edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
            if (cu != null) {
                username = cu.getUsername();
            }
        } catch (Throwable ignore) {
        }

        // Welcome label (spans both columns)
        JLabel welcomeLabel = new JLabel("Welcome back" + (username.isEmpty() ? ", Instructor!" : ", " + username + "!")) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        welcomeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        FadePanel welcomeWrapper = new FadePanel(0f);
        welcomeWrapper.setOpaque(false);
        welcomeWrapper.setLayout(new BorderLayout());
        welcomeWrapper.add(welcomeLabel, BorderLayout.CENTER);
        add(welcomeWrapper, "span,wrap");
        SwingUtilities.invokeLater(() -> welcomeWrapper.startFadeIn(0));

        // Add section headers above the cards
        JLabel sectionsLabel = new JLabel("My Sections") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        sectionsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        JLabel notifsLabel = new JLabel("Notifications 🔔") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        notifsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        add(sectionsLabel, "growx");
        add(notifsLabel, "growx, wrap");

        // Main content row: left = course list card (grows), right = notifications card (fixed width and height)
        add(createCourseListPanel(), "grow, push");
        add(createNotificationsPanel(), "grow, push, wrap");

        // Ensure courses are fetched to populate the list
        fetchAndPopulateInstructorCourses();

        // Refresh the entire window when Look & Feel (mode) changes.
        // This logic is PRESERVED as requested.
        if (!lafListenerInstalled) {
            lafListenerInstalled = true;
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
        }

        // One-time refresh when dashboard is first shown
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

                    // REMOVED calendar update
                    // REMOVED gauge card animation calls
                    // Fetch fresh courses asynchronously
                    fetchAndPopulateInstructorCourses();

                    revalidate();
                    repaint();
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

    @Override
    public void formInitAndOpen() {
        // Called by FormManager when this form becomes the active form.
        SwingUtilities.invokeLater(() -> {
            installWindowListeners(); // idempotent
            scheduleResizeSync();
            // Extra delayed nudge to ensure layout stabilizes after animation
            Timer t = new Timer(220, ev -> {
                scheduleResizeSync();
                ((Timer) ev.getSource()).stop();
            });
            t.setRepeats(false);
            t.start();
        });
    }

    /**
     * Creates the main panel for the left column, which includes a header and a
     * scrollable container for the course cards. REDESIGNED: Small bordered box
     * like student dashboard gauge charts, but wider.
     */
    private JPanel createCourseListPanel() {
        // Build a wrapper with NO separate header (header is in the top summary row)
        courseWrapperPanel = new JPanel(new BorderLayout());
        courseWrapperPanel.setOpaque(false);

        // Container for course cards
        courseCardsContainer = new JPanel();
        courseCardsContainer.setOpaque(false);
        courseCardsContainer.setLayout(new MigLayout("fillx, wrap, insets 6", "[fill,grow]", "[]4[]"));

        // Scrollpane with NO scrollbar (fixed 3 items max)
        courseListScrollPane = new JScrollPane(courseCardsContainer);
        courseListScrollPane.setOpaque(false);
        courseListScrollPane.getViewport().setOpaque(false);
        courseListScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        courseListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        courseListScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        courseListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        // Make the left panel a bit taller
        courseListScrollPane.setPreferredSize(new java.awt.Dimension(0, 520));
        courseListScrollPane.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 520));

        // Place the scroll inside a rounded card for a polished look
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        card.add(courseListScrollPane, BorderLayout.CENTER);

        courseWrapperPanel.add(card, BorderLayout.CENTER);

        // Populate after adding to avoid first-render emptiness
        SwingUtilities.invokeLater(this::fetchAndPopulateInstructorCourses);
        return courseWrapperPanel;
    }

    /**
     * Small summary card used in the top row for quick glance items.
     */
    private JPanel createSummaryCard(String title, String subtitle) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel titleLabel = new JLabel(title) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        JLabel subLabel = new JLabel(subtitle == null ? "" : subtitle) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        subLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(titleLabel, BorderLayout.NORTH);
        content.add(subLabel, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        card.setPreferredSize(new java.awt.Dimension(0, 86));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Fetches the instructor's assigned sections from the server and populates
     * the courseCardsContainer.
     */
    private void fetchAndPopulateInstructorCourses() {
        if (courseCardsContainer == null) {
            return; // Panel not ready yet
        }

        // Clear old cards
        courseCardsContainer.removeAll();

        edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
        if (u == null) {
            return; // Not logged in
        }

        // Show a "Loading..." label
        JLabel loadingLabel = new JLabel("Loading sections...") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        loadingLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
        courseCardsContainer.add(loadingLabel, "align center");
        courseCardsContainer.revalidate();
        courseCardsContainer.repaint();

        UIHelper.runAsync(() -> {
            // Call the central handler which wraps Instructor API
            try {
                edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                if (cu == null) {
                    return new ArrayList<Section>();
                }
                edu.univ.erp.ui.handlers.InstructorUiHandlers handlers = new edu.univ.erp.ui.handlers.InstructorUiHandlers(cu);
                java.util.List<Section> assigned = handlers.displayAssignedSections(cu.getUserId());
                // displayAssignedSections may return empty list on error; keep it safe
                return assigned == null ? new ArrayList<Section>() : new ArrayList<Section>(assigned);
            } catch (Exception ex) {
                // fallback to empty list
                return new ArrayList<Section>();
            }

        }, (Object result) -> {
            // Success callback
            try {
                List<Section> sections = (List<Section>) result;
                courseCardsContainer.removeAll(); // Remove "Loading..." label

                if (sections == null || sections.isEmpty()) {
                    JLabel noneLabel = new JLabel("No sections assigned.") {
                        @Override
                        protected void paintComponent(Graphics g) {
                            setForeground(secondaryTextColor());
                            super.paintComponent(g);
                        }
                    };
                    noneLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
                    courseCardsContainer.add(noneLabel, "align center");
                } else {
                    // Show ONLY first 3 sections to keep layout compact
                    int maxSections = Math.min(sections.size(), 3);
                    for (int i = 0; i < maxSections; i++) {
                        Section sec = sections.get(i);
                        CourseCardPanel card = new CourseCardPanel(sec);
                        // Add fade-in effect
                        FadePanel wrapper = new FadePanel(0f);
                        wrapper.setOpaque(false);
                        wrapper.setLayout(new BorderLayout());
                        wrapper.add(card, BorderLayout.CENTER);

                        courseCardsContainer.add(wrapper, "growx");
                        // Stagger the fade-in
                        final int delay = i * 60;
                        SwingUtilities.invokeLater(() -> wrapper.startFadeIn(delay));
                    }
                }
            } catch (Exception e) {
                // Handle error
                courseCardsContainer.removeAll();
                JLabel errorLabel = new JLabel("Error loading sections: " + e.getMessage()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        setForeground(new Color(200, 50, 50)); // Use a visible error color
                        super.paintComponent(g);
                    }
                };
                errorLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
                courseCardsContainer.add(errorLabel, "align center");
            }
            courseCardsContainer.revalidate();
            courseCardsContainer.repaint();

        }, (Exception ex) -> {
            // Error callback
            courseCardsContainer.removeAll();
            JLabel errorLabel = new JLabel("Failed to load sections: " + ex.getMessage()) {
                @Override
                protected void paintComponent(Graphics g) {
                    setForeground(new Color(200, 50, 50)); // Use a visible error color
                    super.paintComponent(g);
                }
            };
            errorLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
            courseCardsContainer.add(errorLabel, "align center");
            courseCardsContainer.revalidate();
            courseCardsContainer.repaint();
        });
    }

    // REMOVED fetchAndPopulateStudentMetrics()
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
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null) {
            return; // not yet attached
        }
        // If we've already attached to the same window, nothing to do.
        if (attachedWindow == w && windowListenersInstalled) {
            return;
        }
        attachedWindow = w;
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

        // Also listen for focus/activation events — switching away and back
        // (or restoring from minimized) can leave the layout collapsed; when
        // the window regains focus/activation, re-run the resize sync and also
        // schedule a couple of delayed nudges to handle OS/window-manager
        // timing variability.
        try {
            w.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    // Force immediate validate/repaint and pack() then schedule nudges
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // Avoid calling pack() here — it can shrink the window when
                            // preferred sizes are smaller than current size. Use validate
                            // and repaint to stabilize layout without changing window size.
                            w.invalidate();
                            w.validate();
                            //w.pack();
                            w.repaint();
                        } catch (Throwable ignore) {
                        }
                    });
                    scheduleResizeSync();
                    // Additional delayed nudges with longer intervals to ensure layout stabilizes
                    Timer t1 = new Timer(120, ev -> {
                        scheduleResizeSync();
                        ((Timer) ev.getSource()).stop();
                    });
                    t1.setRepeats(false);
                    t1.start();
                    Timer t2 = new Timer(350, ev -> {
                        scheduleResizeSync();
                        ((Timer) ev.getSource()).stop();
                    });
                    t2.setRepeats(false);
                    t2.start();
                }
            });

            w.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // Avoid pack() on activation — prefer validate/repaint to
                            // prevent unexpected window size changes when returning
                            // focus to the application.
                            w.invalidate();
                            w.validate();
                            w.repaint();
                        } catch (Throwable ignore) {
                        }
                    });
                    scheduleResizeSync();
                }

                @Override
                public void windowDeiconified(WindowEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // Avoid pack() after deiconify as well — rely on the resize
                            // sync to adjust layout without enforcing a pack().
                            w.invalidate();
                            w.validate();
                            w.repaint();
                        } catch (Throwable ignore) {
                        }
                    });
                    scheduleResizeSync();
                }
            });
        } catch (Throwable ignore) {
            // Some lightweight window implementations may not support these
            // listeners; ignore failures to remain robust.
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
        refreshNotificationList();

        // Simplified: just validate and repaint without dynamically resizing scroll panes
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

    private JPanel createNoticeItem(edu.univ.erp.domain.Notification notif, int index) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        // Add comfortable outer padding so each notice breathes
        item.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        // Set fixed size for each notification item to prevent infinite growth
        item.setPreferredSize(new java.awt.Dimension(0, 75));
        item.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 75));

        // Title and time in one line
        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BorderLayout(10, 0));

        // Prepend a small numeric marker to the title
        String titleText = (notif.getTitle() == null ? "" : notif.getTitle());
        JLabel titleLabel = new JLabel(String.format("%d. %s", index, titleText)) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        JLabel timeLabel = new JLabel(getTimeAgo(parseTimestamp(notif.getTimestamp()))) {
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
        JTextArea messageLabel = new JTextArea(notif.getMessage() == null ? "" : notif.getMessage());
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);
        messageLabel.setOpaque(false);
        messageLabel.setEditable(false);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageLabel.setForeground(secondaryTextColor());
        // Slight left indent for the message to visually separate from the title
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 0));
        // Remove any internal top margin so the body sits closer to the title
        messageLabel.setMargin(new java.awt.Insets(3, 10, 3, 0));
        messageLabel.setFocusable(false);

        item.add(titleRow);
        // Remove extra vertical gap between title and body (keep very tight)
        // small gap handled via the JTextArea Insets and the card padding
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

    // REMOVED createCalendarPanel()
    // REMOVED createGaugeCard()
    private JPanel createNotificationsPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        // Slightly reduce inner padding to pull content up and avoid overflow
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Build a wrapper with NO separate header (header is in the top summary row)
        notifWrapperPanel = new JPanel(new BorderLayout());
        // Add subtle bottom padding for a professional look
        notifWrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        notifWrapperPanel.setOpaque(false);
        notifWrapperPanel.add(card, BorderLayout.CENTER);

        // notification list panel with comfortable padding
        notificationListPanel = new JPanel();
        notificationListPanel.setOpaque(false);
        notificationListPanel.setLayout(new BoxLayout(notificationListPanel, BoxLayout.Y_AXIS));
        notificationListPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        // Fixed size for 5 notifications; add a little extra breathing room
        notificationListPanel.setPreferredSize(new java.awt.Dimension(0, 420));
        notificationListPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 420));

        // Wrap notifications in a scroll pane with NO scrollbar (fixed 5 items max)
        notifScrollPane = new JScrollPane(notificationListPanel);
        notifScrollPane.setOpaque(false);
        notifScrollPane.getViewport().setOpaque(false);
        notifScrollPane.setBorder(BorderFactory.createEmptyBorder());
        notifScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        notifScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        notifScrollPane.getVerticalScrollBar().setUnitIncrement(14);
        // Make notifications card slightly taller as well
        notifScrollPane.setPreferredSize(new java.awt.Dimension(0, 460));
        notifScrollPane.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 460));

        // Place the scroll inside the card. The wrapper contains the header above it
        card.add(notifScrollPane, BorderLayout.CENTER);

        // Populate after adding to avoid first-render emptiness
        SwingUtilities.invokeLater(this::refreshNotificationList);
        return notifWrapperPanel;
    }

    private void refreshNotificationList() {
        if (notificationListPanel == null) {
            return;
        }
        notificationListPanel.removeAll();
        // bump sequence id so old async responses are ignored
        final long mySeq = ++notificationsFetchSeq;

        // Async fetch notifications for current user; fall back to dummy data on error
        UIHelper.runAsync(() -> {
            edu.univ.erp.domain.UserAuth u = edu.univ.erp.ClientContext.getCurrentUser();
            if (u == null) {
                return new java.util.ArrayList<edu.univ.erp.domain.Notification>();
            }
            // THIS LOGIC IS PRESERVED - It correctly switches to INSTRUCTOR
            String recipientType = "STUDENT";
            try {
                String role = u.getRole();
                if (role != null && role.toUpperCase().contains("INSTRUCTOR")) {
                    recipientType = "INSTRUCTOR";
                }
            } catch (Throwable ignore) {
            }
            NotificationAPI api = new NotificationAPI();
            try {
                // Limit server fetch to 5 notifications for a compact notifications panel
                java.util.List<edu.univ.erp.domain.Notification> list = api.fetchNotificationsForUser(u.getUserId(), recipientType, 5);
                return list == null ? new java.util.ArrayList<edu.univ.erp.domain.Notification>() : list; // allow empty list
            } catch (Exception ex) {
                return new java.util.ArrayList<edu.univ.erp.domain.Notification>();
            }
        }, (java.util.List<edu.univ.erp.domain.Notification> result) -> {
            try {
                // Ignore stale responses
                if (mySeq != notificationsFetchSeq) {
                    return;
                }

                if (result == null || result.isEmpty()) {
                    JLabel none = new JLabel("No notifications") {
                        @Override
                        protected void paintComponent(Graphics g) {
                            setForeground(secondaryTextColor());
                            super.paintComponent(g);
                        }
                    };
                    none.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
                    none.setHorizontalAlignment(SwingConstants.CENTER);
                    notificationListPanel.add(none);
                } else {
                    // Show ONLY first 5 notifications to keep layout compact
                    int maxNotifs = Math.min(result.size(), 5);
                    for (int i = 0; i < maxNotifs; i++) {
                        edu.univ.erp.domain.Notification n = result.get(i);
                        JPanel item = createNoticeItem(n, i + 1);
                        FadePanel wrapper = new FadePanel(0f);
                        wrapper.setOpaque(false);
                        wrapper.setLayout(new BorderLayout());
                        wrapper.add(item, BorderLayout.CENTER);
                        notificationListPanel.add(wrapper);
                        final int delay = i * 60;
                        SwingUtilities.invokeLater(() -> wrapper.startFadeIn(delay));
                        if (i < maxNotifs - 1) {
                            // subtle 1px separator line that adapts to theme via secondaryTextColor
                            JPanel divider = new JPanel() {
                                @Override
                                protected void paintComponent(Graphics g) {
                                    super.paintComponent(g);
                                    // Use secondary text color for a subtle, theme-aware line
                                    Color line = secondaryTextColor();
                                    g.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), 70)); // Make it semi-transparent
                                    g.fillRect(10, 4, getWidth() - 20, 1); // Inset the line
                                }
                            };
                            divider.setOpaque(false);
                            divider.setPreferredSize(new java.awt.Dimension(0, 9));
                            divider.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 9));
                            notificationListPanel.add(divider);
                        }
                    }
                }
                notificationListPanel.revalidate();
                notificationListPanel.repaint();
            } catch (Throwable t) {
                // Ensure UI remains stable
                notificationListPanel.revalidate();
                notificationListPanel.repaint();
            }
        }, (Exception ex) -> {
            // On error, show a concise empty/failure state instead of dummy data
            if (mySeq != notificationsFetchSeq) {
                return;
            }
            javax.swing.JLabel failed = new javax.swing.JLabel("Failed to load notifications") {
                @Override
                protected void paintComponent(Graphics g) {
                    setForeground(secondaryTextColor());
                    super.paintComponent(g);
                }
            };
            failed.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            failed.setHorizontalAlignment(SwingConstants.CENTER);
            notificationListPanel.add(failed);
            notificationListPanel.revalidate();
            notificationListPanel.repaint();
        });
    }

    // Parse timestamp string into LocalDateTime. Accepts ISO with 'T', offsets, and SQL DATETIME formats.
    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null) {
            return LocalDateTime.now();
        }
        // Try ISO-8601 LocalDateTime first
        try {
            return LocalDateTime.parse(ts);
        } catch (Exception ignored) {
        }

        // Try OffsetDateTime (e.g., 2025-10-23T11:00:00Z or with offset)
        try {
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(ts);
            return odt.toLocalDateTime();
        } catch (Exception ignored) {
        }

        // Try common SQL DATETIME formats: 'yyyy-MM-dd HH:mm:ss' and fractional seconds
        java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[]{
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        };
        for (java.time.format.DateTimeFormatter fmt : fmts) {
            try {
                return LocalDateTime.parse(ts, fmt);
            } catch (Exception ignored) {
            }
        }

        // Fallback to now
        return LocalDateTime.now();
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

    /**
     * Reusable card background panel. PRESERVED.
     */
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

    /**
     * RENAMED from GlassCardPanel. This is the new Course Card. It REPLACES the
     * gauge content with text labels but KEEPS the glass background and hover
     * effect.
     */
    private class CourseCardPanel extends JPanel {

        // private final AnimatedGaugeChart gauge; // REMOVED
        // No field required: constructor parameter 'section' is used directly by inner listeners
        private float hoverProgress = 0f; // 0..1
        private float hoverTarget = 0f;
        private Timer hoverTimer;

        public CourseCardPanel(Section section) {
            // this.gauge = ... // REMOVED
            setOpaque(false);
            setLayout(new BorderLayout());

            // --- NEW CONTENT ---
            // Create a panel to hold the text details
            JPanel contentPanel = new JPanel(new MigLayout("fillx, insets 18 24 18 24", "[grow]", "[]10[]10[]"));
            contentPanel.setOpaque(false);

            // Course Code (Big and Bold)
            JLabel courseCodeLabel = new JLabel(section.getCourseCode()) {
                @Override
                protected void paintComponent(Graphics g) {
                    setForeground(textColor()); // Theme-aware
                    super.paintComponent(g);
                }
            };
            courseCodeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

            // Course Title (Secondary)
            JLabel courseTitleLabel = new JLabel(section.getCourseName()) {
                @Override
                protected void paintComponent(Graphics g) {
                    setForeground(secondaryTextColor()); // Theme-aware
                    super.paintComponent(g);
                }
            };
            courseTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

            // Section (Also secondary, but distinct)
            JLabel sectionLabel = new JLabel("Section: " + section.getSectionId()) {
                @Override
                protected void paintComponent(Graphics g) {
                    setForeground(textColor()); // Theme-aware
                    super.paintComponent(g);
                }
            };
            sectionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

            contentPanel.add(courseCodeLabel, "wrap");
            contentPanel.add(courseTitleLabel, "wrap");
            contentPanel.add(sectionLabel, "wrap, gaptop 10"); // Add some space above section

            add(contentPanel, BorderLayout.CENTER);
            // --- END NEW CONTENT ---

            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // REMOVED componentShown listener (no animation to start)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // ACTION: Open the course form for this section
                    try {
                        FormManager.showForm(new MyCourseForm(section));
                    } catch (Throwable t) {
                        JOptionPane.showMessageDialog(DashboardForm.this,
                                "Opening " + section.getCourseCode() + " - Section " + section.getSectionId(),
                                "Navigation",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverTo(1f);
                    // startAnimation(); // REMOVED
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

        // REMOVED startAnimation()
        // REMOVED updateGauge() methods
        /**
         * This paintComponent is PRESERVED from GlassCardPanel to give the same
         * background and hover effect.
         */
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

            // Hover overlay (subtle lightening) - PRESERVED
            if (hoverProgress > 0f) {
                int alpha = (int) (hoverProgress * (isDark ? 50 : 30));
                // Use the accent color for a more professional hover
                Color accent = accentColor();
                Color hoverOverlay = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (alpha * 0.5f)); // More subtle
                g2.setColor(hoverOverlay);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));

                // Also draw a border highlight
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (alpha * 1.5f)));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 18, 18));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // REMOVED AnimatedGaugeChart inner class
    // REMOVED CourseDeadline inner class
    // REMOVED InteractiveCalendarPanel inner class
    // REMOVED RoundedBorder inner class (wasn't used)
    // Using domain Section from edu.univ.erp.domain.Section
}

package edu.univ.erp.ui.studentdashboard.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.ClientContext;
import edu.univ.erp.api.NotificationAPI;
import edu.univ.erp.domain.Notification;
import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import edu.univ.erp.ui.studentdashboard.menu.FormManager;
import net.miginfocom.swing.MigLayout;

public class NotificationForm extends SimpleForm {

    private JPanel notificationListPanel; // The panel that holds all items
    private JScrollPane scrollPane;
    private JPanel headerPanel;
    private JLabel titleLabel;
    private JButton backButton;

    // --- Theme Helpers (Copied from your DashboardForm) ---

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

    private Color getCalButtonBg() {
        return FlatLaf.isLafDark() ? new Color(42, 42, 42) : new Color(245, 245, 245);
    }

    private Color getCalButtonFg() {
        return FlatLaf.isLafDark() ? new Color(234, 234, 234) : new Color(30, 30, 30);
    }

    private Color getCalButtonBorder() {
        return FlatLaf.isLafDark() ? new Color(74, 74, 74) : new Color(200, 200, 200);
    }

    private Color getDividerColor() {
        return FlatLaf.isLafDark() ? new Color(50, 50, 50) : new Color(220, 220, 220);
    }

    // We use the client-domain `edu.univ.erp.domain.Notification` returned by the API

    // --- "Time ago" Helper (Copied from your DashboardForm) ---
    private String getTimeAgo(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + (minutes == 1 ? " min ago" : " mins ago");
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        return days + (days == 1 ? " day ago" : " days ago");
    }

    /**
     * Creates one notification item (reusable)
     * Spacing is now tighter for a professional look.
     */
    private JPanel createNoticeItem(Notification notif) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String titleText = notif.getTitle() == null ? "" : notif.getTitle();
        JLabel itemTitleLabel = new JLabel(titleText) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        itemTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        String timeAgo = "";
        try {
            if (notif.getTimestamp() != null && !notif.getTimestamp().isEmpty()) {
                LocalDateTime ts = LocalDateTime.parse(notif.getTimestamp());
                timeAgo = getTimeAgo(ts);
            }
        } catch (Exception ex) {
            timeAgo = "";
        }

        JLabel timeLabel = new JLabel(timeAgo) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

    titleRow.add(itemTitleLabel, BorderLayout.WEST);
        titleRow.add(timeLabel, BorderLayout.EAST);

        String msgText = notif.getMessage() == null ? "" : notif.getMessage();
        JTextArea messageLabel = new JTextArea(msgText) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(secondaryTextColor());
                super.paintComponent(g);
            }
        };
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);
        messageLabel.setOpaque(false);
        messageLabel.setEditable(false);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageLabel.setBorder(null);
        messageLabel.setFocusable(false);
        messageLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        item.add(titleRow);
        item.add(Box.createVerticalStrut(4));
        item.add(messageLabel);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divider = new JPanel();
        divider.setOpaque(false);
        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getDividerColor()));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(item);
        container.add(Box.createVerticalStrut(8));
        container.add(divider);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        return container;
    }


    public NotificationForm() {
        init();
    }

    private void init() {
        setBackground(panelBg());
        // Match other forms: 20px insets, let scroll pane fill to bottom
        setLayout(new MigLayout("fill, insets 20", "[fill]", "[][grow,fill]"));        // 1. Header Panel
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("All Notifications") {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        // Match other forms: 32pt bold for main headlines
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));

    backButton = new ThemeableButton("Back to Dashboard");
        backButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            // This assumes you have a FormManager like in your DashboardForm
            // to handle navigation.
            // If not, you'll need to pass a reference to your main frame
            // to handle closing this form.
            try {
                 FormManager.showForm(new DashboardForm());
            } catch (Exception ex) {
                System.err.println("Navigation failed. Implement back button logic.");
                // Fallback: just close this window
                SwingUtilities.getWindowAncestor(this).dispose();
            }
        });

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(backButton, BorderLayout.EAST);

        add(headerPanel, "wrap");

        // 2. Main Scrollable List
        notificationListPanel = new JPanel();
        notificationListPanel.setOpaque(true);
        notificationListPanel.setBackground(panelBg());
    // Use a single fill column; we'll center a 90% width content block per item inside a full-width row
    notificationListPanel.setLayout(new MigLayout("ins 0, fillx, wrap 1", "[grow,fill]", ""));
    // Add padding around the notification list (increase top padding for more space under headline)
        // Add symmetric left/right padding so content isn't flush to the window edges
        notificationListPanel.setBorder(BorderFactory.createEmptyBorder(24, 12, 20, 12));

        scrollPane = new JScrollPane(notificationListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(panelBg());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        // Only show vertical scrollbar when needed
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        // Let scroll pane expand to fill window bottom - no artificial size limits
        // MigLayout "grow,fill" constraint handles proper expansion
        add(scrollPane, "grow");
        
        // Add theme change listener
        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    formRefresh();
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        SwingUtilities.updateComponentTreeUI(w);
                    }
                });
            }
        });

        // Load data *after* the form is visible to allow animation to play
        SwingUtilities.invokeLater(this::loadAllNotifications);
    }

    /**
     * Loads all notifications with the staggered fade-in animation.
     */
    private void loadAllNotifications() {
        notificationListPanel.removeAll();
        // Fetch real notifications from server asynchronously
        notificationListPanel.add(new JLabel("Loading..."));
        SwingUtilities.invokeLater(() -> {
            // Run in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    if (ClientContext.getCurrentUser() == null) {
                        SwingUtilities.invokeLater(() -> {
                            notificationListPanel.removeAll();
                            notificationListPanel.add(new JLabel("No user authenticated."));
                            notificationListPanel.revalidate();
                            notificationListPanel.repaint();
                        });
                        return;
                    }

                    int uid = ClientContext.getCurrentUser().getUserId();
                    String role = ClientContext.getCurrentUser().getRole();
                    NotificationAPI api = new NotificationAPI();
                    List<Notification> allNotifications = api.fetchNotificationsForUser(uid, role, 50);

                    SwingUtilities.invokeLater(() -> {
                        notificationListPanel.removeAll();
                        int count = allNotifications == null ? 0 : allNotifications.size();
                        for (int i = 0; i < count; i++) {
                            Notification n = allNotifications.get(i);
                            JPanel item = createNoticeItem(n);

                            FadePanel wrapper = new FadePanel(0f);
                            wrapper.setOpaque(false);
                            wrapper.setLayout(new MigLayout("ins 0, fillx", "[grow,center]", ""));
                            wrapper.add(item, "w 90%!, alignx center");
                            notificationListPanel.add(wrapper, "growx, gapbottom 8");

                            final int delay = i * 40;
                            wrapper.startFadeIn(delay);
                        }

                        if (count == 0) {
                            notificationListPanel.add(new JLabel("No notifications."));
                        }

                        notificationListPanel.revalidate();
                        notificationListPanel.repaint();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        notificationListPanel.removeAll();
                        notificationListPanel.add(new JLabel("Failed to load notifications: " + ex.getMessage()));
                        notificationListPanel.revalidate();
                        notificationListPanel.repaint();
                    });
                }
            }).start();
        });

        notificationListPanel.revalidate();
        notificationListPanel.repaint();
    }

    /**
     * A longer list with shorter, professional messages.
     */
    

    @Override
    public void formRefresh() {
        // Update background colors
        setBackground(panelBg());
        notificationListPanel.setBackground(panelBg());
        scrollPane.getViewport().setBackground(panelBg());

        // Update scrollbars
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        // Reload all items to apply new theme colors
        loadAllNotifications();
        
        // Repaint all components
        headerPanel.revalidate();
        headerPanel.repaint();
        revalidate();
        repaint();
    }

    // --- Inner Class: FadePanel (Copied from your DashboardForm) ---
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
                // Start from 0 for fade-in effect
                alpha = 0f; 
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

    // --- Inner Class: ThemeableButton (For "Back" button) ---
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
    
    // --- Inner Class: CustomScrollBarUI (Theme-Aware) ---
    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        private Color thumb;
        private Color track;

        public CustomScrollBarUI() {
            updateColors();
        }

        private void updateColors() {
            this.thumb = FlatLaf.isLafDark() ? Color.decode("#4A4A4A") : Color.decode("#C7C7CC");
            this.track = FlatLaf.isLafDark() ? new Color(39, 39, 39) : new Color(255, 255, 255);
        }

        @Override
        protected void configureScrollBarColors() {
            updateColors();
            this.thumbColor = thumb;
            this.trackColor = track;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}
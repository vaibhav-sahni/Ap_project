package edu.univ.erp.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Custom Toast Notification implementation for maintenance mode alerts
 */
public class ToastNotification extends JWindow {

    public enum MessageType {
        INFO(new Color(52, 152, 219), new Color(100, 181, 246), "Info"),
        WARNING(new Color(220, 53, 69), new Color(244, 67, 54), "Warning"), // Red warning color
        ERROR(new Color(220, 53, 69), new Color(244, 67, 54), "Error"),
        SUCCESS(new Color(40, 167, 69), new Color(76, 175, 80), "Success");

        private final Color lightColor;
        private final Color darkColor;
        private final String label;

        MessageType(Color lightColor, Color darkColor, String label) {
            this.lightColor = lightColor;
            this.darkColor = darkColor;
            this.label = label;
        }

        public Color getColor() {
            return UIManager.getLookAndFeel().getName().toLowerCase().contains("dark") ? darkColor : lightColor;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Timer slideTimer;
    private final Timer hideTimer;
    private final int slideDistance = 50;
    private int targetY;
    private boolean isVisible = false;
    private static ToastNotification currentToast;

    public ToastNotification(Window parent, String message, MessageType type) {
        super(parent);

        // Hide any existing toast
        if (currentToast != null && currentToast.isVisible) {
            currentToast.hideToast();
        }
        currentToast = this;

        setAlwaysOnTop(true);
        setFocusableWindowState(false);

        // Position at top center of parent window with better sizing
        Dimension parentSize = parent.getSize();
        Point parentLocation = parent.getLocation();

        // Calculate better width based on message length and screen size
        int maxWidth = Math.min(600, parentSize.width - 60); // Increased max width
        int minWidth = 400; // Increased min width
        int preferredWidth = Math.max(minWidth, Math.min(maxWidth, message.length() * 7 + 150));

        setSize(preferredWidth, 80); // Increased height for better text fitting

        int x = parentLocation.x + (parentSize.width - getWidth()) / 2;
        targetY = parentLocation.y + 20;

        // Initialize components after sizing is set
        initComponents(message, type);

        setLocation(x, targetY - slideDistance);

        // Slide animation timer
        slideTimer = new Timer(10, new ActionListener() {
            private int currentY = targetY - slideDistance;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentY < targetY) {
                    currentY += 2;
                    setLocation(getX(), currentY);
                } else {
                    slideTimer.stop();
                    isVisible = true;
                }
            }
        });

        // Auto-hide timer (disabled for maintenance mode - requires manual close)
        hideTimer = new Timer(0, e -> hideToast());
        hideTimer.setRepeats(false);
    }

    private void initComponents(String message, MessageType type) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 8));

        // Adapt background and text colors to theme
        Color backgroundColor = UIManager.getColor("Panel.background");
        Color textColor = UIManager.getColor("Label.foreground");
        Color borderColor = type.getColor();

        if (backgroundColor == null) {
            backgroundColor = UIManager.getLookAndFeel().getName().toLowerCase().contains("dark")
                    ? new Color(43, 43, 43) : Color.WHITE;
        }
        if (textColor == null) {
            textColor = UIManager.getLookAndFeel().getName().toLowerCase().contains("dark")
                    ? Color.WHITE : Color.BLACK;
        }

        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 3), // Thicker border for better visibility
                BorderFactory.createEmptyBorder(10, 18, 10, 12) // Increased padding
        ));

        // Type indicator with better sizing
        JLabel typeLabel = new JLabel("● " + type.getLabel());
        typeLabel.setForeground(borderColor);
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        typeLabel.setPreferredSize(new Dimension(80, 20)); // Fixed width for type label

        // Message label with proper text wrapping and sizing
        int availableWidth = getWidth() - 160; // Account for padding, type label, and close button
        String wrappedMessage = "<html><div style='width:" + availableWidth + "px; line-height:1.4; word-wrap:break-word;'>" + message + "</div></html>";
        JLabel messageLabel = new JLabel(wrappedMessage);
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        messageLabel.setForeground(textColor);
        messageLabel.setVerticalAlignment(SwingConstants.CENTER);
        messageLabel.setHorizontalAlignment(SwingConstants.LEFT);

        // Close button with theme adaptation
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeButton.setForeground(borderColor);
        closeButton.setBackground(backgroundColor);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setPreferredSize(new Dimension(28, 28));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeButton.addActionListener(e -> hideToast());
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(borderColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(borderColor);
            }
        });

        // Left panel for type and message with better layout
        JPanel leftPanel = new JPanel(new BorderLayout(8, 0));
        leftPanel.setBackground(backgroundColor);
        leftPanel.add(typeLabel, BorderLayout.WEST);
        leftPanel.add(messageLabel, BorderLayout.CENTER);

        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.EAST);

        add(mainPanel);

        // Make the whole toast clickable to close
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    hideToast();
                }
            }
        });
    }

    public void showToast() {
        setVisible(true);
        slideTimer.start();
    }

    public void hideToast() {
        if (isVisible) {
            isVisible = false;

            Timer slideOutTimer = new Timer(10, new ActionListener() {
                private int currentY = getY();
                private final int endY = targetY - slideDistance;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentY > endY) {
                        currentY -= 3;
                        setLocation(getX(), currentY);
                    } else {
                        ((Timer) e.getSource()).stop();
                        dispose();
                        if (currentToast == ToastNotification.this) {
                            currentToast = null;
                        }
                        // Notify manager that user closed the toast (so it won't reappear until maintenance toggles)
                        try {
                            edu.univ.erp.ui.components.MaintenanceModeManager.getInstance().notifyToastDismissed();
                        } catch (Throwable ignore) {}
                    }
                }
            });
            slideOutTimer.start();
        }
    }

    /**
     * Static method to show maintenance mode notification
     */
    public static void showMaintenanceNotification(Window parent, boolean isMaintenanceMode) {
        if (isMaintenanceMode) {
            String message = "🔧 Maintenance Mode is active - Some features may be temporarily unavailable";
            ToastNotification toast = new ToastNotification(parent, message, MessageType.WARNING);
            toast.showToast();
        } else if (currentToast != null) {
            currentToast.hideToast();
        }
    }

    /**
     * Static method to show custom notifications
     */
    public static void showNotification(Window parent, String message, MessageType type) {
        ToastNotification toast = new ToastNotification(parent, message, type);
        toast.showToast();
    }

    /**
     * Check if maintenance notification is currently visible
     */
    public static boolean isMaintenanceNotificationVisible() {
        return currentToast != null && currentToast.isVisible;
    }
}

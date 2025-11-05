package edu.univ.erp.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Demo window to test maintenance mode notifications
 */
public class MaintenanceModeDemo extends JFrame {

    private MaintenanceModeManager manager;
    private JLabel statusLabel;

    public MaintenanceModeDemo() {
        initComponents();
        manager = MaintenanceModeManager.getInstance();
        manager.registerWindow(this);
    }

    private void initComponents() {
        setTitle("Maintenance Mode Demo");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Title
        JLabel titleLabel = new JLabel("Maintenance Mode Integration Demo");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 20, 10);
        panel.add(titleLabel, gbc);

        // Status
        statusLabel = new JLabel("Status: Checking...");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel.add(statusLabel, gbc);

        // Buttons
        JButton refreshButton = new JButton("Refresh Status");
        refreshButton.addActionListener(e -> {
            manager.refreshMaintenanceStatus();
            updateStatusLabel();
        });
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 5);
        panel.add(refreshButton, gbc);

        JButton toggleButton = new JButton("Toggle Demo Mode");
        toggleButton.addActionListener(e -> {
            manager.toggleMaintenanceMode();
            updateStatusLabel();
        });
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 5, 10, 10);
        panel.add(toggleButton, gbc);

        // Info
        JTextArea infoArea = new JTextArea(
                "This demo shows the maintenance mode integration:\n\n"
                + "• Toast notifications appear when maintenance mode is active\n"
                + "• Notifications persist and reappear when switching windows\n"
                + "• Status is synchronized with the database\n"
                + "• Both student and instructor dashboards will show notifications\n\n"
                + "Click 'Refresh Status' to check the current database state\n"
                + "Click 'Toggle Demo Mode' to test local notifications"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(panel.getBackground());
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setPreferredSize(new Dimension(450, 120));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(scrollPane, gbc);

        add(panel);

        // Initial status update
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        SwingUtilities.invokeLater(() -> {
            boolean isMaintenanceMode = manager.isMaintenanceMode();
            String status = isMaintenanceMode ? "🔧 Maintenance Mode ON" : "✅ Normal Operation";
            statusLabel.setText("Status: " + status);
            statusLabel.setForeground(isMaintenanceMode ? Color.RED : Color.GREEN.darker());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use system look and feel
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Windows".equals(info.getName()) || "GTK".equals(info.getName()) || "Aqua".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MaintenanceModeDemo().setVisible(true);
        });
    }
}

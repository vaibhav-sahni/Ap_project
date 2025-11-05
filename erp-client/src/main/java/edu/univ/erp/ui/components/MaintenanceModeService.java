package edu.univ.erp.ui.components;

import edu.univ.erp.api.common.CommonAPI;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Service to integrate maintenance mode functionality into dashboard menus
 */
public class MaintenanceModeService {

    private final CommonAPI commonAPI;
    private final MaintenanceModeManager manager;

    public MaintenanceModeService() {
        this.commonAPI = new CommonAPI();
        this.manager = MaintenanceModeManager.getInstance();
    }

    /**
     * Create a menu item to manually refresh maintenance status
     */
    public JMenuItem createRefreshStatusMenuItem() {
        JMenuItem refreshItem = new JMenuItem("Refresh Maintenance Status");
        refreshItem.setToolTipText("Check current maintenance mode status from server");

        refreshItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show loading state
                refreshItem.setEnabled(false);
                refreshItem.setText("Checking...");

                // Perform check in background
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return commonAPI.checkMaintenanceMode();
                    }

                    @Override
                    protected void done() {
                        try {
                            boolean maintenanceMode = get();
                            manager.setMaintenanceMode(maintenanceMode);

                            // Show success notification
                            if (manager.getCurrentWindow() != null) {
                                String statusMessage = maintenanceMode
                                        ? "Maintenance mode is currently ACTIVE"
                                        : "Maintenance mode is currently INACTIVE";
                                ToastNotification.MessageType type = maintenanceMode
                                        ? ToastNotification.MessageType.WARNING
                                        : ToastNotification.MessageType.SUCCESS;
                                ToastNotification.showNotification(
                                        manager.getCurrentWindow(),
                                        statusMessage,
                                        type
                                );
                            }
                        } catch (Exception ex) {
                            // Show error notification
                            if (manager.getCurrentWindow() != null) {
                                ToastNotification.showNotification(
                                        manager.getCurrentWindow(),
                                        "Failed to check maintenance status: " + ex.getMessage(),
                                        ToastNotification.MessageType.ERROR
                                );
                            }
                        } finally {
                            // Restore menu item
                            refreshItem.setEnabled(true);
                            refreshItem.setText("Refresh Maintenance Status");
                        }
                    }
                };
                worker.execute();
            }
        });

        return refreshItem;
    }

    /**
     * Create a status indicator label
     */
    public JLabel createMaintenanceStatusLabel() {
        JLabel statusLabel = new JLabel("Status: Checking...");
        statusLabel.setToolTipText("Current maintenance mode status");

        // Update label periodically
        Timer updateTimer = new Timer(15000, e -> updateStatusLabel(statusLabel));
        updateTimer.start();

        // Initial update
        updateStatusLabel(statusLabel);

        return statusLabel;
    }

    private void updateStatusLabel(JLabel label) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return commonAPI.checkMaintenanceMode();
            }

            @Override
            protected void done() {
                try {
                    boolean maintenanceMode = get();
                    String status = maintenanceMode ? "🔧 Maintenance ON" : "✅ Normal Operation";
                    label.setText("Status: " + status);
                    label.setForeground(maintenanceMode
                            ? java.awt.Color.RED
                            : java.awt.Color.GREEN.darker());
                } catch (Exception ex) {
                    label.setText("Status: Connection Error");
                    label.setForeground(java.awt.Color.GRAY);
                }
            }
        };
        worker.execute();
    }
}

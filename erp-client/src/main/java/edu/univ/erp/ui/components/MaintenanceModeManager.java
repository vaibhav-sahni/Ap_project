package edu.univ.erp.ui.components;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import edu.univ.erp.api.common.CommonAPI;

/**
 * Manages maintenance mode notifications across the application
 */
public class MaintenanceModeManager {

    private static MaintenanceModeManager instance;
    private boolean maintenanceMode = false;
    // If the user manually dismissed the maintenance notification, don't re-show until maintenance toggles
    private boolean dismissedByUser = false;
    private Window currentWindow;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CommonAPI commonAPI = new CommonAPI();
    private long lastNotificationTime = 0;
    private static final long NOTIFICATION_COOLDOWN = 2000; // 2 seconds cooldown

    private MaintenanceModeManager() {
        // Start periodic maintenance mode checking
        startPeriodicMaintenanceCheck();
    }

    public static MaintenanceModeManager getInstance() {
        if (instance == null) {
            instance = new MaintenanceModeManager();
        }
        return instance;
    }

    /**
     * Set maintenance mode state
     */
    public void setMaintenanceMode(boolean enabled) {
        boolean previous = this.maintenanceMode;
        this.maintenanceMode = enabled;

        // Reset dismissed flag when maintenance mode transitions from OFF -> ON
        if (enabled && !previous) {
            dismissedByUser = false;
        }

        if (currentWindow != null) {
            SwingUtilities.invokeLater(() -> {
                // Only show notification if not dismissed by user
                if (!dismissedByUser) {
                    ToastNotification.showMaintenanceNotification(currentWindow, enabled);
                } else {
                    // if dismissed and maintenance turned off, ensure any toast is hidden
                    if (!enabled && ToastNotification.isMaintenanceNotificationVisible()) {
                        ToastNotification.showMaintenanceNotification(currentWindow, false);
                    }
                }
                // repaint window so forms can update (eg. disable buttons)
                currentWindow.invalidate();
                currentWindow.validate();
                currentWindow.repaint();
            });
        }
    }

    /**
     * Register a window to show maintenance notifications
     */
    public void registerWindow(Window window) {
        this.currentWindow = window;

        // Add window focus listener to re-show notification when window gains focus
        window.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (maintenanceMode && !dismissedByUser) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastNotificationTime > NOTIFICATION_COOLDOWN) {
                        // Show notification when window gains focus if maintenance mode is active and not dismissed
                        scheduler.schedule(() -> {
                            SwingUtilities.invokeLater(() -> {
                                ToastNotification.showMaintenanceNotification(window, true);
                                lastNotificationTime = System.currentTimeMillis();
                            });
                        }, 800, TimeUnit.MILLISECONDS); // Increased delay for smoother experience
                    }
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // Keep notification visible even when window loses focus
            }
        });

        // Add window listener for cleanup
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentWindow == window) {
                    currentWindow = null;
                }
            }
        });

        // Show notification if maintenance mode is already active
        if (maintenanceMode) {
            SwingUtilities.invokeLater(() -> {
                ToastNotification.showMaintenanceNotification(window, true);
            });
        }

        // Also perform an immediate check on registration (for login scenarios)
        scheduler.schedule(() -> {
            try {
                boolean databaseMaintenanceMode = commonAPI.checkMaintenanceMode();
                SwingUtilities.invokeLater(() -> {
                    setMaintenanceMode(databaseMaintenanceMode);
                });
            } catch (Exception e) {
                System.err.println("Failed initial maintenance mode check: " + e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Check if maintenance mode is active
     */
    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    /**
     * Get the current registered window
     */
    public Window getCurrentWindow() {
        return currentWindow;
    }

    /**
     * Show a custom notification
     */
    public void showNotification(String message, ToastNotification.MessageType type) {
        if (currentWindow != null) {
            SwingUtilities.invokeLater(() -> {
                ToastNotification.showNotification(currentWindow, message, type);
            });
        }
    }

    /**
     * Called when user dismisses the maintenance toast. This prevents re-showing
     * the maintenance banner until maintenance mode toggles.
     */
    public void notifyToastDismissed() {
        this.dismissedByUser = true;
    }

    /**
     * Simulate maintenance mode toggle for demo purposes
     */
    public void toggleMaintenanceMode() {
        setMaintenanceMode(!maintenanceMode);
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Start periodic maintenance mode checking from database
     */
    private void startPeriodicMaintenanceCheck() {
        // Check maintenance mode every 15 seconds after initial login delay
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean databaseMaintenanceMode = commonAPI.checkMaintenanceMode();

                // Only update if there's a change
                if (databaseMaintenanceMode != maintenanceMode) {
                    SwingUtilities.invokeLater(() -> {
                        setMaintenanceMode(databaseMaintenanceMode);
                    });
                }
            } catch (Exception e) {
                // Log error but don't update maintenance mode state
                System.err.println("Failed to check maintenance mode from database: " + e.getMessage());
            }
        }, 3, 15, TimeUnit.SECONDS); // Initial delay 3 seconds, then every 15 seconds
    }

    /**
     * Manually refresh maintenance mode status from database
     */
    public void refreshMaintenanceStatus() {
        scheduler.execute(() -> {
            try {
                boolean databaseMaintenanceMode = commonAPI.checkMaintenanceMode();
                SwingUtilities.invokeLater(() -> {
                    setMaintenanceMode(databaseMaintenanceMode);
                });
            } catch (Exception e) {
                System.err.println("Failed to refresh maintenance mode status: " + e.getMessage());
                // Show error notification
                if (currentWindow != null) {
                    SwingUtilities.invokeLater(() -> {
                        ToastNotification.showNotification(currentWindow,
                                "Unable to connect to server. Maintenance status may not be current.",
                                ToastNotification.MessageType.WARNING);
                    });
                }
            }
        });
    }

    /**
     * Trigger maintenance notification check on form/view switch This is called
     * when navigating between different forms in the dashboard
     */
    public void onFormSwitch() {
        if (maintenanceMode && currentWindow != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNotificationTime > NOTIFICATION_COOLDOWN) {
                SwingUtilities.invokeLater(() -> {
                    ToastNotification.showMaintenanceNotification(currentWindow, true);
                    lastNotificationTime = System.currentTimeMillis();
                });
            }
        }
    }
}

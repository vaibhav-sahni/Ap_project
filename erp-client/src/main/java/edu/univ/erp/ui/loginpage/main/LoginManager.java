package edu.univ.erp.ui.loginpage.main;

import java.awt.Window;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

/**
 * Helper to ensure only one Login window is shown at a time.
 */
public final class LoginManager {
    private static final AtomicBoolean showing = new AtomicBoolean(false);

    private LoginManager() {}

    public static void showLogin() {
        // If a login is already showing or being created, no-op
        if (!showing.compareAndSet(false, true)) return;

        SwingUtilities.invokeLater(() -> {
            try {
                // Close existing windows to avoid stray frames
                for (Window w : Window.getWindows()) {
                    try { if (w != null) w.dispose(); } catch (Exception ignore) {}
                }

                Login login = new Login();
                // Reset flag when login window is disposed so future calls can show it again
                login.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        showing.set(false);
                    }

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        showing.set(false);
                    }
                });
                login.setVisible(true);
            } catch (Throwable t) {
                // On any failure, ensure flag is cleared so callers can retry
                showing.set(false);
                System.err.println("LOGINMANAGER: failed to show login: " + t.getMessage());
            }
        });
    }
}

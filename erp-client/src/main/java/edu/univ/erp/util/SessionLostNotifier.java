package edu.univ.erp.util;

import java.awt.Window;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Helper to notify the user that the persistent session/connection was lost
 * and redirect them to the Login screen when they acknowledge.
 */
public class SessionLostNotifier {
    private static final AtomicBoolean showing = new AtomicBoolean(false);

    public static void notifyAndRedirect() {
        // Respect suppression flag: if the application intentionally suppressed
        // session-lost notifications (e.g., during an explicit logout), do nothing.
        try {
            if (edu.univ.erp.net.ClientSession.isSuppressSessionLost()) return;
        } catch (Throwable ignore) {
            // If the ClientSession class is not available for some reason, continue.
        }
        // Only show one dialog at a time
        if (!showing.compareAndSet(false, true)) return;

        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    JOptionPane.showMessageDialog(null,
                            "Connection to the ERP server was lost. Please login again.",
                            "Connection lost",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Close existing windows to avoid stray frames
                    for (Window w : Window.getWindows()) {
                        try { if (w != null) w.dispose(); } catch (Exception ignore) {}
                    }

                    // Delegate to LoginManager to ensure single-instance behavior
                    try {
                        edu.univ.erp.ui.loginpage.main.LoginManager.showLogin();
                    } catch (Throwable ex) {
                        // failed to invoke LoginManager; swallow to avoid crash
                    }
                } finally {
                    showing.set(false);
                }
            });
        } catch (Throwable t) {
            // In case Swing isn't available or we are off-EDT, ensure flag is reset
            showing.set(false);
        }
    }
}

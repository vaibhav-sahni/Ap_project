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

                    // Open the login screen (use fully-qualified class to avoid cyclic imports)
                    try {
                        Class<?> loginClass = Class.forName("edu.univ.erp.ui.loginpage.main.Login");
                        Object login = loginClass.getDeclaredConstructor().newInstance();
                        if (login instanceof java.awt.Window) {
                            ((java.awt.Window) login).setVisible(true);
                        }
                    } catch (Exception ex) {
                        // If reflection fails, there's nothing more we can do here.
                        ex.printStackTrace();
                    }
                } finally {
                    showing.set(false);
                }
            });
        } catch (Throwable t) {
            // In case Swing isn't available or we are off-EDT, just log and reset flag
            t.printStackTrace();
            showing.set(false);
        }
    }
}

package edu.univ.erp.ui.studentdashboard.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.UIScale;

import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.ui.components.MaintenanceModeManager;
import edu.univ.erp.ui.studentdashboard.components.Background;
import edu.univ.erp.ui.studentdashboard.forms.DashboardForm;
import edu.univ.erp.ui.studentdashboard.menu.FormManager;
import edu.univ.erp.ui.studentdashboard.model.ModelUser;
import raven.popup.GlassPanePopup;

public class Application extends JFrame {

    private final boolean UNDECORATED = !true;

    public Application() {
        init();
    }

    private void init() {
        // Perform graceful logout when user presses the window close (X) button.
        // We set DO_NOTHING_ON_CLOSE and handle the event so FormManager.logout()
        // can cleanly dispose the dashboard and open the login window.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(Application.this,
                        "Log out and exit?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    // Attempt server-side logout; ignore errors but try best-effort.
                    try {
                        new AuthAPI().logout();
                    } catch (Throwable ex) {
                        // ignore - we'll still exit
                    }
                    try {
                        dispose();
                    } catch (Throwable ignore) {
                    }
                    System.exit(0);
                }
                // If NO_OPTION, just return and keep the app open
            }
        });
        setSize(UIScale.scale(new Dimension(1366, 768)));
        setLocationRelativeTo(null);
        if (UNDECORATED) {
            setUndecorated(UNDECORATED);
            setBackground(new Color(0, 0, 0, 0));
        } else {
            getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        }
        setContentPane(new Background(UNDECORATED));
        GlassPanePopup.install(this);
        FormManager.install(this, UNDECORATED);

        // Register this window with the maintenance mode manager
        MaintenanceModeManager.getInstance().registerWindow(this);
        // Use current authenticated user (set by Login) when available so the dashboard
        // shows the real user rather than hardcoded values used during development.
        try {
            edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
            if (cu != null) {
                boolean isAdmin = "Admin".equals(cu.getRole());
                System.out.println("Client LOG: Application.init using ClientContext user=" + cu.getUsername());
                FormManager.login(new ModelUser(cu.getUsername(), isAdmin));
            } else {
                // fallback to guest/development user
                System.out.println("Client LOG: Application.init no ClientContext user; using dev user");
                FormManager.login(new ModelUser("Student User", false));
            }
        } catch (Throwable t) {
            System.err.println("Client WARN: Application.init failed to read ClientContext: " + t.getMessage());
            t.printStackTrace(System.err);
            FormManager.login(new ModelUser("Student User", false));
        }
        FormManager.showForm(new DashboardForm());
        // FormManager.logout(); // Disabled for development so dashboard stays open
        // applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("themes");
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Choose theme: FlatMacDarkLaf for dark mode, FlatMacLightLaf for light mode
        FlatMacDarkLaf.setup();  // Change to FlatMacLightLaf.setup() for light mode

        EventQueue.invokeLater(() -> new Application().setVisible(true));
    }
}

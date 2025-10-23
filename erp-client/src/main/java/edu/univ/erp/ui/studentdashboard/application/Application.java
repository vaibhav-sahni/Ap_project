package application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.UIScale;

import components.Background;
import forms.DashboardForm;
import menu.FormManager;
import model.ModelUser;
import raven.popup.GlassPanePopup;

public class Application extends JFrame {

    private final boolean UNDECORATED = !true;

    public Application() {
        init();
    }

    private void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(UIScale.scale(new Dimension(1366, 768)));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        // Simulate a student login to show the complete dashboard UI with profile and menu
        FormManager.login(new ModelUser("Student User", false));
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

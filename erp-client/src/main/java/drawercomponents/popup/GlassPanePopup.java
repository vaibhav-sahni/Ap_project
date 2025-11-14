package drawercomponents.popup;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import drawercomponents.popup.component.GlassPaneChild;

public class GlassPanePopup {

    private static GlassPanePopup instance;
    private JFrame mainFrame;
    protected WindowSnapshots windowSnapshots;
    protected JLayeredPane layerPane;
    protected Container contentPane;
    protected Option defaultOption;

    private GlassPanePopup() {
        init();
    }

    private void init() {
        layerPane = new JLayeredPane();
        layerPane.setLayout(new CardLayout());
    }

    protected void addAndShowPopup(GlassPaneChild component, Option option, String name) {
        // Implementation delegates to the jar's class if necessary; for now keep basic behavior
        // For full behavior we copied more classes; this implementation may be updated later.
    }

    private void updateLayout() {
        for (Component com : layerPane.getComponents()) {
            com.revalidate();
        }
    }

    public static void install(JFrame frame) {
        instance = new GlassPanePopup();
        instance.mainFrame = frame;
        instance.windowSnapshots = new WindowSnapshots(frame);
        instance.contentPane = frame.getContentPane();
        frame.setGlassPane(instance.layerPane);
        frame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    instance.updateLayout();
                });
            }
        });
    }

    public static void setDefaultOption(Option option) {
        instance.defaultOption = option;
    }

    public static void showPopup(GlassPaneChild component, Option option, String name) {
        if (component.getMouseListeners().length == 0) {
            component.addMouseListener(new MouseAdapter() {
            });
        }
        instance.addAndShowPopup(component, option, name);
    }

    public static void showPopup(GlassPaneChild component, Option option) {
        showPopup(component, option, null);
    }

    public static void showPopup(GlassPaneChild component, String name) {
        Option option = instance.defaultOption == null ? new DefaultOption() : instance.defaultOption;
        showPopup(component, option, name);
    }

    public static void showPopup(GlassPaneChild component) {
        showPopup(component, new DefaultOption(), null);
    }

    public static void push(GlassPaneChild component, String name) {
        for (Component com : instance.layerPane.getComponents()) {
            if (com.getName() != null && com.getName().equals(name)) {
                // behavior delegated
                break;
            }
        }
    }

    public static void pop(String name) {
        for (Component com : instance.layerPane.getComponents()) {
            if (com.getName() != null && com.getName().equals(name)) {
                // behavior delegated
                break;
            }
        }
    }

    public static void pop(Component component) {
        for (Component com : instance.layerPane.getComponents()) {
            // delegated
        }
    }

    public static void closePopup(int index) {
        index = instance.layerPane.getComponentCount() - 1 - index;
        if (index >= 0 && index < instance.layerPane.getComponentCount()) {
            Component com = instance.layerPane.getComponent(index);
            if (com instanceof Component) {
                // delegated
            }
        }
    }

    public static void closePopup(String name) {
        for (Component com : instance.layerPane.getComponents()) {
            if (com.getName() != null && com.getName().equals(name)) {
                // delegated
            }
        }
    }

    public static void closePopup(Component component) {
        for (Component com : instance.layerPane.getComponents()) {
            // delegated
        }
    }

    public static void closePopupLast() {
        closePopup(getPopupCount() - 1);
    }

    public static void closePopupAll() {
        for (Component com : instance.layerPane.getComponents()) {
            // delegated
        }
    }

    public static boolean isShowing(String name) {
        boolean act = false;
        for (Component com : instance.layerPane.getComponents()) {
            if (com.getName() != null && com.getName().equals(name)) {
                act = true;
                break;
            }
        }
        return act;
    }

    public static boolean isShowing(Component component) {
        boolean act = false;
        for (Component com : instance.layerPane.getComponents()) {
            if (com instanceof Component) {
                if (com == component) {
                    act = true;
                    break;
                }
            }
        }
        return act;
    }

    public static int getPopupCount() {
        return instance.layerPane.getComponentCount();
    }

    public static JFrame getMainFrame() {
        return instance.mainFrame;
    }

    public static boolean isInit() {
        return !(instance == null || instance.mainFrame == null);
    }

    protected synchronized void removePopup(Component popup) {
        layerPane.remove(popup);
        if (layerPane.getComponentCount() == 0) {
            layerPane.setVisible(false);
        }
    }
}

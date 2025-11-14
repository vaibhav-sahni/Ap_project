package edu.univ.erp.ui.studentdashboard.menu;

import java.awt.Image;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import edu.univ.erp.ui.components.MaintenanceModeManager;
import edu.univ.erp.ui.studentdashboard.components.MainForm;
import edu.univ.erp.ui.studentdashboard.components.SimpleForm;
import edu.univ.erp.ui.studentdashboard.model.ModelUser;
import edu.univ.erp.ui.studentdashboard.swing.slider.PanelSlider;
import edu.univ.erp.ui.studentdashboard.swing.slider.SimpleTransition;
import edu.univ.erp.ui.studentdashboard.utils.UndoRedo;

public class FormManager {

    private static FormManager instance;
    private final JFrame frame;

    private final UndoRedo<SimpleForm> forms = new UndoRedo<>();

    private boolean menuShowing = true;
    private final PanelSlider panelSlider;
    private final MainForm mainForm;
    private final Menu menu;
    private final boolean undecorated;
    // Guard to avoid opening multiple login windows if logout is invoked more than once
    private static volatile boolean logoutInProgress = false;

    public static void install(JFrame frame, boolean undecorated) {
        instance = new FormManager(frame, undecorated);
    }

    private FormManager(JFrame frame, boolean undecorated) {
        this.frame = frame;
        panelSlider = new PanelSlider();
        mainForm = new MainForm(undecorated);
        menu = new Menu(new MyDrawerBuilder());
        this.undecorated = undecorated;
    }

    public static void showMenu() {
        instance.menuShowing = true;
        instance.panelSlider.addSlide(instance.menu, SimpleTransition.getShowMenuTransition(instance.menu.getDrawerBuilder().getDrawerWidth(), instance.undecorated));
    }

    public static void showForm(SimpleForm component) {
        if (isNewFormAble()) {
            instance.forms.add(component);
            if (instance.menuShowing == true) {
                instance.menuShowing = false;
                Image oldImage = instance.panelSlider.createOldImage();
                instance.mainForm.setForm(component);
                instance.panelSlider.addSlide(instance.mainForm, SimpleTransition.getSwitchFormTransition(oldImage, instance.menu.getDrawerBuilder().getDrawerWidth()));
            } else {
                instance.mainForm.showForm(component);
            }
            instance.forms.getCurrent().formInitAndOpen();

            // Trigger maintenance mode notification check on form switch
            MaintenanceModeManager.getInstance().onFormSwitch();
        }
    }

    public static void logout() {
        // Prevent duplicate logout flows (which can open multiple Login windows)
        if (logoutInProgress) return;
        logoutInProgress = true;
        // Suppress session-lost notifier while we perform the UI logout transition
        try { edu.univ.erp.net.ClientSession.setSuppressSessionLost(true); } catch (Throwable ignore) {}
        FlatAnimatedLafChange.showSnapshot();
        try {
            // Dispose the current dashboard window and open a new standalone Login JFrame.
            java.awt.EventQueue.invokeLater(() -> {
                try {
                    instance.frame.dispose();
                } catch (Throwable ignore) {
                }
                try {
                    edu.univ.erp.ui.loginpage.main.LoginManager.showLogin();
                } finally {
                    // reset the flag after the login window is shown so future logouts can occur
                    try { edu.univ.erp.net.ClientSession.setSuppressSessionLost(false); } catch (Throwable ignore) {}
                    logoutInProgress = false;
                }
            });
        } finally {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    public static void login(ModelUser user) {
        FlatAnimatedLafChange.showSnapshot();
        instance.frame.getContentPane().removeAll();
        instance.frame.getContentPane().add(instance.panelSlider);
        // set new user and rebuild menu for user role
        ((MyDrawerBuilder) instance.menu.getDrawerBuilder()).setUser(user);
        instance.frame.repaint();
        instance.frame.revalidate();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    public static void hideMenu() {
        instance.menuShowing = false;
        instance.panelSlider.addSlide(instance.mainForm, SimpleTransition.getHideMenuTransition(instance.menu.getDrawerBuilder().getDrawerWidth(), instance.undecorated));
    }

    public static void undo() {
        if (isNewFormAble()) {
            if (!instance.menuShowing && instance.forms.isUndoAble()) {
                instance.mainForm.showForm(instance.forms.undo(), SimpleTransition.getDefaultTransition(true));
                instance.forms.getCurrent().formOpen();

                // Trigger maintenance mode notification check on form switch
                MaintenanceModeManager.getInstance().onFormSwitch();
            }
        }
    }

    public static void redo() {
        if (isNewFormAble()) {
            if (!instance.menuShowing && instance.forms.isRedoAble()) {
                instance.mainForm.showForm(instance.forms.redo());
                instance.forms.getCurrent().formOpen();

                // Trigger maintenance mode notification check on form switch
                MaintenanceModeManager.getInstance().onFormSwitch();
            }
        }
    }

    public static void refresh() {
        if (!instance.menuShowing) {
            instance.forms.getCurrent().formRefresh();
        }
    }

    /**
     * Refresh student-related views (Dashboard, MyCourses) if they are present
     * in the form stack. This is used by actions like register/drop to ensure
     * gauges and course lists are updated after server-side changes.
     */
    public static void refreshStudentViews() {
        for (SimpleForm f : instance.forms) {
            if (f == null) {
                continue;
            }
            // Compare by class name to avoid import cycles
            String cname = f.getClass().getName();
            if (cname.equals("edu.univ.erp.ui.studentdashboard.forms.DashboardForm") || cname.equals("edu.univ.erp.ui.studentdashboard.forms.MyCoursesForm")) {
                try {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            f.formRefresh();
                        } catch (Throwable t) {
                            // best-effort refresh; swallow errors
                        }
                    });
                } catch (Throwable ignore) {
                }
            }
        }
    }

    public static UndoRedo<SimpleForm> getForms() {
        return instance.forms;
    }

    public static boolean isNewFormAble() {
        return instance.forms.getCurrent() == null || instance.forms.getCurrent().formClose();
    }

    public static void updateTempFormUI() {
        for (SimpleForm f : instance.forms) {
            SwingUtilities.updateComponentTreeUI(f);
        }
    }
}

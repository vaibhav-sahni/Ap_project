package edu.univ.erp.ui.studentdashboard.menu;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;


import edu.univ.erp.ui.studentdashboard.forms.*;
import edu.univ.erp.ui.studentdashboard.model.ModelUser;
import raven.drawer.component.DrawerPanel;
import raven.drawer.component.SimpleDrawerBuilder;
import raven.drawer.component.footer.SimpleFooterData;
import raven.drawer.component.header.SimpleHeaderData;
import raven.drawer.component.header.SimpleHeaderStyle;
import raven.drawer.component.menu.MenuAction;
import raven.drawer.component.menu.MenuEvent;
import raven.drawer.component.menu.MenuValidation;
import raven.drawer.component.menu.SimpleMenuOption;
import raven.drawer.component.menu.SimpleMenuStyle;
import raven.drawer.component.menu.data.Item;
import raven.drawer.component.menu.data.MenuItem;
import raven.swing.AvatarIcon;
public class MyDrawerBuilder extends SimpleDrawerBuilder {

    private ModelUser user;
    private final ThemesChange themesChange;

    public void setUser(ModelUser user) {
        this.user = user;
        SimpleHeaderData headerData = header.getSimpleHeaderData();
        headerData.setTitle(user.getUserName());
        header.setSimpleHeaderData(headerData);
        rebuildMenu();
    }

    public MyDrawerBuilder() {
        themesChange = new ThemesChange();
    }

    @Override
    public Component getFooter() {
        return themesChange;
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        AvatarIcon icon = new AvatarIcon(getClass().getResource("/image/profile.png"), 60, 60, 999);
        icon.setBorder(2);
        // Use the authenticated user's name when available so the header isn't hardcoded
        String title = "Pramag Basantia";
        String desc = "pramag24421@iiitd.ac.in";
        try {
            edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
            if (cu != null) {
                String uname = cu.getUsername();
                if (uname != null && !uname.isEmpty()) {
                    title = uname;
                    desc = uname + "@univ.edu";
                }
            }
        } catch (Throwable ignore) {
        }
        return new SimpleHeaderData()
                .setTitle(title)
                .setDescription(desc)
                .setHeaderStyle(new SimpleHeaderStyle() {

                    @Override
                    public void styleTitle(JLabel label) {
                        label.putClientProperty(FlatClientProperties.STYLE, ""
                                + "[light]foreground:#FAFAFA");
                    }

                    @Override
                    public void styleDescription(JLabel label) {
                        label.putClientProperty(FlatClientProperties.STYLE, ""
                                + "[light]foreground:#E1E1E1");
                    }
                });
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData();
    }

    @Override
    public SimpleMenuOption getSimpleMenuOption() {

        MenuItem items[] = new MenuItem[]{
            new Item.Label("MAIN"),
            new Item("Dashboard", "dashboard.svg"),
            new Item.Label("STUDENT PORTAL"),
            new Item("Course Catalog", "book.svg")
            .subMenu("  All Courses")
            .subMenu("  My Registered Courses"),
            new Item("My Timetable", "schedule.svg"),
            new Item("My Grades", "school.svg"),
            new Item("Notification", "notification.svg"),
            new Item.Label("ACCOUNT"),
            new Item("Logout", "logout.svg")
        };

        SimpleMenuOption simpleMenuOption = new SimpleMenuOption() {
            @Override
            public Icon buildMenuIcon(String path, float scale) {
                FlatSVGIcon icon = new FlatSVGIcon(path, scale);
                FlatSVGIcon.ColorFilter colorFilter = new FlatSVGIcon.ColorFilter();
                colorFilter.add(Color.decode("#969696"), Color.decode("#FAFAFA"), Color.decode("#969696"));
                icon.setColorFilter(colorFilter);
                return icon;
            }
        };

        // Build a parallel array of labels for the top-level items so we can detect selections
        // without depending on unstable numeric indices. Use reflection to try common getter names.
        final String[] itemLabels = new String[items.length];
        java.util.List<String> actionable = new java.util.ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            MenuItem mi = items[i];
            String label = null;
            try {

                java.lang.reflect.Method m = null;
                for (String name : new String[]{"getText", "getName", "getTitle", "getLabel"}) {
                    try {
                        m = mi.getClass().getMethod(name);
                        Object o = m.invoke(mi);
                        if (o != null) {
                            label = o.toString();
                            break;
                        }
                    } catch (NoSuchMethodException ns) {
                        // try next
                    }
                }
            } catch (Throwable ignore) {
            }
            if (label == null) label = mi.toString();
            itemLabels[i] = label == null ? "" : label;

            // If this item is not a section label (Item.Label) then include it in actionable list
            try {
                if (!(mi instanceof Item.Label)) {
                    actionable.add(itemLabels[i]);
                }
            } catch (Throwable ignore) {
                // fallback: include
                actionable.add(itemLabels[i]);
            }
        }

        final String[] actionableLabels = actionable.toArray(new String[0]);

        // (debug mapping removed)

        simpleMenuOption.setMenuValidation(new MenuValidation() {

            private boolean checkMenu(int[] index, int[] indexHide) {
                if (index.length == indexHide.length) {
                    for (int i = 0; i < index.length; i++) {
                        if (index[i] != indexHide[i]) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }

            @Override
            public boolean menuValidation(int[] index) {
                if (user == null) {
                    return false;
                }
                if (!user.isAdmin()) {
                    
                    boolean act
                            // `Email`->`Gropu Read`->`Read 3`
                            = checkMenu(index, new int[]{1, 2, 3})
                            // `Email`->`Gropu Read`->`Read 5`
                            && checkMenu(index, new int[]{1, 2, 5})
                            // `Email`->`Group Read`->`Group Item->`Item 4`
                            && checkMenu(index, new int[]{1, 2, 2, 3})
                            // `Advanced UI`->`Owl Carousel`
                            && checkMenu(index, new int[]{4, 1})
                            // `Special Pages`
                            && checkMenu(index, new int[]{8});
                    return act;
                }
                return true;
            }
        });

        simpleMenuOption.setMenuStyle(new SimpleMenuStyle() {
            @Override
            public void styleMenuItem(JButton menu, int[] index) {
                menu.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]foreground:#FAFAFA;"
                        + "arc:10");
            }

            @Override
            public void styleMenu(JComponent component) {
                component.putClientProperty(FlatClientProperties.STYLE, ""
                        + "background:$Drawer.background");
            }

            @Override
            public void styleLabel(JLabel label) {
                label.putClientProperty(FlatClientProperties.STYLE, ""
                        + "[light]foreground:darken(#FAFAFA,15%);"
                        + "[dark]foreground:darken($Label.foreground,30%)");
            }
        });
        simpleMenuOption.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                // selection debug removed

                // Try to detect the label for the top-level item. The drawer implementation
                // often only counts actionable items (skips section labels), so prefer
                // actionableLabels (which excludes Item.Label entries) when mapping the
                // runtime top index to a label.
                String detectedLabel = null;
                if (index != null && index.length > 0) {
                    int top = index[0];
                    if (top >= 0 && top < actionableLabels.length) {
                        detectedLabel = actionableLabels[top];
                    } else if (top >= 0 && top < itemLabels.length) {
                        // fallback to raw top-level mapping
                        detectedLabel = itemLabels[top];
                    }
                } else {
                    // Fallback: inspect action string
                    try {
                        if (action != null && action.toString() != null) {
                            String s = action.toString();
                            if (s.toLowerCase().contains("logout")) detectedLabel = "Logout";
                        }
                    } catch (Throwable ignore) {}
                }

                // detection debug removed

                // Handle Logout explicitly
                if (detectedLabel != null && detectedLabel.equalsIgnoreCase("Logout")) {
                    try {
                        edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                        boolean didLogout = false;
                        if (cu != null) {
                            edu.univ.erp.ui.handlers.AuthUiHandlers authHandlers = new edu.univ.erp.ui.handlers.AuthUiHandlers(cu);
                            didLogout = authHandlers.confirmAndLogout();
                        } else {
                            int choice = javax.swing.JOptionPane.showConfirmDialog(null, "Do you want to logout?", "Logout", javax.swing.JOptionPane.YES_NO_OPTION);
                            if (choice == javax.swing.JOptionPane.YES_OPTION) {
                                try {
                                    new edu.univ.erp.api.auth.AuthAPI().logout();
                                    didLogout = true;
                                } catch (Exception ex) {
                                    javax.swing.JOptionPane.showMessageDialog(null, "Logout failed: " + ex.getMessage(), "Logout Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                        if (didLogout) {
                            FormManager.logout();
                        }
                    } catch (Throwable t) {
                        System.err.println("CLIENT WARN: logout via drawer failed: " + t.getMessage());
                        t.printStackTrace(System.err);
                        FormManager.logout();
                    }
                    return;
                }

                // Open full timetable in main area when My Timetable is selected
                if (detectedLabel != null && detectedLabel.equalsIgnoreCase("My Timetable")) {
                    FormManager.showForm(new TimetableForm());
                    return;
                }

                // Open My Grades when selected
                if (detectedLabel != null && detectedLabel.equalsIgnoreCase("My Grades")) {
                    FormManager.showForm(new MyGradesForm());
                    return;
                }

                // Open Notifications when selected
                if (detectedLabel != null && detectedLabel.equalsIgnoreCase("Notification")) {
                    FormManager.showForm(new NotificationForm());
                    return;
                }

                // Top-level Dashboard
                if (index != null && index.length == 1 && index[0] == 0) {
                    FormManager.showForm(new DashboardForm());
                    return;
                }

                // Two-level menu items (submenus)
                if (index != null && index.length == 2) {
                    // Use top-level actionable label to decide submenu actions (more robust than hardcoded indices)
                    String topLabel = null;
                    if (index[0] >= 0 && index[0] < actionableLabels.length) {
                        topLabel = actionableLabels[index[0]];
                    }

                    if (topLabel != null && topLabel.equalsIgnoreCase("Course Catalog")) {
                        // "All Courses" selected
                        if (index[1] == 0) {
                            FormManager.showForm(new RegisterCoursesForm());
                            return;
                        }
                        // "My Registered Courses" selected - open MyCoursesForm
                        if (index[1] == 1) {
                            FormManager.showForm(new MyCoursesForm());
                            return;
                        }
                    }

                    // legacy email mapping (kept for compatibility if menu structure differs)
                    if (index[0] == 1) {
                        if (index[1] == 0) {
                            FormManager.showForm(new InboxForm());
                        } else if (index[1] == 1) {
                            FormManager.showForm(new ReadForm());
                        }
                    }
                }
            }
        });

        simpleMenuOption.setMenus(items).setBaseIconPath("menu").setIconScale(0.6f);
        return simpleMenuOption;
    }

    @Override

    public void build(DrawerPanel drawerPanel) {
        drawerPanel.putClientProperty(FlatClientProperties.STYLE, "" + "background:$Drawer.background");
    }

    @Override
    public int getDrawerWidth() {
        return 270;
    }
}

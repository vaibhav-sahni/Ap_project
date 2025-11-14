package drawercomponents.drawer.component.menu;

import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import drawercomponents.drawer.component.menu.data.Item;
import drawercomponents.drawer.component.menu.data.MenuItem;
import drawercomponents.utils.FlatLafStyleUtils;

public class SimpleMenu extends JPanel {

    private final SimpleMenuOption simpleMenuOption;

    public SimpleMenu(SimpleMenuOption simpleMenuOption) {
        this.simpleMenuOption = simpleMenuOption;
        init();
    }

    public void rebuildMenu() {
        removeAll();
        buildMenu();
    }

    private void init() {
        setLayout(new MenuLayout());
        if (simpleMenuOption.simpleMenuStyle != null) {
            simpleMenuOption.simpleMenuStyle.styleMenu(this);
        }
        buildMenu();
    }

    // buildMenu and other methods omitted for brevity; the original implementation is large.
    private void buildMenu() {
        MenuItem[] menus = simpleMenuOption.menus;
        if (menus == null) {
            return;
        }
        int index = 0;
        for (MenuItem menuItem : menus) {
            if (menuItem.isMenu()) {
                Item item = (Item) menuItem;
                JButton button = createMenuItem(item.getName(), item.getIcon(), new int[]{index}, 0);
                applyMenuEvent(button, new int[]{index});
                add(button);
                index++;
            } else {
                // label
                if (menuItem instanceof Item.Label) {
                    JLabel label = new JLabel(((Item.Label) menuItem).getName());
                    if (simpleMenuOption.simpleMenuStyle != null) {
                        simpleMenuOption.simpleMenuStyle.styleLabel(label);
                    }
                    add(label);
                }
            }
        }
    }

    private int[] copyArray(int[] arr) {
        return Arrays.copyOf(arr, arr.length);
    }

    private String getBasePath() {
        if (simpleMenuOption.baseIconPath == null) {
            return "";
        }
        if (simpleMenuOption.baseIconPath.endsWith("/")) {
            return simpleMenuOption.baseIconPath;
        } else {
            return simpleMenuOption.baseIconPath + "/";
        }
    }

    protected Icon getIcon(String icon, int menuLevel) {
        if (icon != null) {
            String path = getBasePath();
            float iconScale = simpleMenuOption.iconScale.length > 0 ? simpleMenuOption.iconScale[0] : 1f;
            Icon iconObject = simpleMenuOption.buildMenuIcon(path + icon, iconScale);
            return iconObject;
        } else {
            return null;
        }
    }

    protected JButton createMenuItem(String name, String icon, int[] index, int menuLevel) {
        JButton button = new JButton(name);
        Icon iconObject = getIcon(icon, menuLevel);
        if (iconObject != null) {
            button.setIcon(iconObject);
        }
        button.setHorizontalAlignment(JButton.LEADING);
        if (simpleMenuOption.simpleMenuStyle != null) {
            simpleMenuOption.simpleMenuStyle.styleMenuItem(button, copyArray(index));
        }
        FlatLafStyleUtils.appendStyleIfAbsent(button, "" +
                "arc:0;" +
                "margin:6,20,6,20;" +
                "borderWidth:0;" +
                "focusWidth:0;" +
                "innerFocusWidth:0;" +
                "background:null;" +
                "iconTextGap:5;");
        return button;
    }

    protected void applyMenuEvent(JButton button, int[] index) {
        button.addActionListener(e -> {
            MenuAction action = runEvent(index);
            if (action != null) {
                // placeholder for future action handling
            }
        });
    }

    private MenuAction runEvent(int[] index) {
        if (!simpleMenuOption.events.isEmpty()) {
            MenuAction action = new MenuAction();
            if (simpleMenuOption.menuItemAutoSelect) {
                action.selected();
            }
            for (MenuEvent event : simpleMenuOption.events) {
                event.selected(action, copyArray(index));
            }
            return action;
        }
        return null;
    }
}

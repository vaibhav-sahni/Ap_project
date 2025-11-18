package drawercomponents.drawer.component.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import drawercomponents.drawer.component.menu.data.Item;
import drawercomponents.drawer.component.menu.data.MenuItem;
import drawercomponents.utils.FlatLafStyleUtils;
import java.beans.PropertyChangeListener;
import com.formdev.flatlaf.FlatClientProperties;

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
                // create top-level button
                JButton button = createMenuItem(item.getName(), item.getIcon(), new int[]{index}, 0);
                add(button);

                // if this item has submenus, create them and keep them hidden initially
                if (item.isSubmenuAble() && item.getSubMenu() != null) {
                    java.util.List<Item> subs = item.getSubMenu();
                        java.util.List<SlidePanel> createdSubs = new java.util.ArrayList<>();
                    for (int s = 0; s < subs.size(); s++) {
                        Item sub = subs.get(s);
                        // sub-menu index: [topIndex, subIndex]
                        final int subIdx = s;
                        final int topIdx = index;
                        int[] subIndex = new int[]{topIdx, subIdx};
                        // create sub-button with extra left padding
                        JButton subBtn = createMenuItem(sub.getName(), sub.getIcon(), subIndex, 1);
                        // apply same menu event mapping so listeners receive the two-element index
                        applyMenuEvent(subBtn, subIndex);
                        // visually indent subitems by increasing left margin
                        FlatLafStyleUtils.appendStyleIfAbsent(subBtn, "margin:6,40,6,20; arc:0; background:null; iconTextGap:5;");
                        // ensure submenus explicitly use the menu background (override any background:null)
                        try {
                            String oldStyle = FlatClientProperties.clientProperty(subBtn, FlatClientProperties.STYLE, null, String.class);
                            String merged = FlatLafStyleUtils.appendStyle(oldStyle, "background:$Drawer.background");
                            subBtn.putClientProperty(FlatClientProperties.STYLE, merged);
                        } catch (Throwable ignore) {
                        }
                        subBtn.putClientProperty("isSubmenuItem", Boolean.TRUE);
                        // wrap submenu button in a SlidePanel so we can animate open/close
                        SlidePanel sp = new SlidePanel(subBtn);
                        sp.setOpen(false);
                        add(sp);
                        createdSubs.add(sp);
                    }

                    // toggle open/close of the created submenu SlidePanels when parent is clicked
                    button.addActionListener(ev -> {
                        boolean anyOpen = false;
                        for (SlidePanel p : createdSubs) {
                            if (p.isOpen()) {
                                anyOpen = true;
                                break;
                            }
                        }
                        for (SlidePanel p : createdSubs) {
                            p.setOpen(!anyOpen);
                        }
                    });
                } else {
                    // no submenus: clicking should trigger menu event normally
                    applyMenuEvent(button, new int[]{index});
                }

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

    // Minimal slide panel to animate submenu open/close by changing preferred height
    private class SlidePanel extends JPanel {
        private final JComponent content;
        private final int targetHeight;
        private int currentHeight;
        private boolean open;
        private Timer timer;

        SlidePanel(JComponent content) {
            super(new BorderLayout());
            this.content = content;
            setOpaque(false);

            // estimate content preferred height first
            Dimension contentPref = content.getPreferredSize();
            int estimatedHeight = (contentPref != null && contentPref.height > 0) ? contentPref.height : 24;

            // if this is marked as a submenu item, wrap it with an accent bar on the left
            boolean isSub = Boolean.TRUE.equals(content.getClientProperty("isSubmenuItem"));
            JComponent shown = content;
            if (isSub) {
                JPanel inner = new JPanel(new BorderLayout());
                inner.setOpaque(false);
                JPanel accent = new JPanel();
                accent.setOpaque(true);
                // match the menu background color
                accent.setBackground(SimpleMenu.this.getBackground());
                accent.setPreferredSize(new Dimension(6, estimatedHeight));
                inner.add(accent, BorderLayout.WEST);
                // allow the button to keep its own background so FlatLaf
                // can paint it consistently with top-level menu items
                content.setOpaque(true);
                content.setBackground(SimpleMenu.this.getBackground());
                inner.add(content, BorderLayout.CENTER);
                shown = inner;

                // keep submenu background in sync when the parent menu's background changes (theme switch)
                SimpleMenu.this.addPropertyChangeListener("background", evt -> {
                    java.awt.Color bg = SimpleMenu.this.getBackground();
                    accent.setBackground(bg);
                    content.setBackground(bg);
                    revalidate();
                    repaint();
                });

                // Ensure initial sync occurs after FlatLaf has had a chance to apply styles.
                EventQueue.invokeLater(() -> {
                    java.awt.Color bg = SimpleMenu.this.getBackground();
                    accent.setBackground(bg);
                    content.setBackground(bg);
                    revalidate();
                    repaint();
                });
            } else {
                content.setOpaque(true);
                content.setBackground(SimpleMenu.this.getBackground());
            }

            add(shown, BorderLayout.CENTER);
            Dimension pref = shown.getPreferredSize();
            this.targetHeight = (pref != null && pref.height > 0) ? pref.height : estimatedHeight;
            this.currentHeight = 0;
            setPreferredSize(new Dimension(pref != null ? pref.width : 0, 0));
        }

        boolean isOpen() {
            return open;
        }

        void setOpen(boolean open) {
            if (this.open == open) {
                return;
            }
            this.open = open;
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            final int step = Math.max(1, targetHeight / 8);
            timer = new Timer(12, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (SlidePanel.this.open) {
                        currentHeight = Math.min(targetHeight, currentHeight + step);
                    } else {
                        currentHeight = Math.max(0, currentHeight - step);
                    }
                    Dimension d = getPreferredSize();
                    int width = d != null ? d.width : 0;
                    setPreferredSize(new Dimension(width, currentHeight));
                    revalidate();
                    repaint();
                    if (currentHeight == (SlidePanel.this.open ? targetHeight : 0)) {
                        timer.stop();
                    }
                }
            });
            timer.start();
        }
    }
}

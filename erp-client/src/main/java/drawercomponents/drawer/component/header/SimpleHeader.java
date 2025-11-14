package drawercomponents.drawer.component.header;

import javax.swing.JLabel;
import javax.swing.JPanel;

import drawercomponents.utils.FlatLafStyleUtils;
import net.miginfocom.swing.MigLayout;

public class SimpleHeader extends JPanel {

    private SimpleHeaderData simpleHeaderData;

    public SimpleHeader(SimpleHeaderData simpleHeaderData) {
        this.simpleHeaderData = simpleHeaderData;
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,insets 10 20 5 20,fill,gap 3"));
        JLabel profile = new JLabel(simpleHeaderData.icon);
        JLabel labelTitle = new JLabel(simpleHeaderData.title);
        JLabel labelDescription = new JLabel(simpleHeaderData.description);

        if (simpleHeaderData.simpleHeaderStyle != null) {
            simpleHeaderData.simpleHeaderStyle.styleHeader(this);
            simpleHeaderData.simpleHeaderStyle.styleProfile(profile);
            simpleHeaderData.simpleHeaderStyle.styleTitle(labelTitle);
            simpleHeaderData.simpleHeaderStyle.styleDescription(labelDescription);
        }

        FlatLafStyleUtils.appendStyleIfAbsent(this, "" +
                "background:null");

        FlatLafStyleUtils.appendStyleIfAbsent(profile, "" +
                "background:$Component.borderColor");
        FlatLafStyleUtils.appendStyleIfAbsent(labelDescription, "" +
                "font:-1;" +
                "[light]foreground:lighten(@foreground,30%);" +
                "[dark]foreground:darken(@foreground,30%)");
        add(profile);
        add(labelTitle);
        add(labelDescription);
    }

    public SimpleHeaderData getSimpleHeaderData() {
        return simpleHeaderData;
    }

    public void setSimpleHeaderData(SimpleHeaderData simpleHeaderData) {
        this.simpleHeaderData = simpleHeaderData;
        // update UI components if needed
    }
}

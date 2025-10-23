package edu.univ.erp.ui.studentdashboard.components;

import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

public class SimpleForm extends JPanel {

    public SimpleForm() {
        init();
    }

    private void init() {
        putClientProperty(FlatClientProperties.STYLE, "" + "border:5,5,5,5;" + "background:null");
    }

    public void formInitAndOpen() {

    }

    public void formOpen() {

    }

    public void formRefresh() {

    }

    public boolean formClose() {
        return true;
    }
}

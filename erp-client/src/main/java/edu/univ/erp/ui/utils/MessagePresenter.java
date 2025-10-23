package edu.univ.erp.ui.utils;

import java.awt.Component;

import javax.swing.JOptionPane;

public final class MessagePresenter {

    private MessagePresenter() {}

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message == null ? "An error occurred" : message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message == null ? "" : message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}

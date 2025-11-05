package edu.univ.erp.ui.handlers;

import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.domain.UserAuth;

/**
 * Small handler for auth-related UI operations (logout confirmation flow).
 */
public class AuthUiHandlers {

    private final AuthAPI authApi = new AuthAPI();
    private final UserAuth user;

    public AuthUiHandlers(UserAuth user) {
        this.user = user;
    }

    public boolean confirmAndLogout() throws Exception {
        int choice = javax.swing.JOptionPane.showConfirmDialog(null, "Do you want to logout?", "Logout", javax.swing.JOptionPane.YES_NO_OPTION);
        if (choice != javax.swing.JOptionPane.YES_OPTION) return false;

        try {
            String resp = authApi.logout();
            javax.swing.JOptionPane.showMessageDialog(null, resp, "Logged out", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Logout failed: " + e.getMessage(), "Logout Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    public void changePasswordWithUi() {
        // Show a styled modal dialog with three password fields (Old, New, Confirm)
        try {
            javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "Change Password", true);
            javax.swing.JPanel content = new javax.swing.JPanel(new net.miginfocom.swing.MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

            content.add(new javax.swing.JLabel("Current Password:"), "gaptop 5");
            javax.swing.JPasswordField txtOld = new javax.swing.JPasswordField();
            txtOld.setPreferredSize(new java.awt.Dimension(200, 30));
            content.add(txtOld, "gapbottom 10");

            content.add(new javax.swing.JLabel("New Password:"));
            javax.swing.JPasswordField txtNew = new javax.swing.JPasswordField();
            txtNew.setPreferredSize(new java.awt.Dimension(200, 30));
            content.add(txtNew, "gapbottom 10");

            content.add(new javax.swing.JLabel("Confirm New Password:"));
            javax.swing.JPasswordField txtConfirm = new javax.swing.JPasswordField();
            txtConfirm.setPreferredSize(new java.awt.Dimension(200, 30));
            content.add(txtConfirm, "gapbottom 15");

            javax.swing.JPanel buttons = new javax.swing.JPanel(new net.miginfocom.swing.MigLayout("align center", "[120!][120!]") );
            javax.swing.JButton btnCancel = new javax.swing.JButton("Cancel");
            javax.swing.JButton btnChange = new javax.swing.JButton("Change");
            btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
            btnChange.setPreferredSize(new java.awt.Dimension(120, 35));
            buttons.add(btnCancel, "gap 10");
            buttons.add(btnChange);
            content.add(buttons, "span, growx, gaptop 10");

            btnCancel.addActionListener(a -> dialog.dispose());
            btnChange.addActionListener(a -> {
                try {
                    String oldPass = new String(txtOld.getPassword());
                    String newPass = new String(txtNew.getPassword());
                    String confirm = new String(txtConfirm.getPassword());
                    if (oldPass.trim().isEmpty() || newPass.trim().isEmpty() || confirm.trim().isEmpty()) {
                        javax.swing.JOptionPane.showMessageDialog(dialog, "All fields are required.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        javax.swing.JOptionPane.showMessageDialog(dialog, "New password and confirmation do not match.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    new AuthAPI().changePassword(user.getUserId(), oldPass, newPass);
                    dialog.dispose();
                    javax.swing.JOptionPane.showMessageDialog(null, "Password changed successfully.", "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    javax.swing.JOptionPane.showMessageDialog(dialog, e.getMessage(), "Password Change Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            });

            dialog.setContentPane(content);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Password Change Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}

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
        try {
            String oldPass = javax.swing.JOptionPane.showInputDialog(null, "Enter Current Password:");
            if (oldPass == null) return;

            String newPass = javax.swing.JOptionPane.showInputDialog(null, "Enter New Password:");
            if (newPass == null) return;

            String confirmPass = javax.swing.JOptionPane.showInputDialog(null, "Confirm New Password:");
            if (confirmPass == null) return;

            if (!newPass.equals(confirmPass)) {
                javax.swing.JOptionPane.showMessageDialog(null, "New password and confirmation do not match.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            new AuthAPI().changePassword(user.getUserId(), oldPass, newPass);
            javax.swing.JOptionPane.showMessageDialog(null, "Password changed successfully.", "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Password Change Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}

package edu.univ.erp.ui.loginpage.main;

import java.awt.Color;

import javax.swing.JOptionPane;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;
import edu.univ.erp.ui.preview.AdminDashboardFrame;
import edu.univ.erp.ui.preview.InstructorDashboardFrame;
import edu.univ.erp.ui.preview.StudentDashboardFrame;

public class Login extends javax.swing.JFrame {
    edu.univ.erp.api.auth.AuthAPI authApi = new edu.univ.erp.api.auth.AuthAPI();
    
    public Login() {
        setUndecorated(true);   
        initComponents();

        setBackground(new Color(0, 0, 0, 0));
    }
    
    private void initComponents() {

        background = new edu.univ.erp.ui.loginpage.login.Background();
        panel = new javax.swing.JPanel();
        txtUser = new edu.univ.erp.ui.loginpage.swing.TextField();
        txtPassword = new edu.univ.erp.ui.loginpage.swing.PasswordField();
        jLabel1 = new javax.swing.JLabel();
        cmdLogin = new edu.univ.erp.ui.loginpage.swing.Button();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);

        background.setBlur(panel);

        panel.setOpaque(false);

        txtUser.setHint("User Name");

        txtPassword.setHint("Password");

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("SIGN IN");

        cmdLogin.setForeground(new java.awt.Color(231, 231, 231));
        cmdLogin.setText("SIGN IN");
        // use lambda to avoid anonymous inner class lint warnings
        cmdLogin.addActionListener(evt -> cmdLoginActionPerformed());

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addGap(60, 60, 60)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmdLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(70, Short.MAX_VALUE))
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addGap(70, 70, 70)
                .addComponent(jLabel1)
                .addGap(30, 30, 30)
                .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(cmdLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(70, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundLayout = new javax.swing.GroupLayout(background);
        background.setLayout(backgroundLayout);
        backgroundLayout.setHorizontalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addContainerGap(311, Short.MAX_VALUE)
                .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(311, Short.MAX_VALUE))
        );
        backgroundLayout.setVerticalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addContainerGap(136, Short.MAX_VALUE)
                .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(137, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }

    private void cmdLoginActionPerformed() {//LOGIN BUTTON
        String username = txtUser.getText();
        String password = String.valueOf(txtPassword.getPassword());
        try {

            UserAuth user = authApi.login(username, password);

            if (user == null) {
                JOptionPane.showMessageDialog(this, "Login failed: no user returned", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            System.out.println("Client LOG: Login SUCCESS. User Role: " + user.getRole());

            this.dispose();

            // Open role-specific dashboard windows (preview frames)
            String role = user.getRole();
            switch (role) {
                case "Student" -> {
                    StudentDashboardFrame f = new StudentDashboardFrame(user);
                    DashboardController controller = new DashboardController(user, f);
                    f.setController(controller);
                    f.setVisible(true);
                }
                case "Instructor" -> {
                    InstructorDashboardFrame f = new InstructorDashboardFrame(user);
                    DashboardController controller = new DashboardController(user, f);
                    f.setController(controller);
                    f.setVisible(true);
                }
                case "Admin" -> {
                    AdminDashboardFrame f = new AdminDashboardFrame(user);
                    DashboardController controller = new DashboardController(user, f);
                    f.setController(controller);
                    f.setVisible(true);
                }
                default -> {
                    // Fallback to the original DashboardMainPanel embedded frame for unknown roles
                    javax.swing.JFrame dashboardFrame = new javax.swing.JFrame("ERP Dashboard");
                    dashboardFrame.setSize(1000, 700);
                    dashboardFrame.setLocationRelativeTo(null);
                    dashboardFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                    DashboardController controller = new DashboardController(user, dashboardFrame);
                    edu.univ.erp.ui.preview.DashboardMainPanel mainPanel = new edu.univ.erp.ui.preview.DashboardMainPanel(user, controller);
                    dashboardFrame.getContentPane().add(mainPanel);
                    dashboardFrame.setVisible(true);
                }
            }

        } catch (Exception ex) {
            // 3. FAILURE HANDLING (Catches Network error OR Server-side Auth failure message)

            // Display the specific error message relayed from the Server/API
            String errorMessage = "Login Failed: " + ex.getMessage();
            System.err.println("Client ERROR: " + errorMessage);

            // Show the error to the user using a standard Swing dialog
            JOptionPane.showMessageDialog(this,
                                           ex.getMessage(), // Show only the relayed error message
                                           "Login Failed",
                                           JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
    private edu.univ.erp.ui.loginpage.login.Background background;
    private edu.univ.erp.ui.loginpage.swing.Button cmdLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panel;
    private edu.univ.erp.ui.loginpage.swing.PasswordField txtPassword;
    private edu.univ.erp.ui.loginpage.swing.TextField txtUser;
}

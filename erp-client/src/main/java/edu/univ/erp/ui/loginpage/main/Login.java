package edu.univ.erp.ui.loginpage.main;

import java.awt.Color;

import javax.swing.JOptionPane;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.admindashboard.application.Application;

public class Login extends javax.swing.JFrame {

    edu.univ.erp.api.auth.AuthAPI authApi = new edu.univ.erp.api.auth.AuthAPI();
    private final boolean UNDECORATED = !true; // Same as student dashboard

    // Panel state management
    private boolean isPasswordResetMode;

    public Login() {
        initComponents();
    }

    private void initComponents() {

        background = new edu.univ.erp.ui.loginpage.login.Background();
        panel = new javax.swing.JPanel();

        // Login form components
        txtUser = new edu.univ.erp.ui.loginpage.swing.TextField();
        txtPassword = new edu.univ.erp.ui.loginpage.swing.PasswordField();
        jLabel1 = new javax.swing.JLabel();
        cmdLogin = new edu.univ.erp.ui.loginpage.swing.Button();
        lblForgotPassword = new javax.swing.JLabel();

        // Password reset form components
        txtResetUser = new edu.univ.erp.ui.loginpage.swing.TextField();
        txtNewPassword = new edu.univ.erp.ui.loginpage.swing.PasswordField();
        txtConfirmPassword = new edu.univ.erp.ui.loginpage.swing.PasswordField();
        cmdChangePassword = new edu.univ.erp.ui.loginpage.swing.Button();
        lblSignIn = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        // Copy from student dashboard - use FlatLaf window controls with animations
        if (UNDECORATED) {
            setUndecorated(UNDECORATED);
            setBackground(new Color(0, 0, 0, 0));
        } else {
            getRootPane().putClientProperty(com.formdev.flatlaf.FlatClientProperties.FULL_WINDOW_CONTENT, true);
        }

        background.setBlur(panel);

        panel.setOpaque(false);

        txtUser.setHint("User Name");

        txtPassword.setHint("Password");

        // Configure password reset components
        txtResetUser.setHint("User Name");
        txtNewPassword.setHint("New Password");
        txtConfirmPassword.setHint("Re-enter New Password");

        cmdChangePassword.setForeground(new java.awt.Color(231, 231, 231));
        cmdChangePassword.setText("CHANGE PASSWORD");
        cmdChangePassword.addActionListener(evt -> cmdChangePasswordActionPerformed());

        // Configure sign in link
        lblSignIn.setFont(new java.awt.Font("sansserif", 0, 12));
        lblSignIn.setForeground(new java.awt.Color(200, 200, 200));
        lblSignIn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignIn.setText("<html><u>Sign In</u></html>");
        lblSignIn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblSignIn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblSignInMouseClicked();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lblSignIn.setForeground(new java.awt.Color(255, 255, 255));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lblSignIn.setForeground(new java.awt.Color(200, 200, 200));
            }
        });

        // Initially hide password reset components
        txtResetUser.setVisible(false);
        txtNewPassword.setVisible(false);
        txtConfirmPassword.setVisible(false);
        cmdChangePassword.setVisible(false);
        lblSignIn.setVisible(false);

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("SIGN IN");

        cmdLogin.setForeground(new java.awt.Color(231, 231, 231));
        cmdLogin.setText("SIGN IN");
        // use lambda to avoid anonymous inner class lint warnings
        cmdLogin.addActionListener(evt -> cmdLoginActionPerformed());

        // Configure forgot password link
        lblForgotPassword.setFont(new java.awt.Font("sansserif", 0, 12));
        lblForgotPassword.setForeground(new java.awt.Color(200, 200, 200));
        lblForgotPassword.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblForgotPassword.setText("<html><u>Forgot Password?</u></html>");
        lblForgotPassword.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblForgotPassword.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblForgotPasswordMouseClicked();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lblForgotPassword.setForeground(new java.awt.Color(255, 255, 255));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lblForgotPassword.setForeground(new java.awt.Color(200, 200, 200));
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGap(60, 60, 60)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        // Login form components
                                        .addComponent(txtUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cmdLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblForgotPassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        // Password reset form components (same alignment)
                                        .addComponent(txtResetUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtNewPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                        .addComponent(txtConfirmPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                        .addComponent(cmdChangePassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblSignIn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(70, Short.MAX_VALUE))
        );
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        // Login form group
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGap(70, 70, 70)
                                .addComponent(jLabel1)
                                .addGap(30, 30, 30)
                                .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(cmdLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(lblForgotPassword)
                                .addContainerGap(55, Short.MAX_VALUE))
                        // Password reset form group (same vertical positioning)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGap(70, 70, 70)
                                .addGap(54, 54, 54) // Space where title would be (24px font + 30px gap)
                                .addComponent(txtResetUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtNewPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtConfirmPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(cmdChangePassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(lblSignIn)
                                .addContainerGap(37, Short.MAX_VALUE))
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

        setSize(1366, 768);
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

            

            // Persist current user for in-process components (dashboard) BEFORE disposing
            try {
                edu.univ.erp.ClientContext.setCurrentUser(user);
                
            } catch (Throwable ignore) {
            }

            this.dispose();

            // Open role-specific dashboard windows (preview frames)
            String role = user.getRole();
            switch (role) {
                case "Student" -> {
                    // Launch the new student dashboard reflectively so we avoid compile-time deps.
                    java.awt.EventQueue.invokeLater(() -> {
                        try {
                            // 1) Try to instantiate Application and call setVisible(true)
                            try {
                                Class<?> appClass = Class.forName("edu.univ.erp.ui.studentdashboard.application.Application");
                                Object app = appClass.getDeclaredConstructor().newInstance();
                                try {
                                    java.lang.reflect.Method setVisible = appClass.getMethod("setVisible", boolean.class);
                                    setVisible.invoke(app, true);
                                } catch (NoSuchMethodException ignored) {
                                }
                            } catch (ClassNotFoundException ignored) {
                                // Application class not present; dashboard not available
                            }

                            // 2) Build a ModelUser instance reflectively (try common package names)
                            Object muObj = null;
                            String[] muCandidates = new String[]{"edu.univ.erp.ui.studentdashboard.model.ModelUser", "model.ModelUser"};
                            for (String muName : muCandidates) {
                                try {
                                    Class<?> muClass = Class.forName(muName);
                                    try {
                                        java.lang.reflect.Constructor<?> ctor = muClass.getConstructor(String.class, boolean.class);
                                        muObj = ctor.newInstance(user.getUsername(), false);
                                    } catch (NoSuchMethodException ns) {
                                        try {
                                            muObj = muClass.getDeclaredConstructor().newInstance();
                                            try {
                                                java.lang.reflect.Method setName = muClass.getMethod("setUserName", String.class);
                                                java.lang.reflect.Method setAdmin = muClass.getMethod("setAdmin", boolean.class);
                                                setName.invoke(muObj, user.getUsername());
                                                setAdmin.invoke(muObj, false);
                                            } catch (NoSuchMethodException ignored) {
                                            }
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    if (muObj != null) {
                                        break;
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }

                            // 3) If we have a ModelUser, try to call menu.FormManager.login(mu) reflectively
                            if (muObj != null) {
                                try {
                                    Class<?> fmClass = Class.forName("menu.FormManager");
                                    // try to find a login method accepting the ModelUser type or Object
                                    java.lang.reflect.Method m = null;
                                    for (java.lang.reflect.Method mm : fmClass.getMethods()) {
                                        if (mm.getName().equals("login") && mm.getParameterCount() == 1) {
                                            m = mm;
                                            break;
                                        }
                                    }
                                    if (m != null) {
                                        m.invoke(null, muObj);
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }
                        } catch (Throwable t) {
                            // Log reflection/runtime issues so we can debug why dashboard didn't open
                            System.err.println("Client WARN: Failed to launch student dashboard reflectively: " + t);
                            t.printStackTrace(System.err);
                        }
                    });
                }
                case "Instructor" -> {
                    // Prefer launching the new instructor dashboard Application reflectively
                    // to avoid introducing a compile-time dependency on that package.
                    java.awt.EventQueue.invokeLater(() -> {
                        try {
                            // 1) Try to instantiate Application and call setVisible(true)
                            try {
                                Class<?> appClass = Class.forName("edu.univ.erp.ui.instructordashboard.application.Application");
                                Object app = appClass.getDeclaredConstructor().newInstance();
                                try {
                                    java.lang.reflect.Method setVisible = appClass.getMethod("setVisible", boolean.class);
                                    setVisible.invoke(app, true);
                                } catch (NoSuchMethodException ignored) {
                                }
                            } catch (ClassNotFoundException ignored) {
                                // Application class not present; dashboard not available
                            }

                            // 2) Build a ModelUser instance reflectively (try common package names)
                            Object muObj = null;
                            String[] muCandidates = new String[]{"edu.univ.erp.ui.instructordashboard.model.ModelUser", "model.ModelUser"};
                            for (String muName : muCandidates) {
                                try {
                                    Class<?> muClass = Class.forName(muName);
                                    try {
                                        java.lang.reflect.Constructor<?> ctor = muClass.getConstructor(String.class, boolean.class);
                                        muObj = ctor.newInstance(user.getUsername(), false);
                                    } catch (NoSuchMethodException ns) {
                                        try {
                                            muObj = muClass.getDeclaredConstructor().newInstance();
                                            try {
                                                java.lang.reflect.Method setName = muClass.getMethod("setUserName", String.class);
                                                java.lang.reflect.Method setAdmin = muClass.getMethod("setAdmin", boolean.class);
                                                setName.invoke(muObj, user.getUsername());
                                                setAdmin.invoke(muObj, false);
                                            } catch (NoSuchMethodException ignored) {
                                            }
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    if (muObj != null) {
                                        break;
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }

                            // 3) If we have a ModelUser, try to call menu.FormManager.login(mu) reflectively
                            if (muObj != null) {
                                try {
                                    Class<?> fmClass = Class.forName("edu.univ.erp.ui.instructordashboard.menu.FormManager");
                                    // try to find a login method accepting the ModelUser type or Object
                                    java.lang.reflect.Method m = null;
                                    for (java.lang.reflect.Method mm : fmClass.getMethods()) {
                                        if (mm.getName().equals("login") && mm.getParameterCount() == 1) {
                                            m = mm;
                                            break;
                                        }
                                    }
                                    if (m != null) {
                                        m.invoke(null, muObj);
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }
                        } catch (Throwable t) {
                            System.err.println("Client WARN: Failed to launch instructor dashboard reflectively: " + t);
                            t.printStackTrace(System.err);
                        }
                    });
                }
                case "Admin" -> {
                    // Prefer launching the new admin dashboard Application directly
                    // so it can read ClientContext and initialize with real data.
                    java.awt.EventQueue.invokeLater(() -> {
                        try {
                            // Directly instantiate the admin Application (compile-time link).
                            Application app = new Application();
                            app.setVisible(true);
                            return;
                        } catch (Throwable t) {
                            // If direct instantiation fails for any reason, fall back to
                            // the previous reflective/fallback behavior to keep compatibility.
                            System.err.println("Client WARN: Direct admin Application launch failed: " + t);
                            t.printStackTrace(System.err);
                        }

                        // FALLBACK: previous reflective/fallback path
                        try {
                            // Attempt reflective instantiation as before
                            Class<?> appClass = Class.forName("edu.univ.erp.ui.admindashboard.application.Application");
                            Object app = appClass.getDeclaredConstructor().newInstance();
                            try {
                                java.lang.reflect.Method setVisible = appClass.getMethod("setVisible", boolean.class);
                                setVisible.invoke(app, true);
                            } catch (NoSuchMethodException ignored) {
                            }
                        } catch (ClassNotFoundException ignored) {
                            // Application class not present; do not show preview placeholder frames.
                            // Inform the user that the admin dashboard is not available in this build.
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                        "Admin dashboard is not available in this build.",
                                        "Dashboard Unavailable",
                                        JOptionPane.WARNING_MESSAGE);
                            });
                            return;
                        } catch (Throwable t) {
                            System.err.println("Client WARN: Failed to launch admin dashboard reflectively: " + t);
                            t.printStackTrace(System.err);
                        }

                        // Ensure ModelUser login is attempted reflectively as a last step
                        try {
                            Object muObj = null;
                            String[] muCandidates = new String[]{"edu.univ.erp.ui.admindashboard.model.ModelUser", "model.ModelUser"};
                            for (String muName : muCandidates) {
                                try {
                                    Class<?> muClass = Class.forName(muName);
                                    try {
                                        java.lang.reflect.Constructor<?> ctor = muClass.getConstructor(String.class, boolean.class);
                                        muObj = ctor.newInstance(user.getUsername(), true); // Admin = true
                                    } catch (NoSuchMethodException ns) {
                                        try {
                                            muObj = muClass.getDeclaredConstructor().newInstance();
                                            try {
                                                java.lang.reflect.Method setName = muClass.getMethod("setUserName", String.class);
                                                java.lang.reflect.Method setAdmin = muClass.getMethod("setAdmin", boolean.class);
                                                setName.invoke(muObj, user.getUsername());
                                                setAdmin.invoke(muObj, true); // Admin = true
                                            } catch (NoSuchMethodException ignored) {
                                            }
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    if (muObj != null) {
                                        break;
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }

                            if (muObj != null) {
                                try {
                                    Class<?> fmClass = Class.forName("edu.univ.erp.ui.admindashboard.menu.FormManager");
                                    java.lang.reflect.Method m = null;
                                    for (java.lang.reflect.Method mm : fmClass.getMethods()) {
                                        if (mm.getName().equals("login") && mm.getParameterCount() == 1) {
                                            m = mm;
                                            break;
                                        }
                                    }
                                    if (m != null) {
                                        m.invoke(null, muObj);
                                    }
                                } catch (ClassNotFoundException ignored) {
                                }
                            }
                        } catch (Throwable ignore) {
                        }
                    });
                }
                default -> {
                    // No dedicated dashboard available for this role. Do not open preview placeholders.
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null,
                                "No dashboard is available for role: " + role,
                                "Not Implemented",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
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

    private void lblForgotPasswordMouseClicked() {
        showPasswordResetForm();
    }

    private void showPasswordResetForm() {
        isPasswordResetMode = true;

        // Hide login form components instantly
        txtUser.setVisible(false);
        txtPassword.setVisible(false);
        jLabel1.setVisible(false);
        cmdLogin.setVisible(false);
        lblForgotPassword.setVisible(false);

        // Clear any existing text
        txtResetUser.setText("");
        txtNewPassword.setText("");
        txtConfirmPassword.setText("");

        // Show password reset form components instantly
        txtResetUser.setVisible(true);
        txtNewPassword.setVisible(true);
        txtConfirmPassword.setVisible(true);
        cmdChangePassword.setVisible(true);
        lblSignIn.setVisible(true);

        // Focus on the first field
        txtResetUser.requestFocus();

        // Repaint the panel
        panel.revalidate();
        panel.repaint();
    }

    private void lblSignInMouseClicked() {
        showLoginForm();
    }

    private void showLoginForm() {
        isPasswordResetMode = false;

        // Hide password reset form components instantly
        txtResetUser.setVisible(false);
        txtNewPassword.setVisible(false);
        txtConfirmPassword.setVisible(false);
        cmdChangePassword.setVisible(false);
        lblSignIn.setVisible(false);

        // Clear any existing text
        txtUser.setText("");
        txtPassword.setText("");

        // Show login form components instantly
        txtUser.setVisible(true);
        txtPassword.setVisible(true);
        jLabel1.setVisible(true);
        cmdLogin.setVisible(true);
        lblForgotPassword.setVisible(true);

        // Focus on the first field
        txtUser.requestFocus();

        // Repaint the panel
        panel.revalidate();
        panel.repaint();
    }

    private void cmdChangePasswordActionPerformed() {
        String username = txtResetUser.getText().trim();
        String newPassword = String.valueOf(txtNewPassword.getPassword());
        String confirmPassword = String.valueOf(txtConfirmPassword.getPassword());

        // Validate inputs
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your username.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            txtResetUser.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a new password.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 6 characters long.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match. Please try again.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            txtConfirmPassword.requestFocus();
            return;
        }

        try {
        // Call server to request password reset (notify admin)
        String resp = authApi.requestPasswordReset(username, newPassword);
        String message = "Password reset request submitted.";
        if (resp != null && !resp.isEmpty()) message = resp;
        JOptionPane.showMessageDialog(this,
            message,
            "Password Reset Requested",
            JOptionPane.INFORMATION_MESSAGE);

        // Return to login form
        showLoginForm();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to change password: " + ex.getMessage(),
                    "Password Change Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String args[]) {
        // Copy EXACTLY from student dashboard Application.java for consistent theme and animations
        com.formdev.flatlaf.fonts.roboto.FlatRobotoFont.install();
        com.formdev.flatlaf.FlatLaf.registerCustomDefaultsSource("themes");
        javax.swing.UIManager.put("defaultFont", new java.awt.Font(com.formdev.flatlaf.fonts.roboto.FlatRobotoFont.FAMILY, java.awt.Font.PLAIN, 13));
        com.formdev.flatlaf.themes.FlatMacDarkLaf.setup();

        // Register a shutdown hook to attempt a graceful logout when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Try to logout using the AuthAPI; ignore errors
                new edu.univ.erp.api.auth.AuthAPI().logout();
            } catch (Exception ignore) {
            }
        }));

        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
    private edu.univ.erp.ui.loginpage.login.Background background;
    private edu.univ.erp.ui.loginpage.swing.Button cmdLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblForgotPassword;
    private javax.swing.JPanel panel;
    private edu.univ.erp.ui.loginpage.swing.PasswordField txtPassword;
    private edu.univ.erp.ui.loginpage.swing.TextField txtUser;

    // Password reset form components
    private edu.univ.erp.ui.loginpage.swing.TextField txtResetUser;
    private edu.univ.erp.ui.loginpage.swing.PasswordField txtNewPassword;
    private edu.univ.erp.ui.loginpage.swing.PasswordField txtConfirmPassword;
    private edu.univ.erp.ui.loginpage.swing.Button cmdChangePassword;
    private javax.swing.JLabel lblSignIn;
}

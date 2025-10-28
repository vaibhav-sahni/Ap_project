package edu.univ.erp.ui.loginpage.main;

import java.awt.Color;

import javax.swing.JOptionPane;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.controller.DashboardController;
import edu.univ.erp.ui.preview.AdminDashboardFrame;



public class Login extends javax.swing.JFrame {

    edu.univ.erp.api.auth.AuthAPI authApi = new edu.univ.erp.api.auth.AuthAPI();
    private final boolean UNDECORATED = !true; // Same as student dashboard

    public Login() {
        initComponents();
    }

    private void initComponents() {

        background = new edu.univ.erp.ui.loginpage.login.Background();
        panel = new javax.swing.JPanel();
        txtUser = new edu.univ.erp.ui.loginpage.swing.TextField();
        txtPassword = new edu.univ.erp.ui.loginpage.swing.PasswordField();
        jLabel1 = new javax.swing.JLabel();
        cmdLogin = new edu.univ.erp.ui.loginpage.swing.Button();

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

            System.out.println("Client LOG: Login SUCCESS. User Role: " + user.getRole());

            // Persist current user for in-process components (dashboard) BEFORE disposing
            try {
                edu.univ.erp.ClientContext.setCurrentUser(user);
                System.out.println("Client LOG: ClientContext set to user=" + user.getUsername());
            } catch (Throwable ignore) {}

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
                                    if (muObj != null) break;
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
    private javax.swing.JPanel panel;
    private edu.univ.erp.ui.loginpage.swing.PasswordField txtPassword;
    private edu.univ.erp.ui.loginpage.swing.TextField txtUser;
}

package edu.univ.erp.ui.admindashboard.forms;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;

import edu.univ.erp.ui.admindashboard.components.SimpleForm;
import net.miginfocom.swing.MigLayout;

/**
 * The main dashboard form for the Admin user. This form serves as the central
 * hub for all admin-related tasks.
 *
 * This version is updated with theme-aware, aesthetic components and connects
 * to real data sources.
 */
public class DashboardForm extends SimpleForm {

    // --- UI Components ---
    private JLabel lbTitle;
    private JPanel buttonBar; // This will be a CardPanel
    private JToggleButton btnToggleMaintenance;
    private JButton btnSetDeadline;
    private JButton btnBackup;
    private JButton btnRestore;
    private JButton btnSendNotification;
    private JButton btnCreateCourse;
    private JButton btnCreateStudent;
    private JButton btnCreateInstructor;
    private JButton btnCreateSection;
    private JButton btnReassignInstructor;

    private JTabbedPane tabbedPane;
    private JTable studentTable;
    private JTable courseTable;
    private JTable sectionTable;

    // --- Theme/Aesthetic Fields ---
    private boolean lafListenerInstalled = false; // ensure we add Look&Feel listener once
    private FadePanel titleWrapper;
    private CardPanel buttonWrapper;
    private CardPanel tabWrapper;
    // Simple in-memory store for created instructors (dummy mode)
    private List<String> instructors = new ArrayList<>();

    // --- UI theme helpers (from student dashboard file) ---
    private Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private Color panelBg() {
        return uiColor("Panel.background", getBackground());
    }

    private Color textColor() {
        boolean isDark = FlatLaf.isLafDark();
        return isDark ? new Color(234, 234, 234) : new Color(30, 30, 30);
    }

    private Color secondaryTextColor() {
        boolean isDark = FlatLaf.isLafDark();
        return isDark ? new Color(153, 153, 153) : new Color(100, 100, 100);
    }

    private Color accentColor() {
        Color c = UIManager.getColor("Component.accentColor");
        if (c == null) {
            c = UIManager.getColor("Button.startBackground");
        }
        if (c == null) {
            c = UIManager.getColor("Component.focusColor");
        }
        return c != null ? c : Color.decode("#5856D6");
    }

    public DashboardForm() {
        // Add SimpleForm styling
        putClientProperty(FlatClientProperties.STYLE, "" + "border:5,5,5,5;" + "background:null");
        init();
    }

    /**
     * Initializes the layout and all UI components on the panel.
     */
    private void init() {
        // Set main panel to be transparent; background color is managed by formRefresh
        setOpaque(false);

        // Use MigLayout for flexible and powerful layout management
        // "wrap" - new components go on a new line
        // "fillx" - components fill the horizontal space
        // "insets 20" - adds 20px padding (matches student dashboard)
        // "[fill]" - one column that fills the width
        // "[]unrel[]unrel[grow,fill]" - 3 rows: title, button bar, main content
        setLayout(new MigLayout("wrap, fillx, insets 20", "[fill]", "[]unrel[]unrel[grow,fill]"));

        // 1. Title Label (wrapped in a FadePanel) - Match student dashboard style exactly
        String username = "";
        try {
            edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
            if (cu != null) {
                username = cu.getUsername();
            }
        } catch (Throwable ignore) {
        }

        lbTitle = new JLabel("Welcome back" + (username.isEmpty() ? ", Admin!" : ", " + username + "!")) {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(textColor());
                super.paintComponent(g);
            }
        };
        lbTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));

        titleWrapper = new FadePanel(0f);
        titleWrapper.setOpaque(false);
        titleWrapper.setLayout(new BorderLayout());
        titleWrapper.add(lbTitle, BorderLayout.CENTER);
        add(titleWrapper); // Add the wrapper, not the label directly

        // 2. Button Bar (wrapped in a CardPanel)
        initButtonBar(); // This now creates 'buttonBar' as a CardPanel
        add(buttonBar); // Add the card panel wrapper

        // 3. Main Content Area (Tables with real data) - Updated to focus on all sections/courses/students
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("All Sections", createTablePanel(createAllSectionsTable()));
        tabbedPane.addTab("All Courses", createTablePanel(createAllCoursesTable()));
        tabbedPane.addTab("All Students", createTablePanel(createAllStudentsTable()));

        // Add padding inside the card, around the tabs
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the card wrapper for the tabs
        tabWrapper = new CardPanel();
        tabWrapper.setLayout(new BorderLayout());
        tabWrapper.add(tabbedPane, BorderLayout.CENTER);

        // Set maximum size to prevent infinite expansion
        tabWrapper.setPreferredSize(new Dimension(900, 500));
        tabWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));

        // Add with grow constraint but limit expansion
        add(tabWrapper, "grow");

        // Start fade-in animation for the title
        SwingUtilities.invokeLater(() -> titleWrapper.startFadeIn(0));

        // Refresh the entire window when Look & Feel (mode) changes.
        if (!lafListenerInstalled) {
            lafListenerInstalled = true;
            UIManager.addPropertyChangeListener(evt -> {
                if ("lookAndFeel".equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        // Refresh this form's specific dynamic colors
                        formRefresh();
                        // Update the whole tree
                        SwingUtilities.updateComponentTreeUI(this);
                    });
                }
            });
        }
    }

    /**
     * Creates the horizontal panel containing the main admin action buttons.
     * This is now a CardPanel for aesthetics.
     */
    private void initButtonBar() {
        // Use CardPanel for the aesthetic background
        buttonBar = new CardPanel();
        // Use MigLayout *inside* the card panel
        // "insets 15" adds padding inside the card
        buttonBar.setLayout(new MigLayout("fillx, insets 15", "[]push[]push[]push[]push[]")); // Buttons spaced out

        // Create buttons from the user's image
        btnToggleMaintenance = new JToggleButton("Maintenance: OFF");
        btnToggleMaintenance.setSelected(false); // Default to OFF
        // Set initial green color for OFF state
        btnToggleMaintenance.setBackground(new Color(34, 197, 94)); // Green
        btnToggleMaintenance.setForeground(Color.WHITE);
        btnToggleMaintenance.setOpaque(true);

        btnSetDeadline = new JButton("Set Drop Deadline");
        btnBackup = new JButton("Download DB Backup");
        btnRestore = new JButton("Restore DB Backup");
        btnSendNotification = new JButton("Send Notification");

        // Admin creation buttons
        btnCreateCourse = new JButton("Create Course");
        btnCreateStudent = new JButton("Create Student");
        btnCreateInstructor = new JButton("Create Instructor");
        btnCreateSection = new JButton("Create Section");
        btnReassignInstructor = new JButton("Reassign Instructor");

    // Apply rounded-corner styling to match the social-media-ui look
    String roundedStyle = "arc: 10";
    btnSetDeadline.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnBackup.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnRestore.putClientProperty(FlatClientProperties.STYLE, roundedStyle);

    // Special style for the "Send Notification" button
    btnSendNotification.putClientProperty(FlatClientProperties.STYLE, roundedStyle);

    // Apply the same rounded style to create/reassign buttons for visual consistency
    btnCreateCourse.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnCreateStudent.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnCreateInstructor.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnCreateSection.putClientProperty(FlatClientProperties.STYLE, roundedStyle);
    btnReassignInstructor.putClientProperty(FlatClientProperties.STYLE, roundedStyle);

    // Primary black/white style used for important admin actions
    String primaryBlackStyle = "arc: 10; background: #000000; borderColor: #000000; focusedBackground: #000000; hoverBackground: #111111; pressedBackground: #222222;";
    JButton[] primaryButtons = new JButton[] { btnSetDeadline, btnBackup, btnRestore, btnSendNotification,
        btnCreateCourse, btnCreateStudent, btnCreateInstructor, btnCreateSection, btnReassignInstructor };
    for (JButton b : primaryButtons) {
        if (b == null) continue;
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.putClientProperty(FlatClientProperties.STYLE, primaryBlackStyle);
    }

        // Style for the Toggle Button - Remove glass effects and ensure solid colors
        btnToggleMaintenance.putClientProperty(FlatClientProperties.STYLE,
                "arc: 10; "
                + "borderWidth: 0; "
                + "focusWidth: 0; "
                + "innerFocusWidth: 0; "
                + "selectedBackground: $Component.accentColor; "
                + "background: $Component.accentColor");

        // Add buttons to the bar
        buttonBar.add(btnToggleMaintenance);
        buttonBar.add(btnSetDeadline);
        buttonBar.add(btnBackup);
        buttonBar.add(btnRestore);
        buttonBar.add(btnSendNotification);

        // Add create / admin tools (compact, to the right)
        buttonBar.add(btnCreateCourse);
        buttonBar.add(btnCreateStudent);
        buttonBar.add(btnCreateInstructor);
        buttonBar.add(btnCreateSection);
        buttonBar.add(btnReassignInstructor);

        // Ensure buttons reflect current maintenance state
        updateMaintenanceButtons();

        // When this form becomes showing, refresh the maintenance-guarded button states
        this.addHierarchyListener(evt -> {
            long flags = evt.getChangeFlags();
            if ((flags & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                updateMaintenanceButtons();
            }
        });
    }

    /**
     * Helper method to create standardized dialogs with consistent sizing.
     */
    private JDialog createStandardDialog(String title) {
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Set professional size that fits content properly
        dialog.setSize(550, 520); // Increased height for better spacing
        dialog.setLocationRelativeTo(owner);
        dialog.setResizable(true); // Allow resizing for flexibility

        return dialog;
    }

    // --- Creation dialogs (dummy implementations that modify local table models) ---
    private void showCreateCourseDialog() {
        JDialog dialog = createStandardDialog("Create Course");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        content.add(new JLabel("Course Code:"), "gaptop 5");
        JTextField txtCode = new JTextField();
        txtCode.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtCode, "gapbottom 10");

        content.add(new JLabel("Course Title:"));
        JTextField txtTitle = new JTextField();
        txtTitle.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtTitle, "gapbottom 10");

        content.add(new JLabel("Credits:"));
        JTextField txtCredits = new JTextField();
        txtCredits.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtCredits, "gapbottom 15");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnCreate = new JButton("Create");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnCreate.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnCreate);
        content.add(buttons, "span, growx, gaptop 10");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnCreate.addActionListener(a -> {
            String code = txtCode.getText().trim();
            String title = txtTitle.getText().trim();
            String credits = txtCredits.getText().trim();
            if (code.isEmpty() || title.isEmpty() || credits.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int cr = Integer.parseInt(credits);

                // Create course object using constructor
                edu.univ.erp.domain.CourseCatalog course = new edu.univ.erp.domain.CourseCatalog(
                        code, title, cr, 0, "", "", 0, 0, "Fall", 2025, 0, ""
                );

                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                String result = adminActions.createCourseOnly(course);

                // Refresh the course table
                loadCourseDataAsync((DefaultTableModel) courseTable.getModel());

                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Course created successfully: " + result, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Credits must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to create course: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void showCreateStudentDialog() {
        JDialog dialog = createStandardDialog("Create Student");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        content.add(new JLabel("Roll No:"), "gaptop 5");
        JTextField txtRoll = new JTextField();
        txtRoll.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtRoll, "gapbottom 10");

        content.add(new JLabel("Student Name:"));
        JTextField txtName = new JTextField();
        txtName.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtName, "gapbottom 10");

        content.add(new JLabel("Program:"));
        JTextField txtProg = new JTextField();
        txtProg.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtProg, "gapbottom 10");

        content.add(new JLabel("Year:"));
        JTextField txtYear = new JTextField();
        txtYear.setPreferredSize(new java.awt.Dimension(200, 30));
    content.add(txtYear, "gapbottom 15");

    content.add(new JLabel("Initial Password:"));
    javax.swing.JPasswordField txtPassword = new javax.swing.JPasswordField();
    txtPassword.setPreferredSize(new java.awt.Dimension(200, 30));
    content.add(txtPassword, "gapbottom 15");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnCreate = new JButton("Create");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnCreate.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnCreate);
        content.add(buttons, "span, growx, gaptop 10");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnCreate.addActionListener(a -> {
            String roll = txtRoll.getText().trim();
            String name = txtName.getText().trim();
            String prog = txtProg.getText().trim();
            String year = txtYear.getText().trim();
            if (roll.isEmpty() || name.isEmpty() || prog.isEmpty() || year.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int y = Integer.parseInt(year);

                // Create student object using constructor
                edu.univ.erp.domain.Student student = new edu.univ.erp.domain.Student(
                        0, // userId will be set by server
                        name, // username same as name
                        "Student", // role
                        roll, // rollNo
                        prog, // program
                        y // year
                );

                // Read password from the dialog (allow empty to use default)
                String pwd = new String(txtPassword.getPassword());
                if (pwd == null || pwd.trim().isEmpty()) pwd = "student123";

                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                String result = adminActions.createStudent(student, pwd);

                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Student created successfully: " + result, "Success", JOptionPane.INFORMATION_MESSAGE);

                // Refresh the student data in the table
                loadStudentData();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Year must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating student: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void showCreateInstructorDialog() {
        JDialog dialog = createStandardDialog("Create Instructor");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        content.add(new JLabel("Instructor Name:"), "gaptop 5");
        JTextField txtName = new JTextField();
        txtName.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtName, "gapbottom 10");

        content.add(new JLabel("Department:"));
        JTextField txtDept = new JTextField();
        txtDept.setPreferredSize(new java.awt.Dimension(200, 30));
    content.add(txtDept, "gapbottom 15");

    content.add(new JLabel("Initial Password:"));
    javax.swing.JPasswordField txtInstrPassword = new javax.swing.JPasswordField();
    txtInstrPassword.setPreferredSize(new java.awt.Dimension(200, 30));
    content.add(txtInstrPassword, "gapbottom 15");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnCreate = new JButton("Create");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnCreate.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnCreate);
        content.add(buttons, "span, growx, gaptop 10");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnCreate.addActionListener(a -> {
            String name = txtName.getText().trim();
            String dept = txtDept.getText().trim();
            if (name.isEmpty() || dept.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Create instructor object using constructor and setters
                edu.univ.erp.domain.Instructor instructor = new edu.univ.erp.domain.Instructor(
                        0, // userId will be set by server
                        name, // username same as name
                        "Instructor", // role
                        dept // department
                );
                instructor.setName(name);

                // Read password from the dialog (allow empty to use default)
                String pwd = new String(txtInstrPassword.getPassword());
                if (pwd == null || pwd.trim().isEmpty()) pwd = "instr123";

                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                String result = adminActions.createInstructor(instructor, pwd);

                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Instructor created successfully: " + result, "Success", JOptionPane.INFORMATION_MESSAGE);

                // Refresh the instructor data
                loadInstructorData();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating instructor: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void showCreateSectionDialog() {
        JDialog dialog = createStandardDialog("Create Section");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        // Course selector (from course table)
        content.add(new JLabel("Course Code:"), "gaptop 10");
        DefaultTableModel courseModel = (DefaultTableModel) courseTable.getModel();
        int rows = courseModel.getRowCount();
        String[] codes = new String[Math.max(1, rows)];
        for (int i = 0; i < rows; i++) {
            codes[i] = String.valueOf(courseModel.getValueAt(i, 0));
        }
        if (rows == 0) {
            codes[0] = "<no-courses>";
        }
        JComboBox<String> courseDropdown = new JComboBox<>(codes);
        courseDropdown.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(courseDropdown, "gapbottom 15");

        content.add(new JLabel("Instructor:"));
        JComboBox<String> instrDropdown = new JComboBox<>(instructors.toArray(new String[0]));
        instrDropdown.setEditable(true); // allow manual entry
        instrDropdown.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(instrDropdown, "gapbottom 15");

        content.add(new JLabel("Day/Time:"));
        JTextField txtDT = new JTextField();
        txtDT.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtDT, "gapbottom 15");

        content.add(new JLabel("Room:"));
        JTextField txtRoom = new JTextField();
        txtRoom.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtRoom, "gapbottom 15");

        content.add(new JLabel("Capacity:"));
        JTextField txtCap = new JTextField();
        txtCap.setPreferredSize(new java.awt.Dimension(200, 30));
        content.add(txtCap, "gapbottom 20");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnCreate = new JButton("Create");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnCreate.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnCreate);
        content.add(buttons, "span, growx, gaptop 20");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnCreate.addActionListener(a -> {
            String course = (String) courseDropdown.getSelectedItem();
            String instructor = (String) instrDropdown.getSelectedItem();
            String dt = txtDT.getText().trim();
            String room = txtRoom.getText().trim();
            String cap = txtCap.getText().trim();
            if (course == null || course.isEmpty() || instructor == null || instructor.isEmpty() || dt.isEmpty() || room.isEmpty() || cap.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int c = Integer.parseInt(cap);
                DefaultTableModel model = (DefaultTableModel) sectionTable.getModel();
                model.addRow(new Object[]{course, instructor, dt, room, c});
                // keep instructor in list for reuse
                if (!instructors.contains(instructor)) {
                    instructors.add(instructor);
                }
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Section created.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Capacity must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Update the enabled/tooltip state of admin buttons when maintenance mode changes.
     */
    private void updateMaintenanceButtons() {
        boolean maintenance = edu.univ.erp.ui.components.MaintenanceModeManager.getInstance().isMaintenanceMode();
        JButton[] guarded = new JButton[] { btnSendNotification, btnCreateInstructor, btnCreateSection, btnReassignInstructor, btnSetDeadline };
        for (JButton b : guarded) {
            if (b == null) continue;
            b.setEnabled(!maintenance);
            if (maintenance) {
                b.setToolTipText("Cannot make edits when maintenance mode is on");
                java.awt.Color disabledColor = UIManager.getColor("Button.disabledBackground");
                if (disabledColor == null) disabledColor = new java.awt.Color(200, 200, 200);
                b.setBackground(disabledColor);
                b.setForeground(UIManager.getColor("Label.disabledForeground") != null ? UIManager.getColor("Label.disabledForeground") : new java.awt.Color(120,120,120));
            } else {
                b.setToolTipText(null);
                // restore primary black/white style when maintenance is off
                b.setBackground(Color.BLACK);
                b.setForeground(Color.WHITE);
                b.setOpaque(true);
            }
        }
    }

    /**
     * Check maintenance mode and show a warning dialog if edits are not allowed.
     * @return true if edits are allowed, false if maintenance mode is active
     */
    private boolean checkMaintenanceAndWarn() {
        if (edu.univ.erp.ui.components.MaintenanceModeManager.getInstance().isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot make edits while maintenance mode is active.",
                    "Maintenance Active",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void showReassignInstructorDialog() {
        JDialog dialog = createStandardDialog("Reassign Instructor");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        // Build list of sections for selection
        DefaultTableModel sectionModel = (DefaultTableModel) sectionTable.getModel();
        int srows = sectionModel.getRowCount();
        if (srows == 0) {
            JOptionPane.showMessageDialog(this, "No sections defined to reassign.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] sections = new String[srows];
        for (int i = 0; i < srows; i++) {
            sections[i] = sectionModel.getValueAt(i, 0) + " - " + sectionModel.getValueAt(i, 2) + " (" + sectionModel.getValueAt(i, 1) + ")";
        }
        content.add(new JLabel("Select Section:"), "gaptop 5");
        JComboBox<String> sectionDropdown = new JComboBox<>(sections);
        sectionDropdown.setPreferredSize(new java.awt.Dimension(300, 30));
        content.add(sectionDropdown, "gapbottom 15");

        content.add(new JLabel("New Instructor:"));
        if (instructors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No instructors available. Please create an instructor first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<String> instrDropdown = new JComboBox<>(instructors.toArray(new String[0]));
        instrDropdown.setEditable(true);
        instrDropdown.setPreferredSize(new java.awt.Dimension(300, 30));
        content.add(instrDropdown, "gapbottom 20");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnApply = new JButton("Reassign");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnApply.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnApply);
        content.add(buttons, "span, growx, gaptop 10");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnApply.addActionListener(a -> {
            int sel = sectionDropdown.getSelectedIndex();
            String newInstr = (String) instrDropdown.getSelectedItem();
            if (sel < 0 || newInstr == null || newInstr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select a section and provide an instructor.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sectionModel.setValueAt(newInstr, sel, 1); // update Instructor column
            if (!instructors.contains(newInstr)) {
                instructors.add(newInstr);
            }
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Instructor reassigned.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Creates a JTable with all students data from server.
     */
    private JTable createAllStudentsTable() {
        String[] columns = {"Student ID", "Name", "Program", "Year", "Status"};

        // Create table model that doesn't allow cell editing and removes highlighting
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No cell editing
            }
        };

        studentTable = new ModernTable(model);
        styleTable(studentTable);

        // Load data asynchronously
        loadStudentDataAsync(model);

        return studentTable;
    }

    /**
     * Creates a JTable with all courses data from server.
     */
    private JTable createAllCoursesTable() {
        String[] columns = {"Course Code", "Course Name", "Credits", "Total Sections", "Status"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        courseTable = new ModernTable(model);
        styleTable(courseTable);

        // Load data asynchronously
        loadCourseDataAsync(model);

        return courseTable;
    }

    /**
     * Creates a JTable with all sections data from server.
     */
    private JTable createAllSectionsTable() {
        String[] columns = {"Section ID", "Course Code", "Course Name", "Instructor", "Schedule", "Room", "Enrolled", "Capacity"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        sectionTable = new ModernTable(model);
        styleTable(sectionTable);

        // Custom renderer for the Instructor column: show red "NOT ASSIGNED" when no instructor
        try {
            int instrCol = 3; // Instructor column index
            sectionTable.getColumnModel().getColumn(instrCol).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    String s = value == null ? "" : String.valueOf(value).trim();
                    if (s.isEmpty() || "TBA".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s)) {
                        setText("NOT ASSIGNED");
                        setForeground(new Color(220, 38, 38)); // red
                    } else {
                        setText(s);
                        setForeground(textColor());
                    }
                    return c;
                }
            });
        } catch (Exception ignore) {
            // If column model not ready, ignore silently (table will show plain text)
        }

        // Load data asynchronously
        loadSectionDataAsync(model);

        return sectionTable;
    }

    // Table styling similar to instructor dashboard
    private void styleTable(JTable table) {
        table.setRowHeight(46);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowGrid(false);

        // Remove selection highlighting
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        // Remove focus highlighting and borders
        table.setFocusable(false);
        table.setBorder(null);

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        header.setDefaultRenderer(new SortHeaderRenderer());

        // Remove header focus
        header.setFocusable(false);
    }

    // Async data loading methods
    private void loadStudentDataAsync(DefaultTableModel model) {
        new Thread(() -> {
            try {
                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                java.util.List<edu.univ.erp.domain.Student> students = adminActions.fetchAllStudents();

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0); // Clear existing data
                    if (students != null) {
                        for (edu.univ.erp.domain.Student student : students) {
                            model.addRow(new Object[]{
                                student.getRollNo(),
                                student.getUsername(), // Name is from UserAuth
                                student.getProgram(),
                                student.getYear(),
                                "Active"
                            });
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    model.addRow(new Object[]{"Error loading data", ex.getMessage(), "", "", ""});
                });
            }
        }).start();
    }

    private void loadCourseDataAsync(DefaultTableModel model) {
        new Thread(() -> {
            try {
                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                java.util.List<edu.univ.erp.domain.CourseCatalog> courses = adminActions.fetchAllCourses();

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);
                    if (courses != null) {
                        // Group by course code to count sections
                        java.util.Map<String, Integer> sectionCounts = new java.util.HashMap<>();
                        java.util.Map<String, edu.univ.erp.domain.CourseCatalog> courseMap = new java.util.HashMap<>();

                        for (edu.univ.erp.domain.CourseCatalog course : courses) {
                            String courseCode = course.getCourseCode();
                            sectionCounts.put(courseCode, sectionCounts.getOrDefault(courseCode, 0) + 1);
                            if (!courseMap.containsKey(courseCode)) {
                                courseMap.put(courseCode, course);
                            }
                        }

                        for (String courseCode : courseMap.keySet()) {
                            edu.univ.erp.domain.CourseCatalog course = courseMap.get(courseCode);
                            model.addRow(new Object[]{
                                course.getCourseCode(),
                                course.getCourseTitle(), // Correct method name
                                course.getCredits(),
                                sectionCounts.get(courseCode),
                                "Active"
                            });
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    model.addRow(new Object[]{"Error", ex.getMessage(), "", "", ""});
                });
            }
        }).start();
    }

    private void loadSectionDataAsync(DefaultTableModel model) {
        new Thread(() -> {
            try {
                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                java.util.List<edu.univ.erp.domain.CourseCatalog> sections = adminActions.fetchAllCourses();

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);
                    if (sections != null) {
                        for (edu.univ.erp.domain.CourseCatalog section : sections) {
                            model.addRow(new Object[]{
                                section.getSectionId(),
                                section.getCourseCode(),
                                section.getCourseTitle(), // Correct method name
                                section.getInstructorName() != null ? section.getInstructorName() : "TBA",
                                section.getDayTime() != null ? section.getDayTime() : "TBA",
                                section.getRoom() != null ? section.getRoom() : "TBA",
                                section.getEnrolledCount(), // Correct method name
                                section.getCapacity()
                            });
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    model.addRow(new Object[]{"Error", ex.getMessage(), "", "", "", "", "", ""});
                });
            }
        }).start();
    }

    /**
     * Helper method to wrap a JTable in a JScrollPane with proper sizing
     * constraints.
     */
    private JScrollPane createTablePanel(JTable table) {
        // Set table properties to prevent infinite expansion
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setRowHeight(25);

        // Set preferred size for the table to control initial dimensions
        table.setPreferredScrollableViewportSize(new Dimension(800, 400));

        JScrollPane scrollPane = new JScrollPane(table);
        // Set maximum size to prevent infinite expansion
        scrollPane.setPreferredSize(new Dimension(800, 400));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        // Ensure scroll pane doesn't try to expand infinitely
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Remove focus and borders from scroll pane
        scrollPane.setFocusable(false);
        scrollPane.setBorder(null);

        return scrollPane;
    }

    /**
     * Displays the "Send Notification" popup panel using GlassPanePopup. This
     * method builds the entire popup UI dynamically, styled like a card.
     */
    private void showNotificationPopup() {
        JDialog dialog = createStandardDialog("Send Notification");
        // Override size for notification dialog to be larger with more height
        dialog.setSize(550, 600);

        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow,fill]"));

        content.add(new JLabel("Send to:"), "gaptop 5");
        String[] roles = {"All Users", "Students Only", "Instructors Only"};
        JComboBox<String> roleDropdown = new JComboBox<>(roles);
        roleDropdown.setPreferredSize(new java.awt.Dimension(300, 30));
        content.add(roleDropdown, "gapbottom 15");

        // Create conditional recipient selection components
        JLabel recipientLabel = new JLabel("Select Recipient:");
        JComboBox<String> recipientDropdown = new JComboBox<>();
        recipientDropdown.setPreferredSize(new java.awt.Dimension(300, 30));

        // Initially hide the recipient selection
        recipientLabel.setVisible(false);
        recipientDropdown.setVisible(false);

        content.add(recipientLabel, "gaptop 5");
        content.add(recipientDropdown, "gapbottom 15");

        content.add(new JLabel("Notification Title:"));
        JTextField txtTitle = new JTextField();
        txtTitle.setPreferredSize(new java.awt.Dimension(300, 30));
        content.add(txtTitle, "gapbottom 10");

        content.add(new JLabel("Message:"));
        JTextArea txtMessage = new JTextArea(8, 40);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(txtMessage);
        content.add(messageScrollPane, "h 180!, gapbottom 15");

        JPanel buttons = new JPanel(new MigLayout("align center", "[120!][120!]"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnSend = new JButton("Send");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnSend.setPreferredSize(new java.awt.Dimension(120, 35));
        buttons.add(btnCancel, "gap 10");
        buttons.add(btnSend);
        content.add(buttons, "span, growx, gaptop 15");

        // Add listener for role dropdown to show/hide recipient selection
        roleDropdown.addActionListener(e -> {
            String selectedRole = (String) roleDropdown.getSelectedItem();
            if ("Students Only".equals(selectedRole)) {
                recipientLabel.setText("Select Student:");
                recipientLabel.setVisible(true);
                recipientDropdown.setVisible(true);

                // Load students from database
                recipientDropdown.removeAllItems();
                recipientDropdown.addItem("All Students");
                try {
                    edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                    java.util.List<edu.univ.erp.domain.Student> students = adminActions.fetchAllStudents();
                    for (edu.univ.erp.domain.Student student : students) {
                        recipientDropdown.addItem(student.getRollNo() + " - " + student.getUsername());
                    }
                } catch (Exception ex) {
                    System.err.println("Error loading students: " + ex.getMessage());
                }

            } else if ("Instructors Only".equals(selectedRole)) {
                recipientLabel.setText("Select Instructor:");
                recipientLabel.setVisible(true);
                recipientDropdown.setVisible(true);

                // Load instructors from database
                recipientDropdown.removeAllItems();
                recipientDropdown.addItem("All Instructors");
                try {
                    edu.univ.erp.ui.actions.AdminActions adminActionsLocal = new edu.univ.erp.ui.actions.AdminActions();
                    java.util.List<java.util.Map<String, Object>> instructorsList = adminActionsLocal.fetchAllInstructors();
                    for (java.util.Map<String, Object> instructor : instructorsList) {
                        String name = (String) instructor.get("username");
                        if (name != null) {
                            recipientDropdown.addItem(name);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error loading instructors: " + ex.getMessage());
                }

            } else {
                recipientLabel.setVisible(false);
                recipientDropdown.setVisible(false);
            }

            // Force layout update
            content.revalidate();
            content.repaint();
            dialog.pack();
        });

        btnCancel.addActionListener(a -> dialog.dispose());
        btnSend.addActionListener(a -> {
            String sendTo = (String) roleDropdown.getSelectedItem();
            String specificRecipient = (String) recipientDropdown.getSelectedItem();
            String title = txtTitle.getText();
            String message = txtMessage.getText();
            if (title == null || title.isBlank() || message == null || message.isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Title and message cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Map UI selection to server recipientType
            String recipientType = "ALL";
            int recipientId = 0; // 0 means all users of that type

            if ("Students Only".equalsIgnoreCase(sendTo)) {
                recipientType = "STUDENT";
                // Check if specific student is selected
                if (specificRecipient != null && !specificRecipient.equals("All Students")) {
                    // Extract student ID from the format "rollNo - name"
                    String[] parts = specificRecipient.split(" - ");
                    if (parts.length > 0) {
                        // For now, we'll use 0 as we'd need to map rollNo to userId
                        // This could be enhanced to fetch the actual user ID
                        recipientId = 0; // Still send to all students for now
                    }
                }
            } else if ("Instructors Only".equalsIgnoreCase(sendTo)) {
                recipientType = "INSTRUCTOR";
                // Check if specific instructor is selected
                if (specificRecipient != null && !specificRecipient.equals("All Instructors")) {
                    // For specific instructor, we'd need to map username to userId
                    // This could be enhanced to fetch the actual user ID
                    recipientId = 0; // Still send to all instructors for now
                }
            }

            // capture into effectively-final variables for use inside the worker thread
            final String fRecipientType = recipientType;
            final int fRecipientId = recipientId;
            final String fTitle = title;
            final String fMessage = message;
            final String fSelectedRecipient = specificRecipient;

            // Send in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    edu.univ.erp.api.NotificationAPI api = new edu.univ.erp.api.NotificationAPI();
                    boolean ok = api.sendNotification(fRecipientType, fRecipientId, fTitle, fMessage);
                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            dialog.dispose();
                            String successMsg = "Notification sent successfully!";
                            if (fSelectedRecipient != null
                                    && !fSelectedRecipient.startsWith("All")) {
                                successMsg = "Notification sent to " + fSelectedRecipient + " successfully!";
                            }
                            JOptionPane.showMessageDialog(this, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to send notification (unknown response)", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to send notification: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Shows dialog to set the global drop deadline
     */
    private void showSetDropDeadlineDialog() {
        JDialog dialog = createStandardDialog("Set Drop Deadline");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 20", "[fill]"));

        content.add(new JLabel("Drop Deadline (YYYY-MM-DD):"));
        JTextField txtDate = new JTextField();
        txtDate.setText(java.time.LocalDate.now().plusDays(7).toString()); // Default to 1 week from now
        content.add(txtDate, "gapbottom 8");

        JPanel buttons = new JPanel(new MigLayout("align right"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnSet = new JButton("Set Deadline");
        buttons.add(btnCancel);
        buttons.add(btnSet);
        content.add(buttons, "span, growx");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnSet.addActionListener(a -> {
            String dateStr = txtDate.getText().trim();
            if (dateStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Date is required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                // Validate date format
                java.time.LocalDate.parse(dateStr);

                // Call server API
                edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                String result = adminActions.setDropDeadline(dateStr);

                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Drop deadline set to: " + dateStr, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to set deadline: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Shows dialog for database backup
     */
    private void showBackupDialog() {
        JDialog dialog = createStandardDialog("Database Backup");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 20", "[fill]"));

        content.add(new JLabel("Database Backup Tool"));
        content.add(new JLabel("This will create a backup of the entire database."), "gapbottom 8");

        JPanel buttons = new JPanel(new MigLayout("align right"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnCreateBackup = new JButton("Create Backup");
        buttons.add(btnCancel);
        buttons.add(btnCreateBackup);
        content.add(buttons, "span, growx");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnCreateBackup.addActionListener(a -> {
            // Delegate to AdminUiHandlers which implement download behaviour (file chooser + server request)
            dialog.dispose();
            try {
                edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                new edu.univ.erp.ui.handlers.AdminUiHandlers(cu).handleDownloadBackupClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to start backup: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Shows dialog for database restore
     */
    private void showRestoreDialog() {
        JDialog dialog = createStandardDialog("Database Restore");
        JPanel content = new JPanel(new MigLayout("wrap, fillx, insets 20", "[fill]"));

        content.add(new JLabel("Database Restore Tool"));
        content.add(new JLabel("This will restore the database from a backup file."), "gapbottom 8");
        content.add(new JLabel("WARNING: This will overwrite current data!"), "gapbottom 8");

        JPanel buttons = new JPanel(new MigLayout("align right"));
        JButton btnCancel = new JButton("Cancel");
        JButton btnSelectFile = new JButton("Select Backup File");
        buttons.add(btnCancel);
        buttons.add(btnSelectFile);
        content.add(buttons, "span, growx");

        btnCancel.addActionListener(a -> dialog.dispose());
        btnSelectFile.addActionListener(a -> {
            // Delegate to AdminUiHandlers which implement restore behaviour (file chooser + server upload)
            dialog.dispose();
            try {
                edu.univ.erp.domain.UserAuth cu = edu.univ.erp.ClientContext.getCurrentUser();
                new edu.univ.erp.ui.handlers.AdminUiHandlers(cu).handleRestoreBackupClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to start restore: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Initialize maintenance mode button state from server
     */
    private void initializeMaintenanceMode() {
        try {
            edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
            boolean isMaintenanceOn = adminActions.checkMaintenanceMode();

            btnToggleMaintenance.setSelected(isMaintenanceOn);
            if (isMaintenanceOn) {
                btnToggleMaintenance.setText("Maintenance: ON");
                btnToggleMaintenance.setBackground(new Color(220, 38, 38));
                btnToggleMaintenance.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 10; "
                        + "background: #dc2626; "
                        + "borderColor: #dc2626; "
                        + "focusedBackground: #dc2626; "
                        + "hoverBackground: #b91c1c; "
                        + "pressedBackground: #991b1b");
            } else {
                btnToggleMaintenance.setText("Maintenance: OFF");
                btnToggleMaintenance.setBackground(new Color(34, 197, 94));
                btnToggleMaintenance.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 10; "
                        + "background: #22c55e; "
                        + "borderColor: #22c55e; "
                        + "focusedBackground: #22c55e; "
                        + "hoverBackground: #16a34a; "
                        + "pressedBackground: #15803d");
            }
            btnToggleMaintenance.setForeground(Color.WHITE);
            btnToggleMaintenance.setOpaque(true);
            btnToggleMaintenance.repaint();
        } catch (Exception e) {
            System.err.println("Failed to check maintenance mode: " + e.getMessage());
            // Default to OFF if unable to check
            btnToggleMaintenance.setSelected(false);
            btnToggleMaintenance.setText("Maintenance: OFF");
            btnToggleMaintenance.setBackground(new Color(34, 197, 94));
        }
    }

    /**
     * Reload student data from the server
     */
    private void loadStudentData() {
        DefaultTableModel model = (DefaultTableModel) studentTable.getModel();
        loadStudentDataAsync(model);
    }

    /**
     * Reload instructor data from the server
     */
    private void loadInstructorData() {
        try {
            edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
            java.util.List<java.util.Map<String, Object>> instructorList = adminActions.fetchAllInstructors();

            instructors.clear();
            if (instructorList != null) {
                for (java.util.Map<String, Object> inst : instructorList) {
                    String username = inst.get("username") == null ? "" : inst.get("username").toString();
                    String name = inst.get("name") == null ? username : inst.get("name").toString();
                    if (name == null) name = "";
                    String dept = inst.get("department") == null ? "" : inst.get("department").toString();

                    // Build a clean display string. Avoid showing literal "null".
                    String display;
                    if (!dept.isEmpty()) {
                        display = name.isEmpty() ? (username.isEmpty() ? "Unnamed" : username) : name + " (" + dept + ")";
                    } else {
                        display = name.isEmpty() ? (username.isEmpty() ? "Unnamed" : username) : name;
                    }
                    instructors.add(display);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load instructor data: " + e.getMessage());
        }
    }

    // --- Form Lifecycle ---
    @Override
    public void formInitAndOpen() {
        // This is where we add all our button actions (listeners). Called when the form
        // is first shown by the FormManager (matches student dashboard behavior).

        // Initialize maintenance mode state from server
        initializeMaintenanceMode();

        // Start asynchronous load of instructor data so that dialogs which depend on
        // the in-memory `instructors` list won't encounter a race if opened immediately.
        new Thread(() -> {
            try {
                loadInstructorData();
            } catch (Throwable t) {
                System.err.println("Failed async loadInstructorData: " + t.getMessage());
            }
        }).start();

        // "Toggle Maintenance" Button - toggle behavior
        btnToggleMaintenance.addActionListener((ActionEvent e) -> {
            boolean isMaintenanceOn = btnToggleMaintenance.isSelected();

            if (isMaintenanceOn) {
                System.out.println("Client LOG: Setting Maintenance Mode to ON");
                btnToggleMaintenance.setText("Maintenance: ON");
                // Use a more vibrant, solid red color and force FlatLaf to respect it
                btnToggleMaintenance.setBackground(new Color(220, 38, 38));
                btnToggleMaintenance.setForeground(Color.WHITE);
                btnToggleMaintenance.setOpaque(true);
                // Force FlatLaf to use our custom colors
                btnToggleMaintenance.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 10; "
                        + "background: #dc2626; "
                        + "borderColor: #dc2626; "
                        + "focusedBackground: #dc2626; "
                        + "hoverBackground: #b91c1c; "
                        + "pressedBackground: #991b1b");
                btnToggleMaintenance.repaint();

                // Also toggle maintenance mode on server
                try {
                    edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                    adminActions.toggleMaintenance(true);
                    // Update local maintenance manager immediately so UI reflects change
                    edu.univ.erp.ui.components.MaintenanceModeManager.getInstance().setMaintenanceMode(true);
                    updateMaintenanceButtons();
                } catch (Exception ex) {
                    System.err.println("Failed to toggle maintenance mode: " + ex.getMessage());
                }

                JOptionPane.showMessageDialog(this,
                        "Maintenance Mode is now ON.\nStudents/Instructors are view-only.",
                        "Maintenance Mode Enabled",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println("Client LOG: Setting Maintenance Mode to OFF");
                btnToggleMaintenance.setText("Maintenance: OFF");
                btnToggleMaintenance.setBackground(new Color(34, 197, 94));
                btnToggleMaintenance.setForeground(Color.WHITE);
                btnToggleMaintenance.setOpaque(true);
                // Force FlatLaf to use our custom colors
                btnToggleMaintenance.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 10; "
                        + "background: #22c55e; "
                        + "borderColor: #22c55e; "
                        + "focusedBackground: #22c55e; "
                        + "hoverBackground: #16a34a; "
                        + "pressedBackground: #15803d");
                btnToggleMaintenance.repaint();

                // Also toggle maintenance mode on server
                try {
                    edu.univ.erp.ui.actions.AdminActions adminActions = new edu.univ.erp.ui.actions.AdminActions();
                    adminActions.toggleMaintenance(false);
                    // Update local maintenance manager immediately so UI reflects change
                    edu.univ.erp.ui.components.MaintenanceModeManager.getInstance().setMaintenanceMode(false);
                    updateMaintenanceButtons();
                } catch (Exception ex) {
                    System.err.println("Failed to toggle maintenance mode: " + ex.getMessage());
                }

                JOptionPane.showMessageDialog(this,
                        "Maintenance Mode is now OFF.\nNormal operations restored.",
                        "Maintenance Mode Disabled",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // "Send Notification" Button
        btnSendNotification.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showNotificationPopup();
        });

        // Create / admin tool buttons (guarded by maintenance mode)
        btnCreateCourse.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showCreateCourseDialog();
        });
        btnCreateStudent.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showCreateStudentDialog();
        });
        btnCreateInstructor.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showCreateInstructorDialog();
        });
        btnCreateSection.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showCreateSectionDialog();
        });
        btnReassignInstructor.addActionListener(e -> {
            if (!checkMaintenanceAndWarn()) return;
            showReassignInstructorDialog();
        });

        // --- Other admin button implementations ---
        btnSetDeadline.addActionListener(e -> showSetDropDeadlineDialog());
        btnBackup.addActionListener(e -> showBackupDialog());
        btnRestore.addActionListener(e -> showRestoreDialog());
    }

    @Override
    public void formRefresh() {
        // Update background color on theme change
        setBackground(panelBg());

        // Update text color on theme change
        if (lbTitle != null) {
            lbTitle.setForeground(textColor());
        }

        // In a real app, this method would be called to refresh the data
        // in the tables (e.g., studentTable.getModel()...)
        System.out.println("Client LOG: DashboardForm refreshed.");
    }

    @Override
    public boolean formClose() {
        // This can be.t used to ask "Are you sure?" before closing a form.
        // Returning true allows the form to be closed without a prompt.
        return true;
    }

    public String getFormName() {
        return "Dashboard";
    }

    // --- Inner Classes for Aesthetics (from student dashboard file) ---
    /**
     * Generic fading container used to reveal refreshed content smoothly
     */
    private class FadePanel extends JPanel {

        private float alpha;
        private Timer fadeTimer;

        public FadePanel(float initialAlpha) {
            this.alpha = initialAlpha;
            setOpaque(false);
        }

        public void startFadeIn(int delayMs) {
            Runnable starter = () -> {
                if (fadeTimer != null && fadeTimer.isRunning()) {
                    fadeTimer.stop();
                }
                fadeTimer = new Timer(16, ev -> {
                    alpha += (1f - alpha) * 0.18f; // ease-out
                    if (1f - alpha < 0.02f) {
                        alpha = 1f;
                        fadeTimer.stop();
                    }
                    repaint();
                });
                fadeTimer.start();
            };
            if (delayMs > 0) {
                Timer d = new Timer(delayMs, e -> {
                    ((Timer) e.getSource()).stop();
                    starter.run();
                });
                d.setRepeats(false);
                d.start();
            } else {
                starter.run();
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }
    }

    /**
     * A rounded-corner panel that adapts to light/dark themes.
     */
    private class CardPanel extends JPanel {

        public CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Use theme-aware colors
            boolean isDark = FlatLaf.isLafDark();
            Color cardBg = isDark ? new Color(39, 39, 39) : new Color(255, 255, 255);
            Color cardBorder = isDark ? new Color(255, 255, 255, 25) : new Color(0, 0, 0, 15);

            // Soft, subtle shadow to lift cards slightly from the background
            int arc = 20;
            int shadowLayers = 6;
            for (int i = shadowLayers; i >= 1; i--) {
                float alpha = (isDark ? 8 : 10) / 255f; // lighter in dark mode
                int blur = i * 2;
                Color shadow = new Color(0, 0, 0, Math.round(alpha * 255));
                g2.setColor(shadow);
                g2.fill(new RoundRectangle2D.Float(0 + blur, 2 + blur, getWidth() - blur * 2, getHeight() - blur * 2, arc, arc));
            }

            g2.setColor(cardBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));

            g2.setColor(cardBorder);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Modern table styling from instructor dashboard
    private static class ModernTable extends JTable {

        ModernTable(javax.swing.table.TableModel m) {
            super(m);
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean dark = FlatLaf.isLafDark();
            setBackground(dark ? Color.decode("#272727") : Color.WHITE);
            setForeground(dark ? Color.decode("#EAEAEA") : Color.decode("#1E1E1E"));

            // Remove selection colors to eliminate highlighting
            setSelectionBackground(getBackground());
            setSelectionForeground(getForeground());

            setGridColor(dark ? Color.decode("#3C3C3C") : Color.decode("#E5E7EB"));
            javax.swing.table.JTableHeader h = getTableHeader();
            if (h != null) {
                h.setBackground(dark ? Color.decode("#1E1E1E") : Color.decode("#F1F5F9"));
                h.setForeground(dark ? Color.decode("#B3B3B3") : Color.decode("#475569"));
            }
            super.paintComponent(g);
        }
    }

    // Header renderer from instructor dashboard
    private static class SortHeaderRenderer implements javax.swing.table.TableCellRenderer {

        SortHeaderRenderer() {
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            javax.swing.table.JTableHeader header = table.getTableHeader();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(header.getBackground());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 10));
            JLabel text = new JLabel(value == null ? "" : value.toString());
            text.setFont(header.getFont());
            text.setForeground(header.getForeground());
            panel.add(text, BorderLayout.WEST);
            return panel;
        }
    }
}

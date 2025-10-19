package edu.univ.erp.ui.handlers;

import javax.swing.JOptionPane;

import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.actions.AdminActions;


/**
 * UI click handlers for admin actions
 */
public class AdminUiHandlers {

    private final AdminActions adminActions;
    private final UserAuth user;

    public AdminUiHandlers(UserAuth user) {
        this.adminActions = new AdminActions();
        this.user = user;
    }

    // Headless fetch methods for future UI to call
    public java.util.List<Student> fetchAllStudentsReturn() throws Exception {
        return adminActions.fetchAllStudents();
    }

    public java.util.List<CourseCatalog> fetchAllCoursesReturn() throws Exception {
        return adminActions.fetchAllCourses();
    }

    public void handleToggleMaintenanceClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Do you want to TURN ON maintenance mode? (Cancel will turn OFF)",
                    "Maintenance Mode", JOptionPane.YES_NO_CANCEL_OPTION);

            boolean on = choice == JOptionPane.YES_OPTION;
            String response = adminActions.toggleMaintenance(on);
            JOptionPane.showMessageDialog(null,
                    "Maintenance mode updated: " + response,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to toggle maintenance mode: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Toggle Maintenance: " + e.getMessage());
        }
    }

    public void handleSetDropDeadlineClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            boolean on = adminActions.checkMaintenanceMode();
            if (on) {
                JOptionPane.showMessageDialog(null,
                        "Cannot change drop deadline while maintenance mode is active.",
                        "Maintenance Active",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String iso = JOptionPane.showInputDialog(null, "Enter new drop deadline (YYYY-MM-DD):");
            if (iso == null || iso.trim().isEmpty()) return;

            String resp = adminActions.setDropDeadline(iso.trim());
            JOptionPane.showMessageDialog(null, resp, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Set Drop Deadline Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: SetDropDeadline: " + e.getMessage());
        }
    }

    public void handleCreateStudentClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            // Let the server allocate user ID automatically. Use 0 to signal allocation.
            int userId = 0;

            String username = JOptionPane.showInputDialog(null, "Enter Username:");
            if (username == null) return;

            String role = "Student";
            String rollNo = JOptionPane.showInputDialog(null, "Enter Roll Number:");
            if (rollNo == null) return;

            String program = JOptionPane.showInputDialog(null, "Enter Program Name:");
            if (program == null) return;

            String yearStr = JOptionPane.showInputDialog(null, "Enter Year of Study:");
            if (yearStr == null) return;
            int year = Integer.parseInt(yearStr);

            String password = JOptionPane.showInputDialog(null, "Enter Initial Password:");
            if (password == null) return;

            Student newStudent = new Student(userId, username, role, rollNo, program, year);
            String successMsg = adminActions.createStudent(newStudent, password);
            JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Student Creation Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: " + e.getMessage());
        }
    }

    public void handleCreateInstructorClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            // Let the server allocate user ID automatically. Use 0 to signal allocation.
            int userId = 0;

            String username = JOptionPane.showInputDialog(null, "Enter Username:");
            if (username == null) return;

            String name = JOptionPane.showInputDialog(null, "Enter Full Name (optional):");
            if (name == null) name = "";

            String department = JOptionPane.showInputDialog(null, "Enter Department:");
            if (department == null) department = "";

            String role = "Instructor";
            String password = JOptionPane.showInputDialog(null, "Enter Initial Password:");
            if (password == null) return;

            Instructor instructor = new Instructor(userId, username, role, department);
            instructor.setName(name);
            String successMsg = adminActions.createInstructor(instructor, password);
            JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            if (e instanceof NumberFormatException) {
                JOptionPane.showMessageDialog(null, "Invalid User ID entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Instructor Creation Failed", JOptionPane.ERROR_MESSAGE);
                System.err.println("CLIENT ERROR: " + e.getMessage());
            }
        }
    }

    public void handleCreateCourseClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            // Create only the course metadata. Sections are created separately via Create Section.
            String code = JOptionPane.showInputDialog(null, "Enter Course Code:");
            if (code == null) return;

            String title = JOptionPane.showInputDialog(null, "Enter Course Title:");
            if (title == null) return;

            String creditsStr = JOptionPane.showInputDialog(null, "Enter Credits:");
            if (creditsStr == null) return;
            int credits = Integer.parseInt(creditsStr);

            CourseCatalog course = new CourseCatalog(code, title, credits, 0, "", "", 0, 0, "", 0, 0, "");
            try {
                String successMsg = adminActions.createCourseOnly(course);
                JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // Friendly handling for duplicate-course scenarios
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("already exists")) {
                    int choice = JOptionPane.showOptionDialog(null,
                            "A course with this code already exists.\nWould you like to create a section for the existing course or just view it?",
                            "Course Already Exists",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[] {"Create Section", "View Course", "Cancel"},
                            "Create Section");

                    if (choice == 0) {
                        // Launch create section flow pre-filled with course code
                        handleCreateSectionClickPrefill(code);
                    } else if (choice == 1) {
                        // Show course details in a simple message (fetch from server)
                        try {
                            java.util.List<CourseCatalog> courses = adminActions.fetchAllCourses();
                            CourseCatalog found = null;
                            if (courses != null) for (CourseCatalog c : courses) if (code.equalsIgnoreCase(c.getCourseCode())) { found = c; break; }
                                if (found != null) {
                                    // Show detailed view (sections + enrolled counts)
                                    viewCourseDetails(code);
                                } else {
                                    JOptionPane.showMessageDialog(null, "Course exists but could not be fetched.", "Info", JOptionPane.INFORMATION_MESSAGE);
                                }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Failed to fetch course details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        // Cancelled
                    }
                } else {
                    throw e; // rethrow for generic handling below
                }
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Course Creation Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: " + e.getMessage());
        }
    }

    /** Show a simple course details dialog listing sections and enrolled counts. */
    private void viewCourseDetails(String courseCode) {
        try {
            java.util.List<CourseCatalog> courses = adminActions.fetchAllCourses();
            if (courses == null || courses.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No course data available.", "Course Details", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (CourseCatalog c : courses) {
                if (courseCode.equalsIgnoreCase(c.getCourseCode())) {
                    sb.append(String.format("Section ID: %d\nDay/Time: %s\nRoom: %s\nCapacity: %d\nEnrolled: %d\nSemester: %s %d\nInstructor ID: %d\nInstructor Name: %s\n---\n",
                            c.getSectionId(), c.getDayTime(), c.getRoom(), c.getCapacity(), c.getEnrolledCount(), c.getSemester(), c.getYear(), c.getInstructorId(), c.getInstructorName()));
                }
            }

            if (sb.length() == 0) {
                JOptionPane.showMessageDialog(null, "Course has no sections yet.", "Course Details", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, sb.toString(), "Course Details: " + courseCode, JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load course details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Helper to pre-fill course code when creating a section from the duplicate-course dialog. */
    private void handleCreateSectionClickPrefill(String prefillCourseCode) {
        if (!"Admin".equals(user.getRole())) return;
        try {
            String courseCode = prefillCourseCode;

            // Pre-check that course exists (should be true)
            java.util.List<CourseCatalog> courses = adminActions.fetchAllCourses();
            boolean exists = false;
            if (courses != null) {
                for (CourseCatalog c : courses) if (courseCode.equalsIgnoreCase(c.getCourseCode())) { exists = true; break; }
            }
            if (!exists) {
                JOptionPane.showMessageDialog(null, "Course not found on server.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Fetch instructors and present a dropdown (including an Unassigned option)
            java.util.List<java.util.Map<String,Object>> instructors = adminActions.fetchAllInstructors();
            java.util.List<String> instrOptionsList = new java.util.ArrayList<>();
            instrOptionsList.add("0 - Unassigned");
            if (instructors != null) {
                for (java.util.Map<String,Object> m : instructors) {
                    Number uid = (Number) m.get("user_id");
                    String username = m.get("username") == null ? "" : m.get("username").toString();
                    String name = m.get("name") == null ? username : m.get("name").toString();
                    Number assigned = (Number) (m.get("assigned_count") == null ? 0 : m.get("assigned_count"));
                    instrOptionsList.add(String.format("%d - %s (assigned: %d)", uid.intValue(), name, assigned.intValue()));
                }
            }
            String[] instrOptions = instrOptionsList.toArray(new String[0]);
            javax.swing.JComboBox<String> instrBox = new javax.swing.JComboBox<>(instrOptions);
            int instrSel = JOptionPane.showConfirmDialog(null, instrBox, "Select Instructor (or Unassigned)", JOptionPane.OK_CANCEL_OPTION);
            if (instrSel != JOptionPane.OK_OPTION) return;
            int instrId = Integer.parseInt(((String)instrBox.getSelectedItem()).split(" - ")[0]);

            String dayTime = JOptionPane.showInputDialog(null, "Enter Day/Time (e.g., Mon 9-11):");
            if (dayTime == null) dayTime = "";

            String room = JOptionPane.showInputDialog(null, "Enter Room:");
            if (room == null) room = "";

            String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
            if (capacityStr == null) return;
            int capacity = Integer.parseInt(capacityStr);

            String semester = JOptionPane.showInputDialog(null, "Enter Semester:");
            if (semester == null) semester = "";

            String yearStr = JOptionPane.showInputDialog(null, "Enter Year:");
            if (yearStr == null) return;
            int year = Integer.parseInt(yearStr);

            CourseCatalog section = new CourseCatalog(courseCode, "", 0, 0, dayTime, room, capacity, 0, semester, year, instrId, "");
            String resp = adminActions.createSectionOnly(section);
            JOptionPane.showMessageDialog(null, resp, "Create Section", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Create Section Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Create Section prefill: " + e.getMessage());
        }
    }

    /**
     * Create only a section for an existing course.
     */
    public void handleCreateSectionClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            // Present a dropdown of existing courses (code - title)
            java.util.List<CourseCatalog> courses = adminActions.fetchAllCourses();
            if (courses == null || courses.isEmpty()) {
                int create = JOptionPane.showConfirmDialog(null, "No courses found. Would you like to create a course now?", "No Courses", JOptionPane.YES_NO_OPTION);
                if (create == JOptionPane.YES_OPTION) {
                    handleCreateCourseClick();
                }
                return;
            }

            String[] courseOptions = courses.stream()
                    .map(c -> String.format("%s - %s", c.getCourseCode(), c.getCourseTitle()))
                    .toArray(String[]::new);
            javax.swing.JComboBox<String> courseBox = new javax.swing.JComboBox<>(courseOptions);
            int courseChoice = JOptionPane.showConfirmDialog(null, courseBox, "Select Course for New Section", JOptionPane.OK_CANCEL_OPTION);
            if (courseChoice != JOptionPane.OK_OPTION) return;
            String selectedCourse = (String) courseBox.getSelectedItem();
            String courseCode = selectedCourse.split(" - ")[0].trim();

            // Fetch instructors and present a dropdown (including an Unassigned option)
            java.util.List<java.util.Map<String,Object>> instructors = adminActions.fetchAllInstructors();
            java.util.List<String> instrOptionsList = new java.util.ArrayList<>();
            instrOptionsList.add("0 - Unassigned");
            if (instructors != null) {
                for (java.util.Map<String,Object> m : instructors) {
                    Number uid = (Number) m.get("user_id");
                    String username = m.get("username") == null ? "" : m.get("username").toString();
                    String name = m.get("name") == null ? username : m.get("name").toString();
                    Number assigned = (Number) (m.get("assigned_count") == null ? 0 : m.get("assigned_count"));
                    instrOptionsList.add(String.format("%d - %s (assigned: %d)", uid.intValue(), name, assigned.intValue()));
                }
            }
            String[] instrOptions = instrOptionsList.toArray(new String[0]);
            javax.swing.JComboBox<String> instrBox = new javax.swing.JComboBox<>(instrOptions);
            int instrSel = JOptionPane.showConfirmDialog(null, instrBox, "Select Instructor (or Unassigned)", JOptionPane.OK_CANCEL_OPTION);
            if (instrSel != JOptionPane.OK_OPTION) return;
            int instrId = Integer.parseInt(((String)instrBox.getSelectedItem()).split(" - ")[0]);

            String dayTime = JOptionPane.showInputDialog(null, "Enter Day/Time (e.g., Mon 9-11):");
            if (dayTime == null) dayTime = "";

            String room = JOptionPane.showInputDialog(null, "Enter Room:");
            if (room == null) room = "";

            String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
            if (capacityStr == null) return;
            int capacity = Integer.parseInt(capacityStr);

            String semester = JOptionPane.showInputDialog(null, "Enter Semester:");
            if (semester == null) semester = "";

            String yearStr = JOptionPane.showInputDialog(null, "Enter Year:");
            if (yearStr == null) return;
            int year = Integer.parseInt(yearStr);

            CourseCatalog section = new CourseCatalog(courseCode, "", 0, 0, dayTime, room, capacity, 0, semester, year, instrId, "");
            String resp = adminActions.createSectionOnly(section);
            JOptionPane.showMessageDialog(null, resp, "Create Section", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Create Section Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Create Section: " + e.getMessage());
        }
    }

    /**
     * Shows a dialog allowing the admin to choose a section and a new instructor from dropdowns
     * and performs the reassignment via AdminActions.reassignInstructor.
     */
    public void handleReassignInstructorClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            java.util.List<edu.univ.erp.domain.CourseCatalog> courses = adminActions.fetchAllCourses();
            if (courses == null || courses.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No course sections available.", "Reassign Instructor", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            java.util.List<java.util.Map<String,Object>> instructors = adminActions.fetchAllInstructors();
            if (instructors == null || instructors.isEmpty()) {
                int create = JOptionPane.showConfirmDialog(null, "No instructors found. Would you like to create an instructor now?", "No Instructors", JOptionPane.YES_NO_OPTION);
                if (create == JOptionPane.YES_OPTION) {
                    handleCreateInstructorClick();
                    instructors = adminActions.fetchAllInstructors();
                    if (instructors == null || instructors.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Still no instructors available. Aborting.", "Reassign Instructor", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                } else {
                    return;
                }
            }

            String[] courseOptions = courses.stream().map(c -> c.getSectionId() + " - " + c.getCourseCode() + " : " + c.getCourseTitle()).toArray(String[]::new);
            String[] instrOptions = instructors.stream().map(m -> {
                Number uid = (Number) m.get("user_id");
                String username = m.get("username") == null ? "" : m.get("username").toString();
                String name = m.get("name") == null ? username : m.get("name").toString();
                Number assigned = (Number) (m.get("assigned_count") == null ? 0 : m.get("assigned_count"));
                return String.format("%d - %s (assigned: %d)", uid.intValue(), name, assigned.intValue());
            }).toArray(String[]::new);

            javax.swing.JComboBox<String> courseBox = new javax.swing.JComboBox<>(courseOptions);
            javax.swing.JComboBox<String> instrBox = new javax.swing.JComboBox<>(instrOptions);

            Object[] message = {"Select Section:", courseBox, "Select New Instructor:", instrBox};
            int choice = JOptionPane.showConfirmDialog(null, message, "Reassign Instructor", JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) return;

            int sectionId = Integer.parseInt(((String)courseBox.getSelectedItem()).split(" - ")[0]);
            int newInstrId = Integer.parseInt(((String)instrBox.getSelectedItem()).split(" - ")[0]);

            String resp = adminActions.reassignInstructor(sectionId, newInstrId);
            JOptionPane.showMessageDialog(null, resp, "Reassign Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Reassign Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Reassign Instructor: " + e.getMessage());
        }
    }

    /**
     * Helper that enforces a server maintenance pre-check and shows UI if maintenance is active.
     * Returns true if maintenance mode is active (and UI was shown), false otherwise.
     */
    public boolean abortIfMaintenance() {
        try {
            boolean on = adminActions.checkMaintenanceMode();
            if (on) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "The system is currently in maintenance mode. Database-modifying actions are disabled.",
                        "Maintenance Mode Active",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return true;
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    /**
     * Fetch and print all students (console output). Controller delegates here.
     */
    public void displayAllStudents() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            java.util.List<edu.univ.erp.domain.Student> students = adminActions.fetchAllStudents();
            if (students == null || students.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No students found.", "All Students", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-30s %-15s %-20s\n", "ID", "ROLL_NO", "USERNAME", "PROGRAM"));
            sb.append("---------------------------------------------------------------------\n");
            for (edu.univ.erp.domain.Student s : students) {
                sb.append(String.format("%-10d %-30s %-15s %-20s\n",
                        s.getUserId(), s.getRollNo(), s.getUsername(), s.getProgram()));
            }

            javax.swing.JTextArea ta = new javax.swing.JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setColumns(80);
            ta.setRows(Math.min(25, students.size() + 5));
            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(ta);
            JOptionPane.showMessageDialog(null, sp, "All Students", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch students via API: " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Fetch and print all courses/sections (console output). Controller delegates here.
     */
    public void displayAllCourses() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            java.util.List<edu.univ.erp.domain.CourseCatalog> courses = adminActions.fetchAllCourses();
            if (courses == null || courses.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No courses found.", "All Courses", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Build a table for aligned columns
            String[] cols = new String[] { "Code", "Title", "Credits", "Section ID", "Day/Time", "Room", "Capacity", "Enrolled", "Semester", "Year", "Instructor ID", "Instructor" };
            Object[][] data = new Object[courses.size()][cols.length];
            for (int i = 0; i < courses.size(); i++) {
                edu.univ.erp.domain.CourseCatalog c = courses.get(i);
                data[i][0] = c.getCourseCode();
                data[i][1] = c.getCourseTitle();
                data[i][2] = c.getCredits();
                data[i][3] = c.getSectionId();
                data[i][4] = c.getDayTime();
                data[i][5] = c.getRoom();
                data[i][6] = c.getCapacity();
                data[i][7] = c.getEnrolledCount();
                data[i][8] = c.getSemester();
                data[i][9] = c.getYear();
                data[i][10] = c.getInstructorId();
                data[i][11] = c.getInstructorName();
            }

            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            javax.swing.JTable table = new javax.swing.JTable(model);
            table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
            // Set preferred widths for readability
            int[] widths = new int[] {80, 240, 60, 80, 120, 80, 70, 70, 80, 60, 90, 160};
            for (int col = 0; col < widths.length && col < table.getColumnModel().getColumnCount(); col++) {
                table.getColumnModel().getColumn(col).setPreferredWidth(widths[col]);
            }

            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(table);
            sp.setPreferredSize(new java.awt.Dimension(1000, Math.min(600, courses.size() * 25 + 60)));
            JOptionPane.showMessageDialog(null, sp, "All Courses / Sections", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch courses via API: " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a file chooser to download a server-created gzipped DB backup and save it locally.
     */
    public void handleDownloadBackupClick() {
        if (!"Admin".equals(user.getRole())) return;
    javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
    fc.setDialogTitle("Save DB Backup As");
    // Prefill a sensible default filename with timestamp
    String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
    String defaultName = "erp_backup_" + ts + ".gz";
    fc.setSelectedFile(new java.io.File(defaultName));
    int ret = fc.showSaveDialog(null);
        if (ret != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.nio.file.Path out = fc.getSelectedFile().toPath();

        // Show modal progress dialog while downloading
        final javax.swing.JDialog progressDialog = new javax.swing.JDialog((java.awt.Frame) null, "Downloading...", true);
        final javax.swing.JProgressBar pb = new javax.swing.JProgressBar();
        pb.setIndeterminate(true);
        progressDialog.getContentPane().add(pb);
        progressDialog.setSize(300, 60);
        progressDialog.setLocationRelativeTo(null);

        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<>() {
            private Exception error = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = edu.univ.erp.api.ClientRequest.send("DB_BACKUP");
                    if (!resp.startsWith("FILE_DOWNLOAD:")) throw new Exception("Unexpected response: " + resp);
                    String[] parts = resp.split(":", 5);
                    if (parts.length < 5) throw new Exception("Malformed FILE_DOWNLOAD response: " + resp);
                    String payload = parts[4];
                    byte[] decoded = java.util.Base64.getDecoder().decode(payload);
                    java.nio.file.Files.write(out, decoded);
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                if (this.error != null) {
                    javax.swing.JOptionPane.showMessageDialog(null, this.error.getMessage(), "Backup Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                    System.err.println("CLIENT ERROR: DB Backup: " + this.error.getMessage());
                } else {
                    javax.swing.JOptionPane.showMessageDialog(null, "Saved DB backup to " + out.toString(), "Backup Saved", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        worker.execute();
        progressDialog.setVisible(true);
    }

    /**
     * Opens a file chooser to select a gzipped SQL dump file and uploads it to the server for restore.
     */
    public void handleRestoreBackupClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            // Require maintenance mode to be ON for restore
            boolean on = adminActions.checkMaintenanceMode();
            if (!on) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "DB restore requires the system to be in MAINTENANCE mode. Please enable maintenance mode and retry.",
                        "Maintenance Required", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setDialogTitle("Select DB Backup to Restore");
            int ret = fc.showOpenDialog(null);
            if (ret != javax.swing.JFileChooser.APPROVE_OPTION) return;
            java.nio.file.Path in = fc.getSelectedFile().toPath();

            final javax.swing.JDialog progressDialog = new javax.swing.JDialog((java.awt.Frame) null, "Restoring...", true);
            final javax.swing.JProgressBar pb = new javax.swing.JProgressBar();
            pb.setIndeterminate(true);
            progressDialog.getContentPane().add(pb);
            progressDialog.setSize(300, 60);
            progressDialog.setLocationRelativeTo(null);

            javax.swing.SwingWorker<String, Void> worker = new javax.swing.SwingWorker<>() {
                private Exception error = null;

                @Override
                protected String doInBackground() {
                    try {
                        byte[] data = java.nio.file.Files.readAllBytes(in);
                        String b64 = java.util.Base64.getEncoder().encodeToString(data);
                        String request = "DB_RESTORE:BASE64:" + b64;
                        String resp = edu.univ.erp.api.ClientRequest.send(request);
                        return resp;
                    } catch (Exception e) {
                        this.error = e;
                        return null;
                    }
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    if (this.error != null) {
                        javax.swing.JOptionPane.showMessageDialog(null, this.error.getMessage(), "Restore Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                        System.err.println("CLIENT ERROR: DB Restore: " + this.error.getMessage());
                    } else {
                        try {
                            String resp = get();
                            javax.swing.JOptionPane.showMessageDialog(null, resp, "Restore Result", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception e) {
                            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Restore Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            };
            worker.execute();
            progressDialog.setVisible(true);

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Restore Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: DB Restore: " + e.getMessage());
        }
    }
}

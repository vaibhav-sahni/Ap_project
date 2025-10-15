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
            String userIdStr = JOptionPane.showInputDialog(null, "Enter User ID:");
            if (userIdStr == null) return;
            int userId = Integer.parseInt(userIdStr);

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
            String username = JOptionPane.showInputDialog(null, "Enter Username:");
            if (username == null) return;

            String role = "Instructor";
            String password = JOptionPane.showInputDialog(null, "Enter Initial Password:");
            if (password == null) return;

            Instructor instructor = new Instructor(0, username, role, password);
            String successMsg = adminActions.createInstructor(instructor, password);
            JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Instructor Creation Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: " + e.getMessage());
        }
    }

    public void handleCreateCourseClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            String code = JOptionPane.showInputDialog(null, "Enter Course Code:");
            if (code == null) return;

            String title = JOptionPane.showInputDialog(null, "Enter Course Title:");
            if (title == null) return;

            String creditsStr = JOptionPane.showInputDialog(null, "Enter Credits:");
            if (creditsStr == null) return;
            int credits = Integer.parseInt(creditsStr);

            String dayTime = JOptionPane.showInputDialog(null, "Enter Day/Time (e.g., Mon 9-11):");
            if (dayTime == null) dayTime = "";

            String room = JOptionPane.showInputDialog(null, "Enter Room:");
            if (room == null) room = "";

            String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
            if (capacityStr == null) return;
            int capacity = Integer.parseInt(capacityStr);

            String enrolledCountStr = JOptionPane.showInputDialog(null, "Enter Enrolled Count:");
            if (enrolledCountStr == null) return;
            int enrolledCount = Integer.parseInt(enrolledCountStr);

            String semester = JOptionPane.showInputDialog(null, "Enter Semester:");
            if (semester == null) semester = "";

            String yearStr = JOptionPane.showInputDialog(null, "Enter Year:");
            if (yearStr == null) return;
            int year = Integer.parseInt(yearStr);

            String instructorID = JOptionPane.showInputDialog(null, "Enter Instructor ID:");
            if (instructorID == null) return;
            int instrid = Integer.parseInt(instructorID);

            String instr_name = JOptionPane.showInputDialog(null, "Enter Instructor Name:");
            if (instr_name == null) instr_name = "";

            CourseCatalog course = new CourseCatalog(
                    code, title, credits, 0, dayTime, room, capacity,
                    enrolledCount, semester, year, instrid, instr_name
            );

            String successMsg = adminActions.createCourse(course);
            JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Course Creation Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: " + e.getMessage());
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

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-40s %-15s %-10s %-10s\n",
                    "CODE", "TITLE", "INSTRUCTOR", "CAPACITY", "SECTION ID"));
            sb.append("---------------------------------------------------------------------\n");
            for (edu.univ.erp.domain.CourseCatalog c : courses) {
                sb.append(String.format("%-10s %-40s %-15s %-10d %-10d\n",
                        c.getCourseCode(), c.getCourseTitle(), c.getInstructorName(), c.getCapacity(), c.getSectionId()));
            }

            javax.swing.JTextArea ta = new javax.swing.JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setColumns(100);
            ta.setRows(Math.min(25, courses.size() + 5));
            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(ta);
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

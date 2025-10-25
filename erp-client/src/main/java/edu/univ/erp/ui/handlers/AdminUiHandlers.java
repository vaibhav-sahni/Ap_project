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

    /**
     * Show a dialog for admin to compose and send a notification to Students, Instructors or All.
     */
    public void handleSendNotificationClick() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            String[] options = new String[] {"ALL", "STUDENT", "INSTRUCTOR"};
            String recipientType = (String) JOptionPane.showInputDialog(null, "Choose recipient type:", "Send Notification", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (recipientType == null) return;

            int recipientId = 0;
            if (!"ALL".equals(recipientType)) {
                try {
                    if ("STUDENT".equals(recipientType)) {
                        java.util.List<Student> students = fetchAllStudentsReturn();
                        java.util.List<String> opts = new java.util.ArrayList<>();
                        opts.add("0 - All Students");
                        if (students != null) {
                            for (Student s : students) {
                                String uname = s.getUsername() == null ? "" : s.getUsername();
                                String roll = s.getRollNo() == null ? "" : s.getRollNo();
                                opts.add(String.format("%d - %s (%s)", s.getUserId(), uname, roll));
                            }
                        }
                        javax.swing.JComboBox<String> box = new javax.swing.JComboBox<>(opts.toArray(new String[0]));
                        int sel = JOptionPane.showConfirmDialog(null, box, "Select Student", JOptionPane.OK_CANCEL_OPTION);
                        if (sel != JOptionPane.OK_OPTION) return;
                        recipientId = Integer.parseInt(((String)box.getSelectedItem()).split(" - ")[0]);
                    } else if ("INSTRUCTOR".equals(recipientType)) {
                        java.util.List<java.util.Map<String,Object>> instructors = adminActions.fetchAllInstructors();
                        java.util.List<String> opts = new java.util.ArrayList<>();
                        opts.add("0 - All Instructors");
                        if (instructors != null) {
                            for (java.util.Map<String,Object> m : instructors) {
                                Number uid = (Number) m.get("user_id");
                                String username = m.get("username") == null ? "" : m.get("username").toString();
                                String name = m.get("name") == null ? username : m.get("name").toString();
                                opts.add(String.format("%d - %s", uid == null ? 0 : uid.intValue(), name));
                            }
                        }
                        javax.swing.JComboBox<String> box = new javax.swing.JComboBox<>(opts.toArray(new String[0]));
                        int sel = JOptionPane.showConfirmDialog(null, box, "Select Instructor", JOptionPane.OK_CANCEL_OPTION);
                        if (sel != JOptionPane.OK_OPTION) return;
                        recipientId = Integer.parseInt(((String)box.getSelectedItem()).split(" - ")[0]);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Failed to fetch recipients: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String title = JOptionPane.showInputDialog(null, "Enter notification title:");
            if (title == null) return;
            String message = JOptionPane.showInputDialog(null, "Enter notification message:");
            if (message == null) return;

            String resp = adminActions.sendNotification(recipientType, recipientId, title, message);
            JOptionPane.showMessageDialog(null, resp, "Notification Sent", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Send Notification Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: SendNotification: " + e.getMessage());
        }
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

    /** Display all existing sections (one row per section) with basic metadata. */
    public void displayAllSections() {
        if (!"Admin".equals(user.getRole())) return;
        try {
            java.util.List<CourseCatalog> courses = adminActions.fetchAllCourses();
            if (courses == null || courses.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No sections available.", "View Sections", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] cols = new String[] {"Section ID", "Course Code", "Title", "Instructor", "Enrolled/Capacity", "Semester"};
            Object[][] data = new Object[courses.size()][];
            for (int i = 0; i < courses.size(); i++) {
                CourseCatalog c = courses.get(i);
                String instr = c.getInstructorName() == null || c.getInstructorName().trim().isEmpty() ? "Unassigned" : c.getInstructorName();
                String enrolled = c.getEnrolledCount() + "/" + c.getCapacity();
                data[i] = new Object[] { c.getSectionId(), c.getCourseCode(), c.getCourseTitle(), instr, enrolled, c.getSemester() + " " + c.getYear() };
            }

            javax.swing.JTable table = new javax.swing.JTable(data, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(table);
            scroll.setPreferredSize(new java.awt.Dimension(900, 400));
            javax.swing.JOptionPane.showMessageDialog(null, scroll, "All Sections", javax.swing.JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load sections: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: displayAllSections: " + e.getMessage());
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

            String dayTime = showDayTimeDialog();
            if (dayTime == null) dayTime = "";
            // Normalize/validate dayTime on client side before sending to server
            if (!dayTime.trim().isEmpty()) {
                try {
                    dayTime = normalizeDayTimeClient(dayTime);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Invalid day/time: " + e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String room = JOptionPane.showInputDialog(null, "Enter Room:");
            if (room == null) room = "";

            String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
            if (capacityStr == null) return;
            int capacity = Integer.parseInt(capacityStr);

            String[] semOptions = new String[] {"Monsoon", "Winter", "Summer"};
            javax.swing.JComboBox<String> semBox = new javax.swing.JComboBox<>(semOptions);
            int semChoice = JOptionPane.showConfirmDialog(null, semBox, "Select Semester", JOptionPane.OK_CANCEL_OPTION);
            if (semChoice != JOptionPane.OK_OPTION) return;
            String semester = (String) semBox.getSelectedItem();

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

            // Deduplicate courses by course code so the Create Section dropdown
            // shows one entry per course (even if multiple sections exist).
            java.util.LinkedHashMap<String, CourseCatalog> unique = new java.util.LinkedHashMap<>();
            for (CourseCatalog c : courses) {
                String codeKey = c.getCourseCode() == null ? "" : c.getCourseCode().trim().toUpperCase();
                if (!unique.containsKey(codeKey)) {
                    unique.put(codeKey, c);
                }
            }
            String[] courseOptions = unique.values().stream()
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

            String dayTime = showDayTimeDialog();
            if (dayTime == null) dayTime = "";
            // Normalize/validate dayTime on client side before sending to server
            if (!dayTime.trim().isEmpty()) {
                try {
                    dayTime = normalizeDayTimeClient(dayTime);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Invalid day/time: " + e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String room = JOptionPane.showInputDialog(null, "Enter Room:");
            if (room == null) room = "";

            String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
            if (capacityStr == null) return;
            int capacity = Integer.parseInt(capacityStr);

            String[] semOptions = new String[] {"Monsoon", "Winter", "Summer"};
            javax.swing.JComboBox<String> semBox = new javax.swing.JComboBox<>(semOptions);
            int semChoice = JOptionPane.showConfirmDialog(null, semBox, "Select Semester", JOptionPane.OK_CANCEL_OPTION);
            if (semChoice != JOptionPane.OK_OPTION) return;
            String semester = (String) semBox.getSelectedItem();

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

            // Present one row per course (deduplicated by course code) with a sections count
            java.util.Map<String, java.util.List<edu.univ.erp.domain.CourseCatalog>> grouped = new java.util.LinkedHashMap<>();
            for (edu.univ.erp.domain.CourseCatalog c : courses) {
                String key = c.getCourseCode() == null ? "" : c.getCourseCode().trim().toUpperCase();
                grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(c);
            }

            String[] cols = new String[] { "Code", "Title", "Credits", "Sections", "Total Capacity", "Total Enrolled", "Instructors" };
            Object[][] data = new Object[grouped.size()][cols.length];
            int ri = 0;
            for (java.util.List<edu.univ.erp.domain.CourseCatalog> list : grouped.values()) {
                edu.univ.erp.domain.CourseCatalog sample = list.get(0);
                int totalCapacity = 0;
                int totalEnrolled = 0;
                java.util.Set<String> instrs = new java.util.LinkedHashSet<>();
                for (edu.univ.erp.domain.CourseCatalog sc : list) {
                    totalCapacity += sc.getCapacity();
                    totalEnrolled += sc.getEnrolledCount();
                    if (sc.getInstructorName() != null && !sc.getInstructorName().trim().isEmpty()) instrs.add(sc.getInstructorName());
                }
                String instrDisplay = instrs.isEmpty() ? "Unassigned" : String.join(", ", instrs);
                data[ri][0] = sample.getCourseCode();
                data[ri][1] = sample.getCourseTitle();
                data[ri][2] = sample.getCredits();
                data[ri][3] = list.size();
                data[ri][4] = totalCapacity;
                data[ri][5] = totalEnrolled;
                data[ri][6] = instrDisplay;
                ri++;
            }

            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            javax.swing.JTable table = new javax.swing.JTable(model);
            table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
            int[] widths = new int[] {100, 300, 60, 80, 100, 100, 200};
            for (int col = 0; col < widths.length && col < table.getColumnModel().getColumnCount(); col++) {
                table.getColumnModel().getColumn(col).setPreferredWidth(widths[col]);
            }

            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(table);
            sp.setPreferredSize(new java.awt.Dimension(1000, Math.min(600, grouped.size() * 25 + 60)));
            JOptionPane.showMessageDialog(null, sp, "All Courses (deduped)", JOptionPane.INFORMATION_MESSAGE);
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

    /**
     * Show a dialog that lets admin pick days (Mon-Fri) and a fixed time slot.
     * Returns a normalized dayTime string using compact day codes (e.g. "MWF 09:00-10:00" or "TTh 11:00-12:30").
     * Returns null if user cancelled.
     */
    private String showDayTimeDialog() {
        javax.swing.JCheckBox cbMon = new javax.swing.JCheckBox("Mon");
        javax.swing.JCheckBox cbTue = new javax.swing.JCheckBox("Tue");
        javax.swing.JCheckBox cbWed = new javax.swing.JCheckBox("Wed");
        javax.swing.JCheckBox cbThu = new javax.swing.JCheckBox("Thu");
        javax.swing.JCheckBox cbFri = new javax.swing.JCheckBox("Fri");

        String[] slots = new String[] {
            "08:00-09:00",
            "09:00-10:00",
            "10:00-11:00",
            "11:00-12:30",
            "13:00-14:00",
            "14:00-15:30",
            "16:00-17:00"
        };
        javax.swing.JComboBox<String> slotBox = new javax.swing.JComboBox<>(slots);

        javax.swing.JPanel panel = new javax.swing.JPanel(new net.miginfocom.swing.MigLayout("wrap 1", "[grow]"));
        panel.add(new javax.swing.JLabel("Select Days:"));
        javax.swing.JPanel days = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        days.add(cbMon); days.add(cbTue); days.add(cbWed); days.add(cbThu); days.add(cbFri);
        panel.add(days);
        panel.add(new javax.swing.JLabel("Select Time Slot:"));
        panel.add(slotBox);

        int choice = JOptionPane.showConfirmDialog(null, panel, "Select Day(s) and Time Slot", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return null;

        StringBuilder daysCode = new StringBuilder();
        if (cbMon.isSelected()) daysCode.append("M");
        if (cbTue.isSelected()) daysCode.append("T");
        if (cbWed.isSelected()) daysCode.append("W");
        if (cbThu.isSelected()) daysCode.append("Th");
        if (cbFri.isSelected()) daysCode.append("F");

        if (daysCode.length() == 0) {
            JOptionPane.showMessageDialog(null, "Please select at least one day.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String slot = (String) slotBox.getSelectedItem();
        return daysCode.toString() + " " + slot;
    }

    /**
     * Normalize a dayTime string supplied by the UI. Accepts friendly variants and
     * returns a string of the form: "<days> HH:MM-HH:MM". Throws Exception on invalid input.
     * Examples accepted: "MWF 11", "MWF 11-12", "11", "11:30", "TTh 11:00-12:30".
     */
    private String normalizeDayTimeClient(String dayTime) throws Exception {
        if (dayTime == null) throw new Exception("dayTime cannot be null");
        String s = dayTime.trim();
        if (s.isEmpty()) throw new Exception("dayTime cannot be empty");

        String[] parts = s.split("\\s+", 2);
        String daysPart;
        String slotPart;
        if (parts.length == 1) {
            // No explicit days provided. We'll treat this as slot-only and require caller to provide days
            // in the UI; however, accept it by using an empty days part (server expects days present in many flows).
            // To avoid surprising the server, require days to be present here.
            throw new Exception("Please select days (e.g. MWF) and a time slot.");
        } else {
            daysPart = parts[0];
            slotPart = parts[1];
        }

        // Normalize slot: accept variants like "11", "11-12", "11:30", or "11:00-12:30"
        String normalizedSlot;
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("H:mm");
            String[] times = slotPart.split("-", 2);
            if (times.length == 1) {
                String startRaw = times[0].trim();
                if (startRaw.isEmpty()) throw new Exception("Empty time value in slot: " + slotPart);
                String startNorm = startRaw.contains(":") ? startRaw : startRaw + ":00";
                java.time.LocalTime st = java.time.LocalTime.parse(startNorm, fmt);
                java.time.LocalTime et = st.plusHours(1);
                normalizedSlot = String.format("%02d:%02d-%02d:%02d", st.getHour(), st.getMinute(), et.getHour(), et.getMinute());
            } else {
                String startRaw = times[0].trim();
                String endRaw = times[1].trim();
                if (startRaw.isEmpty() || endRaw.isEmpty()) throw new Exception("Empty start or end time in slot: " + slotPart);
                String startNorm = startRaw.contains(":") ? startRaw : startRaw + ":00";
                String endNorm = endRaw.contains(":") ? endRaw : endRaw + ":00";
                java.time.LocalTime st = java.time.LocalTime.parse(startNorm, fmt);
                java.time.LocalTime et = java.time.LocalTime.parse(endNorm, fmt);
                if (!st.isBefore(et)) throw new Exception("Start time must be before end time in slot: " + slotPart);
                normalizedSlot = String.format("%02d:%02d-%02d:%02d", st.getHour(), st.getMinute(), et.getHour(), et.getMinute());
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new Exception("Invalid time values in slot: " + slotPart, ex);
        }

        // Validate daysPart tokens
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < daysPart.length(); i++) {
            char c = daysPart.charAt(i);
            if (c == 'M') tokens.add("M");
            else if (c == 'W') tokens.add("W");
            else if (c == 'F') tokens.add("F");
            else if (c == 'T') {
                if (i + 1 < daysPart.length() && daysPart.charAt(i + 1) == 'h') {
                    tokens.add("Th"); i++; 
                } else tokens.add("T");
            } else {
                throw new Exception("Invalid day code character: '" + c + "' in days part: " + daysPart);
            }
        }
        if (tokens.isEmpty()) throw new Exception("No day codes found in days part: " + daysPart);

        StringBuilder normalizedDays = new StringBuilder();
        for (String t : tokens) normalizedDays.append(t);
        return normalizedDays.toString() + " " + normalizedSlot;
    }
}

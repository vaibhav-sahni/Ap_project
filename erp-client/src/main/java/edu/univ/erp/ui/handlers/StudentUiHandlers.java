package edu.univ.erp.ui.handlers;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.actions.StudentActions;

/**
 * UI click handlers for student-related UI actions.
 */
public class StudentUiHandlers {

    private final StudentActions studentActions;
    private final UserAuth user;

    public StudentUiHandlers(UserAuth user) {
        this.studentActions = new StudentActions();
        this.user = user;
    }

    /**
     * Programmatic fetch methods (headless) for UI to consume later.
     */
    public java.util.List<Grade> fetchGrades() throws Exception {
        return studentActions.getMyGrades(user.getUserId());
    }

    public java.util.List<CourseCatalog> fetchCatalog() throws Exception {
        return studentActions.getCourseCatalog();
    }

    public java.util.List<CourseCatalog> fetchTimetable() throws Exception {
        return studentActions.getTimetable(user.getUserId());
    }

    public String fetchTranscriptHtml() throws Exception {
        return studentActions.downloadTranscript(user.getUserId());
    }

    public String registerCourseReturn(int sectionId) throws Exception {
        return studentActions.registerCourse(user.getUserId(), sectionId);
    }

    public String dropCourseReturn(int sectionId) throws Exception {
        return studentActions.dropCourse(user.getUserId(), sectionId);
    }

    public void handleDownloadTranscriptClick() {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can download transcripts.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String htmlContent = studentActions.downloadTranscript(user.getUserId());
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Student Transcript");
            String defaultFileName = "transcript_user_" + user.getUserId() + "_" + System.currentTimeMillis() + ".html";
            fileChooser.setSelectedFile(new File(defaultFileName));
            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try (FileWriter writer = new FileWriter(fileToSave)) {
                    writer.write(htmlContent);
                    JOptionPane.showMessageDialog(null,
                            "Transcript successfully downloaded and saved to:\n" + fileToSave.getAbsolutePath(),
                            "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Error saving file locally: " + e.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE);
                    throw e;
                }
            } else {
                JOptionPane.showMessageDialog(null, "Transcript download cancelled.", "Download Status", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Download Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Transcript Download Failed: " + errorMsg);
        }
    }

    /**
     * Initialize student dashboard flows (grades, catalog, timetable). Any UI errors
     * will be shown from this handler so the controller remains UI-free.
     */
    public void initStudentDashboard() {
        try {
            // Fetch and display grades
            List<Grade> grades = studentActions.getMyGrades(user.getUserId());
            if (grades != null) {
                System.out.println("\n--- DETAILED GRADES RECEIVED ---");
                if (grades.isEmpty()) System.out.println("No grade data received.");
                else grades.forEach(g -> {
                    System.out.println("\nCourse: " + g.getCourseName() + " (Final Grade: " + g.getFinalGrade() + ")");
                    System.out.println("  Components:");
                    if (g.getComponents() == null || g.getComponents().isEmpty()) {
                        System.out.println("    - No components recorded.");
                    } else {
                        for (AssessmentComponent c : g.getComponents()) {
                            System.out.printf("       - %-20s: %.2f\n", c.getComponentName(), c.getScore());
                        }
                    }
                });
            }

            // Fetch and display catalog
            List<CourseCatalog> catalog = studentActions.getCourseCatalog();
            System.out.println("\n--- COURSE CATALOG RECEIVED ---");
            if (catalog == null || catalog.isEmpty()) {
                System.out.println("No courses available in the catalog.");
            } else {
                System.out.printf("%-10s %-40s %-15s %-10s %s\n", "CODE", "TITLE", "INSTRUCTOR", "CAPACITY", "TIME");
                System.out.println("-------------------------------------------------------------------------------------------------");
                catalog.forEach(c -> System.out.printf("%-10s %-40s %-15s %-10s %s\n",
                        c.getCourseCode(), c.getCourseTitle(), c.getInstructorName(), c.getEnrolledCount() + "/" + c.getCapacity(), c.getDayTime()));
            }

            // Fetch and display timetable
            List<CourseCatalog> schedule = studentActions.getTimetable(user.getUserId());
            System.out.println("\n--- CURRENT STUDENT TIMETABLE ---");
            if (schedule == null || schedule.isEmpty()) {
                System.out.println("You are not currently registered for any courses.");
            } else {
                System.out.printf("%-10s %-40s %-15s %-10s %s\n", "SECTION", "COURSE TITLE", "INSTRUCTOR", "ROOM", "TIME/DAY");
                System.out.println("-------------------------------------------------------------------------------------------------");
                schedule.forEach(c -> System.out.printf("%-10s %-40s %-15s %-10s %s\n", c.getSectionId(), c.getCourseTitle(), c.getInstructorName(), c.getRoom(), c.getDayTime()));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Student dashboard init: " + e.getMessage());
        }
    }

    public void handleRegisterCourseClick(int sectionId) {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can register for courses.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String successMsg = studentActions.registerCourse(user.getUserId(), sectionId);
            JOptionPane.showMessageDialog(null, successMsg, "Registration Success", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("CLIENT LOG: Registration Success: " + successMsg);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Registration Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Registration Failed: " + errorMsg);
        }
    }

    public void handleDropCourseClick(int sectionId) {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can drop courses.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to drop section ID " + sectionId + "? This action is final.",
                    "Confirm Course Drop", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            String successMsg = studentActions.dropCourse(user.getUserId(), sectionId);
            JOptionPane.showMessageDialog(null, successMsg, "Course Drop Success", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("CLIENT LOG: Drop Success: " + successMsg);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Course Drop Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Drop Failed: " + errorMsg);
        }
    }

    public void refreshCatalogAndTimetable() {
        try {
            List<CourseCatalog> catalog = studentActions.getCourseCatalog();
            List<CourseCatalog> schedule = studentActions.getTimetable(user.getUserId());
            // print minimal refresh summary
            System.out.println("CLIENT LOG: Refreshed catalog (" + (catalog==null?0:catalog.size()) + ") and timetable (" + (schedule==null?0:schedule.size()) + ")");
        } catch (Exception e) {
            System.err.println("ERROR: Refresh failed: " + e.getMessage());
        }
    }

    public void displayGrades() {
        try {
            List<Grade> grades = studentActions.getMyGrades(user.getUserId());
            if (grades == null || grades.isEmpty()) {
                System.out.println("No grades to display.");
                return;
            }
            grades.forEach(g -> {
                System.out.println("Course: " + g.getCourseName() + " Final: " + g.getFinalGrade());
                if (g.getComponents()!=null) {
                    g.getComponents().forEach(c -> System.out.printf("  - %s: %.2f\n", c.getComponentName(), c.getScore()));
                }
            });
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Student display grades: " + e.getMessage());
        }
    }

    public void displayCatalog() {
        try {
            List<CourseCatalog> catalog = studentActions.getCourseCatalog();
            if (catalog == null || catalog.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No courses available in the catalog.", "Course Catalog", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-40s %-15s %-10s %s\n", "CODE", "TITLE", "INSTRUCTOR", "CAPACITY", "TIME"));
            sb.append("-----------------------------------------------------------------------------------------------\n");
            for (CourseCatalog c : catalog) {
                sb.append(String.format("%-10s %-40s %-15s %-10s %s\n",
                        c.getCourseCode(), c.getCourseTitle(), c.getInstructorName(), c.getEnrolledCount() + "/" + c.getCapacity(), c.getDayTime()));
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(800, 400));
            JOptionPane.showMessageDialog(null, sp, "Course Catalog", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch course catalog via API: " + e.getMessage());
        }
    }

    public void displayTimetable() {
        try {
            List<CourseCatalog> schedule = studentActions.getTimetable(user.getUserId());
            if (schedule == null || schedule.isEmpty()) {
                JOptionPane.showMessageDialog(null, "You are not currently registered for any courses.", "Timetable", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-40s %-15s %-10s %s\n", "SECTION", "COURSE TITLE", "INSTRUCTOR", "ROOM", "TIME/DAY"));
            sb.append("-----------------------------------------------------------------------------------------------\n");
            for (CourseCatalog c : schedule) {
                sb.append(String.format("%-10s %-40s %-15s %-10s %s\n", c.getSectionId(), c.getCourseTitle(), c.getInstructorName(), c.getRoom(), c.getDayTime()));
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(800, 400));
            JOptionPane.showMessageDialog(null, sp, "Student Timetable", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch student timetable via API: " + e.getMessage());
        }
    }
}

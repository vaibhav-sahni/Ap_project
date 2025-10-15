package edu.univ.erp.ui.handlers;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.actions.InstructorActions;

/**
 * UI click handlers for instructor-related actions.
 */
public class InstructorUiHandlers {

    private final InstructorActions instructorActions;
    private final UserAuth user;

    public InstructorUiHandlers(UserAuth user) {
        this.instructorActions = new InstructorActions();
        this.user = user;
    }

    public void displayRoster(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = instructorActions.getRoster(instructorId, sectionId);
            if (roster == null || roster.isEmpty()) {
                System.out.println("Roster is empty for this section.");
                return;
            }

            System.out.println("\n--- ROSTER FOR SECTION ID: " + sectionId + " ---");
            System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
            System.out.println("-------------------------------------------------------------------------------------------------");

            roster.forEach(r -> {
                System.out.printf("%-10d %-30s %-10s %-10s\n",
                        r.getEnrollmentId(),
                        r.getStudentName(),
                        r.getRollNo(),
                        r.getFinalGrade());
            });
            System.out.println("-------------------------------------------------------------------------------------------------");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Roster Fetch Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Roster Fetch Failed: " + e.getMessage());
        }
    }

    public void computeFinalGradesWithUi(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = javax.swing.JOptionPane.showConfirmDialog(null,
                "Are you sure you want to compute final grades for all students in section " + sectionId + "? " +
                        "This action will finalize grades and mark enrollments as Completed.",
                "Confirm Final Grading",
                javax.swing.JOptionPane.YES_NO_OPTION);

        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            System.out.println("CLIENT LOG: Final grading cancelled by user.");
            return;
        }

        try {
            java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = instructorActions.getRoster(instructorId, sectionId);
            if (roster == null || roster.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(null, "No students in this section.", "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            System.out.println("\n--- COMPUTING FINAL GRADES FOR SECTION ID: " + sectionId + " ---");
            for (edu.univ.erp.domain.EnrollmentRecord student : roster) {
                try {
                    int enrollmentId = student.getEnrollmentId();
                    String finalGradeMsg = instructorActions.computeFinalGrade(instructorId, enrollmentId);
                    System.out.println("CLIENT LOG: Final Grade Computed for " + student.getStudentName() +
                            " (EID " + enrollmentId + "): " + finalGradeMsg);
                } catch (Exception e) {
                    System.err.println("CLIENT ERROR: Failed to compute grade for " + student.getStudentName() + ": " + e.getMessage());
                }
            }

            roster = instructorActions.getRoster(instructorId, sectionId);
            System.out.println("\n--- UPDATED ROSTER AFTER FINAL GRADING ---");
            System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
            System.out.println("-------------------------------------------------------------------------------------------------");
            roster.forEach(r -> {
                System.out.printf("%-10d %-30s %-10s %-10s\n",
                        r.getEnrollmentId(),
                        r.getStudentName(),
                        r.getRollNo(),
                        r.getFinalGrade());
            });
            System.out.println("-------------------------------------------------------------------------------------------------");

            javax.swing.JOptionPane.showMessageDialog(null,
                    "Final grades computed for all " + roster.size() + " students in section " + sectionId + ".",
                    "Final Grading Complete",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Final Grading Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Final Grading Failed: " + e.getMessage());
        }
    }

    public java.util.List<edu.univ.erp.domain.Section> displayAssignedSections(int instructorId) {
        if (!"Instructor".equals(user.getRole())) return java.util.Collections.emptyList();
        try {
            java.util.List<edu.univ.erp.domain.Section> assignedSections = instructorActions.getAssignedSections(instructorId);
            if (assignedSections.isEmpty()) {
                System.out.println("You are not currently assigned to teach any sections.");
            } else {
                System.out.println("\n--- ASSIGNED SECTIONS FOR INSTRUCTOR ---");
                System.out.printf("%-10s %-40s %-15s %s\n", "SECTION ID", "COURSE NAME", "COURSE CODE", "ENROLLED/CAPACITY");
                System.out.println("-------------------------------------------------------------------------------------------------");
                for (edu.univ.erp.domain.Section s : assignedSections) {
                    int enrolledCount = instructorActions.getRoster(instructorId, s.getSectionId()).size();
                    String enrolledCapacity = enrolledCount + "/" + s.getCapacity();
                    System.out.printf("%-10s %-40s %-15s %s\n", s.getSectionId(), s.getCourseName(), s.getCourseCode(), enrolledCapacity);
                }
                System.out.println("--------------------------------\n");
            }
            return assignedSections;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Failed to fetch assigned sections via API: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // Headless fetch method for UI previews/tests
    public java.util.List<edu.univ.erp.domain.EnrollmentRecord> fetchRoster(int instructorId, int sectionId) throws Exception {
        return instructorActions.getRoster(instructorId, sectionId);
    }
}

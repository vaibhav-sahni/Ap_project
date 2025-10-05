package edu.univ.erp.service.student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List; // <-- NEW IMPORT: Required for drop deadline check

import edu.univ.erp.dao.course.CourseDAO;
import edu.univ.erp.dao.enrollment.EnrollmentDAO; 
import edu.univ.erp.dao.grade.GradeDAO;
import edu.univ.erp.dao.grade.GradeDAO.RawGradeResult;
import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;

public class StudentService {
    
    private final GradeDAO gradeDAO = new GradeDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    // ----------------------------------------------------------------------
    // --- 1. COURSE DROP FEATURE (NEW) -------------------------------------
    // ----------------------------------------------------------------------

    /**
     * Drops a student from a course section, applying all business rules.
     * Rule #1: Check Drop Deadline.
     * Rule #2: Must be currently 'Registered' (enforced by the DAO).
     * @return A success message.
     */
    public String dropCourse(int userId, int sectionId) throws Exception {
        if (userId <= 0 || sectionId <= 0) {
            throw new IllegalArgumentException("Invalid Student ID or Section ID provided.");
        }
        
        // --- 1. Check Drop Deadline (Rule #1) ---
        // Hardcoded Placeholder: Use a mock deadline for demonstration.
        // In a real system, this date would be fetched from a configuration table.
        LocalDate dropDeadline = LocalDate.of(2025, 10, 31); 
        if (LocalDate.now().isAfter(dropDeadline)) {
            throw new Exception("The official course drop deadline has passed (" + dropDeadline + "). Drop failed.");
        }
        
        // --- 2. Execute the Drop (Rule #2 enforced in DAO) ---
        // The DAO method attempts to update status from 'Registered' to 'Dropped'.
        int affectedRows = enrollmentDAO.dropStudent(userId, sectionId);
        
        if (affectedRows == 0) {
            // This happens if the student was never registered, already dropped, or the IDs were invalid.
            throw new Exception("Enrollment record not found or course was not in 'Registered' status.");
        }
        
        System.out.println("SERVER LOG: Student " + userId + " successfully dropped section " + sectionId);
        return "Successfully dropped section ID: " + sectionId + ".";
    }

    // ----------------------------------------------------------------------
    // --- 2. ENROLLMENT FEATURE (EXISTING) ---------------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Registers a student in a specific course section, applying all business rules.
     */
    public String registerCourse(int userId, int sectionId) throws Exception {
        if (userId <= 0 || sectionId <= 0) {
            throw new IllegalArgumentException("Invalid Student ID or Section ID.");
        }
        
        // --- 1. Check Capacity ---
        int remainingSeats = enrollmentDAO.getRemainingCapacity(sectionId);
        if (remainingSeats <= 0) {
            throw new Exception("Section is full. Registration failed.");
        }
        
        // --- 2. Check Existing Enrollment ---
        if (enrollmentDAO.isStudentRegistered(userId, sectionId)) {
            throw new Exception("You are already registered in this section.");
        }

        // --- 3. Check Prerequisites (Placeholder for future feature) ---
        // if (!PrerequisiteDAO.check(userId, sectionId)) {
        //     throw new Exception("Prerequisites not met for this course.");
        // }

        // --- 4. Check Time Conflict (Simplified Logic) ---
        CourseCatalog targetSection = courseDAO.getCatalogItemBySectionId(sectionId); 
        if (targetSection == null) {
            throw new Exception("Target section data not found.");
        }
        
        List<String> currentScheduleTimes = enrollmentDAO.getStudentScheduleDayTimes(userId);
        String newSectionDayTime = targetSection.getDayTime();

        // SIMPLIFIED CONFLICT CHECK: Check if the new time overlaps with any existing time string
        for (String existingTime : currentScheduleTimes) {
            if (hasTimeConflict(existingTime, newSectionDayTime)) {
                throw new Exception("Time conflict detected with an existing course: " + existingTime);
            }
        }
        
        // --- 5. Final Registration Transaction ---
        enrollmentDAO.registerStudent(userId, sectionId);
        
        System.out.println("SERVER LOG: Student " + userId + " successfully registered in section " + sectionId);
        return "Successfully registered for section ID: " + sectionId + " (" + targetSection.getCourseCode() + ")";
    }
    
    /**
     * VERY SIMPLIFIED: Placeholder for robust time conflict logic. 
     */
    private boolean hasTimeConflict(String existingTime, String newTime) {
        return false; 
    }
    
    // ----------------------------------------------------------------------
    // --- 3. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------

    public List<CourseCatalog> fetchCourseCatalog() throws Exception {
        System.out.println("SERVER LOG: Fetching full course catalog.");
        List<CourseCatalog> catalog = courseDAO.getCourseCatalog();
        System.out.println("SERVER LOG: Successfully retrieved " + catalog.size() + " sections for the catalog.");
        return catalog;
    }


    // ----------------------------------------------------------------------
    // --- 4. GRADES FEATURE (EXISTING) -------------------------------------
    // ----------------------------------------------------------------------
    
    public List<Grade> fetchGrades(int userId) throws Exception {
        if (userId <= 0) {
            throw new Exception("Invalid user ID provided.");
        }
        
        List<RawGradeResult> rawResults = gradeDAO.getRawGradeResultsByUserId(userId); 
        List<Grade> finalGrades = new ArrayList<>();
        
        for (RawGradeResult raw : rawResults) {
            List<AssessmentComponent> components = gradeDAO.getComponentsByEnrollmentId(raw.enrollmentId());
            finalGrades.add(new Grade(
                raw.courseTitle(),
                raw.finalGrade(),
                components
            ));
        }
        return finalGrades;
    }
}
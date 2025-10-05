package edu.univ.erp.service.student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List; 

import edu.univ.erp.dao.course.CourseDAO;
import edu.univ.erp.dao.enrollment.EnrollmentDAO; 
import edu.univ.erp.dao.grade.GradeDAO;
import edu.univ.erp.dao.grade.GradeDAO.RawGradeResult;
import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.util.TranscriptFormatter; 

public class StudentService {
    
    private final GradeDAO gradeDAO = new GradeDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    // ----------------------------------------------------------------------
    // --- 1. COURSE DROP FEATURE (EXISTING) --------------------------------
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
        LocalDate dropDeadline = LocalDate.of(2025, 10, 31); 
        if (LocalDate.now().isAfter(dropDeadline)) {
            throw new Exception("The official course drop deadline has passed (" + dropDeadline + "). Drop failed.");
        }
        
        // --- 2. Execute the Drop (Rule #2 enforced in DAO) ---
        int affectedRows = enrollmentDAO.dropStudent(userId, sectionId);
        
        if (affectedRows == 0) {
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
        //      throw new Exception("Prerequisites not met for this course.");
        // }

        // --- 4. Check Time Conflict (Simplified Logic) ---
        CourseCatalog targetSection = courseDAO.getCatalogItemBySectionId(sectionId); 
        if (targetSection == null) {
            throw new Exception("Target section data not found.");
        }
        
        List<String> currentScheduleTimes = enrollmentDAO.getStudentScheduleDayTimes(userId);
        String newSectionDayTime = targetSection.getDayTime();

        // SIMPLIFIED CONFLICT CHECK
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
    // --- 3. TIMETABLE FEATURE (NEW) ---------------------------------------
    // ----------------------------------------------------------------------

    /**
     * Fetches the current course schedule/timetable for the specified student.
     * @param userId The ID of the student.
     * @return A list of CourseCatalog objects representing the student's schedule.
     */
    public List<CourseCatalog> fetchTimetable(int userId) throws Exception {
        if (userId <= 0) {
            throw new Exception("Invalid user ID provided for timetable request.");
        }
        System.out.println("SERVER LOG: Fetching timetable for student " + userId);
        
        List<CourseCatalog> schedule = courseDAO.getStudentTimetable(userId);
        
        System.out.println("SERVER LOG: Timetable retrieved. Contains " + schedule.size() + " sections.");
        return schedule;
    }


    // ----------------------------------------------------------------------
    // --- 4. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------

    public List<CourseCatalog> fetchCourseCatalog() throws Exception {
        System.out.println("SERVER LOG: Fetching full course catalog.");
        List<CourseCatalog> catalog = courseDAO.getCourseCatalog();
        System.out.println("SERVER LOG: Successfully retrieved " + catalog.size() + " sections for the catalog.");
        return catalog;
    }


    // ----------------------------------------------------------------------
    // --- 5. GRADES FEATURE (EXISTING) -------------------------------------
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

    // ----------------------------------------------------------------------
    // --- 6. DOWNLOAD TRANSCRIPT FEATURE (NEW) -----------------------------
    // ----------------------------------------------------------------------

    /**
     * Fetches all student grades and formats them into a CSV string for export.
     * @param userId The ID of the student.
     * @return The entire transcript formatted as a CSV string.
     */
    public String downloadTranscript(int userId) throws Exception {
        List<Grade> grades = fetchGrades(userId); // Uses your existing, working data fetch method

    // CRITICAL CHANGE: Instantiate and call the HTML generator method
        TranscriptFormatter formatter = new TranscriptFormatter();
        return formatter.generateHtml(grades, userId);
    }
}
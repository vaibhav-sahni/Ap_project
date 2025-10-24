package edu.univ.erp.service.student;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import edu.univ.erp.dao.course.CourseDAO;
import edu.univ.erp.dao.enrollment.EnrollmentDAO; 
import edu.univ.erp.dao.grade.GradeDAO;
import edu.univ.erp.dao.grade.GradeDAO.RawGradeResult;
import edu.univ.erp.dao.settings.SettingDAO;
import edu.univ.erp.dao.student.StudentDAO;
import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.util.GradeUtils;
import edu.univ.erp.util.TranscriptFormatter; 

public class StudentService {
    private static final Logger LOGGER = Logger.getLogger(StudentService.class.getName());
    
    private final GradeDAO gradeDAO = new GradeDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    // ----------------------------------------------------------------------
    // --- 1. COURSE DROP FEATURE (EXISTING) --------------------------------
    // ----------------------------------------------------------------------

    /**
     * Drops a student from a course section, applying all business rules.
     * Rule #1: Check Drop Deadline.
     * Rule #2: Must be currently 'Registered' (enforced by the DAO).
     * @return A success message.
     */
    /**
     * Drops a student from a course section. Performs owner/admin check using actorUserId.
     * @param actorUserId the user id of the caller (must be the student or an admin)
     */
    public String dropCourse(int actorUserId, int userId, int sectionId) throws Exception {
        // Service-level maintenance check (defense-in-depth)
        if (settingDAO.isMaintenanceModeOn()) {
            throw new Exception("The system is currently under maintenance. Please try later.");
        }
        // Authorization: caller must be the same student or an admin
        edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
        if (actorUserId != userId && !checker.isAdmin(actorUserId)) {
            throw new Exception("NOT_AUTHORIZED:Only the student or admins may drop a course.");
        }
        if (userId <= 0 || sectionId <= 0) {
            throw new IllegalArgumentException("Invalid Student ID or Section ID provided.");
        }
        
        // --- 1. Check Drop Deadline (Rule #1) ---
        String deadlineStr = settingDAO.getSetting("DROP_DEADLINE"); // e.g., "2025-10-31"
        LocalDate dropDeadline;
        if (deadlineStr != null) {
            dropDeadline = LocalDate.parse(deadlineStr); // expects YYYY-MM-DD format
        } else {
            dropDeadline = LocalDate.of(2025, 10, 31); // fallback
        }

        if (LocalDate.now().isAfter(dropDeadline)) {
            throw new Exception("The official course drop deadline has passed (" + dropDeadline + "). Drop failed.");
        }
        
        // --- 2. Execute the Drop (Rule #2 enforced in DAO) ---
    int affectedRows = enrollmentDAO.dropStudent(userId, sectionId);
        
        if (affectedRows == 0) {
            throw new Exception("Enrollment record not found or course was not in 'Registered' status.");
        }
        
        LOGGER.info("Student " + userId + " successfully dropped section " + sectionId);
        return "Successfully dropped section ID: " + sectionId + ".";
    }

    // ----------------------------------------------------------------------
    // --- 2. ENROLLMENT FEATURE (EXISTING) ---------------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Registers a student in a specific course section, applying all business rules.
     */
    /**
     * Registers a student in a specific course section. actorUserId must be the student or an admin.
     */
    public String registerCourse(int actorUserId, int userId, int sectionId) throws Exception {
        // Service-level maintenance check (defense-in-depth)
        if (settingDAO.isMaintenanceModeOn()) {
            throw new Exception("The system is currently under maintenance. Please try later.");
        }
        if (userId <= 0 || sectionId <= 0) {
            throw new IllegalArgumentException("Invalid Student ID or Section ID.");
        }
        // Authorization: caller must be the same student or an admin
        edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
        if (actorUserId != userId && !checker.isAdmin(actorUserId)) {
            throw new Exception("NOT_AUTHORIZED:Only the student or admins may register for a course.");
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

        // --- 2b. Prevent registering for another section of the same course ---
        if (enrollmentDAO.isRegisteredForSameCourseInAnotherSection(userId, sectionId)) {
            throw new Exception("You are already registered in another section of this course.");
        }

        // --- 2c. Prevent registering again for a course already completed in any section ---
        if (enrollmentDAO.hasCompletedCourseForSameCode(userId, sectionId)) {
            throw new Exception("You have completed this course");
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
        
        java.util.Map<Integer, String> currentSchedule = enrollmentDAO.getStudentScheduleEntries(userId);
        String newSectionDayTime = targetSection.getDayTime();

        // Robust conflict check: check every registered section for overlap even on a single day
        for (java.util.Map.Entry<Integer, String> e : currentSchedule.entrySet()) {
            Integer existingSectionId = e.getKey();
            String existingTime = e.getValue();
            if (hasTimeConflict(existingTime, newSectionDayTime)) {
                // Get course info for the conflicting section so client can display useful details
                CourseCatalog existing = courseDAO.getCatalogItemBySectionId(existingSectionId);
                String existingCourseCode = existing == null ? "" : existing.getCourseCode();
                String existingCourseTitle = existing == null ? "" : existing.getCourseTitle();
                // Return a structured conflict error so client can parse it
                String json = "{\"type\":\"time_conflict\"," +
                        "\"existingSectionId\":" + existingSectionId + "," +
                        "\"existingDayTime\":\"" + escapeForJson(existingTime) + "\"," +
                        "\"existingCourseCode\":\"" + escapeForJson(existingCourseCode) + "\"," +
                        "\"existingCourseTitle\":\"" + escapeForJson(existingCourseTitle) + "\"}";
                throw new Exception("CONFLICT:" + json);
            }
        }
        
        // --- 5. Final Registration Transaction ---
        enrollmentDAO.registerStudent(userId, sectionId);
        
        LOGGER.info("Student " + userId + " successfully registered in section " + sectionId);
        return "Successfully registered for section ID: " + sectionId + " (" + targetSection.getCourseCode() + ")";
    }
    
    /**
     * VERY SIMPLIFIED: Placeholder for robust time conflict logic. 
     */
    private boolean hasTimeConflict(String existingTime, String newTime) {
        if (existingTime == null || newTime == null) return false;

        try {
            // Split into optional day part and time part. Examples:
            // "Mon/Wed 09:00-10:30" => days="Mon/Wed", times="09:00-10:30"
            String[] exParts = existingTime.trim().split("\\s+", 2);
            String[] newParts = newTime.trim().split("\\s+", 2);

            String exDaysPart = exParts.length == 2 ? exParts[0] : "";
            String exTimePart = exParts.length == 2 ? exParts[1] : exParts[0];

            String newDaysPart = newParts.length == 2 ? newParts[0] : "";
            String newTimePart = newParts.length == 2 ? newParts[1] : newParts[0];

            Set<String> exDays = parseDays(exDaysPart);
            Set<String> newDays = parseDays(newDaysPart);

            // If neither provides an explicit day, conservatively assume possible overlap (treat as overlap candidate)
            boolean daysOverlap = false;
            if (exDays.isEmpty() || newDays.isEmpty()) {
                daysOverlap = true;
            } else {
                for (String d : exDays) {
                    if (newDays.contains(d)) { daysOverlap = true; break; }
                }
            }

            if (!daysOverlap) return false;

            // Parse time ranges: "HH:mm-HH:mm" or "H:mm-H:mm"
            String[] exTimes = exTimePart.split("-", 2);
            String[] newTimes = newTimePart.split("-", 2);
            if (exTimes.length < 2 || newTimes.length < 2) return false;
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("H:mm");

            LocalTime exStart = LocalTime.parse(exTimes[0].trim(), fmt);
            LocalTime exEnd = LocalTime.parse(exTimes[1].trim(), fmt);
            LocalTime newStart = LocalTime.parse(newTimes[0].trim(), fmt);
            LocalTime newEnd = LocalTime.parse(newTimes[1].trim(), fmt);

            // Overlap exists unless one ends before the other starts
            return !(exEnd.compareTo(newStart) <= 0 || newEnd.compareTo(exStart) <= 0);
        } catch (Exception e) {
            // Parsing errors: log and conservatively assume no conflict to avoid blocking valid registrations.
            LOGGER.warning(() -> "Could not parse timetable strings for conflict check: '" + existingTime + "' vs '" + newTime + "' - " + e.getMessage());
            return false;
        }
    }

    private Set<String> parseDays(String daysPart) {
        Set<String> days = new HashSet<>();
        if (daysPart == null) return days;
        String cleaned = daysPart.trim();
        if (cleaned.isEmpty()) return days;

        // Accept separators '/', ',', or whitespace
        String[] tokens = cleaned.split("[\\/\\,\\s]+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            // If token looks like compact codes e.g., MWF, TTh, parse char-by-char
            if (t.matches("(?i)^[MTWThfhr]+$")) {
                // iterate and detect 'Th' as Thursday
                for (int i = 0; i < t.length(); i++) {
                    char c = t.charAt(i);
                    if (c == 'M' || c == 'm') {
                        days.add("mon");
                    } else if (c == 'W' || c == 'w') {
                        days.add("wed");
                    } else if (c == 'F' || c == 'f') {
                        days.add("fri");
                    } else if (c == 'T' || c == 't') {
                        // Could be 'Th' for Thursday
                        if (i + 1 < t.length() && (t.charAt(i + 1) == 'h' || t.charAt(i + 1) == 'H')) {
                            days.add("thu");
                            i++; // skip the 'h'
                        } else {
                            days.add("tue");
                        }
                    } else if (c == 'h' || c == 'H') {
                        // standalone 'h' unlikely, skip
                        continue;
                    } else {
                        // fallback to generic handling below
                        String norm = t.substring(0, Math.min(3, t.length())).toLowerCase();
                        switch (norm) {
                            case "mon": days.add("mon"); break;
                            case "tue": days.add("tue"); break;
                            case "wed": days.add("wed"); break;
                            case "thu": days.add("thu"); break;
                            case "fri": days.add("fri"); break;
                            case "sat": days.add("sat"); break;
                            case "sun": days.add("sun"); break;
                            default:
                                days.add(norm);
                        }
                        break;
                    }
                }
            } else {
                String norm = t.substring(0, Math.min(3, t.length())).toLowerCase();
                // normalize common day names/abbreviations
                switch (norm) {
                    case "mon": days.add("mon"); break;
                    case "tue": days.add("tue"); break;
                    case "wed": days.add("wed"); break;
                    case "thu": days.add("thu"); break;
                    case "fri": days.add("fri"); break;
                    case "sat": days.add("sat"); break;
                    case "sun": days.add("sun"); break;
                    default:
                        // unknown token: attempt to use first three letters
                        days.add(norm);
                }
            }
        }
        return days;
    }

    /** Escape a string to be safely embedded in a JSON string value (very small helper). */
    private static String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
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
    LOGGER.info("Fetching timetable for student " + userId);
        
        List<CourseCatalog> schedule = courseDAO.getStudentTimetable(userId);
        
        LOGGER.info("Timetable retrieved. Contains " + schedule.size() + " sections.");
        return schedule;
    }


    // ----------------------------------------------------------------------
    // --- 4. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------

    public List<CourseCatalog> fetchCourseCatalog() throws Exception {
        LOGGER.info("Fetching full course catalog.");
        List<CourseCatalog> catalog = courseDAO.getCourseCatalog();
        LOGGER.info("Successfully retrieved " + catalog.size() + " sections for the catalog.");
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

        // Instantiate and call the HTML generator method
        TranscriptFormatter formatter = new TranscriptFormatter();
        String rollno = StudentDAO.getStudentRollNo(userId);
        return formatter.generateHtml(grades, rollno);
    }

    /**
     * Computes the credit-weighted CGPA for a student using a 10-point scale.
     * Only courses with a recorded final grade (non-null and not "IP") are counted.
     * Returns Double.NaN if no completed courses are found.
     */
    public double computeCgpa(int userId) throws Exception {
        if (userId <= 0) throw new IllegalArgumentException("Invalid userId");

        List<RawGradeResult> rawResults = gradeDAO.getRawGradeResultsByUserId(userId);

        double totalWeightedPoints = 0.0;
        double totalCredits = 0.0;

    // Quiet: avoid verbose info logs during CGPA computation

        for (RawGradeResult raw : rawResults) {
            String finalGrade = raw.finalGrade();
            double credits = raw.credits();

            Double points = GradeUtils.gradeToPoints(finalGrade);
            // per-enrollment debug information suppressed
            if (points == null) {
                // skip courses with no final grade or in-progress or unknown format
                continue;
            }

            // If credits is zero or negative, skip (avoid affecting denominator)
            if (credits <= 0.0) continue;

            totalWeightedPoints += points * credits;
            totalCredits += credits;
        }

        if (totalCredits <= 0.0) {
            return Double.NaN; // indicate no CGPA available
        }

        double cgpa = totalWeightedPoints / totalCredits;
        // Round to 2 decimal places
        return Math.round(cgpa * 100.0) / 100.0;
    }

    /**
     * Computes total credits earned by the student.
     * A course counts as earned if it has a recorded final grade and the grade maps to > 0 points.
     * Returns 0.0 when none are earned.
     */
    public double computeTotalCreditsEarned(int userId) throws Exception {
        if (userId <= 0) throw new IllegalArgumentException("Invalid userId");

        List<RawGradeResult> rawResults = gradeDAO.getRawGradeResultsByUserId(userId);

        double totalEarned = 0.0;
    // Quiet: avoid verbose info logs during credits computation
        for (RawGradeResult raw : rawResults) {
            String finalGrade = raw.finalGrade();
            double credits = raw.credits();
            Double points = GradeUtils.gradeToPoints(finalGrade);
            // per-enrollment debug information suppressed
            if (points == null) continue; // skip IP or unknown
            if (points > 0.0 && credits > 0.0) {
                totalEarned += credits;
            }
        }
        // Round to 2 decimals for consistency
        return Math.round(totalEarned * 100.0) / 100.0;
    }
}
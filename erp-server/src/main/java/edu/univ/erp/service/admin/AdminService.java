package edu.univ.erp.service.admin;

import java.util.List;

import edu.univ.erp.dao.admin.AdminDAO;
import edu.univ.erp.dao.settings.SettingDAO;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;

public class AdminService {

    private final AdminDAO adminDAO = new AdminDAO();

    /** Reassigns the instructor for a given section. Only called by admin layer. */
    public String reassignInstructor(int sectionId, int newInstructorId) throws Exception {
        // Validate new instructor exists
        if (!adminDAO.instructorExists(newInstructorId)) {
            throw new Exception("Instructor with id " + newInstructorId + " does not exist.");
        }

        boolean ok = adminDAO.reassignInstructor(sectionId, newInstructorId);
        if (!ok) throw new Exception("Failed to reassign instructor. Check sectionId and instructorId.");
        // optional: could return previous instructor id if DAO returned it
        return "Instructor reassigned successfully.";
    }
    private final SettingDAO settingDAO = new SettingDAO(); // For maintenance toggle

    // --------------------------
    // 1. CREATE NEW STUDENT
    // --------------------------
    public String createStudent(Student student, String passwordHash) throws Exception {
        if (student == null) throw new Exception("Student object cannot be null.");
        if (student.getUserId() == 0 || student.getRollNo() == null) {
            throw new Exception("Student must have userId and roll number.");
        }

        boolean created = adminDAO.createStudent(student, passwordHash);
        if (!created) throw new Exception("Failed to create student. Check DB constraints.");
        return "Student created successfully: " + student.getRollNo();
    }

    // --------------------------
    // 2. CREATE COURSE AND SECTION
    // --------------------------
    public String createCourseAndSection(CourseCatalog course) throws Exception {
        if (course == null) throw new Exception("CourseCatalog object cannot be null.");
        if (course.getCourseCode() == null || course.getSectionId() != 0) {
            throw new Exception("Invalid course/section data.");
        }

        // Validate instructor exists before creating section
        if (course.getInstructorId() != 0 && !adminDAO.instructorExists(course.getInstructorId())) {
            throw new Exception("Instructor with id " + course.getInstructorId() + " does not exist.");
        }

        // Validate day/time format for the section if provided. CourseCatalog is immutable
        // (no setters), so construct a normalized copy when needed.
        if (course.getDayTime() != null && !course.getDayTime().trim().isEmpty()) {
            String normalized = validateAndNormalizeDayTime(course.getDayTime());
            course = new CourseCatalog(
                    course.getCourseCode(), course.getCourseTitle(), course.getCredits(),
                    course.getSectionId(), normalized, course.getRoom(), course.getCapacity(),
                    course.getEnrolledCount(), course.getSemester(), course.getYear(),
                    course.getInstructorId(), course.getInstructorName(), course.getEnrollmentStatus()
            );
        }

        boolean created = adminDAO.createCourseAndSection(course);
        if (!created) throw new Exception("Failed to create course and section.");
        return "Course and section created successfully: " + course.getCourseCode();
    }

    /** Create only the course record (no section) */
    public String createCourse(CourseCatalog course) throws Exception {
        if (course == null) throw new Exception("CourseCatalog object cannot be null.");
        if (course.getCourseCode() == null) throw new Exception("Course must have a course code.");
        // Reject duplicate course codes
        if (adminDAO.courseExists(course.getCourseCode())) {
            throw new Exception("Course with code '" + course.getCourseCode() + "' already exists.");
        }

        boolean created = adminDAO.createCourse(course);
        if (!created) throw new Exception("Failed to create course. Check DB constraints.");
        return "Course created successfully: " + course.getCourseCode();
    }

    /** Create only a new section for an existing course */
    public String createSection(CourseCatalog course) throws Exception {
        if (course == null) throw new Exception("CourseCatalog object cannot be null.");
        if (course.getCourseCode() == null) throw new Exception("Section must reference a course code.");

        // Validate instructor exists before creating section
        if (course.getInstructorId() != 0 && !adminDAO.instructorExists(course.getInstructorId())) {
            throw new Exception("Instructor with id " + course.getInstructorId() + " does not exist.");
        }

        // Ensure the referenced course exists
        if (!adminDAO.courseExists(course.getCourseCode())) {
            throw new Exception("Cannot create section: course with code '" + course.getCourseCode() + "' does not exist.");
        }

        // Validate/normalize dayTime before creating the section. Use a normalized copy
        if (course.getDayTime() != null && !course.getDayTime().trim().isEmpty()) {
            String normalized = validateAndNormalizeDayTime(course.getDayTime());
            course = new CourseCatalog(
                    course.getCourseCode(), course.getCourseTitle(), course.getCredits(),
                    course.getSectionId(), normalized, course.getRoom(), course.getCapacity(),
                    course.getEnrolledCount(), course.getSemester(), course.getYear(),
                    course.getInstructorId(), course.getInstructorName(), course.getEnrollmentStatus()
            );
        }

        boolean created = adminDAO.createSection(course);
        if (!created) throw new Exception("Failed to create section. Check DB constraints.");
        return "Section created successfully for course: " + course.getCourseCode();
    }

    /**
     * Validate and normalize a dayTime string produced by the admin UI.
     * Expected form: compact day codes (M, T, W, Th, F) concatenated, a space,
     * then a time slot in 24-hour HH:MM-HH:MM. Examples: "MWF 09:00-10:00", "TTh 11:00-12:30".
     * Returns a normalized string (trimmed) or throws Exception on invalid input.
     */
    private String validateAndNormalizeDayTime(String dayTime) throws Exception {
        if (dayTime == null) throw new Exception("dayTime cannot be null");
        String s = dayTime.trim();
        if (s.isEmpty()) throw new Exception("dayTime cannot be empty");

        String[] parts = s.split("\\s+", 2);
        if (parts.length < 2) throw new Exception("Invalid day/time format. Expected e.g. 'MWF 09:00-10:00'.");
        String daysPart = parts[0];
        String slotPart = parts[1];

        // Normalize and validate slot. Accept several friendly formats such as:
        //  - "11" -> treated as 11:00-12:00
        //  - "11:30" -> treated as 11:30-12:30
        //  - "11-12" -> treated as 11:00-12:00
        //  - "11:00-12:30" -> kept as-is
        // The goal is to be permissive on input while storing a normalized HH:MM-HH:MM slot.
        String normalizedSlot;
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("H:mm");
            String[] times = slotPart.split("-", 2);
            if (times.length == 1) {
                // Single token: treat as start time and add 1 hour for end
                String startRaw = times[0].trim();
                if (startRaw.isEmpty()) throw new Exception("Empty time value in slot: " + slotPart);
                String startNorm = startRaw.contains(":") ? startRaw : startRaw + ":00";
                java.time.LocalTime st = java.time.LocalTime.parse(startNorm, fmt);
                java.time.LocalTime et = st.plusHours(1);
                normalizedSlot = String.format("%02d:%02d-%02d:%02d", st.getHour(), st.getMinute(), et.getHour(), et.getMinute());
            } else if (times.length == 2) {
                String startRaw = times[0].trim();
                String endRaw = times[1].trim();
                if (startRaw.isEmpty() || endRaw.isEmpty()) throw new Exception("Empty start or end time in slot: " + slotPart);
                String startNorm = startRaw.contains(":") ? startRaw : startRaw + ":00";
                String endNorm = endRaw.contains(":") ? endRaw : endRaw + ":00";
                java.time.LocalTime st = java.time.LocalTime.parse(startNorm, fmt);
                java.time.LocalTime et = java.time.LocalTime.parse(endNorm, fmt);
                if (!st.isBefore(et)) throw new Exception("Start time must be before end time in slot: " + slotPart);
                normalizedSlot = String.format("%02d:%02d-%02d:%02d", st.getHour(), st.getMinute(), et.getHour(), et.getMinute());
            } else {
                throw new Exception("Invalid time slot format. Expected single time or range. Got: " + slotPart);
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new Exception("Invalid time values in slot: " + slotPart, ex);
        }

        // Parse daysPart into tokens (M, T, W, Th, F) in a single left-to-right scan
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < daysPart.length(); i++) {
            char c = daysPart.charAt(i);
            if (c == 'M') tokens.add("M");
            else if (c == 'W') tokens.add("W");
            else if (c == 'F') tokens.add("F");
            else if (c == 'T') {
                // Could be T or Th
                if (i + 1 < daysPart.length() && daysPart.charAt(i + 1) == 'h') {
                    tokens.add("Th");
                    i++; // skip 'h'
                } else {
                    tokens.add("T");
                }
            } else {
                throw new Exception("Invalid day code character: '" + c + "' in days part: " + daysPart);
            }
        }
        if (tokens.isEmpty()) throw new Exception("No day codes found in days part: " + daysPart);

        // Reconstruct normalized days string using same token order
        StringBuilder normalizedDays = new StringBuilder();
        for (String t : tokens) normalizedDays.append(t);

    return normalizedDays.toString() + " " + normalizedSlot;
    }

    // --------------------------
    // 3. TOGGLE MAINTENANCE
    // --------------------------
    public void toggleMaintenance(boolean on) throws Exception {
        try {
            // Call the DAO which now throws SQLException on failure
            settingDAO.setMaintenanceMode(on);
        } catch (Exception e) {
            throw new Exception("Failed to update maintenance mode: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the global drop deadline in settings (expects YYYY-MM-DD).
     */
    public void setDropDeadline(String isoDate) throws Exception {
        // basic validation
        try {
            java.time.LocalDate.parse(isoDate);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Exception("Invalid date format for drop deadline. Expected YYYY-MM-DD.", e);
        }

        try {
            settingDAO.setSetting("DROP_DEADLINE", isoDate);
        } catch (Exception e) {
            throw new Exception("Failed to update drop deadline: " + e.getMessage(), e);
        }
    }

    public boolean isMaintenanceModeOn() {
    return settingDAO.isMaintenanceModeOn();
    }

    // --------------------------
    // 4. FETCH ALL COURSES
    // --------------------------
    public List<CourseCatalog> getAllCourses() throws Exception {
        return adminDAO.fetchAllCourses();
    }

    // --------------------------
    // 5. FETCH ALL STUDENTS
    // --------------------------
    public List<Student> getAllStudents() throws Exception {
        return adminDAO.fetchAllStudents();
    }

    public java.util.List<java.util.Map<String,Object>> getAllInstructors() throws Exception {
        return adminDAO.fetchAllInstructors();
    }

    /** Returns next available user id for new accounts. */
    public int getNextUserId() {
        return adminDAO.getNextUserId();
    }

    public String createInstructor(Instructor instructor, String passwordHash) throws Exception {
    if (instructor == null) throw new Exception("Instructor object cannot be null.");
    if (instructor.getUserId() == 0) throw new Exception("Instructor must have a valid user ID.");

    boolean created = adminDAO.createInstructor(instructor, passwordHash);
    if (!created) throw new Exception("Failed to create instructor. Check DB constraints.");
    return "Instructor created successfully: " + instructor.getName();
}
}

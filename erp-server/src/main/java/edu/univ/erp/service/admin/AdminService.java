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

        boolean created = adminDAO.createSection(course);
        if (!created) throw new Exception("Failed to create section. Check DB constraints.");
        return "Section created successfully for course: " + course.getCourseCode();
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

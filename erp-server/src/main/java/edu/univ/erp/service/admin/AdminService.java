package edu.univ.erp.service.admin;

import java.util.List;

import edu.univ.erp.dao.admin.AdminDAO;
import edu.univ.erp.dao.settings.SettingDAO;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;

public class AdminService {

    private final AdminDAO adminDAO = new AdminDAO();
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

        boolean created = adminDAO.createCourseAndSection(course);
        if (!created) throw new Exception("Failed to create course and section.");
        return "Course and section created successfully: " + course.getCourseCode();
    }

    // --------------------------
    // 3. TOGGLE MAINTENANCE
    // --------------------------
    public void toggleMaintenance(boolean on) throws Exception {
        try {
            // Call the existing DAO method (throws SQLException)
            settingDAO.setMaintenanceMode(on);
        } catch (Exception e) {
            throw new Exception("Failed to update maintenance mode: " + e.getMessage(), e);
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

    public String createInstructor(Instructor instructor, String passwordHash) throws Exception {
    if (instructor == null) throw new Exception("Instructor object cannot be null.");
    if (instructor.getUserId() == 0) throw new Exception("Instructor must have a valid user ID.");

    boolean created = adminDAO.createInstructor(instructor, passwordHash);
    if (!created) throw new Exception("Failed to create instructor. Check DB constraints.");
    return "Instructor created successfully: " + instructor.getName();
}
}

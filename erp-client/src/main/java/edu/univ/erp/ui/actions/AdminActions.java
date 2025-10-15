package edu.univ.erp.ui.actions;

import java.util.List;

import edu.univ.erp.api.admin.AdminAPI;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;

/**
 * Headless action wrappers for admin-related operations.
 *
 * These methods perform API calls and return data structures or messages.
 * All UI (dialogs, confirmations) should live in the ui.handlers package.
 */
public class AdminActions {

    private final AdminAPI adminApi = new AdminAPI();

    /**
     * Fetch all students from the server. Headless: does not print or show dialogs.
     */
    public List<Student> fetchAllStudents() throws Exception {
        return adminApi.getAllStudents();
    }

    /**
     * Fetch all courses/sections from the server. Headless: does not print or show dialogs.
     */
    public List<CourseCatalog> fetchAllCourses() throws Exception {
        return adminApi.getAllCourses();
    }

    public String createStudent(Student student, String password) throws Exception {
        return adminApi.createStudent(student, password);
    }

    public String createInstructor(Instructor instructor, String password) throws Exception {
        return adminApi.createInstructor(instructor, password);
    }

    public String createCourse(CourseCatalog course) throws Exception {
        return adminApi.createCourseAndSection(course);
    }

    public String toggleMaintenance(boolean on) throws Exception {
        return adminApi.toggleMaintenance(on);
    }

    public String setDropDeadline(String iso) throws Exception {
        return adminApi.setDropDeadline(iso);
    }

    public boolean checkMaintenanceMode() throws Exception {
        return adminApi.checkMaintenanceMode();
    }
}

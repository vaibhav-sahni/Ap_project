package edu.univ.erp.ui.actions;

import java.util.List;

import edu.univ.erp.api.student.StudentAPI;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;

/**
 * UI-facing wrapper for student-related operations. Keeps StudentAPI usage
 * centralized and makes the controller easier to read and test.
 */
public class StudentActions {

    private final StudentAPI studentApi = new StudentAPI();

    public List<Grade> getMyGrades(int userId) throws Exception {
        return studentApi.getMyGrades(userId);
    }

    public List<CourseCatalog> getTimetable(int userId) throws Exception {
        return studentApi.getTimetable(userId);
    }

    public List<CourseCatalog> getCourseCatalog() throws Exception {
        return studentApi.getCourseCatalog();
    }

    public String registerCourse(int userId, int sectionId) throws Exception {
        return studentApi.registerCourse(userId, sectionId);
    }

    public String dropCourse(int userId, int sectionId) throws Exception {
        return studentApi.dropCourse(userId, sectionId);
    }

    public String downloadTranscript(int userId) throws Exception {
        return studentApi.downloadTranscript(userId);
    }

    // Actions are now headless. The UI/handlers will render results.
}

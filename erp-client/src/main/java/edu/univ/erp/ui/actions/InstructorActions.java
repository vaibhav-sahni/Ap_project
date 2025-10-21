package edu.univ.erp.ui.actions;

import java.util.List;

import edu.univ.erp.api.instructor.InstructorAPI;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Section;

/**
 * UI-facing wrapper for instructor-related operations.
 */
public class InstructorActions {

    private final InstructorAPI instructorApi = new InstructorAPI();

    public List<Section> getAssignedSections(int instructorId) throws Exception {
        return instructorApi.getAssignedSections(instructorId);
    }

    public List<EnrollmentRecord> getRoster(int instructorId, int sectionId) throws Exception {
        return instructorApi.getRoster(instructorId, sectionId);
    }

    public String computeFinalGrade(int instructorId, int enrollmentId) throws Exception {
        return instructorApi.computeFinalGrade(instructorId, enrollmentId);
    }
    
    public String recordScore(int instructorId, int enrollmentId, String componentName, double score) throws Exception {
        return instructorApi.recordScore(instructorId, enrollmentId, componentName, score);
    }
    
    public String exportGradesCsv(int instructorId, int sectionId) throws Exception {
        return instructorApi.exportGrades(instructorId, sectionId);
    }

    public String importGradesCsv(int instructorId, int sectionId, String csvContent) throws Exception {
        return instructorApi.importGrades(instructorId, sectionId, csvContent);
    }
    
}

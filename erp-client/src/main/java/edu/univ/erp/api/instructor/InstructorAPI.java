package edu.univ.erp.api.instructor;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Section;

/**
 * Client-side API for Instructor functionality.
 * Handles serializing instructor requests into command strings and 
 * deserializing the JSON responses from the server's ClientHandler.
 * Uses the established edu.univ.erp.api.ClientRequest for network I/O.
 */
public class InstructorAPI {
    private final Gson gson = new Gson();

    // ----------------------------------------------------------------------
    // --- 1. SECTION VIEW FEATURE ------------------------------------------
    // ----------------------------------------------------------------------

    /**
     * Fetches all sections assigned to a specific instructor.
     * Command: GET_INSTRUCTOR_SECTIONS:instructorId
     */
    public List<Section> getAssignedSections(int instructorId) throws Exception {
        String request = "GET_INSTRUCTOR_SECTIONS:" + instructorId;
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<Section>>() {}.getType();
            return gson.fromJson(json, listType);
        } else {
            // ClientRequest.send() handles the generic ERROR, but we ensure to throw if it wasn't a SUCCESS
            throw new Exception("Failed to retrieve assigned sections from server.");
        }
    }

    /**
     * Fetches the student roster and current grades for a given section.
     * Command: GET_ROSTER:instructorId:sectionId
     */
    public List<EnrollmentRecord> getRoster(int instructorId, int sectionId) throws Exception {
        String request = String.format("GET_ROSTER:%d:%d", instructorId, sectionId);
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<EnrollmentRecord>>() {}.getType();
            return gson.fromJson(json, listType);
        } else {
            throw new Exception("Failed to retrieve roster from server.");
        }
    }

    // ----------------------------------------------------------------------
    // --- 2. GRADING FEATURES ----------------------------------------------
    // ----------------------------------------------------------------------

    /**
     * Records or updates a score for a single assessment component.
     * Command: RECORD_SCORE:instructorId:enrollmentId:componentName:score
     */
    public String recordScore(int instructorId, int enrollmentId, String componentName, double score) throws Exception {
        // Use String.format for precise double output
        String request = String.format("RECORD_SCORE:%d:%d:%s:%.2f", 
            instructorId, enrollmentId, componentName, score);
            
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length()); // Returns the success message
        } else {
            throw new Exception("Grade recording failed due to an unexpected server response.");
        }
    }

    /**
     * Computes the final weighted grade and records the letter grade.
     * Command: COMPUTE_FINAL_GRADE:instructorId:enrollmentId
     */
    public String computeFinalGrade(int instructorId, int enrollmentId) throws Exception {
        String request = String.format("COMPUTE_FINAL_GRADE:%d:%d", instructorId, enrollmentId);
        
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length()); // Returns the final grade message
        } else {
            throw new Exception("Final grade computation failed due to an unexpected server response.");
        }
    }
}

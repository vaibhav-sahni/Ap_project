package edu.univ.erp.api.student; 

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;

public class StudentAPI { 
    private final Gson gson = new Gson();

    // ----------------------------------------------------------------------
    // --- 1. ENROLLMENT / DROP FEATURES (UPDATED) --------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Sends a request to the server to register the student in a specific section.
     * Command: REGISTER:userId:sectionId
     * @return A success message from the server.
     */
    public String registerCourse(int userId, int sectionId) throws Exception {
        
        // 1. Build the command string
        String request = "REGISTER:" + userId + ":" + sectionId;

        // 2. Send the request
        String response = ClientRequest.send(request);

        // 3. Process the response
        if (response.startsWith("SUCCESS:")) {
            // Return the success message relayed from the StudentService
            return response.substring("SUCCESS:".length());
        } 
        
        throw new Exception("Enrollment failed due to an unexpected server response.");
    }
    
    /**
     * Sends a request to the server to drop the student from a specific section.
     * Command: DROP_SECTION:userId:sectionId
     * @param userId The ID of the student.
     * @param sectionId The ID of the course section to drop.
     * @return A success message from the server.
     * @throws Exception If the server returns an ERROR message (e.g., deadline passed, not registered, maintenance mode).
     */
    public String dropCourse(int userId, int sectionId) throws Exception {
        
        // 1. Build the command string
        String request = "DROP_SECTION:" + userId + ":" + sectionId;

        // 2. Send the request
        String response = ClientRequest.send(request);

        // 3. Process the response
        if (response.startsWith("SUCCESS:")) {
            // Return the success message relayed from the StudentService
            return response.substring("SUCCESS:".length());
        } 
        
        // ClientRequest.send() handles the generic ERROR, but we ensure to throw if it wasn't a SUCCESS
        throw new Exception("Course drop failed due to an unexpected server response.");
    }


    // ----------------------------------------------------------------------
    // --- 2. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Sends a request to the server to fetch the entire course catalog.
     */
    public List<CourseCatalog> getCourseCatalog() throws Exception {
        
        String request = "GET_CATALOG"; 
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String catalogJson = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<CourseCatalog>>() {}.getType();
            return gson.fromJson(catalogJson, listType);
        } 
        
        throw new Exception("Unexpected response during course catalog request.");
    }

    // ----------------------------------------------------------------------
    // --- 3. GRADES FEATURE (EXISTING) -------------------------------------
    // ----------------------------------------------------------------------

    public List<Grade> getMyGrades(int userId) throws Exception {
        String request = "GET_GRADES:" + userId;
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String gradesJson = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<Grade>>() {}.getType();
            return gson.fromJson(gradesJson, listType);
        } 
        throw new Exception("Failed to receive grades from server.");
    }
}
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

    // NEW COMMAND: Used to fetch the transcript CSV file content.
    private static final String CMD_DOWNLOAD_TRANSCRIPT = "DOWNLOAD_TRANSCRIPT";

    // ----------------------------------------------------------------------
    // --- 1. ENROLLMENT / DROP FEATURES (EXISTING) -------------------------
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
    // --- 2. TIMETABLE FEATURE (EXISTING) ----------------------------------
    // ----------------------------------------------------------------------

    /**
     * Sends a request to the server to fetch the student's current course schedule.
     * Command: GET_TIMETABLE:userId
     * @param userId The ID of the student.
     * @return A list of CourseCatalog objects representing the student's schedule.
     */
    public List<CourseCatalog> getTimetable(int userId) throws Exception {
        String request = "GET_TIMETABLE:" + userId; 
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String scheduleJson = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<CourseCatalog>>() {}.getType();
            return gson.fromJson(scheduleJson, listType);
        } 
        
        throw new Exception("Failed to retrieve timetable from server.");
    }


    // ----------------------------------------------------------------------
    // --- 3. COURSE CATALOG FEATURE (EXISTING) -----------------------------
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
    // --- 4. GRADES FEATURE (EXISTING) -------------------------------------
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

    // ----------------------------------------------------------------------
    // --- 5. DOWNLOAD TRANSCRIPT FEATURE (NEW) -----------------------------
    // ----------------------------------------------------------------------

    /**
     * Requests the full student transcript as a CSV string.
     * Command: DOWNLOAD_TRANSCRIPT:userId
     * @param userId The ID of the student.
     * @return The entire transcript content as a raw CSV string.
     */
    public String downloadTranscript(int userId) throws Exception {
        String request = "DOWNLOAD_TRANSCRIPT:" + userId; 
        
        // The server sends back a single raw string containing the file content or error.
        String response = ClientRequest.send(request);
        
        if (response.startsWith("ERROR:")) {
            // Propagate the specific error message sent by the server
            throw new Exception(response.substring("ERROR:".length()).trim());
            
        } else if (response.startsWith("FILE_DOWNLOAD:")) { // CRITICAL FIX: Look for FILE_DOWNLOAD protocol
            
            // The response format is: FILE_DOWNLOAD:content_type:filename:file_content
            // We need to find the third colon (the end of the header) and return everything after it.
            
            int firstColon = response.indexOf(':');
            int secondColon = response.indexOf(':', firstColon + 1);
            int thirdColon = response.indexOf(':', secondColon + 1);
            
            if (thirdColon == -1) {
                // Should not happen if server follows protocol, but handles malformed response
                throw new Exception("Malformed FILE_DOWNLOAD response (missing file content).");
            }

            // Return the raw file content (HTML string) after the third colon
            return response.substring(thirdColon + 1);

        } else {
            // Generic failure if the response format is totally unexpected (not ERROR or FILE_DOWNLOAD)
            throw new Exception("Failed to download transcript from server. Unexpected format: " + response);
        }
        }
        
        
    }

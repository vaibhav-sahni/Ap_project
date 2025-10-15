package edu.univ.erp.api.admin;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;

public class AdminAPI {

    private final Gson gson = new Gson();

    // ----------------------------------------------------------------------
    // --- 1. CREATE NEW STUDENT -------------------------------------------
    // ----------------------------------------------------------------------
    public String createStudent(Student student, String password) throws Exception {
    
    String request = String.format(
        "CREATE_STUDENT:%d:%s:%s:%s:%s:%d:%s",
        student.getUserId(),
        student.getUsername(),
        student.getRole(),
        student.getRollNo(),
        student.getProgram(),
        student.getYear(),
        password
    );
    String response = ClientRequest.send(request);

    if (response.startsWith("SUCCESS:")) {
        return response.substring("SUCCESS:".length());
    }
    throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error creating student");
}
    // ----------------------------------------------------------------------
    // --- 2. CREATE COURSE AND SECTION ------------------------------------
    // ----------------------------------------------------------------------
    public String createCourseAndSection(CourseCatalog course) throws Exception {
        String request = String.format("CREATE_COURSE_SECTION:%s:%s:%d:%d:%s:%d:%s:%d:%s:%d:%s:%d",
            course.getCourseCode(),
            course.getCourseTitle(),
            course.getCredits(),
            course.getSectionId(),
            course.getRoom(),
            course.getCapacity(),
            course.getDayTime(),
            course.getEnrolledCount(),
            course.getSemester(),
            course.getInstructorId(),
            course.getInstructorName(),
            course.getYear()



        );
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length());
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error creating course/section");
    }

    // ----------------------------------------------------------------------
    // --- 3. TOGGLE MAINTENANCE -------------------------------------------
    // ----------------------------------------------------------------------
    public String toggleMaintenance(boolean on) throws Exception {
        String request = "TOGGLE_MAINTENANCE:" + (on ? "ON" : "OFF");
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length());
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error toggling maintenance");
    }

    public boolean checkMaintenanceMode() throws Exception{
        String response = ClientRequest.send("CHECK_MAINTENANCE");
        if (response.startsWith("SUCCESS:")) {
            String payload = response.substring("SUCCESS:".length()).trim();
            // Server returns SUCCESS:ON or SUCCESS:OFF (or TRUE/FALSE). Only treat ON/TRUE as maintenance active.
            return "ON".equalsIgnoreCase(payload) || "TRUE".equalsIgnoreCase(payload);
        }
        if (response.startsWith("ERROR:")) {
            throw new Exception(response.substring("ERROR:".length()));
        }
        throw new Exception("Unknown response from server: " + response);
    }
    // ----------------------------------------------------------------------
    // --- 4. GET ALL COURSES / SECTIONS (Optional helper for admin UI) ---
    // ----------------------------------------------------------------------
    public List<CourseCatalog> getAllCourses() throws Exception {
        String request = "GET_ALL_COURSES";
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<CourseCatalog>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error fetching courses");
    }

    // ----------------------------------------------------------------------
    // --- 5. GET ALL STUDENTS (Optional helper for admin UI) -------------
    // ----------------------------------------------------------------------
    public List<Student> getAllStudents() throws Exception {
        String request = "GET_ALL_STUDENTS";
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<Student>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error fetching students");
    }

    public String createInstructor(Instructor instructor, String password) throws Exception {
    
    String request = String.format(
    "CREATE_INSTRUCTOR:%d:%s:%s:%s:%s:%s",
    instructor.getUserId(),
    instructor.getUsername(),
    instructor.getRole(),
    instructor.getName(),
    instructor.getDepartmentName(),
    password
);
String response = ClientRequest.send(request);


    if (response.startsWith("SUCCESS:")) {
        return response.substring("SUCCESS:".length());
    }
    throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error creating instructor");
}

    /**
     * Set the global drop deadline (ISO date YYYY-MM-DD). CLI and admin UI should login first
     * to establish the server-side session, then call this. The server authorizes using the
     * session's current user.
     */
    public String setDropDeadline(String isoDate) throws Exception {
        String request = String.format("SET_DROP_DEADLINE:%s", isoDate);
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            return response.substring("SUCCESS:".length());
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error setting drop deadline");
    }
}

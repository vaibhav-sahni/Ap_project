package edu.univ.erp.api.grade;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.univ.erp.api.ClientRequest;
import edu.univ.erp.domain.Grade;
import java.lang.reflect.Type;
import java.util.List;

public class GradeAPI {
    private final Gson gson = new Gson();

    public List<Grade> getMyGrades(int userId) throws Exception {
        String request = "GET_GRADES:" + userId;
        String response = ClientRequest.send(request);

        if (response.startsWith("SUCCESS:")) {
            String gradesJson = response.substring("SUCCESS:".length());
            
            // Deserialize List<Grade> using TypeToken
            Type listType = new TypeToken<List<Grade>>() {}.getType();
            return gson.fromJson(gradesJson, listType);
        } 
        throw new Exception("Failed to receive grades from server.");
    }
}
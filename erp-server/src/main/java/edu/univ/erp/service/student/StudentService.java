package edu.univ.erp.service.student;

import edu.univ.erp.dao.grade.GradeDAO;
import edu.univ.erp.dao.grade.GradeDAO.RawGradeResult;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.AssessmentComponent;
import java.util.ArrayList;
import java.util.List;

public class StudentService {
    
    private final GradeDAO gradeDAO = new GradeDAO();

    public List<Grade> fetchGrades(int userId) throws Exception {
        if (userId <= 0) {
            throw new Exception("Invalid user ID provided.");
        }
        
        // 1. Get initial course results (title, final grade, and enrollment ID)
        List<RawGradeResult> rawResults = gradeDAO.getRawGradeResultsByUserId(userId); 
        List<Grade> finalGrades = new ArrayList<>();
        
        // 2. Loop and aggregate components
        for (RawGradeResult raw : rawResults) {
            
            // 3. SECOND LOOKUP: Fetch all component details for this specific enrollment
            List<AssessmentComponent> components = gradeDAO.getComponentsByEnrollmentId(raw.enrollmentId());
            
            // 4. AGGREGATION: Combine all parts into the final Grade POJO
            finalGrades.add(new Grade(
                raw.courseTitle(),
                raw.finalGrade(),
                components
            ));
        }
        
        return finalGrades;
    }
}
package edu.univ.erp.util;

import java.util.List;

import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.Grade;


public class TranscriptFormatter {

    // Helper method to generate the HTML for component scores
    private String generateComponentTable(List<AssessmentComponent> components) {
        if (components == null || components.isEmpty()) {
            return "<p style=\"font-style: italic; color: #555; padding-left: 20px;\">No component scores recorded for this course.</p>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"components-table\"><thead><tr><th>Assessment Component</th><th>Score</th></tr></thead><tbody>");
        
        for (AssessmentComponent component : components) {
            sb.append("<tr>");
            sb.append("<td>").append(component.getComponentName()).append("</td>");
            sb.append("<td>").append(String.format("%.2f", component.getScore())).append("</td>");
            sb.append("</tr>");
        }
        
        sb.append("</tbody></table>");
        return sb.toString();
    }

    /**
     * Generates a full HTML document from the List<Grade> data.
     * * @param grades The complete list of course grades.
     * @param studentRollNo The student's official Roll Number (String).
     * @return A string containing the full HTML document.
     */
    public String generateHtml(List<Grade> grades, String studentRollNo) { // UPDATED SIGNATURE
        StringBuilder html = new StringBuilder();
        
        // Start HTML structure and styling for both screen and print
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Official Transcript - Roll No: ").append(studentRollNo).append("</title>");
        html.append("<style>");
        html.append("body { font-family: 'Inter', sans-serif; margin: 0; padding: 20px; background-color: #f7f9fc; color: #333; }");
        html.append(".container { max-width: 900px; margin: 0 auto; background: #fff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); }");
        html.append("h1 { color: #004d99; border-bottom: 3px solid #004d99; padding-bottom: 10px; margin-bottom: 25px; text-align: center; }");
        html.append("h2 { color: #1c7ed6; margin-top: 25px; margin-bottom: 5px; }");
        html.append(".course-card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px; margin-bottom: 25px; background-color: #ffffff; }");
        html.append(".course-summary { font-size: 1.1em; margin-bottom: 10px; }");
        html.append(".course-summary strong { color: #333; }");
        html.append(".components-table { width: 100%; border-collapse: collapse; margin-top: 10px; border-radius: 4px; overflow: hidden; }");
        html.append(".components-table th, .components-table td { border: 1px solid #eeeeee; padding: 12px; text-align: left; }");
        html.append(".components-table th { background-color: #f0f8ff; font-weight: 600; color: #004d99; }");
        html.append(".final-grade { font-size: 1.2em; font-weight: bold; color: #d9534f; margin-left: 10px; }");
        html.append("@media print { body { background-color: #fff; padding: 0; } .container { box-shadow: none; border: none; padding: 0; } }");
        html.append("</style></head><body>");
        
        html.append("<div class=\"container\">");
        html.append("<h1>Official Academic Transcript</h1>");
        html.append("<p><strong>Student Roll Number:</strong> ").append(studentRollNo).append("</p>"); 
        html.append("<hr style=\"margin-bottom: 30px;\">");

        if (grades == null || grades.isEmpty()) {
            html.append("<p style=\"text-align: center; margin-top: 50px; font-size: 1.2em; color: #888;\">No historical grade data found for this student.</p>");
        } else {
            for (Grade grade : grades) {
                String finalGrade = grade.getFinalGrade() != null && !grade.getFinalGrade().isEmpty() ? grade.getFinalGrade() : "In Progress (IP)";
                
                html.append("<div class=\"course-card\">");
                html.append("<h2>").append(grade.getCourseName()).append("</h2>");
                
                html.append("<div class=\"course-summary\">");
                html.append("<strong>Final Letter Grade:</strong> <span class=\"final-grade\">").append(finalGrade).append("</span>");
                html.append("</div>");
                
                html.append("<h3>Assessment Component Scores</h3>");
                html.append(generateComponentTable(grade.getComponents())); // getComponents() returns List<AssessmentComponent>
                
                html.append("</div>");
            }
        }

        html.append("</div></body></html>");
        return html.toString();
    }
}

package edu.univ.erp.dao.course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.CourseCatalog;

public class CourseDAO {

    // --- 1. SQL for the full Course Catalog (Fixed/Confirmed) ---
    // Uses LEFT JOIN enrollments to ensure sections with 0 students still appear.
    private static final String GET_CATALOG_SQL = 
        "SELECT " +
        "    c.code AS course_code, c.title, c.credits, " +
        "    s.section_id, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, " +
        "    i.name AS instructor_name, " +
        "    COUNT(e.student_id) AS enrolled_count " +
        "FROM courses c " +
        "JOIN sections s ON c.code = s.course_code " +
        "JOIN instructors i ON s.instructor_id = i.user_id " +
        "LEFT JOIN enrollments e ON s.section_id = e.section_id AND e.status = 'Registered' " +
        "GROUP BY s.section_id, c.code, c.title, c.credits, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, i.name " + // Added explicit grouping for safety
        "ORDER BY c.code, s.day_time";
        
    // --- 2. SQL to fetch a single catalog item by section ID ---
    private static final String GET_CATALOG_BY_SECTION_ID_SQL = 
        // Ensure WHERE appears before GROUP BY; replace the GROUP BY marker with a WHERE + GROUP BY
        GET_CATALOG_SQL.replace("GROUP BY s.section_id, c.code, c.title, c.credits, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, i.name ",
                                 "WHERE s.section_id = ? GROUP BY s.section_id, c.code, c.title, c.credits, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, i.name ");

    // --- 3. SQL QUERY: To fetch a student's Timetable (ONLY currently 'Registered' sections) ---
    // Note: Previously this query returned both 'Registered' and 'Completed' enrollments,
    // which caused the client to treat completed enrollments as still active. We now only
    // select rows where enrollment status = 'Registered' so the timetable represents
    // the student's ongoing schedule. Completed courses are surfaced from the grades API.
    private static final String GET_TIMETABLE_SQL = 
        "SELECT " +
        "    c.code AS course_code, c.title, c.credits, " +
        "    s.section_id, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, " +
        "    i.name AS instructor_name, " +
        "    er.status AS enrollment_status, " +
        "    1 AS enrolled_count " + // We know they are enrolled, so capacity check isn't strictly needed here.
        "FROM enrollments er " +
        "JOIN sections s ON er.section_id = s.section_id " +
        "JOIN courses c ON s.course_code = c.code " +
        "JOIN instructors i ON s.instructor_id = i.user_id " +
        "WHERE er.student_id = ? AND er.status = 'Registered' " +
        "ORDER BY s.day_time, c.code";


    /**
     * Fetches the entire course catalog including section and enrollment details.
     * @return A list of CourseCatalog objects.
     */
    public List<CourseCatalog> getCourseCatalog() {
        List<CourseCatalog> catalog = new ArrayList<>();

        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_CATALOG_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                catalog.add(mapResultSetToCourseCatalog(rs));
            }
        } catch (SQLException e) {
            System.err.println("DB Error fetching course catalog: " + e.getMessage());
        }
        return catalog;
    }
    
    // --- NEW METHOD: Get Student's Timetable ---
    /**
     * Fetches all actively 'Registered' sections for a given student ID.
     * @param studentId The ID of the student.
     * @return A list of CourseCatalog objects (representing the student's schedule).
     */
    public List<CourseCatalog> getStudentTimetable(int studentId) throws SQLException {
        List<CourseCatalog> schedule = new ArrayList<>();

        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TIMETABLE_SQL)) {
            
            stmt.setInt(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedule.add(mapResultSetToCourseCatalog(rs));
                }
                return schedule;
            }
        }
    }


    /**
     * Fetches a single CourseCatalog item by its unique Section ID.
     * @param sectionId The unique ID of the section.
     * @return A CourseCatalog object, or null if the section is not found.
     */
    public CourseCatalog getCatalogItemBySectionId(int sectionId) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_CATALOG_BY_SECTION_ID_SQL)) {
            
            stmt.setInt(1, sectionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourseCatalog(rs);
                }
                return null; // Section not found
            }
        }
    }
    
    // Helper method to reduce code duplication
    private CourseCatalog mapResultSetToCourseCatalog(ResultSet rs) throws SQLException {
        // NOTE: We rely on the SQL to provide all necessary columns for the CourseCatalog POJO
        return new CourseCatalog(
            rs.getString("course_code"),
            rs.getString("title"),
            rs.getInt("credits"),
            rs.getInt("section_id"),
            rs.getString("day_time"),
            rs.getString("room"),
            rs.getInt("capacity"),
            rs.getInt("enrolled_count"), // This is calculated by the SQL or hardcoded to 1 in the timetable query
            rs.getString("semester"),
            rs.getInt("year"),
            rs.getInt("instructor_id"),
            rs.getString("instructor_name"),
            // enrollment_status may be null in catalog queries
            (hasColumn(rs, "enrollment_status") ? rs.getString("enrollment_status") : null)
        );
    }

    // small helper to safely check for optional columns in a ResultSet
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
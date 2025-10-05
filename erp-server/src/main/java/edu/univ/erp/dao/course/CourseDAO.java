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

    // SQL to fetch all necessary course, section, and instructor details.
    // It also counts current enrollments.
    private static final String GET_CATALOG_SQL = 
        "SELECT " +
        "    c.code AS course_code, c.title, c.credits, " +
        "    s.section_id, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, " +
        "    i.name AS instructor_name, " +
        "    COUNT(e.student_id) AS enrolled_count " +
        "FROM courses c " +
        "JOIN sections s ON c.code = s.course_code " +
        "JOIN instructors i ON s.instructor_id = i.user_id " +
        "LEFT JOIN enrollments e ON s.section_id = e.section_id AND e.status = 'Registered' " +
        "GROUP BY s.section_id " +
        "ORDER BY c.code, s.day_time";
        
    // --- NEW SQL QUERY: To fetch a single catalog item by section ID ---
    // Note: We use the existing complex JOIN query structure to ensure all data for the CourseCatalog POJO is fetched.
    private static final String GET_CATALOG_BY_SECTION_ID_SQL = GET_CATALOG_SQL + 
        " HAVING s.section_id = ?"; // Use HAVING since we are grouping by s.section_id

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
    
    // --- NEW METHOD: Used by StudentService for time conflict checks ---
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
        return new CourseCatalog(
            rs.getString("course_code"),
            rs.getString("title"),
            rs.getInt("credits"),
            rs.getInt("section_id"),
            rs.getString("day_time"),
            rs.getString("room"),
            rs.getInt("capacity"),
            rs.getInt("enrolled_count"),
            rs.getString("semester"),
            rs.getInt("year"),
            rs.getInt("instructor_id"),
            rs.getString("instructor_name")
        );
    }
}
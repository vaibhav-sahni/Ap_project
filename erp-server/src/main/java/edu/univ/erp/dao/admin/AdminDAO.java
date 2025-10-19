package edu.univ.erp.dao.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Student;
import edu.univ.erp.security.PasswordHasher;

public class AdminDAO {

    private static final Logger LOGGER = Logger.getLogger(AdminDAO.class.getName());

    private static final String INSERT_AUTH_SQL =
        "INSERT INTO auth_db.users_auth (user_id, username, role, password_hash) VALUES (?, ?, ?, ?)";
    // Variant that lets the DB auto-generate the user_id (AUTO_INCREMENT)
    private static final String INSERT_AUTH_SQL_NO_ID =
        "INSERT INTO auth_db.users_auth (username, role, password_hash) VALUES (?, ?, ?)";
    private static final String INSERT_STUDENT_SQL =
        "INSERT INTO erp_db.students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";
    private static final String INSERT_COURSE_SQL =
            "INSERT INTO erp_db.courses (code, title, credits) VALUES (?, ?, ?)";
    private static final String INSERT_SECTION_SQL =
            "INSERT INTO erp_db.sections (course_code, instructor_id, day_time, room, capacity, semester, year) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SECTION_INSTRUCTOR_SQL =
        "UPDATE erp_db.sections SET instructor_id = ? WHERE section_id = ?";
    private static final String SELECT_STUDENTS_SQL =
            "SELECT s.user_id, s.roll_no, s.program, s.year, u.username, u.role " +
            "FROM erp_db.students s " +
            "JOIN auth_db.users_auth u ON s.user_id = u.user_id";
    private static final String INSERT_USER_SQL = "INSERT INTO auth_db.users_auth (user_id, username, password_hash, role) VALUES (?, ?, ?, ?)";
    private static final String INSERT_INSTRUCTOR_SQL = "INSERT INTO erp_db.instructors (user_id, name, department) VALUES (?, ?, ?)";

        private static final String SELECT_COURSES_SQL =
            "SELECT c.code, c.title, c.credits, s.section_id, s.day_time, s.room, s.capacity, s.semester, s.year, " +
            "COALESCE(i.user_id, 0) AS instructor_id, COALESCE(i.name, '') AS instructor_name, " +
            "COALESCE((SELECT COUNT(*) FROM erp_db.enrollments e WHERE e.section_id = s.section_id AND e.status = 'Registered'), 0) AS enrolled_count " +
            "FROM erp_db.courses c " +
            "LEFT JOIN erp_db.sections s ON s.course_code = c.code " +
            "LEFT JOIN erp_db.instructors i ON s.instructor_id = i.user_id";
    private static final String SELECT_INSTRUCTORS_SQL =
        "SELECT i.user_id, u.username, i.name, i.department AS department_name, COUNT(s.section_id) AS assigned_count " +
        "FROM erp_db.instructors i " +
        "JOIN auth_db.users_auth u ON i.user_id = u.user_id " +
        "LEFT JOIN erp_db.sections s ON s.instructor_id = i.user_id " +
        "GROUP BY i.user_id, u.username, i.name, i.department";
    private static final String CHECK_INSTRUCTOR_EXISTS_SQL =
        "SELECT COUNT(*) FROM erp_db.instructors WHERE user_id = ?";
    private static final String CHECK_COURSE_EXISTS_SQL =
        "SELECT COUNT(*) FROM erp_db.courses WHERE code = ?";
   

    public boolean createStudent(Student student, String password) {
        try (Connection conn = DBConnector.getErpConnection()) {
            conn.setAutoCommit(false); // Transaction for both DBs

            int allocatedUserId = student.getUserId();
            // If userId == 0, let DB allocate via auto-increment and retrieve generated key
            if (allocatedUserId == 0) {
                try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL_NO_ID, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmtAuth.setString(1, student.getUsername());
                    stmtAuth.setString(2, student.getRole());
                    stmtAuth.setString(3, PasswordHasher.hashPassword(password));
                    int updated = stmtAuth.executeUpdate();
                    if (updated == 0) throw new SQLException("Creating auth entry failed, no rows affected.");
                    try (ResultSet gen = stmtAuth.getGeneratedKeys()) {
                        if (gen.next()) allocatedUserId = gen.getInt(1);
                        else throw new SQLException("Creating auth entry failed, no generated key obtained.");
                    }
                }
            } else {
                try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL)) {
                    stmtAuth.setInt(1, allocatedUserId);
                    stmtAuth.setString(2, student.getUsername());
                    stmtAuth.setString(3, student.getRole());
                    stmtAuth.setString(4, PasswordHasher.hashPassword(password));
                    stmtAuth.executeUpdate();
                }
            }

            // Insert into students table using the allocated or provided user id
            try (PreparedStatement stmtERP = conn.prepareStatement(INSERT_STUDENT_SQL)) {
                stmtERP.setInt(1, allocatedUserId);
                stmtERP.setString(2, student.getRollNo());
                stmtERP.setString(3, student.getProgram());
                stmtERP.setInt(4, student.getYear());
                stmtERP.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO createStudent error: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean createCourseAndSection(CourseCatalog course) {
        try (Connection conn = DBConnector.getErpConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmtCourse = conn.prepareStatement(INSERT_COURSE_SQL)) {
                stmtCourse.setString(1, course.getCourseCode());
                stmtCourse.setString(2, course.getCourseTitle());
                stmtCourse.setInt(3, course.getCredits());
                stmtCourse.executeUpdate();
            }

            try (PreparedStatement stmtSection = conn.prepareStatement(INSERT_SECTION_SQL)) {
                stmtSection.setString(1, course.getCourseCode());
                if (course.getInstructorId() == 0) {
                    stmtSection.setNull(2, java.sql.Types.INTEGER);
                } else {
                    stmtSection.setInt(2, course.getInstructorId());
                }
                stmtSection.setString(3, course.getDayTime());
                stmtSection.setString(4, course.getRoom());
                stmtSection.setInt(5, course.getCapacity());
                stmtSection.setString(6, course.getSemester());
                stmtSection.setInt(7, course.getYear());
                stmtSection.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO createCourseAndSection error: " + e.getMessage(), e);
            return false;
        }
    }

    /** Create only the course (no section) */
    public boolean createCourse(CourseCatalog course) {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmtCourse = conn.prepareStatement(INSERT_COURSE_SQL)) {
            stmtCourse.setString(1, course.getCourseCode());
            stmtCourse.setString(2, course.getCourseTitle());
            stmtCourse.setInt(3, course.getCredits());
            stmtCourse.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO createCourse error: " + e.getMessage(), e);
            return false;
        }
    }

    /** Create only the section for an existing course */
    public boolean createSection(CourseCatalog course) {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmtSection = conn.prepareStatement(INSERT_SECTION_SQL)) {
            stmtSection.setString(1, course.getCourseCode());
            if (course.getInstructorId() == 0) {
                stmtSection.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmtSection.setInt(2, course.getInstructorId());
            }
            stmtSection.setString(3, course.getDayTime());
            stmtSection.setString(4, course.getRoom());
            stmtSection.setInt(5, course.getCapacity());
            stmtSection.setString(6, course.getSemester());
            stmtSection.setInt(7, course.getYear());
            stmtSection.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO createSection error: " + e.getMessage(), e);
            return false;
        }
    }

    public List<CourseCatalog> fetchAllCourses() {
        List<CourseCatalog> courses = new ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_COURSES_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                courses.add(new CourseCatalog(
                        rs.getString("code"),
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
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO fetchAllCourses error: " + e.getMessage(), e);
        }
        return courses;
    }

    public List<Student> fetchAllStudents() {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_STUDENTS_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("year")
                ));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO fetchAllStudents error: " + e.getMessage(), e);
        }
        return students;
    }

    public java.util.List<java.util.Map<String,Object>> fetchAllInstructors() {
        java.util.List<java.util.Map<String,Object>> instructors = new java.util.ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_INSTRUCTORS_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("user_id", rs.getInt("user_id"));
                m.put("username", rs.getString("username"));
                m.put("name", rs.getString("name"));
                m.put("department_name", rs.getString("department_name"));
                m.put("assigned_count", rs.getInt("assigned_count"));
                instructors.add(m);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO fetchAllInstructors error: " + e.getMessage(), e);
        }
        return instructors;
    }

    public boolean instructorExists(int userId) {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_INSTRUCTOR_EXISTS_SQL)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO instructorExists error: " + e.getMessage(), e);
            return false;
        }
    }

    /** Returns true if a course with the given code already exists. */
    public boolean courseExists(String courseCode) {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_COURSE_EXISTS_SQL)) {
            stmt.setString(1, courseCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO courseExists error: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compute next available user_id by looking at the auth_db.users_auth table.
     * Falls back to 1000+1 when table empty.
     */
    public int getNextUserId() {
        final String sql = "SELECT COALESCE(MAX(user_id), 1000) + 1 FROM auth_db.users_auth";
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO getNextUserId error: " + e.getMessage(), e);
        }
        return 1001; // reasonable default
    }

    public boolean createInstructor(Instructor instructor, String password) {
    try (Connection conn = DBConnector.getErpConnection()) {
        conn.setAutoCommit(false);

        int allocatedUserId = instructor.getUserId();
        try {
            if (allocatedUserId == 0) {
                try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL_NO_ID, PreparedStatement.RETURN_GENERATED_KEYS);
                     PreparedStatement stmtInstr = conn.prepareStatement(INSERT_INSTRUCTOR_SQL)) {
                    stmtAuth.setString(1, instructor.getUsername());
                    stmtAuth.setString(2, instructor.getRole());
                    stmtAuth.setString(3, PasswordHasher.hashPassword(password));
                    int updated = stmtAuth.executeUpdate();
                    if (updated == 0) throw new SQLException("Creating auth entry failed, no rows affected.");
                    try (ResultSet gen = stmtAuth.getGeneratedKeys()) {
                        if (gen.next()) allocatedUserId = gen.getInt(1);
                        else throw new SQLException("Creating auth entry failed, no generated key obtained.");
                    }

                    stmtInstr.setInt(1, allocatedUserId);
                    stmtInstr.setString(2, instructor.getName());
                    stmtInstr.setString(3, instructor.getDepartmentName());
                    stmtInstr.executeUpdate();
                }
            } else {
                try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL);
                     PreparedStatement stmtInstr = conn.prepareStatement(INSERT_INSTRUCTOR_SQL)) {
                    stmtAuth.setInt(1, allocatedUserId);
                    stmtAuth.setString(2, instructor.getUsername());
                    stmtAuth.setString(3, instructor.getRole());
                    stmtAuth.setString(4, PasswordHasher.hashPassword(password));
                    stmtAuth.executeUpdate();

                    stmtInstr.setInt(1, allocatedUserId);
                    stmtInstr.setString(2, instructor.getName());
                    stmtInstr.setString(3, instructor.getDepartmentName());
                    stmtInstr.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.WARNING, "Rollback failed", ex); }
            LOGGER.log(Level.SEVERE, "AdminDAO createInstructor error: " + e.getMessage(), e);
            return false;
        }
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "AdminDAO createInstructor outer SQL error: " + e.getMessage(), e);
        return false;
    }
}

    /**
     * Reassigns the instructor for an existing section. Returns true if an update occurred.
     */
    public boolean reassignInstructor(int sectionId, int newInstructorId) {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SECTION_INSTRUCTOR_SQL)) {
            stmt.setInt(1, newInstructorId);
            stmt.setInt(2, sectionId);
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "AdminDAO reassignInstructor error: " + e.getMessage(), e);
            return false;
        }
    }

}

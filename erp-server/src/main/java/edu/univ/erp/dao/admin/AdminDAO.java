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
    private static final String INSERT_STUDENT_SQL =
        "INSERT INTO erp_db.students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";
    private static final String INSERT_COURSE_SQL =
            "INSERT INTO erp_db.courses (code, title, credits) VALUES (?, ?, ?)";
    private static final String INSERT_SECTION_SQL =
            "INSERT INTO erp_db.sections (course_code, instructor_id, day_time, room, capacity, semester, year) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_STUDENTS_SQL =
            "SELECT s.user_id, s.roll_no, s.program, s.year, u.username, u.role " +
            "FROM erp_db.students s " +
            "JOIN auth_db.users_auth u ON s.user_id = u.user_id";
    private static final String INSERT_USER_SQL = "INSERT INTO auth_db.users_auth (user_id, username, password_hash, role) VALUES (?, ?, ?, ?)";
    private static final String INSERT_INSTRUCTOR_SQL = "INSERT INTO erp_db.instructors (user_id, name, department_name) VALUES (?, ?, ?)";

        private static final String SELECT_COURSES_SQL =
            "SELECT s.section_id, c.code, c.title, c.credits, s.day_time, s.room, s.capacity, s.semester, s.year, " +
            "i.user_id AS instructor_id, i.name AS instructor_name, " +
            "(SELECT COUNT(*) FROM erp_db.enrollments e WHERE e.section_id = s.section_id AND e.status = 'Registered') AS enrolled_count " +
            "FROM erp_db.sections s " +
            "JOIN erp_db.courses c ON s.course_code = c.code " +
            "JOIN erp_db.instructors i ON s.instructor_id = i.user_id";
   

    public boolean createStudent(Student student, String password) {
        try (Connection conn = DBConnector.getErpConnection()) {
            conn.setAutoCommit(false); // Transaction for both DBs

            try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL)) {
                stmtAuth.setInt(1, student.getUserId());
                stmtAuth.setString(2, student.getUsername());
                stmtAuth.setString(3, student.getRole());
                stmtAuth.setString(4, PasswordHasher.hashPassword(password));
                stmtAuth.executeUpdate();
            }

            try (PreparedStatement stmtERP = conn.prepareStatement(INSERT_STUDENT_SQL)) {
                stmtERP.setInt(1, student.getUserId());
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
                stmtSection.setInt(2, course.getInstructorId());
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

    public boolean createInstructor(Instructor instructor, String password) {
    try (Connection conn = DBConnector.getErpConnection()) {
        conn.setAutoCommit(false);

        try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL);
             PreparedStatement stmtInstr = conn.prepareStatement(INSERT_INSTRUCTOR_SQL)) {
            
            stmtAuth.setInt(1, instructor.getUserId());
            stmtAuth.setString(2, instructor.getUsername());
            stmtAuth.setString(3, instructor.getRole());
            stmtAuth.setString(4, PasswordHasher.hashPassword(password));
            stmtAuth.executeUpdate();

            stmtInstr.setInt(1, instructor.getUserId());
            stmtInstr.setString(2, instructor.getName());
            stmtInstr.setString(3, instructor.getDepartmentName());
            stmtInstr.executeUpdate();

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

}

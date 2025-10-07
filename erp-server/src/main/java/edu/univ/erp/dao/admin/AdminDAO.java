package edu.univ.erp.dao.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Student;

public class AdminDAO {

    // -----------------------------
    // 1. CREATE STUDENT (AUTH + ERP)
    // -----------------------------
    private static final String INSERT_AUTH_SQL =
        "INSERT INTO auth_db.users_auth (user_id, username, role, password_hash) VALUES (?, ?, ?, ?)";
    private static final String INSERT_STUDENT_SQL =
        "INSERT INTO erp_db.students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";

    /**
     * Creates a new student record in both auth_db and erp_db.
     * Assumes passwordHash is pre-hashed (e.g., BCrypt) before calling this method.
     */
    public boolean createStudent(Student student, String passwordHash) {
        try (Connection conn = DBConnector.getErpConnection()) {
            conn.setAutoCommit(false); // Transaction for both DBs

            // 1️⃣ Insert into auth_db.users_auth
            try (PreparedStatement stmtAuth = conn.prepareStatement(INSERT_AUTH_SQL)) {
                stmtAuth.setInt(1, student.getUserId());
                stmtAuth.setString(2, student.getUsername());
                stmtAuth.setString(3, student.getRole());
                stmtAuth.setString(4, passwordHash);
                stmtAuth.executeUpdate();
            }

            // 2️⃣ Insert into erp_db.students
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
            System.err.println("AdminDAO createStudent error: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------
    // 2. CREATE COURSE + SECTION
    // -----------------------------
    private static final String INSERT_COURSE_SQL =
            "INSERT INTO erp_db.courses (code, title, credits) VALUES (?, ?, ?)";
    private static final String INSERT_SECTION_SQL =
            "INSERT INTO erp_db.sections (course_code, instructor_id, day_time, room, capacity, semester, year) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public boolean createCourseAndSection(CourseCatalog course) {
        try (Connection conn = DBConnector.getErpConnection()) {
            conn.setAutoCommit(false);

            // Insert course
            try (PreparedStatement stmtCourse = conn.prepareStatement(INSERT_COURSE_SQL)) {
                stmtCourse.setString(1, course.getCourseCode());
                stmtCourse.setString(2, course.getCourseTitle());
                stmtCourse.setInt(3, course.getCredits());
                stmtCourse.executeUpdate();
            }

            // Insert section
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
            System.err.println("AdminDAO createCourseAndSection error: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------
    // 3. FETCH ALL COURSES
    // -----------------------------
    private static final String SELECT_COURSES_SQL =
            "SELECT s.section_id, c.code, c.title, c.credits, s.day_time, s.room, s.capacity, s.semester, s.year, " +
            "i.user_id AS instructor_id, i.name AS instructor_name, " +
            "(SELECT COUNT(*) FROM erp_db.enrollments e WHERE e.section_id = s.section_id AND e.status = 'Registered') AS enrolled_count " +
            "FROM erp_db.sections s " +
            "JOIN erp_db.courses c ON s.course_code = c.code " +
            "JOIN erp_db.instructors i ON s.instructor_id = i.user_id";

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
            System.err.println("AdminDAO fetchAllCourses error: " + e.getMessage());
        }
        return courses;
    }

    // -----------------------------
    // 4. FETCH ALL STUDENTS
    // -----------------------------
    private static final String SELECT_STUDENTS_SQL =
            "SELECT s.user_id, s.roll_no, s.program, s.year, u.username, u.role " +
            "FROM erp_db.students s " +
            "JOIN auth_db.users_auth u ON s.user_id = u.user_id";

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
            System.err.println("AdminDAO fetchAllStudents error: " + e.getMessage());
        }
        return students;
    }
}

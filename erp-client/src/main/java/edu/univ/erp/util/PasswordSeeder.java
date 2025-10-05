package edu.univ.erp.util;

import edu.univ.erp.auth.hash.PasswordHasher;

/**
 * Utility class to generate secure password hashes for initial database seeding.
 * THIS CLASS SHOULD BE REMOVED/BLOCKED FROM PRODUCTION CODE.
 */
public class PasswordSeeder {

    public static void main(String[] args) {
        // Define the plaintext passwords for your initial users
        final String ADMIN_PASS = "admin123";
        final String INSTR_PASS = "instr123";
        final String STUD_PASS = "student123";
        
        System.out.println("--- Generating Secure Hashes ---");
        System.out.println("Plaintext: " + ADMIN_PASS);
        System.out.println("Admin Hash: " + PasswordHasher.hashPassword(ADMIN_PASS)); //$2a$12$R62RZ3S7clI1BuOa3BL60u3qxkfs5XgAvPaRT/xkuxQPR5o/dru/a
        System.out.println("\nPlaintext: " + INSTR_PASS);
        System.out.println("Instructor Hash: " + PasswordHasher.hashPassword(INSTR_PASS)); //$2a$12$1CrqjHJ2koDqkIJMReWFGOKJFw16qgC7awUo5IP2yQRky2aMxbD2O
        System.out.println("\nPlaintext: " + STUD_PASS);
        System.out.println("Student Hash: " + PasswordHasher.hashPassword(STUD_PASS)); //$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC
        System.out.println("------------------------------");
    }
}

/*

SEEDING USERS. 

 INSERT INTO users_auth (username, role, password_hash) 
VALUES 
('admin', 'Admin', '$2a$12$R62RZ3S7clI1BuOa3BL60u3qxkfs5XgAvPaRT/xkuxQPR5o/dru/a'),
('instructor_1', 'Instructor', '$2a$12$1CrqjHJ2koDqkIJMReWFGOKJFw16qgC7awUo5IP2yQRky2aMxbD2O'),
('student_1', 'Student', '$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC');



INSERT INTO instructors (user_id, name, department) 
VALUES (2, 'Sambuddho Chakravarty', 'Computer Science');

INSERT INTO students (user_id, roll_no, program, year) 
VALUES (3, '2024596', 'CSE', 2);

INSERT INTO settings (setting_key, setting_value) 
VALUES ('maintenance_on', 'false');
 */
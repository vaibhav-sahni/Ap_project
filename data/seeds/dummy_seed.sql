-- Dummy seed for local testing: 3 courses, each with 1 section taught by instructor_1,
-- and 5 students per course (15 students total). Idempotent so it can be re-run.

-- Assumptions:
-- - Databases: auth_db and erp_db exist (as in sql_commands.txt)
-- - A user 'instructor_1' already exists in auth_db.users_auth (the project uses that username)
-- If instructor_1 is missing, the script will create a placeholder instructor.

-- Run this script with: mysql -u root -p < data/seeds/dummy_seed.sql

-- 1. Ensure DBs
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS erp_db;

USE auth_db;

-- 2. Ensure instructor_1 exists in auth_db (create if missing)
INSERT INTO users_auth (username, role, password_hash)
SELECT 'instructor_1', 'INSTRUCTOR', ''
WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username = 'instructor_1');

-- Fetch instructor id into a user variable
SET @instr_id = (SELECT user_id FROM users_auth WHERE username = 'instructor_1' LIMIT 1);

-- If instructor record in instructors table missing, add
USE erp_db;
INSERT INTO instructors (user_id, name, department)
SELECT @instr_id, 'Instructor One', 'Computer Science'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE user_id = @instr_id);

-- 3. Create three test courses (idempotent)
INSERT INTO courses (code, title, credits)
VALUES
  ('CSE100', 'Intro to Programming (Dummy)', 3),
  ('MTH100', 'Foundations of Mathematics (Dummy)', 3),
  ('DES100', 'Introduction to Design (Dummy)', 3)
ON DUPLICATE KEY UPDATE title = VALUES(title), credits = VALUES(credits);

-- 4. Create one section per course taught by instructor_1
INSERT INTO sections (course_code, instructor_id, day_time, room, capacity, semester, year)
SELECT 'CSE100', @instr_id, 'Mon 09:00-11:00', 'R101', 100, 'Monsoon', YEAR(CURDATE())
WHERE NOT EXISTS (SELECT 1 FROM sections WHERE course_code='CSE100' AND instructor_id=@instr_id);

INSERT INTO sections (course_code, instructor_id, day_time, room, capacity, semester, year)
SELECT 'MTH100', @instr_id, 'Tue 11:00-13:00', 'R102', 80, 'Monsoon', YEAR(CURDATE())
WHERE NOT EXISTS (SELECT 1 FROM sections WHERE course_code='MTH100' AND instructor_id=@instr_id);

INSERT INTO sections (course_code, instructor_id, day_time, room, capacity, semester, year)
SELECT 'DES100', @instr_id, 'Wed 14:00-16:00', 'R103', 60, 'Monsoon', YEAR(CURDATE())
WHERE NOT EXISTS (SELECT 1 FROM sections WHERE course_code='DES100' AND instructor_id=@instr_id);

-- 5. Add 15 student accounts in auth_db (5 per course). Use unique usernames/rolls and idempotent insert.
USE auth_db;

-- We'll create student usernames: s_c1_01 .. s_c1_05, s_c2_01 .. s_c2_05, s_c3_01 .. s_c3_05
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c1_01', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c1_01');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c1_02', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c1_02');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c1_03', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c1_03');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c1_04', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c1_04');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c1_05', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c1_05');

INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c2_01', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c2_01');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c2_02', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c2_02');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c2_03', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c2_03');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c2_04', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c2_04');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c2_05', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c2_05');

INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c3_01', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c3_01');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c3_02', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c3_02');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c3_03', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c3_03');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c3_04', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c3_04');
INSERT INTO users_auth (username, role, password_hash)
SELECT 's_c3_05', 'STUDENT', '' WHERE NOT EXISTS (SELECT 1 FROM users_auth WHERE username='s_c3_05');

-- 6. Insert into erp_db.students for those users (assign roll numbers)
USE erp_db;

-- Helper: map usernames to ids via auth_db
SET @s_c1_01 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c1_01');
SET @s_c1_02 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c1_02');
SET @s_c1_03 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c1_03');
SET @s_c1_04 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c1_04');
SET @s_c1_05 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c1_05');

SET @s_c2_01 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c2_01');
SET @s_c2_02 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c2_02');
SET @s_c2_03 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c2_03');
SET @s_c2_04 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c2_04');
SET @s_c2_05 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c2_05');

SET @s_c3_01 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c3_01');
SET @s_c3_02 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c3_02');
SET @s_c3_03 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c3_03');
SET @s_c3_04 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c3_04');
SET @s_c3_05 = (SELECT user_id FROM auth_db.users_auth WHERE username='s_c3_05');

INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c1_01, 'S-C1-001', 'CSE', 1 WHERE @s_c1_01 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c1_01);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c1_02, 'S-C1-002', 'CSE', 1 WHERE @s_c1_02 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c1_02);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c1_03, 'S-C1-003', 'CSE', 1 WHERE @s_c1_03 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c1_03);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c1_04, 'S-C1-004', 'CSE', 1 WHERE @s_c1_04 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c1_04);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c1_05, 'S-C1-005', 'CSE', 1 WHERE @s_c1_05 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c1_05);

INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c2_01, 'S-C2-001', 'MTH', 1 WHERE @s_c2_01 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c2_01);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c2_02, 'S-C2-002', 'MTH', 1 WHERE @s_c2_02 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c2_02);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c2_03, 'S-C2-003', 'MTH', 1 WHERE @s_c2_03 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c2_03);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c2_04, 'S-C2-004', 'MTH', 1 WHERE @s_c2_04 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c2_04);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c2_05, 'S-C2-005', 'MTH', 1 WHERE @s_c2_05 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c2_05);

INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c3_01, 'S-C3-001', 'DES', 1 WHERE @s_c3_01 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c3_01);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c3_02, 'S-C3-002', 'DES', 1 WHERE @s_c3_02 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c3_02);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c3_03, 'S-C3-003', 'DES', 1 WHERE @s_c3_03 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c3_03);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c3_04, 'S-C3-004', 'DES', 1 WHERE @s_c3_04 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c3_04);
INSERT INTO students (user_id, roll_no, program, year)
SELECT @s_c3_05, 'S-C3-005', 'DES', 1 WHERE @s_c3_05 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM students WHERE user_id = @s_c3_05);

-- 7. Enroll students into the corresponding sections
-- Lookup section ids for each course
SET @sec_c1 = (SELECT section_id FROM sections WHERE course_code='CSE100' AND instructor_id=@instr_id LIMIT 1);
SET @sec_c2 = (SELECT section_id FROM sections WHERE course_code='MTH100' AND instructor_id=@instr_id LIMIT 1);
SET @sec_c3 = (SELECT section_id FROM sections WHERE course_code='DES100' AND instructor_id=@instr_id LIMIT 1);

INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c1_01, @sec_c1, 'Registered' WHERE @s_c1_01 IS NOT NULL AND @sec_c1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c1_01 AND section_id=@sec_c1);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c1_02, @sec_c1, 'Registered' WHERE @s_c1_02 IS NOT NULL AND @sec_c1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c1_02 AND section_id=@sec_c1);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c1_03, @sec_c1, 'Registered' WHERE @s_c1_03 IS NOT NULL AND @sec_c1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c1_03 AND section_id=@sec_c1);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c1_04, @sec_c1, 'Registered' WHERE @s_c1_04 IS NOT NULL AND @sec_c1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c1_04 AND section_id=@sec_c1);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c1_05, @sec_c1, 'Registered' WHERE @s_c1_05 IS NOT NULL AND @sec_c1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c1_05 AND section_id=@sec_c1);

INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c2_01, @sec_c2, 'Registered' WHERE @s_c2_01 IS NOT NULL AND @sec_c2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c2_01 AND section_id=@sec_c2);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c2_02, @sec_c2, 'Registered' WHERE @s_c2_02 IS NOT NULL AND @sec_c2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c2_02 AND section_id=@sec_c2);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c2_03, @sec_c2, 'Registered' WHERE @s_c2_03 IS NOT NULL AND @sec_c2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c2_03 AND section_id=@sec_c2);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c2_04, @sec_c2, 'Registered' WHERE @s_c2_04 IS NOT NULL AND @sec_c2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c2_04 AND section_id=@sec_c2);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c2_05, @sec_c2, 'Registered' WHERE @s_c2_05 IS NOT NULL AND @sec_c2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c2_05 AND section_id=@sec_c2);

INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c3_01, @sec_c3, 'Registered' WHERE @s_c3_01 IS NOT NULL AND @sec_c3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c3_01 AND section_id=@sec_c3);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c3_02, @sec_c3, 'Registered' WHERE @s_c3_02 IS NOT NULL AND @sec_c3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c3_02 AND section_id=@sec_c3);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c3_03, @sec_c3, 'Registered' WHERE @s_c3_03 IS NOT NULL AND @sec_c3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c3_03 AND section_id=@sec_c3);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c3_04, @sec_c3, 'Registered' WHERE @s_c3_04 IS NOT NULL AND @sec_c3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c3_04 AND section_id=@sec_c3);
INSERT INTO enrollments (student_id, section_id, status)
SELECT @s_c3_05, @sec_c3, 'Registered' WHERE @s_c3_05 IS NOT NULL AND @sec_c3 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM enrollments WHERE student_id=@s_c3_05 AND section_id=@sec_c3);

-- 8. Optionally create empty grade rows (component rows) for each enrollment for convenience
-- Components: Quiz, Assignment, Midterm, Endterm
INSERT IGNORE INTO grades (enrollment_id, component, score, final_grade)
SELECT e.enrollment_id, 'Quiz', 0, NULL FROM enrollments e WHERE e.section_id IN (@sec_c1, @sec_c2, @sec_c3);
INSERT IGNORE INTO grades (enrollment_id, component, score, final_grade)
SELECT e.enrollment_id, 'Assignment', 0, NULL FROM enrollments e WHERE e.section_id IN (@sec_c1, @sec_c2, @sec_c3);
INSERT IGNORE INTO grades (enrollment_id, component, score, final_grade)
SELECT e.enrollment_id, 'Midterm', 0, NULL FROM enrollments e WHERE e.section_id IN (@sec_c1, @sec_c2, @sec_c3);
INSERT IGNORE INTO grades (enrollment_id, component, score, final_grade)
SELECT e.enrollment_id, 'Endterm', 0, NULL FROM enrollments e WHERE e.section_id IN (@sec_c1, @sec_c2, @sec_c3);

-- 9. Summary queries to verify
SELECT 'Instructor id' as note, @instr_id as id;
SELECT section_id, course_code FROM sections WHERE instructor_id = @instr_id;
SELECT u.user_id, u.username FROM auth_db.users_auth u WHERE u.username LIKE 's_c%';
SELECT e.enrollment_id, e.student_id, e.section_id FROM enrollments e WHERE e.section_id IN (@sec_c1, @sec_c2, @sec_c3);

-- End of seed

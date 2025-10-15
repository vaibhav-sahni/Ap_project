-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: erp_db
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `code` varchar(10) NOT NULL,
  `title` varchar(150) NOT NULL,
  `credits` decimal(3,1) NOT NULL,
  PRIMARY KEY (`code`),
  CONSTRAINT `courses_chk_1` CHECK ((`credits` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES ('C222','DSA',4.0),('CSE201','Advanced Programming',4.0),('MTH101','Linear Algebra',4.0);
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `section_id` int NOT NULL,
  `status` enum('Registered','Dropped','Completed') DEFAULT 'Registered',
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uq_student_section` (`student_id`,`section_id`),
  KEY `section_id` (`section_id`),
  CONSTRAINT `enrollments_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`),
  CONSTRAINT `enrollments_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `sections` (`section_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollments`
--

LOCK TABLES `enrollments` WRITE;
/*!40000 ALTER TABLE `enrollments` DISABLE KEYS */;
INSERT INTO `enrollments` VALUES (1,3,1,'Completed'),(2,5,1,'Completed'),(3,3,2,'Registered'),(4,7,1,'Completed'),(5,8,1,'Completed'),(6,9,1,'Completed'),(9,5,2,'Registered'),(10,6,2,'Registered');
/*!40000 ALTER TABLE `enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grades`
--

DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `grade_id` int NOT NULL AUTO_INCREMENT,
  `enrollment_id` int NOT NULL,
  `component` varchar(50) NOT NULL,
  `score` decimal(5,2) DEFAULT NULL,
  `final_grade` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `uq_enrollment_component` (`enrollment_id`,`component`),
  CONSTRAINT `grades_ibfk_1` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`enrollment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=224 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
INSERT INTO `grades` VALUES (11,1,'Quiz',90.00,NULL),(12,1,'Assignment',95.00,NULL),(13,1,'Midterm',88.00,NULL),(14,1,'Endterm',92.00,NULL),(17,3,'Quiz',95.00,NULL),(18,3,'Assignment',92.00,NULL),(19,3,'Midterm',90.00,NULL),(20,3,'Endterm',95.00,NULL),(51,2,'Quiz',95.00,NULL),(52,2,'Assignment',92.00,NULL),(53,2,'Midterm',98.00,NULL),(54,2,'Endterm',95.00,NULL),(55,4,'Quiz',55.00,NULL),(56,4,'Assignment',40.00,NULL),(57,4,'Midterm',50.00,NULL),(58,4,'Endterm',55.00,NULL),(59,5,'Quiz',90.00,NULL),(60,5,'Assignment',90.00,NULL),(61,5,'Midterm',90.00,NULL),(62,5,'Endterm',90.00,NULL),(63,6,'Quiz',90.00,NULL),(64,6,'Assignment',90.00,NULL),(65,6,'Midterm',90.00,NULL),(66,6,'Endterm',90.00,NULL),(71,2,'FinalGrade',NULL,'A'),(76,4,'FinalGrade',NULL,'F'),(81,5,'FinalGrade',NULL,'A'),(86,6,'FinalGrade',NULL,'A'),(215,1,'FinalGrade',NULL,'A');
/*!40000 ALTER TABLE `grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instructors`
--

DROP TABLE IF EXISTS `instructors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `instructors` (
  `user_id` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `department` varchar(50) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `instructors_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `auth_db`.`users_auth` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instructors`
--

LOCK TABLES `instructors` WRITE;
/*!40000 ALTER TABLE `instructors` DISABLE KEYS */;
INSERT INTO `instructors` VALUES (2,'Sambuddho Chakravarty','Computer Science');
/*!40000 ALTER TABLE `instructors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sections`
--

DROP TABLE IF EXISTS `sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sections` (
  `section_id` int NOT NULL AUTO_INCREMENT,
  `course_code` varchar(10) NOT NULL,
  `instructor_id` int DEFAULT NULL,
  `day_time` varchar(50) NOT NULL,
  `room` varchar(10) NOT NULL,
  `capacity` int NOT NULL,
  `semester` enum('Fall','Spring','Summer') NOT NULL,
  `year` int NOT NULL,
  PRIMARY KEY (`section_id`),
  KEY `course_code` (`course_code`),
  KEY `instructor_id` (`instructor_id`),
  CONSTRAINT `sections_ibfk_1` FOREIGN KEY (`course_code`) REFERENCES `courses` (`code`),
  CONSTRAINT `sections_ibfk_2` FOREIGN KEY (`instructor_id`) REFERENCES `instructors` (`user_id`),
  CONSTRAINT `sections_chk_1` CHECK ((`capacity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sections`
--

LOCK TABLES `sections` WRITE;
/*!40000 ALTER TABLE `sections` DISABLE KEYS */;
INSERT INTO `sections` VALUES (1,'CSE201',2,'TTh 11:30-13:00','Lab B305',25,'Fall',2025),(2,'MTH101',2,'MWF 10:00-11:00','Room A101',30,'Fall',2025),(3,'C222',2,'Mon 9-11','C201',50,'Summer',2);
/*!40000 ALTER TABLE `sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `settings`
--

DROP TABLE IF EXISTS `settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settings` (
  `setting_key` varchar(50) NOT NULL,
  `setting_value` varchar(255) NOT NULL,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `settings`
--

LOCK TABLES `settings` WRITE;
/*!40000 ALTER TABLE `settings` DISABLE KEYS */;
INSERT INTO `settings` VALUES ('MAINTENANCE_MODE','OFF'),('maintenance_on','OFF');
/*!40000 ALTER TABLE `settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `user_id` int NOT NULL,
  `roll_no` varchar(20) NOT NULL,
  `program` varchar(50) NOT NULL,
  `year` int NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `roll_no` (`roll_no`),
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `auth_db`.`users_auth` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (3,'2024596','CSE',2),(5,'2024597','CSAM',2),(6,'2024598','CSSS',1),(7,'2025001','CSE',1),(8,'2025002','CSAM',2),(9,'2025003','CSSS',2),(10,'2024533','CSE',2),(13,'2024333','CSAI',3),(99999,'RT-000','TestProg',1);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-15 13:09:01

-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: auth_db
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
-- Table structure for table `users_auth`
--

DROP TABLE IF EXISTS `users_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_auth` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('Admin','Instructor','Student') NOT NULL,
  `status` enum('Active','Locked','Inactive') DEFAULT 'Active',
  `last_login` timestamp NULL DEFAULT NULL,
  `failed_attempts` int DEFAULT '0',
  `locked_until` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_auth`
--

LOCK TABLES `users_auth` WRITE;
/*!40000 ALTER TABLE `users_auth` DISABLE KEYS */;
INSERT INTO `users_auth` VALUES (1,'admin','$2a$12$R62RZ3S7clI1BuOa3BL60u3qxkfs5XgAvPaRT/xkuxQPR5o/dru/a','Admin','Active',NULL,0,NULL),(2,'instructor_1','$2a$12$1CrqjHJ2koDqkIJMReWFGOKJFw16qgC7awUo5IP2yQRky2aMxbD2O','Instructor','Active',NULL,0,NULL),(3,'student_1','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(5,'student_2','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(6,'student_3','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(7,'student_test_1','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(8,'student_test_2','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(9,'student_test_3','$2a$12$xcAOY55CS4AF8HgtC7K.feQETNsYd9/rpb02/tx5jpvtpZLJhRZFC','Student','Active',NULL,0,NULL),(10,'vaibhav','$2a$12$TL.ZLAjPTovGjc6NnhFuTu/SCOhH8eguQGkhoioJIzLOcrjuLkip.','Student','Active',NULL,0,NULL),(13,'bablu','$2a$12$hD.McN5jKSNrV7uFqEw2FuGdRi/hVjsg92giYOO4YWiweRyOMgLi6','Student','Active',NULL,0,NULL),(99999,'smoke_test','$2a$12$g6Dex.OanZo48zwEc6hzwe3ucR1ZgFuh6zHxZ2NhHqNsXSlH231we','Student','Active',NULL,0,NULL);
/*!40000 ALTER TABLE `users_auth` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-15 13:10:11

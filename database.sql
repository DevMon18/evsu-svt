-- Create the database
DROP DATABASE IF EXISTS evsu_violation_db;
CREATE DATABASE evsu_violation_db;
USE evsu_violation_db;

-- Drop tables if they exist
DROP TABLE IF EXISTS VIOLATION;
DROP TABLE IF EXISTS STUDENT;
DROP TABLE IF EXISTS USER;
DROP TABLE IF EXISTS OFFENSE_CATEGORY;

-- Create USER table
CREATE TABLE IF NOT EXISTS USER (
    userID INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    firstName VARCHAR(50),
    lastName VARCHAR(50),
    role ENUM('ADMIN', 'TEACHER') NOT NULL,
    email VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    lastLogin TIMESTAMP NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    profile_image LONGBLOB
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create Student table
CREATE TABLE STUDENT (
    studentID VARCHAR(20) PRIMARY KEY,
    firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    course VARCHAR(50) NOT NULL,
    yearLevel INT NOT NULL,
    contactNumber VARCHAR(20),
    parentEmail VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create Offense Category table
CREATE TABLE OFFENSE_CATEGORY (
    categoryID INT PRIMARY KEY AUTO_INCREMENT,
    categoryName VARCHAR(100) NOT NULL,
    defaultSeverity VARCHAR(20) NOT NULL,
    description TEXT,
    parent_id INTEGER,
    CONSTRAINT fk_parent 
        FOREIGN KEY (parent_id) 
        REFERENCES OFFENSE_CATEGORY(categoryID) 
        ON DELETE SET NULL
);

-- Create Violation table
CREATE TABLE VIOLATION (
    violationID INT PRIMARY KEY AUTO_INCREMENT,
    studentID VARCHAR(20) NOT NULL,
    userID INT,
    categoryID INT NOT NULL,
    violationDate DATE NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT,
    resolved_by INT,
    resolution_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (studentID) REFERENCES STUDENT(studentID) ON UPDATE CASCADE,
    FOREIGN KEY (userID) REFERENCES USER(userID) ON UPDATE CASCADE,
    FOREIGN KEY (categoryID) REFERENCES OFFENSE_CATEGORY(categoryID) ON UPDATE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES USER(userID) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create Violation History table
CREATE TABLE VIOLATION_HISTORY (
    historyID INT PRIMARY KEY AUTO_INCREMENT,
    violationID INT NOT NULL,
    userID INT NOT NULL,
    action_type ENUM('STATUS_CHANGE', 'NOTE_ADDED', 'RESOLUTION_UPDATED') NOT NULL,
    old_value TEXT,
    new_value TEXT,
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (violationID) REFERENCES VIOLATION(violationID) ON UPDATE CASCADE,
    FOREIGN KEY (userID) REFERENCES USER(userID) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default users (admin and teacher)
INSERT INTO USER (username, password, firstName, lastName, role, email, status) VALUES
('admin', SHA2('Admin@123', 256), 'System', 'Administrator', 'ADMIN', 'admin@evsu.edu.ph', 'ACTIVE'),
('teacher', SHA2('Teacher@123', 256), 'Default', 'Teacher', 'TEACHER', 'teacher@evsu.edu.ph', 'ACTIVE'),
('teacher1', SHA2('Teacher@123', 256), 'John', 'Smith', 'TEACHER', 'john.smith@evsu.edu.ph', 'ACTIVE'),
('teacher2', SHA2('Teacher@123', 256), 'Mary', 'Johnson', 'TEACHER', 'mary.johnson@evsu.edu.ph', 'ACTIVE');

-- Insert default offense categories
INSERT INTO OFFENSE_CATEGORY (categoryName, defaultSeverity, description) VALUES
('Dress Code Violation', 'Minor', 'Violations related to school uniform and dress code policies'),
('Attendance Issues', 'Minor', 'Tardiness, cutting classes, and unauthorized absences'),
('Academic Dishonesty', 'Severe', 'Cheating, plagiarism, and other forms of academic misconduct'),
('Behavioral Misconduct', 'Moderate', 'Disruptive behavior, insubordination, and misconduct'),
('Property Damage', 'Moderate', 'Vandalism and damage to school property');

-- Insert sample students
INSERT INTO STUDENT (studentID, firstName, lastName, course, yearLevel, contactNumber, parentEmail) VALUES
('2024-0001', 'Ritzmond', 'Manzo', 'BSIT', 4, '09123456789', 'parent1@example.com'),
('2024-0002', 'Jane', 'Smith', 'BSCS', 3, '09234567890', 'parent2@example.com'),
('2024-0003', 'Mike', 'Johnson', 'BSCpE', 1, '09345678901', 'parent3@example.com'),
('2024-0004', 'Sarah', 'Williams', 'BSIT', 2, '09456789012', 'parent4@example.com'),
('2024-0005', 'David', 'Brown', 'BSCS', 4, '09567890123', 'parent5@example.com');


-- Insert sample violations
INSERT INTO VIOLATION (studentID, userID, categoryID, violationDate, description, severity, status) VALUES
('2024-0001', 1, 1, '2024-01-15', 'Not wearing proper school uniform', 'Minor', 'PENDING'),
('2024-0002', 1, 2, '2024-01-16', 'Late for class by 30 minutes', 'Minor', 'ACTIVE'),
('2024-0003', 1, 3, '2024-01-17', 'Caught cheating during exam', 'Severe', 'IN_PROGRESS'),
('2024-0004', 1, 4, '2024-01-18', 'Disruptive behavior in class', 'Moderate', 'ACTIVE'),
('2024-0005', 1, 5, '2024-01-19', 'Vandalized classroom chair', 'Moderate', 'PENDING'),
('2024-0001', 1, 2, '2024-01-20', 'Unauthorized absence', 'Minor', 'PENDING'),
('2024-0002', 1, 4, '2024-01-21', 'Verbal altercation with classmate', 'Moderate', 'ACTIVE'),
('2024-0003', 1, 1, '2024-01-22', 'Improper haircut', 'Minor', 'RESOLVED'),
('2024-0004', 1, 3, '2024-01-23', 'Plagiarism in term paper', 'Severe', 'IN_PROGRESS'),
('2024-0005', 1, 2, '2024-01-24', 'Cutting classes', 'Minor', 'PENDING');
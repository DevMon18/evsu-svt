# EVSU - OC Student  Violation Tracker

## Overview
The **Student Policy Violation Tracker** is a system designed to help schools track and manage student violations based on predefined offense categories. The system allows users (admin, teachers, and counselors) to log, view, and manage student violations, providing efficient reporting tools for administrative use.

## Project Objectives
- **Log violations** with predefined categories.
- **Customize, add, or delete** offense categories.
- **Centralized dashboard** for tracking violations by date, severity, and student.
- **Search and filter violations** by student, type, or date.
- **Maintain student violation histories**.
- **Categorize offenses** and generate reports for administrative use.
- **Generate reports** by student, offense, or time period.
- **Replace paper logs** with digital forms accessible on any device.
- **Quick-fill templates** for common offenses to streamline data entry.
- **User-friendly interface** with different access levels (admins, teachers, counselors).

---

## Key Components and Features

### 1. **Login & Authentication**
- **User Role:** Staff (Admin, Teacher, Counselor)
- **Login Fields:** Username/Email and Password
- **Validation:** Admin, Teacher, and Counselor access levels based on credentials.

### 2. **Violation Logging**
- **User Role:** Teacher
- **Fields:** 
  - Select Student ID
  - Offense Category (Predefined or Custom)
  - Violation Description
  - Severity (Minor, Moderate, Severe)
  - Attachments (optional)
- **Actions:** Submit Violation and Save to Student Record.

### 3. **Offense Categories Management**
- **User Role:** Admin
- **Actions:** Add, Edit, or Delete offense categories.
- **Auto-assignment:** Offenses assigned severity based on predefined rules.

### 4. **Search & Filter Violations**
- **User Role:** Admin, Teacher, Counselor
- **Search Criteria:** 
  - By Student (Name, ID)
  - By Violation Type (Category)
  - By Date Range
  - By Severity or Status (Pending, Resolved)

### 5. **Violation History**
- **User Role:** Admin, Teacher, Counselor
- **Fields:** Student Name, Violation Description, Date, Severity, Status
- **Actions:** View complete violation history for a student.

### 6. **Dashboard Overview**
- **User Role:** Admin
- **Statistics:**
  - Total violations, breakdown by category and severity.
  - Violation frequency over time.
  - Quick summary of pending/resolved violations.

### 7. **Reporting & Data Export**
- **User Role:** Admin, Counselor
- **Report Options:**
  - By Student, Violation Type, or Time Period.
  - Exportable to PDF/Excel.

### 8. **Notification System**
- **User Role:** Admin, Teacher
- **Actions:** Send automatic notifications to students' parents/guardians upon logging violations.

---

## GUI Design

### 1. **Login Panel**
- **Fields:** Username, Password
- **Button:** Login
- **Action:** Validates credentials and redirects to role-specific dashboard (Admin, Teacher, Counselor).


### 2. **Violation Log Panel**
- **Fields:** 
  - Student ID
  - Violation Description
  - Offense Category (ComboBox)
  - Severity (ComboBox)
- **Button:** Submit Violation
- **Action:** Submits violation data and stores it in the database.


### 3. **Violation History Panel**
- **Table:** Displays student violation history.
- **Columns:** Student ID, Violation Type, Date, Severity, Status


### 4. **Main Application Window**
- **Navigation Menu:**
  - Log Violation
  - View Violation History
- **Switching Panels:** Based on user actions (log violation or view history).


---

## Process Flow

### 1. **User Login**
- Users log in using their username and password.
- The system validates the credentials and redirects users to their specific dashboards based on their roles.

### 2. **Violation Logging**
- Teachers log violations for students by selecting the student ID, violation type, description, and severity.
- The violation is stored in the student's profile for future reference.

### 3. **Search and Filter Violations**
- Users can filter and search for specific violations based on various criteria (student ID, offense type, date, etc.).

### 4. **Reports and Data Export**
- Admins can generate reports of violations for specific students or categories and export them in PDF or Excel formats.


## Conclusion
The **EVSU - OC Student Policy Violation Tracker** system simplifies the management of student violations, improving efficiency and accessibility. The system ensures accurate record-keeping, easy reporting, and role-based access to meet the needs of staff at various levels.

To create this system with a modern style
1. Use JavaFX for the GUI, as it provides a more modern and flexible approach compared to Swing.
2. Implement a responsive design using JavaFX's layout managers (e.g., BorderPane, GridPane) to ensure the UI adapts to different screen sizes.
3. Use FXML for designing complex UI layouts, which allows for better separation of concerns between UI design and logic.
4. Implement the Model-View-Controller (MVC) or Model-View-ViewModel (MVVM) pattern for better code organization and maintainability.
5. Use Java Database Connectivity (JDBC) for database interactions.
6. Implement multi-threading for background tasks (e.g., database operations, report generation) to keep the UI responsive.
7. Use Java's built-in logging framework (java.util.logging) or a third-party library like Log4j for proper error handling and logging.
8. Implement user authentication using secure hashing algorithms (e.g., bcrypt) for password storage.
9. Use Java's Properties API for managing application configuration.
10. Implement data validation and input sanitization to ensure data integrity and security.
11. Use CSS styling for JavaFX components to create a modern and consistent look across the application.
12. Consider using dependency injection frameworks like Spring for better modularity and testability.

4. A build tool like Maven for dependency management
5. A database system XAMPP (MySQL)

Pseudo Code:
PROGRAM StudentPolicyViolationTracker

// Main application
FUNCTION main()
    INITIALIZE JavaFX application
    LOAD main.fxml
    DISPLAY LoginPanel

// Login Panel
FUNCTION loginUser(username, password)
    IF authenticateUser(username, password) THEN
        userRole = getUserRole(username)
        SWITCH userRole
            CASE ADMIN:
                DISPLAY AdminDashboard
            CASE TEACHER:
                DISPLAY TeacherDashboard
            CASE COUNSELOR:
                DISPLAY CounselorDashboard
        END SWITCH
    ELSE
        DISPLAY error message
    END IF

// Violation Logging (for Teachers)
FUNCTION logViolation(studentID, offenseCategory, description, severity)
    CREATE new Violation object
    SET Violation properties
    SAVE Violation to database
    UPDATE student violation history
    IF autoNotification enabled THEN
        SEND notification to parent/guardian
    END IF

// Offense Categories Management (for Admin)
FUNCTION manageOffenseCategories()
    DISPLAY list of current offense categories
    ALLOW add, edit, delete categories
    UPDATE database with changes

// Search and Filter Violations
FUNCTION searchViolations(criteria)
    QUERY database based on search criteria
    RETURN list of matching violations

// Generate Reports
FUNCTION generateReport(reportType, parameters)
    QUERY database for required data
    FORMAT data according to report type
    EXPORT report as PDF or Excel

// Dashboard Overview (for Admin)
FUNCTION updateDashboard()
    CALCULATE violation statistics
    DISPLAY charts and summaries

END PROGRAM



READ THIS A MUST!

Superadmin account: 
Username: Admin
Password:admin123

You must use this: 
- I am using version Java 2 , javax.mail-1.6.2, openjfx-21.0.5, mysql-connector-j-9.1.0
- VSCODE as an IDE
- DONT USE MAVEN 

Path:
JavaFX : C:\Java\openjfx-21.0.5_windows-x64_bin-sdk\javafx-sdk-21.0.5\lib
Maven: C:\Java\apache-maven-3.9.9
MySQL Connector: C:\Java\mysql-connector-j-9.1.0\mysql-connector-j-9.1.0


Create the system using modern style so that it must be appealing with modernize styling with responsive design.
Give me checklist what is completed and what is remaining, do only what is tasked


# EVSU Violation Tracker - API Documentation

## Overview

This documentation covers the core APIs and components of the EVSU Violation Tracker system. The system is built using JavaFX and follows the MVC (Model-View-Controller) architecture pattern.

## Core Components

### 1. Database Connection
```java
package com.evsu.violation.util;

public class DatabaseConnection {
    /**
     * Establishes MySQL database connection
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection()
}
```

### 2. Email Service
```java
package com.evsu.violation.util;

public class EmailService {
    /**
     * Sends email notifications
     * @param toEmail recipient's email
     * @param subject email subject
     * @param plainTextBody email content
     */
    public static void sendEmail(String toEmail, 
                               String subject, 
                               String plainTextBody)
}
```

### 3. Models

#### Student Model
```java
package com.evsu.violation.models;

public class Student {
    private String studentID;
    private String firstName;
    private String lastName;
    private String course;
    private int yearLevel;
    private String contactNumber;
    private String parentEmail;

    // Constructor
    public Student(String studentID, String firstName, 
                  String lastName, String course, 
                  int yearLevel, String contactNumber, 
                  String parentEmail)

    // Getters and Setters
}
```

#### Violation Model
```java
package com.evsu.violation.models;

public class Violation {
    private int violationID;
    private String studentID;
    private int categoryID;
    private String description;
    private String severity;
    private String status;

    // Constructor
    public Violation(int violationID, String studentID, 
                    int categoryID, String description, 
                    String severity, String status)

    // Getters and Setters
}
```

### 4. Controllers

#### LoginController
```java
package com.evsu.violation.controllers;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    /**
     * Handles login button click
     * @param event ActionEvent
     */
    @FXML
    private void handleLoginButton(ActionEvent event)

    /**
     * Initializes the controller
     */
    @FXML
    public void initialize()
}
```

#### ViolationsController
```java
package com.evsu.violation.controllers;

public class ViolationsController {
    @FXML private TableView<Violation> violationsTable;
    @FXML private ComboBox<String> statusComboBox;

    /**
     * Records new violation
     * @param violation Violation object
     * @return success status
     */
    public boolean recordViolation(Violation violation)

    /**
     * Updates violation status
     * @param violationID ID of violation
     * @param newStatus updated status
     * @param notes additional notes
     * @return success status
     */
    public boolean updateViolationStatus(int violationID, 
                                       String newStatus, 
                                       String notes)
}
```

## Database Schema

### Tables

#### USER Table
```sql
CREATE TABLE USER (
    userID INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    firstName VARCHAR(50),
    lastName VARCHAR(50),
    role ENUM('ADMIN', 'TEACHER') NOT NULL,
    email VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'
);
```

#### STUDENT Table
```sql
CREATE TABLE STUDENT (
    studentID VARCHAR(20) PRIMARY KEY,
    firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    course VARCHAR(50) NOT NULL,
    yearLevel INT NOT NULL,
    contactNumber VARCHAR(20),
    parentEmail VARCHAR(100)
);
```

#### VIOLATION Table
```sql
CREATE TABLE VIOLATION (
    violationID INT PRIMARY KEY AUTO_INCREMENT,
    studentID VARCHAR(20) NOT NULL,
    userID INT,
    categoryID INT NOT NULL,
    violationDate DATE NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'IN_PROGRESS', 'RESOLVED')
);
```

## Error Handling

### Database Errors
```java
try {
    Connection conn = DatabaseConnection.getConnection();
    // Database operations
} catch (SQLException e) {
    AlertHelper.showError("Database Error", 
        "Failed to connect to database: " + e.getMessage());
}
```

### Email Errors
```java
try {
    EmailService.sendEmail(toEmail, subject, body);
} catch (MessagingException e) {
    AlertHelper.showError("Email Error", 
        "Failed to send email: " + e.getMessage());
}
```

## Best Practices

### 1. Database Operations
- Use prepared statements to prevent SQL injection
- Close connections in finally blocks
- Use connection pooling for better performance

### 2. Email Service
- Send emails asynchronously to prevent UI blocking
- Validate email addresses before sending
- Use HTML templates for professional formatting

### 3. User Interface
- Follow JavaFX best practices
- Implement responsive design
- Use CSS for styling

### 4. Security
- Hash passwords using secure algorithms
- Implement role-based access control
- Validate all user inputs

## Dependencies

- JavaFX SDK 21.0.5
- MySQL Connector/J 9.1.0
- javax.mail 1.6.2

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── evsu/
│   │           └── violation/
│   │               ├── controllers/
│   │               ├── models/
│   │               └── util/
│   └── resources/
│       ├── css/
│       └── fxml/
```

## Support

For technical assistance:
- Course: [Your Course]
- Instructor: [Instructor Name]
- Student: [Your Name]
- ID: [Your Student ID]
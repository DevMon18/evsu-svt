# EVSU Violation Tracker - Installation Guide

## System Requirements

### Hardware Requirements
- Processor: Intel Core i3 or equivalent (i5 recommended)
- RAM: 4GB minimum (8GB recommended)
- Storage: 500MB free space
- Internet connection for email notifications

### Software Prerequisites
1. **Java Development Kit (JDK)**
   - Version: Java SE Development Kit 21
   - Download: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

2. **JavaFX SDK**
   - Version: JavaFX SDK 21.0.5
   - Download: [JavaFX](https://gluonhq.com/products/javafx/)
   - Required Path: `C:\Java\openjfx-21.0.5_windows-x64_bin-sdk\javafx-sdk-21.0.5\lib`

3. **XAMPP**
   - Version: Latest (for MySQL database)
   - Download: [XAMPP](https://www.apachefriends.org/download.html)

4. **Visual Studio Code**
   - Download: [VS Code](https://code.visualstudio.com/download)
   - Required Extensions:
     - Extension Pack for Java
     - JavaFX Support
     - MySQL (optional, for database management)

## Installation Steps

### 1. Development Environment Setup

#### JDK Installation
1. Run JDK 21 installer
2. Add System Environment Variables:
   ```
   JAVA_HOME = C:\Program Files\Java\jdk-21
   PATH = %JAVA_HOME%\bin
   ```
3. Verify installation: `java -version`

#### JavaFX Setup
1. Extract JavaFX SDK to `C:\Java\openjfx-21.0.5_windows-x64_bin-sdk`
2. Add to VS Code settings.json:
   ```json
   {
     "java.project.referencedLibraries": [
       "lib/**/*.jar",
       "C:\\Java\\openjfx-21.0.5_windows-x64_bin-sdk\\javafx-sdk-21.0.5\\lib\\*.jar"
     ]
   }
   ```

#### XAMPP Installation
1. Install XAMPP with default settings
2. Start Apache and MySQL services
3. Test access: http://localhost/phpmyadmin

### 2. Project Setup

1. **Create Project Directory**
   ```bash
   mkdir C:\javaDev\evsuvt
   cd C:\javaDev\evsuvt
   ```

2. **Copy Project Files**
   - Copy all source files to `C:\javaDev\evsuvt`
   - Maintain directory structure:
     ```
     evsuvt/
     ├── src/
     │   └── main/
     │       └── java/
     │           └── com/
     │               └── evsu/
     │                   └── violation/
     ├── lib/
     ├── docs/
     └── database.sql
     ```

### 3. Database Configuration

1. **Create Database**
   - Open phpMyAdmin (http://localhost/phpmyadmin)
   - Create new database: `evsu_violation_db`
   - Import `database.sql`

2. **Verify Database Connection**
   - Open `src/main/java/com/evsu/violation/util/DatabaseConnection.java`
   - Confirm settings:
     ```java
     url = "jdbc:mysql://localhost:3306/evsu_violation_db"
     user = "root"
     password = ""
     ```

### 4. Email Configuration

1. **Setup Email Service**
   - Open `src/main/java/com/evsu/violation/util/EmailService.java`
   - Update if needed:
     ```java
     USERNAME = "violationstudent@gmail.com"
     PASSWORD = "your-app-specific-password"
     ```

### 5. Running the Application

1. **Using VS Code**
   - Open project in VS Code
   - Run `Main.java`

2. **Using Command Line**
   - Navigate to project directory
   - Run: `run.bat`

### 6. Default Accounts

```
Administrator:
Username: admin
Password: Admin@123

Teacher:
Username: teacher
Password: Teacher@123
```

## Troubleshooting Guide

### Common Issues

1. **JavaFX Not Found**
   ```
   Error: JavaFX runtime components are missing
   ```
   Solution:
   - Verify JavaFX SDK installation
   - Check VS Code settings.json
   - Confirm VM arguments in launch configuration

2. **Database Connection Failed**
   ```
   Error: Communications link failure
   ```
   Solution:
   - Start MySQL in XAMPP
   - Check database credentials
   - Verify database exists

3. **Email Service Error**
   ```
   Error: Authentication failed
   ```
   Solution:
   - Check internet connection
   - Verify Gmail credentials
   - Enable 2FA and use App Password

## Support

For technical support:
- Course: [Your Course]
- Instructor: [Instructor Name]
- Email: [Your Email]
- Student ID: [Your ID]
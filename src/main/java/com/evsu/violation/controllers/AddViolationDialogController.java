package main.java.com.evsu.violation.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.java.com.evsu.violation.util.AlertHelper;
import main.java.com.evsu.violation.util.DatabaseConnection;
import main.java.com.evsu.violation.controllers.ViolationsController.ViolationEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AddViolationDialogController {
    @FXML private ComboBox<StudentChoice> studentComboBox;
    @FXML private ComboBox<CategoryChoice> categoryComboBox;
    @FXML private ComboBox<String> severityComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea resolutionArea;
    @FXML private Label resolutionLabel;
    @FXML private DatePicker violationDate;
    
    private Stage dialogStage;
    private boolean isConfirmed = false;
    private ViolationEntry violationToEdit;
    private int currentUserId;
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCurrentUser(int userId) {
        this.currentUserId = userId;
        // Initialize data after user ID is set
        initializeData();
    }

    @FXML
    public void initialize() {
        setupSeverityComboBox();
        setupStatusComboBox();
        setupValidation();
        
        // Set default date to today
        violationDate.setValue(LocalDate.now());
        
        // Auto-set severity when category changes
        categoryComboBox.setOnAction(e -> {
            CategoryChoice selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                severityComboBox.setValue(selectedCategory.defaultSeverity);
            }
        });

        // Show/hide resolution notes based on status
        statusComboBox.setOnAction(e -> {
            boolean isResolved = "RESOLVED".equals(statusComboBox.getValue());
            resolutionLabel.setVisible(isResolved);
            resolutionArea.setVisible(isResolved);
        });
    }

    private void initializeData() {
        loadStudents();
        loadCategories();
    }
    
    private void loadStudents() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT studentID, firstName, lastName, parentEmail FROM STUDENT ORDER BY lastName, firstName")) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                StudentChoice student = new StudentChoice(
                    rs.getString("studentID"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("parentEmail")
                );
                studentComboBox.getItems().add(student);
            }
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to load students: " + e.getMessage());
        }
    }
    
    private void loadCategories() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT categoryID, categoryName, defaultSeverity FROM OFFENSE_CATEGORY ORDER BY categoryName")) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categoryComboBox.getItems().add(new CategoryChoice(
                    rs.getInt("categoryID"),
                    rs.getString("categoryName"),
                    rs.getString("defaultSeverity")
                ));
            }
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    private void setupSeverityComboBox() {
        severityComboBox.setItems(FXCollections.observableArrayList(
            "Minor", "Moderate", "Severe"
        ));
    }
    
    private void setupStatusComboBox() {
        statusComboBox.setItems(FXCollections.observableArrayList(
            "PENDING", "ACTIVE", "IN_PROGRESS", "RESOLVED"
        ));
        // Set default status for new violations
        if (violationToEdit == null) {
            statusComboBox.setValue("PENDING");
        }
    }
    
    private void setupValidation() {
        studentComboBox.setOnAction(e -> validateForm());
        categoryComboBox.setOnAction(e -> validateForm());
        severityComboBox.setOnAction(e -> validateForm());
        statusComboBox.setOnAction(e -> validateForm());
        descriptionArea.textProperty().addListener((obs, old, newVal) -> validateForm());
        violationDate.valueProperty().addListener((obs, old, newVal) -> validateForm());
    }
    
    public void setViolation(ViolationEntry violation) {
        this.violationToEdit = violation;
        
        // Find and select the student
        studentComboBox.getItems().stream()
            .filter(s -> s.studentID.equals(violation.getStudentID()))
            .findFirst()
            .ifPresent(studentComboBox::setValue);
        
        // Find and select the category
        categoryComboBox.getItems().stream()
            .filter(c -> c.categoryName.equals(violation.getCategory()))
            .findFirst()
            .ifPresent(categoryComboBox::setValue);
        
        severityComboBox.setValue(violation.getSeverity());
        statusComboBox.setValue(violation.getStatus().toUpperCase());
        descriptionArea.setText(violation.getDescription());
        violationDate.setValue(violation.getViolationDate());

        // Load resolution notes if status is RESOLVED
        if ("RESOLVED".equals(violation.getStatus().toUpperCase())) {
            loadResolutionNotes(violation.getViolationID());
        }
    }
    
    private void loadResolutionNotes(int violationId) {
        String query = "SELECT resolution_notes FROM VIOLATION WHERE violationID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, violationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                resolutionArea.setText(rs.getString("resolution_notes"));
            }
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to load resolution notes: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSave() {
        if (!validateForm()) {
            AlertHelper.showError("Validation Error", "Please fill in all required fields.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (violationToEdit == null) {
                insertViolation(conn);
            } else {
                updateViolation(conn);
            }
            isConfirmed = true;
            dialogStage.close();
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to save violation: " + e.getMessage());
        }
    }

    private void insertViolation(Connection conn) throws SQLException {
        if (currentUserId == 0) {
            throw new SQLException("No user ID set. Please ensure you are properly logged in.");
        }

        String sql = """
            INSERT INTO VIOLATION (studentID, userID, categoryID, violationDate, 
                                 description, severity, status, resolution_notes, 
                                 resolved_by, resolution_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            StudentChoice selectedStudent = studentComboBox.getValue();
            String newStatus = statusComboBox.getValue();
            CategoryChoice selectedCategory = categoryComboBox.getValue();
            
            stmt.setString(1, selectedStudent.studentID);
            stmt.setInt(2, currentUserId);
            stmt.setInt(3, selectedCategory.categoryID);
            stmt.setDate(4, java.sql.Date.valueOf(violationDate.getValue()));
            stmt.setString(5, descriptionArea.getText());
            stmt.setString(6, severityComboBox.getValue());
            stmt.setString(7, newStatus);
            
            if ("RESOLVED".equals(newStatus)) {
                stmt.setString(8, resolutionArea.getText());
                stmt.setInt(9, currentUserId);
                stmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(8, Types.VARCHAR);
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.TIMESTAMP);
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0 && selectedStudent.parentEmail != null && !selectedStudent.parentEmail.isEmpty()) {
                // Send email notification to parent
                String studentName = selectedStudent.firstName + " " + selectedStudent.lastName;
                String parentEmail = selectedStudent.parentEmail;
                
                System.out.println("Attempting to send parent notification:");
                System.out.println("Parent Email: " + parentEmail);
                System.out.println("Status: " + newStatus);
                
                String parentSubject = "EVSU OC - New Student Violation Record";
                String parentBody = String.format("""
                    Dear Parent/Guardian,
                    
                    We hope this message finds you well. This is an official notification from the EVSU Office of Student Affairs regarding a violation record for your child.
                    
                    Student Information:
                    Student Name: %s
                    Student ID: %s
                    
                    Violation Details:
                    Type: %s
                    Severity: %s
                    Status: %s
                    Date: %s
                    
                    Description:
                    %s
                    
                    Important Notice:
                    Please take time to discuss this matter with your child. Your involvement in addressing this situation is crucial for their academic and personal development.
                    
                    Next Steps:
                    1. Review the violation details with your child
                    2. Ensure compliance with school policies
                    3. Visit the Office of Student Affairs if needed
                    
                    For any questions or concerns, please visit the Office of Student Affairs during office hours.
                    
                    Best regards,
                    EVSU OC Student Affairs Office
                    """, 
                    studentName,
                    selectedStudent.studentID,
                    selectedCategory.categoryName,
                    severityComboBox.getValue(),
                    newStatus,
                    violationDate.getValue(),
                    descriptionArea.getText()
                );
                
                main.java.com.evsu.violation.util.EmailService.sendEmail(parentEmail, parentSubject, parentBody);
                AlertHelper.showToast("Violation saved and notification sent to parent: " + parentEmail, null);
            } else {
                AlertHelper.showToast("Violation saved. No parent email available for notification.", null);
            }
        }
    }

    private void updateViolation(Connection conn) throws SQLException {
        if (currentUserId == 0) {
            throw new SQLException("No user ID set. Please ensure you are properly logged in.");
        }

        String sql = """
            UPDATE VIOLATION 
            SET studentID = ?, 
                categoryID = ?,
                violationDate = ?, description = ?, severity = ?, status = ?,
                resolution_notes = ?, resolved_by = ?, resolution_date = ?
            WHERE violationID = ?
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            StudentChoice selectedStudent = studentComboBox.getValue();
            String newStatus = statusComboBox.getValue();
            CategoryChoice selectedCategory = categoryComboBox.getValue();
            
            stmt.setString(1, selectedStudent.studentID);
            stmt.setInt(2, selectedCategory.categoryID);
            stmt.setDate(3, java.sql.Date.valueOf(violationDate.getValue()));
            stmt.setString(4, descriptionArea.getText());
            stmt.setString(5, severityComboBox.getValue());
            stmt.setString(6, newStatus);
            
            if ("RESOLVED".equals(newStatus)) {
                stmt.setString(7, resolutionArea.getText());
                stmt.setInt(8, currentUserId);
                stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.INTEGER);
                stmt.setNull(9, Types.TIMESTAMP);
            }
            
            stmt.setInt(10, violationToEdit.getViolationID());
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0 && selectedStudent.parentEmail != null && !selectedStudent.parentEmail.isEmpty()) {
                // Send email notification to parent
                String studentName = selectedStudent.firstName + " " + selectedStudent.lastName;
                String parentEmail = selectedStudent.parentEmail;
                
                System.out.println("Attempting to send parent notification:");
                System.out.println("Parent Email: " + parentEmail);
                System.out.println("Status: " + newStatus);
                
                String parentSubject = "EVSU OC - Student Violation Update";
                String parentBody = String.format("""
                    Dear Parent/Guardian,
                    
                    We hope this message finds you well. This is an official update from the EVSU Office of Student Affairs regarding your child's violation record.
                    
                    Student Information:
                    Student Name: %s
                    Student ID: %s
                    
                    Updated Violation Details:
                    Type: %s
                    Status: %s
                    Date: %s
                    
                    %s
                    
                    Important Notice:
                    Your continued support in addressing this matter is greatly appreciated. Please ensure ongoing compliance with school policies.
                    
                    Next Steps:
                    1. Review the updated violation status
                    2. Follow up with your child regarding any required actions
                    3. Contact the Office of Student Affairs if needed
                    
                    For any questions or concerns, please visit the Office of Student Affairs during office hours.
                    
                    Best regards,
                    EVSU OC Student Affairs Office
                    """, 
                    studentName,
                    selectedStudent.studentID,
                    selectedCategory.categoryName,
                    newStatus,
                    violationDate.getValue(),
                    "RESOLVED".equals(newStatus) ? 
                        String.format("Resolution Details:\n%s", resolutionArea.getText()) : 
                        "Please continue to monitor your child's compliance with school policies."
                );
                
                main.java.com.evsu.violation.util.EmailService.sendEmail(parentEmail, parentSubject, parentBody);
                AlertHelper.showToast("Violation updated and notification sent to parent: " + parentEmail, null);
            } else {
                AlertHelper.showToast("Violation updated. No parent email available for notification.", null);
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
    
    @FXML
    private void handleStudentSearch() {
        // Implement student search functionality if needed
        // For now, just show all students in the combo box
    }
    
    private boolean validateForm() {
        if (studentComboBox.getValue() == null ||
            categoryComboBox.getValue() == null ||
            severityComboBox.getValue() == null ||
            statusComboBox.getValue() == null ||
            violationDate.getValue() == null ||
            descriptionArea.getText().trim().isEmpty()) {
            return false;
        }

        // Require resolution notes if status is RESOLVED
        if ("RESOLVED".equals(statusComboBox.getValue()) && 
            resolutionArea.getText().trim().isEmpty()) {
            AlertHelper.showError("Validation Error", 
                "Please provide resolution notes when marking as RESOLVED.");
            return false;
        }

        return true;
    }
    
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    // Helper classes for ComboBox items
    private static class StudentChoice {
        final String studentID;
        final String firstName;
        final String lastName;
        final String parentEmail;
        
        StudentChoice(String studentID, String firstName, String lastName, String parentEmail) {
            this.studentID = studentID;
            this.firstName = firstName;
            this.lastName = lastName;
            this.parentEmail = parentEmail;
        }
        
        @Override
        public String toString() {
            return lastName + ", " + firstName + " (" + studentID + ")";
        }
    }
    
    private static class CategoryChoice {
        final int categoryID;
        final String categoryName;
        final String defaultSeverity;
        
        CategoryChoice(int categoryID, String categoryName, String defaultSeverity) {
            this.categoryID = categoryID;
            this.categoryName = categoryName;
            this.defaultSeverity = defaultSeverity;
        }
        
        @Override
        public String toString() {
            return categoryName;
        }
    }
} 
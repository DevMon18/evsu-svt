package main.java.com.evsu.violation.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import main.java.com.evsu.violation.models.Student;
import main.java.com.evsu.violation.models.Violation;
import main.java.com.evsu.violation.util.DatabaseConnection;
import main.java.com.evsu.violation.util.AlertHelper;
import main.java.com.evsu.violation.util.EmailService;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ViolationDialogController {
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> severityComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker violationDate;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea resolutionArea;
    @FXML private Label resolutionLabel;
    @FXML private DialogPane dialogPane;

    private Stage dialogStage;
    private int currentUserId;
    private Violation violation;
    private boolean isEditMode = false;
    private boolean confirmed = false;
    private boolean violationAdded = false;

    @FXML
    public void initialize() {
        // Initialize combo boxes
        statusComboBox.getItems().addAll("PENDING", "ACTIVE", "IN_PROGRESS", "RESOLVED");
        severityComboBox.getItems().addAll("Minor", "Moderate", "Severe");
        
        // Show/hide resolution notes based on status
        statusComboBox.setOnAction(e -> {
            boolean isResolved = "RESOLVED".equals(statusComboBox.getValue());
            resolutionLabel.setVisible(isResolved);
            resolutionArea.setVisible(isResolved);
        });

        loadCategories();
        loadStudents();
        
        // Set default values
        violationDate.setValue(LocalDate.now());

        // Set up button actions
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume(); // Prevent dialog from closing
            handleSave();
        });
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setCurrentUser(int userId) {
        this.currentUserId = userId;
    }

    public void initializeForAdd() {
        this.violation = null;
        this.isEditMode = false;
        this.violationAdded = false;
        clearFields();
    }

    public void setViolation(Violation violation) {
        this.violation = violation;
        this.isEditMode = true;
        populateFields();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isViolationAdded() {
        return violationAdded;
    }

    private void loadStudents() {
        List<Student> students = new ArrayList<>();
        String query = "SELECT studentID, firstName, lastName, course, yearLevel, contactNumber, parentEmail FROM STUDENT";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Student student = new Student(
                    rs.getString("studentID"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("course"),
                    rs.getInt("yearLevel"),
                    rs.getString("contactNumber"),
                    rs.getString("parentEmail")
                );
                students.add(student);
            }
            studentComboBox.setItems(FXCollections.observableArrayList(students));
            
            // Setup display for student names
            studentComboBox.setConverter(new javafx.util.StringConverter<Student>() {
                @Override
                public String toString(Student student) {
                    return student == null ? "" : student.getFirstName() + " " + student.getLastName();
                }

                @Override
                public Student fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            showError("Database Error", "Failed to load students: " + e.getMessage());
        }
    }

    private void loadCategories() {
        String query = "SELECT categoryName FROM OFFENSE_CATEGORY";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            List<String> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(rs.getString("categoryName"));
            }
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void setupComboBoxes() {
        severityComboBox.setItems(FXCollections.observableArrayList(
            "Minor", "Moderate", "Major", "Severe"
        ));
        statusComboBox.setItems(FXCollections.observableArrayList(
            "PENDING", "ACTIVE", "IN_PROGRESS", "RESOLVED"
        ));
    }

    private void clearFields() {
        studentComboBox.getSelectionModel().clearSelection();
        categoryComboBox.getSelectionModel().clearSelection();
        severityComboBox.getSelectionModel().clearSelection();
        statusComboBox.setValue("Pending");
        violationDate.setValue(LocalDate.now());
        descriptionArea.clear();
        resolutionArea.clear();
    }

    private void populateFields() {
        if (violation != null) {
            // Find and select the student
            studentComboBox.getItems().stream()
                .filter(s -> s.getStudentID().equals(violation.getStudentID()))
                .findFirst()
                .ifPresent(studentComboBox::setValue);
            
            categoryComboBox.setValue(violation.getCategory());
            severityComboBox.setValue(violation.getSeverity());
            statusComboBox.setValue(violation.getStatus());
            descriptionArea.setText(violation.getDescription());
            violationDate.setValue(violation.getViolationDate());
            resolutionArea.setText(violation.getResolutionNotes());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (isEditMode) {
                updateViolation(conn);
            } else {
                insertViolation(conn);
                violationAdded = true;
            }
            confirmed = true;
            dialogStage.close();
        } catch (SQLException e) {
            showError("Database Error", "Failed to save violation: " + e.getMessage());
        }
    }

    private void insertViolation(Connection conn) throws SQLException {
        String query = """
            INSERT INTO VIOLATION (studentID, userID, categoryID, violationDate, 
                                 description, severity, status, resolution_notes,
                                 resolved_by, resolution_date) 
            VALUES (?, ?, (SELECT categoryID FROM OFFENSE_CATEGORY WHERE categoryName = ?), 
                   ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            Student selectedStudent = studentComboBox.getValue();
            String newStatus = statusComboBox.getValue();
            
            stmt.setString(1, selectedStudent.getStudentID());
            stmt.setInt(2, currentUserId);
            stmt.setString(3, categoryComboBox.getValue());
            stmt.setDate(4, java.sql.Date.valueOf(violationDate.getValue()));
            stmt.setString(5, descriptionArea.getText());
            stmt.setString(6, severityComboBox.getValue());
            stmt.setString(7, newStatus);
            
            if ("RESOLVED".equals(newStatus)) {
                stmt.setString(8, resolutionArea.getText());
                stmt.setInt(9, currentUserId);
                stmt.setTimestamp(10, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                stmt.setNull(8, java.sql.Types.VARCHAR);
                stmt.setNull(9, java.sql.Types.INTEGER);
                stmt.setNull(10, java.sql.Types.TIMESTAMP);
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Send email notification to parent
                String parentEmail = selectedStudent.getParentEmail();
                String studentName = selectedStudent.getFullName();
                
                System.out.println("Attempting to send parent notification:");
                System.out.println("Parent Email: " + parentEmail);
                System.out.println("Status: " + newStatus);
                
                // Send notification to parent
                if (parentEmail != null && !parentEmail.isEmpty()) {
                    System.out.println("Sending notification to parent");
                    String parentSubject = "EVSU OC - New Student Violation Record";
                    String parentBody = String.format("""
                        Dear Parent/Guardian,
                        
                        This is to inform you about a new violation record for your child (%s):
                        
                        Violation Type: %s
                        Severity: %s
                        Status: %s
                        Date: %s
                        
                        Description: %s
                        
                        Please ensure to discuss this matter with your child.
                        For any concerns, please visit the Office of Student Affairs.
                        
                        Best regards,
                        EVSU OC Student Affairs Office
                        """, 
                        studentName,
                        categoryComboBox.getValue(),
                        severityComboBox.getValue(),
                        newStatus,
                        violationDate.getValue(),
                        descriptionArea.getText()
                    );
                    
                    EmailService.sendEmail(parentEmail, parentSubject, parentBody);
                }
            }
        }
    }

    private void updateViolation(Connection conn) throws SQLException {
        String query = """
            UPDATE VIOLATION 
            SET studentID = ?, 
                categoryID = (SELECT categoryID FROM OFFENSE_CATEGORY WHERE categoryName = ?),
                violationDate = ?, description = ?, severity = ?, status = ?,
                resolution_notes = ?, resolved_by = ?, resolution_date = ?
            WHERE violationID = ?
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            Student selectedStudent = studentComboBox.getValue();
            String newStatus = statusComboBox.getValue();
            
            stmt.setString(1, selectedStudent.getStudentID());
            stmt.setString(2, categoryComboBox.getValue());
            stmt.setDate(3, java.sql.Date.valueOf(violationDate.getValue()));
            stmt.setString(4, descriptionArea.getText());
            stmt.setString(5, severityComboBox.getValue());
            stmt.setString(6, newStatus);
            
            if ("RESOLVED".equals(newStatus)) {
                stmt.setString(7, resolutionArea.getText());
                stmt.setInt(8, currentUserId);
                stmt.setTimestamp(9, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
                stmt.setNull(8, java.sql.Types.INTEGER);
                stmt.setNull(9, java.sql.Types.TIMESTAMP);
            }
            
            stmt.setInt(10, violation.getViolationID());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Send email notification to parent
                String parentEmail = selectedStudent.getParentEmail();
                String studentName = selectedStudent.getFullName();
                
                System.out.println("Attempting to send parent notification:");
                System.out.println("Parent Email: " + parentEmail);
                System.out.println("Status: " + newStatus);
                
                // Send notification to parent
                if (parentEmail != null && !parentEmail.isEmpty()) {
                    System.out.println("Sending notification to parent");
                    String parentSubject = "EVSU OC - Student Violation Update";
                    String parentBody = String.format("""
                        Dear Parent/Guardian,
                        
                        This is to inform you about your child's (%s) violation record:
                        
                        Violation Type: %s
                        Status: %s
                        Date: %s
                        
                        %s
                        
                        Please ensure to discuss this matter with your child.
                        For any concerns, please visit the Office of Student Affairs.
                        
                        Best regards,
                        EVSU OC Student Affairs Office
                        """, 
                        studentName,
                        categoryComboBox.getValue(),
                        newStatus,
                        violationDate.getValue(),
                        "RESOLVED".equals(newStatus) ? 
                            "Resolution Notes: " + resolutionArea.getText() : 
                            "Please monitor your child's compliance with school policies."
                    );
                    
                    EmailService.sendEmail(parentEmail, parentSubject, parentBody);
                }
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();

        if (studentComboBox.getValue() == null) {
            errorMessage.append("Please select a student.\n");
        }
        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().trim().isEmpty()) {
            errorMessage.append("Please select a category.\n");
        }
        if (severityComboBox.getValue() == null || severityComboBox.getValue().trim().isEmpty()) {
            errorMessage.append("Please select a severity level.\n");
        }
        if (statusComboBox.getValue() == null || statusComboBox.getValue().trim().isEmpty()) {
            errorMessage.append("Please select a status.\n");
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            errorMessage.append("Please enter a description.\n");
        }
        if (violationDate.getValue() == null) {
            errorMessage.append("Please select a date.\n");
        }
        if ("RESOLVED".equals(statusComboBox.getValue()) && 
            (resolutionArea.getText() == null || resolutionArea.getText().trim().isEmpty())) {
            errorMessage.append("Please enter resolution notes when marking as RESOLVED.\n");
        }

        if (errorMessage.length() > 0) {
            AlertHelper.showError("Validation Error", errorMessage.toString());
            return false;
        }

        return true;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 
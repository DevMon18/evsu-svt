package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import main.java.com.evsu.violation.models.Student;
import main.java.com.evsu.violation.util.DatabaseConnection;
import main.java.com.evsu.violation.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class AddStudentDialogController {
    @FXML private TextField idNumberField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> courseField;
    @FXML private ComboBox<Integer> yearLevelField;
    @FXML private TextField contactNumberField;
    @FXML private TextField parentEmailField;
    
    @FXML private Label idNumberError;
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label courseError;
    @FXML private Label yearLevelError;
    @FXML private Label contactError;
    @FXML private Label emailError;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        yearLevelField.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        loadCourses();
        setupValidation();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public static boolean showDialog(javafx.scene.Node owner) {
        try {
            FXMLLoader loader = new FXMLLoader(AddStudentDialogController.class.getResource("/fxml/AddStudentDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Student");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(owner.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            AddStudentDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            dialogStage.showAndWait();
            
            if (controller.isConfirmed()) {
                AlertHelper.showToast("Student added successfully", owner);
                return true; // Return true if student was added successfully
            }
            return false;
        } catch (IOException e) {
            AlertHelper.showError("Error", "Could not open add student dialog: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleSave() {
        if (!validateFields()) {
            AlertHelper.showError("Validation Error", "Please fix all errors before saving.");
            return;
        }

        Student student = getStudent();
        if (student != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check if student ID already exists
                PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM STUDENT WHERE studentID = ?"
                );
                checkStmt.setString(1, student.getStudentID());
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    AlertHelper.showError("Error", "Student ID already exists.");
                    return;
                }

                // Insert new student
                String sql = """
                    INSERT INTO STUDENT (studentID, firstName, lastName, course, yearLevel, 
                                      contactNumber, parentEmail)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, student.getStudentID());
                stmt.setString(2, student.getFirstName());
                stmt.setString(3, student.getLastName());
                stmt.setString(4, student.getCourse());
                stmt.setInt(5, student.getYearLevel());
                stmt.setString(6, student.getContactNumber());
                stmt.setString(7, student.getParentEmail());
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    confirmed = true;
                    dialogStage.close();
                } else {
                    AlertHelper.showError("Error", "Failed to save student record.");
                }
            } catch (SQLException e) {
                AlertHelper.showError("Database Error", "Failed to save student: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        dialogStage.close();
    }

    private void loadCourses() {
        List<String> courses = new ArrayList<>(List.of(
            "BSIT", // Bachelor of Science in Information Technology
            "BPed", // Bachelor of Physical Education
            "BSInd-tech", // Bachelor of Science in Industrial Technology
            "BSCE", // Bachelor of Science in Civil Engineering
            "BSHM", // Bachelor of Science in Hospitality Management
            "BSHRM", // Bachelor of Science in Human Resource Management
            "BSIE", // Bachelor of Science in Industrial Engineering
            "BSEE", // Bachelor of Science in Electrical Engineering
            "BEED", // Bachelor of Elementary Education
            "BSED" // Bachelor of Secondary Education
        ));
        
        courseField.setItems(FXCollections.observableArrayList(courses));
        // Optionally set a default value
        // courseField.setValue(courses.get(0));
    }

    private void setupValidation() {
        idNumberField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d{4}-\\d{4,}")) {
                idNumberError.setText("Invalid format (YYYY-NNNN...)");
                idNumberError.setVisible(true);
            } else {
                idNumberError.setVisible(false);
            }
        });

        contactNumberField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d{11}")) {
                contactError.setText("Must be 11 digits");
                contactError.setVisible(true);
            } else {
                contactError.setVisible(false);
            }
        });

        parentEmailField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                emailError.setText("Invalid email format");
                emailError.setVisible(true);
            } else {
                emailError.setVisible(false);
            }
        });
    }

    public Student getStudent() {
        if (!validateFields()) {
            return null;
        }

        return new Student(
            idNumberField.getText().trim(),
            firstNameField.getText().trim(),
            lastNameField.getText().trim(),
            courseField.getValue(),
            yearLevelField.getValue(),
            contactNumberField.getText().trim(),
            parentEmailField.getText().trim()
        );
    }

    public void setStudent(Student student) {
        idNumberField.setText(student.getStudentID());
        firstNameField.setText(student.getFirstName());
        lastNameField.setText(student.getLastName());
        courseField.setValue(student.getCourse());
        yearLevelField.setValue(student.getYearLevel());
        contactNumberField.setText(student.getContactNumber());
        parentEmailField.setText(student.getParentEmail());
    }

    private boolean validateFields() {
        boolean isValid = true;
        errorLabel.setVisible(false);

        if (idNumberField.getText().isEmpty() || !idNumberField.getText().matches("\\d{4}-\\d{4,}")) {
            idNumberError.setText("Invalid Student ID (format: YYYY-NNNN...)");
            idNumberError.setVisible(true);
            isValid = false;
        }
        
        if (firstNameField.getText().isEmpty()) {
            firstNameError.setText("First Name is required");
            firstNameError.setVisible(true);
            isValid = false;
        }
        
        if (lastNameField.getText().isEmpty()) {
            lastNameError.setText("Last Name is required");
            lastNameError.setVisible(true);
            isValid = false;
        }
        
        if (courseField.getValue() == null) {
            courseError.setText("Course is required");
            courseError.setVisible(true);
            isValid = false;
        }
        
        if (yearLevelField.getValue() == null) {
            yearLevelError.setText("Year Level is required");
            yearLevelError.setVisible(true);
            isValid = false;
        }
        
        if (contactNumberField.getText().isEmpty() || !contactNumberField.getText().matches("\\d{11}")) {
            contactError.setText("Invalid Contact Number (11 digits required)");
            contactError.setVisible(true);
            isValid = false;
        }
        
        if (parentEmailField.getText().isEmpty() || 
            !parentEmailField.getText().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            emailError.setText("Invalid Parent Email");
            emailError.setVisible(true);
            isValid = false;
        }

        if (!isValid) {
            errorLabel.setText("Please fix the errors above");
            errorLabel.setVisible(true);
        }

        return isValid;
    }
} 
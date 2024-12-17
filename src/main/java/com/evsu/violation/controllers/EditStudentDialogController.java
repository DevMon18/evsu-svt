package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import main.java.com.evsu.violation.models.Student;
import main.java.com.evsu.violation.util.DatabaseConnection;
import javafx.collections.FXCollections;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EditStudentDialogController {
    @FXML private TextField idNumberField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> courseField;
    @FXML private ComboBox<Integer> yearLevelField;
    @FXML private TextField contactNumberField;
    @FXML private TextField parentEmailField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        yearLevelField.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        loadCourses();
        setupValidation();
        
        // Disable ID field since it's the primary key
        idNumberField.setDisable(true);
    }

    private void loadCourses() {
        List<String> courses = new ArrayList<>();
        String sql = "SELECT DISTINCT course FROM STUDENT ORDER BY course";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String course = rs.getString("course");
                if (course != null && !course.isEmpty()) {
                    courses.add(course);
                }
            }
            courseField.setItems(FXCollections.observableArrayList(courses));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupValidation() {
        firstNameField.textProperty().addListener((obs, old, newValue) -> validateField());
        lastNameField.textProperty().addListener((obs, old, newValue) -> validateField());
        courseField.valueProperty().addListener((obs, old, newValue) -> validateField());
        yearLevelField.valueProperty().addListener((obs, old, newValue) -> validateField());
        contactNumberField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                contactNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            validateField();
        });
        parentEmailField.textProperty().addListener((obs, old, newValue) -> validateField());
    }

    private void validateField() {
        StringBuilder error = new StringBuilder();
        
        if (firstNameField.getText().trim().isEmpty()) {
            error.append("First Name is required\n");
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            error.append("Last Name is required\n");
        }
        
        if (courseField.getValue() == null) {
            error.append("Course is required\n");
        }
        
        if (yearLevelField.getValue() == null) {
            error.append("Year Level is required\n");
        }
        
        if (contactNumberField.getText().trim().isEmpty() || 
            !contactNumberField.getText().matches("\\d{11}")) {
            error.append("Contact Number must be 11 digits\n");
        }
        
        if (parentEmailField.getText().trim().isEmpty() || 
            !parentEmailField.getText().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            error.append("Invalid Parent Email format\n");
        }
        
        errorLabel.setText(error.toString());
        errorLabel.setVisible(!error.toString().isEmpty());
        
        // Disable OK button if there are errors
        Button okButton = (Button) errorLabel.getScene().getWindow().getScene()
            .lookup(".dialog-pane .button:default");
        if (okButton != null) {
            okButton.setDisable(!error.toString().isEmpty());
        }
    }

    public void setStudent(Student student) {
        idNumberField.setText(student.getStudentID());
        firstNameField.setText(student.getFirstName());
        lastNameField.setText(student.getLastName());
        courseField.setValue(student.getCourse());
        yearLevelField.setValue(student.getYearLevel());
        contactNumberField.setText(student.getContactNumber());
        parentEmailField.setText(student.getParentEmail());
        
        validateField();
    }

    public Student getStudent() {
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
} 
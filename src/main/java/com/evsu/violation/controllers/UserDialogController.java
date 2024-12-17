package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import main.java.com.evsu.violation.models.User;

public class UserDialogController {
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleField;
    @FXML private ComboBox<String> statusField;
    
    private User user;
    private boolean saveClicked = false;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        roleField.setItems(FXCollections.observableArrayList(
            "ADMIN", "TEACHER"
        ));
        
        statusField.setItems(FXCollections.observableArrayList(
            "ACTIVE", "INACTIVE"
        ));

        // Set default values
        statusField.setValue("ACTIVE");
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            roleField.setValue(user.getRole());
            statusField.setValue(user.getStatus());
            passwordField.setPromptText("Leave blank to keep current password");
        }
    }
    
    public boolean isSaveClicked() {
        return saveClicked;
    }
    
    public User getUser() {
        if (!validateInputs()) {
            return null;
        }
        
        if (user == null) {
            user = new User();
        }
        
        user.setUsername(usernameField.getText().trim());
        user.setFullName(fullNameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setRole(roleField.getValue());
        user.setStatus(statusField.getValue());
        
        // Only set password if it's not empty (for new users or password changes)
        String password = passwordField.getText();
        if (!password.isEmpty()) {
            user.setPassword(password);
        }
        
        saveClicked = true;
        return user;
    }
    
    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (isNullOrEmpty(usernameField.getText())) {
            errorMessage.append("Username is required!\n");
            usernameField.getStyleClass().add("error-field");
        } else {
            usernameField.getStyleClass().remove("error-field");
        }
        
        if (isNullOrEmpty(fullNameField.getText())) {
            errorMessage.append("Full Name is required!\n");
            fullNameField.getStyleClass().add("error-field");
        } else {
            fullNameField.getStyleClass().remove("error-field");
        }
        
        if (isNullOrEmpty(emailField.getText())) {
            errorMessage.append("Email is required!\n");
            emailField.getStyleClass().add("error-field");
        } else if (!isValidEmail(emailField.getText().trim())) {
            errorMessage.append("Invalid email format!\n");
            emailField.getStyleClass().add("error-field");
        } else {
            emailField.getStyleClass().remove("error-field");
        }
        
        if (roleField.getValue() == null) {
            errorMessage.append("Role must be selected!\n");
            roleField.getStyleClass().add("error-field");
        } else {
            roleField.getStyleClass().remove("error-field");
        }
        
        if (statusField.getValue() == null) {
            errorMessage.append("Status must be selected!\n");
            statusField.getStyleClass().add("error-field");
        } else {
            statusField.getStyleClass().remove("error-field");
        }
        
        if (user == null && isNullOrEmpty(passwordField.getText())) {
            errorMessage.append("Password is required for new users!\n");
            passwordField.getStyleClass().add("error-field");
        } else {
            passwordField.getStyleClass().remove("error-field");
        }
        
        if (errorMessage.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage.toString());
            alert.showAndWait();
            return false;
        }
        
        return true;
    }
    
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    @FXML
    private void handleSave() {
        if (getUser() != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        saveClicked = false;
        dialogStage.close();
    }
} 
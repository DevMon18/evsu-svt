package main.java.com.evsu.violation.controllers;

import main.java.com.evsu.violation.models.Category;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.stage.Stage;

public class CategoryDialogController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> severityField;
    
    private Category category;
    private boolean saveClicked = false;
    private Stage dialogStage;
    
    @FXML
    private void initialize() {
        severityField.setItems(FXCollections.observableArrayList(
            "Minor", "Moderate", "Major", "Severe"
        ));
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public void setCategory(Category category) {
        this.category = category;
        if (category != null) {
            nameField.setText(category.getName());
            descriptionField.setText(category.getDescription());
            severityField.setValue(category.getSeverityLevel());
        }
    }
    
    public boolean isSaveClicked() {
        return saveClicked;
    }
    
    public Category getCategory() {
        if (!validateInputs()) {
            return null;
        }
        
        if (category == null) {
            category = new Category();
        }
        category.setName(nameField.getText().trim());
        category.setDescription(descriptionField.getText().trim());
        category.setSeverityLevel(severityField.getValue());
        saveClicked = true;
        
        return category;
    }
    
    private boolean validateInputs() {
        String errorMessage = "";
        
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage += "Category name is required!\n";
        }
        
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            errorMessage += "Description is required!\n";
        }
        
        if (severityField.getValue() == null) {
            errorMessage += "Severity level must be selected!\n";
        }
        
        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
    
    @FXML
    private void handleSave() {
        if (getCategory() != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleCancel() {
        saveClicked = false;
        dialogStage.close();
    }
} 
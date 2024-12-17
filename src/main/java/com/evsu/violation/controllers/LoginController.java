package main.java.com.evsu.violation.controllers;

import main.java.com.evsu.violation.util.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.sql.*;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private StackPane mainStackPane;
    @FXML private StackPane loadingContainer;
    @FXML private Label loadingText;
    @FXML private VBox errorContainer;
    @FXML private Label errorMessageLabel;
    @FXML private Label errorDetailsLabel;

    private Connection conn;

    @FXML
    public void initialize() {
        // Set up key event handlers
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (passwordField.getText().isEmpty()) {
                    passwordField.requestFocus();
                } else {
                    handleLogin();
                }
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        // Initialize database connection
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                showError("Database Error", "Could not connect to database", null);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize database connection", e.getMessage());
        }

        // Initially hide loading and error containers
        loadingContainer.setVisible(false);
        loadingContainer.setManaged(false);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
    }

    private void showLoading(String message) {
        loadingText.setText(message);
        
        // Create fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), loadingContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        // Show loading container
        loadingContainer.setVisible(true);
        loadingContainer.setManaged(true);
        fadeIn.play();
        
        // Disable controls
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        loginButton.setDisable(true);
    }

    private void hideLoading() {
        // Create fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), loadingContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            loadingContainer.setVisible(false);
            loadingContainer.setManaged(false);
        });
        fadeOut.play();
        
        // Enable controls
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        loginButton.setDisable(false);
    }

    private void showError(String message, String details, String technicalDetails) {
        errorMessageLabel.setText(message);
        
        if (details != null && !details.isEmpty()) {
            errorDetailsLabel.setText(details);
            errorDetailsLabel.setVisible(true);
            errorDetailsLabel.setManaged(true);
        } else {
            errorDetailsLabel.setVisible(false);
            errorDetailsLabel.setManaged(false);
        }
        
        // Log technical details if available
        if (technicalDetails != null) {
            System.err.println("Technical Error Details: " + technicalDetails);
        }
        
        // Create fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), errorContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        // Show error container
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
        fadeIn.play();
    }

    private void hideError() {
        if (errorContainer.isVisible()) {
            // Create fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), errorContainer);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                errorContainer.setVisible(false);
                errorContainer.setManaged(false);
            });
            fadeOut.play();
        }
    }

    @FXML
    private void handleLogin() {
        hideError(); // Hide any previous error
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Login Failed", "Please enter both username and password", null);
            if (username.isEmpty()) usernameField.requestFocus();
            else passwordField.requestFocus();
            return;
        }

        showLoading("Signing in..."); // Show loading indicator

        try {
            String sql = "SELECT userID, role FROM USER WHERE username = ? AND password = SHA2(?, 256) AND status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("userID");
                    String role = rs.getString("role");
                    
                    // Load appropriate dashboard
                    String fxmlPath = role.equalsIgnoreCase("ADMIN") ? 
                        "/fxml/AdminDashboard.fxml" : "/fxml/TeacherDashboard.fxml";
                    
                    try {
                        // Get the resource URL
                        java.net.URL resourceUrl = getClass().getResource(fxmlPath);
                        if (resourceUrl == null) {
                            throw new IOException("Cannot find FXML file: " + fxmlPath);
                        }
                        
                        // Create and configure the loader
                        FXMLLoader loader = new FXMLLoader(resourceUrl);
                        Parent root = loader.load();
                        
                        // Get and initialize the controller
                        Object controller = loader.getController();
                        if (controller == null) {
                            throw new IOException("Cannot get controller for: " + fxmlPath);
                        }
                        
                        // Set current user for the controller
                        if (role.equalsIgnoreCase("ADMIN")) {
                            AdminDashboardController adminController = (AdminDashboardController) controller;
                            adminController.initData(userId);
                        } else if (role.equalsIgnoreCase("TEACHER")) {
                            TeacherDashboardController teacherController = (TeacherDashboardController) controller;
                            teacherController.initData(userId);
                        }
                        
                        // Show dashboard
                        Scene scene = new Scene(root);
                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setScene(scene);
                        stage.show();
                        
                    } catch (IOException e) {
                        showError("System Error", 
                                "Failed to load dashboard. Please try again.", 
                                "FXML Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    showError("Login Failed", 
                            "Invalid username or password. Please try again.", 
                            null);
                    passwordField.clear();
                    passwordField.requestFocus();
                }
            }
        } catch (SQLException e) {
            showError("Database Error", 
                    "Unable to connect to the server. Please try again later.", 
                    e.getMessage());
            e.printStackTrace();
        } finally {
            hideLoading();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.close();
    }
} 
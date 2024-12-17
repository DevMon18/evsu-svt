package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.control.Alert;

public class SplashScreenController {
    @FXML private VBox splashScreen;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label loadingText;
    
    private double xOffset = 0;
    private double yOffset = 0;
    
    public void initialize() {
        // Create fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), splashScreen);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Start loading process
        new Thread(() -> {
            try {
                // Database initialization
                updateLoadingText("Connecting to database...");
                initializeDatabase();
                
                // Load configuration
                updateLoadingText("Loading configuration...");
                loadConfiguration();
                
                // Initialize cache
                updateLoadingText("Initializing cache...");
                initializeCache();
                
                // Prepare UI components
                updateLoadingText("Preparing interface...");
                prepareUIComponents();
                
                // Load the login screen
                Platform.runLater(() -> loadLoginScreen());
                
            } catch (Exception e) {
                Platform.runLater(() -> showErrorDialog(e));
                e.printStackTrace();
            }
        }).start();
    }
    
    private void updateLoadingText(String text) {
        Platform.runLater(() -> loadingText.setText(text));
    }
    
    private void loadLoginScreen() {
        try {
            // Create fade-out animation
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashScreen);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            
            fadeOut.setOnFinished(e -> {
                try {
                    // Create new stage for login
                    Stage loginStage = new Stage(StageStyle.DECORATED);
                    
                    // Add application icon
                    javafx.scene.image.Image icon = new javafx.scene.image.Image(getClass().getResourceAsStream("/images/mini-logo.png"));
                    loginStage.getIcons().add(icon);
                    
                    // Load login screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                    Parent loginRoot = loader.load();
                    
                    // Make the window draggable
                    loginRoot.setOnMousePressed(event -> {
                        xOffset = event.getSceneX();
                        yOffset = event.getSceneY();
                    });
                    
                    loginRoot.setOnMouseDragged(event -> {
                        loginStage.setX(event.getScreenX() - xOffset);
                        loginStage.setY(event.getScreenY() - yOffset);
                    });
                    
                    // Create and configure scene
                    Scene loginScene = new Scene(loginRoot);
                    loginScene.getStylesheets().addAll(
                        getClass().getResource("/css/login.css").toExternalForm(),
                        getClass().getResource("/css/style.css").toExternalForm()
                    );
                    
                    // Configure stage
                    loginStage.setScene(loginScene);
                    loginStage.setTitle("EVSU-OC SVT");
                    
                    // Set minimum size
                    loginStage.setMinWidth(800);
                    loginStage.setMinHeight(600);
                    
                    // Set initial size and position
                    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                    double initialWidth = Math.min(1200, screenBounds.getWidth() * 0.8);
                    double initialHeight = Math.min(800, screenBounds.getHeight() * 0.8);
                    loginStage.setWidth(initialWidth);
                    loginStage.setHeight(initialHeight);
                    loginStage.centerOnScreen();
                    
                    // Close splash screen
                    Stage splashStage = (Stage) splashScreen.getScene().getWindow();
                    splashStage.close();
                    
                    // Show login screen
                    loginStage.show();
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeDatabase() throws Exception {
        // Add your database initialization code here
        Thread.sleep(800); // Remove this when you add actual initialization
    }
    
    private void loadConfiguration() throws Exception {
        // Load application settings, properties files, etc.
        Thread.sleep(600); // Remove this when you add actual initialization
    }
    
    private void initializeCache() throws Exception {
        // Initialize any cache systems
        Thread.sleep(400); // Remove this when you add actual initialization
    }
    
    private void prepareUIComponents() throws Exception {
        // Preload commonly used UI components
        Thread.sleep(400); // Remove this when you add actual initialization
    }
    
    private void showErrorDialog(Exception e) {
        // Show error dialog to user
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Initialization Error");
        alert.setHeaderText("Failed to initialize application");
        alert.setContentText("Error: " + e.getMessage());
        alert.showAndWait();
    }
} 
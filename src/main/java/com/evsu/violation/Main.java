package main.java.com.evsu.violation;

import java.io.File;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.image.Image;

public class Main extends Application {
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private Rectangle2D backupWindowBounds;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load the splash screen first
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/SplashScreen.fxml"));
            Parent root = loader.load();

            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            
            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            });

            root.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    toggleMaximize(primaryStage);
                }
            });

            // Create the scene
            Scene scene = new Scene(root);
            
            // Add CSS
            scene.getStylesheets().addAll(
                getClass().getResource("/css/splash-screen.css").toExternalForm(),
                getClass().getResource("/css/style.css").toExternalForm()
            );
            
            // Configure and show the stage
           // Alternative method using File path
            Image icon = new Image(new File("src/main/resources/images/mini-logo.png").toURI().toString());
            primaryStage.getIcons().add(icon);
            primaryStage.setTitle("EVSU-OC SVT");
            primaryStage.initStyle(StageStyle.UNDECORATED); // No window decorations for splash
            primaryStage.setScene(scene);

            
            // Center on screen
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX((screenBounds.getWidth() - 400) / 2);
            primaryStage.setY((screenBounds.getHeight() - 300) / 2);
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.out.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleMaximize(Stage stage) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        if (!isMaximized) {
            // Store current window bounds before maximizing
            backupWindowBounds = new Rectangle2D(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()
            );
            
            // Maximize the window
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            isMaximized = true;
        } else {
            // Restore the window to its previous size
            stage.setX(backupWindowBounds.getMinX());
            stage.setY(backupWindowBounds.getMinY());
            stage.setWidth(backupWindowBounds.getWidth());
            stage.setHeight(backupWindowBounds.getHeight());
            isMaximized = false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 
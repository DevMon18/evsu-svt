package main.java.com.evsu.violation.util;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class LoadingManager {
    public static void showLoading(StackPane container, String message) {
        Platform.runLater(() -> {
            if (container != null) {
                container.setVisible(true);
                container.setManaged(true);
                
                // Update loading message if exists
                container.lookupAll(".loading-text")
                    .stream()
                    .filter(node -> node instanceof Label)
                    .map(node -> (Label) node)
                    .findFirst()
                    .ifPresent(label -> label.setText(message));
            }
        });
    }
    
    public static void hideLoading(StackPane container) {
        Platform.runLater(() -> {
            if (container != null) {
                container.setVisible(false);
                container.setManaged(false);
            }
        });
    }
} 
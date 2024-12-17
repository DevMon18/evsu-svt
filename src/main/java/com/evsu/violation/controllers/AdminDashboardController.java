package main.java.com.evsu.violation.controllers;

import main.java.com.evsu.violation.models.*;
import main.java.com.evsu.violation.util.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.io.IOException;
import javafx.application.Platform;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class AdminDashboardController {
    @FXML private BorderPane mainPane;
    @FXML private VBox contentArea;
    @FXML private VBox dashboardContent;
    @FXML private Label totalViolationsLabel;
    @FXML private Label activeCasesLabel;
    @FXML private Label resolvedCasesLabel;
    @FXML private Label userNameLabel;
    @FXML private Label titleLabel;
    
    @FXML private TableView<Violation> recentViolationsTable;
    @FXML private TableColumn<Violation, LocalDate> dateColumn;
    @FXML private TableColumn<Violation, String> studentColumn;
    @FXML private TableColumn<Violation, String> violationColumn;
    @FXML private TableColumn<Violation, String> statusColumn;

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    
    private Timeline clockTimeline;

    private Connection conn;
    private int currentUserId;

    @FXML
    public void initialize() {
        try {
            conn = DatabaseConnection.getConnection();
            setupDashboard();
            loadDashboardData();
            initializeClock();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }

    private void setupDashboard() {
        if (contentArea == null) {
            throw new RuntimeException("contentArea is not properly initialized from FXML");
        }
        
        // Initialize labels with default values
        if (totalViolationsLabel != null) totalViolationsLabel.setText("0");
        if (activeCasesLabel != null) activeCasesLabel.setText("0");
        if (resolvedCasesLabel != null) resolvedCasesLabel.setText("0");
        
        setupTable();
    }

    @FXML
    public void showStudents() {
        loadView("/fxml/StudentsView.fxml", "Students Dashboard");
    }

    
    @FXML
    public void showCategories() {
        loadView("/fxml/CategoriesView.fxml", "Categories Management");
    }
    @FXML
    public void showViolations() {
        loadView("/fxml/ViolationsView.fxml", "Violations Management");
    }


    @FXML
    public void showReports() {
        loadView("/fxml/ReportsView.fxml", "Reports");
    }

    @FXML
    public void showUserManagement() {
        loadView("/fxml/UserManagementView.fxml", "User Management");
    }

    private void loadView(String fxmlPath, String title) {
        try {
            if (contentArea == null) {
                throw new RuntimeException("contentArea is null. Check FXML file for proper initialization.");
            }

            // Use the controller's class loader to get the resource
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            // Set user context in the controller if it's a ViolationsController
            Object controller = loader.getController();
            if (controller instanceof ViolationsController) {
                ((ViolationsController) controller).setUserContext(currentUserId, false);
            } else if (controller instanceof AddViolationDialogController) {
                ((AddViolationDialogController) controller).setCurrentUser(currentUserId);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            if (titleLabel != null) {
                titleLabel.setText(title);
            }
        } catch (IOException e) {
            showError("Navigation Error", "Failed to load " + title + " view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void setupTable() {
        if (recentViolationsTable != null) {
            dateColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getViolationDate()));
            studentColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStudentName()));
            violationColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));
            statusColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        }
    }

    private void loadDashboardData() throws SQLException {
        updateStatistics();
        loadRecentViolations();
    }

    private void updateStatistics() throws SQLException {
        // Total violations
        String totalSQL = "SELECT COUNT(*) as count FROM VIOLATION";
        try (PreparedStatement stmt = conn.prepareStatement(totalSQL)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && totalViolationsLabel != null) {
                totalViolationsLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }

        // Active cases
        String activeSQL = "SELECT COUNT(*) as count FROM VIOLATION WHERE status = 'Active'";
        try (PreparedStatement stmt = conn.prepareStatement(activeSQL)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && activeCasesLabel != null) {
                activeCasesLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }

        // Resolved cases
        String resolvedSQL = "SELECT COUNT(*) as count FROM VIOLATION WHERE status = 'Resolved'";
        try (PreparedStatement stmt = conn.prepareStatement(resolvedSQL)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && resolvedCasesLabel != null) {
                resolvedCasesLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }
    }

    private void loadRecentViolations() throws SQLException {
        String sql = "SELECT v.*, s.firstName, s.lastName, oc.categoryName " +
                    "FROM VIOLATION v " +
                    "JOIN STUDENT s ON v.studentID = s.studentID " +
                    "JOIN OFFENSE_CATEGORY oc ON v.categoryID = oc.categoryID " +
                    "ORDER BY v.violationDate DESC LIMIT 5";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            ObservableList<Violation> violations = FXCollections.observableArrayList();
            
            while (rs.next()) {
                Violation violation = new Violation(
                    rs.getInt("violationID"),
                    rs.getString("studentID"),
                    rs.getString("firstName") + " " + rs.getString("lastName"),
                    rs.getString("categoryName"),
                    rs.getString("severity"),
                    rs.getString("description"),
                    rs.getString("status")
                );
                violation.setViolationDate(rs.getDate("violationDate").toLocalDate());
                violations.add(violation);
            }
            
            if (recentViolationsTable != null) {
                recentViolationsTable.setItems(violations);
            }
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Close the current connection
            if (conn != null) {
                conn.close();
            }
            
            URL resource = getClass().getClassLoader().getResource("fxml/Login.fxml");
            if (resource == null) {
                throw new IOException("Cannot find Login.fxml");
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException | SQLException e) {
            showError("Error", "Failed to logout: " + e.getMessage());
        }
    }

    public void initData(int userId) {
        this.currentUserId = userId;
        try {
            String sql = "SELECT firstName, lastName FROM USER WHERE userID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && userNameLabel != null) {
                    String fullName = rs.getString("firstName") + " " + rs.getString("lastName");
                    userNameLabel.setText("Welcome, " + fullName);
                }
            }
        } catch (SQLException e) {
            showError("Error", "Failed to load user data: " + e.getMessage());
        }
    }

    @FXML
    private void showDashboard() {
        try {
            // Ensure we have a valid connection
            if (conn == null || conn.isClosed()) {
                conn = DatabaseConnection.getConnection();
            }
            
            if (dashboardContent != null && contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardContent);
                if (titleLabel != null) {
                    titleLabel.setText("Dashboard");
                }
                loadDashboardData();
            }
        } catch (SQLException e) {
            showError("Error", "Failed to refresh dashboard: " + e.getMessage());
        }
    }

    private void initializeClock() {
        // Create formatters for time and date
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        
        // Update clock every second
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            clockLabel.setText(timeFormatter.format(now));
            dateLabel.setText(dateFormatter.format(now));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    public void cleanup() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
} 
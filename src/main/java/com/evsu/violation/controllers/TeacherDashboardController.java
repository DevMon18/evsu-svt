package main.java.com.evsu.violation.controllers;

import main.java.com.evsu.violation.models.*;
import main.java.com.evsu.violation.util.DatabaseConnection;
import main.java.com.evsu.violation.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class TeacherDashboardController {
    @FXML private TableView<Violation> recentViolationsTable;
    @FXML private TableColumn<Violation, LocalDate> dateColumn;
    @FXML private TableColumn<Violation, String> studentIdColumn;
    @FXML private TableColumn<Violation, String> studentNameColumn;
    @FXML private TableColumn<Violation, String> categoryColumn;
    @FXML private TableColumn<Violation, String> severityColumn;
    @FXML private TableColumn<Violation, String> statusColumn;
    @FXML private Label todayViolationsLabel;
    @FXML private Label weekViolationsLabel;
    @FXML private Label pendingCasesLabel;

    private Connection conn;
    private int currentUserId;

    @FXML
    public void initialize() {
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                throw new SQLException("Database connection failed");
            }
            setupTable();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }

    public void initData(int userId) {
        this.currentUserId = userId;
        try {
            loadDashboardData();
        } catch (SQLException e) {
            showError("Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("violationDate"));
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        setupTableContextMenu();
    }

    private void setupTableContextMenu() {
        recentViolationsTable.setRowFactory(tv -> {
            TableRow<Violation> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem editItem = new MenuItem("Edit Violation");
            editItem.setOnAction(event -> {
                Violation violation = row.getItem();
                if (violation != null) {
                    showEditViolationDialog(violation);
                }
            });
            
            MenuItem deleteItem = new MenuItem("Delete Violation");
            deleteItem.setOnAction(event -> {
                Violation violation = row.getItem();
                if (violation != null) {
                    deleteViolation(violation);
                }
            });
            
            contextMenu.getItems().addAll(editItem, deleteItem);
            
            // Only show context menu for non-empty rows
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu)
            );
            
            return row;
        });
    }

    private void loadDashboardData() throws SQLException {
        updateStatistics();
        loadRecentViolations();
    }

    private void updateStatistics() throws SQLException {
        // Today's violations
        String todaySQL = """
            SELECT COUNT(*) as count 
            FROM VIOLATION 
            WHERE DATE(violationDate) = CURDATE() 
            AND userID = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(todaySQL)) {
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                todayViolationsLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }

        // This week's violations
        String weekSQL = """
            SELECT COUNT(*) as count 
            FROM VIOLATION 
            WHERE violationDate >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
            AND userID = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(weekSQL)) {
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                weekViolationsLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }

        // Pending cases
        String pendingSQL = """
            SELECT COUNT(*) as count 
            FROM VIOLATION 
            WHERE status = 'Active'
            AND userID = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(pendingSQL)) {
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                pendingCasesLabel.setText(String.valueOf(rs.getInt("count")));
            }
        }
    }

    private void loadRecentViolations() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT v.*, s.firstName, s.lastName, oc.categoryName, " +
            "DATE_FORMAT(v.violationDate, '%Y-%m-%d %h:%i %p') as formattedDate " +
            "FROM VIOLATION v " +
            "JOIN STUDENT s ON v.studentID = s.studentID " +
            "JOIN OFFENSE_CATEGORY oc ON v.categoryID = oc.categoryID " +
            "WHERE v.userID = ? " +
            "ORDER BY v.violationDate DESC");
        
        stmt.setInt(1, currentUserId);
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
        
        recentViolationsTable.setItems(violations);
        
        // Format the date column to show date and time
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    setText(date.format(formatter));
                }
            }
        });
    }

    @FXML
    private void showViolationDialog() {
        if (currentUserId == 0) {
            AlertHelper.showError("Error", "No user ID set. Please log in again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddViolationDialog.fxml"));
            Parent root = loader.load();
            
            AddViolationDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Log New Violation");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            
            controller.setDialogStage(dialogStage);
            controller.setCurrentUser(currentUserId);
            
            dialogStage.showAndWait();
            
            if (controller.isConfirmed()) {
                try (Connection newConn = DatabaseConnection.getConnection()) {
                    conn = newConn;
                    loadDashboardData();
                    AlertHelper.showToast("Violation record saved successfully", recentViolationsTable);
                } catch (SQLException e) {
                    AlertHelper.showError("Database Error", "Failed to refresh dashboard: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            AlertHelper.showError("Error", "Failed to open violation dialog: " + e.getMessage());
        }
    }

    @FXML
    private void showAddStudentDialog() {
        boolean studentAdded = AddStudentDialogController.showDialog(recentViolationsTable);
        if (studentAdded) {
            try (Connection newConn = DatabaseConnection.getConnection()) {
                conn = newConn;
                loadDashboardData();
            } catch (SQLException e) {
                AlertHelper.showError("Database Error", "Failed to refresh dashboard: " + e.getMessage());
            }
        }
    }

    @FXML
    private void showHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViolationsView.fxml"));
            Parent root = loader.load();
            
            ViolationsController controller = loader.getController();
            if (controller != null) {
                controller.setUserContext(currentUserId, true);
            }
            
            Stage stage = new Stage();
            stage.setTitle("Violation History");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            
            stage.show();
        } catch (IOException e) {
            showError("Error", "Failed to open history view: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) recentViolationsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showEditViolationDialog(Violation violation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViolationDialog.fxml"));
            Parent root = loader.load();
            
            ViolationDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Violation");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            
            controller.setDialogStage(dialogStage);
            controller.setViolation(violation);
            controller.setCurrentUser(currentUserId);
            
            dialogStage.showAndWait();
            
            if (controller.isConfirmed()) {
                try (Connection newConn = DatabaseConnection.getConnection()) {
                    conn = newConn;
                    loadDashboardData();
                    AlertHelper.showToast("Violation updated successfully", recentViolationsTable);
                } catch (SQLException e) {
                    AlertHelper.showError("Database Error", "Failed to refresh dashboard: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            AlertHelper.showError("Error", "Failed to open edit dialog: " + e.getMessage());
        }
    }

    private void deleteViolation(Violation violation) {
        if (AlertHelper.showConfirmation("Delete Violation", 
            "Are you sure you want to delete this violation record?")) {
            try (Connection newConn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM VIOLATION WHERE violationID = ?";
                PreparedStatement stmt = newConn.prepareStatement(sql);
                stmt.setInt(1, violation.getViolationID());
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    conn = newConn;
                    loadDashboardData();
                    AlertHelper.showToast("Violation deleted successfully", recentViolationsTable);
                } else {
                    AlertHelper.showError("Error", "Failed to delete violation record");
                }
            } catch (SQLException e) {
                AlertHelper.showError("Database Error", "Failed to delete violation: " + e.getMessage());
            }
        }
    }
} 
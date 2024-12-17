package main.java.com.evsu.violation.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.evsu.violation.util.AlertHelper;
import main.java.com.evsu.violation.util.DatabaseConnection;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ViolationsController {
    @FXML private TableView<ViolationEntry> violationsTable;
    @FXML private TableColumn<ViolationEntry, String> studentNameColumn;
    @FXML private TableColumn<ViolationEntry, String> categoryColumn;
    @FXML private TableColumn<ViolationEntry, String> severityColumn;
    @FXML private TableColumn<ViolationEntry, String> descriptionColumn;
    @FXML private TableColumn<ViolationEntry, String> statusColumn;
    @FXML private TableColumn<ViolationEntry, LocalDate> dateColumn;
    @FXML private TableColumn<ViolationEntry, String> addedByColumn;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;

    private ObservableList<ViolationEntry> violationsList = FXCollections.observableArrayList();
    private FilteredList<ViolationEntry> filteredViolations;

    private int currentUserId;
    private boolean isTeacherView;

    @FXML
    public void initialize() {
        // Initialize with default values
        this.currentUserId = 0;
        this.isTeacherView = false;
        
        setupTableColumns();
        setupFilters();
        setupSearch();
        applyTableStyling();
        // Don't load violations here - wait for setUserContext
    }

    public void setUserContext(int userId, boolean isTeacher) {
        if (userId <= 0) {
            AlertHelper.showError("Error", "Invalid user ID provided");
            return;
        }
        this.currentUserId = userId;  // Add this line
        this.isTeacherView = isTeacher; 
        loadViolations(); // Now load violations with proper user context
    }

    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("violationDate"));
        addedByColumn.setCellValueFactory(new PropertyValueFactory<>("addedBy"));

        // Add style classes to columns
        studentNameColumn.getStyleClass().add("student-name");
        categoryColumn.getStyleClass().add("category");
        severityColumn.getStyleClass().add("severity");
        descriptionColumn.getStyleClass().add("description");
        statusColumn.getStyleClass().add("status");
        dateColumn.getStyleClass().add("date");
        addedByColumn.getStyleClass().add("added-by");

        // Style severity cells
        severityColumn.setCellFactory(column -> new TableCell<ViolationEntry, String>() {
            private final Label label = new Label();
            
            {
                label.getStyleClass().add("severity-label");
            }
            
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    label.setText(severity.toUpperCase());
                    label.getStyleClass().removeAll("severity-minor", "severity-moderate", "severity-severe");
                    label.getStyleClass().add("severity-" + severity.toLowerCase());
                    setGraphic(label);
                    setText(null);
                }
            }
        });

        // Style status cells
        statusColumn.setCellFactory(column -> new TableCell<ViolationEntry, String>() {
            private final Label label = new Label();
            
            {
                label.getStyleClass().add("status-label");
            }
            
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    label.setText(status.toUpperCase());
                    label.getStyleClass().removeAll("status-pending", "status-in_progress", "status-active", "status-resolved");
                    label.getStyleClass().add("status-" + status.toLowerCase());
                    setGraphic(label);
                    setText(null);
                }
            }
        });

        // Style date cells
        dateColumn.setCellFactory(column -> new TableCell<ViolationEntry, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    getStyleClass().add("date-cell");
                }
            }
        });

        // Style description cells
        descriptionColumn.setCellFactory(column -> new TableCell<ViolationEntry, String>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                if (empty || description == null) {
                    setText(null);
                } else {
                    setText(description);
                    getStyleClass().add("description-cell");
                }
            }
        });
    }

    private void loadViolations() {
        violationsList.clear();
        String query = """
            SELECT 
                v.violationID,
                v.studentID,
                CONCAT(s.firstName, ' ', s.lastName) as studentName,
                oc.categoryName,
                v.severity,
                v.description,
                v.status,
                v.violationDate,
                v.resolution_notes,
                CONCAT(u.firstName, ' ', u.lastName) as resolved_by_name,
                v.resolution_date,
                CONCAT(creator.firstName, ' ', creator.lastName) as added_by_name
            FROM VIOLATION v
            JOIN STUDENT s ON v.studentID = s.studentID
            JOIN OFFENSE_CATEGORY oc ON v.categoryID = oc.categoryID
            JOIN USER creator ON v.userID = creator.userID
            LEFT JOIN USER u ON v.resolved_by = u.userID
            ORDER BY v.violationDate DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ViolationEntry violation = new ViolationEntry(
                    rs.getInt("violationID"),
                    rs.getString("studentID"),
                    rs.getString("studentName"),
                    rs.getString("categoryName"),
                    rs.getString("severity"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getDate("violationDate").toLocalDate(),
                    rs.getString("resolution_notes"),
                    rs.getString("resolved_by_name"),
                    rs.getTimestamp("resolution_date") != null ? 
                        rs.getTimestamp("resolution_date").toLocalDateTime() : null,
                    rs.getString("added_by_name")
                );
                violationsList.add(violation);
            }
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to load violations: " + e.getMessage());
        }

        violationsTable.setItems(violationsList);
    }

    private void setupFilters() {
        // Setup category filter
        ObservableList<String> categories = FXCollections.observableArrayList();
        String categoryQuery = "SELECT DISTINCT categoryName FROM OFFENSE_CATEGORY";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(categoryQuery);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("categoryName"));
            }
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
        categoryFilter.setItems(categories);

        // Setup status filter
        statusFilter.setItems(FXCollections.observableArrayList("PENDING", "ACTIVE", "IN_PROGRESS", "RESOLVED"));

        // Add listeners for filters
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (startDatePicker != null) startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (endDatePicker != null) endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void applyFilters() {
        filteredViolations = new FilteredList<>(violationsList);

        // Apply category filter
        if (categoryFilter.getValue() != null) {
            filteredViolations.setPredicate(violation -> 
                violation.getCategory().equals(categoryFilter.getValue()));
        }

        // Apply status filter
        if (statusFilter.getValue() != null) {
            filteredViolations.setPredicate(violation -> 
                violation.getStatus().equals(statusFilter.getValue()));
        }

        // Apply date range filter
        if (startDatePicker != null && startDatePicker.getValue() != null) {
            filteredViolations.setPredicate(violation -> 
                !violation.getViolationDate().isBefore(startDatePicker.getValue()));
        }
        if (endDatePicker != null && endDatePicker.getValue() != null) {
            filteredViolations.setPredicate(violation -> 
                !violation.getViolationDate().isAfter(endDatePicker.getValue()));
        }

        // Apply search filter
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            String searchText = searchField.getText().toLowerCase();
            filteredViolations.setPredicate(violation -> 
                violation.getStudentName().toLowerCase().contains(searchText));
        }

        violationsTable.setItems(filteredViolations);
    }

    @FXML
    private void handleAddViolation() {
        if (currentUserId == 0) {
            AlertHelper.showError("Error", "No user ID set. Please log in again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddViolationDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Violation");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(root));
            
            AddViolationDialogController controller = loader.getController();
            // Set the dialog stage first
            controller.setDialogStage(dialogStage);
            // Then set the current user ID, which will trigger data initialization
            controller.setCurrentUser(currentUserId);
            
            dialogStage.showAndWait();
            
            if (controller.isConfirmed()) {
                loadViolations();  // Refresh the table
                AlertHelper.showToast("Violation added successfully", violationsTable);
            }
        } catch (IOException e) {
            AlertHelper.showError("Error", "Failed to open add violation dialog: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditViolation() {
        if (currentUserId == 0) {
            AlertHelper.showError("Error", "No user ID set. Please log in again.");
            return;
        }

        ViolationEntry selectedViolation = violationsTable.getSelectionModel().getSelectedItem();
        if (selectedViolation == null) {
            AlertHelper.showError("No Selection", "Please select a violation to edit.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddViolationDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Violation");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(root));
            
            AddViolationDialogController controller = loader.getController();
            // Set the dialog stage first
            controller.setDialogStage(dialogStage);
            // Then set the current user ID, which will trigger data initialization
            controller.setCurrentUser(currentUserId);
            // Finally, set the violation to edit
            controller.setViolation(selectedViolation);
            
            dialogStage.showAndWait();
            
            if (controller.isConfirmed()) {
                loadViolations();  // Refresh the table
                AlertHelper.showToast("Violation updated successfully", violationsTable);
            }
        } catch (IOException e) {
            AlertHelper.showError("Error", "Failed to open edit dialog: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteViolation() {
        if (currentUserId == 0) {
            AlertHelper.showError("Error", "No user ID set. Please log in again.");
            return;
        }

        ViolationEntry selectedViolation = violationsTable.getSelectionModel().getSelectedItem();
        if (selectedViolation == null) {
            AlertHelper.showError("No Selection", "Please select a violation to delete.");
            return;
        }

        if (AlertHelper.showConfirmation("Delete Violation", 
            "Are you sure you want to delete this violation record?")) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM VIOLATION WHERE violationID = ? AND userID = ?")) {
                
                stmt.setInt(1, selectedViolation.getViolationID());
                stmt.setInt(2, currentUserId);  // Only allow deletion of own violations
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    loadViolations();
                    AlertHelper.showToast("Violation deleted successfully", violationsTable);
                } else {
                    AlertHelper.showError("Error", "You can only delete violations that you created.");
                }
            } catch (SQLException e) {
                AlertHelper.showError("Database Error", 
                    "Failed to delete violation: " + e.getMessage());
            }
        }
    }

    private void applyTableStyling() {
        violationsTable.getStyleClass().add("violations-table");
        violationsTable.setRowFactory(tv -> new TableRow<ViolationEntry>() {
            @Override
            protected void updateItem(ViolationEntry violation, boolean empty) {
                super.updateItem(violation, empty);
                if (empty || violation == null) {
                    setStyle("");
                } else {
                    getStyleClass().remove("severe-violation");
                    getStyleClass().remove("moderate-violation");
                    getStyleClass().remove("minor-violation");
                    
                    switch (violation.getSeverity().toLowerCase()) {
                        case "severe":
                            getStyleClass().add("severe-violation");
                            break;
                        case "moderate":
                            getStyleClass().add("moderate-violation");
                            break;
                        case "minor":
                            getStyleClass().add("minor-violation");
                            break;
                    }
                }
            }
        });
    }

    @FXML
    private void resetFilters() {
        categoryFilter.setValue(null);
        statusFilter.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        searchField.clear();
        violationsTable.setItems(violationsList);
    }

    // ViolationEntry inner class
    public static class ViolationEntry {
        private final int violationID;
        private final String studentID;
        private final String studentName;
        private final String category;
        private final String severity;
        private final String description;
        private final String status;
        private final LocalDate violationDate;
        private final String resolutionNotes;
        private final String resolvedByName;
        private final LocalDateTime resolutionDate;
        private final String addedBy;

        public ViolationEntry(int violationID, String studentID, String studentName, 
                            String category, String severity, String description, 
                            String status, LocalDate violationDate,
                            String resolutionNotes, String resolvedByName,
                            LocalDateTime resolutionDate, String addedBy) {
            this.violationID = violationID;
            this.studentID = studentID;
            this.studentName = studentName;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.status = status;
            this.violationDate = violationDate;
            this.resolutionNotes = resolutionNotes;
            this.resolvedByName = resolvedByName;
            this.resolutionDate = resolutionDate;
            this.addedBy = addedBy;
        }

        // Getters
        public int getViolationID() { return violationID; }
        public String getStudentID() { return studentID; }
        public String getStudentName() { return studentName; }
        public String getCategory() { return category; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public LocalDate getViolationDate() { return violationDate; }
        public String getResolutionNotes() { return resolutionNotes; }
        public String getResolvedByName() { return resolvedByName; }
        public LocalDateTime getResolutionDate() { return resolutionDate; }
        public String getAddedBy() { return addedBy; }
    }
} 
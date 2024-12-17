package main.java.com.evsu.violation.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import main.java.com.evsu.violation.models.Student;
import main.java.com.evsu.violation.util.DatabaseConnection;
import main.java.com.evsu.violation.util.AlertHelper;
import main.java.com.evsu.violation.util.LoadingManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentsController {
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> idNumberColumn;
    @FXML private TableColumn<Student, String> firstNameColumn;
    @FXML private TableColumn<Student, String> lastNameColumn;
    @FXML private TableColumn<Student, String> courseColumn;
    @FXML private TableColumn<Student, Integer> yearLevelColumn;
    @FXML private TableColumn<Student, String> contactNumberColumn;
    @FXML private TableColumn<Student, String> parentEmailColumn;
    @FXML private TableColumn<Student, Void> actionsColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<Integer> yearFilter;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label withViolationsLabel;
    @FXML private Label statusLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private StackPane loadingOverlay;

    private ObservableList<Student> masterData = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        
        // Load initial data with loading indicator
        LoadingManager.showLoading(loadingOverlay, "Loading students...");
        new Thread(() -> {
            try {
                loadStudents();
                updateStatistics();
                Platform.runLater(() -> {
                    statusLabel.setText("Ready");
                    LoadingManager.hideLoading(loadingOverlay);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertHelper.showError("Error", "Failed to load students: " + e.getMessage());
                    statusLabel.setText("Error loading data");
                    LoadingManager.hideLoading(loadingOverlay);
                });
            }
        }).start();
        
        // Setup keyboard shortcuts after the scene is available
        Platform.runLater(this::setupKeyboardShortcuts);
    }

    private void setupKeyboardShortcuts() {
        if (studentTable.getScene() != null) {
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    searchField.clear();
                    courseFilter.setValue("All Courses");
                    yearFilter.setValue(1);
                }
            });
        }
    }

    private void setupTable() {
        idNumberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStudentID()));
        firstNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFirstName()));
        lastNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLastName()));
        courseColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCourse()));
        yearLevelColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getYearLevel()));
        contactNumberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getContactNumber()));
        parentEmailColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getParentEmail()));
        
        setupActionColumn();
        studentTable.getSortOrder().add(lastNameColumn);
        studentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupActionColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");
                editBtn.setOnAction(e -> handleEditStudent(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteStudent(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupFilters() {
        List<String> courses = loadCourses();
        courseFilter.setItems(FXCollections.observableArrayList("All Courses"));
        courseFilter.getItems().addAll(courses);
        courseFilter.setValue("All Courses");
        
        yearFilter.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        yearFilter.setValue(1);
        
        // Setup filter listeners
        courseFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        yearFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Initialize filtered data
        filteredData = new FilteredList<>(masterData);
        studentTable.setItems(filteredData);
    }

    private void applyFilters() {
        if (filteredData == null) return;
        
        filteredData.setPredicate(student -> {
            boolean matchesSearch = searchField.getText() == null || searchField.getText().isEmpty() ||
                student.getStudentID().toLowerCase().contains(searchField.getText().toLowerCase()) ||
                student.getFirstName().toLowerCase().contains(searchField.getText().toLowerCase()) ||
                student.getLastName().toLowerCase().contains(searchField.getText().toLowerCase());
                
            boolean matchesCourse = courseFilter.getValue() == null || 
                courseFilter.getValue().equals("All Courses") ||
                student.getCourse().equals(courseFilter.getValue());
                
            boolean matchesYear = yearFilter.getValue() == null || 
                yearFilter.getValue() == 1 ||
                student.getYearLevel() == yearFilter.getValue();
                
            return matchesSearch && matchesCourse && matchesYear;
        });
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        courseFilter.setValue("All Courses");
        yearFilter.setValue(1);
    }

    @FXML
    private void handleAddStudent() {
        boolean studentAdded = AddStudentDialogController.showDialog(studentTable);
        if (studentAdded) {
            loadStudents(); // Refresh the table after successful addition
            updateStatistics();
            updateLastModified();
        }
    }

    private void handleEditStudent(Student student) {
        if (student == null) return;
        
        try {
            Dialog<Student> dialog = new Dialog<>();
            dialog.setTitle("Edit Student");
            
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/EditStudentDialog.fxml"));
            DialogPane dialogPane = loader.load();
            dialog.setDialogPane(dialogPane);
            
            EditStudentDialogController controller = loader.getController();
            controller.setStudent(student);
            
            // The button types are already defined in the FXML
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return controller.getStudent();
                }
                return null;
            });
            
            Optional<Student> result = dialog.showAndWait();
            result.ifPresent(updatedStudent -> {
                if (updateStudent(updatedStudent)) {
                    int index = masterData.indexOf(student);
                    masterData.set(index, updatedStudent);
                    updateStatistics();
                    updateLastModified();
                    AlertHelper.showToast("Student updated successfully", studentTable);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Could not edit student: " + e.getMessage());
        }
    }

    private void handleDeleteStudent(Student student) {
        if (student == null) return;
        
        if (AlertHelper.showConfirmation("Delete Student", 
            "Are you sure you want to delete " + student.getFullName() + "?")) {
            if (deleteStudent(student)) {
                masterData.remove(student);
                updateStatistics();
                updateLastModified();
                AlertHelper.showToast("Student deleted successfully", studentTable);
            }
        }
    }

    private void loadStudents() {
        String sql = "SELECT * FROM STUDENT ORDER BY lastName, firstName";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            masterData.clear();
            
            while (rs.next()) {
                Student student = new Student(
                    rs.getString("studentID"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("course"),
                    rs.getInt("yearLevel"),
                    rs.getString("contactNumber"),
                    rs.getString("parentEmail")
                );
                masterData.add(student);
            }
            
            filteredData = new FilteredList<>(masterData);
            studentTable.setItems(filteredData);
            updateStatistics();
            updateLastModified();
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Error loading students: " + e.getMessage());
        }
    }

    private List<String> loadCourses() {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return courses;
    }



    private boolean updateStudent(Student student) {
        String sql = "UPDATE STUDENT SET firstName=?, lastName=?, course=?, yearLevel=?, " +
                    "contactNumber=?, parentEmail=? WHERE studentID=?";
                    
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, student.getFirstName());
            stmt.setString(2, student.getLastName());
            stmt.setString(3, student.getCourse());
            stmt.setInt(4, student.getYearLevel());
            stmt.setString(5, student.getContactNumber());
            stmt.setString(6, student.getParentEmail());
            stmt.setString(7, student.getStudentID());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Error updating student: " + e.getMessage());
            return false;
        }
    }

    private boolean deleteStudent(Student student) {
        String sql = "DELETE FROM STUDENT WHERE studentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, student.getStudentID());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Error deleting student: " + e.getMessage());
            return false;
        }
    }

    private void updateStatistics() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total students
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM STUDENT")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalStudentsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // Active students (students with ongoing violations)
            String activeQuery = """
                SELECT COUNT(DISTINCT s.studentID) 
                FROM STUDENT s 
                JOIN VIOLATION v ON s.studentID = v.studentID 
                WHERE v.status IN ('ACTIVE', 'IN_PROGRESS')
            """;
            try (PreparedStatement stmt = conn.prepareStatement(activeQuery)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    activeStudentsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // Students with any violations (resolved or not)
            String violationsQuery = """
                SELECT COUNT(DISTINCT studentID) 
                FROM VIOLATION
            """;
            try (PreparedStatement stmt = conn.prepareStatement(violationsQuery)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    withViolationsLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Failed to update statistics: " + e.getMessage());
        }
    }

    private void updateLastModified() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        lastUpdatedLabel.setText("Last updated: " + now.format(formatter));
    }
}


package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.evsu.violation.models.User;
import main.java.com.evsu.violation.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class UserManagementController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    
    private Connection conn;
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        try {
            conn = DatabaseConnection.getConnection();
            setupTable();
            setupFilters();
            loadUsers();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTable() {
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        // Add context menu for actions
        userTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem editItem = new MenuItem("Edit User");
            editItem.setOnAction(e -> handleEditUser(row.getItem()));
            
            MenuItem deleteItem = new MenuItem("Delete User");
            deleteItem.setOnAction(e -> handleDeleteUser(row.getItem()));
            
            MenuItem toggleStatusItem = new MenuItem("Toggle Status");
            toggleStatusItem.setOnAction(e -> handleToggleStatus(row.getItem()));
            
            contextMenu.getItems().addAll(editItem, deleteItem, toggleStatusItem);
            
            // Set context menu only if row is not empty
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu)
            );
            
            return row;
        });
        
        userTable.setItems(userList);
    }

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList(
            "All", "ADMIN", "TEACHER"
        ));
        
        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "ACTIVE", "INACTIVE"
        ));
        
        roleFilter.setValue("All");
        statusFilter.setValue("All");
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
        roleFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterUsers());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    }

    @FXML
    private void showAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDialog.fxml"));
            VBox dialogContent = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add New User");
            dialog.setHeaderText(null);
            
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(dialogContent);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.getStylesheets().add(getClass().getResource("/css/userDialog.css").toExternalForm());
            
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.initModality(Modality.APPLICATION_MODAL);
            
            UserDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                User newUser = controller.getUser();
                if (newUser != null && controller.isSaveClicked()) {
                    saveNewUser(newUser);
                    showSuccess("Success", "User added successfully!");
                    loadUsers();
                }
            }
        } catch (IOException e) {
            showError("Error", "Could not load the dialog: " + e.getMessage());
        }
    }

    private void saveNewUser(User user) {
        String sql = "INSERT INTO USER (username, password, firstName, lastName, role, email, status) VALUES (?, SHA2(?, 256), ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String[] nameParts = user.getFullName().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getStatus());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Database Error", "Failed to save user: " + e.getMessage());
        }
    }

    private void loadUsers() {
        userList.clear();
        String sql = "SELECT userID, username, firstName, lastName, email, role, status, lastLogin, createdAt, updatedAt FROM USER ORDER BY username";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User(
                    rs.getInt("userID"),
                    rs.getString("username"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getString("status")
                );
                user.setLastLogin(rs.getTimestamp("lastLogin") != null ? 
                    rs.getTimestamp("lastLogin").toLocalDateTime() : null);
                user.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
                user.setUpdatedAt(rs.getTimestamp("updatedAt").toLocalDateTime());
                userList.add(user);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void filterUsers() {
        String searchText = searchField.getText().toLowerCase();
        String selectedRole = roleFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        
        ObservableList<User> filteredList = userList.filtered(user ->
            (searchText.isEmpty() || 
             user.getUsername().toLowerCase().contains(searchText) ||
             user.getFullName().toLowerCase().contains(searchText) ||
             user.getEmail().toLowerCase().contains(searchText)) &&
            (selectedRole.equals("All") || user.getRole().equals(selectedRole)) &&
            (selectedStatus.equals("All") || user.getStatus().equals(selectedStatus))
        );
        
        userTable.setItems(filteredList);
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        roleFilter.setValue("All");
        statusFilter.setValue("All");
        userTable.setItems(userList);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleEditUser(User user) {
        if (user == null) {
            showError("Selection Error", "Please select a user to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDialog.fxml"));
            VBox dialogContent = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit User");
            dialog.setHeaderText(null);
            
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(dialogContent);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.getStylesheets().add(getClass().getResource("/css/userDialog.css").toExternalForm());
            
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.initModality(Modality.APPLICATION_MODAL);
            
            UserDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setUser(user);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                User editedUser = controller.getUser();
                if (editedUser != null && controller.isSaveClicked()) {
                    updateUser(editedUser);
                    showSuccess("Success", "User updated successfully!");
                    loadUsers();
                }
            }
        } catch (IOException e) {
            showError("Error", "Could not load the dialog: " + e.getMessage());
        }
    }

    private void handleDeleteUser(User user) {
        if (user == null) {
            showError("Selection Error", "Please select a user to delete");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete User Account");
        confirmDialog.setContentText("Are you sure you want to delete the user: " + user.getUsername() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUser(user);
        }
    }

    private void handleToggleStatus(User user) {
        if (user == null) {
            showError("Selection Error", "Please select a user to update status");
            return;
        }

        String newStatus = user.getStatus().equals("ACTIVE") ? "INACTIVE" : "ACTIVE";
        String message = String.format("Are you sure you want to change %s's status to %s?", 
            user.getUsername(), newStatus);

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Status Change");
        confirmDialog.setHeaderText("Update User Status");
        confirmDialog.setContentText(message);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateUserStatus(user, newStatus);
        }
    }

    private void updateUser(User user) {
        String sql = "UPDATE USER SET username = ?, firstName = ?, lastName = ?, email = ?, role = ?, status = ? WHERE userID = ?";
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            sql = "UPDATE USER SET username = ?, firstName = ?, lastName = ?, email = ?, role = ?, status = ?, password = SHA2(?, 256) WHERE userID = ?";
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String[] nameParts = user.getFullName().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus());
            
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                stmt.setString(7, user.getPassword());
                stmt.setInt(8, user.getId());
            } else {
                stmt.setInt(7, user.getId());
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Database Error", "Failed to update user: " + e.getMessage());
        }
    }

    private void deleteUser(User user) {
        // First check if user has any violations
        String checkSQL = "SELECT COUNT(*) as count FROM VIOLATION WHERE userID = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
            checkStmt.setInt(1, user.getId());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                showError("Cannot Delete", "This user has existing violations. Please reassign or delete the violations first.");
                return;
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to check user violations: " + e.getMessage());
            return;
        }

        String sql = "DELETE FROM USER WHERE userID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.executeUpdate();
            showSuccess("Success", "User deleted successfully!");
            loadUsers();
        } catch (SQLException e) {
            showError("Database Error", "Failed to delete user: " + e.getMessage());
        }
    }

    private void updateUserStatus(User user, String newStatus) {
        String sql = "UPDATE USER SET status = ? WHERE userID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, user.getId());
            stmt.executeUpdate();
            showSuccess("Success", "User status updated successfully!");
            loadUsers();
        } catch (SQLException e) {
            showError("Database Error", "Failed to update user status: " + e.getMessage());
        }
    }
} 
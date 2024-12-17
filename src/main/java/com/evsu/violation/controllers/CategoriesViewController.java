package main.java.com.evsu.violation.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.java.com.evsu.violation.models.Category;
import main.java.com.evsu.violation.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class CategoriesViewController {
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    @FXML private TableColumn<Category, String> severityColumn;
    @FXML private TableColumn<Category, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> severityFilter;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label activeCategoriesLabel;
    @FXML private Label lastUpdatedLabel;
    
    private Connection conn;
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        try {
            conn = DatabaseConnection.getConnection();
            setupTable();
            setupFilters();
            loadCategories();
            updateStatistics();
        } catch (SQLException e) {
            showError("Database Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        severityColumn.setCellValueFactory(cellData -> cellData.getValue().severityLevelProperty());
        
        setupActionsColumn();
        categoriesTable.setItems(categoryList);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            {
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");
                
                editBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });
                
                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(8);
                    buttons.getStyleClass().add("action-buttons-container");
                    buttons.getChildren().addAll(editBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupFilters() {
        severityFilter.setItems(FXCollections.observableArrayList(
            "All", "Minor", "Moderate", "Major", "Severe"
        ));
        severityFilter.setValue("All");
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterCategories());
        severityFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterCategories());
    }

    @FXML
    private void handleAddCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryDialog.fxml"));
            VBox dialogContent = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add New Category");
            dialog.setHeaderText(null);
            
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(dialogContent);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.getStylesheets().add(getClass().getResource("/css/categoryDialog.css").toExternalForm());
            
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.initModality(Modality.APPLICATION_MODAL);
            
            CategoryDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Category newCategory = controller.getCategory();
                if (newCategory != null && controller.isSaveClicked()) {
                    saveNewCategory(newCategory);
                    showSuccess("Success", "Category added successfully!");
                    loadCategories();
                    updateStatistics();
                }
            }
        } catch (IOException e) {
            showError("Error", "Could not load the dialog: " + e.getMessage());
        }
    }

    private void handleEditCategory(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryDialog.fxml"));
            VBox dialogContent = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Category");
            dialog.setHeaderText(null);
            
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(dialogContent);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.getStylesheets().add(getClass().getResource("/css/categoryDialog.css").toExternalForm());
            
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.initModality(Modality.APPLICATION_MODAL);
            
            CategoryDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setCategory(category);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK && controller.isSaveClicked()) {
                updateCategory(category);
                showSuccess("Success", "Category updated successfully!");
                loadCategories();
                updateStatistics();
            }
        } catch (IOException e) {
            showError("Error", "Could not load the dialog: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(Category category) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Category");
        confirmDialog.setContentText("Are you sure you want to delete the category: " + category.getName() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteCategory(category);
        }
    }

    private void saveNewCategory(Category category) {
        String sql = "INSERT INTO OFFENSE_CATEGORY (categoryName, description, defaultSeverity) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getSeverityLevel());
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Database Error", "Failed to save category: " + e.getMessage());
        }
    }

    private void updateCategory(Category category) {
        String sql = "UPDATE OFFENSE_CATEGORY SET categoryName = ?, description = ?, defaultSeverity = ? WHERE categoryID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getSeverityLevel());
            stmt.setInt(4, category.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Database Error", "Failed to update category: " + e.getMessage());
        }
    }

    private void deleteCategory(Category category) {
        String sql = "DELETE FROM OFFENSE_CATEGORY WHERE categoryID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, category.getId());
            stmt.executeUpdate();
            showSuccess("Success", "Category deleted successfully!");
            loadCategories();
            updateStatistics();
        } catch (SQLException e) {
            showError("Database Error", "Failed to delete category: " + e.getMessage());
        }
    }

    private void loadCategories() {
        categoryList.clear();
        String sql = "SELECT * FROM OFFENSE_CATEGORY ORDER BY categoryName";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categoryList.add(new Category(
                    rs.getInt("categoryID"),
                    rs.getString("categoryName"),
                    rs.getString("description"),
                    rs.getString("defaultSeverity")
                ));
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void filterCategories() {
        String searchText = searchField.getText().toLowerCase();
        String selectedSeverity = severityFilter.getValue();
        
        ObservableList<Category> filteredList = categoryList.filtered(category ->
            (searchText.isEmpty() || 
             category.getName().toLowerCase().contains(searchText) ||
             category.getDescription().toLowerCase().contains(searchText)) &&
            (selectedSeverity.equals("All") || 
             category.getSeverityLevel().equals(selectedSeverity))
        );
        
        categoriesTable.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        severityFilter.setValue("All");
        categoriesTable.setItems(categoryList);
        updateStatistics();
    }

    private void updateStatistics() {
        totalCategoriesLabel.setText(String.valueOf(categoryList.size()));
        activeCategoriesLabel.setText(String.valueOf(categoriesTable.getItems().size()));
        lastUpdatedLabel.setText(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        ));
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
} 
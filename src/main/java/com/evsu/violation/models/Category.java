package main.java.com.evsu.violation.models;

import javafx.beans.property.*;

public class Category {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty description;
    private final StringProperty severityLevel;

    public Category() {
        this(0, "", "", "");
    }

    public Category(int id, String name, String description, String severityLevel) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.severityLevel = new SimpleStringProperty(severityLevel);
    }

    // ID Property
    public IntegerProperty idProperty() {
        return id;
    }
    
    public int getId() {
        return id.get();
    }
    
    public void setId(int id) {
        this.id.set(id);
    }

    // Name Property
    public StringProperty nameProperty() {
        return name;
    }
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }

    // Description Property
    public StringProperty descriptionProperty() {
        return description;
    }
    
    public String getDescription() {
        return description.get();
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }

    // Severity Level Property
    public StringProperty severityLevelProperty() {
        return severityLevel;
    }
    
    public String getSeverityLevel() {
        return severityLevel.get();
    }
    
    public void setSeverityLevel(String severityLevel) {
        this.severityLevel.set(severityLevel);
    }

    @Override
    public String toString() {
        return getName();
    }
} 
package main.java.com.evsu.violation.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class User {
    private final IntegerProperty id;
    private final StringProperty username;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty email;
    private final StringProperty role;
    private final StringProperty status;
    private final ObjectProperty<LocalDateTime> lastLogin;
    private final ObjectProperty<LocalDateTime> createdAt;
    private final ObjectProperty<LocalDateTime> updatedAt;
    private String password; // Not using property for security reasons

    public User() {
        this(0, "", "", "", "", "", "ACTIVE");
    }

    public User(int id, String username, String firstName, String lastName, String email, String role, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
        this.status = new SimpleStringProperty(status);
        this.lastLogin = new SimpleObjectProperty<>();
        this.createdAt = new SimpleObjectProperty<>();
        this.updatedAt = new SimpleObjectProperty<>();
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

    // Username Property
    public StringProperty usernameProperty() {
        return username;
    }
    
    public String getUsername() {
        return username.get();
    }
    
    public void setUsername(String username) {
        this.username.set(username);
    }

    // First Name Property
    public StringProperty firstNameProperty() {
        return firstName;
    }
    
    public String getFirstName() {
        return firstName.get();
    }
    
    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    // Last Name Property
    public StringProperty lastNameProperty() {
        return lastName;
    }
    
    public String getLastName() {
        return lastName.get();
    }
    
    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    // Full Name (computed from firstName and lastName)
    public String getFullName() {
        String first = getFirstName() != null ? getFirstName().trim() : "";
        String last = getLastName() != null ? getLastName().trim() : "";
        return (first + " " + last).trim();
    }
    
    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            setFirstName("");
            setLastName("");
            return;
        }
        
        String[] parts = fullName.trim().split("\\s+", 2);
        setFirstName(parts[0]);
        setLastName(parts.length > 1 ? parts[1] : "");
    }

    // Full Name Property (for TableView binding)
    public StringProperty fullNameProperty() {
        return new SimpleStringProperty(getFullName());
    }

    // Email Property
    public StringProperty emailProperty() {
        return email;
    }
    
    public String getEmail() {
        return email.get();
    }
    
    public void setEmail(String email) {
        this.email.set(email);
    }

    // Role Property
    public StringProperty roleProperty() {
        return role;
    }
    
    public String getRole() {
        return role.get();
    }
    
    public void setRole(String role) {
        this.role.set(role);
    }

    // Status Property
    public StringProperty statusProperty() {
        return status;
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }

    // Password (not a property)
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    // DateTime Properties
    public ObjectProperty<LocalDateTime> lastLoginProperty() {
        return lastLogin;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin.get();
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin.set(lastLogin);
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }

    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt.set(updatedAt);
    }

    @Override
    public String toString() {
        return getUsername();
    }
} 
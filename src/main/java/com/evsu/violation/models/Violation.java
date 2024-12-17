package main.java.com.evsu.violation.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Violation {
    private int violationID;
    private String studentID;
    private int userID;
    private int categoryID;
    private String studentName;
    private String category;
    private LocalDate violationDate;
    private String description;
    private String severity;
    private String status;
    private String resolutionNotes;
    private Integer resolvedBy;
    private LocalDateTime resolutionDate;

    public Violation() {}

    public Violation(int violationID, String studentID, String studentName, String category, 
                    String severity, String description, String status) {
        this.violationID = violationID;
        this.studentID = studentID;
        this.studentName = studentName;
        this.category = category;
        this.severity = severity;
        this.description = description;
        this.status = status;
    }

    // Getters
    public int getViolationID() { return violationID; }
    public String getStudentID() { return studentID; }
    public int getUserID() { return userID; }
    public int getCategoryID() { return categoryID; }
    public String getStudentName() { return studentName; }
    public String getCategory() { return category; }
    public LocalDate getViolationDate() { return violationDate; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getResolutionNotes() { return resolutionNotes; }
    public Integer getResolvedBy() { return resolvedBy; }
    public LocalDateTime getResolutionDate() { return resolutionDate; }

    // Setters
    public void setViolationID(int violationID) { this.violationID = violationID; }
    public void setStudentID(String studentID) { this.studentID = studentID; }
    public void setUserID(int userID) { this.userID = userID; }
    public void setCategoryID(int categoryID) { this.categoryID = categoryID; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setCategory(String category) { this.category = category; }
    public void setViolationDate(LocalDate violationDate) { this.violationDate = violationDate; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setStatus(String status) { this.status = status; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public void setResolvedBy(Integer resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setResolutionDate(LocalDateTime resolutionDate) { this.resolutionDate = resolutionDate; }

    // Helper methods
    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }
} 
package main.java.com.evsu.violation.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ViolationReport {
    private int id;
    private String studentId;
    private String studentName;
    private String course;
    private String yearLevel;
    private String category;
    private String severity;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolution;
    private String handledBy;

    public ViolationReport() {}

    public ViolationReport(int id, String studentId, String studentName, String course, String yearLevel,
                          String category, String severity, String status, String description) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.description = description;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    // Helper methods
    public LocalDate getDate() {
        return createdAt != null ? createdAt.toLocalDate() : null;
    }

    public boolean isResolved() {
        return "Resolved".equalsIgnoreCase(status);
    }

    public long getDaysToResolve() {
        if (!isResolved() || createdAt == null || resolvedAt == null) {
            return -1;
        }
        return java.time.Duration.between(createdAt, resolvedAt).toDays();
    }
} 
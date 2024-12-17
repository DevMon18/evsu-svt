package main.java.com.evsu.violation.models;

import javafx.beans.property.*;
import main.java.com.evsu.violation.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;

public class Report {
    private final IntegerProperty totalViolations;
    private final IntegerProperty totalStudents;
    private final StringProperty mostCommonCategory;
    private final StringProperty mostCommonSeverity;
    private final ObjectProperty<LocalDate> startDate;
    private final ObjectProperty<LocalDate> endDate;

    public Report() {
        this.totalViolations = new SimpleIntegerProperty(0);
        this.totalStudents = new SimpleIntegerProperty(0);
        this.mostCommonCategory = new SimpleStringProperty("");
        this.mostCommonSeverity = new SimpleStringProperty("");
        this.startDate = new SimpleObjectProperty<>();
        this.endDate = new SimpleObjectProperty<>();
    }

    // Total Violations
    public int getTotalViolations() {
        return totalViolations.get();
    }

    public void setTotalViolations(int value) {
        totalViolations.set(value);
    }

    public IntegerProperty totalViolationsProperty() {
        return totalViolations;
    }

    // Total Students
    public int getTotalStudents() {
        return totalStudents.get();
    }

    public void setTotalStudents(int value) {
        totalStudents.set(value);
    }

    public IntegerProperty totalStudentsProperty() {
        return totalStudents;
    }

    // Most Common Category
    public String getMostCommonCategory() {
        return mostCommonCategory.get();
    }

    public void setMostCommonCategory(String value) {
        mostCommonCategory.set(value != null ? value : "");
    }

    public StringProperty mostCommonCategoryProperty() {
        return mostCommonCategory;
    }

    // Most Common Severity
    public String getMostCommonSeverity() {
        return mostCommonSeverity.get();
    }

    public void setMostCommonSeverity(String value) {
        mostCommonSeverity.set(value != null ? value : "");
    }

    public StringProperty mostCommonSeverityProperty() {
        return mostCommonSeverity;
    }

    // Start Date
    public LocalDate getStartDate() {
        return startDate.get();
    }

    public void setStartDate(LocalDate value) {
        startDate.set(value);
    }

    public ObjectProperty<LocalDate> startDateProperty() {
        return startDate;
    }

    // End Date
    public LocalDate getEndDate() {
        return endDate.get();
    }

    public void setEndDate(LocalDate value) {
        endDate.set(value);
    }

    public ObjectProperty<LocalDate> endDateProperty() {
        return endDate;
    }

    // Database Operations
    public static Report generateReport(LocalDate startDate, LocalDate endDate) {
        Report report = new Report();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            // Get total violations
            String violationsQuery = "SELECT COUNT(*) as total FROM VIOLATION WHERE violationDate BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(violationsQuery)) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setTotalViolations(rs.getInt("total"));
                }
            }

            // Get total students with violations
            String studentsQuery = "SELECT COUNT(DISTINCT studentID) as total FROM VIOLATION WHERE violationDate BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(studentsQuery)) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setTotalStudents(rs.getInt("total"));
                }
            }

            // Get most common category
            String categoryQuery = """
                SELECT oc.categoryName, COUNT(*) as count 
                FROM VIOLATION v 
                JOIN OFFENSE_CATEGORY oc ON v.categoryID = oc.categoryID 
                WHERE v.violationDate BETWEEN ? AND ? 
                GROUP BY oc.categoryName 
                ORDER BY count DESC 
                LIMIT 1
            """;
            try (PreparedStatement stmt = conn.prepareStatement(categoryQuery)) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setMostCommonCategory(rs.getString("categoryName"));
                }
            }

            // Get most common severity
            String severityQuery = """
                SELECT severity, COUNT(*) as count 
                FROM VIOLATION 
                WHERE violationDate BETWEEN ? AND ? 
                GROUP BY severity 
                ORDER BY count DESC 
                LIMIT 1
            """;
            try (PreparedStatement stmt = conn.prepareStatement(severityQuery)) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setMostCommonSeverity(rs.getString("severity"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return report;
    }
} 
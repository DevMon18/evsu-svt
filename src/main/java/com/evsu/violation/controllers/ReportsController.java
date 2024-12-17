package main.java.com.evsu.violation.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.ColumnText;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import main.java.com.evsu.violation.models.ViolationReport;
import main.java.com.evsu.violation.util.DatabaseConnection;

public class ReportsController implements Initializable {
    @FXML private BarChart<String, Number> trendsBarChart;
    @FXML private BarChart<String, Number> courseBarChart;
    @FXML private StackedBarChart<String, Number> yearLevelChart;
    @FXML private PieChart violationsPieChart;
    @FXML private LineChart<String, Number> comparisonChart;
    @FXML private StackedBarChart<String, Number> resolutionTimeChart;
    @FXML private Canvas radarChartCanvas;
    
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> yearLevelFilter;
    
    @FXML private Label totalViolationsLabel;
    @FXML private Label activeViolationsLabel;
    @FXML private Label resolvedViolationsLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label resolutionRateLabel;
    
    private final ObservableList<ViolationReport> reports = FXCollections.observableArrayList();
    private GraphicsContext radarGc;
    private boolean isDetailedView;
    private LocalDate previousPeriodStart;
    private LocalDate previousPeriodEnd;
    private int currentUserId;
    private boolean isTeacher;
    
    public void setUserContext(int userId, boolean isTeacher) {
        this.currentUserId = userId;
        this.isTeacher = isTeacher;
        // Refresh reports with user context
        generateReport();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupInitialDates();
        setupChartListeners();
        radarGc = radarChartCanvas.getGraphicsContext2D();
    }

    private void setupFilters() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load Categories
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT DISTINCT categoryName FROM OFFENSE_CATEGORY ORDER BY categoryName");
            ResultSet rs = stmt.executeQuery();
            ObservableList<String> categories = FXCollections.observableArrayList("All");
            while (rs.next()) {
                categories.add(rs.getString("categoryName"));
            }
            categoryFilter.setItems(categories);
            categoryFilter.setValue("All");

            // Load Courses
            stmt = conn.prepareStatement(
                "SELECT DISTINCT course FROM STUDENT ORDER BY course");
            rs = stmt.executeQuery();
            ObservableList<String> courses = FXCollections.observableArrayList("All");
            while (rs.next()) {
                courses.add(rs.getString("course"));
            }
            courseFilter.setItems(courses);
            courseFilter.setValue("All");

            // Setup Year Levels
            yearLevelFilter.setItems(FXCollections.observableArrayList(
                "All", "1", "2", "3", "4", "5"));
            yearLevelFilter.setValue("All");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load filters: " + e.getMessage());
        }
    }

    private void setupInitialDates() {
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
    }

    private void setupChartListeners() {
        // Add click listeners for drill-down
        trendsBarChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                XYChart.Data<String, Number> data = getClickedData(event, trendsBarChart);
                if (data != null) {
                    showDailyBreakdown(data.getXValue());
                }
            }
        });

        courseBarChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                XYChart.Data<String, Number> data = getClickedData(event, courseBarChart);
                if (data != null) {
                    showCourseDetails(data.getXValue());
                }
            }
        });
    }

    private <X,Y> XYChart.Data<X,Y> getClickedData(MouseEvent event, XYChart<X,Y> chart) {
        for (XYChart.Series<X,Y> series : chart.getData()) {
            for (XYChart.Data<X,Y> data : series.getData()) {
                if (data.getNode().getBoundsInParent().contains(event.getX(), event.getY())) {
                    return data;
                }
            }
        }
        return null;
    }

    private void showDailyBreakdown(String monthDay) {
        if (!isDetailedView) {
            isDetailedView = true;
            
            // Parse the clicked date
            LocalDate selectedDate = LocalDate.parse("2024-" + monthDay, 
                DateTimeFormatter.ofPattern("yyyy-MM/dd"));
            
            // Query violations for that specific day with hourly breakdown
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = """
                    SELECT HOUR(violationDate) as hour, COUNT(*) as count 
                    FROM VIOLATION 
                    WHERE DATE(violationDate) = ? 
                    GROUP BY HOUR(violationDate)
                    ORDER BY hour
                """;
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setDate(1, java.sql.Date.valueOf(selectedDate));
                ResultSet rs = stmt.executeQuery();

                XYChart.Series<String, Number> hourlyData = new XYChart.Series<>();
                hourlyData.setName("Hourly Breakdown for " + monthDay);

                while (rs.next()) {
                    int hour = rs.getInt("hour");
                    String timeLabel = String.format("%02d:00", hour);
                    hourlyData.getData().add(
                        new XYChart.Data<>(timeLabel, rs.getInt("count"))
                    );
                }

                trendsBarChart.getData().clear();
                trendsBarChart.getData().add(hourlyData);
                
                // Add back button
                Button backBtn = new Button("Back to Overview");
                backBtn.setOnAction(e -> {
                    isDetailedView = false;
                    updateTrendsChart();
                });
                // Add the back button to your layout
            } catch (SQLException e) {
                showError("Database Error", "Failed to load hourly breakdown: " + e.getMessage());
            }
        }
    }

    private void showCourseDetails(String course) {
        if (!isDetailedView) {
            isDetailedView = true;
            
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = """
                    SELECT oc.categoryName, COUNT(*) as count 
                    FROM VIOLATION v
                    JOIN STUDENT s ON v.studentID = s.studentID
                    JOIN OFFENSE_CATEGORY oc ON v.categoryID = oc.categoryID
                    WHERE s.course = ?
                    GROUP BY oc.categoryName
                """;
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, course);
                ResultSet rs = stmt.executeQuery();

                XYChart.Series<String, Number> categoryData = new XYChart.Series<>();
                categoryData.setName("Violations by Category for " + course);

                while (rs.next()) {
                    categoryData.getData().add(
                        new XYChart.Data<>(
                            rs.getString("categoryName"), 
                            rs.getInt("count")
                        )
                    );
                }

                courseBarChart.getData().clear();
                courseBarChart.getData().add(categoryData);
                
                // Add back button
                Button backBtn = new Button("Back to Overview");
                backBtn.setOnAction(e -> {
                    isDetailedView = false;
                    updateCourseChart();
                });
                // Add the back button to your layout
            } catch (SQLException e) {
                showError("Database Error", "Failed to load course details: " + e.getMessage());
            }
        }
    }

    private void drawRadarChart() {
        // Clear canvas
        radarGc.clearRect(0, 0, radarChartCanvas.getWidth(), radarChartCanvas.getHeight());
        
        // Get severity distribution data
        Map<String, Integer> severityData = reports.stream()
            .collect(Collectors.groupingBy(
                ViolationReport::getSeverity,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Setup radar chart parameters
        double centerX = radarChartCanvas.getWidth() / 2;
        double centerY = radarChartCanvas.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 50;
        
        String[] severities = {"Minor", "Moderate", "Major", "Severe"};
        int numPoints = severities.length;
        double angleStep = 2 * Math.PI / numPoints;
        
        drawRadarAxes(centerX, centerY, radius, severities, numPoints, angleStep);
        drawRadarData(centerX, centerY, radius, severities, numPoints, angleStep, severityData);
    }

    private void drawRadarAxes(double centerX, double centerY, double radius, 
            String[] severities, int numPoints, double angleStep) {
        radarGc.setStroke(Color.GRAY);
        radarGc.setLineWidth(1);
        
        for (int i = 0; i < numPoints; i++) {
            double angle = i * angleStep - Math.PI / 2;
            double endX = centerX + radius * Math.cos(angle);
            double endY = centerY + radius * Math.sin(angle);
            
            // Draw axis line
            radarGc.strokeLine(centerX, centerY, endX, endY);
            
            // Draw label
            double labelX = centerX + (radius + 20) * Math.cos(angle);
            double labelY = centerY + (radius + 20) * Math.sin(angle);
            radarGc.setTextAlign(TextAlignment.CENTER);
            radarGc.fillText(severities[i], labelX, labelY);
        }
    }

    private void drawRadarData(double centerX, double centerY, double radius,
            String[] severities, int numPoints, double angleStep, Map<String, Integer> severityData) {
        // Find max value for scaling
        double maxValue = severityData.values().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(1);
        
        // Set style for data polygon
        radarGc.setFill(Color.rgb(139, 0, 0, 0.3));
        radarGc.setStroke(Color.rgb(139, 0, 0));
        radarGc.setLineWidth(2);
        
        // Draw data polygon
        radarGc.beginPath();
        for (int i = 0; i < numPoints; i++) {
            double angle = i * angleStep - Math.PI / 2;
            int value = severityData.getOrDefault(severities[i], 0);
            double ratio = value / maxValue;
            double pointX = centerX + radius * ratio * Math.cos(angle);
            double pointY = centerY + radius * ratio * Math.sin(angle);
            
            if (i == 0) {
                radarGc.moveTo(pointX, pointY);
            } else {
                radarGc.lineTo(pointX, pointY);
            }
        }
        radarGc.closePath();
        radarGc.fill();
        radarGc.stroke();
    }

    private void updateComparisonChart() {
        // Calculate previous period dates
        long periodDays = ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue());
        previousPeriodStart = startDatePicker.getValue().minusDays(periodDays);
        previousPeriodEnd = startDatePicker.getValue().minusDays(1);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Current period series
            XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
            currentSeries.setName("Current Period");
            
            // Previous period series
            XYChart.Series<String, Number> previousSeries = new XYChart.Series<>();
            previousSeries.setName("Previous Period");
            
            // Query for both periods
            String query = """
                SELECT DATE(violationDate) as date, COUNT(*) as count 
                FROM VIOLATION 
                WHERE violationDate BETWEEN ? AND ?
                GROUP BY DATE(violationDate)
                ORDER BY date
            """;
            
            // Current period data
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setDate(1, java.sql.Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(2, java.sql.Date.valueOf(endDatePicker.getValue()));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                currentSeries.getData().add(new XYChart.Data<>(
                    rs.getDate("date").toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                    rs.getInt("count")
                ));
            }
            
            // Previous period data
            stmt.setDate(1, java.sql.Date.valueOf(previousPeriodStart));
            stmt.setDate(2, java.sql.Date.valueOf(previousPeriodEnd));
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                previousSeries.getData().add(new XYChart.Data<>(
                    rs.getDate("date").toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                    rs.getInt("count")
                ));
            }
            
            comparisonChart.getData().clear();
            comparisonChart.getData().addAll(currentSeries, previousSeries);
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load comparison data: " + e.getMessage());
        }
    }

    private void updateResolutionTimeAnalysis() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = """
                SELECT 
                    CASE 
                        WHEN TIMESTAMPDIFF(HOUR, violationDate, resolution_date) < 24 THEN '< 24 hours'
                        WHEN TIMESTAMPDIFF(HOUR, violationDate, resolution_date) < 72 THEN '1-3 days'
                        WHEN TIMESTAMPDIFF(HOUR, violationDate, resolution_date) < 168 THEN '3-7 days'
                        ELSE '> 7 days'
                    END as resolution_time,
                    severity,
                    COUNT(*) as count
                FROM VIOLATION 
                WHERE status = 'Resolved' 
                    AND resolution_date IS NOT NULL
                    AND violationDate BETWEEN ? AND ?
                GROUP BY resolution_time, severity
                ORDER BY 
                    CASE resolution_time
                        WHEN '< 24 hours' THEN 1
                        WHEN '1-3 days' THEN 2
                        WHEN '3-7 days' THEN 3
                        ELSE 4
                    END
            """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setDate(1, java.sql.Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(2, java.sql.Date.valueOf(endDatePicker.getValue()));
            ResultSet rs = stmt.executeQuery();
            
            // Create series for each severity level
            Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();
            String[] severityLevels = {"Minor", "Moderate", "Major", "Severe"};
            java.util.List<String> severities = Arrays.asList(severityLevels);
            
            for (String severity : severities) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(severity);
                seriesMap.put(severity, series);
            }
            
            // Populate data
            while (rs.next()) {
                String resolutionTime = rs.getString("resolution_time");
                String severity = rs.getString("severity");
                int count = rs.getInt("count");
                
                seriesMap.get(severity).getData().add(
                    new XYChart.Data<>(resolutionTime, count)
                );
            }
            
            resolutionTimeChart.getData().clear();
            resolutionTimeChart.getData().addAll(seriesMap.values());
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load resolution time analysis: " + e.getMessage());
        }
    }

    @FXML
    private void generateReport() {
        loadViolationData();
        updateStatistics();
        updateCharts();
        drawRadarChart();
        updateComparisonChart();
        updateResolutionTimeAnalysis();
    }

    private void loadViolationData() {
        reports.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder query = new StringBuilder(
                "SELECT v.*, s.firstName, s.lastName, s.course, s.yearLevel, " +
                "c.categoryName, c.defaultSeverity " +
                "FROM VIOLATION v " +
                "JOIN STUDENT s ON v.studentID = s.studentID " +
                "JOIN OFFENSE_CATEGORY c ON v.categoryID = c.categoryID " +
                "WHERE v.violationDate BETWEEN ? AND ?");

            // Add filters
            if (!"All".equals(categoryFilter.getValue())) {
                query.append(" AND c.categoryName = ?");
            }
            if (!"All".equals(courseFilter.getValue())) {
                query.append(" AND s.course = ?");
            }
            if (!"All".equals(yearLevelFilter.getValue())) {
                query.append(" AND s.yearLevel = ?");
            }

            PreparedStatement stmt = conn.prepareStatement(query.toString());
            int paramIndex = 1;
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(endDatePicker.getValue()));

            if (!"All".equals(categoryFilter.getValue())) {
                stmt.setString(paramIndex++, categoryFilter.getValue());
            }
            if (!"All".equals(courseFilter.getValue())) {
                stmt.setString(paramIndex++, courseFilter.getValue());
            }
            if (!"All".equals(yearLevelFilter.getValue())) {
                stmt.setInt(paramIndex++, Integer.parseInt(yearLevelFilter.getValue()));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ViolationReport report = new ViolationReport();
                report.setId(rs.getInt("violationID"));
                report.setStudentId(rs.getString("studentID"));
                report.setStudentName(rs.getString("firstName") + " " + rs.getString("lastName"));
                report.setCourse(rs.getString("course"));
                report.setYearLevel(rs.getString("yearLevel"));
                report.setCategory(rs.getString("categoryName"));
                report.setSeverity(rs.getString("severity"));
                report.setStatus(rs.getString("status"));
                report.setDescription(rs.getString("description"));
                report.setCreatedAt(rs.getTimestamp("violationDate").toLocalDateTime());
                reports.add(report);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load violation data: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String baseQuery = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'ACTIVE' OR status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as active,
                    SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved,
                    COUNT(DISTINCT studentID) as totalStudents
                FROM VIOLATION v
                WHERE violationDate BETWEEN ? AND ?
            """;
            
            // Add user context filter for teachers
            if (isTeacher) {
                baseQuery += " AND userID = ?";
            }
            
            // Add other filters
            if (!"All".equals(categoryFilter.getValue())) {
                baseQuery += " AND categoryID IN (SELECT categoryID FROM OFFENSE_CATEGORY WHERE categoryName = ?)";
            }
            if (!"All".equals(courseFilter.getValue())) {
                baseQuery += " AND studentID IN (SELECT studentID FROM STUDENT WHERE course = ?)";
            }
            if (!"All".equals(yearLevelFilter.getValue())) {
                baseQuery += " AND studentID IN (SELECT studentID FROM STUDENT WHERE yearLevel = ?)";
            }
            
            PreparedStatement stmt = conn.prepareStatement(baseQuery);
            int paramIndex = 1;
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(endDatePicker.getValue()));
            
            // Set user ID for teachers
            if (isTeacher) {
                stmt.setInt(paramIndex++, currentUserId);
            }
            
            // Set other filter parameters
            if (!"All".equals(categoryFilter.getValue())) {
                stmt.setString(paramIndex++, categoryFilter.getValue());
            }
            if (!"All".equals(courseFilter.getValue())) {
                stmt.setString(paramIndex++, courseFilter.getValue());
            }
            if (!"All".equals(yearLevelFilter.getValue())) {
                stmt.setInt(paramIndex++, Integer.parseInt(yearLevelFilter.getValue()));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int active = rs.getInt("active");
                int resolved = rs.getInt("resolved");
                int totalStudents = rs.getInt("totalStudents");
                
                totalViolationsLabel.setText(String.valueOf(total));
                activeViolationsLabel.setText(String.valueOf(active));
                resolvedViolationsLabel.setText(String.valueOf(resolved));
                totalStudentsLabel.setText(String.valueOf(totalStudents));
                
                // Calculate resolution rate
                double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 0;
                resolutionRateLabel.setText(String.format("%.1f%%", resolutionRate));
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update statistics: " + e.getMessage());
        }
    }

    private void updateCharts() {
        trendsBarChart.setAnimated(true);
        courseBarChart.setAnimated(true);
        yearLevelChart.setAnimated(true);
        violationsPieChart.setAnimated(true);

        updateTrendsChart();
        updateCourseChart();
        updateYearLevelChart();
        updateCategoryChart();

        // Add tooltips to all charts
        trendsBarChart.getData().forEach(series -> {
            series.getData().forEach(data -> {
                Tooltip tooltip = new Tooltip(
                    "Date: " + data.getXValue() + "\n" +
                    "Violations: " + data.getYValue()
                );
                Tooltip.install(data.getNode(), tooltip);
            });
        });

        courseBarChart.getData().forEach(series -> {
            series.getData().forEach(data -> {
                Tooltip tooltip = new Tooltip(
                    "Course: " + data.getXValue() + "\n" +
                    "Violations: " + data.getYValue()
                );
                Tooltip.install(data.getNode(), tooltip);
            });
        });
    }

    private void updateTrendsChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Violations");
        
        // Group by date and count
        Map<LocalDate, Long> trendData = reports.stream()
            .collect(Collectors.groupingBy(
                r -> r.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        trendData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(DateTimeFormatter.ofPattern("MM/dd")),
                    entry.getValue()
                ));
            });

        trendsBarChart.getData().clear();
        trendsBarChart.getData().add(series);
    }

    private void updateCourseChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Violations");
        
        Map<String, Long> courseData = reports.stream()
            .collect(Collectors.groupingBy(
                ViolationReport::getCourse,
                Collectors.counting()
            ));
        
        courseData.forEach((course, count) -> {
            series.getData().add(new XYChart.Data<>(course, count));
        });

        courseBarChart.getData().clear();
        courseBarChart.getData().add(series);
    }

    private void updateYearLevelChart() {
        Map<String, Map<String, Long>> yearLevelData = reports.stream()
            .collect(Collectors.groupingBy(
                ViolationReport::getYearLevel,
                Collectors.groupingBy(
                    ViolationReport::getSeverity,
                    Collectors.counting()
                )
            ));

        yearLevelChart.getData().clear();
        Set<String> severities = reports.stream()
            .map(ViolationReport::getSeverity)
            .collect(Collectors.toSet());
        
        severities.forEach(severity -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(severity);
            yearLevelData.forEach((yearLevel, data) -> {
                series.getData().add(new XYChart.Data<>(
                    yearLevel,
                    data.getOrDefault(severity, 0L)
                ));
            });
            yearLevelChart.getData().add(series);
        });
    }

    private void updateCategoryChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        Map<String, Long> categoryData = reports.stream()
            .collect(Collectors.groupingBy(
                ViolationReport::getCategory,
                Collectors.counting()
            ));
        
        categoryData.forEach((category, count) -> {
            pieChartData.add(new PieChart.Data(category, count));
        });

        violationsPieChart.setData(pieChartData);
    }

    @FXML
    private void exportToPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try {
                // Create document with custom margins
                Document document = new Document(PageSize.A4, 50, 50, 140, 150); // Increased bottom margin for footer
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
                
                // Add event handler for header and footer
                writer.setPageEvent(new PdfPageEventHelper() {
                    private Image headerImage;
                    private Image footerImage;
                    
                    @Override
                    public void onOpenDocument(PdfWriter writer, Document document) {
                        try {
                            // Load header and footer images from resources
                            String headerPath = getClass().getResource("/images/header.png").getPath();
                            String footerPath = getClass().getResource("/images/footer.png").getPath();
                            headerImage = Image.getInstance(headerPath);
                            footerImage = Image.getInstance(footerPath);
                            
                            // Scale header image to fit page width while maintaining aspect ratio
                            float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                            float headerRatio = pageWidth / headerImage.getWidth();
                            headerImage.scalePercent(headerRatio * 100);
                            
                            // Scale footer image to fit page width
                            float footerRatio = pageWidth / footerImage.getWidth();
                            footerImage.scalePercent(footerRatio * 100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        try {
                            // Add header at the top of each page
                            if (headerImage != null) {
                                headerImage.setAbsolutePosition(
                                    document.leftMargin(),
                                    document.getPageSize().getHeight() - 120 // Position header 120 points from top
                                );
                                writer.getDirectContent().addImage(headerImage);
                            }
                            
                            // Position footer at the bottom
                            if (footerImage != null) {
                                float footerY = 0; // This puts it at the very bottom edge of the page
                                footerImage.setAbsolutePosition(
                                    document.leftMargin(),
                                    footerY
                                );
                                writer.getDirectContent().addImage(footerImage);
                                
                                // Add page number
                                Font pageNumberFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
                                Phrase pageNumber = new Phrase(String.format("Page %d", writer.getPageNumber()), pageNumberFont);
                                
                                // Position page number above footer
                                float x = document.getPageSize().getWidth() - document.rightMargin() - 60;
                                float y = footerY + footerImage.getScaledHeight() + 10;
                                
                                PdfContentByte canvas = writer.getDirectContent();
                                ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, pageNumber, x, y, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                
                document.open();

                // Add title with proper spacing
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
                Paragraph title = new Paragraph("Violation Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingBefore(10);
                title.setSpacingAfter(20);
                document.add(title);

                // Add date range
                Font dateFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.GRAY);
                Paragraph dateRange = new Paragraph(
                    "Period: " + startDatePicker.getValue() + " to " + endDatePicker.getValue(),
                    dateFont
                );
                dateRange.setSpacingAfter(20);
                document.add(dateRange);

                // Add statistics with slightly smaller font
                Font statFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
                document.add(new Paragraph("Statistics:", statFont));
                document.add(new Paragraph("Total Violations: " + totalViolationsLabel.getText(), statFont));
                document.add(new Paragraph("Active Cases: " + activeViolationsLabel.getText(), statFont));
                document.add(new Paragraph("Resolved Cases: " + resolvedViolationsLabel.getText(), statFont));
                document.add(new Paragraph("Resolution Rate: " + resolutionRateLabel.getText(), statFont));
                document.add(new Paragraph("\n"));

                // Add violations table
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10);
                table.setKeepTogether(true);
                
                // Set table header style
                BaseColor headerBgColor = new BaseColor(240, 240, 240);
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.BLACK);
                String[] headers = {"Date", "Student", "Course", "Category", "Severity", "Status"};
                float[] columnWidths = {15f, 20f, 15f, 20f, 15f, 15f}; // Adjust column widths
                table.setWidths(columnWidths);
                
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(headerBgColor);
                    cell.setPadding(5);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                // Add data rows with consistent styling
                Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (ViolationReport report : reports) {
                    addStyledCell(table, report.getCreatedAt().format(formatter), cellFont);
                    addStyledCell(table, report.getStudentName(), cellFont);
                    addStyledCell(table, report.getCourse(), cellFont);
                    addStyledCell(table, report.getCategory(), cellFont);
                    addStyledCell(table, report.getSeverity(), cellFont);
                    addStyledCell(table, report.getStatus(), cellFont);
                }

                document.add(table);
                document.close();
                
                showSuccess("Success", "Report exported successfully to PDF!");
            } catch (Exception e) {
                showError("Export Error", "Failed to export PDF: " + e.getMessage());
            }
        }
    }

    private void addStyledCell(PdfPTable table, String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    @FXML
    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Violation Report");

                // Create header style
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // Create headers
                Row headerRow = sheet.createRow(0);
                String[] columns = {"Date", "Student", "Course", "Category", "Severity", "Status"};
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Add data
                int rowNum = 1;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (ViolationReport report : reports) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(report.getCreatedAt().format(formatter));
                    row.createCell(1).setCellValue(report.getStudentName());
                    row.createCell(2).setCellValue(report.getCourse());
                    row.createCell(3).setCellValue(report.getCategory());
                    row.createCell(4).setCellValue(report.getSeverity());
                    row.createCell(5).setCellValue(report.getStatus());
                }

                // Autosize columns
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Write to file
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    workbook.write(outputStream);
                }
                
                showSuccess("Success", "Report exported successfully to Excel!");
            } catch (IOException e) {
                showError("Export Error", "Failed to export Excel: " + e.getMessage());
            }
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        configureAlert(alert, title, message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        configureAlert(alert, title, message);
        alert.showAndWait();
    }

    private void configureAlert(Alert alert, String title, String message) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
    }
} 
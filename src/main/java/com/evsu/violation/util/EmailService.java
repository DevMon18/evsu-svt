package main.java.com.evsu.violation.util;

/**
 * EmailService class handles all email communication functionality for the EVSU Violation Tracker system.
 * This service is responsible for sending automated notifications to students, parents, and staff.
 * It uses Gmail SMTP for sending emails with both plain text and HTML formatting.
 * 
 * @author [Your Name]
 * @version 1.0
 * @since 2024-01-14
 */
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.io.UnsupportedEncodingException;

public class EmailService {
    /** SMTP host for Gmail */
    private static final String SMTP_HOST = "smtp.gmail.com";
    
    /** SMTP port for TLS */
    private static final String SMTP_PORT = "587";
    
    /** Email username for authentication */
    private static final String USERNAME = "violationstudent@gmail.com";
    
    /** App-specific password for Gmail */
    private static final String PASSWORD = "qvbe invr zqay fcoo";
    
    /** Sender email address */
    private static final String FROM_EMAIL = "violationstudent@gmail.com";
    
    /** Display name for the sender */
    private static final String FROM_NAME = "EVSU Violation Tracker";

    /** Theme color constants */
    private static final String PRIMARY_COLOR = "#8B0000";
    private static final String BACKGROUND_COLOR = "#f5f5f5";
    private static final String TEXT_COLOR = "#333333";

    /**
     * Creates and configures an email session with Gmail SMTP settings.
     * 
     * @return Session configured email session
     */
    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.debug", "true");
        props.put("mail.debug.auth", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        
        session.setDebug(true);
        return session;
    }

    /**
     * Sends an email to the specified recipient with both plain text and HTML content.
     * The email is sent asynchronously in a separate thread to prevent UI blocking.
     * Notifications are shown for both success and failure cases.
     *
     * @param toEmail recipient's email address
     * @param subject email subject line
     * @param plainTextBody the main content of the email in plain text format
     */
    public static void sendEmail(String toEmail, String subject, String plainTextBody) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.err.println("Error: Email address is empty");
            return;
        }

        System.out.println("\n=== Starting Email Send Process ===");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println("From: " + FROM_EMAIL);
        
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            
            // Create HTML version of the email
            String htmlContent = createHtmlEmail(plainTextBody);
            
            // Create multipart message
            Multipart multipart = new MimeMultipart("alternative");
            
            // Add plain text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(plainTextBody, "utf-8");
            
            // Add HTML part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            
            message.setContent(multipart);

            System.out.println("Email configured, attempting to send...");

            Thread emailThread = new Thread(() -> {
                try {
                    System.out.println("Starting email transport...");
                    Transport.send(message);
                    System.out.println("Email sent successfully to: " + toEmail);
                    System.out.println("=== Email Send Process Completed ===\n");
                    
                    javafx.application.Platform.runLater(() -> 
                        AlertHelper.showToast("Email notification sent successfully to " + toEmail, null));
                } catch (MessagingException e) {
                    System.err.println("\n=== Email Send Error ===");
                    System.err.println("Failed to send email to: " + toEmail);
                    System.err.println("Error message: " + e.getMessage());
                    e.printStackTrace();
                    System.err.println("=== End of Error Report ===\n");
                    
                    javafx.application.Platform.runLater(() -> 
                        AlertHelper.showError("Email Error", 
                            String.format("Failed to send email to %s\nError: %s", 
                                toEmail, e.getMessage())));
                }
            });
            emailThread.setDaemon(true);
            emailThread.start();
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("\n=== Email Preparation Error ===");
            System.err.println("Failed to prepare email for: " + toEmail);
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== End of Error Report ===\n");
            
            AlertHelper.showError("Email Error", 
                String.format("Failed to prepare email for %s\nError: %s", 
                    toEmail, e.getMessage()));
        }
    }

    /**
     * Creates an HTML version of the email with responsive design and styling.
     * Includes animations, proper formatting, and a professional layout.
     *
     * @param plainTextBody the content to be converted to HTML format
     * @return String containing the complete HTML email template
     */
    private static String createHtmlEmail(String plainTextBody) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    
                    @keyframes slideIn {
                        from { transform: translateX(-20px); opacity: 0; }
                        to { transform: translateX(0); opacity: 1; }
                    }
                    
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.8;
                        color: %s;
                        background-color: %s;
                        margin: 0;
                        padding: 20px;
                        -webkit-font-smoothing: antialiased;
                    }
                    
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                        animation: fadeIn 0.5s ease-out;
                    }
                    
                    .header {
                        background-color: %s;
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                        border-bottom: 4px solid rgba(0,0,0,0.1);
                    }
                    
                    .header h2 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        animation: slideIn 0.5s ease-out;
                    }
                    
                    .content {
                        padding: 40px 30px;
                        background: white;
                        animation: fadeIn 0.7s ease-out;
                    }
                    
                    .section {
                        margin-bottom: 30px;
                        padding: 20px;
                        background-color: %s;
                        border-radius: 6px;
                        border-left: 4px solid %s;
                        animation: slideIn 0.6s ease-out;
                    }
                    
                    .section-title {
                        font-size: 18px;
                        font-weight: 600;
                        color: %s;
                        margin-bottom: 15px;
                        padding-bottom: 8px;
                        border-bottom: 2px solid rgba(139,0,0,0.1);
                    }
                    
                    .info-item {
                        margin: 10px 0;
                        padding: 8px 0;
                        border-bottom: 1px solid rgba(0,0,0,0.05);
                    }
                    
                    .info-item strong {
                        color: %s;
                        font-weight: 600;
                        min-width: 140px;
                        display: inline-block;
                    }
                    
                    .notice-box {
                        background-color: #fff5f5;
                        border: 1px solid %s;
                        border-radius: 6px;
                        padding: 15px 20px;
                        margin: 20px 0;
                        color: %s;
                    }
                    
                    .steps-list {
                        list-style-type: none;
                        padding: 0;
                        margin: 15px 0;
                    }
                    
                    .steps-list li {
                        margin: 10px 0;
                        padding: 8px 0 8px 25px;
                        position: relative;
                    }
                    
                    .steps-list li:before {
                        content: '→';
                        position: absolute;
                        left: 0;
                        color: %s;
                    }
                    
                    .footer {
                        text-align: center;
                        padding: 20px;
                        background-color: #f8f9fa;
                        border-top: 1px solid #eee;
                        font-size: 12px;
                        color: #666;
                    }
                    
                    .signature {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 2px solid rgba(139,0,0,0.1);
                        font-style: italic;
                    }
                    
                    @media only screen and (max-width: 600px) {
                        body { padding: 10px; }
                        .container { margin: 10px; }
                        .content { padding: 20px 15px; }
                        .section { padding: 15px; }
                        .info-item strong { display: block; margin-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>EVSU OC - Student Violation System</h2>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>© 2024 EVSU Office of Student Affairs. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            TEXT_COLOR,           // body color
            BACKGROUND_COLOR,     // body background
            PRIMARY_COLOR,        // header background
            BACKGROUND_COLOR,     // section background
            PRIMARY_COLOR,        // section border
            PRIMARY_COLOR,        // section title
            PRIMARY_COLOR,        // info-item strong
            PRIMARY_COLOR,        // notice-box border
            PRIMARY_COLOR,        // notice-box color
            PRIMARY_COLOR,        // steps-list arrow color
            formatPlainTextToHtml(plainTextBody)  // content
        );
    }

    /**
     * Formats plain text content into structured HTML sections.
     * Handles different types of content like student information, violation details,
     * and important notices with appropriate styling.
     *
     * @param plainText the plain text content to be formatted
     * @return String containing formatted HTML content
     */
    private static String formatPlainTextToHtml(String plainText) {
        if (plainText == null) return "";
        
        // Split the content into sections
        String[] sections = plainText.split("\\n\\n+");
        StringBuilder html = new StringBuilder();
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            if (section.contains("Student Information:")) {
                html.append("<div class='section'>");
                html.append("<div class='section-title'>Student Information</div>");
                html.append(formatSection(section.replace("Student Information:", "")));
                html.append("</div>");
            }
            else if (section.contains("Violation Details:") || section.contains("Updated Violation Details:")) {
                html.append("<div class='section'>");
                html.append("<div class='section-title'>Violation Details</div>");
                html.append(formatSection(section.replaceAll("(Updated )?Violation Details:", "")));
                html.append("</div>");
            }
            else if (section.contains("Description:") || section.contains("Resolution Details:")) {
                html.append("<div class='section'>");
                html.append("<div class='section-title'>" + 
                    (section.contains("Resolution") ? "Resolution Details" : "Description") + "</div>");
                html.append(formatSection(section.replaceAll("(Description|Resolution Details):", "")));
                html.append("</div>");
            }
            else if (section.contains("Important Notice:")) {
                html.append("<div class='notice-box'>");
                html.append("<strong>Important Notice</strong><br>");
                html.append(section.replace("Important Notice:", "").trim());
                html.append("</div>");
            }
            else if (section.contains("Next Steps:")) {
                html.append("<div class='section'>");
                html.append("<div class='section-title'>Next Steps</div>");
                html.append("<ul class='steps-list'>");
                String[] steps = section.split("\\n");
                for (String step : steps) {
                    if (step.matches("\\d+\\..*")) {
                        html.append("<li>").append(step.replaceFirst("\\d+\\.\\s*", "").trim()).append("</li>");
                    }
                }
                html.append("</ul>");
                html.append("</div>");
            }
            else if (section.startsWith("Dear") || section.contains("Best regards")) {
                if (section.startsWith("Dear")) {
                    html.append("<p>").append(section.trim()).append("</p>");
                } else {
                    html.append("<div class='signature'>").append(section.trim()).append("</div>");
                }
            }
            else {
                html.append("<p>").append(section.trim()).append("</p>");
            }
        }
        
        return html.toString();
    }

    /**
     * Formats a section of text into HTML with proper styling and structure.
     * Converts key-value pairs into styled information items.
     *
     * @param content the section content to be formatted
     * @return String containing the formatted HTML section
     */
    private static String formatSection(String content) {
        StringBuilder html = new StringBuilder();
        String[] lines = content.trim().split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                html.append("<div class='info-item'>");
                html.append("<strong>").append(parts[0].trim()).append(":</strong> ");
                html.append(parts[1].trim());
                html.append("</div>");
            } else {
                html.append("<div class='info-item'>").append(line).append("</div>");
            }
        }
        
        return html.toString();
    }
} 
package main.java.com.evsu.violation.models;

public class Student {
    private String studentID;
    private String firstName;
    private String lastName;
    private String course;
    private int yearLevel;
    private String contactNumber;
    private String parentEmail;

    public Student() {}

    public Student(String studentID, String firstName, String lastName, String course,
                  int yearLevel, String contactNumber, String parentEmail) {
        this.studentID = studentID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contactNumber = contactNumber;
        this.parentEmail = parentEmail;
    }

    public Student(String studentID, String firstName, String lastName, String course) {
        this.studentID = studentID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.course = course;
    }

    // Getters
    public String getStudentID() { return studentID; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCourse() { return course; }
    public int getYearLevel() { return yearLevel; }
    public String getContactNumber() { return contactNumber; }
    public String getParentEmail() { return parentEmail; }

    // Setters
    public void setStudentID(String studentID) { this.studentID = studentID; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setCourse(String course) { this.course = course; }
    public void setYearLevel(int yearLevel) { this.yearLevel = yearLevel; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }

    // Convenience methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
} 
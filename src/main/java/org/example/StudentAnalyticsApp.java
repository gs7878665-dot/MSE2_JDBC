package org.example;
import java.sql.*;
import java.util.Scanner;

public class StudentAnalyticsApp {

    // Database connection details - UPDATE YOUR PASSWORD HERE
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_analytics_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "gaurav";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to MySQL Database successfully!");

            while (true) {
                System.out.println("\n=== STUDENT PERFORMANCE & ANALYTICS SYSTEM ===");
                System.out.println("1. Add New Student (Req 8: Validation & Duplicate Check)");
                System.out.println("2. View All Students: Average, Category & Stronger Subject (Req 1, 2, 3)");
                System.out.println("3. Search Student by ID, Email, or Course (Req 4)");
                System.out.println("4. Generate Merit List (Req 5)");
                System.out.println("5. Group Students by Same Technical Skills (Req 6)");
                System.out.println("6. Need Improvement in Programming (<50) & Math >= 75 (Req 7)");
                System.out.println("7. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume leftover newline

                switch (choice) {
                    case 1 -> addStudent(conn, scanner);
                    case 2 -> viewStudentAnalytics(conn);
                    case 3 -> searchStudents(conn, scanner);
                    case 4 -> generateMeritList(conn);
                    case 5 -> groupByTechSkill(conn);
                    case 6 -> filterMathStrongProgWeak(conn);
                    case 7 -> {
                        System.out.println("Exiting application...");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed! Check your URL, username, or password.");
            e.printStackTrace();
        }
    }

    // REQUIREMENT 8: Prevent duplicate IDs and validate scores (0-100)
    private static void addStudent(Connection conn, Scanner scanner) {
        System.out.print("Enter Student ID: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Course: ");
        String course = scanner.nextLine();

        System.out.print("Enter Mathematics Score (0-100): ");
        double math = scanner.nextDouble();

        System.out.print("Enter Programming Score (0-100): ");
        double prog = scanner.nextDouble();
        scanner.nextLine();

        // Application-level validation before hitting the database
        if (math < 0 || math > 100 || prog < 0 || prog > 100) {
            System.out.println("ERROR: Scores must be strictly between 0 and 100. Record rejected.");
            return;
        }

        System.out.print("Enter Technical Skill (e.g., Java, Python): ");
        String skill = scanner.nextLine();

        String sql = "INSERT INTO students (student_id, name, email, course, math_score, prog_score, tech_skill) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, email);
            pstmt.setString(4, course);
            pstmt.setDouble(5, math);
            pstmt.setDouble(6, prog);
            pstmt.setString(7, skill);

            pstmt.executeUpdate();
            System.out.println("Student added successfully!");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Triggered if student_id (PRIMARY KEY) or email (UNIQUE) already exists
            System.out.println("ERROR: Duplicate Student ID or Email detected! Record rejected.");
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    // REQUIREMENTS 1, 2, and 3: Average, Performance Category, and Stronger Subject
    private static void viewStudentAnalytics(Connection conn) throws SQLException {
        String sql = "SELECT student_id, name, math_score, prog_score FROM students";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("\n%-6s %-18s %-8s %-8s %-8s %-20s %-15s%n",
                    "ID", "Name", "Math", "Prog", "Avg", "Category", "Stronger Subject");
            System.out.println("-".repeat(88));

            while (rs.next()) {
                int id = rs.getInt("student_id");
                String name = rs.getString("name");
                double math = rs.getDouble("math_score");
                double prog = rs.getDouble("prog_score");

                // Req 1: Calculate average
                double avg = (math + prog) / 2.0;

                // Req 2: Assign performance category
                String category = getPerformanceCategory(avg);

                // Req 3: Identify stronger subject
                String stronger;
                if (math > prog) {
                    stronger = "Mathematics";
                } else if (prog > math) {
                    stronger = "Programming";
                } else {
                    stronger = "Equal in Both";
                }

                System.out.printf("%-6d %-18s %-8.2f %-8.2f %-8.2f %-20s %-15s%n",
                        id, name, math, prog, avg, category, stronger);
            }
        }
    }

    // Helper method for Requirement 2
    private static String getPerformanceCategory(double avg) {
        if (avg >= 85.0) return "Excellent";
        if (avg >= 70.0) return "Good";
        if (avg >= 50.0) return "Satisfactory";
        return "Needs Improvement";
    }

    // REQUIREMENT 4: Search by ID, Email, or Course
    private static void searchStudents(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter Student ID, Email, or Course to search: ");
        String keyword = scanner.nextLine();

        // CAST(student_id AS CHAR) allows matching numeric IDs alongside text fields cleanly
        String sql = "SELECT * FROM students WHERE CAST(student_id AS CHAR) = ? OR email LIKE ? OR course LIKE ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, keyword);
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("ID: %d | Name: %s | Email: %s | Course: %s | Math: %.2f | Prog: %.2f | Skill: %s%n",
                            rs.getInt("student_id"), rs.getString("name"), rs.getString("email"),
                            rs.getString("course"), rs.getDouble("math_score"), rs.getDouble("prog_score"),
                            rs.getString("tech_skill"));
                }
                if (!found) System.out.println("No matching students found.");
            }
        }
    }

    // REQUIREMENT 5: Merit list sorted by average score descending
    private static void generateMeritList(Connection conn) throws SQLException {
        String sql = "SELECT student_id, name, course, ((math_score + prog_score) / 2) AS avg_score " +
                "FROM students ORDER BY avg_score DESC";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- MERIT LIST ---");
            int rank = 1;
            while (rs.next()) {
                double avg = rs.getDouble("avg_score");
                System.out.printf("Rank #%d | ID: %d | Name: %-16s | Course: %-10s | Avg: %.2f (%s)%n",
                        rank++, rs.getInt("student_id"), rs.getString("name"),
                        rs.getString("course"), avg, getPerformanceCategory(avg));
            }
        }
    }

    // REQUIREMENT 6: Identify students who have the same technical skills
    private static void groupByTechSkill(Connection conn) throws SQLException {
        // GROUP_CONCAT combines matching student names into a single comma-separated string
        String sql = "SELECT tech_skill, GROUP_CONCAT(name SEPARATOR ', ') AS student_names, COUNT(*) AS total " +
                "FROM students GROUP BY tech_skill HAVING total > 1";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- STUDENTS SHARING TECHNICAL SKILLS ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Skill: %-12s | Count: %d | Students: %s%n",
                        rs.getString("tech_skill"), rs.getInt("total"), rs.getString("student_names"));
            }
            if (!found) System.out.println("No two students share the same technical skill yet.");
        }
    }

    // REQUIREMENT 7: Needs improvement in Programming (< 50) AND Math >= 75
    private static void filterMathStrongProgWeak(Connection conn) throws SQLException {
        String sql = "SELECT student_id, name, math_score, prog_score FROM students " +
                "WHERE prog_score < 50 AND math_score >= 75";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- STRONG IN MATH (>=75) BUT NEEDS IMPROVEMENT IN PROGRAMMING (<50) ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Name: %-16s | Math: %.2f | Programming: %.2f%n",
                        rs.getInt("student_id"), rs.getString("name"),
                        rs.getDouble("math_score"), rs.getDouble("prog_score"));
            }
            if (!found) System.out.println("No students match this criteria.");
        }
    }
}

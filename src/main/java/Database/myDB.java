package Database;

import Model.Students;
import Model.Teachers;
import Model.Exam;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.Thread.sleep;

public class myDB
{
    public  static final String dbName = "task3.db";
    public  static final String dbURL = "jdbc:sqlite:" + dbName;

    public static void createDB() {
        File dbFile = new File(dbName);
        if (dbFile.exists()) {
            System.out.println("Database already created");
            return;
        }
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);

        String createUserTableSQL =
                """
                CREATE TABLE IF NOT EXISTS Users (
                     User TEXT NOT NULL UNIQUE
                );
                """;
        String createStudent =
                """
                CREATE TABLE IF NOT EXISTS Student (
                    Student Text NOT NULL,
                    StudentID Text NOT NULL,
                    Grade integer NOT NULL,
                    Class integer NOT NULL,
                    PRIMARY KEY (StudentID)
                );
                """;
        String createTeacher =
                """
                CREATE TABLE IF NOT EXISTS Teacher (
                    Teacher Text NOT NULL,
                    PhoneNumber Text NOT NULL,
                    Grade Text NOT NULL,
                    Class Text NOT NULL,
                    PRIMARY KEY (PhoneNumber)
                );
                """;
        String createCourse =
                """
                CREATE TABLE IF NOT EXISTS Course (
                    CourseName Text NOT NULL,
                    PRIMARY KEY (CourseName)
                );
                """;
        String createScore =
                """
                CREATE TABLE IF NOT EXISTS Score (
                     StudentID Text NOT NULL,
                     CourseName Text Not NULL,
                     Score Double Not NULL,
                     ExamID Date Not NULL,
                     PRIMARY KEY (StudentID, ExamID)
                );
                """;
        String createExam =
                """
                CREATE TABLE IF NOT EXISTS Exam (
                     ExamID int IDENTITY(1,1),
                     CourseName Text Not NULL,
                     ExamDate Text Not NULL,
                     PRIMARY KEY (ExamID)
                );
                """;
        try (Connection ignored = DriverManager.getConnection(dbURL,config.toProperties())) {
            Statement statement = ignored.createStatement();
            statement.execute(createUserTableSQL);
            statement.execute(createStudent);
            statement.execute(createScore);
            statement.execute(createTeacher);
            statement.execute(createCourse);
            statement.execute(createExam);
            // If we get here that means no exception raised from getConnection - meaning it worked
            System.out.println("A new database has been created.");
            INSERTUserNewUser("Admin");
            INSERTUserNewUser("B");
            INSERTUserNewUser("C");
            INSERTUserNewUser("D");
            INSERTUserNewUser("E");
        }

        catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    public static void removeDB() {
        File dbFile = new File(dbName);
        if (dbFile.exists()) {
            boolean result = dbFile.delete();
            if (!result) {
                System.out.println("Couldn't delete existing db file");
                System.exit(-1);
            } else {
                System.out.println("Removed existing DB file.");
            }
        } else {
            System.out.println("No existing DB file.");
        }
    }

    public static void INSERTUserNewUser(String userName) {
        String addUserSQL =
                """
                INSERT INTO Users (User) 
                VALUES
                (?)
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,userName);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void delUser(String userName) {
        if(userName.equals("Admin")){
            return;
        }

        String delUserSQL =
                """
                DELETE FROM Users
                WHERE User == (?)
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(delUserSQL))
        {
            statement.setString(1,userName);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void delUserInCart(String userName){
        String deleteSQL = "DELETE FROM ShoppingCart WHERE User = ?";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;

        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(deleteSQL)) {
            statement.setString(1, userName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getUser(){
        String SELECT_QUERY = "SELECT User FROM Users";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        ArrayList<String> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_QUERY)) {
            // Get the number of rows in the ResultSet
            // Create a String array with the same size as the ResultSet
            // Populate the String array with the User values from the ResultSet
            while (rs.next()) {
                String user = rs.getString("User");
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return users; // Return an empty array if there was an error
        }
    }

    public static List<Entry<String, Integer>> getItem(String user){
        String SELECT_QUERY = "SELECT Item, Count FROM ShoppingCart WHERE User = ?";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement stmt = conn.prepareStatement(SELECT_QUERY)) {
            stmt.setString(1, user);
            ResultSet rs = stmt.executeQuery();
            List<Entry<String, Integer>> entries = new ArrayList<>();
            while (rs.next()) {
                String item = rs.getString("Item");
                int count = rs.getInt("Count");
                Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>(item, count);
                entries.add(entry);
            }
            return entries;
        } catch (SQLException e) {
            // handle the exception
            return null;
        }
    }


    public static void AddItem(String userName, String itemName, int count, double cost){
        //inserts a new item into the Item table with the specified ItemName, Count, and UserName.
        // If an item with the same ItemName and UserName already exists in the table,
        // the ON CONFLICT clause instructs SQLite to update the existing
        // record's Count value by adding the specified Count parameter
        String addItemSQL =
                """
                INSERT INTO ShoppingCart (User, Item, Count, Cost) 
                VALUES (?, ?, ?, ?) 
                ON CONFLICT(User, Item)
                DO UPDATE SET Count = ? , Cost = ?
                """;

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addItemSQL))
        {
            statement.setString(1,userName);
            statement.setString(2,itemName);
            statement.setInt(3,count);
            statement.setDouble(4,cost);
            statement.setInt(5,count);
            statement.setDouble(6,cost);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateCost(String userName, String itemName, int count, double cost){
        //inserts a new item into the Item table with the specified ItemName, Count, and UserName.
        // If an item with the same ItemName and UserName already exists in the table,
        // the ON CONFLICT clause instructs SQLite to update the existing
        // record's Count value by adding the specified Count parameter
        String addItemSQL =
                """
                INSERT INTO ShoppingCart (User, Item, Count, Cost) 
                VALUES (?, ?, ?, ?) 
                ON CONFLICT(User, Item)
                DO UPDATE SET Cost = ?
                """;

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addItemSQL))
        {
            statement.setString(1,userName);
            statement.setString(2,itemName);
            statement.setInt(3,count);
            statement.setDouble(4,cost);
            statement.setDouble(5,cost);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<String> getItems(String user) {
        String SELECT_QUERY = "SELECT Item FROM ShoppingCart WHERE User = ?";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement pstmt = conn.prepareStatement(SELECT_QUERY)) {
            pstmt.setString(1, user);
            List<String> items = new ArrayList<>();
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String item = rs.getString("Item");
                items.add(item);
            }
            return items;
        } catch (SQLException e) {
            // handle the exception
            return null;
        }
    }

    public static int getCountByUserAndItem(String user, String item) {
        String SELECT_QUERY = "SELECT Count FROM ShoppingCart WHERE User = ? AND Item = ?";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement pstmt = conn.prepareStatement(SELECT_QUERY)) {
            pstmt.setString(1, user);
            pstmt.setString(2, item);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("Count");
            } else {
                return 0; // the user and item combination does not exist in the table
            }
        } catch (SQLException e) {
            // handle the exception
            return -1; // or throw an exception
        }
    }

    public static Map<String, Integer> getItemMap(String userName) {
        Map<String, Integer> itemMap = new HashMap<>();
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement pstmt = conn.prepareStatement("SELECT Item, Count FROM ShoppingCart WHERE User = ?")) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String item = rs.getString("Item");
                int count = rs.getInt("Count");
                itemMap.put(item, count);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return itemMap;
    }

    public static double getCostByUserAndItem(String user, String item) {
        String SELECT_QUERY = "SELECT Cost FROM ShoppingCart WHERE User = ? AND Item = ?";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement pstmt = conn.prepareStatement(SELECT_QUERY)) {
            pstmt.setString(1, user);
            pstmt.setString(2, item);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("Cost");
            } else {
                return 0; // the user and item combination does not exist in the table
            }
        } catch (SQLException e) {
            // handle the exception
            return -1; // or throw an exception
        }
    }

    public static void removeItem(String itemName, String userName) {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM ShoppingCart WHERE Item = ? AND User = ?")) {
            pstmt.setString(1, itemName);
            pstmt.setString(2, userName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String[] getAllUsers() {
        List<String> userList = new ArrayList<>();
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT PhoneNumber FROM Teacher")) {
            while (rs.next()) {
                String user = rs.getString("PhoneNumber");
                if(!user.equals("Admin")){
                    userList.add(user);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return userList.toArray(new String[0]);
    }

    public static boolean userLogged(String userName) {
        String sql = "SELECT User FROM ShoppingCart WHERE User = ?";

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            return rs.next(); // returns true if a matching user was found
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, Double> getItemsAndCosts(String userName) {
        String sql = "SELECT Item, Cost FROM ShoppingCart WHERE User = ?";

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        Map<String, Double> itemsAndCosts = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String itemName = rs.getString("Item");
                Double cost = rs.getDouble("Cost");
                itemsAndCosts.put(itemName, cost);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return itemsAndCosts;
    }

    public static List<Students> getAllStudents(){
        List<Students> allStudents = new ArrayList<>();

        String sql = "SELECT * From Student";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
//                Student Text NOT NULL,
//                StudentID Text NOT NULL,
//                Grade integer NOT NULL,
//                Class integer NOT NULL,
//                PRIMARY KEY (StudentID)
                String StudentName = rs.getString("Student");
                String StudentID = rs.getString("StudentID");
                int Grade = rs.getInt("Grade");
                int myClass = rs.getInt("Class");
                allStudents.add(new Students(StudentName,StudentID,Grade,myClass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allStudents;
    }

    public static List<Teachers> getAllTeachers(){
        List<Teachers> allTeachers = new ArrayList<>();

        String sql = "SELECT * From Teacher";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
//                Teacher Text NOT NULL,
//                PhoneNumber Text NOT NULL,
//                Grade Text NOT NULL,
//                Class Text NOT NULL,
//                PRIMARY KEY (PhoneNumber)
                String Teacher = rs.getString("Teacher");
                String PhoneNumber = rs.getString("PhoneNumber");
                String Grade = rs.getString("Grade");
                String myClass = rs.getString("Class");
                allTeachers.add(new Teachers(Teacher,PhoneNumber,Grade,myClass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allTeachers;
    }

    public static void addStudent(Students student){
        String addUserSQL =
                """
                INSERT INTO Student (Student, StudentID, Grade, Class) 
                VALUES (?, ?, ?, ?) 
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,student.getStudentName());
            statement.setString(2,student.getStudentID());
            statement.setInt(3,student.getGrade());
            statement.setInt(4,student.getStudentClass());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addTeacher(Teachers teacher){
        String addUserSQL =
                """
                INSERT INTO Teacher (Teacher, PhoneNumber, Grade, Class) 
                VALUES (?, ?, ?, ?) 
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,teacher.getTeacher());
            statement.setString(2,teacher.getPhoneNumber());
            statement.setString(3,teacher.getGrade());
            statement.setString(4,teacher.getMyClass());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addCourse(String CourseName){
        String addUserSQL =
                """
                INSERT INTO Course (CourseName) 
                VALUES (?) 
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,CourseName);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void deleteCourse(String CourseName){
        String addUserSQL =
                """
                DELETE FROM Course
                WHERE CourseName == (?)
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,CourseName);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<String> getAllCourse(){
        List<String> allCourse = new ArrayList<>();

        String sql = "SELECT * From Course";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
//                Teacher Text NOT NULL,
//                PhoneNumber Text NOT NULL,
//                Grade Text NOT NULL,
//                Class Text NOT NULL,
//                PRIMARY KEY (PhoneNumber)
                String CourseName = rs.getString("CourseName");
                allCourse.add(CourseName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allCourse;
    }

    public static Students findAStudent(String ID){
        Students result = new Students(null,null,0,0);
        String sql = "SELECT * From Student Where StudentID == (?)";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, ID);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String StudentName = rs.getString("Student");
                String StudentID = rs.getString("StudentID");
                int Grade = rs.getInt("Grade");
                int myClass = rs.getInt("Class");
                result.StudentName = StudentName;
                result.StudentID = StudentID;
                result.Grade = Grade;
                result.myClass = myClass;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(result.StudentName == null){
            return null;
        }
        return result;
    }

    public static void addAExam(String examName, String examDate){
        String addUserSQL =
                """
                INSERT INTO Exam (CourseName, ExamDate) 
                VALUES (?, ?) 
                """;
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;
        try (Connection conn = DriverManager.getConnection(dbURL,config.toProperties());
             PreparedStatement statement = conn.prepareStatement(addUserSQL))
        {
            statement.setString(1,examName);
            statement.setString(2,examDate);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<Exam> getAllExam(){
        List<Exam> exams = new ArrayList<>();
        String query = "SELECT CourseName, ExamDate FROM Exam";

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        String dbURL = Database.myDB.dbURL;

        try (Connection conn = DriverManager.getConnection(dbURL, config.toProperties());
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String examName = resultSet.getString("CourseName");
                String examDate = resultSet.getString("ExamDate");

                Exam exam = new Exam(examName, examDate);
                exams.add(exam);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return exams;
    }

    public static void main(String[] args){
        Database.myDB.createDB();
        System.out.println("test");
        //removeDB();
    }


}

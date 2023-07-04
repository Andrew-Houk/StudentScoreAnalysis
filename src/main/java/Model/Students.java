package Model;

public class Students {
    public String StudentID;
    public String StudentName;
    public int Grade;
    public int myClass;

    public Students(String StudentName, String StudentID, int Grade, int Class){
        this.StudentID = StudentID;
        this.StudentName = StudentName;
        this.Grade = Grade;
        this.myClass = Class;
    }

    public String getStudentName() {
        return StudentName;
    }

    public int getStudentClass() {
        return myClass;
    }

    public String getStudentID() {
        return StudentID;
    }

    public int getGrade() {
        return Grade;
    }
}
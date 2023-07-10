package Model;

public class Exam {
    public String CourseName;
    public String examDate;
    public int examID;
    public Exam(String CourseName,String examDate,int examID){
        this.examDate = examDate;
        this.CourseName = CourseName;
        this.examID = examID;
    }
}

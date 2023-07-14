package Model;

public class Score {
    public String studentName;

    public String studentId;
    public double myScore;
    public int grade;
    public int myClass;
    public Score(String studentName, String studentId, int grade, int myClass, double score){
        this.studentName = studentName;
        this.studentId = studentId;
        this.myScore = score;
        this.grade = grade;
        this.myClass = myClass;
    }
}

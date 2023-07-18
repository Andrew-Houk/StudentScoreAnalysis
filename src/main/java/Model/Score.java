package Model;

import java.util.Date;

public class Score {
    public String studentName;
    public String studentId;
    public double myScore;
    public int grade;
    public int myClass;
    public Date scoreDate = null;
    public Score(String studentName, String studentId, int grade, int myClass, double score){
        this.studentName = studentName;
        this.studentId = studentId;
        this.myScore = score;
        this.grade = grade;
        this.myClass = myClass;
    }

    public void editScoreDate(Date scoreDate){
        this.scoreDate = scoreDate;
    }
}

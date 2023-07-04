package Model;

public class Teachers {
    public String PhoneNumber;
    public String Teacher;
    public String Grade;
    public String myClass;

    public Teachers(String Teacher, String PhoneNumber, String Grade, String myClass){
        this.PhoneNumber = PhoneNumber;
        this.Teacher = Teacher;
        this.Grade = Grade;
        this.myClass = myClass;
    }

    public String getGrade() {
        return Grade;
    }

    public String getMyClass() {
        return myClass;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public String getTeacher() {
        return Teacher;
    }
}

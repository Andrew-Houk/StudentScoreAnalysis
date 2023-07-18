package Controller;

import Database.myDB;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Model.Students;
import Model.Teachers;
import Model.Exam;
import Model.Score;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.util.stream.Collectors;

@Controller
public class AppController {
    private final SecureRandom randomNumberGenerator = new SecureRandom();
    private final HexFormat hexFormatter = HexFormat.of();

    private final AtomicLong counter = new AtomicLong();

    Map<String, ShoppingBasket> shoppingBaskets = new HashMap<>();

    Map<String, String> sessions = new HashMap<>();

    //String[] users = {"A", "B", "C", "D", "E"};


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam(value = "user", defaultValue = "") String user) {

        // We are just checking the username, in the real world you would also check their password here
        // or authenticate the user some other way.
        myDB.createDB();

        // Generate the session token.
        byte[] sessionTokenBytes = new byte[16];
        randomNumberGenerator.nextBytes(sessionTokenBytes);
        String sessionToken = hexFormatter.formatHex(sessionTokenBytes);

        // Store the association of the session token with the user.
        sessions.put(sessionToken, user);

        // Create HTTP headers including the instruction for the browser to store the session token in a cookie.
        String setCookieHeaderValue = String.format("session=%s; Path=/; HttpOnly; SameSite=Strict;", sessionToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", setCookieHeaderValue);

        // Check if the user is 'Admin' and set a separate cookie for them
        if(user.equals("Admin")) {
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).location(URI.create("/admin")).build();
        }

        if (!Arrays.asList(myDB.getAllUsers()).contains(user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user.\n");
        }

        // Redirect to the cart page, with the session-cookie-setting headers.
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).location(URI.create("/Home")).build();
    }

    // Home page
    @GetMapping("/Home")
    public String Home(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                       Model model) {

        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        return "Home";
    }

    @GetMapping("/my-class")
    public String myClass(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                          Model model) {
        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }
        List<List<Students>> allStudents = new ArrayList<>();

        System.out.println(sessions.get(sessionToken));
        System.out.println(myDB.teacherFindAllGrade(sessions.get(sessionToken)));
        System.out.println(myDB.teacherFindAllClass(sessions.get(sessionToken)));
        int index = 0;
        for (Integer grade:myDB.teacherFindAllGrade(sessions.get(sessionToken))){
            for (Integer myClass:myDB.teacherFindAllClass(sessions.get(sessionToken)).get(index)){
                if(myDB.findAllStudentInAClass(myClass,grade) != null){
                    List<Students> students = new ArrayList<>();
                    for(Students student:myDB.findAllStudentInAClass(myClass,grade)){
                        students.add(student);
                    }
                    allStudents.add(students);
                }
            }
            index++;
        }


        model.addAttribute("groupedStudents", allStudents);

        return "my-class"; // returns the view name for "My Class" page
    }

    @GetMapping("/student-score-analysis")
    public String studentScoreAnalysis() {
        return "student-score-analysis"; // returns the view name for "Student Score Analysis" page
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) throws InterruptedException {
        // Logic to read data from the uploaded file
        // You can use libraries like Apache POI to read Excel files
        // Process the data as needed
        if(!file.isEmpty()) {
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Adding Score...");
                Sheet sheet = workbook.getSheetAt(0); // Assuming you want to read the first sheet
                int row_index = 0;
                int cell_index = 0;
                String studentID = null;
                int examID = 0;
                double score = 0;
                // Iterate over rows
                for (Row row : sheet) {
                    row_index++;
                    cell_index = 0;
                    if (row_index > 1) {
                        // Iterate over cells
                        for (Cell cell : row) {
                            cell_index++;
                            CellType cellType = cell.getCellType();
                            // Read cell value based on cell type
                            if (cell_index == 1) {
                                if (cellType == CellType.STRING) {
                                    studentID = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    studentID = String.valueOf(cell.getNumericCellValue());
                                }

                            } else if (cell_index == 2) {
                                if (cellType == CellType.STRING) {
                                    examID = Integer.parseInt(cell.getStringCellValue());
                                } else if (cellType == CellType.NUMERIC) {
                                    examID = (int) cell.getNumericCellValue();
                                }
                            } else if (cell_index == 3) {
                                if (cellType == CellType.STRING) {
                                    score = Double.valueOf(cell.getStringCellValue());
                                } else if (cellType == CellType.NUMERIC) {
                                    score = cell.getNumericCellValue();
                                }
                            }
                        }
                        //System.out.println(name + " " + ID + " " + grade + " " + myClass); // Move to the next line after each row
                        myDB.addNewScoreWithExamID(studentID,examID,score);
                    }
                }
                timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/success"; // Redirect to a success page
    }

    @GetMapping("/success")
    public String success() {
        return "success"; // returns the view name for "My Class" page
    }

    @GetMapping("/score-input")
    public String scoreInput() {
        return "score-input"; // returns the view name for "Score Input" page
    }


    @GetMapping("/admin")
    public String admin(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                        Model model) {
        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        if (!sessions.get(sessionToken).equals("Admin")) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        List<Students> students = myDB.getAllStudents();

        model.addAttribute("students", students);

        return "admin";
    }

    @GetMapping("/Add-students")
    public String addStudents(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {
        model.addAttribute("student", new Students("test","test",0,0));
        return "Add-students"; // returns the view name for "Score Input" page
    }

    @PostMapping("/Add-A-student")
    public String addAStudent(Students student) {
        // Save the student to the database or perform other operations
        myDB.addStudent(student);
        return "redirect:/admin"; // Redirect to the student list page or any other desired page
    }

    @PostMapping("/Add-students")
    public String addStudents(@RequestParam("file") MultipartFile file) {
        // Save the student to the database or perform other operations
        if(!file.isEmpty()) {
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Adding Student...");
                Sheet sheet = workbook.getSheetAt(0); // Assuming you want to read the first sheet
                int row_index = 0;
                int cell_index = 0;
                String name = null;
                String ID = null;
                int grade = 0;
                int myClass = 0;
                // Iterate over rows
                for (Row row : sheet) {
                    row_index++;
                    cell_index = 0;
                    if (row_index > 1) {
                        // Iterate over cells
                        for (Cell cell : row) {
                            cell_index++;
                            CellType cellType = cell.getCellType();
                            // Read cell value based on cell type
                            if (cell_index == 1) {
                                name = cell.getStringCellValue();

                            } else if (cell_index == 2) {
                                if (cellType == CellType.STRING) {
                                    ID = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    ID = String.valueOf(cell.getNumericCellValue());
                                }
                            } else if (cell_index == 3) {
                                if (cellType == CellType.STRING) {
                                    grade = Integer.parseInt(cell.getStringCellValue());
                                } else if (cellType == CellType.NUMERIC) {
                                    grade = (int) cell.getNumericCellValue();
                                }
                            } else if (cell_index == 4) {
                                if (cellType == CellType.STRING) {
                                    myClass = Integer.parseInt(cell.getStringCellValue());
                                } else if (cellType == CellType.NUMERIC) {
                                    myClass = (int) cell.getNumericCellValue();
                                }
                            }
                        }
                        //System.out.println(name + " " + ID + " " + grade + " " + myClass); // Move to the next line after each row
                        myDB.addStudent(new Students(name,ID,grade,myClass));
                    }
                }
                timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/admin"; // Redirect to the student list page or any other desired page
    }

    @GetMapping("/students-download")
    public ResponseEntity<Resource> downloadExcelFileStudents() throws IOException {
        // Load the Excel file from the classpath or any other source
        Resource resource = new ClassPathResource("/download/学生批量登记表格.xlsx");

        // Get the file name (including extension)
        String fileName = "学生批量登记表格.xlsx";

        // Encode the file name using URL encoding with UTF-8 encoding
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

        // Set the appropriate headers
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/score-download")
    public ResponseEntity<Resource> downloadExcelFileScore() throws IOException {
        // Load the Excel file from the classpath or any other source
        Resource resource = new ClassPathResource("/download/成绩批量上传表.xlsx");

        // Get the file name (including extension)
        String fileName = "成绩批量上传表.xlsx";

        // Encode the file name using URL encoding with UTF-8 encoding
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

        // Set the appropriate headers
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/All-teachers")
    public String allTeachers(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                        Model model) {
        if (!sessions.containsKey(sessionToken)){
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        if (!sessions.get(sessionToken).equals("Admin")) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        List<Teachers> teachers = myDB.getAllTeachers();

        model.addAttribute("teachers", teachers);

        return "All-teachers";
    }

    @GetMapping("/Add-teachers")
    public String addTeachers(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {
        model.addAttribute("teacher", new Teachers("test","test","",""));
        return "Add-teachers"; // returns the view name for "Score Input" page
    }

    @PostMapping("/Add-A-teacher")
    public String addATeacher(Teachers teacher) {
        // Save the student to the database or perform other operations
        teacher.myClass = teacher.myClass.replace(" ","");
        teacher.myClass = teacher.myClass.replace("，",",");
        teacher.myClass = teacher.myClass.replace("；",";");
        teacher.Grade = teacher.Grade.replace(" ","");
        teacher.Grade = teacher.Grade.replace("，",",");
        myDB.addTeacher(teacher);
        return "redirect:/All-teachers"; // Redirect to the student list page or any other desired page
    }

    @PostMapping("/Add-teachers")
    public String addTeachers(@RequestParam("file") MultipartFile file){
        // Save the student to the database or perform other operations
        if(!file.isEmpty()) {
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Adding Teacher...");
                Sheet sheet = workbook.getSheetAt(0); // Assuming you want to read the first sheet
                int row_index = 0;
                int cell_index = 0;
                String name = null;
                String PhoneNumber = null;
                String grade = null;
                String myClass = null;
                // Iterate over rows
                for (Row row : sheet) {
                    row_index++;
                    cell_index = 0;
                    if (row_index > 1) {
                        // Iterate over cells
                        for (Cell cell : row) {
                            cell_index++;
                            CellType cellType = cell.getCellType();
                            // Read cell value based on cell type
                            if (cell_index == 1) {
                                if (cellType == CellType.STRING) {
                                    name = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    name = String.valueOf(cell.getNumericCellValue());
                                }
                            } else if (cell_index == 2) {
                                if (cellType == CellType.STRING) {
                                    PhoneNumber= cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    PhoneNumber = String.valueOf(cell.getNumericCellValue());
                                }
                            } else if (cell_index == 3) {
                                if (cellType == CellType.STRING) {
                                    grade = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    grade = String.valueOf(cell.getNumericCellValue());
                                }

                                if(grade.contains("，")){
                                    grade = grade.replace("，",",");
                                }
                                if(grade.contains(" ")){
                                    grade = grade.replace(" ","");
                                }
                            } else if (cell_index == 4) {
                                if (cellType == CellType.STRING) {
                                    myClass = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    myClass = String.valueOf(cell.getNumericCellValue());
                                }

                                if(myClass.contains("，")){
                                    myClass = myClass.replace("，",",");
                                }
                                if(myClass.contains(" ")){
                                    myClass = myClass.replace(" ","");
                                }
                            }
                        }
                        //System.out.println(name + " " + PhoneNumber + " " + grade + " " + myClass); // Move to the next line after each row
                        myDB.addTeacher(new Teachers(name,PhoneNumber,grade,myClass));
                    }
                }
                timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                System.out.println(timeStamp + " Finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/All-teachers"; // Redirect to the student list page or any other desired page
    }

    @GetMapping("/teachers-download")
    public ResponseEntity<Resource> downloadExcelFileTeachers() throws IOException {
        // Load the Excel file from the classpath or any other source
        Resource resource = new ClassPathResource("/download/教师批量登记表格.xlsx");

        // Get the file name (including extension)
        String fileName = "教师批量登记表格.xlsx";

        // Encode the file name using URL encoding with UTF-8 encoding
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

        // Set the appropriate headers
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/Course-setting")
    public String CourseSetting(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {

        List<String> courses = myDB.getAllCourse();

        // Pass the list of courses to the view
        model.addAttribute("courses", courses);

        return "Course-setting";
    }

    @PostMapping("/add-course")
    public String addCourse(@RequestParam("courseName") String courseName) {
        // Call the addCourse method to add the new course to the database
        myDB.addCourse(courseName);

        // Redirect back to the course setting page
        return "redirect:/Course-setting";
    }

    @PostMapping("/delete-course")
    public String deleteCourse(@RequestParam("courseName") String courseName) {
        // Call the deleteCourse method to delete the course from the database
        myDB.deleteCourse(courseName);

        // Redirect back to the course setting page
        return "redirect:/Course-setting";
    }


    @GetMapping("/Exam-management")
    public String addExam(Model model) {
        List<Exam> exams = myDB.getAllExam();
        model.addAttribute("exams", exams);
        return "Exam-management";
    }

    @PostMapping("/add-exam")
    public String addExam(@RequestParam("CourseName") String CourseName, @RequestParam("examDate") String examDate) {
        // Perform the logic to add the new exam to the database
        myDB.addAExam(CourseName, examDate);

        // Redirect to the course-setting page after adding the exam
        return "redirect:/Exam-management";
    }


    @GetMapping("/cart")
    public String cart(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                       Model model) {

        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        String user = sessions.get(sessionToken);
        model.addAttribute("items", shoppingBaskets.get(sessions.get(sessionToken)).getItems());
        model.addAttribute("name", user);
        model.addAttribute("value",shoppingBaskets.get(sessions.get(sessionToken)).getValue());

        //return ResponseEntity.status(HttpStatus.OK).body("[" + counter + "]");
        return "cart";
    }


    @GetMapping("/counter")
    public ResponseEntity<String> counter() {
        counter.incrementAndGet();
        return ResponseEntity.status(HttpStatus.OK).body("[" + counter + "]");
    }

    // post response cart page

    @PostMapping("/update-cart")
    public String updateCounts(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "count", defaultValue = "") Integer [] counts,
            Model model
    ) {
        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        // handling error

        try {
            //String user = sessions.get(sessionToken);
            int index = 0;
            List<Entry<String, Integer>> mylist = shoppingBaskets.get(sessions.get(sessionToken)).getItems();
            for(Entry<String, Integer> item:mylist){
                //shoppingBasket.addItem(item, counts[index]);
                shoppingBaskets.get(sessions.get(sessionToken)).updateItemValue(item.getKey(),counts[index]);
                index ++;
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "invalid";
        }


        return "redirect:/cart";
    }



    @GetMapping("/cost")
    public ResponseEntity<String> cost(@CookieValue(value = "session", defaultValue = "") String sessionToken) {
        return ResponseEntity.status(HttpStatus.OK).body(
                shoppingBaskets.get(sessions.get(sessionToken)).getValue() == null ? "0" : shoppingBaskets.get(sessions.get(sessionToken)).getValue().toString()
        );
    }

    @GetMapping("/adduser")
    public String newuser(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                          Model model) {
        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        if (!sessions.get(sessionToken).equals("Admin")) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        return "adduser";
    }

    // post response

    @PostMapping("/adduser")
    public String addUser(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "userName") String user,
            Model model
    ) {
        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        if (!sessions.get(sessionToken).equals("Admin")) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        try {
            myDB.INSERTUserNewUser(user);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "invalid";
        }

        return "redirect:/admin";
    }


    //log out page
    @GetMapping("/logout")
    public String logOut(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                         Model model) {

        if (!sessions.containsKey(sessionToken)) {
            model.addAttribute("error", "Invalid User Id");
            return "invalid";
        }

        String user = sessions.get(sessionToken);
        sessions.remove(sessionToken);


        return "logout";
    }

    @GetMapping("/greeting")
    public String greeting(
            @RequestParam(name="name", required=false, defaultValue="World") String name,
            Model model
    ) {
        model.addAttribute("name", name);
        return "greeting";
    }

    @GetMapping("/get-scores")
    @ResponseBody
    public List<Score> getScores(@RequestParam("examId") String examId) {
        // Fetch student data and scores based on the examId
        List<Score> scores = myDB.getScoresByExamId(examId);
        return scores;
    }

}

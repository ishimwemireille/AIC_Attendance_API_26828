package rw.ac.auca.register;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.service.CourseService;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.repository.SessionRepository;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.repository.StudentRepository;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Class RegisterImportService. Reads the attendance register the centre
 * already keeps in Excel.
 *
 * The file is not a list of students: it is the register itself. Each sheet is
 * one teaching group, the first four columns describe the student, and every
 * column after that is a date on which attendance was taken. One upload
 * therefore brings in the roster, a session per date, and the attendance.
 *
 *   NAME | STUDENT ID | GENDER | SECTOR | Aug 16 | Aug 18 | ...
 *
 * The fourth column is headed SECTOR in the centre's file but holds the
 * thematic area the student works in, so that is what it is called here.
 *
 * Imported attendance is taken as given. It was recorded on paper under the
 * centre's own practice, so the rules that govern marking in this system - a
 * time in for anyone present, a written reason for an absence - are not
 * applied to history that predates it. Nothing is notified either: these
 * absences happened weeks ago.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class RegisterImportService {

    private static final int NAME = 0;
    private static final int STUDENT_ID = 1;
    private static final int GENDER = 2;
    private static final int THEMATIC_AREA = 3;
    private static final int FIRST_DATE_COLUMN = 4;

    private static final LocalTime DEFAULT_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(10, 0);

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional
    public RegisterImportResult importInto(UUID courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("No file was uploaded.");
        }
        String name = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) {
            throw new BusinessRuleException("Only .xlsx spreadsheets are accepted. "
                    + "Save the file as Excel Workbook and try again.");
        }

        Course course = courseService.findCourseById(courseId);
        User actor = currentUserService.require();
        RegisterImportResult result = new RegisterImportResult();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                readSheet(workbook.getSheetAt(index), course, actor, result);
            }
        } catch (IOException exception) {
            throw new BusinessRuleException("The spreadsheet could not be read: "
                    + exception.getMessage());
        }
        return result;
    }

    private void readSheet(Sheet sheet, Course course, User actor, RegisterImportResult result) {
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) {
            return;
        }
        String group = sheet.getSheetName().trim();

        // Which columns carry a date, and which date. Headers like "Column 6"
        // are Excel's own filler and are reported as ignored rather than guessed at.
        Map<Integer, LocalDate> dates = new LinkedHashMap<>();
        for (int column = FIRST_DATE_COLUMN; column < header.getLastCellNum(); column++) {
            Cell cell = header.getCell(column);
            String label = text(cell);
            if (label.isEmpty()) {
                continue;
            }
            LocalDate date = parseDate(cell, label, course);
            if (date == null) {
                if (!label.toLowerCase(Locale.ROOT).startsWith("column")) {
                    result.getColumnsIgnored().add(group + ": " + label);
                }
                continue;
            }
            dates.put(column, date);
        }

        Map<LocalDate, Session> sessions = new LinkedHashMap<>();

        for (int number = sheet.getFirstRowNum() + 1; number <= sheet.getLastRowNum(); number++) {
            Row row = sheet.getRow(number);
            if (row == null) {
                continue;
            }
            String regNumber = text(row.getCell(STUDENT_ID));
            if (!regNumber.matches("[0-9]{5}")) {
                // Blank spacer rows and the repeated header are simply not students.
                continue;
            }
            try {
                Student student = upsertStudent(row, regNumber, group, result);
                recordAttendance(row, student, dates, sessions, course, actor, result, group, number);
            } catch (Exception exception) {
                result.problem(group, number + 1, exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage());
            }
        }
    }

    private Student upsertStudent(Row row, String regNumber, String group,
                                  RegisterImportResult result) {
        return studentRepository.findByRegNumber(regNumber)
                .map(existing -> {
                    result.setStudentsAlreadyKnown(result.getStudentsAlreadyKnown() + 1);
                    return existing;
                })
                .orElseGet(() -> {
                    Student student = new Student();
                    student.setRegNumber(regNumber);
                    applyName(student, text(row.getCell(NAME)));
                    student.setGender(gender(text(row.getCell(GENDER))));
                    student.setThematicArea(blankTo(text(row.getCell(THEMATIC_AREA)), "Not recorded"));
                    student.setStudentGroup(group);
                    student.setActive(true);
                    Student saved = studentRepository.save(student);
                    result.setStudentsCreated(result.getStudentsCreated() + 1);
                    return saved;
                });
    }

    private void recordAttendance(Row row, Student student, Map<Integer, LocalDate> dates,
                                  Map<LocalDate, Session> sessions, Course course, User actor,
                                  RegisterImportResult result, String group, int number) {
        for (Map.Entry<Integer, LocalDate> entry : dates.entrySet()) {
            AttendanceStatus status = status(text(row.getCell(entry.getKey())));
            if (status == null) {
                continue;
            }
            Session session = sessions.computeIfAbsent(entry.getValue(),
                    date -> findOrCreateSession(course, date, result));

            if (attendanceRepository.existsByStudentIdAndSessionId(student.getId(), session.getId())) {
                result.setAttendanceAlreadyRecorded(result.getAttendanceAlreadyRecorded() + 1);
                continue;
            }
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setSession(session);
            attendance.setRecordedBy(actor);
            attendance.setStatus(status);
            attendance.setRemarks("Imported from the attendance register");
            attendanceRepository.save(attendance);
            result.setAttendanceCreated(result.getAttendanceCreated() + 1);
        }
    }

    private Session findOrCreateSession(Course course, LocalDate date, RegisterImportResult result) {
        return sessionRepository
                .findByCourseIdAndSessionDateAndStartTime(course.getId(), date, DEFAULT_START)
                .orElseGet(() -> {
                    Session session = new Session();
                    session.setCourse(course);
                    session.setTitle(course.getTitle() + " - " + date);
                    session.setSessionDate(date);
                    session.setStartTime(DEFAULT_START);
                    session.setEndTime(DEFAULT_END);
                    session.setRoom("Register");
                    session.setCapacity(500);
                    session.setActive(true);
                    Session saved = sessionRepository.save(session);
                    result.setSessionsCreated(result.getSessionsCreated() + 1);
                    return saved;
                });
    }

    /**
     * The register writes a name as one field, sometimes with a role in
     * brackets. Everything before the first space becomes the first name and
     * the remainder the last, which is lossy for a three part name but keeps
     * the full name readable when the two are joined again.
     */
    private void applyName(Student student, String raw) {
        String cleaned = raw.replaceAll("\\(.*?\\)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) {
            throw new BusinessRuleException("The name is empty.");
        }
        int space = cleaned.indexOf(' ');
        String first = space < 0 ? cleaned : cleaned.substring(0, space);
        String last = space < 0 ? cleaned : cleaned.substring(space + 1).trim();

        student.setFirstName(cut(pad(first), 30));
        student.setLastName(cut(pad(last), 30));
    }

    /** The database asks for at least two characters. */
    private String pad(String value) {
        return value.length() >= 2 ? value : value + ".";
    }

    private String cut(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String gender(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("f")) {
            return "Female";
        }
        if (value.startsWith("m")) {
            return "Male";
        }
        throw new BusinessRuleException("Gender \"" + raw + "\" is neither Male nor Female.");
    }

    /** The two sheets disagree: one writes "Excuse", the other "Excused". */
    private AttendanceStatus status(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("p")) {
            return AttendanceStatus.PRESENT;
        }
        if (value.startsWith("a")) {
            return AttendanceStatus.ABSENT;
        }
        if (value.startsWith("e")) {
            return AttendanceStatus.EXCUSED;
        }
        if (value.startsWith("l")) {
            return AttendanceStatus.LATE;
        }
        return null;
    }

    /**
     * A header such as "Aug 16" carries no year, so the course supplies it. A
     * course that runs across New Year is handled by trying the following year
     * when the first attempt lands before it started.
     */
    private LocalDate parseDate(Cell cell, String label, Course course) {
        if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String cleaned = label.replaceAll("[,.]", " ").replaceAll("\\s+", " ").trim();
        for (String pattern : new String[]{"MMM d", "d MMM", "MMMM d", "d MMMM"}) {
            for (int year : new int[]{course.getStartDate().getYear(),
                    course.getStartDate().getYear() + 1}) {
                try {
                    LocalDate date = LocalDate.parse(cleaned + " " + year,
                            DateTimeFormatter.ofPattern(pattern + " yyyy", Locale.ENGLISH));
                    if (!date.isBefore(course.getStartDate()) && !date.isAfter(course.getEndDate())) {
                        return date;
                    }
                } catch (DateTimeParseException ignored) {
                    // try the next shape
                }
            }
        }
        try {
            return LocalDate.parse(cleaned);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /** An empty register in the centre's own shape. */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (String group : new String[]{"GROUP A", "GROUP B"}) {
                Sheet sheet = workbook.createSheet(group);
                Row header = sheet.createRow(0);
                String[] fixed = {"NAME", "STUDENT ID", "GENDER", "SECTOR"};
                for (int i = 0; i < fixed.length; i++) {
                    header.createCell(i).setCellValue(fixed[i]);
                    sheet.setColumnWidth(i, 24 * 256);
                }
                LocalDate date = LocalDate.now().with(DayOfWeek.MONDAY);
                for (int i = 0; i < 6; i++) {
                    header.createCell(FIRST_DATE_COLUMN + i).setCellValue(
                            date.plusDays(i * 2L).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)));
                    sheet.setColumnWidth(FIRST_DATE_COLUMN + i, 12 * 256);
                }
                Row example = sheet.createRow(1);
                String[] sample = {"Aline Uwase", "24011", "Female", "Health"};
                for (int i = 0; i < sample.length; i++) {
                    example.createCell(i).setCellValue(sample[i]);
                }
                example.createCell(FIRST_DATE_COLUMN).setCellValue("Present");
                example.createCell(FIRST_DATE_COLUMN + 1).setCellValue("Absent");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new BusinessRuleException("The template could not be produced.");
        }
    }
}

package rw.ac.auca.student.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.student.domain.Student;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Class StudentImportService. Reads a spreadsheet of students.
 *
 * The columns are fixed and must appear in this order:
 *   registration number, first name, last name, email, phone, gender,
 *   program, year of study
 * A header row is skipped if the first cell does not look like a registration
 * number. Every row is validated with the same annotations the REST API uses,
 * so a spreadsheet cannot introduce a record the API would have refused.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class StudentImportService {

    private static final String[] COLUMNS = {
            "Registration number", "First name", "Last name", "Email",
            "Phone", "Gender", "Program", "Year of study"
    };

    @Autowired
    private StudentService studentService;

    @Autowired
    private Validator validator;

    public StudentImportResult importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("No file was uploaded.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            throw new BusinessRuleException(
                    "Only .xlsx spreadsheets are accepted. Save the file as Excel Workbook and try again.");
        }

        StudentImportResult result = new StudentImportResult();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (isBlank(row)) {
                    continue;
                }
                String first = text(row.getCell(0));
                // Skip the header: a registration number is five digits.
                if (row.getRowNum() == 0 && !first.matches("[0-9]{5}")) {
                    continue;
                }
                readRow(row, result);
            }
        } catch (IOException exception) {
            throw new BusinessRuleException("The spreadsheet could not be read: "
                    + exception.getMessage());
        }

        return result;
    }

    private void readRow(Row row, StudentImportResult result) {
        int number = row.getRowNum() + 1;
        try {
            Student student = new Student();
            student.setRegNumber(text(row.getCell(0)));
            student.setFirstName(text(row.getCell(1)));
            student.setLastName(text(row.getCell(2)));
            student.setEmail(text(row.getCell(3)).toLowerCase());
            student.setPhone(text(row.getCell(4)));
            student.setGender(text(row.getCell(5)));
            student.setProgram(text(row.getCell(6)));
            student.setYearOfStudy(number(row.getCell(7)));
            student.setActive(true);

            Set<ConstraintViolation<Student>> violations = validator.validate(student);
            if (!violations.isEmpty()) {
                result.add(number, violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .sorted()
                        .collect(Collectors.joining("; ")));
                return;
            }

            studentService.registerStudent(student);
            result.setImported(result.getImported() + 1);
        } catch (Exception exception) {
            result.add(number, exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    /** An empty workbook template, so nobody has to guess the column order. */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                header.createCell(i).setCellValue(COLUMNS[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }

            Row example = sheet.createRow(1);
            String[] sample = {"24011", "Aline", "Uwase", "aline.24011@auca.ac.rw",
                    "0788123456", "Female", "Information Technology", "3"};
            for (int i = 0; i < sample.length; i++) {
                example.createCell(i).setCellValue(sample[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new BusinessRuleException("The template could not be produced.");
        }
    }

    private boolean isBlank(Row row) {
        for (int i = 0; i < COLUMNS.length; i++) {
            if (!text(row.getCell(i)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Excel stores a number typed as a registration number as 24011.0. */
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

    private int number(Cell cell) {
        String value = text(cell);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Year of study must be a number, not \"" + value + "\".");
        }
    }
}

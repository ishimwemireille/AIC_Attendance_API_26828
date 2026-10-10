package rw.ac.auca.register;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class RegisterImportResult. What came of reading an attendance register.
 *
 * Nothing here stops at the first problem: a row that cannot be read is
 * reported with its sheet and row number, and the rest of the file is still
 * imported.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class RegisterImportResult {

    private int studentsCreated;
    private int studentsAlreadyKnown;
    private int sessionsCreated;
    private int attendanceCreated;
    private int attendanceAlreadyRecorded;

    /** Column headers that were not a date, so carried no attendance. */
    private List<String> columnsIgnored = new ArrayList<>();

    private List<Problem> problems = new ArrayList<>();

    public void problem(String sheet, int row, String message) {
        problems.add(new Problem(sheet, row, message));
    }

    @Getter
    @Setter
    public static class Problem {
        private String sheet;
        private int row;
        private String message;

        public Problem() {
        }

        public Problem(String sheet, int row, String message) {
            this.sheet = sheet;
            this.row = row;
            this.message = message;
        }
    }
}

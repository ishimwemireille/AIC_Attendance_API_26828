package rw.ac.auca.student.service;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class StudentImportResult. What came of reading a spreadsheet.
 *
 * A bad row never stops the import: every row is attempted and the ones that
 * failed are reported with their row number, so the cohort leader can fix the
 * spreadsheet rather than guess.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class StudentImportResult {

    private int imported;
    private int failed;
    private List<RowError> errors = new ArrayList<>();

    public void add(int row, String message) {
        errors.add(new RowError(row, message));
        failed++;
    }

    @Getter
    @Setter
    public static class RowError {
        private int row;
        private String message;

        public RowError() {
        }

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }
    }
}

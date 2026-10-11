package rw.ac.auca.course.service;

import lombok.Getter;
import lombok.Setter;
import rw.ac.auca.course.domain.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class CourseProgress. What a trainer sees about their own course.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class CourseProgress {

    private Course course;

    /** Sessions scheduled, and how many of them have had attendance taken. */
    private int sessionsPlanned;
    private int sessionsDelivered;

    private int recordsWritten;
    private int attendanceRate;

    /** Where the course is in its own calendar, as a percentage of its days. */
    private int elapsedPercent;

    private List<StudentStanding> atRisk = new ArrayList<>();

    @Getter
    @Setter
    public static class StudentStanding {
        private String studentId;
        private String regNumber;
        private String fullName;
        private int attended;
        private int total;
        private int rate;
    }
}

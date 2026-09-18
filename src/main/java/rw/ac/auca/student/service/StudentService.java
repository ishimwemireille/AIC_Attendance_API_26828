package rw.ac.auca.student.service;

import rw.ac.auca.student.domain.Student;

import java.util.List;
import java.util.UUID;

/**
 * The Interface StudentService.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public interface StudentService {
    Student registerStudent(Student theStudent);
    Student updateStudent(UUID id, Student theStudent);
    void deleteStudent(UUID id);
    Student findStudentById(UUID id);
    List<Student> findAllStudents();
}

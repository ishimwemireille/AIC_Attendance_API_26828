package rw.ac.auca.student.service;

import org.springframework.beans.factory.annotation.Autowired;
import rw.ac.auca.audit.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.repository.StudentRepository;

import java.util.List;
import java.util.UUID;

/**
 * The Class StudentServiceImpl.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public Student registerStudent(Student theStudent) {
        if (studentRepository.existsByRegNumber(theStudent.getRegNumber())) {
            throw new BusinessRuleException("Registration number "
                    + theStudent.getRegNumber() + " is already used by another student.");
        }
        if (hasText(theStudent.getEmail())
                && studentRepository.existsByEmailIgnoreCase(theStudent.getEmail())) {
            throw new BusinessRuleException("Email "
                    + theStudent.getEmail() + " is already used by another student.");
        }
        checkSharedRules(theStudent);
        Student saved = studentRepository.save(theStudent);
        auditService.record("Student", saved.getId(), AuditService.CREATE, null, saved);
        return saved;
    }

    @Override
    public Student updateStudent(UUID id, Student theStudent) {
        Student before = snapshotOf(id);
        Student found = findStudentById(id);

        if (studentRepository.existsByRegNumberAndIdNot(theStudent.getRegNumber(), id)) {
            throw new BusinessRuleException("Registration number "
                    + theStudent.getRegNumber() + " is already used by another student.");
        }
        if (hasText(theStudent.getEmail())
                && studentRepository.existsByEmailIgnoreCaseAndIdNot(theStudent.getEmail(), id)) {
            throw new BusinessRuleException("Email "
                    + theStudent.getEmail() + " is already used by another student.");
        }
        checkSharedRules(theStudent);

        found.setRegNumber(theStudent.getRegNumber());
        found.setFirstName(theStudent.getFirstName());
        found.setLastName(theStudent.getLastName());
        found.setEmail(theStudent.getEmail());
        found.setPhone(theStudent.getPhone());
        found.setGender(theStudent.getGender());
        found.setSector(theStudent.getSector());
        found.setStudentGroup(theStudent.getStudentGroup());
        found.setYearOfStudy(theStudent.getYearOfStudy());
        found.setActive(theStudent.isActive());
        Student saved = studentRepository.save(found);
        auditService.record("Student", saved.getId(), AuditService.UPDATE, before, saved);
        return saved;
    }

    private void checkSharedRules(Student theStudent) {
        if (!theStudent.getEmail().toLowerCase().endsWith("@auca.ac.rw")) {
            throw new BusinessRuleException("Email must be an AUCA email ending with @auca.ac.rw");
        }
        if (theStudent.getFirstName().equalsIgnoreCase(theStudent.getLastName())) {
            throw new BusinessRuleException("First name and last name cannot be the same.");
        }
    }

    @Override
    public void deleteStudent(UUID id) {
        Student found = findStudentById(id);
        long records = attendanceRepository.countByStudentId(id);
        if (records > 0) {
            throw new BusinessRuleException("Cannot delete " + found.getFullName()
                    + " because " + records + " attendance record(s) exist for this student. "
                    + "Deactivate the student instead.");
        }
        auditService.record("Student", found.getId(), AuditService.DELETE, found, null);
        studentRepository.delete(found);
    }

    @Override
    @Transactional(readOnly = true)
    public Student findStudentById(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No student found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }

    /** A detached copy of the row as it is now, for the audit trail. */
    private Student snapshotOf(UUID id) {
        Student current = findStudentById(id);
        Student copy = new Student();
        copy.setId(current.getId());
        copy.setRegNumber(current.getRegNumber());
        copy.setFirstName(current.getFirstName());
        copy.setLastName(current.getLastName());
        copy.setEmail(current.getEmail());
        copy.setPhone(current.getPhone());
        copy.setGender(current.getGender());
        copy.setSector(current.getSector());
        copy.setStudentGroup(current.getStudentGroup());
        copy.setYearOfStudy(current.getYearOfStudy());
        copy.setActive(current.isActive());
        return copy;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

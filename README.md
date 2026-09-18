# AUCA Innovation Center - Student Attendance System (REST API)

Assignment 4 - Web Technology, Adventist University of Central Africa.

A Spring Boot REST API that manages the attendance of students attending sessions at the
AUCA Innovation Center. Three entities are implemented with full CRUD and the business
rules of the project.

## Entities

| Entity | Role |
|---|---|
| `Student` | A student registered at the Innovation Center |
| `Session` | One training session (title, date, times, room, trainer, capacity) |
| `Attendance` | One student at one session, with arrival time and status |

`BaseEntity` holds the fields shared by all three: `id` (UUID), `createdAt`, `updatedAt`.

## Layered structure

```
rw.ac.auca
├── base                  BaseEntity
├── config                SecurityConfig
├── controller            StudentController, SessionController, AttendanceController
├── exception             BusinessRuleException, ResourceNotFoundException, GlobalExceptionHandler
├── student  ├── domain   ├── repository   └── service
├── session  ├── domain   ├── repository   └── service
└── attendance ├── domain ├── repository   └── service
```

## Endpoints

| Method | Students | Sessions | Attendances |
|---|---|---|---|
| GET all | `/api/students` | `/api/sessions` | `/api/attendances` |
| GET one | `/api/students/{id}` | `/api/sessions/{id}` | `/api/attendances/{id}` |
| POST | `/api/students` | `/api/sessions` | `/api/attendances` |
| PUT | `/api/students/{id}` | `/api/sessions/{id}` | `/api/attendances/{id}` |
| DELETE | `/api/students/{id}` | `/api/sessions/{id}` | `/api/attendances/{id}` |

Two extra reads on attendance:

- `GET /api/attendances/session/{sessionId}` - the register of one session
- `GET /api/attendances/student/{studentId}` - the history of one student

## Business rules

**Student**

- Registration number must be 5 digits and unique
- Email must be unique and must end with `@auca.ac.rw`
- First name and last name cannot be the same
- Phone must be 10 digits starting with `07`
- Year of study must be between 1 and 5
- A student who already has attendance records cannot be deleted, only deactivated

**Session**

- End time must be after the start time
- The same room cannot be booked twice on the same date at the same start time
- Capacity cannot be reduced below the number of students already marked
- A session that already has attendance records cannot be deleted

**Attendance**

- A student can only be marked once per session
- Attendance cannot be recorded for a session that takes place in the future
- An inactive student, or a cancelled session, cannot be marked
- `PRESENT` requires an arrival at or before the session start time
- `LATE` requires an arrival after the session start time
- Arrival cannot be after the session end time
- `ABSENT` and `EXCUSED` require a written reason of at least 5 characters
- A session cannot be marked beyond its capacity

## HTTP status codes

| Code | Meaning |
|---|---|
| 200 | Read or update succeeded |
| 201 | Record created |
| 204 | Record deleted |
| 400 | Field validation failed - the body lists each field and its message |
| 404 | No record with that id |
| 409 | A business rule or database constraint was broken |

## Technology

Spring Boot 4.1.1, Spring Data JPA, Hibernate, PostgreSQL, Spring Validation,
Spring Security, Lombok, Maven, Java 26.

## Running the project

1. Create the database:

```sql
CREATE DATABASE aicattendancedb;
```

2. Put your own PostgreSQL password in `src/main/resources/application.properties`.
3. Run:

```
mvn spring-boot:run
```

4. The API is available at `http://localhost:8080/api/...`

Hibernate creates the tables automatically on the first run.

## Testing with Postman

Import `postman_collection.json` from the root of this repository. It contains every
endpoint and a folder of requests that deliberately break each business rule so the
error responses can be demonstrated.

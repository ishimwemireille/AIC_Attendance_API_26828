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

## The programme

The cohort moves through the courses one after another: DevOps, Web Technology,
Internet UI and UX, Mobile Programming, Artificial Intelligence, Internet of
Things. A `Course` carries its position in the programme, the dates it runs and
the trainer who leads it; a `Session` belongs to a course and must fall inside
its dates. Two courses may not overlap, because the cohort can only be in one
at a time.

Every active student is expected at every session of every course, so there is
no enrolment table: the roster is simply the active students.

## Running the whole system

Three pieces: PostgreSQL (installed locally), MongoDB and RabbitMQ (containers),
the Spring Boot API, and the React front end.

```bash
# 1. the two brokers / stores that are not installed natively
docker compose up -d          # MongoDB on 27017, RabbitMQ on 5672 (UI on 15672)

# 2. the API
mvn spring-boot:run           # http://localhost:8080

# 3. the front end, in a second terminal
cd frontend
npm install
npm run dev                   # http://localhost:5173
```

The front end proxies `/api` to the API, so the browser stays on one origin.
Point it somewhere else with `VITE_API_TARGET=http://localhost:9000 npm run dev`.

### Signing in

The usual way in is **Continue with your AUCA Google account**, which needs
Google credentials:

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
mvn spring-boot:run -Dspring-boot.run.profiles=dev,google
```

Google cannot redirect back to a laptop that is not publicly reachable, so the
`dev` profile also accepts HTTP Basic with the email and the single shared
password in `app.dev.password`. That path is development only — no per-user
password is stored by the system in either case.

Seeded accounts (`app.seed-demo-data=true`):

| Email | Role |
|---|---|
| `admin@auca.ac.rw` | ADMIN |
| `cohort.leader@auca.ac.rw` | COHORT_LEADER |
| `trainer.web@auca.ac.rw` | TRAINER |
| `trainer.db@auca.ac.rw` | TRAINER |

### A note for anyone on Spring Boot 4

The MongoDB property prefix changed from `spring.data.mongodb` to
`spring.mongodb`. The old name is ignored silently, so writes land in Mongo's
default `test` database instead of failing.

## Running the tests

```bash
mvn test
```

37 tests, in four groups:

| Group | Needs | Covers |
|---|---|---|
| `AttendanceServiceImplTest` | nothing | the attendance business rules, with mocks |
| `AuthorizationMatrixTest` | nothing | who may call which endpoint, as a web slice |
| `AttendanceEventPublisherTest` | nothing | that a broker outage cannot fail an attendance write |
| `DatabaseConstraintsTest` | PostgreSQL | that the UNIQUE constraints really refuse duplicates |

The constraint tests run against a real PostgreSQL database, `aicattendance_test`,
which is recreated on every run. Create it once:

```bash
createdb -h localhost -U postgres aicattendance_test
```

Connection details come from `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`,
defaulting to `localhost:5432`, `postgres` and `1234`. Override them if your
local password differs:

```bash
TEST_DB_PASSWORD=yourpassword mvn test
```

They deliberately do not use an embedded database. A constraint is a property of
the database engine, so checking it on a different one tests the wrong thing —
H2 demonstrated exactly that by rejecting a value against a generated enum check
that PostgreSQL accepts.

## Continuous integration

`.github/workflows/build.yml` runs on every push to `main` and on every pull
request. Two jobs: the API with its full test suite against a real PostgreSQL
service container, and the front end build. A badge for the result sits at the
top of this file once the first run completes.

## Importing the attendance register

The centre already keeps its register in Excel, so the system reads that file
rather than asking anyone to retype it. **Courses → Import register** on the
course the register belongs to; **Template** downloads an empty one.

The file is not a list of students, it is the register itself:

```
NAME | STUDENT ID | GENDER | SECTOR | Aug 16 | Aug 18 | ...
```

Each sheet is a teaching group, and every column after the fourth is a date on
which attendance was taken. One upload therefore brings in the roster, a
session for each date, and the attendance. Column headers that are not a date
(Excel's own `Column 6` filler) are reported as ignored rather than guessed at,
and both spellings of *Excuse* and *Excused* are accepted because the two
sheets disagree.

Imported attendance is taken as given. It was recorded on paper under the
centre's own practice, so the marking rules this system applies — a time in for
anyone present, a written reason for an absence — are not imposed on history
that predates it, and nothing is notified: those absences happened weeks ago.

Email and phone are optional, because the register carries neither. A student
without them simply receives no alert, and the notification log records that
the channel was skipped rather than pretending it was sent.

The fourth column is headed `SECTOR` in the centre's file but holds the
**thematic area** the student works in — Agriculture, Crime Prevention,
Education, Fintech or Health — so that is what it is called in the system.

A student has no year of study. The register does not record one and the
programme does not use one, so rather than store a number nobody set, the
field is gone.

## A note on the database name

The schema changed when courses were introduced, in a way `ddl-auto=update`
cannot migrate: sessions gained a non-null course. The application therefore
points at `aic_attendance`. If you have an older `aicattendance` or
`aicattendancedb` lying around from before, they are no longer used and can be
dropped.

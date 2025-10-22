# Ap_project — Product brief & quick demo

Short summary
Ap_project is a compact university ERP demo that showcases end-to-end student, instructor and admin workflows via a Java Swing client and a lightweight TCP server.

Why this repo exists
- Students: browse catalog, register/drop sections, view grades and download transcripts.
- Instructors: manage assigned sections, enter component scores, compute and export final grades.
- Administrators: create/manage users, courses and sections; perform DB backups and restores.

Top-level features (at a glance)
- Login / session management with account lockout and last-login tracking.
- Student flow: view course catalog → register for a section (capacity checks) → view timetable and grades → download transcript.
- Instructor flow: view assigned sections → open roster → record component scores → compute final grade → export/import grades (CSV).
- Admin flow: create students/instructors/courses/sections → toggle maintenance mode → DB backup/restore (gzipped SQL via safe BASE64 transfer).

Protocol (simple)
- The client sends single-line commands over TCP, e.g. `LOGIN:username:password`.
- The server responds with `SUCCESS:<payload>` or `ERROR:<message>`; files are transferred as `FILE_DOWNLOAD:<mime>:<name>:BASE64:<payload>`.

Quick start (build & run)
1. Build modules:

```cmd
cd erp-server
mvn -DskipTests package

cd ..\erp-client
mvn -DskipTests package
```

2. Start server: run `ServerMain` in `erp-server` or execute the jar in `erp-server/target`.
3. Start client: run the main/launcher in `erp-client` or the client JAR.

Where to look (concise map)

Notes for reviewers
 This is a student project/demonstration — it uses plain TCP (no TLS) and a simple protocol by design.

If you want me to add a short architecture diagram, minimal README badges, or a short demo script that runs the server and a headless client command, tell me which one and I'll add it.
Want extras?
------------
I can quickly add:
- A one-page ASCII architecture diagram.
- A one-minute demo script (automated starter + sample commands).
- A short non-technical readme badge and contact line for project attribution.

Tell me which extra(s) and I’ll add them.
# Ap_project (ERP) — Features & Developer README

This repository contains a small university ERP-style application implemented in Java (Maven multi-module). It has two primary modules:

- `erp-server` — the server-side services, DAOs, and TCP request router.
- `erp-client` — a Swing-based desktop client, APIs that speak the server protocol, and UI frames.

This README enumerates the features implemented across the server and client so you have a single reference when testing, debugging, or extending the system.

## Architecture Overview

- Language: Java 17
- Build: Maven (multi-module)
- Serialization: Gson (JSON) for domain objects.
- Transport: custom single-line TCP command protocol (plain text over socket). Each request is a single line command with colon-separated arguments. Responses are single-line strings prefixed with `SUCCESS:`, `ERROR:`, or `FILE_DOWNLOAD:` for binary/content transfers.
- Modules:
  - `erp-server` — contains `server` package (request router `ClientHandler`, `ServerMain`), `service` packages (business logic), `dao` packages (database access), and `domain` objects.
  - `erp-client` — contains `api` packages (client-side helpers wrapping the protocol), `ui` packages (Swing frames & handlers), and client `domain` objects.

## Protocol (high-level)

- Client sends commands as: `COMMAND:arg1:arg2:...`
- Server responses:
  - `SUCCESS:<payload>` — success and optional JSON payload.
  - `ERROR:<message>` — error condition.
  - `FILE_DOWNLOAD:<mime>:<filename>:BASE64:<base64payload>` — used for file transfers (CSV, HTML, gz archive) to avoid multi-line transport.

Example: `LOGIN:alice:password123` → `SUCCESS:{...user json...}` or `ERROR:Invalid username or password.`

## Authentication & Security

- Passwords are hashed with BCrypt (see `org.mindrot.jbcrypt.BCrypt` bundle in the server/client libs).
- Login flow includes lockout protection:
  - `failed_attempts` column tracked in DB.
  - On repeated failures a `locked_until` timestamp is set (current code sets lockout window when attempts >= threshold).
  - On successful login, `failed_attempts` is reset and `last_login` timestamp is updated.
- Change Password feature exists (`CHANGE_PASSWORD`) using secure hash update.

## Server: Key Features (Commands & Behavior)

The server router (`ClientHandler`) supports a rich set of commands. Major features include:

- Authentication
  - `LOGIN:username:password` — validates credentials, updates `last_login`, returns a `UserAuth` JSON.
  - `LOGOUT` — clears the per-connection authenticated user.
  - `CHANGE_PASSWORD:userId:old:new` — verifies old password then updates hash.

- Student features
  - `GET_GRADES:userId` — retrieve list of grades.
  - `GET_CATALOG` — retrieve course catalog.
  - `GET_TIMETABLE:userId` — fetch registered timetable.
  - `REGISTER:userId:sectionId` — register a student into a section (business rules enforced: capacity, duplicate registrations, deadlines, etc.).
  - `DROP_SECTION:userId:sectionId` — drop a registered course (enforced rules apply).
  - `DOWNLOAD_TRANSCRIPT:userId` — returns HTML transcript as `FILE_DOWNLOAD`.

- Instructor features
  - `GET_INSTRUCTOR_SECTIONS:instructorId` — list sections taught by instructor.
  - `GET_ROSTER:instructorId:sectionId` — fetch roster of a section.
  - `RECORD_SCORE:instructorId:enrollmentId:component:score` — record a component score.
  - `COMPUTE_FINAL_GRADE:instructorId:enrollmentId` — compute final grade based on component weights and record it.
  - `EXPORT_GRADES:instructorId:sectionId` — returns base64 CSV via `FILE_DOWNLOAD`.
  - `IMPORT_GRADES:instructorId:sectionId:BASE64:<payload>` — import CSV from base64 payload.

- Administrative features
  - `CREATE_STUDENT:...` / `CREATE_INSTRUCTOR:...` — create accounts (server may auto-assign ID if client supplied 0).
  - `CREATE_COURSE`, `CREATE_SECTION`, `CREATE_COURSE_SECTION` — create catalog entries and sections.
  - `GET_ALL_COURSES`, `GET_ALL_INSTRUCTORS`, `GET_ALL_STUDENTS` — admin list endpoints.
  - `REASSIGN_INSTRUCTOR` — change instructor assignment for sections.
  - `TOGGLE_MAINTENANCE:ON|OFF` and `CHECK_MAINTENANCE` — maintenance mode blocks many write operations.
  - `DB_BACKUP` / `DB_RESTORE` — create/restore gzipped SQL dumps (BASE64 transfer, audit log written).
  - `SET_DROP_DEADLINE` — configure drop-deadline enforcement.

- Enrollment rules & business logic
  - Enrollment status uses `Registered` vs `Dropped` semantics.
  - Capacity checks, duplicate registration prevention, and drop-deadline checks are enforced in the service/DAO layers.

## Server Implementation Notes

- DAOs separate SQL from services. Notable DAOs: `AuthDAO`, `CourseDAO`, `EnrollmentDAO`, `InstructorDAO`, `EnrollmentDAO`.
- `AuthDAO` now reads/writes `last_login`, `failed_attempts`, `locked_until`.
- `MysqldumpBackupService` implements DB backup/restore and writes audit entries to `db_backup_audit.log`.

## Client: Key Features

- Swing-based desktop client with the following UI components:
  - Login page that calls `AuthAPI.login()` and navigates to role-specific dashboard.
  - `AdminDashboardFrame`, `InstructorDashboardFrame`, and `StudentDashboardFrame` — main navigation hubs.
  - Preview frames: `CatalogPreviewFrame`, `GradesPreviewFrame`, `RosterPreviewFrame`, `Transcript` download support, etc.
- Client-side API helpers in `erp-client/src/main/java/edu/univ/erp/api/*` wrap the socket protocol and use Gson for JSON parsing:
  - `AuthAPI`, `StudentAPI`, `InstructorAPI`, `AdminAPI` — convenience methods that send commands and parse `SUCCESS` JSON payloads.
- File transfer handling: the client decodes `FILE_DOWNLOAD:...:BASE64:<payload>` responses and writes files locally.
- The client now displays `lastLogin` timestamp returned by the server in the dashboards.

## Data & Schema Notes

- `users_auth` table stores `user_id`, `username`, `password_hash`, `role`, `failed_attempts`, `locked_until`, and `last_login` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP in seed schema).
- Enrollment and course-related tables are accessed across DAOs and support capacity and status semantics.

## Build & Run

1. Build server and client (from repository root):

```cmd
cd erp-server
mvn -DskipTests package

cd ..\erp-client
mvn -DskipTests package
```

2. Run server (run `ServerMain` from `erp-server` module or execute the generated jar in `erp-server/target`).

3. Run client (run the launcher/main in `erp-client` or execute the generated client JAR).

## Example commands (quick smoke tests)

- Login: `LOGIN:alice:password`
- Fetch catalog: `GET_CATALOG`
- Register (student): `REGISTER:studentId:sectionId`

Expect `SUCCESS:` or `ERROR:` responses. File responses return `FILE_DOWNLOAD:...:BASE64:<payload>` which the client decodes.

## Troubleshooting & Notes

- The client UI previously could freeze if long-running network calls were performed on the EDT. The codebase contains network APIs that must be invoked off the EDT; see `ui/handlers` where blocking calls are wrapped for UI usage.
- Socket read timeouts are configured via system property `erp.socketReadTimeoutMs` (default 300000 ms). The server sets socket read timeouts for connections.

## Suggested Next Improvements (low-risk)

- Move client blocking network calls to `SwingWorker` or use an `ExecutorService` to avoid EDT freezes and provide progress/cancellation.
- Improve timestamp format: return timezone-aware ISO timestamps (Z / offset) from server and present localized friendly format on the client.
- Add a shared header component in the client to consistently display `lastLogin` instead of repeating the label across frames.
- Add unit/integration tests for the protocol and a small harness to simulate slow network responses.

## Where to look in the codebase (entry points)

- Server router & handlers: `erp-server/src/main/java/edu/univ/erp/server/ClientHandler.java`
- Authentication: `erp-server/src/main/java/edu/univ/erp/service/auth/AuthService.java` and `dao/auth/AuthDAO.java`
- Student logic: `erp-server/src/main/java/edu/univ/erp/service/student/StudentService.java`
- Instructor logic: `erp-server/src/main/java/edu/univ/erp/service/instructor/InstructorService.java`
- Admin & backup: `erp-server/src/main/java/edu/univ/erp/service/admin/MysqldumpBackupService.java` and `service/admin/AdminService.java`
- Client UI: `erp-client/src/main/java/edu/univ/erp/ui` and handlers in `ui/handlers`.
- Client APIs: `erp-client/src/main/java/edu/univ/erp/api`.

---


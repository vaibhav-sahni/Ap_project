# Ap_project — Product brief & quick demo

This project is a desktop application written using Java Swing that helps universities manage courses, class sections, enrollments and grades.

It uses a MySQL DB to store data, and features a client and server application connected via websockets to support multiple instances of users using the app at once.

There are 3 kinds of users in the app

# Demo Video
[![Demo video](https://img.youtube.com/vi/Re_hPdtRJrQ/0.jpg)](https://youtu.be/Re_hPdtRJrQ)



# User types and supported actions
1. Students: 
- Browse Course Catalog and register for a course
- View Registered Courses
- View Timetable
- View Course Grades
- Get Notifications
- Change Password

2. Instructors: 
- View Assigned Sections
- View Student roster and component-wise grades for an assigned section
- View Section Stats
- Save Section Data to DB
- Export/Import grades using .csv format
- Perform final letter grading for students in a section
- Get Notifications
- Change Password

2. Administrator:
- Toggle Maintenance Mode
- Set Course Drop Deadline
- Download/Restore DB Backup (uses gzip format.)
- Send Notifications to users
- Create Courses/Sections/Students/Instructors
- Reassign Instructors to Sections

# Client Server Communication
Protocol:
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

# Ap_project (ERP) — Features & Developer README

This repository contains a small university ERP-style application implemented in Java (Maven multi-module). It has two primary modules:

- `erp-server` — the server-side services, DAOs, and TCP request router.
- `erp-client` — a Swing-based desktop client, APIs that speak the server protocol, and UI frames.

This README enumerates the features implemented across the server and client so you have a single reference when testing, debugging, or extending the system.

## Architecture Overview

- Language: Java 24
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


## SMTP / Admin email setup (password-reset notifications)

The server sends password-reset notifications to a configured administrator email address. These values are read from the `settings` table in the ERP database. You can configure them directly in the DB or via the admin-only server command `SET_ADMIN_EMAIL:email` (requires an authenticated admin).

Required settings (keys stored in `settings.setting_key`):

- `ADMIN_EMAIL` — the address that receives password-reset notifications.
- `SMTP_HOST` — SMTP server hostname (required for sending email).
- `SMTP_PORT` — SMTP server port (common: `587`).
- `SMTP_USER` / `SMTP_PASS` — credentials if your SMTP server requires authentication.
- `SMTP_FROM` — optional From address used for outbound mail.
- `SMTP_STARTTLS` — set to `true` to enable STARTTLS (common when using port 587).

Quick SQL (MySQL) — upsert the settings (replace with your values):

```sql
INSERT INTO settings (setting_key, setting_value) VALUES ('ADMIN_EMAIL','admin@example.com')
  ON DUPLICATE KEY UPDATE setting_value='admin@example.com';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_HOST','smtp.example.com')
  ON DUPLICATE KEY UPDATE setting_value='smtp.example.com';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_PORT','587')
  ON DUPLICATE KEY UPDATE setting_value='587';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_USER','smtp_user')
  ON DUPLICATE KEY UPDATE setting_value='smtp_user';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_PASS','smtp_pass')
  ON DUPLICATE KEY UPDATE setting_value='smtp_pass';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_FROM','noreply@example.com')
  ON DUPLICATE KEY UPDATE setting_value='noreply@example.com';

INSERT INTO settings (setting_key, setting_value) VALUES ('SMTP_STARTTLS','true')
  ON DUPLICATE KEY UPDATE setting_value='true';
```

Run from Windows `cmd.exe` with the MySQL CLI (example using defaults from `DBConnector`):

```cmd
mysql -u erp_user -p -h localhost erp_db -e "INSERT INTO settings (setting_key,setting_value) VALUES ('ADMIN_EMAIL','admin@example.com') ON DUPLICATE KEY UPDATE setting_value='admin@example.com';"
```

Or combine multiple inserts in one `-e` string (be careful with quoting). After inserting, verify:

```sql
SELECT setting_value FROM settings WHERE setting_key='ADMIN_EMAIL';
```

Provider-specific notes
- Mailtrap (development/testing): use Mailtrap SMTP credentials. Mailtrap captures emails in a web UI so you don't send real mail.
  - SMTP_HOST: `smtp.mailtrap.io`, SMTP_PORT: `587`, SMTP_USER/PASS: from Mailtrap inbox, SMTP_STARTTLS: `true`.

- Gmail (production/testing with App Passwords): use an App Password (recommended) if your account has 2FA.
  - SMTP_HOST: `smtp.gmail.com`, SMTP_PORT: `587`, SMTP_USER: your Gmail address, SMTP_PASS: App password, SMTP_STARTTLS: `true`.
  - Do not enable "less secure apps" in production; prefer App Passwords.

- Office365 / Outlook:
  - SMTP_HOST: `smtp.office365.com`, SMTP_PORT: `587`, SMTP_STARTTLS: `true`.

Testing the flow
- Start the server with Maven so dependencies are on the classpath (this includes the Jakarta Mail library we added):

```cmd
mvn -f "c:\Users\sahni\Documents\GitHub\Ap_project\erp-server" exec:java
```



## Where to look in the codebase (entry points)

- Server router & handlers: `erp-server/src/main/java/edu/univ/erp/server/ClientHandler.java`
- Authentication: `erp-server/src/main/java/edu/univ/erp/service/auth/AuthService.java` and `dao/auth/AuthDAO.java`
- Student logic: `erp-server/src/main/java/edu/univ/erp/service/student/StudentService.java`
- Instructor logic: `erp-server/src/main/java/edu/univ/erp/service/instructor/InstructorService.java`
- Admin & backup: `erp-server/src/main/java/edu/univ/erp/service/admin/MysqldumpBackupService.java` and `service/admin/AdminService.java`
- Client UI: `erp-client/src/main/java/edu/univ/erp/ui` and handlers in `ui/handlers`.
- Client APIs: `erp-client/src/main/java/edu/univ/erp/api`.

---


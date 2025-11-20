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

# Ap_project — University ERP (Merged Documentation)

This repository contains a lightweight university ERP-style application implemented in Java (Maven multi-module). The application consists of a server (`erp-server`) and a Swing desktop client (`erp-client`). This README merges the key developer documentation, architecture notes, usage instructions, and integration contracts into a single reference.

Table of Contents

- Overview
- Quick Start (build & run)
- Architecture & Protocol
- Features (by user role)
- Client details (UI & APIs)
- Dashboard `dashboardcmds` contract (integration guide)
- MVC Mapping & Where-to-find code
- Data & schema notes
- Maintenance Mode & Notifications
- SMTP / Email setup
- Testing, Troubleshooting & Notes
- Contributing
- Documentation Index

---

## Overview

- Language: Java (JDK 17+ / tested with Java 17+ and modern toolchain)
- Build: Maven multi-module
- Modules:
  - `erp-server` — server-side services, DAOs, request router and utilities
  - `erp-client` — Swing client, client-side API adapters, UI components

Purpose: manage courses, sections, enrollments, grades and basic admin tasks for a small university using a desktop client and a TCP server.

## Quick Start — Build & Run

From repository root:

```powershell
cd "c:\ROAD TO SDE\iiitd\third sem\AP project\Ap_project\erp-server"
mvn -DskipTests package

cd "..\erp-client"
mvn -DskipTests package
```

Run server (option A: from IDE) — run `ServerMain` in `erp-server`.

Run server (option B: packaged JAR):

```powershell
java -jar erp-server\target\erp-server-1.0-SNAPSHOT.jar
```

Run client (from IDE) — run the `application.Application` in `erp-client` (or the appropriate launcher class for student/instructor/admin).

Run client (from packaged JAR):

```powershell
java -jar erp-client\target\erp-client-1.0-SNAPSHOT.jar
```

Notes: the server expects a MySQL database configured (see `erp-server/src/main/resources` for DB configuration). You may need to adjust DB credentials or run the seed SQL found under `data/seeds/dummy_seed.sql`.

## Architecture & Protocol

- Transport: simple single-line TCP command protocol. Each client request is a single line using colon-separated tokens.
- Response types:
  - `SUCCESS:<payload>` — success with optional JSON payload
  - `ERROR:<message>` — error
  - `FILE_DOWNLOAD:<mime>:<filename>:BASE64:<payload>` — file payload for downloads

Example request/response:

```
LOGIN:alice:password123
=> SUCCESS:{"userId":123,"role":"Student",...}
```

Security:

- Passwords hashed with BCrypt.
- Lockout tracking (`failed_attempts`, `locked_until`) is implemented in the DB and enforced on login.

## Features (by user role)

Student

- Browse course catalog, register/drop courses, view timetable, view grades, download transcript, notifications, change password.

Instructor

- View assigned sections, see roster and component-wise grades, record component scores, export/import grades (CSV), compute final grades, save section data, receive notifications, change password.

Administrator

- Toggle maintenance mode (global ON/OFF), set drop deadline, create/update courses/sections/users, DB backup/restore (gzipped), send notifications, other admin operations.

## Client (erp-client) — Key details

- UI: Java Swing with FlatLaf themes (light/dark). Dashboards for Student, Instructor and Admin.
- Client APIs: located in `erp-client/src/main/java/edu/univ/erp/api/` (e.g., `AuthAPI`, `StudentAPI`, `InstructorAPI`, `AdminAPI`) — wrappers around the socket protocol, returning domain objects via Gson.
- ClientContext: centralized user/context holder for current session.
- UI navigation: PanelSlider / FormManager (student & instructor) drives in-app navigation for forms.

File transfers: client decodes `FILE_DOWNLOAD` payloads and writes files locally (transcript, CSV exports, DB backup files when admin downloads them).

## Dashboard Integration: `dashboardcmds` (contract)

This project includes a small integration contract for the dashboard detailed view under `erp-client/src/main/java/edu/univ/erp/ui/*/forms/dashboardcmds/README.md`.

Summary:

- Entry points: prefer an event-bus to post `OpenDashboardEvent` with `{ gaugeId, metadata }`.
- Backend endpoints needed:
  - `GET /api/dashboard/summary` — top-level gauge data
  - `GET /api/dashboard/gauges/{gaugeId}` — detailed gauge payload
  - `GET /api/dashboard/notifications?limit=3`
  - `GET /api/dashboard/deadlines?from=...&to=...`
  - WebSocket topic `/topic/dashboard/updates` (push updates)

Frontend notes:

- Always fetch data off the EDT and render on EDT when ready.
- Provide mock JSON under `erp-client/data/mock/dashboard/` for frontend dev (recommended).

## MVC Mapping & Where to Look

- Views (Swing forms): `erp-client/src/main/java/edu/univ/erp/ui/*/forms/*` (DashboardForm, RegisterCoursesForm, MyGradesForm, etc.)
- Client controllers / actions: `erp-client/src/main/java/edu/univ/erp/ui/actions/*` (StudentActions, InstructorActions, AdminActions)
- Client transport adapters: `erp-client/src/main/java/edu/univ/erp/api/*` and `ClientRequest` for socket I/O
- Server router & handlers: `erp-server/src/main/java/edu/univ/erp/server/ClientHandler.java`
- Server services: `erp-server/src/main/java/edu/univ/erp/service/*`
- DAOs: `erp-server/src/main/java/edu/univ/erp/dao/*`

Table models: many forms embed `AbstractTableModel` implementations inside the form files (e.g., `GradesTableModel` inside `MyGradesForm`). These table models use domain objects from `edu.univ.erp.domain`.

## Data & Schema Notes

- `users_auth` table contains `user_id`, `username`, `password_hash`, `role`, `failed_attempts`, `locked_until`, `last_login`.
- `settings` table stores configuration keys (e.g., `MAINTENANCE_MODE`, `ADMIN_EMAIL`, SMTP settings).

## Maintenance Mode & Notifications

- Admins can toggle maintenance mode via `TOGGLE_MAINTENANCE:ON|OFF` or `CHECK_MAINTENANCE` command.
- Client includes a `MaintenanceModeManager` that polls server (`CHECK_MAINTENANCE`) and shows a toast notification in dashboards when maintenance is active. Notifications reappear on window focus and form switches (cooldown enforced to avoid spam).

If you need the maintenance notification to behave differently (no reappearance after manual close, or only show once per session), see `erp-client/src/main/java/edu/univ/erp/ui/components/MaintenanceModeManager.java` and `ToastNotification.java`.

## SMTP / Email (Password Reset & Admin notifications)

Settings keys (in `settings` table):

- `ADMIN_EMAIL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`, `SMTP_STARTTLS`

Example SQL upsert (MySQL):

```sql
INSERT INTO settings (setting_key, setting_value) VALUES ('ADMIN_EMAIL','admin@example.com')
  ON DUPLICATE KEY UPDATE setting_value='admin@example.com';
```

## Testing, Troubleshooting & Notes

- Run the server and client from IDE for faster debugging. Use breakpoints in `ClientHandler` and `ClientRequest` to inspect protocol exchanges.
- If the client UI blocks, check for network calls executed on the EDT. Wrap blocking calls inside `SwingWorker` or `ExecutorService` tasks.
- Socket timeouts: configure `erp.socketReadTimeoutMs` system property as needed.



## Appendix:

- Build server: `mvn -f erp-server -DskipTests package`
- Run server (exec): `mvn -f erp-server exec:java`
- Build client: `mvn -f erp-client -DskipTests package`
- Run client: `mvn -f erp-client exec:java`


## Documentation Index

- docs/README.md — master index
- docs/ARCHITECTURE.md — architecture overview
- docs/SERVER_GUIDE.md — server request routing, services, DAOs
- docs/CLIENT_GUIDE.md — client UI, APIs, navigation
- docs/PROTOCOL.md — TCP command protocol reference
- docs/SETUP_RUNBOOK.md — prerequisites, DB, build/run (PowerShell)
- docs/DB_SCHEMA.md — tables and schema notes
- docs/SECURITY.md — security practices
- docs/MAINTENANCE_MODE.md — maintenance behavior & customization
- docs/TESTING_TROUBLESHOOTING.md — tests and common issues
- docs/CODE_STYLE.md — code style guidelines
- docs/CONTRIBUTING.md — contribution workflow

```cmd

```

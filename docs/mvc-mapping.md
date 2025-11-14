
**MVC Mapping — Dashboards & Forms**

Overview

+ This document maps how the project's student dashboards and forms implement an MVC-like separation across the client and server.
+ It lists file-level responsibilities, where table models live and which domain classes they use, an end-to-end example, protocol notes, and recommendations.

Quick mapping (files & roles
)

+ **View (UI / Swing forms):**
	+ `erp-client/src/main/java/edu/univ/erp/ui/studentdashboard/forms/*`
	+ Examples: `DashboardForm.java`, `RegisterCoursesForm.java`, `MyGradesForm.java`
+ **Client Controllers / Actions:**
	+ `erp-client/src/main/java/edu/univ/erp/ui/actions/*`
	+ Examples: `StudentActions.java`, `AdminActions.java`, `InstructorActions.java`
+ **Client API Adapter & Transport:**
	+ `erp-client/src/main/java/edu/univ/erp/api/*`
	+ Examples: `StudentAPI.java`, `NotificationAPI.java`, `ClientRequest.java` (socket layer)
+ **Client Context & Navigation:**
	+ `erp-client/src/main/java/edu/univ/erp/ClientContext.java`
	+ `erp-client/src/main/java/edu/univ/erp/ui/studentdashboard/menu/FormManager.java`
+ **Server Router / Controller:**
	+ `erp-server/src/main/java/edu/univ/erp/server/ClientHandler.java`
+ **Server Service (business logic):**
	+ `erp-server/src/main/java/edu/univ/erp/service/*`
	+ Examples: `StudentService.java`, `AdminService.java`
+ **Server DAOs / Domain models:**
	+ `erp-server/src/main/java/edu/univ/erp/dao/*`
	+ `erp-server/src/main/java/edu/univ/erp/domain/*`

TableModels & domain usage

+ Table model classes (the data models used by `JTable`) are implemented as private inner classes inside the form files. For example:
	+ `RegisterCoursesForm.java` contains `CourseTableModel` (extends `AbstractTableModel`) and custom renderers/editors (e.g., `CapacityHealthBarRenderer`, `ActionsCellEditor`).
	+ `MyGradesForm.java` contains `GradesTableModel` and `GradeRenderer`.
	+ `MyCoursesForm.java` and instructor/admin forms follow the same pattern.
+ Although the TableModel classes live on the client, they typically store and present objects from the server **domain** package — `edu.univ.erp.domain` — deserialized by the client API adapters. Typical domain types used by table models include:
	+ `edu.univ.erp.domain.CourseCatalog`
	+ `edu.univ.erp.domain.Grade`
	+ `edu.univ.erp.domain.EnrollmentRecord`

End-to-end example: Registering for a course

1. View: User clicks "Register" in `RegisterCoursesForm` (client view).
2. Action: `RegisterCoursesForm` calls `StudentActions.registerCourse(userId, sectionId)` (client controller wrapper).
3. API: `StudentActions` calls `StudentAPI.registerCourse(...)`. `StudentAPI` builds protocol request and calls `ClientRequest.send(request)`.
4. Transport: `ClientRequest` writes the request to the TCP socket (persistent or new socket depending on session).
5. Server Router: `ServerMain` accepts the socket; `ClientHandler` reads the request line and routes `REGISTER` to `handleRegisterCourse(...)`.
6. Service: `ClientHandler` calls `StudentService.registerCourse(...)` which enforces business rules (capacity, scheduling conflicts, authorization checks).
7. DAO: `StudentService` uses `EnrollmentDAO`/`CourseDAO` to apply changes; DAOs operate on server-side domain models and persist to the database.
8. Response: `ClientHandler` returns a single-line protocol response such as `SUCCESS:Successfully registered` or `ERROR:Section is full` (or a structured `FILE_DOWNLOAD:` payload for files).
9. Client: `ClientRequest` receives the raw response, `StudentAPI` parses it into domain objects or throws an exception; `StudentActions` reports the result back to the view which updates UI and/or shows messages.

Protocol notes (tokens you will see)

+ `SUCCESS:<payload>` — success with optional payload (JSON, plain string, or base64-encoded file payload in the `FILE_DOWNLOAD` form).
+ `ERROR:<message>` — server-side error or business rule violation.
+ `FILE_DOWNLOAD:<content_type>:<filename>:<payload>` — used for file responses; the client parses and handles accordingly (base64 or raw content depending on implementation).

Mermaid diagram

+ See `docs/mvc-mapping.mmd` for a visual diagram of the flow. It now includes file paths, client table models note, and server domain model nodes.

Assessment & recommendations

+ Strengths:
	+ Clear separation: views handle presentation, server services encapsulate business rules, DAOs encapsulate persistence.
	+ Client API adapters centralize protocol handling and parsing.
+ Improvements to consider:
	+ Replace ad-hoc colon-delimited protocol with a line-delimited JSON envelope to avoid fragile string escaping and simplify payloads.
	+ Add typed error codes or a small JSON error envelope (e.g., `{code: "NOT_AUTH", message: "..."}`) so the client can reliably react to different error conditions.
	+ Extract frequently-reused UI table helpers (`ModernTable`, `LeftPaddedCellRenderer`) into a shared UI util package to reduce duplication.
	+ Add interfaces for action classes and dependency-inject `StudentAPI` to enable easier unit testing of UI controllers.

Files added/updated

+ `docs/mvc-mapping.mmd` — Mermaid diagram (now includes file paths and domain model notes).
+ `docs/mvc-mapping.md` — this file (updated to include table model details and protocol notes).

Next steps

+ I can (pick one):
	+ Add a Legend to the Mermaid diagram showing protocol tokens and examples.
	+ Extract `ModernTable` into `erp-client/src/main/java/edu/univ/erp/ui/components/ModernTable.java` and update usages.
	+ Create a short PR with these docs and recommended TODOs.

Which would you like me to do next?




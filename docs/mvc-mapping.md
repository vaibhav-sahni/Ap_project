**MVC Mapping — Dashboards & Forms**

**Overview**

- This document maps the main MVC relationships for the student dashboards and associated forms in the project.
- It highlights file-level responsibilities, an example end-to-end flow, and recommendations.

**Quick mapping (files & roles)**n
- **View (UI / Swing forms)**: `erp-client/src/main/java/edu/univ/erp/ui/studentdashboard/forms/*` — e.g., `DashboardForm.java`, `RegisterCoursesForm.java`
- **Client Controllers / Actions**: `erp-client/src/main/java/edu/univ/erp/ui/actions/*` — e.g., `StudentActions.java` (thin controller wrappers)
- **Client API Adapter**: `erp-client/src/main/java/edu/univ/erp/api/*` — e.g., `StudentAPI.java`, `ClientRequest.java` (socket transport)
- **Client Context / Navigation**: `ClientContext.java`, `FormManager.java`, `Application.java`
- **Server Router / Controller**: `erp-server/src/main/java/edu/univ/erp/server/ClientHandler.java` (text-command router)
- **Server Service (business logic)**: `erp-server/src/main/java/edu/univ/erp/service/*` — e.g., `StudentService.java`
- **Server DAOs / Domain**: `erp-server/src/main/java/edu/univ/erp/dao/*` and `erp-server/src/main/java/edu/univ/erp/domain/*`

**End-to-end example: Registering for a course**

1. View: `RegisterCoursesForm` detects user action and calls `StudentActions.registerCourse(userId, sectionId)`.
2. Controller: `StudentActions.registerCourse` delegates to `StudentAPI.registerCourse(userId, sectionId)`.
3. Client API: `StudentAPI` builds the text command `"REGISTER:<userId>:<sectionId>"` and calls `ClientRequest.send(request)`.
4. Transport: `ClientRequest` sends the request over TCP (socket) to the server.
5. Server Router: `ServerMain` accepts the socket; `ClientHandler` reads the line and routes to `handleRegisterCourse(...)`.
6. Service: `handleRegisterCourse` calls `StudentService.registerCourse(...)` which enforces business rules (capacity, conflicts, authorization checks).
7. DAO: `StudentService` uses `EnrollmentDAO` (and other DAOs) to persist the registration.
8. Response: `ClientHandler` returns `SUCCESS:<message>` or `ERROR:<message>` over the socket back to the client.
9. Client: `ClientRequest` returns the raw response; `StudentAPI` parses and returns to `StudentActions`, which returns to the View.

**Mermaid diagram**

See `docs/mvc-mapping.mmd` for a diagram you can open in VS Code (Mermaid preview).

**Assessment & Suggestions**

- The project uses a lightweight, practical MVC separation. Views keep to presentation, services contain business logic, and DAOs manage persistence.
- Consider migrating from the ad-hoc colon-delimited protocol to a line-delimited JSON protocol to simplify payloads and escaping.
- Add typed error codes or a small error envelope to make client-side error handling less brittle.
- If you want to unit-test UI logic more easily, add interfaces for action classes (e.g., `IStudentActions`) and dependency-inject `StudentAPI`.

**Files changed/added**

- `docs/mvc-mapping.mmd` — Mermaid diagram
- `docs/mvc-mapping.md` — This file

**Next steps**

- I can also open a VS Code mermaid preview or commit these changes into a branch and create a PR. Which would you prefer?

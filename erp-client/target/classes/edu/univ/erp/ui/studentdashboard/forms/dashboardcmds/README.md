# dashboardcmds — Dashboard Commands Integration Guide

## Purpose

This README documents the contract between the dashboard UI (gauge charts and other components) and the `dashboardcmds` component that provides the detailed dashboard command view.

Goals:

- Describe how gauge charts can redirect/open `dashboardcmds` when other components are ready.
- List all backend data the dashboard expects and the recommended REST/WebSocket contract.
- Provide backend implementation guidance (Spring Boot examples), DTO shapes, mock JSON, and integration snippets for the Swing client.

Location

- Place this document under:
  `erp-client/src/main/java/edu/univ/erp/ui/studentdashboard/forms/dashboardcmds/README.md`

## High-level contract

- The dashboardcmds view exposes an entry point that the UI can call to show detailed dashboard commands. The entry method should accept a lightweight context object:
  - Example: `DashboardCmdsRouter.open(Context ctx)`
  - `Context` fields: `String source`, `String gaugeId` (optional), `Map<String,Object> metadata`
- Navigation event names (recommended):
  - `DashboardReady` — broadcast by container when all other components (auth, data services) have initialized.
  - `OpenDashboardCmds` — triggered by gauge click or other explicit action. Payload includes `gaugeId` and `targetUserId`.

Contract (2–4 bullets):

- Inputs: navigation payload (gaugeId, optional query date range), user auth token.
- Outputs: UI transitions to `dashboardcmds` panel; requests to backend for summary/details; event callbacks on success/error.
- Error modes: network failure (retryable), 401/403 (re-auth), 4xx validation (show user message).

## Frontend wiring (Swing)

Suggested lightweight API for wiring gauges to dashboardcmds.

1. Publish/subscribe (recommended): use a simple event bus singleton inside `erp-client`.

Example EventBus usage (conceptual):

```java
// payload object
public class OpenDashboardEvent {
    public final String gaugeId;
    public final Map<String,Object> metadata;
    public OpenDashboardEvent(String gaugeId, Map<String,Object> metadata) {
        this.gaugeId = gaugeId;
        this.metadata = metadata;
    }
}

// When gauge clicked
glassCard.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        EventBus.getInstance().post(new OpenDashboardEvent("cgpa", Map.of("from", "overview")));
    }
});

// In dashboardcmds initializer
EventBus.getInstance().register(OpenDashboardEvent.class, ev -> {
    SwingUtilities.invokeLater(() -> DashboardCmdsRouter.open(new Context("gauge", ev.gaugeId, ev.metadata)));
});
```

2. Direct call (if modules are tightly coupled): expose a static/DI router:

```java
// simple router
public class DashboardCmdsRouter {
    public static void open(Context ctx) {
        // create or show the panel, pass context
        DashboardCmdsPanel panel = new DashboardCmdsPanel(ctx);
        mainFrame.setContentPane(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }
}
```

Notes:

- Prefer not to block the EDT while fetching data — always fetch asynchronously and show a spinner.
- Wait for `DashboardReady` broadcast before navigating if you depend on other components (auth, settings, cache).

## Data requirements (what the dashboard needs from backend)

Minimal endpoints and required fields. All responses are JSON.

1. GET /api/dashboard/summary

- Purpose: fetch top-level gauges (CGPA, Credits, Courses, etc.) and small cards data.
- Response shape:

```json
{
  "userId": "student-123",
  "cgpa": 7.5,
  "credits": 112,
  "coursesCount": 5,
  "currentSemester": "Fall 2025",
  "gpaHistory": [
    { "semester": "Fall2024", "gpa": 7.2 },
    { "semester": "Spring2025", "gpa": 7.4 }
  ]
}
```

2. GET /api/dashboard/gauges/{gaugeId}

- Purpose: detailed data for a specific gauge (used when redirecting from a gauge click).
- Example response for `cgpa`:

```json
{
  "gaugeId": "cgpa",
  "title": "CGPA",
  "value": 7.5,
  "max": 10.0,
  "breakdown": {
    "courses": [{ "courseId": "CS101", "grade": 9.0 }],
    "weightedComponents": [{ "name": "Theory", "weight": 0.6, "value": 7.8 }]
  },
  "chartPoints": [{ "label": "Sem1", "value": 6.9 }]
}
```

3. GET /api/dashboard/notifications?limit=3

- Recent notifications relevant to the dashboard.
- Response: array of { id, title, message, timestamp, type, link }

4. GET /api/dashboard/deadlines?from=YYYY-MM-DD&to=YYYY-MM-DD

- List of deadlines with courseId, title, date, importance

5. GET /api/dashboard/courses?active=true

- List of active courses with states, instructors, and small enrollment info

6. WS /topic/dashboard/updates (or SSE)

- Push updates for notifications, deadlines, or gauge changes in real-time
- Message example: `{ "type":"notification", "payload":{ ... } }`

7. Authentication

- All protected endpoints must accept `Authorization: Bearer <jwt>` header.

## Backend design notes (Spring Boot examples)

- Use REST controllers with DTOs. Keep DTOs small and focused for each endpoint.
- Service layer: `DashboardService` returns DTOs for controller to serve.
- Security: validate JWT using existing `erp-server/security` module.

Sample controller (conceptual):

```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService svc;
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary(@AuthenticationPrincipal UserPrincipal user) {
        DashboardSummaryDto dto = svc.getSummary(user.getId());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/gauges/{gaugeId}")
    public ResponseEntity<GaugeDetailDto> gaugeDetail(@PathVariable String gaugeId, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(svc.getGaugeDetail(user.getId(), gaugeId));
    }
}
```

Example DTO (Java):

```java
public class DashboardSummaryDto {
    public String userId;
    public double cgpa;
    public int credits;
    public int coursesCount;
    public String currentSemester;
    public List<GpaPoint> gpaHistory;
}
```

WebSocket (STOMP) quick note:

- Configure endpoint `/ws` and app destination prefix `/app`.
- Broadcast updates on `/topic/dashboard/notifications` and `/topic/dashboard/updates`.
- Clients subscribe and update the UI live.

## Error handling and retries

- Use standard HTTP status codes (200, 400, 401, 403, 404, 500).
- Client-side retry policy for network/timeouts (exponential backoff, cap to 3 retries).
- On 401, trigger re-auth flow.
- Display meaningful messages for 4xx validation errors.

## Mocking & quick-start for frontend developers

- Provide mock JSON files (under `data/mock/dashboard/`) that match the DTOs above.
- Use WireMock or a simple embedded HTTP server during front-end dev.

cURL examples:

```bash
# Fetch summary
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/dashboard/summary"

# Fetch gauge detail
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/dashboard/gauges/cgpa"
```

## Integration snippets (frontend)

- When the gauge is clicked, send user to `DashboardCmdsRouter.open(ctx)` and pass `gaugeId`.
- While the detail loads, show a spinner in the `dashboardcmds` panel and asynchronously call the REST endpoint.

Example asynchronous loader (Swing + executor):

```java
ExecutorService ex = Executors.newSingleThreadExecutor();
ex.submit(() -> {
    try {
        GaugeDetailDto detail = BackendApi.fetchGaugeDetail(token, gaugeId);
        SwingUtilities.invokeLater(() -> panel.render(detail));
    } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> panel.showError("Failed to load gauge details"));
    }
});
```

## Testing and validation

- Backend: unit tests for `DashboardService`, integration tests with `MockMvc`, contract tests for DTO shapes.
- Frontend: WireMock to simulate API responses; unit tests for parsing/mapping; manual UI smoke tests.

## Versioning

- Version your endpoints (e.g., `/api/v1/dashboard/...`) if the contract may change.

## Checklist before production

- [ ] Secure endpoints and verify auth flows
- [ ] Rate-limiting for expensive endpoints
- [ ] Add server-side caching for summary endpoints
- [ ] Create Postman collection and share with frontend team

---

If you want, I can also:

- Add a sample Spring Boot controller + DTO files under `erp-server`.
- Add mock JSON files under `erp-client/data/mock/dashboard/` and a small `BackendApi` helper for the Swing client.

Tell me which of these extras you want next and I'll add them.

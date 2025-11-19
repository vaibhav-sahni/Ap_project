# Architecture Overview

This project is a Maven multi-module Java application with a server (`erp-server`) and a Swing desktop client (`erp-client`).

## High-Level (C4)

- System: University ERP (course, enrollment, grade mgmt)
- Containers: Desktop Client (Swing) and TCP Server (Java)
- Components:
  - Server: Request Router, Services (Auth/Student/Instructor/Admin), DAOs, Domain Models
  - Client: API Adapters, UI Forms, Navigation, Theming

## Modules

- erp-server
  - `edu.univ.erp.server` — socket server, client handler, routing
  - `edu.univ.erp.service` — business logic
  - `edu.univ.erp.dao` — persistence/SQL
  - `edu.univ.erp.domain` — DTOs/domain
  - `edu.univ.erp.util` — helpers (e.g., CSV, backups)
- erp-client
  - `edu.univ.erp.api` — socket client helpers
  - `edu.univ.erp.ui` — Swing UI (forms, frames, components)
  - `edu.univ.erp.domain` — client-side models

## Threading Model

- Server: one thread per connection (blocking IO); services/DAOs run on that thread.
- Client: network work off-EDT (e.g., SwingWorker or executors), UI updates on EDT.

## Configuration

- DB connection and server port configured in server resources.
- Client points to server host/port; FlatLaf theme selection (light/dark).

## Error Handling & Logging

- Server: return `ERROR:<message>` for client-visible errors; log stack traces.
- Client: show dialogs/toasts; log unexpected errors to console/log file.

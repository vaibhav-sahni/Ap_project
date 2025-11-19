# Client Guide

## App Entry

- Main class: `application.Application` (launches login and routes to dashboards by role).
- Themes: FlatLaf Light/Dark (auto or manual selection).

## Using APIs

```java
AuthAPI auth = new AuthAPI(clientRequest);
UserAuth ua = auth.login(username, password);
if (ua.getRole() == Role.STUDENT) openStudentDashboard(ua);
```

## Form Navigation (Student/Instructor)

```java
public void showForm(SimpleForm next) {
  if (current != null) history.push(current);
  current = next;
  panelSlider.slideTo(next.getComponent());
  MaintenanceModeManager.getInstance().onFormSwitch();
}
```

## Toast Notifications (Maintenance)

```java
// Show red warning toast at top-center of current window
ToastNotification.showMaintenanceNotification(window, /*isMaintenance*/ true);
```

## File Downloads

```java
String resp = clientRequest.send("EXPORT_GRADES:"+instId+":"+sectionId);
if (resp.startsWith("FILE_DOWNLOAD:")) {
  FilePayload p = FilePayload.parse(resp);
  Files.write(Path.of(p.filename()), p.decode());
}
```

## Threading

- Perform socket IO off-EDT (SwingWorker or ExecutorService).
- Wrap UI updates in `SwingUtilities.invokeLater`.

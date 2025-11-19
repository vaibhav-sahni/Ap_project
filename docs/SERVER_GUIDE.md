# Server Guide

## Request Handling Flow

```java
class ClientHandler implements Runnable {
  @Override public void run() {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
      for (String line; (line = in.readLine()) != null; ) {
        String[] parts = line.split(":");
        String cmd = parts[0];
        String resp = switch (cmd) {
          case "LOGIN" -> authService.login(parts[1], parts[2]);
          case "GET_CATALOG" -> courseService.getCatalog();
          // ... other commands
          default -> "ERROR:Unknown command";
        };
        out.write(resp);
        out.write('\n');
        out.flush();
      }
    } catch (IOException ex) {
      logger.error("Client IO error", ex);
    }
  }
}
```

## Service Layer Example

```java
class AuthService {
  private final AuthDAO dao;
  String login(String username, String password) {
    var user = dao.findByUsername(username);
    if (user == null) return "ERROR:Invalid username or password";
    if (!BCrypt.checkpw(password, user.getPasswordHash())) {
      dao.incrementFailedAttempts(user.getId());
      return "ERROR:Invalid username or password";
    }
    dao.resetFailedAttempts(user.getId());
    user.setLastLogin(Instant.now());
    dao.updateLastLogin(user);
    return "SUCCESS:" + gson.toJson(UserAuth.from(user));
  }
}
```

## DAO Pattern

```java
class AuthDAO {
  Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users_auth WHERE username=?";
    try (var ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (var rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    }
  }
}
```

## Files & Utilities

- Backups: service writes gz files and returns via `FILE_DOWNLOAD`.
- CSV: export/import for grades with validation in service layer.

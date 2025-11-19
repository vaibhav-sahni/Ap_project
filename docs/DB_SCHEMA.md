# Database Schema

## Key Tables

### users_auth

- user_id (PK)
- username (unique)
- password_hash (BCrypt)
- role (ADMIN|INSTRUCTOR|STUDENT)
- failed_attempts (int)
- locked_until (timestamp nullable)
- last_login (timestamp)

Example DDL (simplified):

```sql
CREATE TABLE users_auth (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  last_login TIMESTAMP NULL
);
```

### settings

- setting_key (PK)
- setting_value (text)

Used for: MAINTENANCE*MODE, SMTP*\* keys, ADMIN_EMAIL, etc.

```sql
CREATE TABLE settings (
  setting_key VARCHAR(64) PRIMARY KEY,
  setting_value TEXT NOT NULL
);
```

## Seed Data

- See `data/seeds/dummy_seed.sql` for sample users, courses, sections.

## Indexes & Constraints

- Unique index on `users_auth.username`.
- Foreign keys between enrollments/sections/courses per schema in seed.

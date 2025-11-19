# Security Practices

- Passwords: BCrypt (`org.mindrot.jbcrypt.BCrypt`)
- Lockout: track `failed_attempts` and `locked_until` to throttle brute-force.
- Least privilege DB user: grant only necessary permissions to `erp_user` in non-dev.
- Input validation: validate and sanitize command tokens before SQL.
- Secrets: keep SMTP credentials and DB passwords outside source control; prefer env vars or secure config.
- TLS: run server behind a TLS terminator (stunnel/Nginx) if needed.
- Logging: never log plaintext passwords; redact sensitive values.

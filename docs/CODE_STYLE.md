# Code Style

- Java: 4-space indent, no tabs.
- Naming: classes PascalCase, methods/fields camelCase, constants UPPER_SNAKE.
- Nullability: avoid returning null; prefer Optional or empty collections.
- Exceptions: use checked exceptions for recoverable cases in DAO; convert to `ERROR:` messages at router boundary.
- Logging: concise messages, include context (userId/sectionId) where safe.
- Tests: favor deterministic tests; isolate DB with test data.

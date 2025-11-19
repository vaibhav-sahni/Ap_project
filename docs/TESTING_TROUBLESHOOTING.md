# Testing & Troubleshooting

## Quick Tests

- Login flow via client
- Catalog retrieval and registration
- Instructor export/import CSV
- Admin maintenance toggle and client toast behavior

## Manual Protocol Test (Windows)

- Install `ncat` or use `telnet`.

```powershell
# With ncat (from Nmap):
ncat localhost 7000
LOGIN:student_1:password
```

## Logs

- Server: console/IDE logs, consider adding a file appender.
- Client: console logs; add a simple logger if needed.

## Common Issues

- ERROR:Unknown command — check command name and arguments.
- Connection refused — verify server is running and port.
- File download not saved — ensure client parses `FILE_DOWNLOAD:` and decodes BASE64.
- UI freeze — move network calls off the Swing EDT.

# Protocol Reference (TCP Command API)

The client and server communicate over a simple single-line TCP protocol.
Each request is a single line with colon-separated tokens. The server replies with one of the response types below.

- SUCCESS:<payload> — success with optional JSON payload
- ERROR:<message> — error, human-readable
- FILE_DOWNLOAD:<mime>:<filename>:BASE64:<payload> — file transfer

## Connection

- Default host/port: see server config (typically `localhost:7000`).
- Each client opens a socket and sends one command per line.

Example (netcat):

```text
LOGIN:student_1:password
SUCCESS:{"userId":101,"role":"Student","username":"student_1"}
```

## Authentication

- LOGIN:username:password
- LOGOUT
- CHANGE_PASSWORD:userId:old:new

Example:

```text
CHANGE_PASSWORD:101:oldPass123:newPass456
SUCCESS:Password changed
```

## Student

- GET_CATALOG
- GET_TIMETABLE:userId
- GET_GRADES:userId
- REGISTER:userId:sectionId
- DROP_SECTION:userId:sectionId
- DOWNLOAD_TRANSCRIPT:userId -> returns FILE_DOWNLOAD:text/html:transcript.html:BASE64:...

Example file download:

```text
DOWNLOAD_TRANSCRIPT:101
FILE_DOWNLOAD:text/html:transcript.html:BASE64:PD94bWwgdmVyc2lvbj0iMS4wIj8+PGh0bWw+...
```

## Instructor

- GET_INSTRUCTOR_SECTIONS:instructorId
- GET_ROSTER:instructorId:sectionId
- RECORD_SCORE:instructorId:enrollmentId:component:score
- COMPUTE_FINAL_GRADE:instructorId:enrollmentId
- EXPORT_GRADES:instructorId:sectionId -> FILE_DOWNLOAD:text/csv:grades.csv:BASE64:...
- IMPORT_GRADES:instructorId:sectionId:BASE64:<base64csv>

## Admin

- CREATE_STUDENT:... | CREATE_INSTRUCTOR:...
- CREATE_COURSE | CREATE_SECTION | CREATE_COURSE_SECTION
- GET_ALL_COURSES | GET_ALL_INSTRUCTORS | GET_ALL_STUDENTS
- REASSIGN_INSTRUCTOR:sectionId:newInstructorId
- TOGGLE_MAINTENANCE:ON|OFF
- CHECK_MAINTENANCE -> SUCCESS:true|false
- DB_BACKUP -> FILE_DOWNLOAD:application/gzip:backup.sql.gz:BASE64:...
- DB_RESTORE:BASE64:<backupGzBase64>
- SET_DROP_DEADLINE:YYYY-MM-DD

## Error Handling

- Always check for `ERROR:` prefix before parsing payloads.
- Keep commands single-line; use BASE64 for binary/multi-line data.

## Client Snippet (Java)

```java
String cmd = "LOGIN:" + username + ":" + password;
out.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
out.flush();
String resp = in.readLine();
if (resp.startsWith("SUCCESS:")) {
    String json = resp.substring("SUCCESS:".length());
    UserAuth ua = gson.fromJson(json, UserAuth.class);
} else if (resp.startsWith("ERROR:")) {
    throw new RuntimeException(resp.substring("ERROR:".length()));
}
```

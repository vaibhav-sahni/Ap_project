package edu.univ.erp.service.admin;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uses system-installed mysqldump/mysql to create and restore gzipped SQL dumps.
 * Relies on system properties for DB connection details (the same ones used by DBConnector):
 *  - erp.jdbcUrl (jdbc:mysql://host:port/db)
 *  - erp.user
 *  - erp.pass
 */
public class MysqldumpBackupService {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(MysqldumpBackupService.class.getName());

    public Path createGzippedBackup() throws Exception {
        DbInfo info = parseJdbcUrl(edu.univ.erp.dao.db.DBConnector.getErpJdbcUrl());
        String user = edu.univ.erp.dao.db.DBConnector.getErpUsername();
        String pass = edu.univ.erp.dao.db.DBConnector.getErpPassword();

        Path tmpSql = Files.createTempFile("erp-backup-", ".sql");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("mysqldump");
            // Avoid tablespace-related errors by disabling tablespaces in the dump
            cmd.add("--no-tablespaces");
            cmd.add("-h"); cmd.add(info.host);
            cmd.add("-P"); cmd.add(String.valueOf(info.port));
            cmd.add("-u"); cmd.add(user);
            // Provide password explicitly to the child process to ensure it is used
            cmd.add("--password=" + pass);
            cmd.add(info.database);

            // Ensure --no-tablespaces is present (defensive) to avoid tablespaces errors
            if (!cmd.contains("--no-tablespaces")) {
                cmd.add(1, "--no-tablespaces");
                LOG.info("Enforced --no-tablespaces on mysqldump command");
            }
            runProcessToFile(cmd, Map.of("MYSQL_PWD", pass), tmpSql);

            // gzip the SQL into .gz
            Path gz = Files.createTempFile("erp-backup-", ".sql.gz");
            try (InputStream in = Files.newInputStream(tmpSql); OutputStream out = Files.newOutputStream(gz); java.util.zip.GZIPOutputStream gzOut = new java.util.zip.GZIPOutputStream(out)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) gzOut.write(buf, 0, r);
            }
            return gz;
        } finally {
            try {
                Files.deleteIfExists(tmpSql);
            } catch (java.io.IOException ignore) {
                // best-effort cleanup
            }
        }
    }

    public void restoreFromGzippedDump(Path gzFile) throws Exception {
        // db credentials are obtained via DBConnector inside used methods; local copies not needed here
        // Pre-scan the gzipped dump for known error patterns (e.g., mysqldump errors) to avoid executing error text as SQL
        try (InputStream gzIn = new java.util.zip.GZIPInputStream(Files.newInputStream(gzFile));
             java.io.BufferedReader scanner = new java.io.BufferedReader(new java.io.InputStreamReader(gzIn))) {
            String ln;
            while ((ln = scanner.readLine()) != null) {
                String t = ln.trim();
                if (t.startsWith("mysqldump:") || t.toLowerCase().contains("error") || t.toLowerCase().contains("access denied")) {
                    // Provide a clearer, actionable error when mysqldump wrote errors into the dump
                    String lower = t.toLowerCase();
                    String advice = "Possible causes: the dump contains mysqldump error output. If the dump was taken with mysqldump, recreate it using --no-tablespaces or grant the PROCESS privilege to the dumping user.";
                    if (lower.contains("tablespace") || lower.contains("tablespaces") || lower.contains("process privilege")) {
                        advice = "mysqldump reported a tablespaces/PROCESS privilege error. Recreate the dump with --no-tablespaces, or grant the PROCESS privilege to the dumping user (recommended: use --no-tablespaces).";
                    }
                    String msg = "Dump file contains error output from mysqldump: " + ln + " -- " + advice;
                    LOG.severe(msg);
                    throw new RuntimeException(msg);
                }
            }
        }

        // Execute the SQL dump over JDBC using the server's configured connection.
        try (java.sql.Connection conn = edu.univ.erp.dao.db.DBConnector.getErpConnection()) {
            conn.setAutoCommit(false);
            // DELIMITER-aware parsing: default delimiter is ';'
            try (InputStream gzIn2 = new java.util.zip.GZIPInputStream(Files.newInputStream(gzFile));
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(gzIn2))) {

                String delimiter = ";";
                StringBuilder sb = new StringBuilder();
                String line;
                try (java.sql.Statement stmt = conn.createStatement()) {
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#")) continue;
                        // Capture DELIMITER directive
                        if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                            // commit any pending statement with previous delimiter
                            if (sb.length() > 0) {
                                String sql = sb.toString();
                                try {
                                    stmt.execute(sql);
                                } catch (java.sql.SQLException e) {
                                    try {
                                        conn.rollback();
                                    } catch (java.sql.SQLException ignore) {
                                        // ignore rollback failure
                                    }
                                    throw new RuntimeException("Failed executing SQL during restore: " + e.getMessage() + "\nStatement:\n" + sql, e);
                                }
                                sb.setLength(0);
                            }
                            delimiter = trimmed.substring("DELIMITER ".length());
                            continue;
                        }

                        sb.append(line).append('\n');
                        if (sb.toString().trim().endsWith(delimiter)) {
                            // remove final delimiter
                            int end = sb.length() - delimiter.length();
                            String sql = sb.substring(0, Math.max(0, end));
                            try {
                                stmt.execute(sql);
                            } catch (java.sql.SQLException e) {
                                try {
                                    conn.rollback();
                                } catch (java.sql.SQLException ignore) {
                                    // ignore rollback failure
                                }
                                throw new RuntimeException("Failed executing SQL during restore: " + e.getMessage() + "\nStatement:\n" + sql, e);
                            }
                            sb.setLength(0);
                        }
                    }
                    // any remaining SQL
                    if (sb.length() > 0) {
                        try {
                            stmt.execute(sb.toString());
                        } catch (java.sql.SQLException e) {
                            try {
                                conn.rollback();
                            } catch (java.sql.SQLException ignore) {
                                // ignore rollback failure
                            }
                            throw new RuntimeException("Failed executing trailing SQL during restore: " + e.getMessage(), e);
                        }
                    }
                }
                conn.commit();
                } catch (Exception ex) {
                    try {
                        conn.rollback();
                    } catch (java.sql.SQLException ignore) {
                        // ignore rollback failures
                    }
                    // write a post-restore failure audit
                    try {
                        String auditLine = java.time.Instant.now().toString() + " | user=unknown | OP=DB_RESTORE | RESULT=FAIL | msg=" + ex.getMessage() + java.lang.System.lineSeparator();
                        java.nio.file.Path audit = java.nio.file.Paths.get("db_backup_audit.log");
                        java.nio.file.Files.writeString(audit, auditLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    } catch (java.io.IOException ignore) {
                        // ignore audit write failures
                    }
                    throw ex;
                }
            // write a post-restore success audit
            try {
                String auditLine = java.time.Instant.now().toString() + " | user=unknown | OP=DB_RESTORE | RESULT=OK" + java.lang.System.lineSeparator();
                java.nio.file.Path audit = java.nio.file.Paths.get("db_backup_audit.log");
                java.nio.file.Files.writeString(audit, auditLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (java.io.IOException ignore) {
                // ignore audit write failures
            }
        }
    }

    private static class DbInfo { String host; int port; String database; }

    private DbInfo parseJdbcUrl(String jdbc) throws Exception {
        // very simple parser for jdbc:mysql://host:port/dbname... ignores params
        if (!jdbc.startsWith("jdbc:mysql://")) throw new IllegalArgumentException("Unsupported JDBC URL: " + jdbc);
        String tail = jdbc.substring("jdbc:mysql://".length());
        String hostportDb = tail.split("\\?")[0];
        String[] parts = hostportDb.split("/", 2);
        String hostport = parts[0];
        String db = parts.length > 1 ? parts[1] : "";
        String host;
        int port;
        if (hostport.contains(":")) {
            String[] hp = hostport.split(":");
            host = hp[0];
            port = Integer.parseInt(hp[1]);
        } else {
            host = hostport;
            port = 3306;
        }
        DbInfo info = new DbInfo();
        info.host = host; info.port = port; info.database = db; return info;
    }

    private void runProcessToFile(List<String> cmd, Map<String,String> env, Path outputFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (env != null) pb.environment().putAll(env);
        // redirect stdout to outputFile and stderr to a separate temp file so we don't mix error text into the dump
        Path errFile = Files.createTempFile("erp-backup-err-", ".log");
        pb.redirectOutput(outputFile.toFile());
        pb.redirectError(errFile.toFile());
        Process p = pb.start();
        int rc = p.waitFor();
        if (rc != 0) {
            String err = "";
            try {
                err = Files.readString(errFile);
            } catch (java.io.IOException ignore) {
                // ignore read failures
            }
            throw new RuntimeException("Command failed with exit code " + rc + ": " + String.join(" ", cmd) + "\n" + err);
        }
        try {
            Files.deleteIfExists(errFile);
        } catch (java.io.IOException ignore) {
            // ignore cleanup failures
        }
    }

    
}

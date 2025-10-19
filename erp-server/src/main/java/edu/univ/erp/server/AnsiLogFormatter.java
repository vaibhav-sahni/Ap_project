package edu.univ.erp.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Simple ANSI-capable log formatter for console output.
 * Maps levels/messages to colors:
 *  - Messages containing "SUCCESS" -> green
 *  - SEVERE -> red
 *  - WARNING -> yellow
 *  - INFO -> light blue
 */
public class AnsiLogFormatter extends Formatter {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String LIGHT_BLUE = "\u001B[94m";

    private final SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        String timestamp = df.format(new Date(record.getMillis()));
        String logger = record.getLoggerName();
        String method = record.getSourceMethodName() == null ? "main" : record.getSourceMethodName();
        String msg = formatMessage(record);

        String color = chooseColor(record.getLevel().getName(), msg);

        StringBuilder sb = new StringBuilder();
        sb.append(color);
        sb.append(timestamp).append(" ").append(logger).append(" ").append(method).append(System.lineSeparator());
        sb.append(record.getLevel().getName()).append(": ").append(msg).append(RESET).append(System.lineSeparator());
        return sb.toString();
    }

    private String chooseColor(String level, String msg) {
        if (msg != null && msg.toUpperCase().contains("SUCCESS")) return GREEN;
        switch (level) {
            case "SEVERE": return RED;
            case "WARNING": return YELLOW;
            case "INFO": return LIGHT_BLUE;
            default: return RESET;
        }
    }
}

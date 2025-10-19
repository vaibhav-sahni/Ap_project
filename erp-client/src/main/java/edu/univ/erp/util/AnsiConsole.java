package edu.univ.erp.util;

/**
 * Very small helper to print colorized log lines to the terminal using ANSI escapes.
 * Colors: info (light blue), success (green), error (red).
 */
public final class AnsiConsole {
    private AnsiConsole() {}

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String LIGHT_BLUE = "\u001B[94m";

    public static void info(String msg) {
        System.out.println(LIGHT_BLUE + msg + RESET);
    }

    public static void success(String msg) {
        System.out.println(GREEN + msg + RESET);
    }

    public static void error(String msg) {
        System.err.println(RED + msg + RESET);
    }
}

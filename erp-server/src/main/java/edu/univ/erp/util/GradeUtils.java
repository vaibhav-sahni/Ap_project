package edu.univ.erp.util;

/**
 * Utility methods for grade handling and conversion to grade points.
 * Mapping (10-point scale with minus):
 *  A  = 10
 *  A- = 9
 *  B  = 8
 *  B- = 7
 *  C  = 6
 *  C- = 5
 *  D  = 4
 *  F  = 0
 *
 * Notes:
 *  - Grades are compared case-insensitively and trimmed.
 *  - Unknown or malformed grades return Optional.empty() so callers can decide
 *    whether to treat them as 0 or skip.
 */
public class GradeUtils {

    /**
     * Convert a letter grade to its numeric points on a 10-point scale.
     * Returns Double.NaN if the grade should be ignored (e.g., null/empty or "IP").
     */
    public static Double gradeToPoints(String letter) {
        if (letter == null) return null;
        String g = letter.trim().toUpperCase();
        if (g.isEmpty()) return null;

        // Skip in-progress marker (commonly "IP")
        if (g.equals("IP")) return null;

        // Direct matches
        switch (g) {
            case "A": return 10.0;
            case "A-": return 9.0;
            case "B": return 8.0;
            case "B-": return 7.0;
            case "C": return 6.0;
            case "C-": return 5.0;
            case "D": return 4.0;
            case "F": return 0.0;
            default:
                // Accept possible whitespace or lower/upper variants already normalized.
                return null; // unknown format -> caller will decide to skip
        }
    }
}

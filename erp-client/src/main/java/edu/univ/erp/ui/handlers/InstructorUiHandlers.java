package edu.univ.erp.ui.handlers;

import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.actions.InstructorActions;

/**
 * UI click handlers for instructor-related actions.
 */
public class InstructorUiHandlers {

    private final InstructorActions instructorActions;
    private final UserAuth user;

    public InstructorUiHandlers(UserAuth user) {
        this.instructorActions = new InstructorActions();
        this.user = user;
    }

    public void displayRoster(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = instructorActions.getRoster(instructorId, sectionId);
            if (roster == null || roster.isEmpty()) {
                System.out.println("Roster is empty for this section.");
                return;
            }

            System.out.println("\n--- ROSTER FOR SECTION ID: " + sectionId + " ---");
            System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
            System.out.println("-------------------------------------------------------------------------------------------------");

            roster.forEach(r -> {
                System.out.printf("%-10d %-30s %-10s %-10s\n",
                        r.getEnrollmentId(),
                        r.getStudentName(),
                        r.getRollNo(),
                        r.getFinalGrade());
            });
            System.out.println("-------------------------------------------------------------------------------------------------");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Roster Fetch Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Roster Fetch Failed: " + e.getMessage());
        }
    }

    public void computeFinalGradesWithUi(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = instructorActions.getRoster(instructorId, sectionId);
            if (roster == null || roster.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(null, "No students in this section.", "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Build preview table
            String[] cols = new String[] {"Enroll ID", "Username", "Roll No", "Quiz", "Assignment", "Midterm", "Endterm", "Final Grade"};
            Object[][] data = new Object[roster.size()][];
            for (int i = 0; i < roster.size(); i++) {
                edu.univ.erp.domain.EnrollmentRecord r = roster.get(i);
                Object[] row = new Object[8];
                row[0] = r.getEnrollmentId();
                row[1] = r.getStudentName();
                row[2] = r.getRollNo();
                // component scores may be null or 0
                row[3] = r.getQuizScore() == null ? "" : r.getQuizScore();
                row[4] = r.getAssignmentScore() == null ? "" : r.getAssignmentScore();
                row[5] = r.getMidtermScore() == null ? "" : r.getMidtermScore();
                row[6] = r.getEndtermScore() == null ? "" : r.getEndtermScore();
                row[7] = r.getFinalGrade() == null ? "In Progress" : r.getFinalGrade();
                data[i] = row;
            }

            javax.swing.JTable table = new javax.swing.JTable(data, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(table);
            scroll.setPreferredSize(new java.awt.Dimension(900, 400));

            int previewChoice = javax.swing.JOptionPane.showConfirmDialog(null, scroll, "Preview Final Grades for Section " + sectionId, javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (previewChoice != javax.swing.JOptionPane.OK_OPTION) {
                System.out.println("CLIENT LOG: Final grading preview cancelled by user.");
                return;
            }

            int confirm = javax.swing.JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to compute final grades for all students in section " + sectionId + "? This action will finalize grades and mark enrollments as Completed.",
                    "Confirm Final Grading",
                    javax.swing.JOptionPane.YES_NO_OPTION);

            if (confirm != javax.swing.JOptionPane.YES_OPTION) {
                System.out.println("CLIENT LOG: Final grading cancelled by user.");
                return;
            }

            System.out.println("\n--- COMPUTING FINAL GRADES FOR SECTION ID: " + sectionId + " ---");
            for (edu.univ.erp.domain.EnrollmentRecord student : roster) {
                try {
                    int enrollmentId = student.getEnrollmentId();
                    String finalGradeMsg = instructorActions.computeFinalGrade(instructorId, enrollmentId);
                    System.out.println("CLIENT LOG: Final Grade Computed for " + student.getStudentName() +
                            " (EID " + enrollmentId + "): " + finalGradeMsg);
                } catch (Exception e) {
                    System.err.println("CLIENT ERROR: Failed to compute grade for " + student.getStudentName() + ": " + e.getMessage());
                }
            }

            roster = instructorActions.getRoster(instructorId, sectionId);
            System.out.println("\n--- UPDATED ROSTER AFTER FINAL GRADING ---");
            System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
            System.out.println("-------------------------------------------------------------------------------------------------");
            roster.forEach(r -> {
                System.out.printf("%-10d %-30s %-10s %-10s\n",
                        r.getEnrollmentId(),
                        r.getStudentName(),
                        r.getRollNo(),
                        r.getFinalGrade());
            });
            System.out.println("-------------------------------------------------------------------------------------------------");

            javax.swing.JOptionPane.showMessageDialog(null,
                    "Final grades computed for all " + roster.size() + " students in section " + sectionId + ".",
                    "Final Grading Complete",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Final Grading Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Final Grading Failed: " + e.getMessage());
        }
    }

    public java.util.List<edu.univ.erp.domain.Section> displayAssignedSections(int instructorId) {
        if (!"Instructor".equals(user.getRole())) return java.util.Collections.emptyList();
        try {
            java.util.List<edu.univ.erp.domain.Section> assignedSections = instructorActions.getAssignedSections(instructorId);
            if (assignedSections.isEmpty()) {
                System.out.println("You are not currently assigned to teach any sections.");
            } else {
                System.out.println("\n--- ASSIGNED SECTIONS FOR INSTRUCTOR ---");
                System.out.printf("%-10s %-40s %-15s %s\n", "SECTION ID", "COURSE NAME", "COURSE CODE", "ENROLLED/CAPACITY");
                System.out.println("-------------------------------------------------------------------------------------------------");
                for (edu.univ.erp.domain.Section s : assignedSections) {
                    int enrolledCount = instructorActions.getRoster(instructorId, s.getSectionId()).size();
                    String enrolledCapacity = enrolledCount + "/" + s.getCapacity();
                    System.out.printf("%-10s %-40s %-15s %s\n", s.getSectionId(), s.getCourseName(), s.getCourseCode(), enrolledCapacity);
                }
                System.out.println("--------------------------------\n");
            }
            return assignedSections;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Failed to fetch assigned sections via API: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // Headless fetch method for UI previews/tests
    public java.util.List<edu.univ.erp.domain.EnrollmentRecord> fetchRoster(int instructorId, int sectionId) throws Exception {
        return instructorActions.getRoster(instructorId, sectionId);
    }

    /**
     * Export grades to a CSV file selected via a JFileChooser. Returns true on success.
     */
    public boolean exportGradesToFile(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        int ret = chooser.showSaveDialog(null);
        if (ret != javax.swing.JFileChooser.APPROVE_OPTION) return false;
        java.io.File file = chooser.getSelectedFile();

        try {
            String csv = instructorActions.exportGradesCsv(instructorId, sectionId);
            java.nio.file.Files.writeString(file.toPath(), csv, java.nio.charset.StandardCharsets.UTF_8);
            javax.swing.JOptionPane.showMessageDialog(null, "Grades exported to " + file.getAbsolutePath(), "Export Successful", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Export Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Export Failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Import grades from a CSV file selected via JFileChooser. Returns server summary or null on error.
     */
    public String importGradesFromFile(int instructorId, int sectionId) {
        if (!"Instructor".equals(user.getRole())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
            return null;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        int ret = chooser.showOpenDialog(null);
        if (ret != javax.swing.JFileChooser.APPROVE_OPTION) return null;
        java.io.File file = chooser.getSelectedFile();

        try {
            String csv = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);

            // --- Parse CSV and prepare preview ---
            String[] lines = csv.split("\r?\n");
            if (lines.length < 2) {
                javax.swing.JOptionPane.showMessageDialog(null, "CSV contains no data rows.", "Import Aborted", javax.swing.JOptionPane.WARNING_MESSAGE);
                return null;
            }

            java.util.List<edu.univ.erp.domain.EnrollmentRecord> roster = fetchRoster(instructorId, sectionId);
            java.util.Set<Integer> rosterIds = new java.util.HashSet<>();
            for (edu.univ.erp.domain.EnrollmentRecord r : roster) rosterIds.add(r.getEnrollmentId());

            int maxPreviewRows = 1000; // safety cap
            java.util.List<Object[]> tableRows = new java.util.ArrayList<>();
            int processed = 0;
            int errorCount = 0;
            // Detect header mapping: support full export (enrollmentId,studentId,studentName,rollNo,quiz,assignment,midterm,endterm,finalGrade)
            // and short import (enrollmentId,quiz,assignment,midterm,endterm)
            String header = lines[0];
            String[] headerCols = header.split(",");
            java.util.Map<String,Integer> headerMap = new java.util.HashMap<>();
            for (int hi = 0; hi < headerCols.length; hi++) {
                headerMap.put(headerCols[hi].trim().toLowerCase(), hi);
            }

            // helper to resolve index for named column with fallback default positions
            java.util.function.BiFunction<String,Integer,Integer> colIndex = (name, def) -> {
                Integer v = headerMap.get(name);
                return v == null ? def : v;
            };

            int idxEnrollment = -1;
            if (headerMap.containsKey("enrollmentid") || headerMap.containsKey("enrollment")) {
                idxEnrollment = colIndex.apply("enrollmentid", colIndex.apply("enrollment", 0));
            } else {
                // assume the first column is enrollmentId
                idxEnrollment = 0;
            }

            // For component columns, detect common names; otherwise assume positions relative to a short-format CSV
            int idxQuiz = headerMap.containsKey("quiz") ? headerMap.get("quiz") : 1;
            int idxAssignment = headerMap.containsKey("assignment") ? headerMap.get("assignment") : 2;
            int idxMidterm = headerMap.containsKey("midterm") ? headerMap.get("midterm") : 3;
            int idxEndterm = headerMap.containsKey("endterm") ? headerMap.get("endterm") : 4;

            for (int i = 1; i < lines.length && processed < maxPreviewRows; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",");
                Object[] row = new Object[6]; // enrollmentId, quiz, assignment, midterm, endterm, errors
                StringBuilder rowErr = new StringBuilder();

                // enrollment id
                try {
                    String rawEnroll = (cols.length > idxEnrollment ? cols[idxEnrollment].trim() : "");
                    int enrollmentId = Integer.parseInt(rawEnroll);
                    row[0] = enrollmentId;
                    if (!rosterIds.contains(enrollmentId)) {
                        rowErr.append("Not in roster; ");
                    }
                } catch (Exception ex) {
                    row[0] = (cols.length > 0 ? cols[0].trim() : "");
                    rowErr.append("Invalid enrollmentId; ");
                }

                // helper to parse optional component
                java.util.function.BiConsumer<Integer, String> parseComp = (idx, name) -> {
                    try {
                        if (cols.length > idx && !cols[idx].trim().isEmpty()) {
                            double v = Double.parseDouble(cols[idx].trim());
                            if (v < 0.0 || v > 100.0) {
                                rowErr.append(name).append(" out of range; ");
                                // place the raw value into the visible column
                                if ("Quiz".equals(name)) row[1] = cols[idx].trim();
                                else if ("Assignment".equals(name)) row[2] = cols[idx].trim();
                                else if ("Midterm".equals(name)) row[3] = cols[idx].trim();
                                else if ("Endterm".equals(name)) row[4] = cols[idx].trim();
                            } else {
                                if ("Quiz".equals(name)) row[1] = v;
                                else if ("Assignment".equals(name)) row[2] = v;
                                else if ("Midterm".equals(name)) row[3] = v;
                                else if ("Endterm".equals(name)) row[4] = v;
                            }
                        } else {
                            // leave blank on preview
                            if ("Quiz".equals(name)) row[1] = "";
                            else if ("Assignment".equals(name)) row[2] = "";
                            else if ("Midterm".equals(name)) row[3] = "";
                            else if ("Endterm".equals(name)) row[4] = "";
                        }
                    } catch (Exception ex) {
                        rowErr.append(name).append(" parse error; ");
                        if ("Quiz".equals(name)) row[1] = (cols.length > idx ? cols[idx].trim() : "");
                        else if ("Assignment".equals(name)) row[2] = (cols.length > idx ? cols[idx].trim() : "");
                        else if ("Midterm".equals(name)) row[3] = (cols.length > idx ? cols[idx].trim() : "");
                        else if ("Endterm".equals(name)) row[4] = (cols.length > idx ? cols[idx].trim() : "");
                    }
                };

                // parse components using detected indices

                parseComp.accept(idxQuiz, "Quiz");
                parseComp.accept(idxAssignment, "Assignment");
                parseComp.accept(idxMidterm, "Midterm");
                parseComp.accept(idxEndterm, "Endterm");

                String errStr = rowErr.toString();
                if (!errStr.isEmpty()) {
                    errorCount++;
                }
                row[5] = errStr;
                tableRows.add(row);
                processed++;
            }

            // Build table model
            String[] colNames = new String[]{"Enrollment ID", "Quiz", "Assignment", "Midterm", "Endterm", "Errors"};
            Object[][] tableData = new Object[tableRows.size()][];
            tableData = tableRows.toArray(tableData);

            javax.swing.JTable previewTable = new javax.swing.JTable(tableData, colNames) {
                public boolean isCellEditable(int row, int column) { return false; }
            };

            // highlight rows with errors
            previewTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    Object err = table.getModel().getValueAt(row, 5);
                    if (err != null && err.toString().length() > 0) {
                        c.setBackground(new java.awt.Color(255, 230, 230));
                    } else {
                        c.setBackground(java.awt.Color.WHITE);
                    }
                    return c;
                }
            });

            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(previewTable);
            scroll.setPreferredSize(new java.awt.Dimension(800, 400));

            // Compose modal dialog
            javax.swing.JOptionPane pane = new javax.swing.JOptionPane(scroll, javax.swing.JOptionPane.PLAIN_MESSAGE, javax.swing.JOptionPane.OK_CANCEL_OPTION);
            javax.swing.JDialog dialog = pane.createDialog(null, "CSV Import Preview - " + file.getName());
            dialog.setModal(true);
            dialog.setVisible(true);
            Object selectedValue = pane.getValue();

            // If user cancelled or closed dialog
            if (selectedValue == null || (int)selectedValue != javax.swing.JOptionPane.OK_OPTION) {
                return null;
            }

            // If there are parsing errors, do NOT allow blind import. Require the user to fix the CSV first.
            if (errorCount > 0) {
                javax.swing.JOptionPane.showMessageDialog(null, "Import aborted: there are " + errorCount + " rows with parsing/validation errors in the CSV. Please fix the file and try again.\nPreview shows the issues in red.", "Import Aborted", javax.swing.JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // Proceed to call server import (server does authoritative validation and atomic apply)
            String summary = instructorActions.importGradesCsv(instructorId, sectionId, csv);
            javax.swing.JOptionPane.showMessageDialog(null, "Import result:\n" + summary, "Import Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return summary;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Import Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Import Failed: " + e.getMessage());
            return null;
        }
    }
}

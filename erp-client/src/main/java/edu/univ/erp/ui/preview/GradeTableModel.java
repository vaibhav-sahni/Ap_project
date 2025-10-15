package edu.univ.erp.ui.preview;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.univ.erp.domain.AssessmentComponent;
import edu.univ.erp.domain.Grade;

public class GradeTableModel extends AbstractTableModel {
    private final String[] cols = {"Course", "Final Grade", "Components"};
    private List<Grade> data;

    public GradeTableModel(List<Grade> data) { this.data = data; }
    public void setData(List<Grade> data) { this.data = data; fireTableDataChanged(); }

    @Override public int getRowCount() { return data == null ? 0 : data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int column) { return cols[column]; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        if (data == null || rowIndex >= data.size()) return null;
        Grade g = data.get(rowIndex);
        return switch(columnIndex) {
            case 0 -> g.getCourseName();
            case 1 -> g.getFinalGrade();
            case 2 -> {
                StringBuilder sb = new StringBuilder();
                if (g.getComponents() != null) {
                    for (AssessmentComponent c : g.getComponents()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(c.getComponentName()).append(":").append(String.format("%.1f", c.getScore()));
                    }
                }
                yield sb.toString();
            }
            default -> null;
        };
    }
}

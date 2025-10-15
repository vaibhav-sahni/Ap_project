package edu.univ.erp.ui.preview;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.univ.erp.domain.CourseCatalog;

public class CourseCatalogTableModel extends AbstractTableModel {
    private final String[] cols = {"Section ID", "Code", "Title", "Instructor", "Capacity", "Enrolled", "Time"};
    private List<CourseCatalog> data;

    public CourseCatalogTableModel(List<CourseCatalog> data) {
        this.data = data;
    }

    public void setData(List<CourseCatalog> data) {
        this.data = data;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int column) {
        return cols[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (data == null || rowIndex >= data.size()) return null;
        CourseCatalog c = data.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> c.getSectionId();
            case 1 -> c.getCourseCode();
            case 2 -> c.getCourseTitle();
            case 3 -> c.getInstructorName();
            case 4 -> c.getCapacity();
            case 5 -> c.getEnrolledCount();
            case 6 -> c.getDayTime();
            default -> null;
        };
    }
}

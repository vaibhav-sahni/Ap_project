package edu.univ.erp.ui.preview;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.univ.erp.domain.EnrollmentRecord;

public class RosterTableModel extends AbstractTableModel {
    private final String[] cols = {"Enroll ID","Student","Roll No","Quiz","Assign","Mid","End","Final"};
    private List<EnrollmentRecord> data;
    public RosterTableModel(List<EnrollmentRecord> data) { this.data = data; }
    public void setData(List<EnrollmentRecord> data) { this.data = data; fireTableDataChanged(); }
    @Override public int getRowCount() { return data==null?0:data.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int col){ return cols[col]; }
    @Override public Object getValueAt(int r,int c){ if (data==null||r>=data.size()) return null; EnrollmentRecord e = data.get(r);
        return switch(c){
            case 0 -> e.getEnrollmentId(); case 1 -> e.getStudentName(); case 2 -> e.getRollNo();
            case 3 -> e.getQuizScoreSafe(); case 4 -> e.getAssignmentScoreSafe(); case 5 -> e.getMidtermScoreSafe();
            case 6 -> e.getEndtermScoreSafe(); case 7 -> e.getFinalGrade(); default -> null;
        };
    }
}

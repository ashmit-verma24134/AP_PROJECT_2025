package edu.univ.erp.ui.Instructor;

import edu.univ.erp.model.Grade;
import edu.univ.erp.model.Section;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.data.SectionRow;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InstructorGradebookPanel extends JPanel {

    private final long instructorId;
    private final SectionService sectionService;
    private final GradeService gradeService;

    private JComboBox<SectionRow> sectionDropdown;
    private JTable gradeTable;
    private DefaultTableModel gradeModel;

    public InstructorGradebookPanel(long instructorId,
                                    SectionService sectionService,
                                    GradeService gradeService) {

        this.instructorId = instructorId;
        this.sectionService = sectionService;
        this.gradeService = gradeService;

        setLayout(new BorderLayout());
        initUI();
        loadSections();
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sectionDropdown = new JComboBox<>();
        JButton loadGradesBtn = new JButton("Load Grades");

        loadGradesBtn.addActionListener(e -> loadGradesForSelectedSection());

        top.add(new JLabel("Select Section:"));
        top.add(sectionDropdown);
        top.add(loadGradesBtn);

        add(top, BorderLayout.NORTH);

        gradeModel = new DefaultTableModel(
                new String[]{"Student ID", "Student Name", "Score", "Letter Grade"},
                0
        );
        gradeTable = new JTable(gradeModel);

        add(new JScrollPane(gradeTable), BorderLayout.CENTER);
    }

    private void loadSections() {
        sectionDropdown.removeAllItems();

        try {
            List<SectionRow> sections =
                    sectionService.getSectionsByInstructor(instructorId, "SPRING"); // or dynamic term

            for (SectionRow s : sections) {
                sectionDropdown.addItem(s);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load instructor's sections.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadGradesForSelectedSection() {
        Section selected = (Section) sectionDropdown.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }

        gradeModel.setRowCount(0);

        try {
            List<Grade> grades = gradeService.listGradesForSection(selected.getSectionId());

            for (Grade g : grades) {
                gradeModel.addRow(new Object[]{
                        g.getStudentId(),
                        g.getStudentName(),
                        g.getScore(),
                        g.getLetterGrade()
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load grades for this section.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

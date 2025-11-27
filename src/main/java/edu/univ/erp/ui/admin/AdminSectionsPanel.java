package edu.univ.erp.ui.admin;

import edu.univ.erp.model.Section;
import edu.univ.erp.service.SectionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminSectionsPanel extends JPanel {

    private final SectionService sectionService;

    private JTable table;
    private DefaultTableModel model;

    public AdminSectionsPanel(SectionService sectionService) {
        this.sectionService = sectionService;

        setLayout(new BorderLayout());
        initUI();
        loadAllSections();
    }

    private void initUI() {
        model = new DefaultTableModel(
                new Object[]{"ID", "Course ID", "Instructor ID", "Capacity", "Semester", "Year"},
                0
        );

        table = new JTable(model);

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadAllSections());

        add(refreshBtn, BorderLayout.SOUTH);
    }

    private void loadAllSections() {
        model.setRowCount(0);

        try {
            List<Section> sections = sectionService.listAllSections();

            for (Section s : sections) {
                model.addRow(new Object[]{
                        s.getSectionId(),
                        s.getCourseId(),
                        s.getInstructorId(),
                        s.getCapacity(),
                        s.getSemester(),
                        s.getYear()
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load sections.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

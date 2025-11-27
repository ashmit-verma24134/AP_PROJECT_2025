package edu.univ.erp.ui.admin;

import edu.univ.erp.model.Course;
import edu.univ.erp.model.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.SectionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin panel for managing courses and sections—clean version using services only.
 */
public class AdminCourseSectionPanel extends JPanel {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final AdminService adminService;

    // UI components
    private JTable courseTable;
    private JTable sectionTable;
    private DefaultTableModel courseModel;
    private DefaultTableModel sectionModel;

    public AdminCourseSectionPanel(CourseService courseService,
                                   SectionService sectionService,
                                   AdminService adminService) {

        this.courseService = courseService;
        this.sectionService = sectionService;
        this.adminService = adminService;

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(buildCoursesPanel());
        splitPane.setRightComponent(buildSectionsPanel());

        add(splitPane, BorderLayout.CENTER);

        loadCourses();
    }

    // -------------------- COURSES PANEL --------------------
    private JPanel buildCoursesPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Courses"));

        courseModel = new DefaultTableModel(
                new Object[]{"ID", "Code", "Title", "Credits"}, 0
        );
        courseTable = new JTable(courseModel);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadCourses());

        JButton delete = new JButton("Delete Course");
        delete.addActionListener(e -> deleteSelectedCourse());

        JPanel btns = new JPanel();
        btns.add(refresh);
        btns.add(delete);

        p.add(new JScrollPane(courseTable), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        return p;
    }

    private void loadCourses() {
        courseModel.setRowCount(0);
        List<Course> list = courseService.listAllCourses();

        for (Course c : list) {
            courseModel.addRow(new Object[]{
                    c.getCourseId(),
                    c.getCode(),
                    c.getTitle(),
                    c.getCredits()
            });
        }
    }

    private void deleteSelectedCourse() {
        int row = courseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a course first.");
            return;
        }

        long courseId = (long) courseModel.getValueAt(row, 0);

        int ok = JOptionPane.showConfirmDialog(
                this,
                "Delete course and ALL its sections?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (ok == JOptionPane.YES_OPTION) {
            adminService.deleteCourseCascade(courseId);
            loadCourses();
            sectionModel.setRowCount(0);
        }
    }

    // -------------------- SECTIONS PANEL --------------------
    private JPanel buildSectionsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Sections"));

        sectionModel = new DefaultTableModel(
                new Object[]{"ID", "Course ID", "Instructor ID", "Capacity", "Semester"}, 0
        );
        sectionTable = new JTable(sectionModel);

        JButton load = new JButton("Load Sections");
        load.addActionListener(e -> loadSectionsForSelectedCourse());

        JButton delete = new JButton("Delete Section");
        delete.addActionListener(e -> deleteSelectedSection());

        JPanel btns = new JPanel();
        btns.add(load);
        btns.add(delete);

        p.add(new JScrollPane(sectionTable), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        return p;
    }

    private void loadSectionsForSelectedCourse() {
        int row = courseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a course first.");
            return;
        }

        long courseId = (long) courseModel.getValueAt(row, 0);

        sectionModel.setRowCount(0);
        List<Section> sections = sectionService.findFiltered(String.valueOf(courseId), null);

        for (Section s : sections) {
            sectionModel.addRow(new Object[]{
                    s.getSectionId(),
                    s.getCourseId(),
                    s.getInstructorId(),
                    s.getCapacity(),
                    s.getSemester()
            });
        }
    }

    private void deleteSelectedSection() {
        int row = sectionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }

        long sectionId = (long) sectionModel.getValueAt(row, 0);

        int ok = JOptionPane.showConfirmDialog(
                this,
                "Delete this section?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (ok == JOptionPane.YES_OPTION) {
            adminService.deleteSectionCascade(sectionId);
            loadSectionsForSelectedCourse();
        }
    }
}

package edu.univ.erp.ui.student;

import edu.univ.erp.service.AssessmentService;
import edu.univ.erp.service.AssessmentServiceImpl;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.StudentGradeService;
import edu.univ.erp.service.StudentGradeService.CourseRow;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Semester-based student grades panel using StudentGradeService (injected).
 */
public class SemesterGradesPanel extends JPanel {

    private final StudentGradeService gradeService;   // <-- SERVICE (injected)
    private String studentId;

    private final JPanel semestersContainer;
    private final JButton btnRefresh;
    private final JLabel statusLabel;

    /**
     * Constructor: inject the StudentGradeService (do not create service inside UI).
     */
    public SemesterGradesPanel(StudentGradeService gradeService) {
        this.gradeService = gradeService;

        setLayout(new BorderLayout(10,10));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(8,8,8,8));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BACKGROUND);
        JLabel title = new JLabel("Grades & Assessments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setBackground(Theme.BACKGROUND);
        btnRefresh = new JButton("Refresh");
        btnRefresh.setBackground(Theme.PRIMARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.addActionListener(e -> reload());
        right.add(btnRefresh);
        header.add(right, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Body
        semestersContainer = new JPanel();
        semestersContainer.setLayout(new BoxLayout(semestersContainer, BoxLayout.Y_AXIS));
        semestersContainer.setBackground(Theme.BACKGROUND);

        JScrollPane bodyScroll = new JScrollPane(
                semestersContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(bodyScroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BACKGROUND);
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(6,6,6,6));
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }



    // ---------------------------
    // PUBLIC API
    // ---------------------------
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        reload();
    }

    // ---------------------------
    // CORE RELOAD (NOW USING SERVICE)
    // ---------------------------
    public void reload() {
        if (studentId == null) {
            statusLabel.setText("No student selected");
            clearSemesters();
            return;
        }

        btnRefresh.setEnabled(false);
        statusLabel.setText("Loading grades...");
        clearSemesters();

        new SwingWorker<Map<String, List<CourseRow>>, Void>() {

            @Override
            protected Map<String, List<CourseRow>> doInBackground() throws Exception {
                // call injected service
                return gradeService.loadGradesForStudent(studentId);
            }

            @Override
            protected void done() {
                try {
                    Map<String, List<CourseRow>> map = get();

                    if (map == null || map.isEmpty()) {
                        JLabel empty = new JLabel("No courses found.", SwingConstants.CENTER);
                        empty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                        empty.setBorder(new EmptyBorder(18,18,18,18));
                        semestersContainer.add(empty);
                    } else {
                        for (var entry : map.entrySet()) {
                            JPanel semPanel = createSemesterPanel(entry.getKey(), entry.getValue());
                            semestersContainer.add(semPanel);
                            semestersContainer.add(Box.createRigidArea(new Dimension(1,12)));
                        }
                    }

                    semestersContainer.revalidate();
                    semestersContainer.repaint();

                    int total = map == null ? 0 : map.values().stream().mapToInt(List::size).sum();
                    statusLabel.setText("Loaded " + total + " courses");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JLabel err = new JLabel("Error loading grades: " + ex.getMessage(), SwingConstants.CENTER);
                    err.setForeground(Color.RED);
                    semestersContainer.add(err);
                    statusLabel.setText("Error");
                } finally {
                    btnRefresh.setEnabled(true);
                }
            }
        }.execute();
    }

    private void clearSemesters() {
        semestersContainer.removeAll();
        semestersContainer.revalidate();
        semestersContainer.repaint();
    }

    // ---------------------------
    // BUILD SEMESTER PANEL
    // ---------------------------
    private JPanel createSemesterPanel(String semLabel, List<CourseRow> rows) {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                new EmptyBorder(8,8,8,8)
        ));

        JLabel label = new JLabel(semLabel);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(label, BorderLayout.NORTH);

        String[] cols = {"#", "Course Code", "Course Title", "Credits", "Grade", "Grade Point", "Actions"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setRowHeight(28);
        t.getTableHeader().setBackground(Theme.PRIMARY);
        t.getTableHeader().setForeground(Color.WHITE);

        // Fill rows
        int idx = 1;
        for (CourseRow cr : rows) {
            Double gp = gradePoint(cr.finalLetter);
            m.addRow(new Object[]{
                    idx++,
                    cr.courseCode,
                    cr.courseTitle,
                    cr.credits,
                    cr.finalLetter == null ? "N/A" : cr.finalLetter,
                    gp == null ? "—" : String.format("%.2f", gp),
                    "See Components"
            });
        }

        JScrollPane sp = new JScrollPane(t);
        panel.add(sp, BorderLayout.CENTER);

        final int actionsCol = 6;

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = t.rowAtPoint(e.getPoint());
                int viewCol = t.columnAtPoint(e.getPoint());
                if (viewRow < 0) return;

                // convert view row to model index in case of sorting
                int modelRow = viewRow;
                try { modelRow = t.convertRowIndexToModel(viewRow); } catch (Exception ignored) {}

                if (viewCol == actionsCol || e.getClickCount() == 2) {
                    if (modelRow >= 0 && modelRow < rows.size()) {
                        openAssessmentsDialog(rows.get(modelRow));
                    }
                }
            }
        });

        return panel;
    }

    // ---------------------------
    // OPEN ASSESSMENTS DIALOG
    // ---------------------------
    private void openAssessmentsDialog(CourseRow cr) {
        Window w = SwingUtilities.getWindowAncestor(this);

        try {
            // create an AssessmentService implementation and pass to dialog
            AssessmentService assessmentService = new AssessmentServiceImpl();
            AssessmentsDialog dlg = new AssessmentsDialog(
                    w,
                    cr.enrollmentId,
                    cr.courseCode,
                    cr.courseTitle,
                    assessmentService
            );
            dlg.setVisible(true);

        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to open assessment dialog: " + t.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static Double gradePoint(String letter) {
        if (letter == null) return null;
        return switch (letter.toUpperCase()) {
            case "A+", "A" -> 10.0;
            case "B+" -> 8.0;
            case "B" -> 7.0;
            case "C+" -> 5.0;
            case "C" -> 4.0;
            case "D" -> 2.0;
            default -> 0.0;
        };
    }
}
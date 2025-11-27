package edu.univ.erp.ui.student;

import edu.univ.erp.service.AssessmentComponent;
import edu.univ.erp.service.AssessmentService;
import edu.univ.erp.ui.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog that shows assessment components (table) and a pie chart
 * for a single enrollment. Uses AssessmentService to fetch data.
 */
public class AssessmentsDialog extends JDialog {

    private final long enrollmentId;
    private final String courseCode;
    private final String courseTitle;

    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel chartPanel;
    private final JLabel lblFinal;

    private final AssessmentService assessmentService;

    public AssessmentsDialog(
            Window owner,
            long enrollmentId,
            String courseCode,
            String courseTitle,
            AssessmentService assessmentService
    ) {
        super(owner,
                "Assessments: " + courseCode + (courseTitle == null ? "" : " — " + courseTitle),
                ModalityType.APPLICATION_MODAL);

        this.enrollmentId = enrollmentId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.assessmentService = assessmentService;

        setLayout(new BorderLayout(8,8));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel header = new JLabel(
                (courseCode == null ? "" : courseCode + " ") +
                        (courseTitle == null ? "" : "- " + courseTitle)
        );
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
        add(header, BorderLayout.NORTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.62);

        // Left side
        JPanel left = new JPanel(new BorderLayout(6,6));
        left.setBackground(Theme.BACKGROUND);

        lblFinal = new JLabel("Final Grade: —");
        lblFinal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFinal.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        left.add(lblFinal, BorderLayout.NORTH);

        String[] cols = {"Component", "Weight (%)", "Score", "Max", "Published"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(26);
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        left.add(new JScrollPane(table), BorderLayout.CENTER);

        split.setLeftComponent(left);

        // Right: Chart
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 320));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Performance"));
        split.setRightComponent(chartPanel);

        add(split, BorderLayout.CENTER);

        // Bottom close
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(920, 520));
        pack();
        setLocationRelativeTo(owner);

        loadComponents();
    }

    private void loadComponents() {
        model.setRowCount(0);
        chartPanel.removeAll();
        chartPanel.add(new JLabel("Loading...", SwingConstants.CENTER), BorderLayout.CENTER);

        SwingWorker<List<AssessmentComponent>, Void> w = new SwingWorker<>() {
            @Override
            protected List<AssessmentComponent> doInBackground() throws Exception {
                return assessmentService.getComponents(enrollmentId);
            }

            @Override
            protected void done() {
                try {
                    List<AssessmentComponent> rows = get();
                    model.setRowCount(0);

                    AssessmentComponent finalComp = null;

                    for (AssessmentComponent r : rows) {
                        String name = safeStr(r.getName());
                        if ("Final".equalsIgnoreCase(name)) {
                            finalComp = r;
                            continue;
                        }

                        model.addRow(new Object[]{
                                name,
                                intToStr(r.getWeight()),
                                doubleToStr(r.getStudentScore()),
                                doubleToStr(r.getMaxScore()),
                                booleanToYesNo(r.getPublished())
                        });
                    }

                    if (finalComp != null) {
                        model.addRow(new Object[]{
                                safeStr(finalComp.getName()),
                                intToStr(finalComp.getWeight()),
                                doubleToStr(finalComp.getStudentScore()),
                                doubleToStr(finalComp.getMaxScore()),
                                booleanToYesNo(finalComp.getPublished())
                        });

                        updateFinalLabel(finalComp);
                    } else {
                        lblFinal.setText("Final Grade: (not computed)");
                    }

                    renderPie(rows);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblFinal.setText("Final Grade: Error");
                    chartPanel.removeAll();
                    chartPanel.add(new JLabel("Error loading: " + ex.getMessage(), SwingConstants.CENTER));
                    chartPanel.revalidate();
                    chartPanel.repaint();
                }
            }
        };
        w.execute();
    }

    private void updateFinalLabel(AssessmentComponent finalComp) {
        Double sc = finalComp.getStudentScore();
        Double mx = finalComp.getMaxScore();
        Boolean pub = finalComp.getPublished();

        if (sc != null && mx != null && mx > 0) {
            double pct = sc * 100.0 / mx;
            lblFinal.setText(String.format(
                    "Final Grade: %s (%.2f%%)",
                    (pub != null && pub) ? "Released" : "Not Released",
                    pct
            ));
        } else if (sc != null) {
            lblFinal.setText("Final Score: " + formatDouble(sc));
        } else {
            lblFinal.setText("Final Grade: " + ((pub != null && pub) ? "Released" : "N/A"));
        }
    }

    private void renderPie(List<AssessmentComponent> rows) {
        chartPanel.removeAll();
        DefaultPieDataset dataset = new DefaultPieDataset();

        for (AssessmentComponent r : rows) {
            Double sc = r.getStudentScore();
            if (sc == null || sc <= 0) continue;

            String label = safeStr(r.getName());
            dataset.setValue(label + " (" + formatDouble(sc) + ")", sc);
        }

        if (dataset.getItemCount() == 0) {
            chartPanel.add(new JLabel("No graded components", SwingConstants.CENTER));
        } else {
            JFreeChart chart = ChartFactory.createPieChart(
                    "Assessment Performance", dataset, true, true, false
            );
            chartPanel.add(new ChartPanel(chart), BorderLayout.CENTER);
        }

        chartPanel.revalidate();
        chartPanel.repaint();
    }

    // Helpers
    private static String safeStr(String s) { return (s == null ? "—" : s); }

    private static String booleanToYesNo(Boolean b) {
        if (b == null) return "Unpublished";
        return b ? "Published" : "Unpublished";
    }

    private static String intToStr(Integer v) {
        return v == null ? "—" : String.valueOf(v);
    }

    private static String doubleToStr(Double d) {
        return d == null ? "—" : formatDouble(d);
    }

    private static String formatDouble(Double d) {
        if (d == null) return "—";
        if (d % 1 == 0) return String.valueOf(d.longValue());
        return String.format("%.2f", d);
    }
}

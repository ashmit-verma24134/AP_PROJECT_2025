package edu.univ.erp.ui.student;

import edu.univ.erp.data.AssessmentComponent;
import edu.univ.erp.data.GradeDao;
import edu.univ.erp.data.GradeDaoImpl;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

/**
 * Modal dialog that shows assessment components (table) and a pie chart for a single enrollment.
 * Uses typed AssessmentComponent objects from GradeDaoImpl.
 */
public class AssessmentsDialog extends JDialog {

    private final long enrollmentId;
    private final String courseCode;
    private final String courseTitle;

    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel chartPanel;
    private final JLabel lblFinal;

    public AssessmentsDialog(Window owner, long enrollmentId, String courseCode, String courseTitle) {
        super(owner, "Assessments: " + courseCode + (courseTitle == null ? "" : " — " + courseTitle),
                ModalityType.APPLICATION_MODAL);

        this.enrollmentId = enrollmentId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;

        setLayout(new BorderLayout(8,8));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel header = new JLabel((courseCode == null ? "" : courseCode + " ") + (courseTitle == null ? "" : "- " + courseTitle));
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
        add(header, BorderLayout.NORTH);

        // center split: left table, right chart
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.62);

        // left panel: final label + table
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

        // right panel: chart
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 320));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Performance"));
        split.setRightComponent(chartPanel);

        add(split, BorderLayout.CENTER);

        // bottom close button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(920, 520));
        pack();
        setLocationRelativeTo(owner);

        // load components & chart
        loadComponents();
    }

    private void loadComponents() {
        model.setRowCount(0);
        chartPanel.removeAll();
        chartPanel.add(new JLabel("Loading...", SwingConstants.CENTER), BorderLayout.CENTER);

        SwingWorker<List<AssessmentComponent>, Void> w = new SwingWorker<>() {
            @Override
            protected List<AssessmentComponent> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    GradeDao dao = new GradeDaoImpl(conn);
                    // include unpublished components so student can still see 'unpublished' rows
                    return dao.findComponentsForEnrollment(enrollmentId, true);
                }
            }

            @Override
            protected void done() {
                try {
                    List<AssessmentComponent> rows = get();
                    model.setRowCount(0);

                    // fill table and detect final row
                    AssessmentComponent finalComp = null;
                    for (AssessmentComponent r : rows) {
                        String name = safeStr(r.getName());
                        if ("Final".equalsIgnoreCase(name)) {
                            finalComp = r;
                            continue; // we will append final at the end
                        }

                        String published = booleanToYesNo(r.getPublished());
                        String weightStr = intToStr(r.getWeight());
                        String scoreStr = doubleToStr(r.getStudentScore());
                        String maxStr = doubleToStr(r.getMaxScore());

                        model.addRow(new Object[]{
                                name == null ? "—" : name,
                                weightStr,
                                scoreStr,
                                maxStr,
                                published
                        });
                    }

                    // optionally show final as the last row (if exists)
                    if (finalComp != null) {
                        String published = booleanToYesNo(finalComp.getPublished());
                        String weightStr = intToStr(finalComp.getWeight());
                        String scoreStr = doubleToStr(finalComp.getStudentScore());
                        String maxStr = doubleToStr(finalComp.getMaxScore());

                        model.addRow(new Object[]{
                                safeStr(finalComp.getName()),
                                weightStr,
                                scoreStr,
                                maxStr,
                                published
                        });

                        String finalText;
                        Double sc = finalComp.getStudentScore();
                        Double mx = finalComp.getMaxScore();
                        Boolean pub = finalComp.getPublished();
                        if (sc != null && mx != null && mx > 0.0) {
                            double pct = sc * 100.0 / mx;
                            finalText = String.format("Final Grade: %s (%.2f%%)", (pub != null && pub) ? "Released" : "Not Released", pct);
                        } else if (sc != null) {
                            finalText = "Final Score: " + formatDouble(sc);
                        } else if (pub != null && pub) {
                            finalText = "Final Grade: Released";
                        } else {
                            finalText = "Final Grade: N/A";
                        }
                        lblFinal.setText(finalText);
                    } else {
                        lblFinal.setText("Final Grade: (not computed)");
                    }

                    // render pie
                    renderPie(rows);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    model.setRowCount(0);
                    chartPanel.removeAll();
                    chartPanel.add(new JLabel("Error loading components: " + ex.getMessage(), SwingConstants.CENTER), BorderLayout.CENTER);
                    chartPanel.revalidate();
                    chartPanel.repaint();
                    lblFinal.setText("Final Grade: Error");
                }
            }
        };
        w.execute();
    }

    private void renderPie(List<AssessmentComponent> rows) {
        chartPanel.removeAll();
        DefaultPieDataset dataset = new DefaultPieDataset();

        for (AssessmentComponent r : rows) {
            Double sc = r.getStudentScore();
            if (sc == null) continue;
            double val = sc.doubleValue();
            if (val <= 0.0) continue;
            String label = safeStr(r.getName());
            if (label == null || label.isBlank()) label = "component";
            dataset.setValue(label + " (" + formatDouble(sc) + ")", val);
        }

        if (dataset.getItemCount() == 0) {
            chartPanel.add(new JLabel("No graded components to show", SwingConstants.CENTER), BorderLayout.CENTER);
            chartPanel.revalidate();
            chartPanel.repaint();
            return;
        }

        JFreeChart chart = ChartFactory.createPieChart("Assessment Performance", dataset, true, true, false);
        ChartPanel cp = new ChartPanel(chart);
        chartPanel.add(cp, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    // --- small helpers ---
    private static String safeStr(String s) { return s == null ? null : s; }

    private static String booleanToYesNo(Boolean b) {
        if (b == null) return "Unpublished";
        return b ? "Published" : "Unpublished";
    }

    private static String intToStr(Integer v) {
        if (v == null) return "—";
        return String.valueOf(v);
    }

    private static String doubleToStr(Double d) {
        if (d == null) return "—";
        return formatDouble(d);
    }

    private static String formatDouble(Double d) {
        if (d == null) return "—";
        if (d % 1.0 == 0) return String.format("%d", d.longValue());
        return String.format("%.2f", d);
    }
}

package edu.univ.erp.ui.student;

import edu.univ.erp.service.StudentService;
import edu.univ.erp.service.StudentSummary;
import edu.univ.erp.service.SemesterRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern dashboard panel: header + metric cards + grade pie.
 * Call loadData(studentId) to refresh values.
 */
public class DashboardPanel extends JPanel {
    private final JLabel welcomeLabel = new JLabel("Welcome, Student!");
    private final JLabel subLabel = new JLabel("Student Portal");
    private final JPanel cardsContainer = new JPanel();
    private final JPanel chartContainer = new JPanel(new BorderLayout());
    private final StudentService studentService = new StudentService();

    // metric labels (updated in loadData)
    private final JLabel creditsLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel cgpaLabel = new JLabel("N/A", SwingConstants.CENTER);
    private final JLabel coursesThisTermLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel creditsEarnedLabel = new JLabel("0", SwingConstants.CENTER);

    private String studentId;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initHeader();
        initCards();
        initChartArea();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xEFFAF9)); // soft teal background
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(Color.DARK_GRAY);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(welcomeLabel);
        left.add(subLabel);

        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
    }

    private JPanel card(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6F2F1), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(new Color(0x00796b)); // teal accent
        p.add(t, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private void initCards() {
        cardsContainer.setLayout(new GridLayout(1, 4, 12, 12));
        cardsContainer.setBorder(new EmptyBorder(16, 16, 16, 16));
        cardsContainer.setOpaque(false);

        cardsContainer.add(card("Registered Credits", creditsLabel));
        cardsContainer.add(card("Current CGPA", cgpaLabel));
        cardsContainer.add(card("Courses This Term", coursesThisTermLabel));
        cardsContainer.add(card("Credits Earned", creditsEarnedLabel));

        add(cardsContainer, BorderLayout.CENTER);
    }

    /**
     * Enable/disable interactive components in this panel.
     * Keeps labels enabled so info remains visible when UI is "view only".
     */
    public void setActionsEnabled(boolean enabled) {
        setEnabledRecursive(this, enabled);
    }

    private void setEnabledRecursive(Component comp, boolean enabled) {
        if (comp instanceof JLabel) {
            comp.setEnabled(true);
        } else {
            comp.setEnabled(enabled);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                setEnabledRecursive(child, enabled);
            }
        }
    }

    private void initChartArea() {
        chartContainer.setBorder(new EmptyBorder(8, 16, 16, 16));
        chartContainer.setBackground(Color.WHITE);

        // placeholder empty chart
        DefaultPieDataset ds = new DefaultPieDataset();
        ds.setValue("No data", 1);
        JFreeChart chart = ChartFactory.createPieChart("", ds, false, false, false);
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(900, 360));
        cp.setOpaque(false);

        chartContainer.add(cp, BorderLayout.CENTER);
        add(chartContainer, BorderLayout.SOUTH);
    }

    /**
     * Fetch and populate data asynchronously.
     */
    public void loadData(String studentId) {
        this.studentId = studentId;

        // set temporary UI state
        welcomeLabel.setText("Welcome, Student!");
        subLabel.setText("Student Portal");
        creditsLabel.setText("0");
        cgpaLabel.setText("N/A");
        coursesThisTermLabel.setText("0");
        creditsEarnedLabel.setText("0");

        new SwingWorker<Void, Void>() {
            StudentSummary summary = null;
            List<SemesterRecord> sems = null;

            @Override
            protected Void doInBackground() {
                try {
                    long sid = Long.parseLong(studentId);
                    summary = studentService.getStudentSummaryById(sid);
                    sems = studentService.getSemestersUpToCurrent(sid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                // update header + cards
                if (summary != null) {
                    welcomeLabel.setText("Welcome, " + (summary.getFullName() == null ? "Student" : summary.getFullName()) + "!");
                    subLabel.setText(summary.getProgram() == null ? "" : summary.getProgram());
                    cgpaLabel.setText(summary.getCurrentCgpa() == null ? "N/A" : String.format("%.2f", summary.getCurrentCgpa()));
                } else {
                    cgpaLabel.setText("N/A");
                }

                // simple demo aggregation (replace with real values from service)
                int coursesThisTerm = 0;
                int regCredits = 0;
                double creditsEarned = 0.0;
                if (sems != null && !sems.isEmpty()) {
                    // derive demo values — replace with your actual logic
                    SemesterRecord last = sems.get(sems.size() - 1);
                    coursesThisTerm = last.getCourseCount() == null ? sems.size() : last.getCourseCount();
                    regCredits = last.getRegisteredCredits() == null ? 0 : last.getRegisteredCredits();
                }

                creditsLabel.setText(String.valueOf(regCredits));
                coursesThisTermLabel.setText(String.valueOf(coursesThisTerm));
                creditsEarnedLabel.setText(String.valueOf((int) creditsEarned));

                // Build demo grade map (replace by real aggregation)
                Map<String, Integer> gradeMap = new LinkedHashMap<>();
                gradeMap.put("A+", 33);
                gradeMap.put("A", 25);
                gradeMap.put("B+", 17);
                gradeMap.put("B", 13);
                gradeMap.put("C+", 8);
                gradeMap.put("C", 4);

                DefaultPieDataset ds = new DefaultPieDataset();
                for (Map.Entry<String, Integer> e : gradeMap.entrySet())
                    ds.setValue(e.getKey() + " (" + e.getValue() + ")", e.getValue());

                JFreeChart chart = ChartFactory.createPieChart(
                        "Grade Distribution (Cumulative)",
                        ds,
                        false, // legend off (we render labels)
                        false,
                        false
                );

                PiePlot plot = (PiePlot) chart.getPlot();
                plot.setBackgroundPaint(Color.WHITE);
                plot.setOutlineVisible(false);
                plot.setSimpleLabels(true);
                plot.setInteriorGap(0.04);
                plot.setLabelGap(0.02);
                // set pleasant colors
                plot.setSectionPaint("A+ (33)", new Color(15, 130, 120));
                plot.setSectionPaint("A (25)", new Color(38, 183, 173));
                plot.setSectionPaint("B+ (17)", new Color(117, 222, 210));
                plot.setSectionPaint("B (13)", new Color(173, 240, 231));
                plot.setSectionPaint("C+ (8)", new Color(120, 120, 120));
                plot.setSectionPaint("C (4)", new Color(160, 160, 160));

                chartContainer.removeAll();
                ChartPanel cp = new ChartPanel(chart);
                cp.setPreferredSize(new Dimension(900, 360));
                cp.setOpaque(false);
                chartContainer.add(cp, BorderLayout.CENTER);
                chartContainer.revalidate();
                chartContainer.repaint();
            }
        }.execute();
    }
}

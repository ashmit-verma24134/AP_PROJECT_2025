package edu.univ.erp.ui.Instructor;

import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.data.GradeDaoImpl;
import edu.univ.erp.service.RegistrationEventBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * InstructorGradebookPanel
 *
 * - Loads sections for instructor
 * - Loads components by joining assessment_component (section templates) -> enrollments -> grade_components (fallback)
 * - Loads enrolled students for a section
 * - Loads existing scores from grade_components for editing and saves via upsert to grades table
 * - Editing only allowed when logged-in instructor owns the section
 *
 * Notes:
 * - assessment_component: section-level templates
 * - grade_components: per-enrollment component storage (legacy)
 * - grades: canonical per-enrollment per-component store (we upsert into it)
 */
public class InstructorGradebookPanel extends JPanel {

    private long instructorId = 0L;
    private final JComboBox<SectionItem> sectionCombo = new JComboBox<>();
    private final JButton btnRefreshSections = new JButton("Refresh Sections");
    private final JButton btnLoad = new JButton("Load Section");
    private final JButton btnSave = new JButton("Save Changes");
    private final JButton btnCompute = new JButton("Compute Final");
    private final JButton btnExport = new JButton("Export CSV");
    private final JButton btnImport = new JButton("Import CSV");
    private final JButton btnAddComponent = new JButton("Add Component");

    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel("<html>Stats: —</html>");

    private final GradeTableModel tableModel = new GradeTableModel();
    private final JTable table = new JTable(tableModel);

    // current loaded data
    private long currentSectionId = -1;
    private List<ComponentDef> components = new ArrayList<>();
    private List<EnrollmentRow> enrollmentRows = new ArrayList<>();
    Double weight = null;
Double maxScore = null;
    private boolean editable = false; // whether current instructor can edit this section

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public InstructorGradebookPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Top controls
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.setOpaque(false);

        left.add(new JLabel("Section:"));
        sectionCombo.setPreferredSize(new Dimension(420, 28));
        left.add(sectionCombo);
        left.add(btnRefreshSections);
        left.add(btnLoad);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(btnImport);
        right.add(btnExport);
        right.add(btnCompute);
        right.add(btnAddComponent);
        right.add(btnSave);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Table area
        table.setFillsViewportHeight(true);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(8, 8, 8, 8), sp.getBorder()));
        add(sp, BorderLayout.CENTER);

        // Bottom: status + stats
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        statusLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        bottom.add(statusLabel, BorderLayout.WEST);
        statsLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        bottom.add(statsLabel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Button actions
        btnRefreshSections.addActionListener(e -> loadSectionsForInstructorAsync());
        btnLoad.addActionListener(e -> {
            SectionItem si = (SectionItem) sectionCombo.getSelectedItem();
            if (si != null) loadSectionAsync(si.sectionId);
        });
        btnCompute.addActionListener(e -> {
            computeFinalsInMemory();
            updateStats();
            tableModel.fireTableDataChanged();
            setStatus("Final scores computed in UI (not yet saved). Click Save to persist.");
        });
        btnSave.addActionListener(e -> saveAllScoresAsync());
        btnExport.addActionListener(e -> exportCsvAction());
        btnImport.addActionListener(e -> importCsvAction());
        btnAddComponent.addActionListener(e -> handleAddComponent());

        // initial visual hints
        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // allow editing only on score columns; model will enforce
        table.setModel(tableModel);

        // initial load (if instructorId already set by caller)
        SwingUtilities.invokeLater(this::loadSectionsForInstructorAsync);
    }

    // Overload to set context
    public void setInstructorContext(long instructorId, String term) {
        this.instructorId = instructorId;
        loadSectionsForInstructorAsync();
    }

    public void setInstructorContext(long instructorId, String term, String username) {
        setInstructorContext(instructorId, term);
    }

    private void setStatus(String s) {
        statusLabel.setText(s);
    }

    private void setEditable(boolean e) {
        this.editable = e;
        tableModel.setEditable(e);
        table.setEnabled(e);
        btnSave.setEnabled(e);
        btnImport.setEnabled(e);
        btnAddComponent.setEnabled(e);
    }

    // ---- Load sections taught by instructor (async) ----
    private void loadSectionsForInstructorAsync() {
        setStatus("Loading sections...");
        sectionCombo.removeAllItems();

        new SwingWorker<List<SectionItem>, Void>() {
            @Override
            protected List<SectionItem> doInBackground() throws Exception {
                List<SectionItem> list = new ArrayList<>();
                String sql =
                        "SELECT sc.section_id, c.code, c.title, sc.semester AS term, sc.year " +
                                "FROM sections sc " +
                                "JOIN courses c ON sc.course_id = c.course_id " +
                                "WHERE sc.instructor_id = ? " +
                                "ORDER BY sc.year DESC, sc.semester DESC";

                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setLong(1, instructorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("section_id");
                            String code = rs.getString("code");
                            String title = rs.getString("title");

                            String term = null;
                            try { term = rs.getString("term"); } catch (SQLException ignore) {}
                            String semStr = null;
                            try { semStr = rs.getString("semester"); } catch (SQLException ignore) {}

                            int year = 0;
                            try { year = rs.getInt("year"); } catch (SQLException ignore) {}

                            String label = String.format("%s - %s (%s %d, sem %s)",
                                    code,
                                    title,
                                   
                                    term == null ? "" : term,
                                    year,
                                    semStr == null ? "" : semStr);

                            list.add(new SectionItem(id, label));
                        }
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    List<SectionItem> items = get();
                    sectionCombo.removeAllItems();
                    for (SectionItem s : items) sectionCombo.addItem(s);
                    setStatus("Sections loaded. Select a section.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed to load sections: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ---- Load a single section: components + enrollments + existing scores ----
    private void loadSectionAsync(long sectionId) {
        setStatus("Loading section data...");
        new SwingWorker<Void, Void>() {
            boolean canEdit = false;
            List<ComponentDef> comps = new ArrayList<>();
            List<EnrollmentRow> rows = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    // check ownership
                    try (PreparedStatement ps = conn.prepareStatement("SELECT instructor_id FROM sections WHERE section_id = ? LIMIT 1")) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                long owner = rs.getLong("instructor_id");
                                canEdit = (owner == instructorId);
                            } else {
                                throw new SQLException("Section not found.");
                            }
                        }
                    }

                    // --- load components ---
                    // prefer section-level templates from assessment_component
                    String compSql = "SELECT id AS component_id, name AS component_name, weight, max_score "
                            + "FROM assessment_component WHERE section_id = ? ORDER BY id";

                    try (PreparedStatement ps = conn.prepareStatement(compSql)) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                long cid = rs.getLong("component_id");
                                String name = rs.getString("component_name");
                                Double weight = rs.getObject("weight") == null ? null : rs.getDouble("weight");
                                Double maxScore = rs.getObject("max_score") == null ? null : rs.getDouble("max_score");
                                comps.add(new ComponentDef(cid, name == null ? ("Comp " + cid) : name, weight, maxScore));
                            }
                        }
                    }

                    // fallback to per-enrollment grade_components if no templates exist
                    if (comps.isEmpty()) {
                        String fallback = ""
                                + "SELECT DISTINCT gc.component_id, gc.component_name, gc.weight, gc.max_score "
                                + "FROM grade_components gc "
                                + "JOIN enrollments e ON gc.enrollment_id = e.enrollment_id "
                                + "WHERE e.section_id = ? "
                                + "ORDER BY gc.component_id";

                        try (PreparedStatement ps2 = conn.prepareStatement(fallback)) {
                            ps2.setLong(1, sectionId);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                while (rs2.next()) {
                                    long cid = rs2.getLong("component_id");
                                    String name = rs2.getString("component_name");
                                    Double weight = rs2.getObject("weight") == null ? null : rs2.getDouble("weight");
                                    Double maxScore = rs2.getObject("max_score") == null ? null : rs2.getDouble("max_score");
                                    comps.add(new ComponentDef(cid, name == null ? ("Comp " + cid) : name, weight, maxScore));
                                }
                            }
                        }
                    }

                    // --- load enrolled students ---
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT e.enrollment_id, st.student_id, st.roll_no, st.full_name " +
                                    "FROM enrollments e JOIN students st ON e.student_id = st.student_id " +
                                    "WHERE e.section_id = ? AND e.status = 'ENROLLED' ORDER BY st.roll_no")) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                rows.add(new EnrollmentRow(rs.getLong("enrollment_id"),
                                        rs.getLong("student_id"),
                                        rs.getString("roll_no"),
                                        rs.getString("full_name")));
                            }
                        }
                    }

                    // --- load existing scores from grade_components for all enrollments & components we fetched ---
                    if (!rows.isEmpty() && !comps.isEmpty()) {
                        String enrollIn = rows.stream().map(r -> String.valueOf(r.enrollmentId)).collect(Collectors.joining(","));
                        String compIn = comps.stream().map(c -> String.valueOf(c.componentId)).collect(Collectors.joining(","));
                        String q = "SELECT gc.enrollment_id, gc.component_id, gc.score "
                                + "FROM grade_components gc "
                                + "WHERE gc.enrollment_id IN (" + enrollIn + ") AND gc.component_id IN (" + compIn + ")";
                        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q)) {
                            while (rs.next()) {
                                long eid = rs.getLong("enrollment_id");
                                long cid = rs.getLong("component_id");
                                Double sc = rs.getObject("score") == null ? null : rs.getDouble("score");
                                for (EnrollmentRow er : rows) {
                                    if (er.enrollmentId == eid) {
                                        er.setScore(cid, sc);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Optional: load any existing 'final' rows from grades (component='__FINAL__') to show computedFinal
                    if (!rows.isEmpty()) {
                        String enrollIn = rows.stream().map(r -> String.valueOf(r.enrollmentId)).collect(Collectors.joining(","));
                        String qFinal = "SELECT enrollment_id, final_grade FROM grades WHERE component = '__FINAL__' AND enrollment_id IN (" + enrollIn + ")";
                        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(qFinal)) {
                            while (rs.next()) {
                                long eid = rs.getLong("enrollment_id");
                                String fg = rs.getString("final_grade");
                                for (EnrollmentRow er : rows) {
                                    if (er.enrollmentId == eid) {
                                        if (fg != null && !fg.isBlank()) er.computedFinal = fg;
                                        break;
                                    }
                                }
                            }
                        } catch (SQLException ignore) {
                            // If this query can't run on some schema, ignore
                        }
                    }

                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    currentSectionId = sectionId;
                    components = comps;
                    enrollmentRows = rows;
                    setEditable(canEdit);
                    tableModel.setData(components, enrollmentRows);
                    tableModel.fireTableStructureChanged();
                    resetColumnSizes();
                    if (canEdit) setStatus("Section loaded (editable). Components: " + components.size());
                    else setStatus("Section loaded (NOT your section). Editing disabled.");
                    updateStats();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed to load section: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void resetColumnSizes() {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i == 0) table.getColumnModel().getColumn(i).setPreferredWidth(80);
            else if (i == 1) table.getColumnModel().getColumn(i).setPreferredWidth(220);
            else if (i >= 2 && i < 2 + components.size()) table.getColumnModel().getColumn(i).setPreferredWidth(100);
            else table.getColumnModel().getColumn(i).setPreferredWidth(100);
        }
    }

    // ---- Compute finals in-memory (UI) using component weights ----
    private void computeFinalsInMemory() {
        for (EnrollmentRow r : enrollmentRows) {
            double total = 0.0;
            double weightSum = 0.0;
            for (ComponentDef c : components) {
                Double sc = r.scores.get(c.componentId);
                double w = c.weight == null ? 0.0 : c.weight;
                if (sc != null) total += (sc * w / 100.0);
                weightSum += w;
            }
            double finalScore;
            if (weightSum <= 0.0) finalScore = 0.0;
            else finalScore = (total * 100.0) / weightSum;
            r.computedFinal = DF.format(finalScore);
        }
    }

    // ---- Save all scores to DB (insert/update) ----
    private void saveAllScoresAsync() {
        if (DBConnection.isMaintenanceMode()) {
    JOptionPane.showMessageDialog(this,
        "Maintenance Mode is ON. Changes are disabled.",
        "Maintenance Mode", JOptionPane.WARNING_MESSAGE);
    return;
}
        if (!editable) {
            JOptionPane.showMessageDialog(this, "You are not permitted to modify this section.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentSectionId <= 0) {
            setStatus("No section loaded.");
            return;
        }
        setStatus("Saving scores...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    // single transaction for updates + recompute final
                    boolean prevAuto = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    try {
                        // --- use single upsert to avoid duplicates and update existing rows in-place ---
                        final String upsertSql =
                                "INSERT INTO grades (enrollment_id, component, score, max_score, weight, created_at, computed_at) " +
                                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW()) " +
                                        "ON DUPLICATE KEY UPDATE " +
                                        "  score = VALUES(score), " +
                                        "  max_score = VALUES(max_score), " +
                                        "  weight = VALUES(weight), " +
                                        "  computed_at = NOW()";

                        try (PreparedStatement psUpsert = conn.prepareStatement(upsertSql)) {
                            for (EnrollmentRow r : enrollmentRows) {
                                for (ComponentDef c : components) {
                                    Double sc = r.scores.get(c.componentId);
                                    String compName = c.name;

                                    psUpsert.setLong(1, r.enrollmentId);               // enrollment_id
                                    psUpsert.setString(2, compName);                   // component (text)
                                    if (sc == null) psUpsert.setNull(3, Types.DOUBLE); // score
                                    else psUpsert.setDouble(3, sc);

                                    if (c.maxScore == null) psUpsert.setNull(4, Types.DOUBLE); // max_score
                                    else psUpsert.setDouble(4, c.maxScore);

                                    if (c.weight == null) psUpsert.setNull(5, Types.DOUBLE); // weight
                                    else psUpsert.setDouble(5, c.weight);

                                    psUpsert.executeUpdate();
                                }

                                // recompute final for this enrollment using GradeDaoImpl (same connection)
                                GradeDaoImpl gdao = new GradeDaoImpl(conn);
                                gdao.recomputeFinalAndStore(r.enrollmentId);
                            }
                        }

                        conn.commit();
                    } catch (Exception ex) {
                        try { conn.rollback(); } catch (Exception rb) { rb.printStackTrace(); }
                        throw ex;
                    } finally {
                        try { conn.setAutoCommit(prevAuto); } catch (Exception ignore) {}
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Saved scores to database and recomputed finals.");
                    updateStats();

                    // --- Refresh Instructor Dashboard Graph ---
try {
    if (SwingUtilities.getWindowAncestor(InstructorGradebookPanel.this) instanceof MainFrame frame) {
        if (frame.getInstructorPanel() != null) {
            frame.getInstructorPanel().refreshDashboardStats();
        }
    }
} catch (Exception ignore) {}


                    // notify other UI parts that grades/registrations changed (SGPA / transcript / mycourses)
                    try {
                        RegistrationEventBus.get().notifyChange();
                    } catch (Throwable t) { t.printStackTrace(); }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed saving: " + ex.getMessage());
                    JOptionPane.showMessageDialog(InstructorGradebookPanel.this, "Failed to save scores: " + ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleAddComponent() {
        if (!editable || currentSectionId <= 0) {
            JOptionPane.showMessageDialog(this, "Load a section where you can edit first.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // simple dialog: name, weight, max score
        JTextField nameF = new JTextField();
        JTextField weightF = new JTextField();
        JTextField maxF = new JTextField();
        Object[] form = {
                "Component name:", nameF,
                "Weight (percentage, optional):", weightF,
                "Max score (optional):", maxF
        };
        int ok = JOptionPane.showConfirmDialog(this, form, "Create Component", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        String nm = nameF.getText().trim();
        if (nm.isEmpty()) { JOptionPane.showMessageDialog(this, "Name required."); return; }


try { 
    if (!weightF.getText().trim().isEmpty()) 
        weight = Double.parseDouble(weightF.getText().trim()); 
} catch (Exception ex) { 
    JOptionPane.showMessageDialog(this, "Invalid weight"); 
    return; 
}

try { 
    if (!maxF.getText().trim().isEmpty()) 
        maxScore = Double.parseDouble(maxF.getText().trim()); 
} catch (Exception ex) { 
    JOptionPane.showMessageDialog(this, "Invalid max score"); 
    return; 
}

// --------------------- ADD THESE TWO LINES -----------------------
final Double fWeight = weight;
final Double fMaxScore = maxScore;
// -----------------------------------------------------------------

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    boolean prevAuto = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    try {
                        String ins = "INSERT INTO assessment_component (section_id, name, weight, max_score, published, created_at) VALUES (?, ?, ?, ?, false, NOW())";
                        long newCompId = -1;
                        try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setLong(1, currentSectionId);
                            ps.setString(2, nm);
                            if (weight == null) ps.setNull(3, Types.DOUBLE); else ps.setDouble(3, weight);
                            if (maxScore == null) ps.setNull(4, Types.DOUBLE); else ps.setDouble(4, maxScore);
                            ps.executeUpdate();
                            try (ResultSet gk = ps.getGeneratedKeys()) {
                                if (gk.next()) newCompId = gk.getLong(1);
                            }
                        }

                        if (newCompId > 0) {
                            // For every enrolled enrollment, create the per-enrollment grade rows by copying templates
                            GradeDaoImpl gdao = new GradeDaoImpl(conn);
                            try (PreparedStatement psE = conn.prepareStatement("SELECT enrollment_id FROM enrollments WHERE section_id = ? AND status = 'ENROLLED'")) {
                                psE.setLong(1, currentSectionId);
                                try (ResultSet rs = psE.executeQuery()) {
                                    while (rs.next()) {
                                        long eid = rs.getLong("enrollment_id");
                                        gdao.createComponentsForEnrollment(eid, currentSectionId);
                                    }
                                }
                            }
                        }

                        conn.commit();
                    } catch (Exception ex) {
                        try { conn.rollback(); } catch (SQLException ignore) {}
                        throw ex;
                    } finally {
                        try { conn.setAutoCommit(prevAuto); } catch (Exception ignore) {}
                    }
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    // reload so UI shows new component column
                    loadSectionAsync(currentSectionId);
                    setStatus("Component created and enrollment rows initialized.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed to create component: " + ex.getMessage());
                    JOptionPane.showMessageDialog(InstructorGradebookPanel.this, "Failed to create component: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private double parseComputedFinal(String s) {
        if (s == null) return Double.NaN;
        try {
            return Double.parseDouble(s);
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    // ---- Stats ----
    private void updateStats() {
        List<Double> finals = enrollmentRows.stream()
                .map(r -> {
                    double v = parseComputedFinal(r.computedFinal);
                    return Double.isNaN(v) ? null : v;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (finals.isEmpty()) {
            statsLabel.setText("<html>Stats: <b>No final scores</b></html>");
            return;
        }
        Collections.sort(finals);
        int n = finals.size();
        double sum = finals.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / n;
        double median = (n % 2 == 1) ? finals.get(n / 2) : (finals.get(n / 2 - 1) + finals.get(n / 2)) / 2.0;
        double min = finals.get(0);
        double max = finals.get(n - 1);
        double variance = finals.stream().mapToDouble(x -> (x - mean) * (x - mean)).sum() / n;
        double std = Math.sqrt(variance);

        String s = String.format("<html>Stats: count=%d, mean=%s, median=%s, std=%s, min=%s, max=%s</html>",
                n, DF.format(mean), DF.format(median), DF.format(std), DF.format(min), DF.format(max));
        statsLabel.setText(s);
    }

    // ---- CSV Export ----
    private void exportCsvAction() {
        if (currentSectionId <= 0) {
            JOptionPane.showMessageDialog(this, "Load a section first.", "No section", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export grades to CSV");
        fc.setSelectedFile(new File("grades_section_" + currentSectionId + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            // header
            List<String> headers = new ArrayList<>();
            headers.add("enrollment_id");
            headers.add("student_id");
            headers.add("roll_no");
            headers.add("full_name");
            for (ComponentDef c : components) headers.add(c.name + " (id:" + c.componentId + ")");
            headers.add("computed_final");
            pw.println(String.join(",", headers));
            // rows
            for (EnrollmentRow r : enrollmentRows) {
                List<String> cols = new ArrayList<>();
                cols.add(String.valueOf(r.enrollmentId));
                cols.add(String.valueOf(r.studentId));
                cols.add(escapeCsv(r.rollNo));
                cols.add(escapeCsv(r.fullName));
                for (ComponentDef c : components) {
                    Double sc = r.scores.get(c.componentId);
                    cols.add(sc == null ? "" : DF.format(sc));
                }
                cols.add(r.computedFinal == null ? "" : r.computedFinal);
                pw.println(cols.stream().map(this::escapeCsv).collect(Collectors.joining(",")));
            }
            setStatus("Exported CSV: " + f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Failed to export CSV: " + ex.getMessage());
        }
    }

    // ---- CSV Import ----
    private void importCsvAction() {
        if (!editable) {
            JOptionPane.showMessageDialog(this, "You are not permitted to modify this section.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentSectionId <= 0) {
            JOptionPane.showMessageDialog(this, "Load a section first.", "No section", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import grades from CSV");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String header = br.readLine();
                    if (header == null) throw new IOException("Empty CSV file.");
                    String[] cols = header.split(",", -1);
                    Map<Integer, ComponentDef> colToComp = new HashMap<>();
                    int enrollmentCol = -1;
                    int rollCol = -1;
                    for (int i = 0; i < cols.length; i++) {
                        String h = cols[i].trim();
                        if (h.equalsIgnoreCase("enrollment_id")) enrollmentCol = i;
                        if (h.equalsIgnoreCase("roll_no")) rollCol = i;
                        for (ComponentDef cd : components) {
                            if (h.startsWith(cd.name) || h.contains("id:" + cd.componentId)) {
                                colToComp.put(i, cd);
                                break;
                            }
                        }
                    }
                    if (enrollmentCol == -1 && rollCol == -1) {
                        throw new IOException("CSV must contain 'enrollment_id' or 'roll_no' column.");
                    }
                    String line;
                    int updated = 0;
                    while ((line = br.readLine()) != null) {
                        String[] row = line.split(",", -1);
                        String enrollmentIdStr = enrollmentCol >= 0 && enrollmentCol < row.length ? row[enrollmentCol].trim() : "";
                        long enrollmentId = -1;
                        if (!enrollmentIdStr.isEmpty()) {
                            try { enrollmentId = Long.parseLong(enrollmentIdStr); } catch (Exception ignore) {}
                        }
                        EnrollmentRow target = null;
                        if (enrollmentId > 0) {
                            for (EnrollmentRow er : enrollmentRows) if (er.enrollmentId == enrollmentId) { target = er; break; }
                        } else if (rollCol >= 0) {
                            String roll = row[rollCol].trim();
                            for (EnrollmentRow er : enrollmentRows) if (er.rollNo != null && er.rollNo.equalsIgnoreCase(roll)) { target = er; break; }
                        }
                        if (target == null) continue;
                        boolean anySet = false;
                        for (Map.Entry<Integer, ComponentDef> e : colToComp.entrySet()) {
                            int ci = e.getKey();
                            ComponentDef cd = e.getValue();
                            if (ci < row.length) {
                                String val = row[ci].trim();
                                if (!val.isEmpty()) {
                                    try {
                                        double sc = Double.parseDouble(val);
                                        target.scores.put(cd.componentId, sc);
                                        anySet = true;
                                    } catch (Exception ignore) {}
                                }
                            }
                        }
                        if (anySet) updated++;
                    }
                    setStatus("Imported CSV. Rows updated in UI: " + updated + ". Click Save to persist.");
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    tableModel.fireTableDataChanged();
                    updateStats();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed import: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n")) {
            return "\"" + out + "\"";
        }
        return out;
    }

    // small helper types
    private static class SectionItem { final long sectionId; final String label; SectionItem(long id, String label) { this.sectionId = id; this.label = label; } @Override public String toString(){return label;} }

    private static class ComponentDef {
        final long componentId;
        final String name;
        final Double weight;      // nullable
        final Double maxScore;    // nullable if DB has NULL

        ComponentDef(long id, String name, Double weight, Double maxScore) {
            this.componentId = id;
            this.name = name;
            this.weight = weight;
            this.maxScore = maxScore;
        }
    }

    private static class EnrollmentRow {
        final long enrollmentId;
        final long studentId;
        final String rollNo;
        final String fullName;
        final Map<Long, Double> scores = new HashMap<>();
        String computedFinal = null;

        EnrollmentRow(long enrollmentId, long studentId, String rollNo, String fullName) {
            this.enrollmentId = enrollmentId;
            this.studentId = studentId;
            this.rollNo = rollNo;
            this.fullName = fullName;
        }

        void setScore(long componentId, Double score) {
            if (score == null) scores.remove(componentId);
            else scores.put(componentId, score);
        }
    }

    // Table model that dynamically adapts to current components
    private class GradeTableModel extends AbstractTableModel {
        private List<ComponentDef> comps = new ArrayList<>();
        private List<EnrollmentRow> rows = new ArrayList<>();
        private boolean editable = false;

        void setEditable(boolean e) { 
            if (DBConnection.isMaintenanceMode()) {
    e = false;  // force disable
}
            this.editable = e; }
        void setData(List<ComponentDef> comps, List<EnrollmentRow> rows) {
            this.comps = comps == null ? new ArrayList<>() : comps;
            this.rows = rows == null ? new ArrayList<>() : rows;
        }


        @Override
        public int getRowCount() { return rows.size(); }

        @Override
        public int getColumnCount() {
            return 2 + comps.size() + 1;
        }


        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Roll No";
            if (column == 1) return "Student Name";
            if (column >= 2 && column < 2 + comps.size()) {
                ComponentDef c = comps.get(column - 2);
                return c.name + " (" + DF.format(c.weight == null ? 0.0 : c.weight) + "%)";
            }
            return "Final";
        }


        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 2 && columnIndex < 2 + comps.size()) return Double.class;
            if (columnIndex == 0) return String.class;
            if (columnIndex == 1) return String.class;
            return String.class;
        }


        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (!editable) return false;
            return (columnIndex >= 2 && columnIndex < 2 + comps.size());
        }


        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            EnrollmentRow r = rows.get(rowIndex);
            if (columnIndex == 0) return r.rollNo;
            if (columnIndex == 1) return r.fullName;
            if (columnIndex >= 2 && columnIndex < 2 + comps.size()) {
                ComponentDef c = comps.get(columnIndex - 2);
                Double v = r.scores.get(c.componentId);
                return v == null ? null : v;
            }
            return r.computedFinal == null ? "" : r.computedFinal;
        }


        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (DBConnection.isMaintenanceMode()) return;
            if (!editable) return;
            if (!(columnIndex >= 2 && columnIndex < 2 + comps.size())) return;
            EnrollmentRow r = rows.get(rowIndex);
            ComponentDef c = comps.get(columnIndex - 2);
            if (aValue == null) {
                r.scores.remove(c.componentId);
            } else {
                try {
                    double d = Double.parseDouble(String.valueOf(aValue));
                    r.scores.put(c.componentId, d);
                } catch (Exception ex) {
                    // ignore invalid input
                }
            }
            double total = 0.0, weightSum = 0.0;
            for (ComponentDef cd : comps) {
                Double sc = r.scores.get(cd.componentId);
                double w = cd.weight == null ? 0.0 : cd.weight;
                if (sc != null) total += (sc * w / 100.0);
                weightSum += w;
            }
            double finalScore;
            if (weightSum <= 0.0) finalScore = 0.0;
            else finalScore = (total * 100.0) / weightSum;
            r.computedFinal = DF.format(finalScore);
            fireTableRowsUpdated(rowIndex, rowIndex);
            updateStats();
        }
    }
}

package edu.univ.erp.ui.Instructor;

import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.service.RegistrationEventBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
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
 * (Your original implementation with one small enhancement:)
 *  - After successful save, we notify RegistrationEventBus so SGPA/transcript panels refresh automatically.
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

    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel("<html>Stats: —</html>");

    private final GradeTableModel tableModel = new GradeTableModel();
    private final JTable table = new JTable(tableModel);

    private long currentSectionId = -1;
    private List<ComponentDef> components = new ArrayList<>();
    private List<EnrollmentRow> enrollmentRows = new ArrayList<>();
    private boolean editable = false;

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public InstructorGradebookPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

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
        right.add(btnSave);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(8, 8, 8, 8), sp.getBorder()));
        add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        statusLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        bottom.add(statusLabel, BorderLayout.WEST);
        statsLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        bottom.add(statsLabel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

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

        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(tableModel);

        SwingUtilities.invokeLater(this::loadSectionsForInstructorAsync);
    }

    // EXISTING METHOD — keep as is
public void setInstructorContext(long instructorId, String ignoredTerm, String ignoredUsername) {
    this.instructorId = instructorId;
    loadSectionsForInstructorAsync();
}

// NEW OVERLOAD — add this BELOW the above method
public void setInstructorContext(long instructorId, String term) {
    this.instructorId = instructorId;
    loadSectionsForInstructorAsync();
}

// NEW 3-PARAM OVERLOAD — add this BELOW the 2-param overload
public void setInstructorContext(long instructorId, String term, String username) {
    // simply route to the 2-argument method
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
    }

    private void loadSectionsForInstructorAsync() {
        setStatus("Loading sections...");
        sectionCombo.removeAllItems();
        new SwingWorker<List<SectionItem>, Void>() {
            @Override
            protected List<SectionItem> doInBackground() throws Exception {
                List<SectionItem> list = new ArrayList<>();
                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT sc.section_id, c.code, c.title, sc.term, sc.year, sc.semester " +
                                     "FROM sections sc JOIN courses c ON sc.course_id = c.course_id " +
                                     "WHERE sc.instructor_id = ? ORDER BY sc.year DESC, sc.semester DESC, sc.section_id DESC"
                     )) {
                    ps.setLong(1, instructorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("section_id");
                            String code = rs.getString("code");
                            String title = rs.getString("title");
                            String term = rs.getString("term");
                            int year = rs.getInt("year");
                            String semStr = rs.getString("semester");
                            String label = String.format("%s - %s (%s %d, sem %s)", code, title, term == null ? "" : term, year, semStr == null ? "" : semStr);
                            list.add(new SectionItem(id, label));
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
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

    private void loadSectionAsync(long sectionId) {
        setStatus("Loading section data...");
        new SwingWorker<Void, Void>() {
            boolean canEdit = false;
            List<ComponentDef> comps = new ArrayList<>();
            List<EnrollmentRow> rows = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
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

                    try (PreparedStatement ps = conn.prepareStatement("SELECT component_id, name, weight FROM grade_components WHERE section_id = ? ORDER BY component_id")) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                comps.add(new ComponentDef(rs.getLong("component_id"), rs.getString("name"), rs.getDouble("weight")));
                            }
                        }
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT e.enrollment_id, st.student_id, st.roll_no, st.full_name " +
                                    "FROM enrollments e JOIN students st ON e.student_id = st.student_id " +
                                    "WHERE e.section_id = ? AND e.status = 'ENROLLED' ORDER BY st.roll_no")) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                rows.add(new EnrollmentRow(rs.getLong("enrollment_id"), rs.getLong("student_id"), rs.getString("roll_no"), rs.getString("full_name")));
                            }
                        }
                    }

                    if (!rows.isEmpty() && !comps.isEmpty()) {
                        String inClause = rows.stream().map(r -> String.valueOf(r.enrollmentId)).collect(Collectors.joining(","));
                        String q = "SELECT enrollment_id, component_id, score, computed_final FROM grades WHERE enrollment_id IN (" + inClause + ")";
                        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q)) {
                            while (rs.next()) {
                                long eid = rs.getLong("enrollment_id");
                                long cid = rs.getLong("component_id");
                                Double sc = rs.getObject("score") == null ? null : rs.getDouble("score");
                                String compFinal = rs.getString("computed_final");
                                for (EnrollmentRow er : rows) {
                                    if (er.enrollmentId == eid) {
                                        er.setScore(cid, sc);
                                        er.computedFinal = compFinal;
                                        break;
                                    }
                                }
                            }
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

    private void computeFinalsInMemory() {
        for (EnrollmentRow r : enrollmentRows) {
            double total = 0.0;
            double weightSum = 0.0;
            for (ComponentDef c : components) {
                Double sc = r.scores.get(c.componentId);
                if (sc != null) total += (sc * c.weight / 100.0);
                weightSum += c.weight;
            }
            double finalScore;
            if (weightSum <= 0.0) finalScore = 0.0;
            else finalScore = (total * 100.0) / weightSum;
            r.computedFinal = DF.format(finalScore);
        }
    }

    private void saveAllScoresAsync() {
        if (!editable) {
            JOptionPane.showMessageDialog(this, "You are not permitted to modify this section.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setStatus("Saving scores...");
        new SwingWorker<Void, Void>() {
            Exception failure = null;
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        for (EnrollmentRow r : enrollmentRows) {
                            for (ComponentDef c : components) {
                                Double sc = r.scores.get(c.componentId);
                                try (PreparedStatement psCheck = conn.prepareStatement("SELECT grade_id FROM grades WHERE enrollment_id = ? AND component_id = ? LIMIT 1")) {
                                    psCheck.setLong(1, r.enrollmentId);
                                    psCheck.setLong(2, c.componentId);
                                    try (ResultSet rs = psCheck.executeQuery()) {
                                        if (rs.next()) {
                                            long gid = rs.getLong("grade_id");
                                            try (PreparedStatement psUpd = conn.prepareStatement("UPDATE grades SET score = ?, updated_at = NOW() WHERE grade_id = ?")) {
                                                if (sc == null) psUpd.setNull(1, Types.DOUBLE);
                                                else psUpd.setDouble(1, sc);
                                                psUpd.setLong(2, gid);
                                                psUpd.executeUpdate();
                                            }
                                        } else {
                                            try (PreparedStatement psIns = conn.prepareStatement("INSERT INTO grades (enrollment_id, component_id, score, created_at, weight) VALUES (?, ?, ?, NOW(), ?)")) {
                                                psIns.setLong(1, r.enrollmentId);
                                                psIns.setLong(2, c.componentId);
                                                if (sc == null) psIns.setNull(3, Types.DOUBLE);
                                                else psIns.setDouble(3, sc);
                                                psIns.setDouble(4, c.weight);
                                                psIns.executeUpdate();
                                            }
                                        }
                                    }
                                }
                            }
                            double finalScore = parseComputedFinal(r.computedFinal);
                            try (PreparedStatement psUpdateFinal = conn.prepareStatement("UPDATE grades SET computed_final = ? WHERE enrollment_id = ?")) {
                                if (Double.isNaN(finalScore)) psUpdateFinal.setNull(1, Types.VARCHAR);
                                else psUpdateFinal.setString(1, DF.format(finalScore));
                                psUpdateFinal.setLong(2, r.enrollmentId);
                                psUpdateFinal.executeUpdate();
                            }
                        }
                        conn.commit();
                    } catch (Exception ex) {
                        conn.rollback();
                        throw ex;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Saved scores to database.");
                    updateStats();

                    // NEW: notify other UI parts that grades/registrations changed (SGPA / transcript / mycourses)
                    try {
                        RegistrationEventBus.get().notifyChange();
                    } catch (Throwable t) { t.printStackTrace(); }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Failed saving: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private double parseComputedFinal(String s) {
        if (s == null) return Double.NaN;
        try { return Double.parseDouble(s); } catch (Exception ex) { return Double.NaN; }
    }

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
            List<String> headers = new ArrayList<>();
            headers.add("enrollment_id");
            headers.add("student_id");
            headers.add("roll_no");
            headers.add("full_name");
            for (ComponentDef c : components) headers.add(c.name + " (id:" + c.componentId + ")");
            headers.add("computed_final");
            pw.println(String.join(",", headers));
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
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
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
                            for (EnrollmentRow er : enrollmentRows) if (er.rollNo.equalsIgnoreCase(roll)) { target = er; break; }
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
    private static class ComponentDef { final long componentId; final String name; final double weight; ComponentDef(long id, String name, double weight){ this.componentId=id; this.name=name; this.weight=weight; } }
    private static class EnrollmentRow { final long enrollmentId; final long studentId; final String rollNo; final String fullName; final Map<Long, Double> scores = new HashMap<>(); String computedFinal = null; EnrollmentRow(long enrollmentId, long studentId, String rollNo, String fullName){ this.enrollmentId = enrollmentId; this.studentId = studentId; this.rollNo=rollNo; this.fullName=fullName; } void setScore(long componentId, Double score){ if (score==null) scores.remove(componentId); else scores.put(componentId, score);} }

    private class GradeTableModel extends AbstractTableModel {
        private List<ComponentDef> comps = new ArrayList<>();
        private List<EnrollmentRow> rows = new ArrayList<>();
        private boolean editable = false;
        void setEditable(boolean e) { this.editable = e; }
        void setData(List<ComponentDef> comps, List<EnrollmentRow> rows) { this.comps = comps == null ? new ArrayList<>() : comps; this.rows = rows == null ? new ArrayList<>() : rows; }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return 2 + comps.size() + 1; }
        @Override public String getColumnName(int column) {
            if (column == 0) return "Roll No";
            if (column == 1) return "Student Name";
            if (column >= 2 && column < 2 + comps.size()) {
                ComponentDef c = comps.get(column - 2);
                return c.name + " (" + DF.format(c.weight) + "%)";
            }
            return "Final";
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 2 && columnIndex < 2 + comps.size()) return Double.class;
            if (columnIndex == 0) return String.class;
            if (columnIndex == 1) return String.class;
            return String.class;
        }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { if (!editable) return false; return (columnIndex >= 2 && columnIndex < 2 + comps.size()); }
        @Override public Object getValueAt(int rowIndex, int columnIndex) {
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
        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (!editable) return;
            if (!(columnIndex >= 2 && columnIndex < 2 + comps.size())) return;
            EnrollmentRow r = rows.get(rowIndex);
            ComponentDef c = comps.get(columnIndex - 2);
            if (aValue == null) { r.scores.remove(c.componentId); }
            else {
                try {
                    double d = Double.parseDouble(String.valueOf(aValue));
                    r.scores.put(c.componentId, d);
                } catch (Exception ex) { }
            }
            double total = 0.0, weightSum = 0.0;
            for (ComponentDef cd : comps) {
                Double sc = r.scores.get(cd.componentId);
                if (sc != null) total += (sc * cd.weight / 100.0);
                weightSum += cd.weight;
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

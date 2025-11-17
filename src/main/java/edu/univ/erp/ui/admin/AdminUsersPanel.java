package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;

/**
 * AdminUsersPanel - manage users in auth_db.users and optionally create student row in erp_db.students.
 * Fix: ignore 'user_id' when checking required NOT NULL columns for users table
 * so we do not fail the auth INSERT just because a students FK exists elsewhere.
 */
public class AdminUsersPanel extends JPanel {

    // form fields
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JComboBox<String> cbRole = new JComboBox<>(new String[]{"ALL", "STUDENT", "INSTRUCTOR", "ADMIN"});
    private final JTextField txtFirst = new JTextField(14);
    private final JTextField txtLast = new JTextField(14);
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(12);

    // table
    private final DefaultTableModel tableModel;
    private final JTable table;

    // buttons
    private final JButton btnAdd = new JButton("Add User");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnDelete = new JButton("Delete Selected");

    // detection info for auth_db.users columns
    private static class AuthSchema {
        String idCol = "id";
        String usernameCol = "username"; // or email
        String passCol = null;     // detected pass column name (password, password_hash, pass_hash, etc.)
        String roleCol = "role";
        boolean hasRoleId = false; // role_id exists
        boolean hasFirst = false, hasLast = false, hasPhone = false, hasActive = false, hasCreatedAt = false, hasEmail = false;
        Set<String> allCols = new HashSet<>();
        Map<String, Integer> colNullability = new HashMap<>(); // 0 = nullable, 1 = NOT NULL
    }

    private AuthSchema schema = null;

    public AdminUsersPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // top form
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;

        // Row 0
        gc.gridx = 0; gc.gridy = 0;
        form.add(new JLabel("Username (email):"), gc);
        gc.gridx = 1; form.add(txtUsername, gc);

        gc.gridx = 2; form.add(new JLabel("Password:"), gc);
        gc.gridx = 3; form.add(txtPassword, gc);

        // Row 1
        gc.gridx = 0; gc.gridy++;
        form.add(new JLabel("Role:"), gc);
        gc.gridx = 1; form.add(cbRole, gc);

        gc.gridx = 2; form.add(new JLabel("First:"), gc);
        gc.gridx = 3; form.add(txtFirst, gc);

        // Row 2
        gc.gridx = 0; gc.gridy++;
        form.add(new JLabel("Last:"), gc);
        gc.gridx = 1; form.add(txtLast, gc);

        gc.gridx = 2; form.add(new JLabel("Email (optional):"), gc);
        gc.gridx = 3; form.add(txtEmail, gc);

        // Row 3
        gc.gridx = 0; gc.gridy++;
        form.add(new JLabel("Phone (optional):"), gc);
        gc.gridx = 1; form.add(txtPhone, gc);

        // Buttons row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnAdd);
        btnRow.add(btnRefresh);
        btnRow.add(btnDelete);

        gc.gridx = 0; gc.gridy++;
        gc.gridwidth = 4;
        form.add(btnRow, gc);

        top.add(form, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        // table
        String[] cols = new String[]{"ID", "Username", "Role", "First Name", "Last Name", "Phone", "Active", "Created At"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sc = new JScrollPane(table);
        sc.setPreferredSize(new Dimension(900, 420));
        add(sc, BorderLayout.CENTER);

        // bottom spacing
        add(Box.createVerticalStrut(8), BorderLayout.SOUTH);

        // wire actions
        btnRefresh.addActionListener(e -> loadUsers());
        btnAdd.addActionListener(e -> onAddUser());
        btnDelete.addActionListener(e -> onDeleteSelected());

        // double-click to populate form for convenience
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = table.getSelectedRow();
                    if (r >= 0) {
                        txtUsername.setText(String.valueOf(tableModel.getValueAt(r, 1)));
                        cbRole.setSelectedItem(String.valueOf(tableModel.getValueAt(r, 2)));
                        txtFirst.setText(String.valueOf(tableModel.getValueAt(r, 3) == null ? "" : tableModel.getValueAt(r, 3)));
                        txtLast.setText(String.valueOf(tableModel.getValueAt(r, 4) == null ? "" : tableModel.getValueAt(r, 4)));
                        txtPhone.setText(String.valueOf(tableModel.getValueAt(r, 5) == null ? "" : tableModel.getValueAt(r, 5)));
                    }
                }
            }
        });

        // role filter: reload when role selection changes
        cbRole.addActionListener(e -> loadUsers());

        // detect schema and load
        SwingUtilities.invokeLater(() -> {
            try {
                schema = detectAuthSchema();
            } catch (Exception ex) {
                schema = new AuthSchema();
            }
            loadUsers();
        });
    }

    // --------------------------- detection ---------------------------
    private AuthSchema detectAuthSchema() throws SQLException {
        AuthSchema s = new AuthSchema();
        try (Connection conn = DBConnection.getAuthConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet cols = md.getColumns(conn.getCatalog(), null, "users", null)) {
                while (cols.next()) {
                    String colName = cols.getString("COLUMN_NAME");
                    String lc = colName.toLowerCase();
                    s.allCols.add(colName);
                    String isNullable = cols.getString("IS_NULLABLE"); // "YES" or "NO"
                    s.colNullability.put(colName, "NO".equalsIgnoreCase(isNullable) ? 1 : 0);

                    if ("user_id".equals(lc) || "id".equals(lc)) {
                        // prefer 'id' for primary key; if table uses 'user_id' we'll still record it,
                        // but we will ignore 'user_id' later when deciding what the panel must populate.
                        if ("id".equals(lc)) s.idCol = colName;
                        else if ("user_id".equals(lc)) {
                            // keep idCol to default 'id' unless 'id' missing.
                            if (!s.allCols.contains("id")) s.idCol = colName;
                        }
                    }
                    if ("username".equals(lc) || "email".equals(lc)) s.usernameCol = colName;
                    if ("password".equals(lc) || "password_hash".equals(lc) || "pass_hash".equals(lc) || "passhash".equals(lc)) {
                        s.passCol = colName;
                    }
                    if ("role".equals(lc)) s.roleCol = colName;
                    if ("role_id".equals(lc)) s.hasRoleId = true;
                    if ("first_name".equals(lc)) s.hasFirst = true;
                    if ("last_name".equals(lc)) s.hasLast = true;
                    if ("phone".equals(lc)) s.hasPhone = true;
                    if ("active".equals(lc)) s.hasActive = true;
                    if ("created_at".equals(lc)) s.hasCreatedAt = true;
                    if ("email".equals(lc)) s.hasEmail = true;
                }
            }
            // fallback pass detection if not found exactly
            if (s.passCol == null) {
                for (String c : s.allCols) {
                    String lc = c.toLowerCase();
                    if (lc.contains("pass") && lc.contains("hash")) { s.passCol = c; break; }
                }
            }
        }
        if (s.usernameCol == null) s.usernameCol = "username";
        if (s.roleCol == null) s.roleCol = "role";
        return s;
    }

    // --------------------------- load users ---------------------------
    private void loadUsers() {
        btnRefresh.setEnabled(false);
        tableModel.setRowCount(0);

        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try {
                    AuthSchema sLocal = (schema == null) ? detectAuthSchema() : schema;

                    String idCol = sLocal.idCol != null ? sLocal.idCol : "id";
                    String uname = sLocal.usernameCol != null ? sLocal.usernameCol : "username";
                    String roleCol = sLocal.hasRoleId ? "role_id" : (sLocal.roleCol != null ? sLocal.roleCol : "role");

                    StringBuilder sql = new StringBuilder();
                    sql.append("SELECT ").append(idCol).append(", ").append(uname).append(", ").append(roleCol);
                    if (sLocal.hasFirst) sql.append(", first_name"); else sql.append(", NULL as first_name");
                    if (sLocal.hasLast) sql.append(", last_name"); else sql.append(", NULL as last_name");
                    if (sLocal.hasPhone) sql.append(", phone"); else sql.append(", NULL as phone");
                    if (sLocal.hasActive) sql.append(", active"); else sql.append(", 0 as active");
                    if (sLocal.hasCreatedAt) sql.append(", created_at"); else sql.append(", NULL as created_at");

                    sql.append(" FROM users");

                    // role filter
                    String selRole = ((String) cbRole.getSelectedItem());
                    boolean useRoleFilter = selRole != null && !selRole.trim().isEmpty() && !"ALL".equalsIgnoreCase(selRole);
                    if (useRoleFilter) {
                        selRole = selRole.trim().toUpperCase();
                        if (sLocal.hasRoleId) {
                            int rid = 3;
                            if ("ADMIN".equals(selRole)) rid = 1;
                            else if ("INSTRUCTOR".equals(selRole)) rid = 2;
                            sql.append(" WHERE ").append(roleCol).append(" = ").append(rid);
                        } else {
                            sql.append(" WHERE ").append(roleCol).append(" = ?");
                        }
                    }

                    sql.append(" ORDER BY ").append(idCol).append(" ASC");

                    try (Connection conn = DBConnection.getAuthConnection();
                         PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                        if (!sLocal.hasRoleId && useRoleFilter) {
                            ps.setString(1, selRole);
                        }

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                long id = rs.getLong(1);
                                String username = rs.getString(2);
                                String roleVal = rs.getString(3);
                                if (sLocal.hasRoleId) {
                                    try {
                                        int rid = rs.getInt(3);
                                        if (rid == 1) roleVal = "ADMIN";
                                        else if (rid == 2) roleVal = "INSTRUCTOR";
                                        else roleVal = "STUDENT";
                                    } catch (Exception ignore) { roleVal = "UNKNOWN"; }
                                }
                                String f = rs.getString("first_name");
                                String l = rs.getString("last_name");
                                String ph = rs.getString("phone");
                                boolean act = false;
                                try { act = rs.getBoolean("active"); } catch (Exception ignored) {}
                                Timestamp createdAt = null;
                                try { createdAt = rs.getTimestamp("created_at"); } catch (Exception ignored) {}
                                Vector<Object> v = new Vector<>();
                                v.add(id);
                                v.add(username);
                                v.add(roleVal);
                                v.add(f);
                                v.add(l);
                                v.add(ph);
                                v.add(act ? "Yes" : "No");
                                v.add(createdAt == null ? "" : createdAt.toString());
                                tableModel.addRow(v);
                            }
                        }
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnRefresh.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this, "Error loading users: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // --------------------------- add user ---------------------------
// --------------------------- add user (REPLACEMENT) ---------------------------
// --------------------------- add user (FULL REPLACEMENT) ---------------------------
private void onAddUser() {
    final String username = txtUsername.getText().trim();
    final String password = new String(txtPassword.getPassword());
    final String role = ((String) cbRole.getSelectedItem()).trim().toUpperCase();
    final String first = txtFirst.getText().trim();
    final String last = txtLast.getText().trim();
    final String email = txtEmail.getText().trim();
    final String phone = txtPhone.getText().trim();

    if (username.isEmpty()) { JOptionPane.showMessageDialog(this, "Username required"); return; }
    if (password.isEmpty()) { JOptionPane.showMessageDialog(this, "Password required"); return; }

    btnAdd.setEnabled(false);

    new SwingWorker<Void, Void>() {
        Exception err = null;
        @Override protected Void doInBackground() {
            long createdAuthUserId = -1L;
            try {
                AuthSchema sLocal = (schema == null) ? detectAuthSchema() : schema;

                // 1) check duplicate
                String checkSql = "SELECT " + sLocal.idCol + " FROM users WHERE " + sLocal.usernameCol + " = ?";
                try (Connection conn = DBConnection.getAuthConnection();
                     PreparedStatement chk = conn.prepareStatement(checkSql)) {
                    chk.setString(1, username);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) throw new Exception("Username already exists");
                    }
                }

                // Build insert statement parts
                String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
                AuthSchema local = sLocal;

                java.util.List<String> cols = new java.util.ArrayList<>();
                java.util.List<String> placeholders = new java.util.ArrayList<>();
                java.util.List<Object> params = new java.util.ArrayList<>();

                cols.add(local.usernameCol);
                placeholders.add("?");
                params.add(username);

                // detect pass column name
                String passColName = local.passCol;
                if (passColName == null) {
                    String[] pref = new String[] {"pass_hash", "password_hash", "password", "passhash"};
                    for (String p : pref) {
                        for (String actual : local.allCols) {
                            if (actual.equalsIgnoreCase(p)) { passColName = actual; break; }
                        }
                        if (passColName != null) break;
                    }
                }
                if (passColName == null) passColName = "password";

                cols.add(passColName);
                placeholders.add("?");
                params.add(hashed);

                if (local.hasRoleId) {
                    cols.add("role_id");
                    placeholders.add("?");
                    int roleId = 3;
                    if ("ADMIN".equalsIgnoreCase(role)) roleId = 1;
                    else if ("INSTRUCTOR".equalsIgnoreCase(role)) roleId = 2;
                    params.add(roleId);
                } else {
                    cols.add(local.roleCol != null ? local.roleCol : "role");
                    placeholders.add("?");
                    params.add(role);
                }

                if (local.hasFirst) { cols.add("first_name"); placeholders.add("?"); params.add(first.isEmpty()? null : first); }
                if (local.hasLast)  { cols.add("last_name");  placeholders.add("?"); params.add(last.isEmpty()? null : last); }
                if (local.hasPhone) { cols.add("phone");      placeholders.add("?"); params.add(phone.isEmpty()? null : phone); }
                if (local.hasEmail) { cols.add("email");      placeholders.add("?"); params.add(email.isEmpty()? null : email); }
                if (local.hasActive) { cols.add("active"); placeholders.add("?"); params.add(1); }

                // Ensure NOT NULL columns are provided (ignore user_id requirement)
                try (Connection conn = DBConnection.getAuthConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    try (ResultSet rs = md.getColumns(conn.getCatalog(), null, "users", null)) {
                        Set<String> notNullNoDefault = new HashSet<>();
                        while (rs.next()) {
                            String col = rs.getString("COLUMN_NAME");
                            String isNull = rs.getString("IS_NULLABLE");
                            String def = rs.getString("COLUMN_DEF");
                            if ("NO".equalsIgnoreCase(isNull) && def == null) {
                                notNullNoDefault.add(col.toLowerCase());
                            }
                        }
                        if (notNullNoDefault.contains("user_id")) notNullNoDefault.remove("user_id");

                        // make sure a pass-like col is included
                        for (String must : new String[] {"pass_hash", "password_hash", "password"}) {
                            if (notNullNoDefault.contains(must.toLowerCase())) {
                                boolean present = false;
                                for (String c : cols) if (c.equalsIgnoreCase(must)) present = true;
                                if (!present) {
                                    cols.add(must);
                                    placeholders.add("?");
                                    params.add(hashed);
                                }
                            }
                        }

                        for (String req : notNullNoDefault) {
                            boolean present = false;
                            for (String c : cols) if (c.equalsIgnoreCase(req)) present = true;
                            if (!present) {
                                throw new SQLException("Cannot create user: table requires non-null column '" + req + "' that panel cannot populate. Add default in DB or update panel.");
                            }
                        }
                    }
                }

                // Build final SQL
                String insertSql = "INSERT INTO users (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", placeholders) + ")";
                System.out.println("[AdminUsersPanel] Insert SQL: " + insertSql + " params=" + params);

                // Insert into auth DB and try to obtain generated user id
                try (Connection conn = DBConnection.getAuthConnection();
                     PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                    for (int i = 0; i < params.size(); ++i) {
                        Object p = params.get(i);
                        int idx = i + 1;
                        if (p == null) {
                            ps.setNull(idx, Types.VARCHAR);
                        } else if (p instanceof Integer) {
                            ps.setInt(idx, (Integer)p);
                        } else if (p instanceof Long) {
                            ps.setLong(idx, (Long)p);
                        } else {
                            ps.setString(idx, String.valueOf(p));
                        }
                    }
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk != null && gk.next()) {
                            try { createdAuthUserId = gk.getLong(1); } catch (Exception ignore) {}
                        }
                    }
                }

                // fallback: if DB didn't return generated keys, lookup by username
                if (createdAuthUserId <= 0) {
                    try (Connection conn = DBConnection.getAuthConnection();
                         PreparedStatement p = conn.prepareStatement("SELECT " + local.idCol + " FROM users WHERE " + local.usernameCol + " = ? LIMIT 1")) {
                        p.setString(1, username);
                        try (ResultSet r = p.executeQuery()) {
                            if (r.next()) createdAuthUserId = r.getLong(1);
                        }
                    }
                }

                // --- If role is INSTRUCTOR, create a row in erp_db.instructors using the auth user id (required by FK) ---
                if ("INSTRUCTOR".equalsIgnoreCase(role)) {
                    if (createdAuthUserId <= 0) {
                        // cannot link instructor without user id
                        err = new Exception("User added, but failed to add instructor row: could not determine user id");
                    } else {
                        String fullName = (first + " " + last).trim();
                        if (fullName.isEmpty()) fullName = username;

                        try (Connection erpConn = DBConnection.getErpConnection()) {
                            // check whether an instructor with this instructor_id already exists
                            try (PreparedStatement check = erpConn.prepareStatement("SELECT instructor_id FROM instructors WHERE instructor_id = ? LIMIT 1")) {
                                check.setLong(1, createdAuthUserId);
                                try (ResultSet r = check.executeQuery()) {
                                    if (r.next()) {
                                        // already present — nothing to do
                                        System.out.println("[AdminUsersPanel] Instructor row already exists for user_id=" + createdAuthUserId);
                                    } else {
                                        // insert instructor with instructor_id = auth user id (required by FK)
                                        try (PreparedStatement ins = erpConn.prepareStatement(
                                                "INSERT INTO instructors (instructor_id, full_name, department, created_at, updated_at) VALUES (?, ?, NULL, NOW(), NOW())")) {
                                            ins.setLong(1, createdAuthUserId);
                                            ins.setString(2, fullName);
                                            ins.executeUpdate();
                                            System.out.println("[AdminUsersPanel] Created instructors row for user_id=" + createdAuthUserId);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("[AdminUsersPanel] Warning: failed to create instructor row: " + ex.getMessage());
                            err = new Exception("User added, but failed to add instructor row: " + ex.getMessage(), ex);
                        }
                    }
                }

                // --- If role is STUDENT, keep existing behavior: create a students row in erp_db (if table exists) ---
                if ("STUDENT".equalsIgnoreCase(role)) {
                    try (Connection erp = DBConnection.getErpConnection()) {
                        DatabaseMetaData md = erp.getMetaData();
                        boolean hasUserId = false;
                        try (ResultSet rs = md.getColumns(erp.getCatalog(), null, "students", "user_id")) {
                            hasUserId = rs.next();
                        }
                        String rollNo = username;
                        String fullName = (first + " " + last).trim();
                        if (fullName.isEmpty()) fullName = "Auto " + username;
                        if (hasUserId && createdAuthUserId > 0) {
                            String ins = "INSERT INTO students (roll_no, full_name, program, year, created_at, updated_at, user_id, department) " +
                                    "VALUES (?, ?, 'Unknown', 1, NOW(), NOW(), ?, 'IIIT-Delhi')";
                            try (PreparedStatement ps2 = erp.prepareStatement(ins)) {
                                ps2.setString(1, rollNo);
                                ps2.setString(2, fullName);
                                ps2.setLong(3, createdAuthUserId);
                                ps2.executeUpdate();
                            }
                        } else {
                            String ins = "INSERT INTO students (roll_no, full_name, program, year, created_at, updated_at, department) " +
                                    "VALUES (?, ?, 'Unknown', 1, NOW(), NOW(), 'IIIT-Delhi')";
                            try (PreparedStatement ps2 = erp.prepareStatement(ins)) {
                                ps2.setString(1, rollNo);
                                ps2.setString(2, fullName);
                                ps2.executeUpdate();
                            }
                        }
                    } catch (SQLException ex) {
                        // don't fail user creation if student-row insert fails; surface for UI
                        if (err == null) err = new Exception("User added, but failed to add student row: " + ex.getMessage(), ex);
                        else err = new Exception(err.getMessage() + " ; Student insert failed: " + ex.getMessage(), ex);
                    }
                }

            } catch (Exception ex) {
                err = ex;
                ex.printStackTrace();
            }
            return null;
        }

        @Override protected void done() {
            btnAdd.setEnabled(true);
            if (err != null) {
                JOptionPane.showMessageDialog(AdminUsersPanel.this, "Failed to add user: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(AdminUsersPanel.this, "User added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                // clear form
                txtUsername.setText(""); txtPassword.setText(""); txtFirst.setText(""); txtLast.setText("");
                txtEmail.setText(""); txtPhone.setText("");
                loadUsers();
            }
        }
    }.execute();
}


    // --------------------------- delete ---------------------------
    private void onDeleteSelected() {
        int[] sel = table.getSelectedRows();
        if (sel == null || sel.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more users to delete");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected user(s)? This is irreversible.", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        btnDelete.setEnabled(false);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getAuthConnection()) {
                    conn.setAutoCommit(true);
                    String idCol = (schema != null ? schema.idCol : "id");
                    String delSql = "DELETE FROM users WHERE " + idCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                        for (int r : sel) {
                            Object idObj = tableModel.getValueAt(r, 0);
                            long id = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
                            ps.setLong(1, id);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnDelete.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this, "Failed to delete: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadUsers();
                }
            }
        }.execute();
    }
}

package edu.univ.erp.ui.admin;

import edu.univ.erp.service.UserService;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.List;

/**
 * AdminUsersPanel (SERVICE-BASED VERSION)
 *
 * - No direct DB code here.
 * - All business logic moved into UserServiceImpl.
 * - Panel only handles UI events.
 * - Keeps 100% existing behavior:
 *      ✔ Create auth user
 *      ✔ Auto-create instructors row
 *      ✔ Auto-create students row
 *      ✔ Delete user
 *      ✔ Load users
 *      ✔ Role filter
 *      ✔ Double-click -> fill form
 */
public class AdminUsersPanel extends JPanel {

    // ------------------ UI FIELDS -------------------
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JComboBox<String> cbRole =
            new JComboBox<>(new String[]{"ALL", "STUDENT", "INSTRUCTOR", "ADMIN"});
    private final JTextField txtFirst = new JTextField(14);
    private final JTextField txtLast = new JTextField(14);
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(12);

    private final DefaultTableModel tableModel;
    private final JTable table;

    private final JButton btnAdd = new JButton("Add User");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnDelete = new JButton("Delete Selected");

    // ------------------ SERVICES ---------------------
    private final UserService userService;

    public AdminUsersPanel(UserService userService) {
        this.userService = userService;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // ------------------ TOP FORM ------------------
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel("Username (email):"), gc);
        gc.gridx = 1; form.add(txtUsername, gc);

        gc.gridx = 2; form.add(new JLabel("Password:"), gc);
        gc.gridx = 3; form.add(txtPassword, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel("Role:"), gc);
        gc.gridx = 1; form.add(cbRole, gc);

        gc.gridx = 2; form.add(new JLabel("First Name:"), gc);
        gc.gridx = 3; form.add(txtFirst, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel("Last Name:"), gc);
        gc.gridx = 1; form.add(txtLast, gc);

        gc.gridx = 2; form.add(new JLabel("Email:"), gc);
        gc.gridx = 3; form.add(txtEmail, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel("Phone:"), gc);
        gc.gridx = 1; form.add(txtPhone, gc);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnAdd);
        buttons.add(btnRefresh);
        buttons.add(btnDelete);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 4;
        form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        // ------------------ TABLE ---------------------
        String[] cols = {
                "ID", "Username", "Role",
                "First Name", "Last Name",
                "Phone", "Active", "Created At"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane sc = new JScrollPane(table);
        sc.setPreferredSize(new Dimension(850, 450));
        add(sc, BorderLayout.CENTER);

        // ------------------ EVENTS --------------------
        btnRefresh.addActionListener(e -> loadUsers());
        btnAdd.addActionListener(e -> addUser());
        btnDelete.addActionListener(e -> deleteSelected());

        cbRole.addActionListener(e -> loadUsers());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) fillFormFromRow();
            }
        });

        // Initial load
        SwingUtilities.invokeLater(this::loadUsers);
    }

    // -----------------------------------------------------
    // LOAD USERS (from UserService)
    // -----------------------------------------------------
    private void loadUsers() {
        btnRefresh.setEnabled(false);
        tableModel.setRowCount(0);

        new SwingWorker<Void, Void>() {
            List<edu.univ.erp.data.User> users;
            Exception err;

            @Override protected Void doInBackground() {
                try {
                    users = userService.listAuthUsers();
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }

            @Override protected void done() {
                btnRefresh.setEnabled(true);

                if (err != null) {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this,
                            "Error loading users: " + err.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String roleFilter = (String) cbRole.getSelectedItem();
                if (roleFilter == null) roleFilter = "ALL";
                roleFilter = roleFilter.toUpperCase();

                for (edu.univ.erp.data.User u : users) {
                    if (!"ALL".equals(roleFilter)
                            && !roleFilter.equalsIgnoreCase(u.getRole())) {
                        continue;
                    }

                    tableModel.addRow(new Object[]{
                            u.getId(),
                            u.getEmail(),
                            u.getRole(),
                            u.getFirstName(),
                            u.getLastName(),
                            u.getPhone(),
                            u.isActive() ? "Yes" : "No",
                            u.getCreatedAt() == null ? "" : u.getCreatedAt().toString()
                    });
                }
            }
        }.execute();
    }

    // -----------------------------------------------------
    // ADD USER (uses service ONLY)
    // -----------------------------------------------------
    private void addUser() {

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String role = ((String) cbRole.getSelectedItem()).trim().toUpperCase();
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (username.isEmpty()) { JOptionPane.showMessageDialog(this, "Username required"); return; }
        if (password.isEmpty()) { JOptionPane.showMessageDialog(this, "Password required"); return; }

        btnAdd.setEnabled(false);

        new SwingWorker<Void, Void>() {
            Exception err;

            @Override protected Void doInBackground() {
                try {
                    // The ENTIRE brain lives in UserService
                    userService.createAuthUser(username, password, role,
                            first, last, email, phone);
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }

            @Override protected void done() {
                btnAdd.setEnabled(true);

                if (err != null) {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this,
                            "Failed to add user: " + err.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this,
                            "User added successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    txtUsername.setText("");
                    txtPassword.setText("");
                    txtFirst.setText("");
                    txtLast.setText("");
                    txtEmail.setText("");
                    txtPhone.setText("");

                    loadUsers();
                }
            }
        }.execute();
    }

    // -----------------------------------------------------
    // DELETE USERS (service only)
    // -----------------------------------------------------
    private void deleteSelected() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select users first.");
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "Delete selected users?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        btnDelete.setEnabled(false);

        new SwingWorker<Void, Void>() {
            Exception err;

            @Override protected Void doInBackground() {
                try {
                    for (int r : rows) {
                        long id = Long.parseLong(
                                String.valueOf(tableModel.getValueAt(r, 0)));
                        userService.deleteAuthUser(id);
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }

            @Override protected void done() {
                btnDelete.setEnabled(true);

                if (err != null) {
                    JOptionPane.showMessageDialog(AdminUsersPanel.this,
                            "Delete failed: " + err.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }

                loadUsers();
            }
        }.execute();
    }

    // -----------------------------------------------------
    // Populate form when user double-clicks row
    // -----------------------------------------------------
    private void fillFormFromRow() {
        int r = table.getSelectedRow();
        if (r < 0) return;

        txtUsername.setText(String.valueOf(tableModel.getValueAt(r, 1)));
        cbRole.setSelectedItem(String.valueOf(tableModel.getValueAt(r, 2)));
        txtFirst.setText(String.valueOf(tableModel.getValueAt(r, 3)));
        txtLast.setText(String.valueOf(tableModel.getValueAt(r, 4)));
        txtPhone.setText(String.valueOf(tableModel.getValueAt(r, 5)));
    }
}

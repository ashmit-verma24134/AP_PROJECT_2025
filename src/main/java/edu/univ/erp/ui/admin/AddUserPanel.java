package edu.univ.erp.ui.admin;

import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
//import tools.HashPassword;


public class AddUserPanel extends JPanel {

    public AddUserPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Add New User");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Theme.NEUTRAL_DARK);

        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Form Panel
        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        form.setBackground(Theme.SURFACE);
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JTextField txtName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtRoll = new JTextField(20);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"STUDENT", "INSTRUCTOR", "ADMIN"});

        form.add(new JLabel("Username:"), gbc);
        gbc.gridx++; form.add(txtUsername, gbc);

        gbc.gridy++; gbc.gridx = 0;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx++; form.add(txtPassword, gbc);

        gbc.gridy++; gbc.gridx = 0;
        form.add(new JLabel("Role:"), gbc);
        gbc.gridx++; form.add(roleBox, gbc);

        gbc.gridy++; gbc.gridx = 0;
        form.add(new JLabel("Name:"), gbc);
        gbc.gridx++; form.add(txtName, gbc);

        gbc.gridy++; gbc.gridx = 0;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx++; form.add(txtEmail, gbc);

        gbc.gridy++; gbc.gridx = 0;
        form.add(new JLabel("Roll No (only for Students):"), gbc);
        gbc.gridx++; form.add(txtRoll, gbc);

        JButton btnCreate = new JButton("Create User");
        btnCreate.setBackground(Theme.PRIMARY);
        btnCreate.setForeground(Color.WHITE);
        btnCreate.setFont(Theme.BODY_BOLD);

        gbc.gridy++; gbc.gridx = 1;
        form.add(btnCreate, gbc);

        add(new JScrollPane(form), BorderLayout.CENTER);

        // Button Action
        btnCreate.addActionListener(e -> {
            try {
                AdminService service = new AdminService();

                String username = txtUsername.getText().trim();
                String password = new String(txtPassword.getPassword()).trim();
                String role = roleBox.getSelectedItem().toString();
                String name = txtName.getText().trim();
                String email = txtEmail.getText().trim();
                String roll = txtRoll.getText().trim();

                long authId = service.createAuthUser(
                        username,
                        password,
                        //AuthService.password,
                        role
                );

                if (role.equals("STUDENT")) {
                    service.createStudentProfile(authId, roll, name, email);
                } else if (role.equals("INSTRUCTOR")) {
                    service.createInstructorProfile(authId, name, email);
                } else {
                    service.createAdminProfile(authId, name, email);
                }

                JOptionPane.showMessageDialog(this, "User created successfully!");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "ERROR: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                ex.printStackTrace();
            }
        });
    }
}

package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AddStudentPanel extends JPanel {

    private final JTextField rollField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField firstNameField = new JTextField();
    private final JTextField lastNameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JComboBox<String> programBox = new JComboBox<>(new String[]{"Select program", "B.Tech", "M.Tech", "PhD"});
    private final JComboBox<String> semesterBox = new JComboBox<>(new String[]{"Select semester", "Monsoon 2025", "Winter 2025", "Monsoon 2026"});
    private final JTextArea addressArea = new JTextArea(3, 20);

    public AddStudentPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ===== Header Section =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Add New Student");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.NEUTRAL_DARK);

        JLabel subtitle = new JLabel("Enter student details to add them to your course");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.NEUTRAL_MED);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(Theme.SURFACE);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ===== Scrollable Content =====
        JPanel formPanel = new JPanel();
        formPanel.setBackground(Theme.BACKGROUND);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(20, 40, 40, 40));

        JLabel formTitle = new JLabel("Student Information");
        formTitle.setFont(Theme.TITLE_FONT);
        formTitle.setForeground(Theme.NEUTRAL_DARK);
        formPanel.add(formTitle);

        JLabel formDesc = new JLabel("Fill in all required fields to register a new student");
        formDesc.setFont(Theme.BODY_FONT);
        formDesc.setForeground(Theme.NEUTRAL_MED);
        formPanel.add(formDesc);
        formPanel.add(Box.createVerticalStrut(20));

        // ===== Grid for Fields =====
        JPanel grid = new JPanel(new GridLayout(5, 2, 20, 16));
        grid.setBackground(Theme.BACKGROUND);

        grid.add(createField("Roll Number", rollField));
        grid.add(createField("Email", emailField));
        grid.add(createField("First Name", firstNameField));
        grid.add(createField("Last Name", lastNameField));
        grid.add(createField("Phone Number", phoneField));
        grid.add(createField("Program", programBox));
        grid.add(createField("Semester", semesterBox));

        JTextArea addressField = addressArea;
        addressField.setFont(Theme.BODY_FONT);
        addressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(6, 6, 6, 6)
        ));
        addressField.setBackground(Theme.SURFACE);
        grid.add(createField("Address (Optional)", new JScrollPane(addressField)));

        formPanel.add(grid);

        // ===== Buttons =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttonPanel.setBackground(Theme.BACKGROUND);

        JButton resetBtn = styledButton("Reset", Theme.SURFACE, Theme.NEUTRAL_DARK);
        JButton addBtn = styledButton("Add Student", Theme.PRIMARY, Color.WHITE);

        resetBtn.addActionListener(e -> clearForm());
        addBtn.addActionListener(e -> submitForm());

        buttonPanel.add(resetBtn);
        buttonPanel.add(addBtn);

        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(buttonPanel);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ===== Helper: Creates a labeled field row =====
    private JPanel createField(String label, Component input) {
        JPanel fieldPanel = new JPanel(new BorderLayout(5, 5));
        fieldPanel.setBackground(Theme.BACKGROUND);

        JLabel l = new JLabel(label);
        l.setFont(Theme.BODY_BOLD);
        l.setForeground(Theme.NEUTRAL_DARK);
        fieldPanel.add(l, BorderLayout.NORTH);

        if (input instanceof JTextField textField) {
            textField.setFont(Theme.BODY_FONT);
            textField.setBackground(Theme.SURFACE);
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.CARD_BORDER),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        }

        if (input instanceof JComboBox comboBox) {
            comboBox.setFont(Theme.BODY_FONT);
            comboBox.setBackground(Theme.SURFACE);
            comboBox.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        }

        fieldPanel.add(input, BorderLayout.CENTER);
        return fieldPanel;
    }

    // ===== Helper: Styled Buttons =====
    private JButton styledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.BODY_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(Theme.darken(bg, 0.1f));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // ===== Logic: Reset and Submit =====
    private void clearForm() {
        rollField.setText("");
        emailField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        phoneField.setText("");
        programBox.setSelectedIndex(0);
        semesterBox.setSelectedIndex(0);
        addressArea.setText("");
    }

    private void submitForm() {
        JOptionPane.showMessageDialog(this,
                "Student " + firstNameField.getText() + " " + lastNameField.getText() +
                        " added successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
    }
}

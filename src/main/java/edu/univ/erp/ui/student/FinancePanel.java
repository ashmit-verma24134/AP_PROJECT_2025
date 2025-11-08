package edu.univ.erp.ui.student;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class FinancePanel extends JPanel {
    private String studentId;
    private DefaultTableModel feeBreakdownModel;
    private DefaultTableModel txnModel;
    private JLabel lblDue, lblCharges, lblPayments;

    public FinancePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // --- Header ---
        JLabel header = new JLabel("💰 Student Finances");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        add(header, BorderLayout.NORTH);

        // --- Scrollable Center Content ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(Theme.BACKGROUND);

        // Wrap everything inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- Summary Cards Section ---
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 15));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        summaryPanel.setBackground(Theme.BACKGROUND);

        lblDue = createSummaryCard(summaryPanel, "Amount Due", "₹ 0", new Color(230, 57, 70), "💸");
        lblCharges = createSummaryCard(summaryPanel, "Total Charges", "₹ 0", new Color(255, 183, 3), "📄");
        lblPayments = createSummaryCard(summaryPanel, "Total Payments", "₹ 0", new Color(34, 139, 34), "💳");

        mainContent.add(summaryPanel);

        // --- Fee Breakdown Table ---
        RoundedPanel breakdownPanel = new RoundedPanel(20);
        breakdownPanel.setBackground(Theme.CARD_BG);
        breakdownPanel.setLayout(new BorderLayout());
        breakdownPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblBreakdown = new JLabel("📘 Fee Breakdown");
        lblBreakdown.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBreakdown.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        breakdownPanel.add(lblBreakdown, BorderLayout.NORTH);

        String[] cols = {"Fee Type", "Amount (₹)"};
        feeBreakdownModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblBreakdown = new JTable(feeBreakdownModel);
        tblBreakdown.setRowHeight(28);
        tblBreakdown.getTableHeader().setBackground(Theme.PRIMARY);
        tblBreakdown.getTableHeader().setForeground(Color.WHITE);
        tblBreakdown.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane spBreakdown = new JScrollPane(tblBreakdown);
        breakdownPanel.add(spBreakdown, BorderLayout.CENTER);
        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(breakdownPanel);

        // --- Important Dates Section ---
        RoundedPanel datesPanel = new RoundedPanel(20);
        datesPanel.setBackground(Theme.CARD_BG);
        datesPanel.setLayout(new GridLayout(2, 1, 5, 5));
        datesPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblDates = new JLabel("📅 Important Dates");
        lblDates.setFont(new Font("Segoe UI", Font.BOLD, 18));
        datesPanel.add(lblDates);

        JLabel lblDueDate = new JLabel("Last date for fee payment (without fine): November 15, 2025");
        JLabel lblFineDate = new JLabel("Last date with late fine: November 20, 2025");
        for (JLabel l : new JLabel[]{lblDueDate, lblFineDate}) {
            l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        }

        JPanel dateContainer = new JPanel();
        dateContainer.setLayout(new BoxLayout(dateContainer, BoxLayout.Y_AXIS));
        dateContainer.setBackground(Theme.CARD_BG);
        dateContainer.add(lblDueDate);
        dateContainer.add(Box.createVerticalStrut(5));
        dateContainer.add(lblFineDate);
        datesPanel.add(dateContainer);

        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(datesPanel);

        // --- Transaction History Section ---
        RoundedPanel txnPanel = new RoundedPanel(20);
        txnPanel.setBackground(Theme.CARD_BG);
        txnPanel.setLayout(new BorderLayout());
        txnPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTxnTitle = new JLabel("📄 Transaction History");
        lblTxnTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTxnTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        txnPanel.add(lblTxnTitle, BorderLayout.NORTH);

        String[] txnCols = {"Date", "Transaction ID", "Description", "Amount (₹)", "Status"};
        txnModel = new DefaultTableModel(txnCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblTxn = new JTable(txnModel);
        tblTxn.setRowHeight(28);
        tblTxn.getTableHeader().setBackground(Theme.PRIMARY);
        tblTxn.getTableHeader().setForeground(Color.WHITE);
        tblTxn.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane spTxn = new JScrollPane(tblTxn);
        txnPanel.add(spTxn, BorderLayout.CENTER);
        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(txnPanel);

        // --- Payment Action Section ---
        RoundedPanel actionPanel = new RoundedPanel(20);
        actionPanel.setBackground(Theme.CARD_BG);
        actionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JButton btnPay = new JButton("💳 Make Payment");
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPay.setBackground(new Color(0, 120, 215));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFocusPainted(false);
        btnPay.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnPay.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Redirecting to secure payment portal...", "Payment", JOptionPane.INFORMATION_MESSAGE));

        actionPanel.add(btnPay);
        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(actionPanel);
    }

    /** Creates a colored summary card with icon, title, and value */
    private JLabel createSummaryCard(JPanel parent, String title, String value, Color color, String icon) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        lblIcon.setForeground(color);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(80, 80, 80));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(color);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(lblTitle);
        textPanel.add(lblValue);

        card.add(lblIcon, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        parent.add(card);
        return lblValue;
    }

    /** Called by StudentPanel after login */
    public void setStudentId(String id) {
        this.studentId = id;
        loadFinanceData();
    }

    private void loadFinanceData() {
        feeBreakdownModel.setRowCount(0);
        txnModel.setRowCount(0);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Thread.sleep(300); // Simulated delay
                return Map.of(
                        "fees", List.of(
                                Map.of("type", "Tuition Fee", "amount", 50000),
                                Map.of("type", "Hostel Fee", "amount", 12000),
                                Map.of("type", "Library Fee", "amount", 2500),
                                Map.of("type", "Lab Fee", "amount", 3000)
                        ),
                        "transactions", List.of(
                                Map.of("date", "2025-08-01", "txn", "TXN1001", "desc", "Tuition Fee", "amt", 25000, "status", "Paid"),
                                Map.of("date", "2025-09-05", "txn", "TXN1002", "desc", "Hostel Fee", "amt", 8000, "status", "Paid"),
                                Map.of("date", "2025-10-10", "txn", "TXN1003", "desc", "Library Fee", "amt", 2500, "status", "Paid")
                        )
                );
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<Map<String, Object>> fees = (List<Map<String, Object>>) data.get("fees");
                    List<Map<String, Object>> txns = (List<Map<String, Object>>) data.get("transactions");

                    double totalCharges = 0;
                    double totalPayments = 0;

                    for (Map<String, Object> f : fees) {
                        double amt = Double.parseDouble(f.get("amount").toString());
                        totalCharges += amt;
                        feeBreakdownModel.addRow(new Object[]{f.get("type"), "₹" + amt});
                    }

                    for (Map<String, Object> t : txns) {
                        double amt = Double.parseDouble(t.get("amt").toString());
                        totalPayments += amt;
                        txnModel.addRow(new Object[]{
                                t.get("date"), t.get("txn"), t.get("desc"),
                                "₹" + amt, t.get("status")
                        });
                    }

                    double due = totalCharges - totalPayments;
                    lblCharges.setText("₹ " + totalCharges);
                    lblPayments.setText("₹ " + totalPayments);
                    lblDue.setText("₹ " + due);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FinancePanel.this,
                            "Error loading finances: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}

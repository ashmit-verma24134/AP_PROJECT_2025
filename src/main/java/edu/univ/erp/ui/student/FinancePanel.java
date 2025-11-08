package edu.univ.erp.ui.student;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class FinancePanel extends JPanel {
    private String studentId;
    private DefaultTableModel feesModel;
    private DefaultTableModel historyModel;
    private JLabel lblTotalDue;
    private JLabel lblPaid;
    private JLabel lblBalance;

    public FinancePanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel header = new JLabel("💰 Student Finances");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        add(header, BorderLayout.NORTH);

        // Main center content: fees summary + history table
        JPanel center = new JPanel(new GridLayout(2, 1, 10, 10));
        center.setBackground(Theme.BACKGROUND);

        // --- Fees Summary Panel ---
        RoundedPanel feesPanel = new RoundedPanel(20);
        feesPanel.setBackground(Theme.CARD_BG);
        feesPanel.setLayout(new BorderLayout(8, 8));
        feesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblFeesTitle = new JLabel("Fee Summary");
        lblFeesTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFeesTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        feesPanel.add(lblFeesTitle, BorderLayout.NORTH);

        String[] feeCols = {"Fee Type", "Amount (₹)"};
        feesModel = new DefaultTableModel(feeCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblFees = new JTable(feesModel);
        tblFees.setRowHeight(26);
        tblFees.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblFees.getTableHeader().setBackground(Theme.PRIMARY);
        tblFees.getTableHeader().setForeground(Color.WHITE);
        tblFees.setGridColor(new Color(230,230,230));

        JScrollPane spFees = new JScrollPane(tblFees);
        feesPanel.add(spFees, BorderLayout.CENTER);

        // Bottom summary labels
        JPanel summary = new JPanel(new GridLayout(3, 2, 6, 4));
        summary.setBackground(Theme.CARD_BG);

        lblTotalDue = new JLabel("Total Due: ₹0");
        lblPaid = new JLabel("Paid: ₹0");
        lblBalance = new JLabel("Balance: ₹0");

        for (JLabel l : new JLabel[]{lblTotalDue, lblPaid, lblBalance}) {
            l.setFont(new Font("Segoe UI", Font.BOLD, 14));
            l.setForeground(Theme.PRIMARY);
        }

        summary.add(new JLabel("Total Due:")); summary.add(lblTotalDue);
        summary.add(new JLabel("Paid:")); summary.add(lblPaid);
        summary.add(new JLabel("Balance:")); summary.add(lblBalance);
        feesPanel.add(summary, BorderLayout.SOUTH);

        // --- Payment History Panel ---
        RoundedPanel historyPanel = new RoundedPanel(20);
        historyPanel.setBackground(Theme.CARD_BG);
        historyPanel.setLayout(new BorderLayout(8, 8));
        historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblHistoryTitle = new JLabel("Payment History");
        lblHistoryTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHistoryTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        historyPanel.add(lblHistoryTitle, BorderLayout.NORTH);

        String[] histCols = {"Date", "Transaction ID", "Description", "Amount (₹)", "Status"};
        historyModel = new DefaultTableModel(histCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblHistory = new JTable(historyModel);
        tblHistory.setRowHeight(26);
        tblHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblHistory.getTableHeader().setBackground(Theme.PRIMARY);
        tblHistory.getTableHeader().setForeground(Color.WHITE);
        tblHistory.setGridColor(new Color(230,230,230));

        JScrollPane spHistory = new JScrollPane(tblHistory);
        historyPanel.add(spHistory, BorderLayout.CENTER);

        // Add to main layout
        center.add(feesPanel);
        center.add(historyPanel);
        add(center, BorderLayout.CENTER);

        // Footer with actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Theme.BACKGROUND);

        JButton btnPay = new JButton("Pay Now");
        JButton btnReceipt = new JButton("Download Receipt");

        btnPay.addActionListener(e -> JOptionPane.showMessageDialog(this, 
            "Payment portal integration coming soon.", "Info", JOptionPane.INFORMATION_MESSAGE));

        btnReceipt.addActionListener(e -> JOptionPane.showMessageDialog(this, 
            "Receipt download feature coming soon.", "Info", JOptionPane.INFORMATION_MESSAGE));

        footer.add(btnReceipt);
        footer.add(btnPay);
        add(footer, BorderLayout.SOUTH);
    }

    /** Called by StudentPanel after login */
    public void setStudentId(String id) {
        this.studentId = id;
        loadFinanceData();
    }

    private void loadFinanceData() {
        // Clear tables first
        feesModel.setRowCount(0);
        historyModel.setRowCount(0);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                // Example: replace this with actual DAO calls later
                // Mock data for now
                Thread.sleep(300);
                return Map.of(
                    "fees", List.of(
                        Map.of("type", "Tuition Fee", "amount", 50000),
                        Map.of("type", "Library Fee", "amount", 2500),
                        Map.of("type", "Lab Fee", "amount", 3000),
                        Map.of("type", "Hostel Fee", "amount", 12000)
                    ),
                    "history", List.of(
                        Map.of("date", "2025-08-01", "txn", "TXN12345", "desc", "Tuition Fee", "amt", 25000, "status", "Paid"),
                        Map.of("date", "2025-09-01", "txn", "TXN56789", "desc", "Library Fee", "amt", 2500, "status", "Paid")
                    )
                );
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<Map<String, Object>> fees = (List<Map<String, Object>>) data.get("fees");
                    List<Map<String, Object>> history = (List<Map<String, Object>>) data.get("history");

                    double totalDue = 0, paid = 0;

                    for (Map<String, Object> f : fees) {
                        double amt = Double.parseDouble(f.get("amount").toString());
                        totalDue += amt;
                        feesModel.addRow(new Object[]{f.get("type"), "₹" + amt});
                    }

                    for (Map<String, Object> h : history) {
                        double amt = Double.parseDouble(h.get("amt").toString());
                        paid += amt;
                        historyModel.addRow(new Object[]{
                            h.get("date"), h.get("txn"), h.get("desc"),
                            "₹" + amt, h.get("status")
                        });
                    }

                    double balance = totalDue - paid;
                    lblTotalDue.setText("₹" + totalDue);
                    lblPaid.setText("₹" + paid);
                    lblBalance.setText("₹" + balance);

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


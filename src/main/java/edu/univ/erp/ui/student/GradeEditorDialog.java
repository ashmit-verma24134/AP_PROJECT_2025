package edu.univ.erp.ui.student;

import edu.univ.erp.service.GradeService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Small modal dialog for editing a component score/max/weight.
 * Calls GradeService.updateScoreAndRecompute(...) on OK (background thread).
 *
 * Usage:
 *   GradeEditorDialog dlg = new GradeEditorDialog(parent, enrollmentId, component, curScore, curMax, curWeight, studentId, () -> {
 *       // reload callback - will be called on EDT after successful update
 *       gradesPanel.reload();
 *       transcriptPanel.reloadForStudent(); // if you want transcript refreshed too
 *   }, gradeServiceInstance);
 *   dlg.setVisible(true);
 */
public class GradeEditorDialog extends JDialog {
    private final JTextField txtScore = new JTextField(8);
    private final JTextField txtMax = new JTextField(8);
    private final JTextField txtWeight = new JTextField(8);
    private final JButton btnOk = new JButton("Save");
    private final JButton btnCancel = new JButton("Cancel");

    private final long enrollmentId;
    private final String component;
    private final long studentId;
    private final Runnable onSuccess;

    // injected service (must be provided by caller)
    private final GradeService gradeService;

    /**
     * Constructor: NOTE the final parameter gradeService must be supplied (a concrete implementation).
     */
    public GradeEditorDialog(Window owner,
                             long enrollmentId, String component,
                             Double curScore, Double curMax, Double curWeight,
                             long studentId,
                             Runnable onSuccess,
                             GradeService gradeService) {
        super(owner, "Edit " + component, ModalityType.APPLICATION_MODAL);
        this.enrollmentId = enrollmentId;
        this.component = component;
        this.studentId = studentId;
        this.onSuccess = onSuccess;
        this.gradeService = gradeService;

        if (curScore != null) txtScore.setText(String.valueOf(curScore));
        if (curMax != null) txtMax.setText(String.valueOf(curMax));
        if (curWeight != null) txtWeight.setText(String.valueOf(curWeight));

        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    // Optional convenience constructor that keeps old signature but forces callers to set gradeService later.
    // I do NOT recommend using this; prefer the constructor that accepts GradeService.
    public GradeEditorDialog(Window owner,
                             long enrollmentId, String component,
                             Double curScore, Double curMax, Double curWeight,
                             long studentId,
                             Runnable onSuccess) {
        this(owner, enrollmentId, component, curScore, curMax, curWeight, studentId, onSuccess, null);
    }

    private void initUI() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; p.add(new JLabel("Component:"), gbc);
        gbc.gridx = 1; p.add(new JLabel(component), gbc);

        gbc.gridx = 0; gbc.gridy = 1; p.add(new JLabel("Score:"), gbc);
        gbc.gridx = 1; p.add(txtScore, gbc);

        gbc.gridx = 0; gbc.gridy = 2; p.add(new JLabel("Max Score:"), gbc);
        gbc.gridx = 1; p.add(txtMax, gbc);

        gbc.gridx = 0; gbc.gridy = 3; p.add(new JLabel("Weight (%):"), gbc);
        gbc.gridx = 1; p.add(txtWeight, gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnOk);
        btns.add(btnCancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(p, BorderLayout.CENTER);
        getContentPane().add(btns, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                txtScore.requestFocusInWindow();
            }
        });
    }

    private void onSave() {
        // parse values (nullable)
        Double score = parseDoubleOrNull(txtScore.getText());
        Double maxScore = parseDoubleOrNull(txtMax.getText());
        Double weight = parseDoubleOrNull(txtWeight.getText());

        // basic validation: if score provided, maxScore must be >0
        if (score != null && (maxScore == null || maxScore <= 0)) {
            JOptionPane.showMessageDialog(this, "Please provide a valid Max Score (> 0) when setting a Score.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Run update in background
        btnOk.setEnabled(false);
        btnCancel.setEnabled(false);
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                if (gradeService == null) {
                    throw new IllegalStateException("GradeService not provided to GradeEditorDialog. Construct with a GradeService instance.");
                }
                gradeService.updateScoreAndRecompute(enrollmentId, component, score, maxScore, weight, studentId);
                return null;
            }

            @Override protected void done() {
                btnOk.setEnabled(true);
                btnCancel.setEnabled(true);
                try {
                    get(); // throw if exception
                    if (onSuccess != null) onSuccess.run();
                    dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(GradeEditorDialog.this,
                            "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException ex) { return null; }
    }
}
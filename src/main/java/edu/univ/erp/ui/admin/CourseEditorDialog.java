package edu.univ.erp.ui.admin;

import edu.univ.erp.service.CourseService;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * CourseEditorDialog refactored to use CourseService.
 * UI no longer talks to DB directly.
 */
public class CourseEditorDialog extends JDialog {

    private final long courseId;
    private final CourseService courseService;

    // course fields
    private final JTextField txtCode = new JTextField(12);
    private final JTextField txtTitle = new JTextField(30);
    private final JTextField txtCredits = new JTextField(6);

    // sections controls
    private final DefaultTableModel sectionsModel;
    private final JTable sectionsTable;
    private final JButton btnAddSection = new JButton("Add Section");
    private final JButton btnEditSection = new JButton("Edit Section");
    private final JButton btnDeleteSection = new JButton("Delete Section");
    private final JButton btnSaveCourse = new JButton("Save Course");
    private final JButton btnClose = new JButton("Close");

    public CourseEditorDialog(Window owner, long courseId, CourseService courseService) {
        this(owner, courseId, courseService, false);
    }

    public CourseEditorDialog(Window owner, long courseId, CourseService courseService, boolean onlyCourseForm) {
        super(owner, "Edit Courses", ModalityType.APPLICATION_MODAL);
        this.courseId = courseId;
        this.courseService = courseService;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(920, onlyCourseForm ? 200 : 420);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(12,12,12,12));
        root.setBackground(Theme.BACKGROUND);
        setContentPane(root);

        JPanel courseForm = buildCourseFormPanel();
        root.add(courseForm, BorderLayout.NORTH);

        // sections model/table
        sectionsModel = new DefaultTableModel(new String[]{"Section ID","Section Code","Day/Time","Room","Capacity","Semester","Year","Instructor"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionsTable.setAutoCreateRowSorter(true);

        if (!onlyCourseForm) {
            JScrollPane sc = new JScrollPane(sectionsTable);
            root.add(sc, BorderLayout.CENTER);

            JPanel secButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            secButtons.setOpaque(false);
            secButtons.add(btnAddSection);
            secButtons.add(btnEditSection);
            secButtons.add(btnDeleteSection);
            root.add(secButtons, BorderLayout.SOUTH);

            btnAddSection.addActionListener(e -> openSectionEditor(-1));
            btnEditSection.addActionListener(e -> {
                int r = sectionsTable.getSelectedRow();
                if (r < 0) { JOptionPane.showMessageDialog(this, "Select a section to edit"); return; }
                int modelRow = sectionsTable.convertRowIndexToModel(r);
                Object idv = sectionsModel.getValueAt(modelRow, 0);
                long sid = idv == null ? -1L : (idv instanceof Number ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv)));
                openSectionEditor(sid);
            });
            btnDeleteSection.addActionListener(e -> deleteSelectedSection());

            sectionsTable.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int r = sectionsTable.getSelectedRow();
                        if (r >= 0) {
                            int modelRow = sectionsTable.convertRowIndexToModel(r);
                            Object idv = sectionsModel.getValueAt(modelRow, 0);
                            long sid = idv == null ? -1L : (idv instanceof Number ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv)));
                            openSectionEditor(sid);
                        }
                    }
                }
            });
        }

        btnClose.addActionListener(e -> dispose());
        btnSaveCourse.addActionListener(e -> saveCourse());

        // load via service
        SwingUtilities.invokeLater(this::loadCourseAndSections);
    }

    private JPanel buildCourseFormPanel() {
        JPanel courseForm = new JPanel(new GridBagLayout());
        courseForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,8,6,8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        courseForm.add(new JLabel("Course Code:"), gc);
        gc.gridx = 1; courseForm.add(txtCode, gc);

        gc.gridx = 2; courseForm.add(new JLabel("Title:"), gc);
        gc.gridx = 3; courseForm.add(txtTitle, gc);

        gc.gridx = 0; gc.gridy = 1;
        courseForm.add(new JLabel("Credits:"), gc);
        gc.gridx = 1; courseForm.add(txtCredits, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 4;
        JPanel courseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        courseButtons.setOpaque(false);
        courseButtons.add(btnSaveCourse);
        courseButtons.add(btnClose);
        courseForm.add(courseButtons, gc);

        return courseForm;
    }

    private void loadCourseAndSections() {
        btnSaveCourse.setEnabled(false);
        btnAddSection.setEnabled(false);
        btnEditSection.setEnabled(false);
        btnDeleteSection.setEnabled(false);

        new SwingWorker<CourseService.CourseResult, Void>() {
            Exception err = null;
            @Override protected CourseService.CourseResult doInBackground() {
                try {
                    return courseService.loadCourseWithSections(courseId);
                } catch (Exception ex) { err = ex; return null; }
            }
            @Override protected void done() {
                btnSaveCourse.setEnabled(true);
                btnAddSection.setEnabled(true);
                btnEditSection.setEnabled(true);
                btnDeleteSection.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to load course/sections: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    CourseService.CourseResult res = get();
                    if (res != null) {
                        txtCode.setText(res.code == null ? "" : res.code);
                        txtTitle.setText(res.title == null ? "" : res.title);
                        txtCredits.setText(res.credits == null ? "" : res.credits);

                        synchronized (sectionsModel) {
                            sectionsModel.setRowCount(0);
                            if (res.sections != null) {
                                for (CourseService.SectionRow sr : res.sections) {
                                    String instr = sr.instructorId == null ? "" : String.valueOf(sr.instructorId);
                                    sectionsModel.addRow(new Object[]{sr.sectionId, sr.sectionCode, sr.dayTime, sr.room, sr.capacity, sr.semester, sr.year, instr});
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to load course data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void saveCourse() {
        final String code = txtCode.getText().trim();
        final String title = txtTitle.getText().trim();
        final String credits = txtCredits.getText().trim();

        btnSaveCourse.setEnabled(false);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try {
                    courseService.saveCourseBasic(courseId, code, title, credits);
                } catch (Exception ex) { err = ex; }
                return null;
            }
            @Override protected void done() {
                btnSaveCourse.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to save course: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Course saved");
                    loadCourseAndSections();
                }
            }
        }.execute();
    }

    private void openSectionEditor(long sid) {
        SectionEditorDialog sed = new SectionEditorDialog(this, courseId, sid, courseService);
        sed.setVisible(true);
        loadCourseAndSections();
    }

    private void deleteSelectedSection() {
        int r = sectionsTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a section to delete"); return; }
        int modelRow = sectionsTable.convertRowIndexToModel(r);
        Object idv = sectionsModel.getValueAt(modelRow, 0);
        long sid = idv == null ? -1L : (idv instanceof Number ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv)));
        int ok = JOptionPane.showConfirmDialog(this, "Delete section?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            Exception err = null;
            @Override protected Boolean doInBackground() {
                try { return courseService.deleteSection(sid); } catch (Exception ex) { err = ex; return false; }
            }
            @Override protected void done() {
                if (err != null) JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to delete section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                loadCourseAndSections();
            }
        }.execute();
    }

    // ---------------- SectionEditorDialog that uses CourseService ----------------
    private static class SectionEditorDialog extends JDialog {
        private final long courseId, sectionId;
        private final CourseService courseService;

        private final JTextField txtSectionCode = new JTextField(12);
        private final JTextField txtDayTime = new JTextField(16);
        private final JTextField txtRoom = new JTextField(12);
        private final JTextField txtCapacity = new JTextField(6);
        private final JTextField txtSemester = new JTextField(8);
        private final JTextField txtYear = new JTextField(6);
        private final JComboBox<CourseService.InstructorItem> cbInstructor = new JComboBox<>();
        private final JButton btnSave = new JButton("Save");
        private final JButton btnCancel = new JButton("Cancel");

        SectionEditorDialog(Window owner, long courseId, long sectionId, CourseService courseService) {
            super(owner, sectionId < 0 ? "Add Section" : "Edit Section", ModalityType.APPLICATION_MODAL);
            this.courseId = courseId;
            this.sectionId = sectionId;
            this.courseService = courseService;

            setSize(560, 320);
            setLocationRelativeTo(owner);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(12,12,12,12));
            p.setBackground(Theme.BACKGROUND);
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6,8,6,8);
            gc.anchor = GridBagConstraints.WEST;

            gc.gridx = 0; gc.gridy = 0; p.add(new JLabel("Section Code:"), gc);
            gc.gridx = 1; p.add(txtSectionCode, gc);

            gc.gridx = 0; gc.gridy = 1; p.add(new JLabel("Day/Time:"), gc);
            gc.gridx = 1; p.add(txtDayTime, gc);

            gc.gridx = 0; gc.gridy = 2; p.add(new JLabel("Room:"), gc);
            gc.gridx = 1; p.add(txtRoom, gc);

            gc.gridx = 0; gc.gridy = 3; p.add(new JLabel("Capacity:"), gc);
            gc.gridx = 1; p.add(txtCapacity, gc);

            gc.gridx = 0; gc.gridy = 4; p.add(new JLabel("Semester:"), gc);
            gc.gridx = 1; p.add(txtSemester, gc);

            gc.gridx = 0; gc.gridy = 5; p.add(new JLabel("Year:"), gc);
            gc.gridx = 1; p.add(txtYear, gc);

            gc.gridx = 0; gc.gridy = 6; p.add(new JLabel("Instructor:"), gc);
            gc.gridx = 1; p.add(cbInstructor, gc);

            JPanel br = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
            br.setOpaque(false);
            br.add(btnSave); br.add(btnCancel);
            gc.gridx = 0; gc.gridy = 7; gc.gridwidth = 2; p.add(br, gc);

            setContentPane(p);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> saveSection());

            SwingUtilities.invokeLater(() -> {
                loadInstructors();
                if (sectionId >= 0) loadSection();
            });
        }

        private void loadInstructors() {
            new SwingWorker<java.util.List<CourseService.InstructorItem>, Void>() {
                Exception err = null;
                @Override protected java.util.List<CourseService.InstructorItem> doInBackground() {
                    try { return courseService.listInstructors(); }
                    catch (Exception ex) { err = ex; return java.util.List.of(new CourseService.InstructorItem(null, "<No Instructor>")); }
                }
                @Override protected void done() {
                    try {
                        java.util.List<CourseService.InstructorItem> list = get();
                        cbInstructor.removeAllItems();
                        for (CourseService.InstructorItem it : list) cbInstructor.addItem(it);
                    } catch (Exception ex) {
                        cbInstructor.removeAllItems();
                        cbInstructor.addItem(new CourseService.InstructorItem(null, "<No Instructor>"));
                    }
                }
            }.execute();
        }

        private void loadSection() {
            new SwingWorker<CourseService.SectionRow, Void>() {
                Exception err = null;
                @Override protected CourseService.SectionRow doInBackground() {
                    try {
                        CourseService.CourseResult cr = courseService.loadCourseWithSections(courseId);
                        if (cr == null || cr.sections == null) return null;
                        for (CourseService.SectionRow s : cr.sections) {
                            if (s.sectionId != null && s.sectionId == sectionId) return s;
                        }
                        return null;
                    } catch (Exception ex) { err = ex; return null; }
                }
                @Override protected void done() {
                    if (err != null) { JOptionPane.showMessageDialog(SectionEditorDialog.this, "Failed to load section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); return; }
                    try {
                        CourseService.SectionRow s = get();
                        if (s != null) {
                            txtSectionCode.setText(s.sectionCode == null ? "" : s.sectionCode);
                            txtDayTime.setText(s.dayTime == null ? "" : s.dayTime);
                            txtRoom.setText(s.room == null ? "" : s.room);
                            txtCapacity.setText(s.capacity == null ? "" : String.valueOf(s.capacity));
                            txtSemester.setText(s.semester == null ? "" : s.semester);
                            txtYear.setText(s.year == null ? "" : String.valueOf(s.year));
                            if (s.instructorId != null) {
                                for (int i=0;i<cbInstructor.getItemCount();++i) {
                                    CourseService.InstructorItem it = cbInstructor.getItemAt(i);
                                    if (it != null && it.id != null && it.id.equals(s.instructorId)) { cbInstructor.setSelectedIndex(i); break; }
                                }
                            } else cbInstructor.setSelectedIndex(0);
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        }

        private void saveSection() {
            final String sCode = txtSectionCode.getText().trim();
            final String dayTime = txtDayTime.getText().trim();
            final String room = txtRoom.getText().trim();
            final String cap = txtCapacity.getText().trim();
            final String sem = txtSemester.getText().trim();
            final String year = txtYear.getText().trim();
            final CourseService.InstructorItem sel = (CourseService.InstructorItem) cbInstructor.getSelectedItem();
            final Long instrId = sel == null ? null : sel.id;

            btnSave.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                Exception err = null;
                @Override protected Boolean doInBackground() {
                    try {
                        CourseService.SectionRow r = new CourseService.SectionRow();
                        r.sectionCode = sCode;
                        r.dayTime = dayTime;
                        r.room = room;
                        r.capacity = cap.isBlank() ? null : Integer.parseInt(cap);
                        r.semester = sem;
                        r.year = year.isBlank() ? null : Integer.parseInt(year);
                        r.instructorId = instrId;

                        if (sectionId < 0) {
                            long newId = courseService.createSection(courseId, r);
                            return newId > 0;
                        } else {
                            return courseService.updateSection(sectionId, r);
                        }
                    } catch (Exception ex) { err = ex; return false; }
                }
                @Override protected void done() {
                    btnSave.setEnabled(true);
                    if (err != null) JOptionPane.showMessageDialog(SectionEditorDialog.this, "Failed to save section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    else dispose();
                }
            }.execute();
        }
    }
}
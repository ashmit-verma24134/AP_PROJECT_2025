package edu.univ.erp.ui.student;

import edu.univ.erp.service.TimetableService;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * TimetablePanel using TimetableService instead of DAO/DB code.
 * 100% same behavior, just decoupled via service.
 */
public class TimetablePanel extends JPanel {

    // ----------------------------------------------------
    // CONSTANTS
    // ----------------------------------------------------
    private static final String[] DAYS = {"Monday","Tuesday","Wednesday","Thursday","Friday"};

    private final int slotMinutes = 30;
    private final String[] TIME_SLOTS;

    private final JPanel gridPanel = new JPanel(new GridBagLayout());
    private final Map<Point, Component> placeholderMap = new HashMap<>();
    private final DefaultListModel<String> debugModel = new DefaultListModel<>();

    private String studentId = null;

    // timetable range
    private final int dayStartHour = 8;
    private final int dayEndHour = 17;

    // NEW SERVICE
    private final TimetableService timetableService = new TimetableService();

    private final RegistrationEventBus.Listener regListener = this::onRegistrationChanged;

    // ----------------------------------------------------
    // CONSTRUCTOR
    // ----------------------------------------------------
    public TimetablePanel() {

        // Build time slot labels
        List<String> labels = new ArrayList<>();
        for (int h = dayStartHour; h < dayEndHour; h++) {
            for (int m = 0; m < 60; m += slotMinutes) {
                int s = h * 60 + m;
                int e = s + slotMinutes;
                labels.add(format(s) + " - " + format(e));
            }
        }
        TIME_SLOTS = labels.toArray(new String[0]);

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel(" Weekly Timetable");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // grid
        gridPanel.setBackground(Theme.BACKGROUND);
        gridPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);

        // debug panel
        JList<String> debugList = new JList<>(debugModel);
        debugList.setFont(Theme.BODY_FONT);
        debugList.setVisibleRowCount(3);
        add(new JScrollPane(debugList), BorderLayout.SOUTH);

        // build initial empty grid
        buildEmptyGrid();

        RegistrationEventBus.get().register(regListener);
    }

    private String format(int minutes) {
        int h = minutes/60;
        int m = minutes%60;
        return String.format("%02d:%02d", h, m);
    }

    // ----------------------------------------------------
    // API
    // ----------------------------------------------------
    public void setStudentId(String id) {
        this.studentId = id;
        loadAndRender();
    }

    public void reloadForStudent() {
        loadAndRender();
    }

    public void setActionsEnabled(boolean enabled) {
        // no-op
    }

    public void dispose() {
        RegistrationEventBus.get().unregister(regListener);
    }

    private void onRegistrationChanged() {
        SwingUtilities.invokeLater(this::loadAndRender);
    }

    // ----------------------------------------------------
    // LOADING (now from SERVICE)
    // ----------------------------------------------------
    private void loadAndRender() {
        if (studentId == null || studentId.isEmpty()) return;

        debugModel.clear();

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String,Object>> doInBackground() throws Exception {
                return timetableService.getStudentSchedule(studentId); // NEW SERVICE
            }

            @Override
            protected void done() {
                try {
                    List<Map<String,Object>> rows = get();
                    render(rows);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    debugModel.addElement("Failed to load timetable: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ----------------------------------------------------
    // GRID + RENDERING (unchanged)
    // ----------------------------------------------------
    private void buildEmptyGrid() {
        gridPanel.removeAll();
        placeholderMap.clear();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6,6,6,6);

        // top-left blank
        gbc.gridx=0; gbc.gridy=0;
        JPanel tl = headerCell("");
        tl.setPreferredSize(new Dimension(140,40));
        gridPanel.add(tl, gbc);

        // time headers
        for (int t=0; t<TIME_SLOTS.length; t++) {
            gbc.gridx = t+1; gbc.gridy = 0;
            JPanel h = headerCell(TIME_SLOTS[t]);
            h.setPreferredSize(new Dimension(100,36));
            gridPanel.add(h, gbc);
        }

        // days + placeholders
        for (int d=0; d<DAYS.length; d++) {

            gbc.gridx = 0; gbc.gridy = d+1;
            JPanel dh = dayHeader(DAYS[d]);
            dh.setPreferredSize(new Dimension(140,80));
            gridPanel.add(dh, gbc);

            for (int t=0; t<TIME_SLOTS.length; t++) {
                gbc.gridx = t+1; gbc.gridy = d+1;

                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(Theme.SURFACE);
                cell.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER));
                cell.setPreferredSize(new Dimension(100,80));

                gridPanel.add(cell, gbc);
                placeholderMap.put(new Point(d,t), cell);
            }
        }

        revalidate();
        repaint();
    }

    private JPanel headerCell(String txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.PRIMARY_DARK);
        JLabel lbl = new JLabel(txt, SwingConstants.CENTER);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(Theme.BODY_BOLD);
        p.add(lbl);
        return p;
    }

    private JPanel dayHeader(String txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.PRIMARY_LIGHT);
        JLabel lbl = new JLabel(txt, SwingConstants.CENTER);
        lbl.setForeground(Theme.NEUTRAL_DARK);
        lbl.setFont(Theme.BODY_BOLD);
        p.add(lbl);
        return p;
    }

    private void render(List<Map<String,Object>> rows) {
        buildEmptyGrid();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6,6,6,6);

        Map<Point, JPanel> occupancy = new HashMap<>();

        for (Map<String,Object> r : rows) {

            String code = Objects.toString(r.get("course_code"), "");
            String title = Objects.toString(r.get("course_title"), "");
            String room = Objects.toString(r.get("room"), "");
            String instructor = Objects.toString(r.get("instructor"), "");
            String dayTime = Objects.toString(r.get("day_time"), "").trim();
            long sectionId = r.get("section_id") == null ? -1L : ((Number)r.get("section_id")).longValue();

            boolean placed = placeCourse(r, occupancy, gbc);

            if (!placed) {
                debugModel.addElement("NOT PLACED: " + code + " time=" + dayTime);
            }
        }

        revalidate();
        repaint();
    }

    // ----------------------------------------------------
    // EXACT SAME PARSING / BLOCK UI CODE
    // ----------------------------------------------------
    private boolean placeCourse(Map<String,Object> r, Map<Point,JPanel> occ, GridBagConstraints gbc) {

        String code = Objects.toString(r.get("course_code"), "");
        String title = Objects.toString(r.get("course_title"), "");
        String room = Objects.toString(r.get("room"), "");
        String instr = Objects.toString(r.get("instructor"), "");
        String dayTime = Objects.toString(r.get("day_time"), "").trim();

        boolean placedAny = false;

        String[] parts = dayTime.split("[;,]");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            List<ParsedSlot> parsed = parseMulti(part);
            if (parsed.isEmpty()) continue;

            for (ParsedSlot ps : parsed) {
                int d = dayToIndex(ps.day);
                if (d < 0) continue;

                Integer startIdx = timeToSlot(ps.start);
                Integer endIdx = (ps.end == null)
                        ? (startIdx == null ? null : startIdx + 1)
                        : timeToEndSlot(ps.end);

                if (startIdx == null || endIdx == null) continue;

                int span = Math.max(1, endIdx - startIdx);

                // Remove placeholders
                for (int i=0;i<span;i++) {
                    Point p = new Point(d, startIdx+i);
                    Component ph = placeholderMap.remove(p);
                    if (ph != null) gridPanel.remove(ph);
                }

                // Stack panel for overlapping courses
                Point startPoint = new Point(d, startIdx);
                JPanel stack = occ.get(startPoint);
                if (stack == null) {
                    stack = new JPanel();
                    stack.setOpaque(false);
                    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
                    stack.setBorder(new EmptyBorder(2,4,2,4));

                    gbc.gridx = startIdx + 1;
                    gbc.gridy = d + 1;
                    gbc.gridwidth = span;
                    gbc.gridheight = 1;

                    gridPanel.add(stack, gbc);
                    occ.put(startPoint, stack);
                }

                // UI block
                JPanel block = courseBlock(code, title, room, ps.start, ps.end, instr);
                block.setAlignmentX(Component.LEFT_ALIGNMENT);
                stack.add(block);

                placedAny = true;
            }
        }

        return placedAny;
    }

    private JPanel courseBlock(String code, String title, String room, String start, String end, String instr) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(new Color(24,160,142));

        if (end == null) end = addMinutes(start, slotMinutes);
        String timeRange = start + " — " + end;

        String html = "<html><div style='padding:4px;'>"
                + "<b style='font-size:13px;'>" + code + "</b><br/>"
                + "<span style='font-size:11px;'>" + title + "</span><br/>"
                + "<span style='font-size:10px;color:#e8fff8;'>"
                + (room.isEmpty() ? "" : room + " — ")
                + timeRange
                + "</span>"
                + (instr.isEmpty() ? "" : "<br/><span style='font-size:10px;color:#dbe;'>" + instr + "</span>")
                + "</div></html>";

        JLabel lbl = new JLabel(html);
        lbl.setForeground(Color.WHITE);
        p.add(lbl);

        return p;
    }

    // ----------------------------------------------------
    // PARSING HELPERS (unchanged)
    // ----------------------------------------------------
    private static class ParsedSlot {
        final String day;
        final String start;
        final String end;
        ParsedSlot(String d, String s, String e) { day=d; start=s; end=e; }
    }

    private List<ParsedSlot> parseMulti(String input) {
        input = input.trim();
        if (input.isEmpty()) return Collections.emptyList();

        // find where time begins
        int idx = -1;
        for (int i=0;i<input.length();i++)
            if (Character.isDigit(input.charAt(i))) { idx = i; break; }

        if (idx < 0) return Collections.emptyList();

        String dayStr = input.substring(0,idx).trim();
        String timeStr = input.substring(idx).trim().replaceAll("\\s*[–-]\\s*","-");

        String start, end=null;
        if (timeStr.contains("-")) {
            String[] x = timeStr.split("-",2);
            start = norm(x[0]);
            end   = norm(x[1]);
        } else {
            start = norm(timeStr);
        }

        String[] tokens = dayStr.split("\\s*/\\s*|\\s*,\\s*|\\s+and\\s+|\\s*&\\s*");

        List<ParsedSlot> out = new ArrayList<>();
        for (String t : tokens) {
            t = t.trim();
            if (!t.isEmpty()) out.add(new ParsedSlot(expand(t), start, end));
        }
        return out;
    }

    private String expand(String t) {
        t = t.toLowerCase();
        if (t.startsWith("mon")) return "Monday";
        if (t.startsWith("tue")) return "Tuesday";
        if (t.startsWith("wed")) return "Wednesday";
        if (t.startsWith("thu")) return "Thursday";
        if (t.startsWith("fri")) return "Friday";
        return t;
    }

    private String norm(String raw) {
        raw = raw.trim();
        if (!raw.contains(":")) return String.format("%02d:00", Integer.parseInt(raw));
        String[] p = raw.split(":");
        return String.format("%02d:%02d", Integer.parseInt(p[0]), (p.length>1?Integer.parseInt(p[1]):0));
    }

    private int toMinutes(String hh) {
        String[] p = hh.split(":");
        return Integer.parseInt(p[0])*60 + Integer.parseInt(p[1]);
    }

    private String addMinutes(String hh, int a) {
        int m = toMinutes(hh) + a;
        return String.format("%02d:%02d", (m/60)%24, m%60);
    }

    private Integer timeToSlot(String start) {
        int m = toMinutes(start);
        int base = dayStartHour * 60;
        if (m < base) return null;
        int idx = (m - base) / slotMinutes;
        return (idx >= 0 && idx < TIME_SLOTS.length) ? idx : null;
    }

    private Integer timeToEndSlot(String end) {
        int m = toMinutes(end);
        int base = dayStartHour * 60;
        if (m <= base) return 0;
        int rel = m - base;
        int idx = (rel + slotMinutes - 1) / slotMinutes;
        return Math.min(Math.max(idx,0), TIME_SLOTS.length);
    }

    private int dayToIndex(String d) {
        for (int i=0;i<DAYS.length;i++)
            if (DAYS[i].equalsIgnoreCase(d))
                return i;
        return -1;
    }
}

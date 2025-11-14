package edu.univ.erp.ui.student;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.util.*;
import java.util.List;

/**
 * TimetablePanel with configurable slot length (30 or 60 minutes).
 * Expects StudentDao.getStudentSchedule(studentId) to return List<Map<String,Object>>
 * with keys: section_id, course_code, course_title, day_time, room, status, instructor
 */
public class TimetablePanel extends JPanel {
    // days
    private static final String[] DAYS = {"Monday","Tuesday","Wednesday","Thursday","Friday"};

    // configurable slot length (30 or 60). Change to 30 for half-hour slots.
    private final int slotMinutes = 30; // <- set to 30 or 60 as you prefer

    // derived time slots between 08:00 and 17:00
    private final String[] TIME_SLOTS;

    private final JPanel gridPanel = new JPanel(new GridBagLayout());
    private final Map<Point, Component> placeholderMap = new HashMap<>(); // (dayIndex, slotIndex) -> placeholder
    private String studentId = null;
    private final DefaultListModel<String> debugModel = new DefaultListModel<>();

    // timetable window range
    private final int dayStartHour = 8;   // 08:00
    private final int dayEndHour = 17;    // 17:00 (exclusive)

    public TimetablePanel() {
        // build slot labels from slotMinutes
        List<String> labels = new ArrayList<>();
        for (int h = dayStartHour; h < dayEndHour; h++) {
            for (int m = 0; m < 60; m += slotMinutes) {
                int startMin = h * 60 + m;
                int endMin = startMin + slotMinutes;
                labels.add(formatSlot(startMin) + " - " + formatSlot(endMin));
            }
        }
        TIME_SLOTS = labels.toArray(new String[0]);

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Header bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("📅 Weekly Timetable");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // grid wrapper
        gridPanel.setBackground(Theme.BACKGROUND);
        gridPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // debug list for parsing issues
        JList<String> debugList = new JList<>(debugModel);
        debugList.setVisibleRowCount(3);
        debugList.setFont(Theme.BODY_FONT);
        add(new JScrollPane(debugList), BorderLayout.SOUTH);

        buildEmptyGrid();
    }

    /** helper to format minutes-of-day to H:mm */
    private static String formatSlot(int minutesOfDay) {
        int h = (minutesOfDay / 60) % 24;
        int m = minutesOfDay % 60;
        return String.format("%02d:%02d", h, m);
    }

    /** set student id and trigger load */
    public void setStudentId(String id) {
        this.studentId = id;
        loadAndRender();
    }

    /** public reload hook */
    public void reloadForStudent() {
        loadAndRender();
    }

    /** optional: no-op so callers compile */
    public void setActionsEnabled(boolean enabled) { /* no-op for now */ }

    /** build empty grid placeholders */
    private void buildEmptyGrid() {
        gridPanel.removeAll();
        placeholderMap.clear();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6,6,6,6);

        // top-left blank
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
        JPanel topLeft = createHeaderCell("");
        topLeft.setPreferredSize(new Dimension(140, 40));
        gridPanel.add(topLeft, gbc);

        // time headers
        for (int t = 0; t < TIME_SLOTS.length; t++) {
            gbc.gridx = t + 1; gbc.gridy = 0;
            JPanel h = createHeaderCell(TIME_SLOTS[t]);
            h.setPreferredSize(new Dimension(100, 36));
            gridPanel.add(h, gbc);
        }

        // day rows + placeholders
        for (int d = 0; d < DAYS.length; d++) {
            // day header
            gbc.gridx = 0; gbc.gridy = d + 1;
            JPanel dayHeader = createDayHeader(DAYS[d]);
            dayHeader.setPreferredSize(new Dimension(140, 80));
            gridPanel.add(dayHeader, gbc);

            // placeholders
            for (int t = 0; t < TIME_SLOTS.length; t++) {
                gbc.gridx = t + 1; gbc.gridy = d + 1; gbc.gridwidth = 1; gbc.gridheight = 1;
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(Theme.SURFACE);
                cell.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER));
                cell.setPreferredSize(new Dimension(100, 80));
                gridPanel.add(cell, gbc);
                placeholderMap.put(new Point(d, t), cell);
            }
        }

        revalidate();
        repaint();
    }

    /** load from DB and render */
    private void loadAndRender() {
        if (studentId == null || studentId.trim().isEmpty()) return;
        debugModel.clear();

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String,Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getStudentSchedule(studentId);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Map<String,Object>> rows = get();
                    renderRows(rows);
                } catch (Exception e) {
                    e.printStackTrace();
                    debugModel.addElement("Failed to load timetable: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Render DAO rows into the grid */
    private void renderRows(List<Map<String,Object>> rows) {
        buildEmptyGrid();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6,6,6,6);

        Map<Point, JPanel> occupancy = new HashMap<>(); // startPoint -> stack panel

        for (Map<String,Object> r : rows) {
            String dayTime = Objects.toString(r.get("day_time"), "").trim();
            String code = Objects.toString(r.get("course_code"), "");
            String title = Objects.toString(r.get("course_title"), "");
            String room = Objects.toString(r.get("room"), "");
            String instructor = Objects.toString(r.get("instructor"), "");
            long sectionId = r.get("section_id") == null ? -1L : ((Number)r.get("section_id")).longValue();

            String[] parts = dayTime.split("[;,]");
            boolean placedAny = false;

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;

                List<ParsedSlot> parsed = parseDayTimeMulti(part);
                if (parsed.isEmpty()) {
                    debugModel.addElement(String.format("UNPARSED: {section=%d, code=%s, day_time=%s}", sectionId, code, part));
                    continue;
                }

                for (ParsedSlot ps : parsed) {
                    int dayIndex = dayStringToIndex(ps.day);
                    if (dayIndex < 0 || dayIndex >= DAYS.length) {
                        debugModel.addElement("Unknown day token: " + ps.day + " (orig: " + part + ")");
                        continue;
                    }

                    Integer startIndex = timeToSlotIndex(ps.startTime); // floor
                    // >>> FIX: treat missing endTime as one slot (start+1)
                    Integer endIndexExclusive;
                    if (ps.endTime == null) {
                        // if startIndex is null we'll detect below
                        endIndexExclusive = (startIndex == null) ? null : (startIndex + 1);
                    } else {
                        endIndexExclusive = timeToSlotEndIndexExclusive(ps.endTime); // exclusive
                    }

                    if (startIndex == null || endIndexExclusive == null) {
                        debugModel.addElement("Time not mapped: " + part + " for student=" + studentId);
                        continue;
                    }

                    int span = Math.max(1, endIndexExclusive - startIndex);
                    if (startIndex + span > TIME_SLOTS.length) {
                        span = TIME_SLOTS.length - startIndex;
                        if (span < 1) span = 1;
                    }

                    // remove placeholders spanned
                    for (int s = 0; s < span; s++) {
                        Point p = new Point(dayIndex, startIndex + s);
                        Component ph = placeholderMap.remove(p);
                        if (ph != null) gridPanel.remove(ph);
                    }

                    Point startPoint = new Point(dayIndex, startIndex);
                    JPanel stack = occupancy.get(startPoint);
                    if (stack == null) {
                        stack = new JPanel();
                        stack.setOpaque(false);
                        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
                        stack.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));

                        gbc.gridx = startIndex + 1;
                        gbc.gridy = dayIndex + 1;
                        gbc.gridwidth = span;
                        gbc.gridheight = 1;
                        gridPanel.add(stack, gbc);
                        occupancy.put(startPoint, stack);
                    }

                    JPanel block = createCourseBlock(code, title, room, ps.startTime, ps.endTime, instructor);
                    block.setAlignmentX(Component.LEFT_ALIGNMENT);
                    block.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
                    stack.add(block);

                    placedAny = true;
                }
            }

            if (!placedAny) {
                debugModel.addElement(String.format("NOT PLACED: section=%d code=%s time=%s", sectionId, code, dayTime));
            }
        }

        revalidate();
        repaint();
    }

    // ----------------- UI helpers -----------------

    private JPanel createHeaderCell(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.PRIMARY_DARK);
        panel.setBorder(BorderFactory.createMatteBorder(0,0,1,1, Theme.BACKGROUND));
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(Theme.BODY_BOLD);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDayHeader(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createMatteBorder(0,0,1,1, Theme.BACKGROUND));
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Theme.NEUTRAL_DARK);
        label.setFont(Theme.BODY_BOLD);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCourseBlock(String code, String title, String room, String start, String end, String instructor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(24,160,142));
        p.setOpaque(true);

        final String timeRange;
        if (start != null) {
            String s = start;
            String e = (end == null ? addMinutesToString(start, slotMinutes) : end);
            timeRange = s + " — " + e;
        } else {
            timeRange = "";
        }

        String html = "<html><div style='padding:4px;'>" +
                "<b style='font-size:13px;'>" + escapeHtml(code) + "</b><br/>" +
                "<span style='font-size:11px;'>" + escapeHtml(title) + "</span><br/>" +
                "<span style='font-size:10px;color:#e8fff8;'>" +
                    (room == null || room.equals("null") ? "" : escapeHtml(room) + " — ") +
                    escapeHtml(timeRange) +
                "</span>" +
                (instructor == null || instructor.isEmpty() ? "" :
                        ("<br/><span style='font-size:10px;color:#dbe;'>" + escapeHtml(instructor) + "</span>")
                ) +
                "</div></html>";

        JLabel label = new JLabel(html);
        label.setForeground(Color.WHITE);
        p.add(label, BorderLayout.CENTER);

        p.setToolTipText(code + " | " + title + (room == null ? "" : " | " + room) + (timeRange.isEmpty() ? "" : " | " + timeRange));

        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { p.setBackground(Theme.PRIMARY); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { p.setBackground(new Color(24,160,142)); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                JOptionPane.showMessageDialog(p,
                        code + " - " + title + "\n" +
                                (room == null ? "" : ("Room: " + room + "\n")) +
                                (instructor == null ? "" : ("Instructor: " + instructor + "\n")) +
                                (timeRange.isEmpty() ? "" : ("Time: " + timeRange)),
                        "Course Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        return p;
    }

    // ----------------- parsing / time helpers -----------------

    // Parse multi-day/time strings, returns list of ParsedSlot (one per day)
    private List<ParsedSlot> parseDayTimeMulti(String input) {
        input = input.trim();
        if (input.isEmpty()) return Collections.emptyList();

        // find first digit (start of time)
        int idx = -1;
        for (int i=0;i<input.length();i++) {
            if (Character.isDigit(input.charAt(i))) { idx = i; break; }
        }
        if (idx == -1) return Collections.emptyList();

        String dayPart = input.substring(0, idx).trim();
        String timePart = input.substring(idx).trim();
        if (dayPart.isEmpty() || timePart.isEmpty()) return Collections.emptyList();

        timePart = timePart.replaceAll("\\s*[–-]\\s*", "-");

        String startStr, endStr = null;
        if (timePart.contains("-")) {
            String[] ts = timePart.split("-",2);
            startStr = normalizeTime(ts[0].trim());
            endStr = normalizeTime(ts[1].trim());
        } else {
            startStr = normalizeTime(timePart);
        }

        String[] dayTokens = dayPart.split("\\s*/\\s*|\\s*,\\s*|\\s+and\\s+|\\s*&\\s*");
        List<ParsedSlot> out = new ArrayList<>();
        for (String dt : dayTokens) {
            dt = dt.trim();
            if (dt.isEmpty()) continue;
            String full = expandDayToken(dt);
            out.add(new ParsedSlot(full, startStr, endStr));
        }
        return out;
    }

    private String expandDayToken(String token) {
        String t = token.toLowerCase();
        if (t.startsWith("mon")) return "Monday";
        if (t.startsWith("tue")) return "Tuesday";
        if (t.startsWith("wed")) return "Wednesday";
        if (t.startsWith("thu") || t.startsWith("thur")) return "Thursday";
        if (t.startsWith("fri")) return "Friday";
        return token;
    }

    private static class ParsedSlot {
        final String day;
        final String startTime; // HH:mm
        final String endTime;   // HH:mm or null
        ParsedSlot(String d, String s, String e) { day=d; startTime=s; endTime=e; }
    }

    private String normalizeTime(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;
        if (!raw.contains(":")) {
            int h = Integer.parseInt(raw);
            return String.format("%02d:00", h);
        } else {
            String[] p = raw.split(":");
            int h = Integer.parseInt(p[0]);
            int m = p.length>1 ? Integer.parseInt(p[1]) : 0;
            return String.format("%02d:%02d", h, m);
        }
    }

    private int timeToMinutes(String hhmm) {
        if (hhmm == null) return 0;
        String[] p = hhmm.split(":");
        int h = Integer.parseInt(p[0]);
        int m = p.length>1 ? Integer.parseInt(p[1]) : 0;
        return h*60 + m;
    }

    // compute slot index (floor) for a start time
    private Integer timeToSlotIndex(String hhmm) {
        if (hhmm == null) return null;
        int mins = timeToMinutes(hhmm);
        int globalStart = dayStartHour * 60;
        if (mins < globalStart) return null;
        int idx = (mins - globalStart) / slotMinutes;
        if (idx < 0 || idx >= TIME_SLOTS.length) return null;
        return idx;
    }

    // compute end index exclusive: ceil(endMin/slotMinutes) - globalStart
    // if end is null -> assume start + slotMinutes (handled by caller)
    private Integer timeToSlotEndIndexExclusive(String endHhmm) {
        if (endHhmm == null) return null;
        int endMin = timeToMinutes(endHhmm);
        int globalStart = dayStartHour * 60;
        if (endMin <= globalStart) return 0;
        // ceil division into slots, relative to globalStart
        int rel = endMin - globalStart;
        int endIndexExclusive = (rel + slotMinutes - 1) / slotMinutes; // ceil
        if (endIndexExclusive < 0) endIndexExclusive = 0;
        if (endIndexExclusive > TIME_SLOTS.length) endIndexExclusive = TIME_SLOTS.length;
        return endIndexExclusive;
    }

    private int dayStringToIndex(String day) {
        for (int i=0;i<DAYS.length;i++) if (DAYS[i].equalsIgnoreCase(day)) return i;
        String d = day.toLowerCase();
        if (d.startsWith("mon")) return 0;
        if (d.startsWith("tue")) return 1;
        if (d.startsWith("wed")) return 2;
        if (d.startsWith("thu")) return 3;
        if (d.startsWith("fri")) return 4;
        return -1;
    }

    private String addMinutesToString(String hhmm, int minutesToAdd) {
        int min = timeToMinutes(hhmm) + minutesToAdd;
        int h = (min/60)%24;
        int m = min%60;
        return String.format("%02d:%02d", h, m);
    }

    private String escapeHtml(String s) {
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}

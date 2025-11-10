package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CourseDistributionPanel extends JPanel {

    public CourseDistributionPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("📚 Course Distribution");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(0, 2, 20, 20));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(20, 40, 40, 40));

        content.add(createCard("Core Courses", "48", "Main required modules"));
        content.add(createCard("Electives", "82", "Offered across departments"));
        content.add(createCard("Labs", "36", "Practical sessions"));
        content.add(createCard("Projects", "12", "Capstone & Research"));

        add(content, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, String value, String desc) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel t = new JLabel(title);
        t.setFont(Theme.BODY_BOLD);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(Theme.PRIMARY_DARK);
        JLabel d = new JLabel(desc);
        d.setFont(Theme.BODY_FONT);
        d.setForeground(Theme.NEUTRAL_MED);

        JPanel text = new JPanel(new GridLayout(3, 1));
        text.setBackground(Theme.SURFACE);
        text.add(t);
        text.add(v);
        text.add(d);

        card.add(text, BorderLayout.CENTER);
        return card;
    }
}

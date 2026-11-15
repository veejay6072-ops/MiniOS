import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/** A desktop shortcut: a colored tile with a badge and a label. Double-click to open. */
public class DesktopIcon extends JPanel {
    private final String badge;
    private final Color color;
    private Icon art;   // optional custom artwork drawn instead of the colored badge tile

    public DesktopIcon(String title, String badge, Color color, Runnable onOpen) {
        this.badge = badge;
        this.color = color;
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);

        JLabel name = new JLabel(title, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                // White text with an XP-style drop shadow, wrapped onto up to two lines.
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String[] lines = wrap(getText(), fm, getWidth() - 4);
                int y = fm.getAscent() + 1;
                for (String line : lines) {
                    int x = (getWidth() - fm.stringWidth(line)) / 2;
                    g2.setColor(new Color(0, 0, 0, 190));
                    g2.drawString(line, x + 1, y + 1);
                    g2.setColor(Color.WHITE);
                    g2.drawString(line, x, y);
                    y += fm.getHeight();
                }
                g2.dispose();
            }
        };
        name.setPreferredSize(new Dimension(0, 38));
        name.setForeground(Color.WHITE);
        name.setFont(UiKit.UI_FONT);
        add(name, BorderLayout.SOUTH);

        if ("Segmentation".equals(title)) {
            Icon segArt = SegmentationIcon.create(52);
            if (segArt != null) this.art = segArt;
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onOpen.run();
            }
        });
    }

    public void setArt(Icon art) {
        this.art = art;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = 52;
        int x = (getWidth() - size) / 2;
        if (art != null) {
            art.paintIcon(this, g2, x, 4);
            g2.dispose();
            return;
        }
        g2.setColor(color);
        g2.fillRoundRect(x, 4, size, size, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (size - fm.stringWidth(badge)) / 2;
        int textY = 4 + (size + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(badge, textX, textY);
        g2.dispose();
    }

    /** Splits text into at most two lines that fit the width (a long single word is broken with a hyphen). */
    private static String[] wrap(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return new String[]{text};
        int split = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ' && fm.stringWidth(text.substring(0, i)) <= width) split = i;
        }
        if (split < 0) {
            int cut = 1;
            while (cut < text.length() - 1 && fm.stringWidth(text.substring(0, cut + 1) + "-") <= width) cut++;
            return new String[]{text.substring(0, cut) + "-", clip(text.substring(cut), fm, width)};
        }
        return new String[]{text.substring(0, split), clip(text.substring(split + 1), fm, width)};
    }

    private static String clip(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "...") > width) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
package opersys.bankers;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * BANK ICON = the app logo, drawn with code (no image file needed):
 * a blue rounded square, a gold shield, and a navy check mark on top.
 */
public final class BankIcon {

    // the logo colors
    private static final Color BLUE_TOP = new Color(0x2F6FD6);
    private static final Color BLUE_BOTTOM = new Color(0x0E3A8C);
    private static final Color GOLD = new Color(0xF5B800);
    private static final Color NAVY = new Color(0x0B2E6B);

    private BankIcon() {
    }

    /** The logo at any size you want (it is always a square, so width = height). */
    public static Icon create(int size) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);   // so the drawing code below can always start at (0, 0)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                paint(g2, size);
                g2.dispose();
            }
        };
    }

    /** The size used for the desktop shortcut. */
    public static Icon desktopIcon() {
        return create(64);
    }

    /** Draws the logo. Every position is a fraction of the size (s), so it looks the same at any size. */
    private static void paint(Graphics2D g2, int s) {
        // blue rounded square, a bit lighter at the top
        double arc = s * 0.22;
        g2.setPaint(new GradientPaint(0, 0, BLUE_TOP, 0, s, BLUE_BOTTOM));
        g2.fill(new RoundRectangle2D.Double(0, 0, s, s, arc, arc));

        // gold shield (flat top with a little curve, pointed bottom)
        Path2D shield = new Path2D.Double();
        shield.moveTo(s * 0.24, s * 0.26);
        shield.quadTo(s * 0.50, s * 0.19, s * 0.76, s * 0.26);
        shield.lineTo(s * 0.76, s * 0.48);
        shield.quadTo(s * 0.72, s * 0.70, s * 0.50, s * 0.82);
        shield.quadTo(s * 0.28, s * 0.70, s * 0.24, s * 0.48);
        shield.closePath();
        g2.setColor(GOLD);
        g2.fill(shield);

        // navy check mark in the middle of the shield
        Path2D check = new Path2D.Double();
        check.moveTo(s * 0.37, s * 0.50);
        check.lineTo(s * 0.46, s * 0.60);
        check.lineTo(s * 0.63, s * 0.40);
        g2.setColor(NAVY);
        g2.setStroke(new BasicStroke((float) (s * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(check);
    }
}
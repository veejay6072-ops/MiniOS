package opersys.paging;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** App logo for Paging: pink rounded square, white house, and a 2 x 2 grid of pages (one outlined). */
public final class PagingIcon {

    private static final Color PINK_TOP = new Color(0xF46E8C);
    private static final Color PINK_BOTTOM = new Color(0xDB1F5B);
    private static final Color WHITE = Color.WHITE;

    private PagingIcon() {
    }

    /** The logo at a given size (width and height are the same). */
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
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                paint(g2, size);
                g2.dispose();
            }
        };
    }

    /** The size used on the desktop. */
    public static Icon desktopIcon() {
        return create(64);
    }

    private static void paint(Graphics2D g2, int s) {
        // Pink rounded background
        double arc = s * 0.22;
        g2.setPaint(new GradientPaint(0, 0, PINK_TOP, 0, s, PINK_BOTTOM));
        g2.fill(new RoundRectangle2D.Double(0, 0, s, s, arc, arc));

        // White house: roof and body
        Path2D roof = new Path2D.Double();
        roof.moveTo(s * 0.50, s * 0.18);
        roof.lineTo(s * 0.84, s * 0.46);
        roof.lineTo(s * 0.16, s * 0.46);
        roof.closePath();
        g2.setColor(WHITE);
        g2.fill(roof);
        g2.fill(new Rectangle2D.Double(s * 0.22, s * 0.44, s * 0.56, s * 0.38));

        // Pink pages inside the house: 2 x 2 grid, bottom-right one is outlined only
        double pw = s * 0.17;
        double ph = s * 0.15;
        double ax = s * 0.31;
        double bx = s * 0.52;
        double ay = s * 0.53;
        double by = s * 0.70;
        double pArc = s * 0.04;
        g2.setColor(PINK_BOTTOM);
        g2.fill(new RoundRectangle2D.Double(ax, ay, pw, ph, pArc, pArc));
        g2.fill(new RoundRectangle2D.Double(bx, ay, pw, ph, pArc, pArc));
        g2.fill(new RoundRectangle2D.Double(ax, by, pw, ph, pArc, pArc));

        g2.setStroke(new BasicStroke((float) Math.max(1.2, s * 0.035)));
        g2.draw(new RoundRectangle2D.Double(bx + s * 0.02, by + s * 0.02, pw - s * 0.04, ph - s * 0.04,
                pArc, pArc));
    }
}
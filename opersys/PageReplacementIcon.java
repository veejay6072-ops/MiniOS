import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The Page Replacement icon: blue rounded tile, a white folder, and a blue refresh arrow on the folder.
 * Separate from the GUI so the desktop and Start menu can use it without touching the app's code.
 */
public final class PageReplacementIcon {
    private static final Color BLUE_TOP = new Color(70, 150, 255);
    private static final Color BLUE_BOTTOM = new Color(28, 84, 214);
    private static final Color ARROW = new Color(30, 110, 230);

    private PageReplacementIcon() {
    }

    /** The icon at the size used on the desktop. */
    public static Icon desktopIcon() {
        return icon(64);   // same size as PagingIcon.desktopIcon()
    }

    /** The same icon at any size (used by the Start menu and the window title bar). */
    public static Icon create(int size) {
        return icon(size);
    }

    /** The icon at any size. */
    public static Icon icon(int size) {
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
                drawIcon(g2, size);
                g2.dispose();
            }
        };
    }

    private static void drawIcon(Graphics2D g2, int size) {
        double s = size;

        // blue rounded tile, full size
        g2.setPaint(new GradientPaint(0, 0, BLUE_TOP, 0, (float) s, BLUE_BOTTOM));
        g2.fill(new RoundRectangle2D.Double(0, 0, s, s, s * 0.22, s * 0.22));

        // white folder: tab on the top-left, body below it
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(s * 0.22, s * 0.24, s * 0.30, s * 0.14, s * 0.05, s * 0.05));
        g2.fill(new RoundRectangle2D.Double(s * 0.22, s * 0.32, s * 0.56, s * 0.46, s * 0.07, s * 0.07));

        // blue refresh arrow inside the folder
        double cx = s * 0.50;
        double cy = s * 0.55;
        double r = s * 0.14;
        g2.setColor(ARROW);
        g2.setStroke(new BasicStroke((float) Math.max(1.5, s * 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 40, 280, Arc2D.OPEN));

        // arrowhead at the end of the arc (angle 40 degrees, counter-clockwise start)
        double a = Math.toRadians(40);
        double tipX = cx + r * Math.cos(a);
        double tipY = cy - r * Math.sin(a);
        double tx = Math.sin(a);     // tangent direction (pointing "up" the arc)
        double ty = Math.cos(a);
        double nx = Math.cos(a);     // normal direction
        double ny = -Math.sin(a);
        double len = s * 0.11;
        double wid = s * 0.09;
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX + tx * len, tipY - ty * len);
        head.lineTo(tipX + nx * wid, tipY + ny * wid);
        head.lineTo(tipX - nx * wid, tipY - ny * wid);
        head.closePath();
        g2.fill(head);
    }
}
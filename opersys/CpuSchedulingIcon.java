import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * The CPU Scheduling icon: red rounded tile, white chip with pins, dark core and three
 * colored process bars. Separate from the GUI so the desktop and Start menu can use it
 * without touching the app's code.
 */
public final class CpuSchedulingIcon {
    private CpuSchedulingIcon() {
    }

    /** The CPU Scheduling icon at the size used on the desktop. */
    public static Icon desktopIcon() {
        return icon(64);   // same size as PagingIcon.desktopIcon()
    }

    /** The same icon at any size (used by the Start menu). */
    public static Icon create(int size) {
        return icon(size);
    }

    /** Red rounded tile with a white chip, a dark core and three colored process bars. */
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

        // red rounded tile
        g2.setPaint(new GradientPaint(0, 0, new Color(236, 62, 58), 0, (float) s, new Color(186, 22, 26)));
        g2.fill(new RoundRectangle2D.Double(0, 0, s, s, s * 0.22, s * 0.22));   // full size, same as PagingIcon

        // white pins: three on each side of the chip
        g2.setColor(Color.WHITE);
        double pinW = s * 0.045;
        double chipX = s * 0.27;
        double chipW = s * 0.46;
        double pinLen = s * 0.12;
        for (int i = 0; i < 3; i++) {
            double p = chipX + chipW * (i + 1) / 4.0 - pinW / 2;
            g2.fill(new RoundRectangle2D.Double(p, chipX - pinLen, pinW, pinLen, pinW, pinW));           // top
            g2.fill(new RoundRectangle2D.Double(p, chipX + chipW, pinW, pinLen, pinW, pinW));           // bottom
            g2.fill(new RoundRectangle2D.Double(chipX - pinLen, p, pinLen, pinW, pinW, pinW));          // left
            g2.fill(new RoundRectangle2D.Double(chipX + chipW, p, pinLen, pinW, pinW, pinW));           // right
        }

        // white chip body
        g2.fill(new RoundRectangle2D.Double(chipX, chipX, chipW, chipW, s * 0.1, s * 0.1));

        // dark core
        double coreInset = s * 0.05;
        double coreX = chipX + coreInset;
        double coreW = chipW - coreInset * 2;
        g2.setColor(new Color(40, 16, 18));
        g2.fill(new RoundRectangle2D.Double(coreX, coreX, coreW, coreW, s * 0.07, s * 0.07));

        // three process bars: blue, orange, green
        double barH = s * 0.065;
        double r = barH / 2;
        g2.setColor(new Color(92, 168, 255));
        g2.fill(new RoundRectangle2D.Double(s * 0.36, s * 0.38, s * 0.14, barH, r, r));
        g2.setColor(new Color(255, 170, 60));
        g2.fill(new RoundRectangle2D.Double(s * 0.42, s * 0.49, s * 0.20, barH, r, r));
        g2.setColor(new Color(110, 222, 120));
        g2.fill(new RoundRectangle2D.Double(s * 0.48, s * 0.60, s * 0.12, barH, r, r));
    }
}
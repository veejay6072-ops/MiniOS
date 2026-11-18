package opersys.segmentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/** The Segmentation app icon (green tile with a white card holding three colored bars), drawn in code. */
public final class SegmentationIcon {

    private SegmentationIcon() {
    }

    /** Returns the icon scaled to size x size pixels. */
    public static Icon create(int size) {
        int scale = 4;   // draw large, then smooth down, so the edges stay clean
        int big = size * scale;
        BufferedImage hi = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.scale(big / 100.0, big / 100.0);

        g.setPaint(new GradientPaint(0, 0, new Color(56, 198, 134), 0, 100, new Color(22, 138, 92)));
        g.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 46, 46));

        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(29, 12, 42, 78, 13, 13));

        g.setColor(new Color(86, 190, 255));
        g.fill(new RoundRectangle2D.Double(35, 20, 30, 28, 8, 8));
        g.setColor(new Color(255, 178, 72));
        g.fill(new RoundRectangle2D.Double(35, 54, 30, 13, 7, 7));
        g.setColor(new Color(200, 130, 255));
        g.fill(new RoundRectangle2D.Double(35, 72, 30, 12, 7, 7));
        g.dispose();

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(hi, 0, 0, size, size, null);
        g2.dispose();

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics gr, int x, int y) {
                gr.drawImage(out, x, y, null);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }
}
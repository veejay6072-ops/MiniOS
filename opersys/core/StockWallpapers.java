package opersys.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Built-in wallpapers that are drawn by code, so they need no image files.
 * Pick one with {@link #create(int, int, int)}; the names are in {@link #NAMES}.
 */
public final class StockWallpapers {
    private StockWallpapers() {
    }

    public static final String[] NAMES = {
            "Blue Waves", "Sunset", "Green Hills", "Night Sky", "Soft Abstract", "Dark Minimal"
    };

    /** Draws wallpaper number {@code index} (0-based, see NAMES) at the given size. */
    public static BufferedImage create(int index, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (index) {
            case 0: blueWaves(g, w, h); break;
            case 1: sunset(g, w, h); break;
            case 2: greenHills(g, w, h); break;
            case 3: nightSky(g, w, h); break;
            case 4: softAbstract(g, w, h); break;
            default: darkMinimal(g, w, h); break;
        }
        g.dispose();
        return img;
    }

    private static void blueWaves(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(40, 130, 220), 0, h, new Color(6, 28, 86)));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 6; i++) {
            Path2D wave = new Path2D.Double();
            double base = h * (0.35 + i * 0.1);
            wave.moveTo(0, base);
            for (int x = 0; x <= w; x += 20) {
                double y = base + Math.sin(x * 0.006 + i) * 40 + Math.sin(x * 0.002 + i * 2) * 60;
                wave.lineTo(x, y);
            }
            wave.lineTo(w, h);
            wave.lineTo(0, h);
            wave.closePath();
            g.setColor(new Color(120, 190, 255, 28 + i * 6));
            g.fill(wave);
        }
    }

    private static void sunset(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(255, 150, 100), 0, h * 0.7f, new Color(120, 40, 110)));
        g.fillRect(0, 0, w, h);
        double sx = w * 0.5;
        double sy = h * 0.58;
        double r = Math.min(w, h) * 0.13;
        for (int i = 4; i >= 1; i--) {   // soft glow around the sun
            g.setColor(new Color(255, 220, 150, 25));
            g.fill(new Ellipse2D.Double(sx - r * (1 + i * 0.5), sy - r * (1 + i * 0.5), r * 2 * (1 + i * 0.5), r * 2 * (1 + i * 0.5)));
        }
        g.setColor(new Color(255, 230, 170));
        g.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
        g.setColor(new Color(60, 25, 70));
        Path2D hills = new Path2D.Double();
        hills.moveTo(0, h * 0.7);
        hills.curveTo(w * 0.25, h * 0.6, w * 0.4, h * 0.78, w * 0.6, h * 0.7);
        hills.curveTo(w * 0.8, h * 0.62, w * 0.9, h * 0.72, w, h * 0.68);
        hills.lineTo(w, h);
        hills.lineTo(0, h);
        hills.closePath();
        g.fill(hills);
    }

    private static void greenHills(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(150, 210, 255), 0, h * 0.6f, new Color(235, 248, 255)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(120, 190, 90));
        Path2D far = new Path2D.Double();
        far.moveTo(0, h * 0.62);
        far.curveTo(w * 0.2, h * 0.48, w * 0.45, h * 0.66, w * 0.7, h * 0.55);
        far.curveTo(w * 0.85, h * 0.5, w * 0.95, h * 0.58, w, h * 0.56);
        far.lineTo(w, h);
        far.lineTo(0, h);
        far.closePath();
        g.fill(far);
        g.setColor(new Color(60, 140, 60));
        Path2D near = new Path2D.Double();
        near.moveTo(0, h * 0.8);
        near.curveTo(w * 0.3, h * 0.66, w * 0.55, h * 0.88, w * 0.8, h * 0.76);
        near.curveTo(w * 0.9, h * 0.72, w * 0.95, h * 0.78, w, h * 0.75);
        near.lineTo(w, h);
        near.lineTo(0, h);
        near.closePath();
        g.fill(near);
    }

    private static void nightSky(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 40), 0, h, new Color(30, 20, 70)));
        g.fillRect(0, 0, w, h);
        Random rnd = new Random(42);   // same stars every time
        for (int i = 0; i < 260; i++) {
            double size = 1 + rnd.nextInt(3);
            int alpha = 120 + rnd.nextInt(136);
            g.setColor(new Color(255, 255, 255, alpha));
            g.fill(new Ellipse2D.Double(rnd.nextInt(w), rnd.nextInt(h), size, size));
        }
        double mr = Math.min(w, h) * 0.07;
        g.setColor(new Color(255, 250, 220, 40));
        g.fill(new Ellipse2D.Double(w * 0.8 - mr * 2, h * 0.2 - mr * 2, mr * 4, mr * 4));
        g.setColor(new Color(250, 246, 230));
        g.fill(new Ellipse2D.Double(w * 0.8 - mr, h * 0.2 - mr, mr * 2, mr * 2));
    }

    private static void softAbstract(Graphics2D g, int w, int h) {
        g.setColor(new Color(242, 236, 250));
        g.fillRect(0, 0, w, h);
        Color[] colors = {
                new Color(255, 150, 190), new Color(170, 130, 255),
                new Color(120, 220, 210), new Color(255, 210, 120)};
        Random rnd = new Random(7);
        for (int i = 0; i < 9; i++) {
            Color c = colors[i % colors.length];
            double d = Math.min(w, h) * (0.35 + rnd.nextDouble() * 0.4);
            double x = rnd.nextDouble() * w - d * 0.3;
            double y = rnd.nextDouble() * h - d * 0.3;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110));
            g.fill(new Ellipse2D.Double(x, y, d, d));
        }
    }

    private static void darkMinimal(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(34, 34, 42), 0, h, new Color(10, 10, 14)));
        g.fillRect(0, 0, w, h);
        double y = h * 0.66;
        for (int i = 4; i >= 1; i--) {   // soft glow line
            g.setColor(new Color(60, 140, 255, 20));
            g.setStroke(new BasicStroke(i * 6f));
            g.draw(new Rectangle2D.Double(0, y, w, 0));
        }
        g.setColor(new Color(90, 170, 255));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Rectangle2D.Double(0, y, w, 0));
    }
}
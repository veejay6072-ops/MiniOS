package opersys.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Windows-style lock screen and sign-in screen, drawn entirely by this panel.
 * 1. Lock screen: a random stock wallpaper, big time and date.
 *    Swipe up (the lock content follows your drag and rises), or click, to continue.
 * 2. Sign-in screen: the Administrator account and a sign-in arrow. Click anywhere (or press Enter)
 *    to sign in. Press Esc to go back to the lock screen.
 * Shown when the program starts and after "Stand By".
 */
public class LockScreen extends JPanel {
    static final String USER_NAME = "Administrator";
    private static final String FONT = "Segoe UI";
    private static final float SIGN_IN_AT = 0.5f;   // past this point, a released swipe finishes going up

    private final Runnable onUnlock;
    private final Timer clockTimer = new Timer(1000, e -> updateClock());
    private final Timer animator = new Timer(16, e -> animateStep());
    private final Random random = new Random();
    private final java.awt.event.AWTEventListener keyListener = this::handleKey;

    private BufferedImage background;
    private int lastIndex = -1;
    private String clockText = "";
    private String dateText = "";

    private float progress = 0f;   // 0 = lock screen, 1 = sign-in screen (animated)
    private float target = 0f;     // where the animation is heading
    private boolean dragging = false;
    private int pressY;

    private final MouseAdapter mouse = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            pressY = e.getY();
            dragging = false;
            animator.stop();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (target != 0f) return;   // only a swipe on the lock screen moves it
            float up = pressY - e.getY();
            progress = clamp(up / (getHeight() * 0.5f));   // the lock content follows the finger/mouse
            dragging = true;
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                dragging = false;
                animateTo(progress > SIGN_IN_AT ? 1f : 0f);   // finish the swipe, or spring back
                return;
            }
            if (target == 0f) animateTo(1f);   // a click on the lock screen
            else unlock();                     // a click on the sign-in screen
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (target == 0f) animateTo(1f);   // two-finger swipe on a touchpad
        }
    };

    public LockScreen(Runnable onUnlock) {
        this.onUnlock = onUnlock;
        setOpaque(true);
        setBackground(Color.BLACK);
        setFocusable(true);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        updateClock();
    }

    /** Shows the lock screen with a new random wallpaper (used at startup and after Stand By). */
    public void showLock() {
        background = pickRandomWallpaper();
        progress = 0f;
        target = 0f;
        dragging = false;
        animator.stop();
        updateClock();
        clockTimer.start();
        setVisible(true);
        Toolkit.getDefaultToolkit().removeAWTEventListener(keyListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(keyListener, java.awt.AWTEvent.KEY_EVENT_MASK);
        repaint();
    }

    /** Ends the lock screen and goes back to the desktop. */
    private void unlock() {
        animator.stop();
        clockTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(keyListener);
        onUnlock.run();
    }

    private void animateTo(float t) {
        target = t;
        if (!animator.isRunning()) animator.start();
    }

    /** One frame of the swipe animation. */
    private void animateStep() {
        float diff = target - progress;
        if (Math.abs(diff) < 0.01f) {
            progress = target;
            animator.stop();
        } else {
            progress += diff * 0.2f;   // ease toward the target
        }
        repaint();
    }

    /** Key presses while the lock screen is up. */
    private void handleKey(java.awt.AWTEvent event) {
        if (!(event instanceof KeyEvent)) return;
        KeyEvent ke = (KeyEvent) event;
        if (ke.getID() != KeyEvent.KEY_PRESSED) return;
        if (target == 0f) {
            animateTo(1f);
        } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            unlock();
        } else if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
            animateTo(0f);
        }
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private BufferedImage pickRandomWallpaper() {
        int n = StockWallpapers.NAMES.length;
        int index = random.nextInt(n);
        if (n > 1 && index == lastIndex) index = (index + 1) % n;   // don't show the same one twice in a row
        lastIndex = index;
        return StockWallpapers.create(index, 1600, 900);
    }

    private void updateClock() {
        Date now = new Date();
        clockText = new SimpleDateFormat("h:mm").format(now);
        dateText = new SimpleDateFormat("EEEE, MMMM d").format(now);
        repaint();
    }

    // ---------- painting ----------

    @Override
    protected void paintComponent(Graphics g) {
        // Everything is painted here, so nothing is lost when the window is resized or minimized.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        drawCover(g2, background, w, h);
        // the dim gets stronger as the sign-in screen comes up
        g2.setColor(new Color(10, 20, 40, (int) (70 + progress * 90)));
        g2.fillRect(0, 0, w, h);

        // lock screen: rises and fades as you swipe
        float lockAlpha = 1f - progress;
        if (lockAlpha > 0.01f) {
            Graphics2D layer = (Graphics2D) g2.create();
            layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, lockAlpha));
            layer.translate(0, Math.round(-progress * h * 0.35f));
            drawLockContent(layer, w, h);
            layer.dispose();
        }

        // sign-in screen: slides up from below and fades in
        if (progress > 0.01f) {
            Graphics2D layer = (Graphics2D) g2.create();
            layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));
            layer.translate(0, Math.round((1f - progress) * h * 0.35f));
            drawSignInContent(layer, w, h);
            layer.dispose();
        }
        g2.dispose();
    }

    private void drawLockContent(Graphics2D g2, int w, int h) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FONT, Font.PLAIN, 92));
        centerText(g2, clockText, w / 2, (int) (h * 0.30));
        g2.setFont(new Font(FONT, Font.PLAIN, 24));
        centerText(g2, dateText, w / 2, (int) (h * 0.30) + 46);

        g2.setFont(new Font(FONT, Font.PLAIN, 15));
        g2.setColor(new Color(235, 235, 240));
        centerText(g2, "\u2191  Swipe up or click to sign in", w / 2, h - 70);
    }

    private void drawSignInContent(Graphics2D g2, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;

        // round user picture: orange circle with a white person silhouette
        double d = 104;
        double ax = cx - d / 2;
        double ay = cy - 70 - d / 2;
        g2.setPaint(new GradientPaint((float) ax, (float) ay, new Color(252, 196, 110),
                (float) ax, (float) (ay + d), new Color(206, 100, 30)));
        g2.fill(new Ellipse2D.Double(ax, ay, d, d));
        Shape oldClip = g2.getClip();
        g2.clip(new Ellipse2D.Double(ax, ay, d, d));
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fill(new Ellipse2D.Double(ax + d * 0.36, ay + d * 0.2, d * 0.28, d * 0.28));
        g2.fill(new Ellipse2D.Double(ax + d * 0.17, ay + d * 0.55, d * 0.66, d * 0.6));
        g2.setClip(oldClip);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FONT, Font.PLAIN, 26));
        centerText(g2, USER_NAME, cx, cy + 10);

        // sign-in arrow button: white circle with a blue arrow
        double r = 22;
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fill(new Ellipse2D.Double(cx - r, cy + 70 - r, r * 2, r * 2));
        g2.setColor(new Color(31, 111, 235));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(cx - 9, cy + 70, cx + 9, cy + 70));
        g2.draw(new Line2D.Double(cx + 2, cy + 63, cx + 9, cy + 70));
        g2.draw(new Line2D.Double(cx + 2, cy + 77, cx + 9, cy + 70));

        g2.setFont(new Font(FONT, Font.PLAIN, 13));
        g2.setColor(new Color(220, 228, 245));
        centerText(g2, "Click the arrow to sign in", cx, cy + 125);
    }

    private static void centerText(Graphics2D g2, String text, int cx, int baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
    }

    /** Fills the whole area with the picture, cropping the overflow (like the desktop wallpaper). */
    private static void drawCover(Graphics2D g2, BufferedImage img, int w, int h) {
        if (img == null) {
            g2.setPaint(new GradientPaint(0, 0, new Color(18, 110, 190), 0, h, new Color(5, 35, 80)));
            g2.fillRect(0, 0, w, h);
            return;
        }
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) Math.ceil(img.getWidth() * scale);
        int dh = (int) Math.ceil(img.getHeight() * scale);
        g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
    }
}
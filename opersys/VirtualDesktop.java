import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Main window: Windows XP-style desktop with wallpaper, icons, Start menu, taskbar and clock. */
public class VirtualDesktop extends JFrame {
    private static final int TASKBAR_HEIGHT = 40;

    // Windows XP "Luna" colors
    private static final Color XP_BLUE_TOP = new Color(61, 130, 240);
    private static final Color XP_BLUE = new Color(36, 94, 220);
    private static final Color XP_BLUE_BOTTOM = new Color(22, 60, 170);
    private static final Color XP_GREEN_TOP = new Color(110, 196, 104);
    private static final Color XP_GREEN = new Color(58, 152, 54);
    private static final Color XP_GREEN_BOTTOM = new Color(36, 112, 36);

    private static final Color XP_SHUTDOWN = new Color(90, 126, 220);
    private static final int KIND_STANDBY = 0;
    private static final int KIND_OFF = 1;
    private static final int KIND_RESTART = 2;

    private final BufferedImage wallpaper = loadWallpaper();
    private final OverlayPane overlay = new OverlayPane();
    private Timer clockTimer;
    private final JDesktopPane desktop;
    private final JPanel taskbarTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 4));
    private final JLabel clock = new JLabel();
    // The Start menu and its All Programs flyout are plain panels in the window's pop-up layer
    // (not JPopupMenus), so both can stay open at the same time.
    private JPanel startPanel;
    private JPanel allProgramsPanel;
    private JButton startButton;
    private final List<AppEntry> apps = new ArrayList<>();
    private final JPopupMenu searchResults = new JPopupMenu();
    private JTextField searchField;
    private int iconCount = 0;
    private int windowOffset = 0;

    public VirtualDesktop() {
        super("VirtualOS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);

        desktop = new JDesktopPane() {
            private BufferedImage cache;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                if (wallpaper == null) {   // fallback if bliss.jpg is missing
                    g2.setPaint(new GradientPaint(0, 0, new Color(18, 110, 190), 0, getHeight(), new Color(5, 35, 80)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    return;
                }
                if (cache == null || cache.getWidth() != getWidth() || cache.getHeight() != getHeight()) {
                    cache = scaleToCover(wallpaper, getWidth(), getHeight());
                }
                g2.drawImage(cache, 0, 0, null);
            }
        };
        add(desktop, BorderLayout.CENTER);
        add(buildTaskbar(), BorderLayout.SOUTH);
        setGlassPane(overlay);
        overlay.setVisible(false);

        registerApp("CPU Scheduling", "CPU", new Color(0, 120, 215), "FCFS, SJF, Priority, RR",
                "cpu process round robin gantt turnaround waiting", 0, CpuSchedulingApp::new);
        registerApp("Banker's Algorithm", "BA", new Color(200, 60, 60), "Deadlock avoidance",
                "deadlock safe sequence request resources", 0, DeadlockApp::new);
        registerApp("Paging", "PG", new Color(40, 150, 80), "Address translation",
                "page table frame logical physical", 1, PagingApp::new);
        registerApp("Page Replacement", "PR", new Color(140, 80, 200), "FIFO, LRU, OPT",
                "page replace fault hit optimal", 1, PageReplacementApp::new);
        registerApp("Segmentation", "SEG", new Color(230, 140, 20), "Base/limit translation",
                "segment table base limit trap", 1, SegmentationApp::new);
        registerApp("Fragmentation", "FRG", new Color(0, 150, 150), "First, Next, Best, Worst Fit",
                "fragmentation memory allocation hole internal external", 1, FragmentationApp::new);
        registerApp("Disk Scheduling", "DSK", new Color(120, 90, 60), "FCFS, SSTF, SCAN, LOOK",
                "disk c-scan cylinder head seek history", 0, DiskSchedulingApp::new);
        registerApp("Multiprogramming", "MP", new Color(90, 90, 110), "Memory + CPU sharing",
                "multiprog jobs degree memory", 0, MultiprogrammingApp::new);
        buildStartMenu();

        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock();
    }

    // ---------- wallpaper ----------

    /** Loads bliss.jpg from the classpath, the run folder, or an Image/ subfolder. */
    private static BufferedImage loadWallpaper() {
        try {
            URL url = VirtualDesktop.class.getResource("/bliss.jpg");
            if (url != null) return ImageIO.read(url);
            String[] places = {"bliss.jpg", "Image/bliss.jpg", "image/bliss.jpg", "images/bliss.jpg"};
            for (String place : places) {
                File file = new File(place);
                if (file.exists()) return ImageIO.read(file);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /** Scales the image to fill the whole area (like "Fill" in Windows), cropping the overflow. */
    private static BufferedImage scaleToCover(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int dw = (int) Math.ceil(src.getWidth() * scale);
        int dh = (int) Math.ceil(src.getHeight() * scale);
        g.drawImage(src, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    // ---------- apps and Start menu ----------

    private void registerApp(String title, String badge, Color color, String subtitle, String keywords,
                             int group, Supplier<JPanel> factory) {
        int column = iconCount / 6;
        int row = iconCount % 6;

        DesktopIcon icon = new DesktopIcon(title, badge, color, () -> openApp(title, factory.get()));
        icon.setBounds(15 + column * 100, 16 + row * 100, 100, 100);
        if (title.equals("Fragmentation")) icon.setArt(FragmentationApp.desktopIcon());
        if (title.equals("Multiprogramming")) icon.setArt(MultiprogrammingApp.desktopIcon());
        if (title.equals("Disk Scheduling")) icon.setArt(DiskSchedulingApp.desktopIcon());
        desktop.add(icon, JLayeredPane.DEFAULT_LAYER);

        AppEntry entry = new AppEntry(title, badge, color, subtitle, keywords, group, factory);
        // the same pictures in the Start menu, so it matches the desktop
        if (title.equals("Fragmentation")) entry.artFactory = FragmentationIcon::create;
        else if (title.equals("Multiprogramming")) entry.artFactory = MultiprogrammingIcon::create;
        else if (title.equals("Disk Scheduling")) entry.artFactory = DiskSchedulingApp::icon;
        apps.add(entry);
        iconCount++;
    }

    /** One program shown in the Start menu. Group 0 goes in the left column, group 1 in the right. */
    static class AppEntry {
        final String title;
        final String badge;
        final Color color;
        final String subtitle;
        final String keywords;   // extra words the search also matches (not shown)
        final int group;
        final Supplier<JPanel> factory;
        IntFunction<Icon> artFactory;   // optional picture at a given size; null = colored badge tile
        private final Map<Integer, Icon> artCache = new HashMap<>();

        /** The picture at this size (made once, then reused), or null if the app only has a badge. */
        Icon art(int size) {
            if (artFactory == null) return null;
            if (!artCache.containsKey(size)) artCache.put(size, artFactory.apply(size));
            return artCache.get(size);
        }

        AppEntry(String title, String badge, Color color, String subtitle, int group, Supplier<JPanel> factory) {
            this(title, badge, color, subtitle, "", group, factory);
        }

        AppEntry(String title, String badge, Color color, String subtitle, String keywords, int group,
                 Supplier<JPanel> factory) {
            this.title = title;
            this.badge = badge;
            this.color = color;
            this.subtitle = subtitle;
            this.keywords = keywords;
            this.group = group;
            this.factory = factory;
        }
    }

    /** XP-style Start menu: a few pinned programs, and an All Programs flyout with everything. */
    private void buildStartMenu() {
        allProgramsPanel = buildAllProgramsPanel(apps, this::openFromStartMenu);
        startPanel = buildStartPanel(apps,
                this::openFromStartMenu,
                () -> {
                    hideStartMenu();
                    showTurnOffDialog();
                },
                this::showAllPrograms,
                () -> allProgramsPanel.setVisible(false));

        JLayeredPane layers = getLayeredPane();
        layers.add(startPanel, JLayeredPane.POPUP_LAYER);
        layers.add(allProgramsPanel, JLayeredPane.POPUP_LAYER);
        startPanel.setSize(startPanel.getPreferredSize());
        allProgramsPanel.setSize(allProgramsPanel.getPreferredSize());
        startPanel.setVisible(false);
        allProgramsPanel.setVisible(false);
    }

    /** Closes the Start menu when the mouse is pressed anywhere outside it and its flyout. */
    private final AWTEventListener outsideClickListener = event -> {
        if (event.getID() != MouseEvent.MOUSE_PRESSED || !(event.getSource() instanceof Component)) return;
        Component c = (Component) event.getSource();
        if (c == startButton
                || SwingUtilities.isDescendingFrom(c, startPanel)
                || SwingUtilities.isDescendingFrom(c, allProgramsPanel)) return;
        hideStartMenu();
    };

    private void toggleStartMenu() {
        if (startPanel.isVisible()) {
            hideStartMenu();
            return;
        }
        searchResults.setVisible(false);
        JLayeredPane layers = getLayeredPane();
        Dimension d = startPanel.getPreferredSize();
        Point p = SwingUtilities.convertPoint(startButton, 0, 0, layers);   // sits right above the Start button
        startPanel.setBounds(p.x, p.y - d.height, d.width, d.height);
        startPanel.setVisible(true);
        layers.moveToFront(startPanel);
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void hideStartMenu() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
        allProgramsPanel.setVisible(false);
        startPanel.setVisible(false);
    }

    private void openFromStartMenu(AppEntry entry) {
        hideStartMenu();
        openApp(entry.title, entry.factory.get());
    }

    /** Shows the flyout to the right of the Start menu, its bottom lined up with the All Programs row. */
    private void showAllPrograms(Component anchor) {
        if (allProgramsPanel.isVisible()) return;
        JLayeredPane layers = getLayeredPane();
        Dimension d = allProgramsPanel.getPreferredSize();
        Point p = SwingUtilities.convertPoint(anchor, anchor.getWidth() + 3, anchor.getHeight(), layers);
        allProgramsPanel.setBounds(p.x, p.y - d.height, d.width, d.height);
        allProgramsPanel.setVisible(true);
        layers.moveToFront(allProgramsPanel);
    }

    private static Shape topRounded(int w, int h) {
        Area a = new Area(new RoundRectangle2D.Double(0, 0, w, h + 20, 16, 16));
        a.intersect(new Area(new Rectangle2D.Double(0, 0, w, h)));
        return a;
    }

    /**
     * The Start menu itself: blue header, a white column with the pinned programs and the
     * All Programs row, and a blue footer. Hovering All Programs calls onAllPrograms; hovering
     * anything else calls onOtherHover (so the flyout can close).
     */
    static JPanel buildStartPanel(List<AppEntry> entries, Consumer<AppEntry> open, Runnable onTurnOff,
                                  Consumer<Component> onAllPrograms, Runnable onOtherHover) {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        left.setPreferredSize(new Dimension(230, 276));

        for (AppEntry e : entries) {
            if (e.group != 0) continue;   // only pinned programs; the rest live under All Programs
            left.add(new StartItem(e, false, () -> open.accept(e)).onHover(onOtherHover));
        }
        left.add(Box.createVerticalGlue());

        JPanel line = new JPanel();
        line.setBackground(new Color(205, 218, 238));
        line.setPreferredSize(new Dimension(0, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(line);
        left.add(new AllProgramsItem(onAllPrograms));

        JPanel bodyWrap = new JPanel(new BorderLayout());   // leaves a thin blue frame at the sides
        bodyWrap.setOpaque(false);
        bodyWrap.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        bodyWrap.add(left, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(66, 130, 228), 0, getHeight(), new Color(30, 86, 204)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(0, 44));
        FooterButton turnOff = new FooterButton("Turn Off Computer", KIND_OFF, onTurnOff);
        footer.add(turnOff);

        StartHeader header = new StartHeader();
        MouseAdapter closeFlyout = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                onOtherHover.run();
            }
        };
        header.addMouseListener(closeFlyout);
        footer.addMouseListener(closeFlyout);
        turnOff.addMouseListener(closeFlyout);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(36, 94, 220));
                g2.fill(topRounded(getWidth(), getHeight()));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.add(header, BorderLayout.NORTH);
        root.add(bodyWrap, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    /** The flyout that lists every program, in alphabetical order. */
    static JPanel buildAllProgramsPanel(List<AppEntry> entries, Consumer<AppEntry> open) {
        List<AppEntry> sorted = new ArrayList<>(entries);
        sorted.sort((x, y) -> x.title.compareToIgnoreCase(y.title));
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 160, 185)),
                BorderFactory.createEmptyBorder(3, 0, 3, 0)));
        for (AppEntry e : sorted) list.add(new FlyoutItem(e, () -> open.accept(e)));
        return list;
    }

    /** Blue top strip with the user picture and name. */
    private static class StartHeader extends JPanel {
        StartHeader() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 60));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(74, 146, 238), 0, h, new Color(24, 84, 206)));
            g2.fill(topRounded(w, h));

            // user picture: orange tile with a simple person silhouette
            int ax = 12;
            int ay = 8;
            int as = 44;
            g2.setPaint(new GradientPaint(ax, ay, new Color(252, 196, 110), ax, ay + as, new Color(206, 100, 30)));
            g2.fillRoundRect(ax, ay, as, as, 8, 8);
            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Double(ax, ay, as, as, 8, 8));
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fill(new Ellipse2D.Double(ax + 14, ay + 7, 16, 16));
            g2.fill(new Ellipse2D.Double(ax + 6, ay + 27, 32, 28));
            g2.setClip(oldClip);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(ax, ay, as, as, 8, 8);

            // name with a soft shadow
            g2.setFont(new Font("Trebuchet MS", Font.BOLD, 19));
            FontMetrics fm = g2.getFontMetrics();
            int tx = ax + as + 12;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(new Color(0, 30, 110, 150));
            g2.drawString("Administrator", tx + 1, ty + 1);
            g2.setColor(Color.WHITE);
            g2.drawString("Administrator", tx, ty);
            g2.dispose();
        }
    }

    /** One program in the Start menu. Highlights in blue when hovered, like XP. */
    private static class StartItem extends JButton {
        private final AppEntry entry;
        private final boolean rightColumn;
        private boolean hover;
        private Runnable hoverCallback;

        StartItem onHover(Runnable callback) {
            this.hoverCallback = callback;
            return this;
        }

        StartItem(AppEntry entry, boolean rightColumn, Runnable action) {
            this.entry = entry;
            this.rightColumn = rightColumn;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                    if (hoverCallback != null) hoverCallback.run();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addActionListener(e -> {
                hover = false;
                action.run();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(188, entry.subtitle == null ? 40 : 52);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(new Color(49, 106, 197));
                g2.fillRect(0, 0, w, h);
            }

            int size = rightColumn ? 26 : 32;
            int ix = 8;
            int iy = (h - size) / 2;
            Icon art = entry.art(size);
            if (art != null) {
                art.paintIcon(this, g2, ix, iy);
            } else {
                g2.setColor(entry.color);
                g2.fillRoundRect(ix, iy, size, size, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, rightColumn ? 9 : 11));
                FontMetrics bf = g2.getFontMetrics();
                g2.drawString(entry.badge, ix + (size - bf.stringWidth(entry.badge)) / 2,
                        iy + (size + bf.getAscent() - bf.getDescent()) / 2);
            }

            int tx = ix + size + 8;
            Font titleFont = new Font("Tahoma", Font.BOLD, 12);
            Color titleColor = hover ? Color.WHITE : (rightColumn ? new Color(0, 19, 107) : Color.BLACK);
            if (entry.subtitle == null) {
                g2.setFont(titleFont);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(titleColor);
                g2.drawString(entry.title, tx, (h + fm.getAscent() - fm.getDescent()) / 2);
            } else {
                Font subFont = new Font("Tahoma", Font.PLAIN, 11);
                FontMetrics tf = g2.getFontMetrics(titleFont);
                FontMetrics sf = g2.getFontMetrics(subFont);
                int total = tf.getHeight() + sf.getHeight();
                int y1 = (h - total) / 2 + tf.getAscent();
                g2.setFont(titleFont);
                g2.setColor(titleColor);
                g2.drawString(entry.title, tx, y1);
                g2.setFont(subFont);
                g2.setColor(hover ? Color.WHITE : new Color(110, 110, 110));
                g2.drawString(entry.subtitle, tx, y1 + tf.getDescent() + sf.getAscent());
            }
            g2.dispose();
        }
    }

    // ---------- taskbar ----------

    private JPanel buildTaskbar() {
        StartButton start = new StartButton();
        startButton = start;
        start.addActionListener(e -> toggleStartMenu());

        searchField = buildSearchField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearchResults();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearchResults();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearchResults();
            }
        });
        searchField.addActionListener(e -> {   // Enter opens the best match
            List<AppEntry> found = search(apps, searchField.getText());
            if (!found.isEmpty()) launchFromSearch(found.get(0));
        });
        searchField.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
            }
        });
        searchResults.setFocusable(false);   // keep typing focus in the search box
        searchResults.setBorder(BorderFactory.createLineBorder(new Color(36, 94, 220), 2));

        JPanel searchHolder = new JPanel(new GridBagLayout());
        searchHolder.setOpaque(false);
        searchHolder.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
        searchHolder.add(searchField);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(start, BorderLayout.WEST);
        left.add(searchHolder, BorderLayout.CENTER);

        taskbarTabs.setOpaque(false);

        clock.setForeground(Color.WHITE);
        clock.setFont(new Font("Tahoma", Font.BOLD, 14));

        JPanel tray = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(),
                        new float[]{0f, 1f},
                        new Color[]{new Color(20, 164, 246), new Color(12, 140, 236)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(9, 88, 190));           // dark left edge
                g2.drawLine(0, 0, 0, getHeight());
                g2.setColor(new Color(70, 190, 255));         // light edge beside it
                g2.drawLine(1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        tray.setOpaque(false);
        tray.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, 0, 0, 12);
        tray.add(new TrayArrow(), gap);
        tray.add(clock);

        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(),
                        new float[]{0f, 0.12f, 0.9f, 1f},
                        new Color[]{new Color(62, 130, 240), new Color(37, 96, 222),
                                new Color(35, 90, 215), XP_BLUE_BOTTOM}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(100, 160, 250));        // thin highlight along the top
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, TASKBAR_HEIGHT));
        bar.add(left, BorderLayout.WEST);
        bar.add(taskbarTabs, BorderLayout.CENTER);
        bar.add(tray, BorderLayout.EAST);
        return bar;
    }

    /** "All Programs" row with the green arrow. Hovering it opens the flyout. */
    private static class AllProgramsItem extends JButton {
        private boolean hover;

        AllProgramsItem(Consumer<Component> onHover) {
            super("All Programs");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                    onHover.accept(AllProgramsItem.this);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addActionListener(e -> onHover.accept(this));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(224, 42);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 42);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(new Color(49, 106, 197));
                g2.fillRect(0, 0, w, h);
            }
            g2.setFont(new Font("Tahoma", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(hover ? Color.WHITE : Color.BLACK);
            g2.drawString("All Programs", 16, (h + fm.getAscent() - fm.getDescent()) / 2);

            int size = 22;
            int x = w - size - 14;
            int y = (h - size) / 2;
            g2.setPaint(new GradientPaint(x, y, new Color(120, 206, 110), x, y + size, new Color(40, 140, 40)));
            g2.fillRoundRect(x, y, size, size, 8, 8);
            g2.setColor(Color.WHITE);
            g2.fillPolygon(new int[]{x + 8, x + 8, x + 16}, new int[]{y + 5, y + 17, y + 11}, 3);
            g2.dispose();
        }
    }

    /** One program in the All Programs flyout: small icon and name. */
    private static class FlyoutItem extends JButton {
        private final AppEntry entry;
        private boolean hover;

        FlyoutItem(AppEntry entry, Runnable action) {
            this.entry = entry;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addActionListener(e -> {
                hover = false;
                action.run();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(212, 30);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(new Color(49, 106, 197));
                g2.fillRect(0, 0, w, h);
            }
            int size = 22;
            int ix = 10;
            int iy = (h - size) / 2;
            Icon art = entry.art(size);
            if (art != null) {
                art.paintIcon(this, g2, ix, iy);
            } else {
                g2.setColor(entry.color);
                g2.fillRoundRect(ix, iy, size, size, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                FontMetrics bf = g2.getFontMetrics();
                g2.drawString(entry.badge, ix + (size - bf.stringWidth(entry.badge)) / 2,
                        iy + (size + bf.getAscent() - bf.getDescent()) / 2);
            }

            g2.setFont(new Font("Tahoma", Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(hover ? Color.WHITE : Color.BLACK);
            g2.drawString(entry.title, ix + size + 8, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // ---------- search ----------

    /** The taskbar search box: white field with a magnifier on the right. */
    static JTextField buildSearchField() {
        PlaceholderTextField field = new PlaceholderTextField("Search programs", 14) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() - 19;
                int cy = getHeight() / 2 - 2;
                g2.setColor(new Color(36, 94, 220));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(cx - 5, cy - 5, 10, 10);
                g2.drawLine(cx + 3, cy + 3, cx + 8, cy + 8);
                g2.dispose();
            }
        };
        field.setFont(new Font("Tahoma", Font.PLAIN, 12));
        field.setForeground(Color.BLACK);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(8, 50, 160)),
                BorderFactory.createEmptyBorder(2, 8, 2, 28)));
        field.setPreferredSize(new Dimension(190, 26));
        return field;
    }

    /** Apps whose name, description or keywords contain every word typed. Name matches come first. */
    static List<AppEntry> search(List<AppEntry> all, String query) {
        List<AppEntry> starts = new ArrayList<>();
        List<AppEntry> others = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) return starts;
        String[] words = q.split("\\s+");
        for (AppEntry e : all) {
            String hay = (e.title + " " + (e.subtitle == null ? "" : e.subtitle) + " " + e.keywords).toLowerCase();
            boolean ok = true;
            for (String w : words) {
                if (!hay.contains(w)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            (e.title.toLowerCase().startsWith(q) ? starts : others).add(e);
        }
        starts.addAll(others);
        return starts;
    }

    /** The list shown in the search pop-up (or a "no match" message). */
    static JPanel buildResultsList(List<AppEntry> found, Consumer<AppEntry> open) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        if (found.isEmpty()) {
            JLabel none = new JLabel("No programs match your search");
            none.setFont(new Font("Tahoma", Font.PLAIN, 12));
            none.setForeground(new Color(110, 110, 110));
            none.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(none);
        } else {
            for (AppEntry e : found) list.add(new StartItem(e, false, () -> open.accept(e)));
        }
        return list;
    }

    private void updateSearchResults() {
        searchResults.setVisible(false);
        if (searchField.getText().trim().isEmpty()) return;
        searchResults.removeAll();
        searchResults.add(buildResultsList(search(apps, searchField.getText()), this::launchFromSearch));
        searchResults.show(searchField, 0, -searchResults.getPreferredSize().height - 2);
    }

    private void launchFromSearch(AppEntry entry) {
        searchResults.setVisible(false);
        searchField.setText("");
        openApp(entry.title, entry.factory.get());
    }

    /** The green XP "start" button with the four-color flag. */
    private static class StartButton extends JButton {
        StartButton() {
            super("Start");
            setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 20));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 40, 2, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            boolean pressed = getModel().isPressed() || getModel().isArmed();
            boolean hover = getModel().isRollover();
            Color top = pressed ? XP_GREEN : hover ? new Color(130, 210, 124) : XP_GREEN_TOP;
            Color mid = pressed ? XP_GREEN_BOTTOM : hover ? new Color(72, 168, 66) : XP_GREEN;
            Color bottom = pressed ? new Color(28, 92, 28) : XP_GREEN_BOTTOM;

            // Left corners are pushed outside the component so only the right side looks rounded.
            Shape shape = new RoundRectangle2D.Float(-24, 0, w + 24, h, 26, 26);
            g2.setPaint(new LinearGradientPaint(0, 0, 0, h,
                    new float[]{0f, 0.45f, 1f}, new Color[]{top, mid, bottom}));
            g2.fill(shape);
            // glossy highlight on the top half
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 95),
                    0, h * 0.55f, new Color(255, 255, 255, 0)));
            g2.fill(shape);
            g2.setColor(new Color(20, 80, 20));
            g2.draw(new RoundRectangle2D.Float(-24, 0, w + 23, h - 1, 26, 26));

            // Windows flag: four slightly waved colored panes
            int fx = 10;
            int fy = (h - 18) / 2;
            flagPane(g2, fx, fy, new Color(244, 84, 40));
            flagPane(g2, fx + 10, fy - 1, new Color(120, 202, 44));
            flagPane(g2, fx, fy + 10, new Color(36, 128, 240));
            flagPane(g2, fx + 10, fy + 9, new Color(252, 204, 30));

            // Text shadow, then the real text
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tx = 38;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setFont(getFont());
            g2.setColor(new Color(0, 60, 0, 170));
            g2.drawString("Start", tx + 1, ty + 1);
            g2.setColor(Color.WHITE);
            g2.drawString("Start", tx, ty);
            g2.dispose();
        }

        private static void flagPane(Graphics2D g2, int x, int y, Color c) {
            g2.setColor(c);
            g2.fillPolygon(new int[]{x, x + 8, x + 8, x}, new int[]{y + 1, y - 1, y + 7, y + 9}, 4);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(118, TASKBAR_HEIGHT);
        }
    }

    /** The little round "<" button at the left of the notification area. */
    private static class TrayArrow extends JComponent {
        TrayArrow() {
            setPreferredSize(new Dimension(20, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(70, 160, 250), 0, getHeight(), new Color(20, 100, 220)));
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{12, 7, 12}, new int[]{5, 10, 15}, 3);
            g2.dispose();
        }
    }

    /** Taskbar button for an open window. Looks pressed-in while its window is active. */
    private static class TaskButton extends JButton {
        private boolean active;

        TaskButton(String title) {
            super(title);
            setFont(new Font("Tahoma", Font.PLAIN, 12));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            setPreferredSize(new Dimension(150, TASKBAR_HEIGHT - 8));
        }

        void setActive(boolean value) {
            active = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color top = active ? new Color(24, 72, 176) : new Color(86, 150, 246);
            Color bottom = active ? new Color(34, 92, 204) : new Color(48, 112, 232);
            if (!active && getModel().isRollover()) {
                top = new Color(110, 172, 250);
                bottom = new Color(66, 130, 240);
            }
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 7, 7);
            g2.setColor(active ? new Color(12, 40, 120) : new Color(20, 60, 170));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 7, 7);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- shutdown ----------

    /** Dims the desktop and shows the "Turn off computer" dialog. */
    private void showTurnOffDialog() {
        overlay.show(new Color(0, 0, 0, 120), "");
        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);

        Runnable cancel = () -> {
            dialog.dispose();
            overlay.hideOverlay();
        };
        JPanel root = buildTurnOffPanel(
                () -> { dialog.dispose(); overlay.hideOverlay(); standBy(); },
                () -> { dialog.dispose(); turnOff(); },
                () -> { dialog.dispose(); restart(); },
                cancel);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel.run();
            }
        });

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);   // blocks until one of the buttons closes it
    }

    /** The dialog's content: blue header, three big buttons and a Cancel button. */
    static JPanel buildTurnOffPanel(Runnable onStandBy, Runnable onOff, Runnable onRestart, Runnable onCancel) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 40, 150), getWidth(), 0, new Color(40, 90, 210)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 42));
        JLabel title = new JLabel("Turn off computer");
        title.setFont(new Font("Trebuchet MS", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        header.add(title, BorderLayout.CENTER);

        JPanel body = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 24));
        body.setBackground(XP_SHUTDOWN);
        body.add(shutdownOption("Stand By", KIND_STANDBY, onStandBy));
        body.add(shutdownOption("Turn Off", KIND_OFF, onOff));
        body.add(shutdownOption("Restart", KIND_RESTART, onRestart));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        cancel.addActionListener(e -> onCancel.run());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        footer.setBackground(new Color(0, 48, 156));
        footer.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(0, 40, 140), 2));
        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private static JPanel shutdownOption(String label, int kind, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        TileButton button = new TileButton(kind, 44);
        button.addActionListener(e -> action.run());
        JLabel text = new JLabel(label, SwingConstants.CENTER);
        text.setFont(new Font("Tahoma", Font.BOLD, 12));
        text.setForeground(Color.WHITE);
        p.add(button, BorderLayout.CENTER);
        p.add(text, BorderLayout.SOUTH);
        return p;
    }

    private void turnOff() {
        overlay.show(XP_SHUTDOWN, "VirtualOS is shutting down...");
        Timer t = new Timer(1800, e -> {
            clockTimer.stop();
            dispose();
            System.exit(0);
        });
        t.setRepeats(false);
        t.start();
    }

    private void restart() {
        overlay.show(XP_SHUTDOWN, "VirtualOS is restarting...");
        Timer t = new Timer(1800, e -> {
            clockTimer.stop();
            new VirtualDesktop().setVisible(true);   // open the new session first, then close this one
            dispose();
        });
        t.setRepeats(false);
        t.start();
    }

    private void standBy() {
        overlay.show(Color.BLACK, "Click anywhere to wake up");
        overlay.onClick = overlay::hideOverlay;
    }

    /** Draws an XP-style colored square button face with a white symbol. */
    static void drawTile(Graphics2D g2, int kind, int x, int y, int s) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color top;
        Color bottom;
        switch (kind) {
            case KIND_OFF:     top = new Color(238, 108, 78);  bottom = new Color(190, 40, 20);  break;
            case KIND_RESTART: top = new Color(110, 205, 96);  bottom = new Color(36, 136, 36);  break;
            case KIND_STANDBY: top = new Color(250, 206, 70);  bottom = new Color(220, 150, 10); break;
            default:           top = new Color(250, 182, 80);  bottom = new Color(214, 120, 20); break;
        }
        int arc = Math.max(4, s / 4);
        g2.setPaint(new GradientPaint(x, y, top, x, y + s, bottom));
        g2.fillRoundRect(x, y, s, s, arc, arc);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(1f, s / 18f)));
        g2.drawRoundRect(x, y, s, s, arc, arc);

        double cx = x + s / 2.0;
        double cy = y + s / 2.0;
        double r = s * 0.27;
        g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        switch (kind) {
            case KIND_OFF:
                g2.draw(new Arc2D.Double(cx - r, cy - r + s * 0.03, 2 * r, 2 * r, 125, 290, Arc2D.OPEN));
                g2.draw(new Line2D.Double(cx, cy - r - s * 0.02, cx, cy));
                break;
            case KIND_RESTART: {
                g2.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 100, 270, Arc2D.OPEN));
                double a = Math.toRadians(10);
                double ex = cx + r * Math.cos(a);
                double ey = cy - r * Math.sin(a);
                double tx = -Math.sin(a);
                double ty = -Math.cos(a);
                double nx = Math.cos(a);
                double ny = -Math.sin(a);
                double k = s * 0.17;
                double w = s * 0.13;
                Path2D.Double head = new Path2D.Double();
                head.moveTo(ex + tx * k, ey + ty * k);
                head.lineTo(ex + nx * w, ey + ny * w);
                head.lineTo(ex - nx * w, ey - ny * w);
                head.closePath();
                g2.fill(head);
                break;
            }
            case KIND_STANDBY: {
                Area moon = new Area(new Ellipse2D.Double(cx - r * 1.1, cy - r * 1.1, 2.2 * r, 2.2 * r));
                moon.subtract(new Area(new Ellipse2D.Double(cx - r * 0.35, cy - r * 1.35, 2.0 * r, 2.0 * r)));
                g2.fill(moon);
                break;
            }
            default:
                break;
        }
    }

    /** Big square button used in the Turn off computer dialog. */
    private static class TileButton extends JButton {
        private final int kind;
        private final int size;

        TileButton(int kind, int size) {
            this.kind = kind;
            this.size = size;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(size + 10, size + 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || getModel().isPressed()) {   // soft glow behind the tile
                g2.setColor(new Color(255, 255, 255, getModel().isPressed() ? 120 : 70));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            drawTile(g2, kind, 5, 5, size);
            g2.dispose();
        }
    }

    /** Button in the Start menu footer: small colored tile followed by a white label. */
    private static class FooterButton extends JButton {
        private final int kind;

        FooterButton(String text, int kind, Runnable action) {
            super(text);
            this.kind = kind;
            setFont(new Font("Tahoma", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 36, 0, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> action.run());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 55));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            drawTile(g2, kind, 6, (getHeight() - 22) / 2, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Full-window layer over the desktop: dims it, or covers it for shutdown, restart and stand by. */
    private static class OverlayPane extends JPanel {
        private Color background = new Color(0, 0, 0, 0);
        private String message = "";
        Runnable onClick;

        OverlayPane() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {   // swallow clicks so the desktop underneath is locked
                @Override
                public void mouseClicked(MouseEvent e) {
                    Runnable r = onClick;
                    onClick = null;
                    if (r != null) r.run();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
            });
        }

        void show(Color bg, String text) {
            background = bg;
            message = text;
            onClick = null;
            setVisible(true);
            repaint();
        }

        void hideOverlay() {
            onClick = null;
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(background);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (!message.isEmpty()) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Trebuchet MS", Font.BOLD | Font.ITALIC, 28));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(message)) / 2;
                int y = getHeight() / 2 + fm.getAscent() / 2;
                g2.setColor(background.equals(Color.BLACK) ? new Color(170, 170, 170) : Color.WHITE);
                g2.drawString(message, x, y);
            }
            g2.dispose();
        }
    }

    // ---------- windows ----------

    /** Opens an app in its own window and adds a taskbar tab for it. */
    public void openApp(String title, JPanel content) {
        JInternalFrame win = new JInternalFrame(title, true, true, true, true);
        win.setContentPane(content);
        win.setSize(content instanceof FragmentationApp ? new Dimension(860, 640)
                : content instanceof MultiprogrammingApp ? new Dimension(900, 650)
                : content instanceof DiskSchedulingApp ? new Dimension(900, 640) : new Dimension(780, 540));
        if (content instanceof MultiprogrammingApp) win.setFrameIcon(MultiprogrammingIcon.create(16));
        if (content instanceof DiskSchedulingApp) win.setFrameIcon(DiskSchedulingApp.icon(16));
        win.setLocation(40 + windowOffset, 30 + windowOffset);
        windowOffset = (windowOffset + 30) % 240;
        desktop.add(win);
        win.setVisible(true);

        TaskButton tab = new TaskButton(title);
        tab.addActionListener(e -> restore(win));
        taskbarTabs.add(tab);
        taskbarTabs.revalidate();
        taskbarTabs.repaint();

        win.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                tab.setActive(true);
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                tab.setActive(false);
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                taskbarTabs.remove(tab);
                taskbarTabs.revalidate();
                taskbarTabs.repaint();
            }
        });
        restore(win);
    }

    private void restore(JInternalFrame win) {
        try {
            win.setIcon(false);
            win.setSelected(true);
        } catch (PropertyVetoException ignored) {
        }
        win.toFront();
    }

    private void updateClock() {
        Date now = new Date();
        clock.setText(new SimpleDateFormat("h:mma").format(now));
        clock.setToolTipText(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(now));
    }
}
package opersys.diskscheduling;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import opersys.core.UiKit;

/**
 * Disk Scheduling in a dark "DiskBeat" player style: pick an algorithm, press play and watch the
 * head visit each track. The algorithms live in DiskScheduler; every run is saved in the History tab.
 */
public class DiskSchedulingApp extends JPanel {
    // ---- palette ----
    private static final Color BG = new Color(8, 8, 8);
    private static final Color PANEL = new Color(18, 18, 18);
    private static final Color CARD = new Color(26, 26, 26);
    private static final Color ROW_ON = new Color(46, 46, 46);
    private static final Color TILE = new Color(44, 44, 44);
    private static final Color GREEN = new Color(30, 215, 96);
    private static final Color MUTED = new Color(176, 176, 176);
    private static final Color DIM = new Color(112, 112, 112);
    private static final String FONT = "Segoe UI";
    private static final double STEP_SECONDS = 0.9;   // how long the head takes per stop while playing

    // ---- setup ----
    private DiskHistory history = new DiskHistory();
    private int[] queue = new int[0];
    private boolean up = true;
    private String algo = "SSTF";
    private boolean started = false;   // false until the user enters something and runs it
    private DiskSchedulingController controller;

    // ---- result of the last run ----
    private int head = 53;
    private int tracks = 200;
    private int maxTrack = 199;
    private List<DiskScheduler.Stop> stops = new ArrayList<>();
    private int[] dist = new int[0];
    private int[] cum = new int[0];   // cum[i] = head movement after reaching stop i
    private int total;

    // ---- playback ----
    private double t;                 // 0..stops.size(): which stop the head is heading to, with the fraction travelled
    private boolean playing;
    private boolean recorded;
    private int lastStep = -1;
    private final javax.swing.Timer timer = new javax.swing.Timer(20, e -> tick());

    // ---- widgets ----
    private final RoundField headField = new RoundField("e.g. 53", 5);
    private final RoundField tracksField = new RoundField("e.g. 200", 5);
    private final PillButton dirChip = new PillButton("Up");
    private final PillButton[] algoChips = new PillButton[DiskScheduler.ALGORITHMS.length];
    private final PillButton schedulerTab = new PillButton("Scheduler");
    private final PillButton historyTab = new PillButton("History");
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final QueuePanel queuePanel = new QueuePanel();
    private final JScrollPane queueScroll = new JScrollPane(queuePanel);
    private final HeroPanel hero = new HeroPanel();
    private final ChartPanel chart = new ChartPanel();
    private final ProgressBar progress = new ProgressBar();
    private final TransportButton playButton = new TransportButton(TransportButton.PLAY);
    private final JLabel stepLabel = softLabel("", SwingConstants.LEFT);
    private final JLabel nowLabel = softLabel("", SwingConstants.RIGHT);
    private final JLabel movedLabel = softLabel("", SwingConstants.LEFT);
    private final JLabel totalLabel = softLabel("", SwingConstants.RIGHT);
    private final JPanel historyBody = new JPanel();

    public DiskSchedulingApp() {
        super(new BorderLayout(0, 8));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildTopBar(), BorderLayout.NORTH);
        center.setOpaque(false);
        center.add(buildSchedulerPage(), "scheduler");
        center.add(buildHistoryPage(), "history");
        add(center, BorderLayout.CENTER);

        refreshChips();
        showTab("scheduler");
        showEmpty();   // start blank: the process only shows once the user enters something
    }

    /** The blank starting screen: no requests, no path, no totals. */
    private void showEmpty() {
        started = false;
        stops = new ArrayList<>();
        dist = new int[0];
        cum = new int[0];
        total = 0;
        t = 0;
        lastStep = -1;
        playing = false;
        timer.stop();
        refreshChips();
        refreshAll();
    }

    /** Re-runs after a setting changed, but only once there is something to run. */
    private void rerun() {
        if (queue.length == 0) {
            refreshChips();
            return;
        }
        run(true, true);
    }

    /** The controller calls this to wire itself up. */
    public void bindController(DiskSchedulingController controller) {
        this.controller = controller;
        if (controller != null) {
            this.history = controller.getHistory();   // share the same history instance
        }
    }

    /** The head position text. */
    public String getHeadText() {
        return headField.getText();
    }

    /** The number of tracks text. */
    public String getTracksText() {
        return tracksField.getText();
    }

    /** The current request queue. */
    public int[] getQueue() {
        return queue.clone();
    }

    /** The currently selected algorithm. */
    public String getAlgo() {
        return algo;
    }

    /** True when the head should start moving up. */
    public boolean isUp() {
        return up;
    }

    /** Apply a fresh result from the controller. */
    public void showResult(DiskSchedulingModel.Result result) {
        this.stops = new ArrayList<>();
        for (DiskScheduler.Stop s : result.getStops()) this.stops.add(s);
        this.dist = result.getDist().clone();
        this.cum = result.getCum().clone();
        this.total = result.getTotal();
        this.head = result.getHead();
        this.tracks = result.getTracks();
        this.maxTrack = result.getMaxTrack();
        this.queue = result.getQueue().clone();
        this.algo = result.getAlgo();
        this.up = result.isUp();
        this.started = true;
        this.t = 0;
        this.lastStep = -1;
        this.playing = false;
        this.timer.stop();
        this.recorded = false;
        refreshChips();
        refreshAll();
    }

    /** Show a friendly error popup. */
    public void showError(String message) {
        UiKit.error(this, message);
    }

    @Override
    public void removeNotify() {
        timer.stop();
        playing = false;
        super.removeNotify();
    }

    // =====================================================================================
    // Icon (also used by the desktop shortcut and the Start menu)
    // =====================================================================================

    /** The green rounded square with the disc and the white arm. */
    static void paintIcon(Graphics2D g0, int x, int y, int s) {
        Graphics2D g = (Graphics2D) g0.create();
        smooth(g);
        int arc = (int) (s * 0.28);
        g.setPaint(new GradientPaint(x, y, new Color(42, 208, 98), x, y + s, new Color(16, 126, 56)));
        g.fillRoundRect(x, y, s, s, arc, arc);
        double cx = x + s * 0.5;
        double cy = y + s * 0.53;
        g.setColor(new Color(14, 14, 14));
        g.fill(circle(cx, cy, s * 0.36));
        g.setColor(GREEN);
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.045f)));
        g.draw(circle(cx, cy, s * 0.27));
        g.draw(circle(cx, cy, s * 0.17));
        g.fill(circle(cx, cy, s * 0.065));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1.5f, s * 0.075f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(cx, cy, cx + s * 0.2, cy - s * 0.28));
        g.dispose();
    }

    /** The radar-disc icon at any size (desktop uses 52, the Start menu smaller sizes, the window title 16). */
    public static Icon icon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                DiskSchedulingApp.paintIcon((Graphics2D) g, x, y, size);
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

    /** The 52 x 52 desktop icon, same style as FragmentationApp.desktopIcon(). */
    public static Icon desktopIcon() {
        return icon(52);
    }

    private static java.awt.geom.Ellipse2D circle(double cx, double cy, double r) {
        return new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
    }

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static Font font(int style, int size) {
        return new Font(FONT, style, size);
    }

    private static JLabel softLabel(String text, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    // =====================================================================================
    // Pages
    // =====================================================================================

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JComponent brand = new JComponent() {
            {
                setPreferredSize(new Dimension(150, 46));
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                paintIcon(g, 0, 5, 36);
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, 20));
                g.drawString("DiskBeat", 46, 31);
                g.dispose();
            }
        };
        bar.add(brand, BorderLayout.WEST);

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 7)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                g.setColor(CARD);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
            }
        };
        pill.setOpaque(false);
        pill.add(softLabel("Head starts at", SwingConstants.RIGHT));
        pill.add(headField);
        pill.add(softLabel("Tracks", SwingConstants.RIGHT));
        pill.add(tracksField);
        pill.add(dirChip);
        TransportButton runButton = new TransportButton(TransportButton.RUN);
        runButton.setToolTipText("Run the simulation");
        runButton.addActionListener(e -> run(true, true));
        pill.add(runButton);
        bar.add(pill, BorderLayout.CENTER);

        headField.addActionListener(e -> rerun());
        tracksField.addActionListener(e -> rerun());
        dirChip.addActionListener(e -> {
            up = !up;
            rerun();
        });

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 7));
        tabs.setOpaque(false);
        tabs.add(schedulerTab);
        tabs.add(historyTab);
        schedulerTab.addActionListener(e -> showTab("scheduler"));
        historyTab.addActionListener(e -> showTab("history"));
        bar.add(tabs, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSchedulerPage() {
        JPanel page = new JPanel(new BorderLayout(8, 0));
        page.setOpaque(false);

        // ---- left: request queue ----
        JPanel side = rounded(PANEL, new BorderLayout(0, 4));
        side.setPreferredSize(new Dimension(232, 0));
        side.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));

        JPanel head1 = new JPanel(new BorderLayout());
        head1.setOpaque(false);
        head1.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        JLabel title = new JLabel("Request queue");
        title.setFont(font(Font.BOLD, 15));
        title.setForeground(MUTED);
        head1.add(title, BorderLayout.WEST);
        TransportButton plus = new TransportButton(TransportButton.PLUS);
        plus.setToolTipText("Edit the request queue");
        plus.addActionListener(e -> editQueue());
        head1.add(plus, BorderLayout.EAST);

        JLabel sub = new JLabel("Order the head serves them");
        sub.setFont(font(Font.PLAIN, 12));
        sub.setForeground(MUTED);
        sub.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(head1, BorderLayout.NORTH);
        top.add(sub, BorderLayout.SOUTH);

        queueScroll.setBorder(null);
        queueScroll.setOpaque(false);
        queueScroll.getViewport().setOpaque(false);
        queueScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(queueScroll.getVerticalScrollBar());

        side.add(top, BorderLayout.NORTH);
        side.add(queueScroll, BorderLayout.CENTER);
        page.add(side, BorderLayout.WEST);

        // ---- right: the player ----
        JPanel main = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                g.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(), new float[]{0f, 0.45f, 1f},
                        new Color[]{new Color(30, 96, 60), new Color(26, 36, 30), new Color(20, 20, 20)}));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(hero, BorderLayout.CENTER);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        for (int i = 0; i < algoChips.length; i++) {
            final String name = DiskScheduler.ALGORITHMS[i];
            algoChips[i] = new PillButton(name);
            algoChips[i].addActionListener(e -> {
                algo = name;
                refreshChips();
                rerun();
            });
            chips.add(algoChips[i]);
        }
        north.add(chips, BorderLayout.SOUTH);
        main.add(north, BorderLayout.NORTH);
        main.add(chart, BorderLayout.CENTER);
        main.add(buildPlaybackCard(), BorderLayout.SOUTH);
        page.add(main, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildPlaybackCard() {
        JPanel card = rounded(CARD, new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        controls.setOpaque(false);
        TransportButton prev = new TransportButton(TransportButton.PREV);
        TransportButton next = new TransportButton(TransportButton.NEXT);
        prev.setToolTipText("Previous step");
        next.setToolTipText("Next step");
        prev.addActionListener(e -> seek(Math.max(0, Math.ceil(t - 1e-9) - 1)));
        next.addActionListener(e -> seek(Math.floor(t + 1e-9) + 1));
        playButton.addActionListener(e -> togglePlay());
        controls.add(prev);
        controls.add(playButton);
        controls.add(next);

        JPanel row1 = new JPanel(new GridLayout(1, 3));
        row1.setOpaque(false);
        row1.add(stepLabel);
        row1.add(controls);
        row1.add(nowLabel);

        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);
        movedLabel.setPreferredSize(new Dimension(80, 16));
        totalLabel.setPreferredSize(new Dimension(80, 16));
        row2.add(movedLabel, BorderLayout.WEST);
        row2.add(progress, BorderLayout.CENTER);
        row2.add(totalLabel, BorderLayout.EAST);

        card.add(row1, BorderLayout.CENTER);
        card.add(row2, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildHistoryPage() {
        JPanel page = rounded(PANEL, new BorderLayout(0, 8));
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 12));

        JPanel head1 = new JPanel(new BorderLayout());
        head1.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("History");
        title.setFont(font(Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Your previous simulations. Press Replay to watch one again.");
        sub.setFont(font(Font.PLAIN, 13));
        sub.setForeground(MUTED);
        titles.add(title);
        titles.add(sub);
        head1.add(titles, BorderLayout.WEST);
        PillButton clear = new PillButton("Clear history");
        clear.addActionListener(e -> {
            if (history.entries().isEmpty()) return;
            int ok = JOptionPane.showConfirmDialog(this, "Delete all saved runs?", "Clear history",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                history.clear();
                refreshHistory();
            }
        });
        JPanel clearWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        clearWrap.setOpaque(false);
        clearWrap.add(clear);
        head1.add(clearWrap, BorderLayout.EAST);
        page.add(head1, BorderLayout.NORTH);

        historyBody.setLayout(new BoxLayout(historyBody, BoxLayout.Y_AXIS));
        historyBody.setOpaque(false);
        JPanel holder = new JPanel(new BorderLayout());   // keeps the rows at the top
        holder.setOpaque(false);
        holder.add(historyBody, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        styleScrollBar(scroll.getVerticalScrollBar());
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private static JPanel rounded(Color fill, LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private static void styleScrollBar(JScrollBar bar) {
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setOpaque(false);
        bar.setUnitIncrement(14);
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            }

            @Override
            protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                g.setColor(new Color(90, 90, 90));
                g.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
                g.dispose();
            }
        });
    }

    // =====================================================================================
    // Running a simulation
    // =====================================================================================

    /**
     * Reads the inputs, runs the chosen algorithm and resets playback to the start.
     *
     * @param autoPlay start playing straight away
     * @param record   save this run in the history
     * @return false if the inputs were rejected (nothing changes on screen in that case)
     */
    private boolean run(boolean autoPlay, boolean record) {
        try {
            if (headField.getText().trim().isEmpty() || tracksField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Type the head position and the number of tracks first.");
            }
            int h = Integer.parseInt(headField.getText().trim());
            int tr = Integer.parseInt(tracksField.getText().trim());
            if (tr < 2) throw new IllegalArgumentException("Tracks must be at least 2.");
            int max = tr - 1;
            if (h < 0 || h > max) throw new IllegalArgumentException("The head must start between 0 and " + max + ".");
            if (queue.length == 0) throw new IllegalArgumentException("Add the requests first: press the + button in the Request queue box.");
            for (int r : queue) {
                if (r < 0 || r > max) throw new IllegalArgumentException("Track " + r + " is outside 0.." + max + ".");
            }
            head = h;
            tracks = tr;
            maxTrack = max;
            started = true;
        } catch (RuntimeException ex) {
            UiKit.error(this, ex.getMessage());
            return false;
        }

        stops = DiskScheduler.schedule(queue, head, maxTrack, up, algo);
        int n = stops.size();
        dist = new int[n];
        cum = new int[n];
        int cur = head;
        int sum = 0;
        for (int i = 0; i < n; i++) {
            dist[i] = Math.abs(stops.get(i).track - cur);
            sum += dist[i];
            cum[i] = sum;
            cur = stops.get(i).track;
        }
        total = sum;

        t = 0;
        lastStep = -1;
        recorded = false;
        playing = autoPlay && n > 0;
        if (playing) timer.start();
        else timer.stop();
        refreshChips();
        if (record) recordHistory();
        refreshAll();
        return true;
    }

    private void recordHistory() {
        boolean dir = DiskScheduler.usesDirection(algo);
        history.add(new DiskHistory.Entry(System.currentTimeMillis(), algo, dir ? up : true, head, tracks, queue, total));
        recorded = true;
    }

    /** The "+" button: lets the user type a new request queue. */
    private void editQueue() {
        Object answer = JOptionPane.showInputDialog(this,
                "Request queue (tracks separated by commas or spaces):", "Edit request queue",
                JOptionPane.PLAIN_MESSAGE, null, null, queueText(queue));
        if (answer == null) return;
        int[] old = queue;
        try {
            queue = UiKit.ints(answer.toString());
        } catch (RuntimeException ex) {
            queue = old;
            UiKit.error(this, ex.getMessage());
            return;
        }
        if (controller != null) {
            try {
                DiskSchedulingModel.Result result = controller.run(getHeadText(), getTracksText(), queue, algo, up);
                showResult(result);
                controller.record(result);
            } catch (RuntimeException ex) {
                queue = old;
                UiKit.error(this, ex.getMessage());
            }
        } else {
            if (!run(true, true)) queue = old;   // rejected (for example a track outside the disk): keep the old queue
        }
    }
    private void replay(DiskHistory.Entry e) {
        headField.setText(String.valueOf(e.head));
        tracksField.setText(String.valueOf(e.tracks));
        queue = e.queue.clone();
        up = e.up;
        algo = e.algo;
        showTab("scheduler");
        run(true, false);
        recorded = true;   // it is already in the history
    }

    // =====================================================================================
    // Playback
    // =====================================================================================

    private void tick() {
        int n = stops.size();
        t += 0.02 / STEP_SECONDS;
        if (t >= n) {
            t = n;
            pause();
        }
        refreshAll();
    }

    private void pause() {
        playing = false;
        timer.stop();
    }

    private void togglePlay() {
        if (stops.isEmpty()) return;
        if (playing) {
            pause();
        } else {
            if (t >= stops.size()) t = 0;
            playing = true;
            if (!recorded) recordHistory();
            timer.start();
        }
        refreshAll();
    }

    /** Jumps to a position (0 = start, k = arrived at stop k). */
    void seek(double newT) {
        t = Math.max(0, Math.min(stops.size(), newT));
        refreshAll();
    }

    /** Which stop the head is serving: 0 = still at the start, k = heading to / arrived at stop k. */
    private int step() {
        if (t <= 1e-9) return 0;
        return (int) Math.min(stops.size(), Math.ceil(t - 1e-9));
    }

    /** Head movement so far, including the part of the current step already travelled. */
    private double moved() {
        int n = stops.size();
        if (n == 0) return 0;
        int k = (int) Math.floor(t + 1e-9);
        if (k >= n) return total;
        double before = k == 0 ? 0 : cum[k - 1];
        return before + (t - k) * dist[k];
    }

    /** Inverse of moved(): moves the playhead to a given amount of head movement. */
    private void seekMoved(double m) {
        int n = stops.size();
        for (int k = 0; k < n; k++) {
            if (m <= cum[k] || k == n - 1) {
                double before = k == 0 ? 0 : cum[k - 1];
                seek(dist[k] == 0 ? k + 1 : k + Math.max(0, Math.min(1, (m - before) / dist[k])));
                return;
            }
        }
    }

    /** The head's track right now (it slides between stops while playing). */
    private double headTrack() {
        int n = stops.size();
        int k = (int) Math.floor(t + 1e-9);
        if (n == 0) return head;
        if (k >= n) return stops.get(n - 1).track;
        double from = k == 0 ? head : stops.get(k - 1).track;
        return from + (t - k) * (stops.get(k).track - from);
    }

    private boolean arrived() {
        return Math.abs(t - Math.rint(t)) < 1e-6;
    }

    // =====================================================================================
    // Refreshing the screen
    // =====================================================================================

    private void refreshAll() {
        int n = stops.size();
        int s = step();
        if (!started) {
            stepLabel.setText("");
            nowLabel.setText("");
            movedLabel.setText("");
            totalLabel.setText("");
        } else {
            stepLabel.setText("Step " + s + " of " + n);
            nowLabel.setText(s == 0 ? "Now: start (track " + head + ")" : "Now: Track " + stops.get(s - 1).track);
            movedLabel.setText((int) Math.floor(moved() + 1e-6) + " moved");
            totalLabel.setText(total + " total");
        }
        queuePanel.revalidate();
        queuePanel.repaint();
        hero.repaint();
        chart.repaint();
        progress.repaint();
        playButton.repaint();
        if (s != lastStep) {   // keep the current row in view
            lastStep = s;
            if (s > 0) {
                queuePanel.scrollRectToVisible(new Rectangle(0, Math.max(0, (s - 2) * QueuePanel.ROW),
                        10, QueuePanel.ROW * 4));
            }
        }
    }

    private void refreshChips() {
        for (int i = 0; i < algoChips.length; i++) {
            algoChips[i].setChosen(DiskScheduler.ALGORITHMS[i].equals(algo));
        }
        dirChip.setEnabled(DiskScheduler.usesDirection(algo));
        dirChip.setText(up ? "Up" : "Down");
    }

    private void showTab(String name) {
        schedulerTab.setChosen(name.equals("scheduler"));
        historyTab.setChosen(name.equals("history"));
        if (name.equals("history")) refreshHistory();
        cards.show(center, name);
    }

    private void refreshHistory() {
        historyBody.removeAll();
        List<DiskHistory.Entry> list = history.entries();
        if (list.isEmpty()) {
            JLabel empty = new JLabel("No runs yet. Press the green play button to run a simulation.");
            empty.setFont(font(Font.PLAIN, 14));
            empty.setForeground(MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 4, 0, 0));
            historyBody.add(empty);
        }
        for (DiskHistory.Entry e : list) {
            historyBody.add(new HistoryRow(e));
            historyBody.add(Box.createVerticalStrut(6));
        }
        historyBody.revalidate();
        historyBody.repaint();
    }

    private static String number(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private static String queueText(int[] q) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < q.length; i++) sb.append(i > 0 ? ", " : "").append(q[i]);
        return sb.toString();
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && fm.stringWidth(test) > width) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    // =====================================================================================
    // Hero (big icon, title, totals)
    // =====================================================================================

    private class HeroPanel extends JComponent {
        HeroPanel() {
            setPreferredSize(new Dimension(100, 176));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();

            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(4, 14, 140, 140, 34, 34);
            paintIcon(g, 0, 6, 140);

            // totals on the right
            double avg = queue.length == 0 ? 0 : (double) total / queue.length;
            String avgText = number(avg);
            String totText = String.valueOf(total);
            Font valueFont = font(Font.BOLD, 34);
            Font labelFont = font(Font.PLAIN, 12);
            FontMetrics vf = g.getFontMetrics(valueFont);
            FontMetrics lf = g.getFontMetrics(labelFont);
            int avgW = Math.max(vf.stringWidth(avgText), lf.stringWidth("Average"));
            int totW = Math.max(vf.stringWidth(totText), lf.stringWidth("Total head movement"));
            int avgRight = w;
            int totRight = w - avgW - 28;
            if (started) {
                drawRight(g, "Total head movement", labelFont, MUTED, totRight, 92);
                drawRight(g, totText, valueFont, GREEN, totRight, 128);
                drawRight(g, "Average", labelFont, MUTED, avgRight, 92);
                drawRight(g, avgText, valueFont, Color.WHITE, avgRight, 128);
            }

            // title block
            int tx = 164;
            int room = started ? Math.max(150, totRight - totW - 22 - tx) : Math.max(150, w - 22 - tx);
            g.setFont(font(Font.BOLD, 12));
            g.setColor(Color.WHITE);
            g.drawString("Playlist", tx, 24);
            Font titleFont = font(Font.BOLD, 32);
            g.setFont(titleFont);
            FontMetrics tf = g.getFontMetrics();
            int y = 24 + 32;
            for (String line : wrap("Disk Scheduling", tf, room)) {
                g.drawString(line, tx, y);
                y += 35;
            }
            String dir = DiskScheduler.usesDirection(algo) ? (up ? " · moving up" : " · moving down") : "";
            String subtitle = started
                    ? algo + dir + " · " + queue.length + " requests · head starts at " + head
                            + " · tracks 0 to " + maxTrack
                    : "Type the head position and the tracks above, then press + to add requests.";
            g.setFont(font(Font.PLAIN, 13));
            g.setColor(MUTED);
            FontMetrics sf = g.getFontMetrics();
            y -= 12;
            for (String line : wrap(subtitle, sf, room)) {
                g.drawString(line, tx, y);
                y += 17;
            }
            if (algo.equals("C-SCAN")) {
                for (int i = 0; i < stops.size(); i++) {
                    if (stops.get(i).kind == DiskScheduler.Kind.JUMP) {
                        g.setFont(font(Font.ITALIC, 12));
                        g.setColor(DIM);
                        g.drawString("The " + dist[i] + "-track return jump is counted.", tx, y + 2);
                        break;
                    }
                }
            }
            g.dispose();
        }

        private void drawRight(Graphics2D g, String text, Font f, Color c, int right, int baseline) {
            g.setFont(f);
            g.setColor(c);
            g.drawString(text, right - g.getFontMetrics().stringWidth(text), baseline);
        }
    }

    // =====================================================================================
    // Request queue list
    // =====================================================================================

    private class QueuePanel extends JPanel {
        static final int ROW = 52;

        QueuePanel() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = (e.getY() - 4) / ROW;
                    if (i >= 0 && i < stops.size()) {
                        pause();
                        seek(i + 1);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, stops.isEmpty() ? 70 : stops.size() * ROW + 8);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();
            if (stops.isEmpty()) {
                g.setFont(font(Font.BOLD, 13));
                g.setColor(MUTED);
                g.drawString("No requests yet", 8, 22);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(DIM);
                g.drawString("Press + to add some", 8, 40);
                g.dispose();
                return;
            }
            int cur = step() - 1;
            boolean moving = !arrived();
            for (int i = 0; i < stops.size(); i++) {
                DiskScheduler.Stop s = stops.get(i);
                int y = 4 + i * ROW;
                if (i == cur) {
                    g.setColor(ROW_ON);
                    g.fillRoundRect(2, y, w - 4, ROW - 4, 10, 10);
                }
                // number badge
                g.setColor(i == cur ? GREEN : TILE);
                g.fillRoundRect(10, y + 6, 36, 36, 8, 8);
                if (i == cur) {
                    g.setColor(new Color(10, 10, 10));
                    g.fillRect(20, y + 24, 4, 10);
                    g.fillRect(26, y + 16, 4, 18);
                    g.fillRect(32, y + 21, 4, 13);
                } else {
                    g.setColor(MUTED);
                    g.setFont(font(Font.PLAIN, 15));
                    String num = String.valueOf(i + 1);
                    g.drawString(num, 28 - g.getFontMetrics().stringWidth(num) / 2, y + 30);
                }
                // text
                g.setFont(font(Font.BOLD, 14));
                g.setColor(i == cur ? GREEN : Color.WHITE);
                g.drawString("Track " + s.track, 58, y + 21);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(i == cur ? GREEN : MUTED);
                g.drawString(describe(i, cur, moving), 58, y + 37);
                // done check
                if (i < cur) {
                    g.setColor(GREEN);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = w - 26;
                    int cy = y + 24;
                    g.drawPolyline(new int[]{cx - 5, cx - 1, cx + 6}, new int[]{cy, cy + 4, cy - 5}, 3);
                }
            }
            g.dispose();
        }

        private String describe(int i, int cur, boolean moving) {
            DiskScheduler.Kind kind = stops.get(i).kind;
            int d = dist[i];
            String note = kind == DiskScheduler.Kind.EDGE ? " · disk edge" : "";
            if (i < cur) return (kind == DiskScheduler.Kind.JUMP ? "jumped " : "moved ") + d + note;
            if (i == cur) {
                if (kind == DiskScheduler.Kind.JUMP) return "jumping back · " + d;
                return (moving ? "moving now · " : "head is here · ") + d + note;
            }
            return (kind == DiskScheduler.Kind.JUMP ? "will jump " : "will move ") + d + note;
        }
    }

    // =====================================================================================
    // Head-movement chart
    // =====================================================================================

    private class ChartPanel extends JComponent {
        ChartPanel() {
            setPreferredSize(new Dimension(300, 190));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(CARD);
            g.fillRoundRect(0, 0, w, h, 12, 12);

            g.setFont(font(Font.BOLD, 13));
            g.setColor(Color.WHITE);
            String t1 = "Head movement";
            g.drawString(t1, 14, 24);
            int t1w = g.getFontMetrics().stringWidth(t1);
            g.setFont(font(Font.PLAIN, 12));
            g.setColor(MUTED);
            g.drawString("each dot is a track the head visits", 14 + t1w + 8, 24);

            if (!started) {
                g.setFont(font(Font.BOLD, 15));
                g.setColor(MUTED);
                String m1 = "No requests yet";
                g.drawString(m1, (w - g.getFontMetrics().stringWidth(m1)) / 2, h / 2 - 4);
                g.setFont(font(Font.PLAIN, 12));
                g.setColor(DIM);
                String m2 = "Enter the head, tracks and requests to see the head movement";
                g.drawString(m2, (w - g.getFontMetrics().stringWidth(m2)) / 2, h / 2 + 16);
                g.dispose();
                return;
            }
            int n = stops.size();
            int left = 44;
            int right = 44;
            int top = 58;
            int bottom = 18;
            int plotW = Math.max(10, w - left - right);
            int plotH = Math.max(10, h - top - bottom);

            // vertical gridlines and track numbers across the top
            g.setFont(font(Font.PLAIN, 10));
            for (int tick : ticks()) {
                int x = left + (int) Math.round(tick * (double) plotW / maxTrack);
                g.setColor(new Color(46, 46, 46));
                g.setStroke(new BasicStroke(1f));
                g.drawLine(x, top - 8, x, h - bottom);
                g.setColor(DIM);
                String label = String.valueOf(tick);
                g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, top - 16);
            }
            if (n == 0) {
                g.dispose();
                return;
            }

            double[] px = new double[n + 1];
            double[] py = new double[n + 1];
            px[0] = left + head * (double) plotW / maxTrack;
            py[0] = top;
            for (int i = 1; i <= n; i++) {
                px[i] = left + stops.get(i - 1).track * (double) plotW / maxTrack;
                py[i] = top + i * (double) plotH / n;
            }
            int k = (int) Math.floor(t + 1e-9);   // stops fully reached
            double frac = t - k;
            double hx;
            double hy;
            if (k >= n) {
                hx = px[n];
                hy = py[n];
            } else {
                hx = px[k] + frac * (px[k + 1] - px[k]);
                hy = py[k] + frac * (py[k + 1] - py[k]);
            }

            // upcoming path: dashed gray
            if (k < n) {
                g.setColor(new Color(100, 100, 100));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
                Path2D.Double future = new Path2D.Double();
                future.moveTo(hx, hy);
                for (int i = k + 1; i <= n; i++) future.lineTo(px[i], py[i]);
                g.draw(future);
            }

            // travelled path: solid green
            g.setColor(GREEN);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double done = new Path2D.Double();
            done.moveTo(px[0], py[0]);
            for (int i = 1; i <= Math.min(k, n); i++) done.lineTo(px[i], py[i]);
            if (k < n) done.lineTo(hx, hy);
            g.draw(done);

            // dots and labels
            boolean room = plotH / (double) n >= 13;
            for (int i = 0; i <= n; i++) {
                boolean reached = i <= k;
                boolean headHere = t > 0 && ((k >= n && i == n) || (i == k && frac < 1e-6 && k > 0));
                if (i == 0) {
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(2f));
                    g.draw(circle(px[0], py[0], 4.5));
                    g.setColor(CARD);
                    g.fill(circle(px[0], py[0], 3));
                    g.setFont(font(Font.PLAIN, 11));
                    g.setColor(Color.WHITE);
                    String s0 = "start " + head;
                    int sw = g.getFontMetrics().stringWidth(s0);
                    if (px[0] - sw - 10 > 4) g.drawString(s0, (int) px[0] - sw - 10, (int) py[0] + 4);
                    else g.drawString(s0, (int) px[0] + 9, (int) py[0] + 4);
                    continue;
                }
                if (headHere) continue;   // the head marker below covers this dot
                g.setColor(reached ? GREEN : new Color(110, 110, 110));
                g.fill(circle(px[i], py[i], reached ? 3.6 : 3.2));
                if (room) {
                    DiskScheduler.Stop s = stops.get(i - 1);
                    String label = String.valueOf(s.track);
                    if (s.kind == DiskScheduler.Kind.EDGE) label += " (edge)";
                    if (s.kind == DiskScheduler.Kind.JUMP) label += " (jump)";
                    g.setFont(font(Font.PLAIN, 10));
                    g.setColor(reached ? new Color(200, 200, 200) : DIM);
                    labelAt(g, label, px[i], py[i], w, 10);
                }
            }

            // the head itself
            if (t > 0) {
                g.setColor(new Color(30, 215, 96, 70));
                g.fill(circle(hx, hy, 11));
                g.setColor(GREEN);
                g.fill(circle(hx, hy, 6.5));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(circle(hx, hy, 6.5));
                g.setFont(font(Font.BOLD, 11));
                g.setColor(GREEN);
                String label = (int) Math.round(headTrack()) + (arrived() ? " · head is here" : "");
                labelAt(g, label, hx, hy, w, 17);
            }
            g.dispose();
        }

        /** Draws the label beside a dot, flipping to the left near the right edge. */
        private void labelAt(Graphics2D g, String text, double x, double y, int panelWidth, int gap) {
            int tw = g.getFontMetrics().stringWidth(text);
            int tx = (int) x + gap;
            if (tx + tw > panelWidth - 6) tx = (int) x - tw - gap;
            g.drawString(text, tx, (int) y + 4);
        }

        private List<Integer> ticks() {
            int[] nice = {5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000, 10000};
            int step = nice[nice.length - 1];
            for (int c : nice) {
                if (maxTrack / (double) c <= 4.0) {
                    step = c;
                    break;
                }
            }
            List<Integer> list = new ArrayList<>();
            for (int v = 0; v <= maxTrack; v += step) list.add(v);
            int last = list.get(list.size() - 1);
            if (last != maxTrack) {
                if (maxTrack - last < maxTrack * 0.06 && list.size() > 1) list.remove(list.size() - 1);
                list.add(maxTrack);
            }
            return list;
        }
    }

    // =====================================================================================
    // Progress bar (also a slider: click or drag to scrub)
    // =====================================================================================

    private class ProgressBar extends JComponent {
        ProgressBar() {
            setPreferredSize(new Dimension(100, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter scrub = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pause();
                    set(e.getX());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    set(e.getX());
                }

                private void set(int x) {
                    double ratio = Math.max(0, Math.min(1, x / (double) Math.max(1, getWidth())));
                    seekMoved(ratio * total);
                }
            };
            addMouseListener(scrub);
            addMouseMotionListener(scrub);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();
            int cy = getHeight() / 2;
            double ratio = total == 0 ? (t > 0 ? 1 : 0) : Math.min(1, moved() / total);
            int fill = (int) Math.round(ratio * w);
            g.setColor(new Color(84, 84, 84));
            g.fillRoundRect(0, cy - 2, w, 4, 4, 4);
            g.setColor(GREEN);
            g.fillRoundRect(0, cy - 2, fill, 4, 4, 4);
            g.setColor(Color.WHITE);
            g.fill(circle(Math.max(6, Math.min(w - 6, fill)), cy, 6));
            g.dispose();
        }
    }

    // =====================================================================================
    // History row
    // =====================================================================================

    private class HistoryRow extends JPanel {
        HistoryRow(DiskHistory.Entry e) {
            super(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            setAlignmentX(LEFT_ALIGNMENT);

            JComponent tile = new JComponent() {
                {
                    setPreferredSize(new Dimension(48, 48));
                }

                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    smooth(g);
                    g.setPaint(new GradientPaint(0, 0, new Color(42, 208, 98), 0, 48, new Color(16, 126, 56)));
                    g.fillRoundRect(0, 0, 48, 48, 12, 12);
                    g.setColor(new Color(8, 8, 8));
                    g.setFont(font(Font.BOLD, e.algo.length() > 4 ? 11 : 13));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(e.algo, (48 - fm.stringWidth(e.algo)) / 2, 29);
                    g.dispose();
                }
            };
            add(tile, BorderLayout.WEST);

            boolean dir = DiskScheduler.usesDirection(e.algo);
            JLabel title = new JLabel(e.algo + (dir ? " · " + (e.up ? "up" : "down") : "") + " · "
                    + e.total + " tracks of head movement");
            title.setFont(font(Font.BOLD, 14));
            title.setForeground(Color.WHITE);
            JLabel detail = new JLabel("Head " + e.head + " · " + e.tracks + " tracks · " + e.queue.length
                    + " requests · average " + number(e.average()) + " · Queue: " + queueText(e.queue));
            detail.setFont(font(Font.PLAIN, 12));
            detail.setForeground(MUTED);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(Box.createVerticalGlue());
            text.add(title);
            text.add(Box.createVerticalStrut(3));
            text.add(detail);
            text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);

            JLabel when = new JLabel(new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date(e.time)));
            when.setFont(font(Font.PLAIN, 12));
            when.setForeground(DIM);
            PillButton replay = new PillButton("Replay");
            replay.addActionListener(ev -> replay(e));
            JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            east.setOpaque(false);
            east.add(when);
            east.add(replay);
            add(east, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            g.setColor(CARD);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
        }
    }

    // =====================================================================================
    // Small controls
    // =====================================================================================

    /** Rounded dark number field used in the top bar. */
    private static class RoundField extends JTextField {
        private final String hint;

        RoundField(String hint, int columns) {
            super("", columns);
            this.hint = hint;
            setOpaque(false);
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
            setFont(font(Font.BOLD, 14));
            setHorizontalAlignment(CENTER);
            setSelectionColor(new Color(30, 215, 96, 130));
            setSelectedTextColor(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            g.setColor(new Color(8, 8, 8));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(g0);
            if (getText().isEmpty() && hint != null) {   // gray example, only while the field is empty
                Graphics2D h = (Graphics2D) g0.create();
                smooth(h);
                h.setFont(font(Font.PLAIN, 13));
                h.setColor(new Color(112, 112, 112));
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                h.dispose();
            }
        }
    }

    /** Rounded text button: white when chosen, dark gray otherwise. */
    private static class PillButton extends JButton {
        private boolean chosen;
        private boolean hover;

        PillButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
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
        }

        void setChosen(boolean value) {
            chosen = value;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(Math.max(48, fm.stringWidth(getText()) + 34), 32);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            Color fill = !isEnabled() ? new Color(30, 30, 30) : chosen ? Color.WHITE : hover ? new Color(66, 66, 66) : TILE;
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setFont(getFont());
            g.setColor(!isEnabled() ? new Color(90, 90, 90) : chosen ? Color.BLACK : Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Round icon buttons: run, play/pause, previous, next and plus. */
    private class TransportButton extends JButton {
        static final int RUN = 0;
        static final int PLAY = 1;
        static final int PREV = 2;
        static final int NEXT = 3;
        static final int PLUS = 4;
        private final int kind;
        private boolean hover;

        TransportButton(int kind) {
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
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
        }

        @Override
        public Dimension getPreferredSize() {
            return kind == RUN ? new Dimension(38, 38) : kind == PLAY ? new Dimension(40, 40) : new Dimension(28, 28);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            double cx = w / 2.0;
            double cy = h / 2.0;
            switch (kind) {
                case RUN:
                    g.setColor(hover ? new Color(60, 230, 120) : GREEN);
                    g.fill(circle(cx, cy, Math.min(w, h) / 2.0 - 1));
                    g.setColor(new Color(8, 8, 8));
                    g.fillPolygon(new int[]{(int) cx - 4, (int) cx - 4, (int) cx + 7}, new int[]{(int) cy - 7, (int) cy + 7, (int) cy}, 3);
                    break;
                case PLAY:
                    g.setColor(hover ? new Color(235, 235, 235) : Color.WHITE);
                    g.fill(circle(cx, cy, Math.min(w, h) / 2.0 - 1));
                    g.setColor(new Color(8, 8, 8));
                    if (playing) {
                        g.fillRect((int) cx - 6, (int) cy - 7, 4, 14);
                        g.fillRect((int) cx + 2, (int) cy - 7, 4, 14);
                    } else {
                        g.fillPolygon(new int[]{(int) cx - 4, (int) cx - 4, (int) cx + 7}, new int[]{(int) cy - 8, (int) cy + 8, (int) cy}, 3);
                    }
                    break;
                case PREV:
                    g.setColor(hover ? Color.WHITE : new Color(190, 190, 190));
                    g.fillRect((int) cx - 7, (int) cy - 7, 2, 14);
                    g.fillPolygon(new int[]{(int) cx + 7, (int) cx + 7, (int) cx - 5}, new int[]{(int) cy - 7, (int) cy + 7, (int) cy}, 3);
                    break;
                case NEXT:
                    g.setColor(hover ? Color.WHITE : new Color(190, 190, 190));
                    g.fillRect((int) cx + 5, (int) cy - 7, 2, 14);
                    g.fillPolygon(new int[]{(int) cx - 7, (int) cx - 7, (int) cx + 5}, new int[]{(int) cy - 7, (int) cy + 7, (int) cy}, 3);
                    break;
                default:   // PLUS
                    g.setColor(hover ? Color.WHITE : MUTED);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine((int) cx - 7, (int) cy, (int) cx + 7, (int) cy);
                    g.drawLine((int) cx, (int) cy - 7, (int) cx, (int) cy + 7);
            }
            g.dispose();
        }
    }
}

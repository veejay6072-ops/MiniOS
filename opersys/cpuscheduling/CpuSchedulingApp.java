package opersys.cpuscheduling;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CPU Scheduling window ("SchedStream" design). This file is DESIGN ONLY: all the scheduling
 * logic lives in CpuScheduler. The window starts empty; the user adds processes and presses Run.
 */
public class CpuSchedulingApp extends JPanel {
    private static final String FONT = "Segoe UI";
    private static final Color RED = new Color(214, 31, 31);
    private static final Color INK = new Color(20, 20, 22);
    private static final Color MUTED = new Color(112, 112, 118);
    private static final Color PILL_GRAY = new Color(240, 240, 240);
    private static final Color[] TILE_COLORS = {
            new Color(92, 190, 255), new Color(255, 178, 74), new Color(126, 222, 136),
            new Color(214, 140, 255), new Color(255, 120, 150), new Color(255, 224, 90),
            new Color(80, 214, 200), new Color(255, 140, 90)};

    private static Color tileColor(int index) {
        return TILE_COLORS[Math.floorMod(index, TILE_COLORS.length)];
    }

    private static Font font(int style, int size) {
        return new Font(FONT, style, size);
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t),
                (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    // ---------- parts of the window ----------

    private final JComboBox<CpuScheduler.Algorithm> algoBox = new JComboBox<>(CpuScheduler.Algorithm.values());
    private final RoundField quantumField = new RoundField("\u2013", 3);
    private final JLabel quantumLabel = new JLabel("Quantum");
    private final JPanel queueList = new JPanel();
    private final JLabel queueSub = new JLabel();
    private final List<ProcessRow> rows = new ArrayList<>();
    private final Stage stage = new Stage();
    private final Summary summary = new Summary();
    private JScrollPane queueScroll;
    // pieces that relayout() moves around when the window gets narrow
    private JPanel headerPanel, bodyPanel, brandPanel, pillPanel, eastPanel, leftColumn, queuePanel;
    private JLabel section;
    private QueueHolder queueHolder;

    // ---- controller integration ----
    private Runnable runListener;

    /** The controller plugs into this, so the screen can say "the user pressed Run" without knowing any model stuff. */
    public void setListener(Runnable listener) {
        this.runListener = listener;
    }

    /** The raw text from the quantum box. */
    public String getQuantumText() {
        return quantumField.getText();
    }

    /** The algorithm the user chose. */
    public CpuScheduler.Algorithm getSelectedAlgorithm() {
        return (CpuScheduler.Algorithm) algoBox.getSelectedItem();
    }

    /** The process rows on screen (for the controller to read). */
    public List<ProcessRow> getProcessRows() {
        return rows;
    }

    /** Shows a new simulation result on the stage and summary. */
    public void showResult(CpuModel.Result result) {
        CpuScheduler.Result schedulerResult = new CpuScheduler.Result(
                result.getAlgorithm(), result.getQuantum(), result.getSlices(),
                result.getRows(), result.getTotalTime(),
                result.getAverageWaiting(), result.getAverageTurnaround());
        summary.showResult(schedulerResult);
        stage.play(schedulerResult);
    }

    /** Shows an error message in the summary area. */
    public void showError(String message) {
        summary.showError(message);
    }

    public CpuSchedulingApp() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        Page page = new Page();
        page.add(buildHeader(), BorderLayout.NORTH);
        page.add(buildBody(), BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(page, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });
        relayout();
        algoBox.addActionListener(e -> onAlgorithmChanged());
        onAlgorithmChanged();
        refreshQueue();   // starts empty: the user adds the processes
    }

    // ---------- building the window ----------

    private JPanel buildHeader() {
        headerPanel = new JPanel(new BorderLayout(24, 0));
        JPanel header = headerPanel;
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 232)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(new Logo(40, false));
        JLabel name = new JLabel("SchedStream");
        name.setFont(font(Font.BOLD, 22));
        name.setForeground(INK);
        brand.add(name);

        JPanel pillHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillHolder.setOpaque(false);
        pillHolder.add(buildPill());

        section = new JLabel("CPU Scheduling");
        section.setFont(font(Font.PLAIN, 14));
        section.setForeground(MUTED);
        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.add(section);

        brandPanel = brand;
        pillPanel = pillHolder;
        eastPanel = east;
        header.add(brand, BorderLayout.WEST);
        header.add(pillHolder, BorderLayout.CENTER);
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /** The rounded control bar: algorithm drop-down, quantum box and the red Run button. */
    private JComponent buildPill() {
        ControlPill pill = new ControlPill();
        pill.setLayout(new BorderLayout());
        pill.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pill.setPreferredSize(new Dimension(470, 42));

        styleCombo();
        algoBox.setPreferredSize(new Dimension(210, 40));
        pill.add(algoBox, BorderLayout.WEST);

        quantumLabel.setFont(font(Font.PLAIN, 13));
        quantumField.setPreferredSize(new Dimension(44, 30));
        JPanel quantum = new JPanel(new GridBagLayout());
        quantum.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 4, 0, 12);
        quantum.add(new Divider(), gc);
        gc.insets = new Insets(0, 0, 0, 8);
        quantum.add(quantumLabel, gc);
        quantum.add(quantumField, gc);
        pill.add(quantum, BorderLayout.CENTER);

        RunButton run = new RunButton();
        run.setPreferredSize(new Dimension(92, 40));
        run.addActionListener(e -> runSimulation());
        pill.add(run, BorderLayout.EAST);
        return pill;
    }

    private void styleCombo() {
        algoBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(INK);
                        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusable(false);
                b.setPreferredSize(new Dimension(34, 30));
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // no background: the pill draws it
            }
        });
        algoBox.setOpaque(false);
        algoBox.setFocusable(false);
        algoBox.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        algoBox.setFont(font(Font.PLAIN, 15));
        algoBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setFont(font(Font.PLAIN, 15));
                l.setForeground(INK);
                l.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                l.setOpaque(index >= 0);   // the closed box stays transparent
                l.setBackground(isSelected ? new Color(255, 232, 232) : Color.WHITE);
                return l;
            }
        });
    }

    private JPanel buildBody() {
        bodyPanel = new JPanel(new BorderLayout(16, 0));
        JPanel body = bodyPanel;
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel left = new JPanel(new BorderLayout(0, 14));
        leftColumn = left;
        left.setOpaque(false);
        left.add(stage, BorderLayout.CENTER);
        left.add(summary, BorderLayout.SOUTH);

        body.add(left, BorderLayout.CENTER);
        queuePanel = buildQueuePanel();
        body.add(queuePanel, BorderLayout.EAST);
        return body;
    }

    private JPanel buildQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(316, 100));

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        JLabel title = new JLabel("Process queue");
        title.setFont(font(Font.BOLD, 18));
        title.setForeground(INK);
        queueSub.setFont(font(Font.PLAIN, 12));
        queueSub.setForeground(MUTED);
        head.add(title);
        head.add(Box.createVerticalStrut(2));
        head.add(queueSub);

        queueList.setLayout(new BoxLayout(queueList, BoxLayout.Y_AXIS));
        queueList.setOpaque(false);
        QueueHolder holder = new QueueHolder();
        queueHolder = holder;
        holder.add(queueList, BorderLayout.NORTH);
        queueScroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        queueScroll.setBorder(null);
        queueScroll.getViewport().setBackground(Color.WHITE);
        queueScroll.getVerticalScrollBar().setUnitIncrement(16);

        PillButton add = new PillButton("+  Add process");
        add.addActionListener(e -> addProcess());

        panel.add(head, BorderLayout.NORTH);
        panel.add(queueScroll, BorderLayout.CENTER);
        panel.add(add, BorderLayout.SOUTH);
        return panel;
    }

    // ---------- responsive layout ----------

    /**
     * Rearranges the header and body for the current window width, so nothing overlaps:
     * narrow windows stack the brand above the control bar and move the queue under the stage.
     * Anything that still does not fit gets a scroll bar instead of being drawn on top of something.
     */
    private void relayout() {
        int w = getWidth() > 0 ? getWidth() : 1000;   // 0 before the window is shown
        boolean stackHeader = w < 720;
        boolean queueBelow = w < 940;

        headerPanel.removeAll();
        if (stackHeader) {
            headerPanel.add(brandPanel, BorderLayout.NORTH);
            headerPanel.add(pillPanel, BorderLayout.CENTER);
        } else {
            headerPanel.add(brandPanel, BorderLayout.WEST);
            headerPanel.add(pillPanel, BorderLayout.CENTER);
            headerPanel.add(eastPanel, BorderLayout.EAST);
        }
        section.setVisible(w >= 900);

        bodyPanel.removeAll();
        bodyPanel.add(leftColumn, BorderLayout.CENTER);
        if (queueBelow) {
            // Narrow: the queue sits under the stage and grows with its rows. There is NO inner scroll
            // pane here, so the mouse wheel reaches the page and scrolls the whole window.
            queuePanel.setPreferredSize(null);
            if (queueScroll.getParent() == queuePanel) {
                queuePanel.remove(queueScroll);
                queuePanel.add(queueHolder, BorderLayout.CENTER);
            }
            bodyPanel.add(queuePanel, BorderLayout.SOUTH);
        } else {
            // Wide: the queue is a column on the right with its own scroll bar.
            queuePanel.setPreferredSize(new Dimension(316, 100));
            if (queueHolder.getParent() == queuePanel) {
                queuePanel.remove(queueHolder);
                queueScroll.setViewportView(queueHolder);
                queuePanel.add(queueScroll, BorderLayout.CENTER);
            }
            bodyPanel.add(queuePanel, BorderLayout.EAST);
        }

        headerPanel.revalidate();
        bodyPanel.revalidate();
        revalidate();
        repaint();
    }

    /** The page inside the scroll pane: fills the window when there is room, otherwise it scrolls. */
    private static class Page extends JPanel implements Scrollable {
        Page() {
            super(new BorderLayout());
            setBackground(Color.WHITE);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            Container p = getParent();
            return !(p instanceof JViewport) || ((JViewport) p).getWidth() >= getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            Container p = getParent();
            return p instanceof JViewport && ((JViewport) p).getHeight() > getPreferredSize().height;
        }
    }

    // ---------- what the window does (it only collects input and shows results) ----------

    private void onAlgorithmChanged() {
        Object selected = algoBox.getSelectedItem();
        boolean priority = selected == CpuScheduler.Algorithm.PRIORITY;
        boolean roundRobin = selected == CpuScheduler.Algorithm.ROUND_ROBIN;
        for (ProcessRow r : rows) r.setPriorityVisible(priority);
        quantumField.setEnabled(roundRobin);
        quantumLabel.setForeground(roundRobin ? MUTED : new Color(190, 190, 196));
        queueList.revalidate();
        queueList.repaint();
    }

    private void addProcess() {
        ProcessRow row = new ProcessRow();
        row.setPriorityVisible(algoBox.getSelectedItem() == CpuScheduler.Algorithm.PRIORITY);
        rows.add(row);
        refreshQueue();
        SwingUtilities.invokeLater(() -> {
            row.scrollRectToVisible(new Rectangle(0, 0, row.getWidth(), row.getHeight()));
            row.arrival.requestFocusInWindow();
        });
    }

    private void refreshQueue() {
        queueList.removeAll();
        if (rows.isEmpty()) {
            JLabel empty = new JLabel("<html><center>Nothing here yet.<br>Press <b>+ Add process</b> to start.</center></html>",
                    SwingConstants.CENTER);
            empty.setFont(font(Font.PLAIN, 13));
            empty.setForeground(MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(48, 0, 0, 0));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            queueList.add(empty);
            queueSub.setText("No processes yet \u00b7 add one below");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                ProcessRow r = rows.get(i);
                r.setIndex(i);
                queueList.add(r);
                queueList.add(Box.createVerticalStrut(10));
            }
            queueSub.setText(rows.size() + (rows.size() == 1 ? " process" : " processes")
                    + " \u00b7 number on tile = burst time");
        }
        queueList.revalidate();
        queueList.repaint();
        revalidate();   // re-measure the whole page, so the outer scroll bar knows the new height
        repaint();
    }

    private void runSimulation() {
        if (runListener != null) runListener.run();
    }

    private static int readInt(JTextField field, String message) {
        String text = field.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException(message);
        return Integer.parseInt(text);
    }

    private static JPanel labeled(String text, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 11));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static void onTextChange(JTextComponent field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    // ---------- one process in the queue ----------

    final class ProcessRow extends JPanel {
        private int index;
        private final Tile tile = new Tile();
        private final RoundField arrival = new RoundField("0", 4);
        private final RoundField burst = new RoundField("1+", 4);
        private final RoundField priority = new RoundField("1", 3);
        private final JPanel fields = new JPanel(new GridLayout(1, 0, 6, 0));
        private final JPanel arrivalBox = labeled("Arrival", arrival);
        private final JPanel burstBox = labeled("Burst", burst);
        private final JPanel priorityBox = labeled("Priority", priority);

        ProcessRow() {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(280, 62));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

            fields.setOpaque(false);
            setPriorityVisible(false);
            JPanel middle = new JPanel(new GridBagLayout());
            middle.setOpaque(false);
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            middle.add(fields, gc);

            JButton remove = new JButton("\u00d7");
            remove.setFont(font(Font.PLAIN, 20));
            remove.setForeground(new Color(160, 160, 166));
            remove.setContentAreaFilled(false);
            remove.setBorderPainted(false);
            remove.setFocusPainted(false);
            remove.setMargin(new Insets(0, 0, 0, 0));
            remove.setPreferredSize(new Dimension(22, 62));
            remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            remove.setToolTipText("Remove this process");
            remove.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    remove.setForeground(RED);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    remove.setForeground(new Color(160, 160, 166));
                }
            });
            remove.addActionListener(e -> {
                rows.remove(ProcessRow.this);
                refreshQueue();
            });

            add(tile, BorderLayout.WEST);
            add(middle, BorderLayout.CENTER);
            add(remove, BorderLayout.EAST);

            onTextChange(burst, () -> {
                String t = burst.getText().trim();
                tile.setBurst(t.isEmpty() ? 0 : Integer.parseInt(t));
            });
        }

        void setIndex(int i) {
            index = i;
            tile.setIndex(i);
        }

        void setPriorityVisible(boolean show) {
            fields.removeAll();
            fields.add(arrivalBox);
            fields.add(burstBox);
            if (show) fields.add(priorityBox);
            fields.revalidate();
            fields.repaint();
        }

        /** The index of this process (0-based). */
        public int getIndex() {
            return index;
        }

        /** The raw text from the arrival box. */
        public String getArrivalText() {
            return arrival.getText();
        }

        /** The raw text from the burst box. */
        public String getBurstText() {
            return burst.getText();
        }

        /** The raw text from the priority box (empty when not used). */
        public String getPriorityText() {
            return priority.getText();
        }
    }

    /** The colored square with the process name and a small badge showing its burst time. */
    private static class Tile extends JComponent {
        private int index;
        private int burst;

        Tile() {
            setPreferredSize(new Dimension(70, 62));
        }

        void setIndex(int i) {
            index = i;
            repaint();
        }

        void setBurst(int b) {
            burst = b;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(tileColor(index));
            g2.fillRoundRect(0, 0, w, h, 18, 18);
            g2.setColor(INK);
            g2.setFont(font(Font.BOLD, 19));
            FontMetrics fm = g2.getFontMetrics();
            String name = "P" + (index + 1);
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2 - 2);
            if (burst > 0) {
                g2.setFont(font(Font.BOLD, 11));
                FontMetrics bf = g2.getFontMetrics();
                String text = String.valueOf(burst);
                int bw = Math.max(18, bf.stringWidth(text) + 10);
                int bh = 16;
                int bx = w - bw - 5;
                int by = h - bh - 5;
                g2.setColor(new Color(28, 28, 30));
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(text, bx + (bw - bf.stringWidth(text)) / 2, by + (bh + bf.getAscent() - bf.getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    // ---------- the dark "now running" stage ----------

    /** Draws a finished simulation as an animation: current process, timeline, play/pause. */
    private static class Stage extends JPanel {
        private CpuScheduler.Result result;
        private final Map<String, Integer> order = new HashMap<>();
        private final Map<String, Integer> bursts = new HashMap<>();
        private double time;          // current position on the timeline, in time units
        private double speed = 2;     // time units per second
        private boolean playing;
        private boolean scrubbing;
        private long lastTick;
        private final Timer timer = new Timer(16, e -> tick());

        Stage() {
            setOpaque(false);
            setPreferredSize(new Dimension(560, 350));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (result == null) return;
                    if (playButton().contains(e.getPoint())) {
                        togglePlay();
                    } else if (barHit().contains(e.getPoint())) {
                        scrubbing = true;
                        scrubTo(e.getX());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (scrubbing) scrubTo(e.getX());
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    scrubbing = false;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean hand = result != null
                            && (playButton().contains(e.getPoint()) || barHit().contains(e.getPoint()));
                    setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        void play(CpuScheduler.Result r) {
            result = r;
            order.clear();
            bursts.clear();
            for (int i = 0; i < r.rows.size(); i++) {
                order.put(r.rows.get(i).name, i);
                bursts.put(r.rows.get(i).name, r.rows.get(i).burst);
            }
            speed = Math.max(2.0, r.totalTime / 10.0);
            time = 0;
            scrubbing = false;
            setPlaying(true);
        }

        private void setPlaying(boolean on) {
            playing = on;
            if (on) {
                lastTick = System.nanoTime();
                timer.start();
            } else {
                timer.stop();
            }
            repaint();
        }

        private void togglePlay() {
            if (result == null) return;
            if (time >= result.totalTime) {   // finished: play again from the start
                time = 0;
                setPlaying(true);
            } else {
                setPlaying(!playing);
            }
        }

        private void tick() {
            long now = System.nanoTime();
            double dt = (now - lastTick) / 1e9;
            lastTick = now;
            if (playing && result != null) {
                time += dt * speed;
                if (time >= result.totalTime) {
                    time = result.totalTime;
                    setPlaying(false);
                    return;
                }
            }
            repaint();
        }

        private void scrubTo(int x) {
            Rectangle b = bar();
            double f = Math.max(0, Math.min(1, (x - b.x) / (double) b.width));
            time = f * result.totalTime;
            repaint();
        }

        private Rectangle bar() {
            return new Rectangle(24, getHeight() - 90, Math.max(10, getWidth() - 48), 26);
        }

        private Rectangle barHit() {
            Rectangle b = bar();
            return new Rectangle(b.x - 6, b.y - 16, b.width + 12, b.height + 26);
        }

        private Rectangle playButton() {
            return new Rectangle(24, getHeight() - 52, 36, 36);
        }

        private int unitsLeft(CpuScheduler.Slice running) {
            Integer burst = bursts.get(running.name);
            if (burst == null) return 0;
            double done = time - running.start;
            for (CpuScheduler.Slice s : result.slices) {
                if (s.name.equals(running.name) && s.end <= running.start) done += s.length();
            }
            return (int) Math.ceil(burst - done - 1e-9);
        }

        private static void drawSpaced(Graphics2D g2, String text, int cx, int baseline, float spacing) {
            FontMetrics fm = g2.getFontMetrics();
            float total = spacing * (text.length() - 1);
            for (char ch : text.toCharArray()) total += fm.charWidth(ch);
            float x = cx - total / 2f;
            for (char ch : text.toCharArray()) {
                g2.drawString(String.valueOf(ch), x, baseline);
                x += fm.charWidth(ch) + spacing;
            }
        }

        private static void centerText(Graphics2D g2, String text, int cx, int cy) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int w = getWidth();
            int h = getHeight();

            RoundRectangle2D card = new RoundRectangle2D.Double(0, 0, w, h, 24, 24);
            g2.setPaint(new GradientPaint(0, 0, new Color(52, 12, 16), 0, h, new Color(15, 15, 17)));
            g2.fill(card);
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.5f, 0), Math.max(w, h) * 0.75f,
                    new float[]{0f, 1f}, new Color[]{new Color(220, 40, 40, 60), new Color(220, 40, 40, 0)}));
            g2.fill(card);

            // what is happening right now?
            CpuScheduler.Slice running = null;
            boolean finished = false;
            if (result != null) {
                if (time >= result.totalTime) {
                    finished = true;
                } else {
                    for (CpuScheduler.Slice s : result.slices) {
                        if (time >= s.start && time < s.end) {
                            running = s;
                            break;
                        }
                    }
                }
            }

            int cx = w / 2;
            String heading;
            Color headingColor;
            if (result == null) {
                heading = "READY";
                headingColor = new Color(170, 170, 178);
            } else if (finished) {
                heading = "FINISHED";
                headingColor = new Color(120, 225, 140);
            } else {
                heading = "NOW RUNNING";
                headingColor = new Color(255, 100, 100);
            }
            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(headingColor);
            drawSpaced(g2, heading, cx, 40, 2.5f);

            // big tile
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fill(new RoundRectangle2D.Double(cx - 74, 50, 148, 148, 44, 44));
            RoundRectangle2D tile = new RoundRectangle2D.Double(cx - 64, 60, 128, 128, 36, 36);
            if (running != null) {
                g2.setColor(running.isIdle() ? new Color(96, 96, 106)
                        : tileColor(order.getOrDefault(running.name, 0)));
                g2.fill(tile);
                g2.setColor(running.isIdle() ? new Color(235, 235, 240) : new Color(15, 15, 15));
                g2.setFont(font(Font.BOLD, running.isIdle() ? 38 : 58));
                centerText(g2, running.name, cx, 124);
            } else if (finished) {
                g2.setColor(new Color(126, 222, 136));
                g2.fill(tile);
                g2.setColor(new Color(15, 15, 15));
                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{cx - 28, cx - 8, cx + 30}, new int[]{126, 148, 100}, 3);
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fill(tile);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.setFont(font(Font.BOLD, 48));
                centerText(g2, "\u2014", cx, 124);
            }

            // line under the tile
            String message;
            if (result == null) {
                message = "Add processes, then press Run";
                g2.setFont(font(Font.PLAIN, 15));
                g2.setColor(new Color(200, 200, 206));
            } else if (finished) {
                message = "All processes finished";
                g2.setFont(font(Font.BOLD, 19));
                g2.setColor(Color.WHITE);
            } else if (running.isIdle()) {
                message = "CPU is idle, waiting for a process";
                g2.setFont(font(Font.PLAIN, 16));
                g2.setColor(new Color(200, 200, 206));
            } else {
                int left = unitsLeft(running);
                message = left + (left == 1 ? " unit left" : " units left");
                g2.setFont(font(Font.BOLD, 19));
                g2.setColor(Color.WHITE);
            }
            g2.drawString(message, cx - g2.getFontMetrics().stringWidth(message) / 2, 226);

            // timeline
            Rectangle b = bar();
            if (result == null) {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fill(new RoundRectangle2D.Double(b.x, b.y, b.width, b.height, 10, 10));
            } else {
                drawTimeline(g2, b);
            }

            // play / pause button, time counter and caption
            Rectangle pb = playButton();
            g2.setColor(result == null ? new Color(255, 255, 255, 70) : Color.WHITE);
            g2.fillOval(pb.x, pb.y, pb.width, pb.height);
            g2.setColor(result == null ? new Color(0, 0, 0, 60) : INK);
            int pcx = pb.x + pb.width / 2;
            int pcy = pb.y + pb.height / 2;
            if (playing) {
                g2.fillRoundRect(pcx - 6, pcy - 7, 4, 14, 2, 2);
                g2.fillRoundRect(pcx + 2, pcy - 7, 4, 14, 2, 2);
            } else {
                g2.fillPolygon(new int[]{pcx - 4, pcx - 4, pcx + 7}, new int[]{pcy - 8, pcy + 8, pcy}, 3);
            }
            int total = result == null ? 0 : result.totalTime;
            int shown = result == null ? 0 : (int) Math.floor(Math.min(time, total));
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            g2.setColor(result == null ? new Color(255, 255, 255, 110) : Color.WHITE);
            g2.drawString(shown + " / " + total, pb.x + pb.width + 14, pcy + 5);
            g2.setFont(font(Font.PLAIN, 13));
            g2.setColor(new Color(255, 255, 255, 220));
            String caption = "Gantt timeline";
            g2.drawString(caption, w - 24 - g2.getFontMetrics().stringWidth(caption), pcy + 5);
            g2.dispose();
        }

        private void drawTimeline(Graphics2D g2, Rectangle b) {
            double scale = b.width / (double) result.totalTime;
            double px = b.x + Math.min(time, result.totalTime) * scale;
            Shape oldClip = g2.getClip();
            g2.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            for (CpuScheduler.Slice s : result.slices) {
                double x1 = b.x + s.start * scale + 1;
                double x2 = b.x + s.end * scale - 1;
                double sw = Math.max(2, x2 - x1);
                RoundRectangle2D seg = new RoundRectangle2D.Double(x1, b.y, sw, b.height, 9, 9);
                Color c = s.isIdle() ? new Color(104, 104, 114) : tileColor(order.getOrDefault(s.name, 0));
                String label = s.isIdle() ? "IDLE" : s.name;
                boolean fits = sw >= fm.stringWidth(label) + 10;
                int tx = (int) Math.round(x1 + sw / 2 - fm.stringWidth(label) / 2.0);
                int ty = b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2;

                // the part that has not played yet is dimmed
                g2.setColor(mix(c, new Color(18, 18, 20), 0.78));
                g2.fill(seg);
                if (fits) {
                    g2.setColor(new Color(255, 255, 255, 120));
                    g2.drawString(label, tx, ty);
                }
                // the part that already played is full color
                g2.clipRect(b.x, b.y - 1, Math.max(0, (int) Math.ceil(px - b.x)), b.height + 2);
                g2.setColor(c);
                g2.fill(seg);
                if (fits) {
                    g2.setColor(new Color(20, 20, 22));
                    g2.drawString(label, tx, ty);
                }
                g2.setClip(oldClip);
            }

            // playhead
            int pxi = (int) Math.round(px);
            g2.setColor(new Color(255, 59, 59));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(pxi, b.y - 6, pxi, b.y + b.height + 12);
            Ellipse2D knob = new Ellipse2D.Double(pxi - 8, b.y - 18, 16, 16);
            g2.fill(knob);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(knob);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    // ---------- results under the stage ----------

    /** Algorithm title, "SchedStream" line and the two average chips. Also shows input errors. */
    private static class Summary extends JPanel {
        private final JLabel title = new JLabel("No simulation yet");
        private final JLabel hint = new JLabel("Add your processes on the right, choose an algorithm, then press Run.");
        private final JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        private final JLabel brandInfo = new JLabel();
        private final Chip waiting = new Chip("Average waiting time");
        private final Chip turnaround = new Chip("Average turnaround time");
        private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        Summary() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setPreferredSize(new Dimension(100, 132));

            title.setFont(font(Font.BOLD, 22));
            title.setForeground(new Color(150, 150, 156));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            hint.setFont(font(Font.PLAIN, 13));
            hint.setForeground(MUTED);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel brandName = new JLabel("SchedStream");
            brandName.setFont(font(Font.BOLD, 14));
            brandName.setForeground(INK);
            brandInfo.setFont(font(Font.PLAIN, 12));
            brandInfo.setForeground(MUTED);
            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            text.add(brandName);
            text.add(brandInfo);
            brandRow.setOpaque(false);
            brandRow.add(new Logo(36, true));
            brandRow.add(Box.createHorizontalStrut(10));
            brandRow.add(text);
            brandRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            brandRow.setVisible(false);

            chips.setOpaque(false);
            chips.add(waiting);
            chips.add(Box.createHorizontalStrut(10));
            chips.add(turnaround);
            chips.setAlignmentX(Component.LEFT_ALIGNMENT);
            chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            chips.setVisible(false);

            add(title);
            add(Box.createVerticalStrut(6));
            add(hint);
            add(Box.createVerticalStrut(10));
            add(brandRow);
            add(Box.createVerticalStrut(12));
            add(chips);
        }

        void showResult(CpuScheduler.Result r) {
            title.setFont(font(Font.BOLD, 22));
            title.setForeground(INK);
            title.setText(r.algorithm.label() + (r.quantum > 0 ? " \u00b7 quantum " + r.quantum : ""));
            hint.setVisible(false);
            brandInfo.setText(r.rows.size() + (r.rows.size() == 1 ? " process" : " processes")
                    + " \u00b7 finished in " + r.totalTime + (r.totalTime == 1 ? " unit" : " units"));
            brandRow.setVisible(true);
            waiting.setValue(String.format("%.2f", r.averageWaiting));
            turnaround.setValue(String.format("%.2f", r.averageTurnaround));
            chips.setVisible(true);
            revalidate();
            repaint();
        }

        void showError(String message) {
            title.setFont(font(Font.BOLD, 17));
            title.setForeground(RED);
            title.setText(message);
            hint.setVisible(false);
            brandRow.setVisible(false);
            chips.setVisible(false);
            revalidate();
            repaint();
        }
    }

    /** Gray pill with a label and a bold red number, like "Average waiting time 7.25". */
    private static class Chip extends JComponent {
        private final String label;
        private String value = "";

        Chip(String label) {
            this.label = label;
        }

        void setValue(String v) {
            value = v;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics a = getFontMetrics(font(Font.PLAIN, 14));
            FontMetrics b = getFontMetrics(font(Font.BOLD, 15));
            return new Dimension(18 + a.stringWidth(label) + 10 + b.stringWidth(value) + 18, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(PILL_GRAY);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setFont(font(Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(INK);
            g2.drawString(label, 18, ty);
            g2.setFont(font(Font.BOLD, 15));
            g2.setColor(new Color(205, 20, 20));
            g2.drawString(value, 18 + fm.stringWidth(label) + 10, ty);
            g2.dispose();
        }
    }

    // ---------- small styled controls ----------

    /** Text box that only accepts digits, with a rounded gray background and a faint hint. */
    private static class RoundField extends JTextField {
        private final String hint;

        RoundField(String hint, int maxDigits) {
            this.hint = hint;
            setOpaque(false);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
            setCaretColor(INK);
            setForeground(INK);
            setDisabledTextColor(new Color(190, 190, 196));
            setPreferredSize(new Dimension(56, 32));
            ((AbstractDocument) getDocument()).setDocumentFilter(new DigitFilter(maxDigits));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? PILL_GRAY : new Color(246, 246, 246));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (isFocusOwner()) {
                g2.setColor(new Color(214, 31, 31, 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            }
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setFont(getFont());
                h.setColor(isEnabled() ? new Color(176, 176, 182) : new Color(210, 210, 214));
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                h.dispose();
            }
        }
    }

    private static class DigitFilter extends DocumentFilter {
        private final int max;

        DigitFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String s = text == null ? "" : text;
            String digits = s.replaceAll("\\D", "");
            int room = max - (fb.getDocument().getLength() - length);
            if (digits.length() > room) digits = digits.substring(0, Math.max(0, room));
            if (digits.isEmpty() && !s.isEmpty()) return;   // nothing usable was typed
            super.replace(fb, offset, length, digits, attrs);
        }
    }

    /** The rounded white bar that holds the algorithm box, quantum and Run. */
    private static class ControlPill extends JPanel {
        ControlPill() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(new Color(214, 214, 218));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
        }
    }

    private static class Divider extends JComponent {
        Divider() {
            setPreferredSize(new Dimension(1, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(224, 224, 228));
            g.fillRect(0, 0, 1, getHeight());
        }
    }

    /** Red "Run" button with a play triangle. Its right side is round to fit the pill. */
    private static class RunButton extends JButton {
        RunButton() {
            super("Run");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean pressed = getModel().isPressed();
            boolean hover = getModel().isRollover();
            g2.setColor(pressed ? new Color(168, 18, 18) : hover ? new Color(232, 48, 48) : RED);
            g2.fill(new RoundRectangle2D.Float(-h, 0, w + h, h, h, h));   // left corners sit outside the button
            g2.setColor(Color.WHITE);
            int cy = h / 2;
            g2.fillPolygon(new int[]{20, 20, 28}, new int[]{cy - 5, cy + 5, cy}, 3);
            g2.setFont(font(Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Run", 38, cy + (fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Wide gray rounded button, like "+ Add process". */
    private static class PillButton extends JButton {
        PillButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(font(Font.BOLD, 14));
            setForeground(INK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? new Color(220, 220, 222)
                    : getModel().isRollover() ? new Color(231, 231, 233) : PILL_GRAY);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** The SchedStream logo: the same icon as the desktop (CpuSchedulingIcon), at any size. */
    private static class Logo extends JComponent {
        private final Icon icon;

        Logo(int size, boolean unused) {
            this.icon = CpuSchedulingIcon.create(size);
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            icon.paintIcon(this, g, 0, 0);
        }
    }

    /** Holder for the process list so it always matches the width of the scroll area. */
    private static class QueueHolder extends JPanel implements Scrollable {
        QueueHolder() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
} 

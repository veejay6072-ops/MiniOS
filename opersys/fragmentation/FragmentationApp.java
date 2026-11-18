package opersys.fragmentation;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import opersys.core.PlaceholderTextField;
import opersys.core.UiKit;

/**
 * Fragmentation simulator. A 72 KB memory is drawn as a tall container: colored segments are
 * assigned space, hatched segments are unused holes. Pieces are dropped in with First, Next, Best or Worst Fit.
 * Views: Memory (the container), Step by Step (last action) and History (every action).
 */
public class FragmentationApp extends JPanel {
    private static final int MAX_MEMORY = 1000;   // largest container the user can create (KB)

    private static final Color BG = new Color(24, 15, 58);
    private static final Color PANEL = new Color(33, 22, 78);
    private static final Color EDGE = new Color(98, 62, 210);
    private static final Color FREE_EDGE = new Color(52, 40, 108);
    private static final Color TEAL = new Color(60, 214, 200);
    private static final Color YELLOW = new Color(255, 214, 80);
    private static final Color GREEN = new Color(126, 222, 128);
    private static final Color PINK = new Color(255, 120, 160);
    private static final Color PURPLE = new Color(200, 130, 250);
    private static final Color CYAN = new Color(80, 210, 225);
    private static final Color TEXT = new Color(238, 234, 255);
    private static final Color MUTED = new Color(172, 162, 218);
    private static final Color DARK_AREA = new Color(18, 11, 46);
    private static final Color[] PALETTE = {
            new Color(86, 190, 255), new Color(255, 176, 70), new Color(126, 222, 128), new Color(200, 130, 250),
            new Color(255, 140, 160), new Color(255, 214, 80), new Color(80, 210, 225), new Color(255, 120, 90),
            new Color(190, 230, 90), new Color(150, 150, 255)
    };
    private static final Font MONO_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 14);

    // kinds of lines in a step
    private static final int L_INFO = 0;
    private static final int L_SKIP = 1;
    private static final int L_TRY = 2;
    private static final int L_PICK = 3;
    private static final int L_FAIL = 4;

    /** Shared by all windows so history survives closing and reopening the app. Newest first. */
    private static final List<Step> HISTORY = new ArrayList<>();
    private static int runCounter = 0;

    private int total = 0;                      // size of the container in KB (0 until the user creates it)
    private boolean created = false;
    private int[] mem = new int[0];             // -1 = free, otherwise the id of the piece in that block
    private int nextId;
    private int rover;                           // where Next Fit starts looking (block after the last placed piece)

    private final JTextField pieceField = new PlaceholderTextField("e.g. 10", 8);
    private final JTextField sizeField = new PlaceholderTextField("e.g. 72", 8);
    private final JLabel memoryHeading = new JLabel();
    private final JComboBox<String> algoBox =
            new JComboBox<>(new String[]{"First Fit", "Next Fit", "Best Fit", "Worst Fit"});

    private final MemoryBar bar = new MemoryBar();
    private final PieceStrip strip = new PieceStrip();
    private final JLabel nextLabel = new JLabel();
    private final JLabel unusedLabel = new JLabel();
    private final StatCard freeCard = new StatCard("Unused", "KB in total", CYAN);
    private final StatCard biggestCard = new StatCard("Biggest hole", "KB in a row", YELLOW);
    private final StatCard holesCard = new StatCard("Holes", "unused pieces", PURPLE);
    private final StatCard fragCard = new StatCard("Fragmented", "of unused space", PINK);

    private final RoundPanel messagePanel = new RoundPanel(new BorderLayout(0, 4), PANEL, EDGE, 20);
    private final JLabel messageTitle = new JLabel();
    private final JLabel messageBody = new JLabel();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final PillButton memoryTab = new PillButton("Memory", PANEL, TEXT, EDGE);
    private final PillButton stepsTab = new PillButton("Step by Step", PANEL, TEXT, EDGE);
    private final PillButton historyTab = new PillButton("History", PANEL, TEXT, EDGE);

    private final JScrollPane stepsScroll =
            scroll(placeholder("Press DROP to see, step by step, how a piece is placed."));
    private final DefaultListModel<Step> historyModel = new DefaultListModel<>();
    private final JList<Step> historyList = new JList<>(historyModel);
    private final JScrollPane historyScroll = scroll(placeholder("No history yet."));

    public FragmentationApp() {
        super(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(860, 610));
        resetMemory();

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(BG);
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(buildTabs(), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        cards.setBackground(BG);
        cards.add(buildMemoryView(), "memory");
        cards.add(buildStepsView(), "steps");
        cards.add(buildHistoryView(), "history");
        add(cards, BorderLayout.CENTER);

        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        };
        pieceField.getDocument().addDocumentListener(dl);

        setMessage("START", "There is no memory yet. Type a memory size (KB) on the left and press CREATE, then drop pieces into it.", 0);
        refreshHistory(0);
        showView("memory");
        refresh();
    }

    // ---------- building the screen ----------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(BG);
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));

        JLabel logo = new JLabel("Fragmentation");
        logo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        logo.setForeground(Color.WHITE);
        Icon img = FragmentationIcon.create(36);
        logo.setIcon(img != null ? img : new LogoIcon());
        logo.setIconTextGap(10);
        header.add(logo, BorderLayout.WEST);

        styleField(pieceField);
        pieceField.addActionListener(e -> drop());

        // Draw the dropdown ourselves so the chosen fit (First / Next / Best / Worst) is always readable
        // (the Windows look and feel paints a white box, which hid the white text).
        algoBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    public void paint(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(20, 12, 50));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        g2.setColor(Color.WHITE);
                        g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setFocusable(false);
                b.setContentAreaFilled(false);
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean hasFocus) {
                g.setColor(new Color(20, 12, 50));
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle r, boolean hasFocus) {
                ListCellRenderer<Object> renderer = comboBox.getRenderer();
                Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
                c.setFont(comboBox.getFont());
                c.setForeground(Color.WHITE);
                c.setBackground(new Color(20, 12, 50));
                currentValuePane.paintComponent(g, c, comboBox, r.x, r.y, r.width, r.height, c instanceof JComponent);
            }
        });
        algoBox.setBorder(BorderFactory.createLineBorder(EDGE, 1, true));
        algoBox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        algoBox.setBackground(new Color(20, 12, 50));
        algoBox.setForeground(Color.WHITE);
        algoBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                l.setOpaque(true);
                l.setBackground(selected ? EDGE : new Color(20, 12, 50));
                l.setForeground(Color.WHITE);
                l.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return l;
            }
        });
        algoBox.addActionListener(e ->
                setMessage("READY", "Algorithm set to " + algoBox.getSelectedItem() + ". Press DROP to place the next piece.", 0));

        PillButton dropButton = new PillButton("DROP", YELLOW, new Color(40, 28, 90), null);
        dropButton.addActionListener(e -> drop());

        RoundPanel capsule = new RoundPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), PANEL, EDGE, 26);
        capsule.add(smallLabel("Piece size"));
        capsule.add(pieceField);
        capsule.add(smallLabel("KB"));
        capsule.add(algoBox);
        capsule.add(dropButton);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(capsule);
        header.add(wrap, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildTabs() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(BG);
        row.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        memoryTab.addActionListener(e -> showView("memory"));
        stepsTab.addActionListener(e -> showView("steps"));
        historyTab.addActionListener(e -> showView("history"));
        row.add(memoryTab);
        row.add(stepsTab);
        row.add(historyTab);
        return row;
    }

    private JComponent buildMemoryView() {
        JPanel view = new JPanel(new BorderLayout(14, 0));
        view.setBackground(BG);
        view.setBorder(BorderFactory.createEmptyBorder(4, 14, 12, 14));

        // left: the memory container
        JLabel heading = memoryHeading;
        heading.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        unusedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        unusedLabel.setForeground(TEXT);
        unusedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.add(heading);
        head.add(Box.createVerticalStrut(3));
        head.add(unusedLabel);

        styleField(sizeField);
        sizeField.addActionListener(e -> createMemory());
        PillButton create = new PillButton("CREATE", YELLOW, new Color(40, 28, 90), null);
        create.addActionListener(e -> createMemory());
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sizeRow.setOpaque(false);
        sizeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeRow.add(smallLabel("Memory size"));
        sizeRow.add(sizeField);
        sizeRow.add(smallLabel("KB"));
        sizeRow.add(create);
        head.add(Box.createVerticalStrut(8));
        head.add(sizeRow);

        RoundPanel left = new RoundPanel(new BorderLayout(0, 6), PANEL, EDGE, 22);
        left.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        left.setPreferredSize(new Dimension(370, 10));
        left.add(head, BorderLayout.NORTH);
        left.add(bar, BorderLayout.CENTER);

        // right: next piece, stats, message, buttons
        JLabel nextTitle = new JLabel("NEXT");
        nextTitle.setFont(MONO_BOLD);
        nextTitle.setForeground(YELLOW);
        nextLabel.setForeground(TEXT);
        nextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        RoundPanel nextCard = new RoundPanel(new BorderLayout(0, 8), PANEL, EDGE, 20);
        nextCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        nextCard.add(nextTitle, BorderLayout.NORTH);
        nextCard.add(strip, BorderLayout.CENTER);
        nextCard.add(nextLabel, BorderLayout.SOUTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 8, 0));
        stats.setOpaque(false);
        stats.add(freeCard);
        stats.add(biggestCard);
        stats.add(holesCard);
        stats.add(fragCard);
        stats.setPreferredSize(new Dimension(10, 92));

        messageTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        messageBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageBody.setForeground(TEXT);
        messageBody.setVerticalAlignment(SwingConstants.TOP);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        messagePanel.add(messageTitle, BorderLayout.NORTH);
        messagePanel.add(messageBody, BorderLayout.CENTER);

        PillButton compact = new PillButton("COMPACT", new Color(126, 222, 128), new Color(30, 40, 60), null);
        compact.addActionListener(e -> compact());
        PillButton reset = new PillButton("Reset memory", BG, TEXT, EDGE);
        reset.addActionListener(e -> reset());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(compact);
        buttons.add(reset);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 8, 0);
        c.gridy = 0;
        c.weighty = 0;
        right.add(nextCard, c);
        c.gridy = 1;
        right.add(stats, c);
        c.gridy = 2;
        c.weighty = 1;
        right.add(messagePanel, c);
        c.gridy = 3;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        right.add(buttons, c);

        view.add(left, BorderLayout.WEST);
        view.add(right, BorderLayout.CENTER);
        return view;
    }

    private JComponent buildStepsView() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(4, 14, 12, 14));
        p.add(stepsScroll, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildHistoryView() {
        historyList.setBackground(PANEL);
        historyList.setCellRenderer(new StepRenderer());
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedHistory();
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll(historyList), historyScroll);
        split.setResizeWeight(0.34);
        split.setDividerLocation(280);
        split.setBorder(null);
        split.setBackground(BG);

        PillButton clear = new PillButton("Clear history", BG, TEXT, EDGE);
        clear.addActionListener(e -> {
            HISTORY.clear();
            refreshHistory(-1);
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(clear);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(4, 14, 12, 14));
        p.add(split, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void showView(String name) {
        cardLayout.show(cards, name);
        styleTab(memoryTab, name.equals("memory"));
        styleTab(stepsTab, name.equals("steps"));
        styleTab(historyTab, name.equals("history"));
    }

    private static void styleTab(PillButton b, boolean active) {
        if (active) b.style(YELLOW, new Color(40, 28, 90), null);
        else b.style(PANEL, TEXT, EDGE);
    }

    private static void styleField(JTextField f) {
        f.setFont(MONO_BOLD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBackground(new Color(20, 12, 50));
        f.setHorizontalAlignment(SwingConstants.CENTER);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE, 1, true), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
    }

    private static JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    private static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(EDGE));
        s.getViewport().setBackground(DARK_AREA);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    private static JComponent placeholder(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        p.setBackground(DARK_AREA);
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(l);
        return p;
    }

    // ---------- memory model ----------

    private void resetMemory() {
        Arrays.fill(mem, -1);   // memory starts completely empty
        nextId = 0;
        rover = 0;
    }

    /** Free holes as {startBlock, length}. */
    private List<int[]> holes() {
        List<int[]> list = new ArrayList<>();
        int i = 0;
        while (i < total) {
            if (mem[i] == -1) {
                int s = i;
                while (i < total && mem[i] == -1) i++;
                list.add(new int[]{s, i - s});
            } else {
                i++;
            }
        }
        return list;
    }

    /** Runs of equal blocks as {startBlock, length, id}; id is -1 for unused space. */
    private List<int[]> runs() {
        List<int[]> list = new ArrayList<>();
        int i = 0;
        while (i < total) {
            int s = i;
            int id = mem[i];
            while (i < total && mem[i] == id) i++;
            list.add(new int[]{s, i - s, id});
        }
        return list;
    }

    private static char letter(int id) {
        return (char) ('A' + id % 26);
    }

    private static Color colorOf(int id) {
        return PALETTE[id % PALETTE.length];
    }

    private String statsLine() {
        List<int[]> hs = holes();
        int free = 0;
        int biggest = 0;
        for (int[] h : hs) {
            free += h[1];
            biggest = Math.max(biggest, h[1]);
        }
        int frag = free == 0 ? 0 : Math.round((1f - biggest / (float) free) * 100);
        return "Unused " + free + " KB, biggest hole " + biggest + " KB, " + hs.size() + " holes, " + frag + "% fragmented";
    }

    // ---------- actions ----------

    private Step newStep(String badge, String title) {
        return new Step(new SimpleDateFormat("HH:mm:ss").format(new Date()), badge, title, mem.clone());
    }

    private void finish(Step step, String summary) {
        step.after = mem.clone();
        step.stats = statsLine();
        step.summary = summary;
        runCounter++;
        step.number = runCounter;
        HISTORY.add(0, step);
        refreshHistory(0);
        stepsScroll.setViewportView(renderStep(step));
        refresh();
    }

    private void createMemory() {
        int size;
        try {
            size = Integer.parseInt(sizeField.getText().trim());
        } catch (NumberFormatException ex) {
            setMessage("MEMORY SIZE", "Type the memory size as a whole number of KB, for example 72.", 1);
            return;
        }
        if (size < 1 || size > MAX_MEMORY) {
            setMessage("MEMORY SIZE", "The memory size must be between 1 and " + MAX_MEMORY + " KB.", 1);
            return;
        }
        Step step = newStep("CREATE", "Memory of " + size + " KB");
        total = size;
        mem = new int[size];
        resetMemory();
        created = true;
        step.kind = 2;
        step.add(L_INFO, "A memory of " + size + " KB was created and it is completely unused");
        step.add(L_INFO, "Type a piece size and press DROP to place the first piece (A)");
        setMessage("MEMORY CREATED!", "Your memory is " + size + " KB and completely empty."
                + "<br>Type a piece size, pick an algorithm and press DROP.", 2);
        finish(step, "Created memory: " + size + " KB");
    }

    private boolean needMemory() {
        if (created) return false;
        setMessage("CREATE FIRST", "There is no memory yet.<br>Type a memory size on the left and press CREATE.", 1);
        return true;
    }

    private void drop() {
        if (needMemory()) return;
        int size;
        try {
            size = Integer.parseInt(pieceField.getText().trim());
        } catch (NumberFormatException ex) {
            UiKit.error(this, "Piece size must be a whole number.");
            return;
        }
        if (size < 1 || size > total) {
            UiKit.error(this, "Piece size must be between 1 and " + total + " KB.");
            return;
        }

        String algo = (String) algoBox.getSelectedItem();
        int id = nextId;
        char name = letter(id);
        List<int[]> hs = holes();
        int free = 0;
        int biggest = 0;
        for (int[] h : hs) {
            free += h[1];
            biggest = Math.max(biggest, h[1]);
        }

        Step step = newStep("DROP", "Piece " + name + " asks for " + size + " KB (" + algo + ")");
        step.add(L_INFO, "Unused space: " + free + " KB in " + hs.size() + " hole(s), biggest hole " + biggest + " KB");

        int idx = choose(hs, size, algo, step);
        String outcome;
        if (idx < 0) {
            outcome = "CAN'T FIT";
            step.kind = 1;
            if (free >= size) {
                step.add(L_FAIL, "No hole is big enough, but " + free + " KB is unused in total");
                step.add(L_INFO, "External fragmentation: the unused space is cut into small holes. COMPACT would fix it.");
                setMessage("CAN'T FIT!", String.format("Piece %c needs %d KB. There are %d KB unused, but the biggest hole is only %d KB."
                        + "<br>The unused space is cut into small pieces. This is external fragmentation.",
                        name, size, free, biggest), 1);
            } else {
                step.add(L_FAIL, "Only " + free + " KB is unused in total, so memory is simply full");
                step.add(L_INFO, "COMPACT will not help, there is not enough space at all.");
                setMessage("CAN'T FIT!", String.format("Piece %c needs %d KB but only %d KB is unused in total."
                        + "<br>Memory is full, so compacting will not help.", name, size, free), 1);
            }
        } else {
            int[] hole = hs.get(idx);
            for (int k = 0; k < size; k++) mem[hole[0] + k] = id;
            nextId++;
            rover = hole[0] + size;
            int leftover = hole[1] - size;
            outcome = "placed at blocks " + (hole[0] + 1) + "-" + (hole[0] + size);
            step.kind = 2;
            step.add(L_INFO, "Piece " + name + " takes blocks " + (hole[0] + 1) + "-" + (hole[0] + size));
            step.add(L_INFO, leftover > 0 ? "Leftover " + leftover + " KB stays as an unused hole"
                    : "The hole is used up exactly, nothing is left over");
            setMessage("PLACED!", String.format("Piece %c (%d KB) placed in blocks %d-%d using %s."
                    + "<br>The hole it used was %d KB, leftover %d KB.",
                    name, size, hole[0] + 1, hole[0] + size, algo, hole[1], leftover), 2);
        }
        finish(step, "Piece " + name + ", " + size + " KB, " + algo + ": " + outcome);
    }

    private void compact() {
        if (needMemory()) return;
        Step step = newStep("COMPACT", "Slide every piece to the left");
        List<int[]> before = holes();
        step.add(L_INFO, "Before: " + before.size() + " unused hole(s)");
        String summary;
        if (before.size() <= 1 && (before.isEmpty() || before.get(0)[0] + before.get(0)[1] == total)) {
            step.kind = 0;
            step.add(L_INFO, "Nothing to do, all pieces are already packed to the left");
            setMessage("COMPACT", "Nothing to compact. All pieces are already packed to the left.", 0);
            summary = "Compact: nothing to do";
        } else {
            int w = 0;
            for (int i = 0; i < total; i++) {
                if (mem[i] != -1) {
                    int v = mem[i];
                    mem[i] = -1;
                    mem[w++] = v;
                }
            }
            rover = w;
            step.kind = 2;
            step.add(L_INFO, "Every piece slid to the left and kept its order");
            step.add(L_PICK, "Unused space is now one hole: blocks " + (w + 1) + "-" + total + " (" + (total - w) + " KB)");
            setMessage("COMPACTED!", "All pieces slid to the left. The unused space is now one big hole."
                    + "<br>In a real OS this costs time, because every moved piece has to be copied.", 2);
            summary = "Compact: unused space is one hole of " + (total - w) + " KB";
        }
        finish(step, summary);
    }

    private void reset() {
        if (needMemory()) return;
        Step step = newStep("RESET", "Memory cleared");
        resetMemory();
        step.kind = 0;
        step.add(L_INFO, "Every piece was removed, all " + total + " KB are unused again");
        step.add(L_INFO, "The next piece is A again");
        setMessage("RESET", "Memory is empty again. Type a piece size and press DROP.", 0);
        finish(step, "Memory reset");
    }

    /** Picks a hole for the piece and writes every check to the step. Returns the hole index or -1. */
    private int choose(List<int[]> holes, int size, String algo, Step step) {
        if (algo.equals("Next Fit")) {
            step.add(L_INFO, "Next Fit starts at block " + (rover + 1) + " (where the last piece ended) and wraps around");
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < holes.size(); i++) if (holes.get(i)[0] >= rover) order.add(i);
            int wrapAt = order.size();
            for (int i = 0; i < holes.size(); i++) if (holes.get(i)[0] < rover) order.add(i);
            for (int pos = 0; pos < order.size(); pos++) {
                if (pos == wrapAt && wrapAt > 0) step.add(L_INFO, "Reached the end of memory, going back to the start");
                int i = order.get(pos);
                String label = holeLabel(holes, i);
                if (holes.get(i)[1] < size) {
                    step.add(L_SKIP, label + " - too small");
                    continue;
                }
                step.add(L_PICK, label + " - fits, Next Fit takes it and stops");
                return i;
            }
            return -1;
        }

        int chosen = -1;
        for (int i = 0; i < holes.size(); i++) {
            int len = holes.get(i)[1];
            String label = holeLabel(holes, i);
            if (len < size) {
                step.add(L_SKIP, label + " - too small");
                continue;
            }
            if (chosen == -1) {
                chosen = i;
                if (algo.equals("First Fit")) {
                    step.add(L_PICK, label + " - fits, First Fit takes the first one and stops");
                    return i;
                }
                step.add(L_TRY, label + " - fits, first candidate");
                continue;
            }
            int current = holes.get(chosen)[1];
            boolean best = algo.equals("Best Fit");
            boolean better = best ? len < current : len > current;
            if (better) {
                chosen = i;
                step.add(L_TRY, label + " - fits and is " + (best ? "smaller" : "bigger") + " than " + current + " KB, new choice");
            } else {
                step.add(L_SKIP, label + " - fits but is not " + (best ? "smaller" : "bigger") + " than " + current + " KB, skip");
            }
        }
        if (chosen >= 0) {
            step.add(L_PICK, "Winner: hole " + (chosen + 1) + " (" + holes.get(chosen)[1] + " KB)");
        }
        return chosen;
    }

    private static String holeLabel(List<int[]> holes, int i) {
        int start = holes.get(i)[0];
        int len = holes.get(i)[1];
        return "Hole " + (i + 1) + ": blocks " + (start + 1) + "-" + (start + len) + " (" + len + " KB)";
    }

    // ---------- history ----------

    private void refreshHistory(int selectIndex) {
        historyModel.clear();
        for (Step s : HISTORY) historyModel.addElement(s);
        if (selectIndex >= 0 && selectIndex < historyModel.size()) historyList.setSelectedIndex(selectIndex);
        else historyList.clearSelection();
        showSelectedHistory();
    }

    private void showSelectedHistory() {
        int idx = historyList.getSelectedIndex();
        if (idx < 0 || idx >= HISTORY.size()) {
            historyScroll.setViewportView(placeholder(HISTORY.isEmpty()
                    ? "No history yet. Press DROP to record the first step."
                    : "Select an entry on the left to see its steps."));
        } else {
            historyScroll.setViewportView(renderStep(HISTORY.get(idx)));
        }
    }

    // ---------- refreshing the screen ----------

    private int currentSize() {
        try {
            int v = Integer.parseInt(pieceField.getText().trim());
            if (v >= 1 && (!created || v <= total)) return v;
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private void refresh() {
        int size = currentSize();
        if (!created) {
            freeCard.setValue("-");
            biggestCard.setValue("-");
            holesCard.setValue("-");
            fragCard.setValue("-");
            memoryHeading.setText("<html><b>Memory</b> <font color='#ACA2DA'>not created yet</font></html>");
            unusedLabel.setText("<html><font color='#ACA2DA'>Type a memory size below and press CREATE</font></html>");
            strip.setPiece(0, nextId);
            nextLabel.setText("<html><b>Waiting for memory</b> - create it first</html>");
            bar.repaint();
            return;
        }

        List<int[]> hs = holes();
        int free = 0;
        int biggest = 0;
        for (int[] h : hs) {
            free += h[1];
            biggest = Math.max(biggest, h[1]);
        }
        int frag = free == 0 ? 0 : Math.round((1f - biggest / (float) free) * 100);
        freeCard.setValue(String.valueOf(free));
        biggestCard.setValue(String.valueOf(biggest));
        holesCard.setValue(String.valueOf(hs.size()));
        fragCard.setValue(frag + "%");
        memoryHeading.setText("<html><b>Memory</b> <font color='#ACA2DA'>" + total
                + " KB - taller segment = more KB</font></html>");

        if (free == 0) {
            unusedLabel.setText("<html><font color='#FF78A0'><b>No unused space</b></font> - memory is full</html>");
        } else {
            unusedLabel.setText("<html>Unused space: <font color='#3CD6C8'><b>" + free + " KB</b></font> in "
                    + hs.size() + (hs.size() == 1 ? " hole" : " holes") + "</html>");
        }

        strip.setPiece(size, nextId);
        nextLabel.setText(size == 0
                ? "<html><b>Process " + letter(nextId) + "</b> - type a piece size</html>"
                : "<html><b>Process " + letter(nextId) + "</b> - " + size + " KB in a row</html>");
        bar.repaint();
    }

    /** kind: 0 = info, 1 = can't fit, 2 = success. */
    private void setMessage(String title, String bodyHtml, int kind) {
        Color fill = PANEL;
        Color edge = EDGE;
        Color titleColor = YELLOW;
        if (kind == 1) {
            fill = new Color(46, 22, 64);
            edge = new Color(255, 140, 170);
            titleColor = new Color(255, 110, 150);
        } else if (kind == 2) {
            fill = new Color(22, 48, 60);
            edge = new Color(110, 220, 130);
            titleColor = GREEN;
        }
        messagePanel.colors(fill, edge);
        messageTitle.setForeground(titleColor);
        messageTitle.setText(title);
        messageBody.setText("<html><body style='width:370px'>" + bodyHtml + "</body></html>");
    }

    // ---------- step cards (used by Step by Step and History) ----------

    /** One recorded action: what was asked, every check made, and memory before and after. */
    private static class Step {
        final String time;
        final String badge;
        final String title;
        final int[] before;
        final List<Object[]> lines = new ArrayList<>();   // {Integer kind, String text}
        int[] after;
        String stats = "";
        String summary = "";
        int kind;      // 0 info, 1 can't fit, 2 success
        int number;

        Step(String time, String badge, String title, int[] before) {
            this.time = time;
            this.badge = badge;
            this.title = title;
            this.before = before;
        }

        void add(int lineKind, String text) {
            lines.add(new Object[]{lineKind, text});
        }
    }

    private static Color accent(int kind) {
        if (kind == 1) return new Color(255, 110, 150);
        if (kind == 2) return GREEN;
        return new Color(160, 120, 255);
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(MUTED);
        return left(l);
    }

    private static JComponent renderStep(Step s) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(DARK_AREA);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Color accent = accent(s.kind);

        RoundPanel head = left(new RoundPanel(new BorderLayout(12, 0), PANEL, accent, 18));
        head.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel badge = new JLabel(s.badge);
        badge.setFont(MONO_BOLD);
        badge.setForeground(accent);
        JLabel title = new JLabel(s.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(Color.WHITE);
        JLabel when = new JLabel("#" + s.number + "  " + s.time);
        when.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        when.setForeground(MUTED);
        head.add(badge, BorderLayout.WEST);
        head.add(title, BorderLayout.CENTER);
        head.add(when, BorderLayout.EAST);
        p.add(head);

        if (s.before != null && s.before.length > 0) {
            p.add(Box.createVerticalStrut(10));
            p.add(section("Memory before"));
            p.add(Box.createVerticalStrut(4));
            p.add(new MiniBar(s.before));
        }

        p.add(Box.createVerticalStrut(10));
        p.add(section("What happened"));
        p.add(Box.createVerticalStrut(4));
        for (Object[] line : s.lines) {
            int k = (Integer) line[0];
            Color edge = k == L_PICK ? GREEN : k == L_TRY ? YELLOW : k == L_FAIL ? PINK
                    : k == L_SKIP ? new Color(86, 62, 130) : FREE_EDGE;
            RoundPanel row = left(new RoundPanel(new BorderLayout(10, 0), new Color(38, 26, 88), edge, 14));
            row.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            JLabel t = new JLabel((String) line[1]);
            t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            t.setForeground(k == L_SKIP ? MUTED : TEXT);
            row.add(new Dot(k), BorderLayout.WEST);
            row.add(t, BorderLayout.CENTER);
            p.add(row);
            p.add(Box.createVerticalStrut(4));
        }

        p.add(Box.createVerticalStrut(6));
        p.add(section("Memory after"));
        p.add(Box.createVerticalStrut(4));
        p.add(new MiniBar(s.after));
        p.add(Box.createVerticalStrut(6));
        JLabel stats = new JLabel(s.stats);
        stats.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stats.setForeground(CYAN);
        p.add(left(stats));
        p.add(Box.createVerticalGlue());
        return p;
    }

    /** History list entry: colored bar on the left, badge and time on top, summary below. */
    private static class StepRenderer extends JLabel implements ListCellRenderer<Step> {
        StepRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Step> list, Step s, int index,
                                                      boolean selected, boolean focus) {
            Color a = accent(s.kind);
            String hex = String.format("#%02x%02x%02x", a.getRed(), a.getGreen(), a.getBlue());
            setText("<html><b><font color='" + hex + "'>" + s.badge + "</font></b> &nbsp;#" + s.number + " &nbsp;"
                    + s.time + "<br>" + s.summary.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
            setForeground(TEXT);
            setBackground(selected ? new Color(62, 42, 142) : PANEL);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 5, 0, 0, a),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, BG),
                            BorderFactory.createEmptyBorder(6, 10, 6, 8))));
            return this;
        }
    }

    /** Round status icon in front of a step line. */
    private static class Dot extends JComponent {
        private final int kind;

        Dot(int kind) {
            this.kind = kind;
            setPreferredSize(new Dimension(20, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            Color c = kind == L_PICK ? GREEN : kind == L_TRY ? YELLOW : kind == L_FAIL ? PINK
                    : kind == L_SKIP ? new Color(150, 110, 170) : new Color(160, 120, 255);
            int cy = getHeight() / 2;
            g2.setColor(c);
            g2.fillOval(1, cy - 9, 18, 18);
            g2.setColor(new Color(30, 20, 70));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (kind == L_PICK) {
                g2.drawPolyline(new int[]{5, 9, 15}, new int[]{cy, cy + 4, cy - 4}, 3);
            } else if (kind == L_SKIP || kind == L_FAIL) {
                g2.drawLine(6, cy - 4, 14, cy + 4);
                g2.drawLine(14, cy - 4, 6, cy + 4);
            } else {
                g2.fillOval(8, cy - 2, 4, 4);
            }
            g2.dispose();
        }
    }

    /** Thin horizontal picture of all 72 blocks, used inside step cards. */
    private static class MiniBar extends JComponent {
        private final int[] cells;

        MiniBar(int[] cells) {
            this.cells = cells;
            setPreferredSize(new Dimension(10, 24));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (cells == null || cells.length == 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D frame = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 12, 12);
            g2.clip(frame);
            double cw = w / (double) cells.length;
            for (int i = 0; i < cells.length; i++) {
                int x = (int) Math.round(i * cw);
                int x2 = (int) Math.round((i + 1) * cw);
                g2.setColor(cells[i] == -1 ? new Color(44, 33, 98) : colorOf(cells[i]));
                g2.fillRect(x, 0, x2 - x, h);
                if (i > 0 && cells[i] != cells[i - 1]) {
                    g2.setColor(new Color(14, 8, 36));
                    g2.fillRect(x, 0, 1, h);
                }
            }
            g2.setClip(null);
            g2.setColor(EDGE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(frame);
            g2.dispose();
        }
    }

    // ---------- custom components ----------

    private static Color shade(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
    }

    private static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void arrow(Graphics2D g2, int x, int y0, int y1, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine(x, y0, x, y1);
        if (y1 - y0 >= 12) {
            g2.fillPolygon(new int[]{x - 3, x + 3, x}, new int[]{y0 + 5, y0 + 5, y0}, 3);
            g2.fillPolygon(new int[]{x - 3, x + 3, x}, new int[]{y1 - 5, y1 - 5, y1}, 3);
        }
    }

    /**
     * The memory container, laid out like the diagram (rounded box with a thick shadow, dividers between segments,
     * "Assigned Space" arrows on the left, "Fragments" + KB arrows on the right) but drawn with the colored blocks.
     * Colored segments are assigned space, dark segments labeled "Fragments" are unused. Height follows size in KB.
     */
    private class MemoryBar extends JPanel {
        MemoryBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(340, 380));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            if (!created) {   // nothing created yet: dashed empty box
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                int bw = Math.max(136, Math.min(w - 12,
                        g2.getFontMetrics().stringWidth("Create it with the size above") + 40));
                int bx = (w - bw) / 2;
                int bh = h - 36;
                RoundRectangle2D box = new RoundRectangle2D.Double(bx, 4, bw, bh, 44, 44);
                g2.setColor(new Color(30, 20, 72));
                g2.fill(box);
                g2.setColor(MUTED);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));
                g2.draw(box);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm0 = g2.getFontMetrics();
                String t1 = "No memory yet";
                g2.drawString(t1, bx + (bw - fm0.stringWidth(t1)) / 2, 4 + bh / 2 - 2);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                fm0 = g2.getFontMetrics();
                String t2 = "Create it with the size above";
                g2.drawString(t2, bx + (bw - fm0.stringWidth(t2)) / 2 + 0, 4 + bh / 2 + 14);
                g2.dispose();
                return;
            }
            int keyH = 22;
            int barW = 136;
            int barX = (w - barW) / 2 + 4;
            int top = 4;
            int barH = h - keyH - top - 10;
            Color line = new Color(228, 222, 255);
            Color divider = new Color(12, 6, 34);

            List<int[]> runs = runs();
            int n = runs.size();
            double minH = 18;
            double scale = barH / (double) total;
            double fixed = 0;
            int fixedBlocks = 0;
            for (int[] r : runs) {
                if (r[1] * scale < minH) {
                    fixed += minH;
                    fixedBlocks += r[1];
                }
            }
            double scale2 = Math.max(0.5, (barH - fixed) / Math.max(1, total - fixedBlocks));

            RoundRectangle2D frame = new RoundRectangle2D.Double(barX, top, barW, barH, 44, 44);
            g2.setColor(new Color(8, 4, 24));   // thick shadow on the left and bottom
            g2.fill(new RoundRectangle2D.Double(barX - 7, top + 6, barW, barH, 44, 44));
            g2.setColor(new Color(30, 20, 72));
            g2.fill(frame);

            int bestY = -1;
            int bestH = 0;
            int bestLen = 0;
            double y = top;
            for (int i = 0; i < n; i++) {
                int[] r = runs.get(i);
                double ht = r[1] * scale < minH ? minH : r[1] * scale2;
                int y0 = (int) Math.round(y);
                int y1 = (int) Math.round(y + ht);
                int hh = y1 - y0;
                y += ht;

                g2.setClip(frame);
                g2.clipRect(barX, y0, barW, hh);
                if (r[2] == -1) {
                    g2.setColor(new Color(34, 24, 80));
                    g2.fillRect(barX, y0, barW, hh);
                    g2.setColor(new Color(60, 46, 126));
                    g2.setStroke(new BasicStroke(1.5f));
                    for (int k = -hh; k < barW; k += 9) g2.drawLine(barX + k, y1, barX + k + hh, y0);
                    if (r[1] > bestLen) {
                        bestLen = r[1];
                        bestY = y0;
                        bestH = hh;
                    }
                } else {
                    Color c = colorOf(r[2]);
                    g2.setColor(c);
                    g2.fillRect(barX, y0, barW, hh);
                    g2.setColor(shade(c, 0.68));    // block edge at the bottom
                    g2.fillRect(barX, y1 - 4, barW, 4);
                    g2.setColor(new Color(255, 255, 255, 70));   // light edge at the top
                    g2.fillRect(barX, y0, barW, 2);
                }
                if (i > 0) {
                    g2.setColor(divider);
                    g2.fillRect(barX, y0 - 1, barW, 2);
                }
                if (r[2] == -1) {
                    if (hh >= 16) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, hh >= 30 ? 16 : 12));
                        g2.setColor(new Color(214, 206, 250));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString("Fragments", barX + (barW - fm.stringWidth("Fragments")) / 2,
                                y0 + (hh + fm.getAscent()) / 2 - 3);
                    }
                } else if (hh >= 18) {
                    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, hh >= 30 ? 20 : 14));
                    g2.setColor(new Color(36, 26, 80));
                    FontMetrics fm = g2.getFontMetrics();
                    String t = String.valueOf(letter(r[2]));
                    g2.drawString(t, barX + (barW - fm.stringWidth(t)) / 2, y0 + (hh + fm.getAscent()) / 2 - 4);
                }
                g2.setClip(null);

                if (r[2] == -1) {   // right side: KB of the unused hole
                    int ax = barX + barW + 18;
                    arrow(g2, ax, y0 + 3, y1 - 3, YELLOW);
                    if (hh >= 34) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                        g2.setColor(YELLOW);
                        g2.drawString(r[1] + " KB", ax + 9, y0 + hh / 2 + 4);
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        g2.setColor(MUTED);
                        g2.drawString("unused", ax + 9, y0 + hh / 2 + 18);
                    } else if (hh >= 14) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        g2.setColor(YELLOW);
                        g2.drawString(r[1] + " KB", ax + 9, y0 + hh / 2 + 4);
                    }
                } else {   // left side: assigned space
                    int ax = barX - 20;
                    arrow(g2, ax, y0 + 3, y1 - 3, new Color(205, 200, 245));
                    FontMetrics fm;
                    if (hh >= 34) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        fm = g2.getFontMetrics();
                        String l1 = "Assigned Space";
                        g2.setColor(TEXT);
                        g2.drawString(l1, ax - 8 - fm.stringWidth(l1), y0 + hh / 2);
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        fm = g2.getFontMetrics();
                        String l2 = "Piece " + letter(r[2]) + " - " + r[1] + " KB";
                        g2.setColor(MUTED);
                        g2.drawString(l2, ax - 8 - fm.stringWidth(l2), y0 + hh / 2 + 14);
                    } else if (hh >= 14) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        fm = g2.getFontMetrics();
                        String l1 = letter(r[2]) + " - " + r[1] + " KB";
                        g2.setColor(TEXT);
                        g2.drawString(l1, ax - 8 - fm.stringWidth(l1), y0 + hh / 2 + 4);
                    }
                }
            }

            if (bestY >= 0 && bestLen > 0) {   // outline the biggest hole
                g2.setClip(frame);
                g2.setColor(TEAL);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(barX + 3, bestY + 3, barW - 7, bestH - 7);
                g2.setClip(null);
            }
            g2.setColor(line);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(frame);

            // small key under the container
            int ky = h - 15;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(TEAL);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(8, ky, 11, 11, 4, 4);
            g2.setColor(TEXT);
            g2.drawString("Teal outline = biggest hole", 25, ky + 10);
            g2.dispose();
        }
    }

    /** The row of blocks showing the size of the next piece. */
    private static class PieceStrip extends JComponent {
        private int count = 10;
        private int id = 0;

        PieceStrip() {
            setPreferredSize(new Dimension(10, 26));
        }

        void setPiece(int count, int id) {
            this.count = count;
            this.id = id;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            if (count <= 0) {
                g2.dispose();
                return;
            }
            int gap = 3;
            int n = count;
            int cw = Math.max(2, Math.min(24, (getWidth() - (n - 1) * gap) / n));
            int h = Math.min(getHeight(), 22);
            g2.setColor(colorOf(id));
            for (int i = 0; i < n; i++) g2.fillRoundRect(i * (cw + gap), 2, cw, h, 6, 6);
            g2.dispose();
        }
    }

    /** Rounded panel with a colored fill and edge. */
    private static class RoundPanel extends JPanel {
        private Color fill;
        private Color edge;
        private final int arc;

        RoundPanel(LayoutManager lm, Color fill, Color edge, int arc) {
            super(lm);
            this.fill = fill;
            this.edge = edge;
            this.arc = arc;
            setOpaque(false);
        }

        void colors(Color fill, Color edge) {
            this.fill = fill;
            this.edge = edge;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
            g2.dispose();
        }
    }

    /** One number card: small label, big colored value, small unit. */
    private static class StatCard extends RoundPanel {
        private final JLabel value = new JLabel("0");

        StatCard(String label, String unit, Color valueColor) {
            super(new GridLayout(3, 1, 0, 0), PANEL, EDGE, 20);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));
            JLabel top = new JLabel(label);
            top.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            top.setForeground(TEXT);
            value.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
            value.setForeground(valueColor);
            JLabel bottom = new JLabel(unit);
            bottom.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bottom.setForeground(MUTED);
            add(top);
            add(value);
            add(bottom);
        }

        void setValue(String text) {
            value.setText(text);
        }
    }

    /** Rounded button with its own colors. */
    private static class PillButton extends JButton {
        private Color fill;
        private Color edge;

        PillButton(String text, Color fill, Color fore, Color edge) {
            super(text);
            this.fill = fill;
            this.edge = edge;
            setForeground(fore);
            setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void style(Color fill, Color fore, Color edge) {
            this.fill = fill;
            this.edge = edge;
            setForeground(fore);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            Color c = getModel().isPressed() ? shade(fill, 0.8) : getModel().isRollover() ? fill.brighter() : fill;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            if (edge != null) {
                g2.setColor(edge);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Fallback logo, used only if the embedded icon image cannot be decoded. */
    private static class LogoIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setPaint(new GradientPaint(x, y, new Color(140, 90, 240), x, y + 36, new Color(80, 50, 190)));
            g2.fillRoundRect(x, y, 36, 36, 12, 12);
            g2.setColor(PALETTE[0]);
            g2.fillRoundRect(x + 6, y + 7, 14, 8, 4, 4);
            g2.setColor(PALETTE[1]);
            g2.fillRoundRect(x + 22, y + 7, 8, 8, 4, 4);
            g2.setColor(PALETTE[2]);
            g2.fillRoundRect(x + 6, y + 18, 8, 8, 4, 4);
            g2.setColor(PALETTE[5]);
            g2.fillRoundRect(x + 16, y + 18, 14, 8, 4, 4);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 36;
        }

        @Override
        public int getIconHeight() {
            return 36;
        }
    }

    /** The 52 x 52 desktop icon: your Fragmentation image (embedded), with a drawn copy as a backup. */
    public static Icon desktopIcon() {
        Icon img = FragmentationIcon.create(52);
        if (img != null) return img;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                smooth(g2);
                g2.setPaint(new GradientPaint(x, y, new Color(150, 100, 245), x, y + 52, new Color(78, 44, 188)));
                g2.fillRoundRect(x, y, 52, 52, 16, 16);
                g2.setColor(PALETTE[0]);
                g2.fillRoundRect(x + 8, y + 9, 22, 10, 5, 5);
                g2.setColor(PALETTE[1]);
                g2.fillRoundRect(x + 34, y + 9, 11, 10, 5, 5);
                g2.setColor(PALETTE[2]);
                g2.fillRoundRect(x + 8, y + 22, 12, 10, 5, 5);
                g2.setColor(PALETTE[5]);
                g2.fillRoundRect(x + 34, y + 22, 11, 10, 5, 5);
                g2.setColor(PALETTE[4]);
                g2.fillRoundRect(x + 8, y + 35, 25, 10, 5, 5);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 2.5f}, 0f));
                g2.drawRoundRect(x + 36, y + 35, 9, 10, 4, 4);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 52;
            }

            @Override
            public int getIconHeight() {
                return 52;
            }
        };
    }
}
package opersys.multiprogramming;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import opersys.core.UiKit;
import opersys.multiprogramming.MultiprogrammingEngine.Job;
import opersys.multiprogramming.MultiprogrammingEngine.Snap;
import opersys.multiprogramming.MultiprogrammingEngine.Step;
import opersys.DecimalInput;
import opersys.multiprogramming.MultiprogrammingModel.Run;

/**
 * Multiprogramming ("Task Monitor"): jobs are loaded into memory while space allows and share the CPU round-robin.
 * Views: Processes (live table + memory bar), Step by Step (every CPU slice explained) and History (every past run).
 */
public class MultiprogrammingDesign extends JPanel {
    // ---------- look ----------
    private static final Color BG = UiKit.SURFACE;
    private static final Color LINE = new Color(225, 225, 225);
    private static final Color TEXT = new Color(32, 32, 32);
    private static final Color MUTED = new Color(96, 96, 96);
    private static final Color ACCENT = UiKit.ACCENT;
    private static final Color BLUE = new Color(0, 120, 215);
    private static final Color GREEN = new Color(16, 124, 16);
    private static final Color BROWN = new Color(157, 93, 0);
    private static final Color AMBER = new Color(214, 140, 0);
    private static final Color PURPLE = new Color(140, 80, 200);
    private static final Color[] PALETTE = {
            new Color(86, 190, 255), new Color(255, 176, 70), new Color(126, 222, 128), new Color(200, 130, 250),
            new Color(255, 140, 160), new Color(255, 214, 80), new Color(80, 210, 225), new Color(255, 120, 90),
            new Color(190, 230, 90), new Color(150, 150, 255)
    };

    // job states inside a snapshot
    private static final int RUNNING = MultiprogrammingEngine.RUNNING;
    private static final int READY = MultiprogrammingEngine.READY;
    private static final int WAITING = MultiprogrammingEngine.WAITING;
    private static final int DONE = MultiprogrammingEngine.DONE;

    // kinds of lines inside a step
    private static final int L_INFO = MultiprogrammingEngine.L_INFO;
    private static final int L_LOAD = MultiprogrammingEngine.L_LOAD;
    private static final int L_RUN = MultiprogrammingEngine.L_RUN;
    private static final int L_DONE = MultiprogrammingEngine.L_DONE;
    private static final int L_QUEUE = MultiprogrammingEngine.L_QUEUE;

    // ---------- screen parts ----------
    // the "Add a process" form: three boxes and an Add button; added processes show as chips
    private final JTextField nameField = miniField("A", 64);
    private final JTextField entryMemField = miniField("200", 64);
    private final JTextField entryBurstField = miniField("5", 56);
    private final FlatButton addBtn = new FlatButton("Add", false);
    private final JPanel chipRow = new JPanel();
    private final List<String[]> addedJobs = new ArrayList<>();   // each one: {name, memory, burst}
    private final JTextField memoryField = miniField("800", 52);
    private final JTextField quantumField = miniField("2", 40);

    private final ProcessTable table = new ProcessTable();
    private final MemoryCard memoryCard = new MemoryCard();
    private final StatCard levelCard = new StatCard("Jobs in\nmemory", BLUE);
    private final StatCard waitCard = new StatCard("Waiting", BROWN);
    private final StatCard cpuCard = new StatCard("CPU use", GREEN);
    private final JLabel timeLabel = new JLabel("Time t = 0");
    private final FlatButton simulateBtn = new FlatButton("Simulate", true);
    private final FlatButton resetBtn = new FlatButton("Reset", false);
    private final GlyphButton prevBtn = new GlyphButton(GlyphButton.PREV);
    private final GlyphButton nextBtn = new GlyphButton(GlyphButton.NEXT);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final NavButton[] nav = {
            new NavButton(NavButton.LIST, "Processes"),
            new NavButton(NavButton.CHART, "Step by Step"),
            new NavButton(NavButton.CLOCK, "History")
    };

    private final ScrollPanel stepsList = new ScrollPanel();
    private final GanttBar stepsGantt = new GanttBar();
    private final List<StepCard> stepCards = new ArrayList<>();
    private JScrollPane stepsScroll;

    private final DefaultListModel<Run> historyModel = new DefaultListModel<>();
    private final JList<Run> historyList = new JList<>(historyModel);
    private final JScrollPane historyDetail = new JScrollPane();
    private List<Run> historyEntries = new ArrayList<>();

    // ---------- simulation state ----------
    private Run run;
    private int cur;
    private boolean playing;
    private boolean paused;
    private final Timer timer;
    private MultiprogrammingController controller;

    public MultiprogrammingDesign() {
        super(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(900, 610));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        timer = new Timer(1000, e -> tick());
        run = null;                 // start empty until the user types jobs and presses Simulate
        rebuildSteps();
        refreshHistory(-1);
        showEmpty();
        showView(0);
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // =====================================================================================
    //  Controller-friendly API
    // =====================================================================================

    /** The controller calls this to wire itself up. */
    public void bindController(MultiprogrammingController controller) {
        this.controller = controller;
    }

    public void setHistory(List<Run> history) {
        historyEntries = new ArrayList<>(history);
        refreshHistory(historyEntries.isEmpty() ? -1 : 0);
    }

    /** The jobs text the user typed. */
    public String getJobsText() {
        StringBuilder text = new StringBuilder();
        for (String[] job : addedJobs) {
            if (text.length() > 0) text.append(", ");
            text.append(job[0]).append(' ').append(job[1]).append(' ').append(job[2]);
        }
        return text.toString();
    }

    /** The total memory text. */
    public String getMemoryText() {
        return memoryField.getText();
    }

    /** The quantum text. */
    public String getQuantumText() {
        return quantumField.getText();
    }

    /** Apply a fresh result from the controller. */
    public void showRun(Run r) {
        this.run = r;
        this.cur = 0;
        this.playing = false;
        this.paused = false;
        this.timer.stop();
        rebuildSteps();
        showStep(0);
        updateButtons();
    }

    /** Show a friendly error popup. */
    public void showError(String message) {
        UiKit.error(this, message);
    }

    // =====================================================================================
    //  Building the screen
    // =====================================================================================

    private JComponent buildTopBar() {
        JLabel title = new JLabel("Multiprogramming");
        title.setFont(ui(Font.BOLD, 16));
        title.setForeground(TEXT);
        Icon logo = MultiprogrammingIcon.create(28);
        title.setIcon(logo != null ? logo : new TileIcon(28));
        title.setIconTextGap(10);

        Box left = Box.createHorizontalBox();
        title.setAlignmentY(0.5f);
        left.add(Box.createHorizontalStrut(4));
        left.add(title);

        Box right = Box.createHorizontalBox();
        right.add(smallLabel("Memory"));
        right.add(Box.createHorizontalStrut(8));
        right.add(memoryField);
        right.add(Box.createHorizontalStrut(6));
        right.add(smallLabel("KB"));
        right.add(Box.createHorizontalStrut(14));
        right.add(smallLabel("Quantum"));
        right.add(Box.createHorizontalStrut(8));
        right.add(quantumField);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 6, 6, 16));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH);
        top.add(buildEntryRow(), BorderLayout.CENTER);
        return top;
    }

    /** The "Add a process" row: Name, Memory, Burst, an Add button, and the processes added so far. */
    private JComponent buildEntryRow() {
        JLabel title = smallLabel("Add a process");
        title.setFont(ui(Font.BOLD, 12));
        title.setForeground(TEXT);

        Box form = Box.createHorizontalBox();
        form.add(title);
        form.add(Box.createHorizontalStrut(12));
        form.add(smallLabel("Name"));
        form.add(Box.createHorizontalStrut(6));
        form.add(nameField);
        form.add(Box.createHorizontalStrut(12));
        form.add(smallLabel("Memory"));
        form.add(Box.createHorizontalStrut(6));
        form.add(entryMemField);
        form.add(Box.createHorizontalStrut(4));
        form.add(smallLabel("KB"));
        form.add(Box.createHorizontalStrut(12));
        form.add(smallLabel("Burst"));
        form.add(Box.createHorizontalStrut(6));
        form.add(entryBurstField);
        form.add(Box.createHorizontalStrut(12));
        addBtn.setAlignmentY(0.5f);
        form.add(addBtn);

        // Enter moves to the next box, and Enter in the last one adds the process
        nameField.addActionListener(e -> entryMemField.requestFocusInWindow());
        entryMemField.addActionListener(e -> entryBurstField.requestFocusInWindow());
        entryBurstField.addActionListener(e -> addJob());
        addBtn.addActionListener(e -> addJob());

        chipRow.setLayout(new BoxLayout(chipRow, BoxLayout.X_AXIS));
        chipRow.setOpaque(false);
        JScrollPane chips = new JScrollPane(chipRow, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chips.setBorder(null);
        chips.setOpaque(false);
        chips.getViewport().setOpaque(false);
        chips.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
        refreshChips();

        EntryCard card = new EntryCard();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(8, 14, 6, 10));
        chips.setPreferredSize(new Dimension(100, 34));
        JPanel formRow = new JPanel(new BorderLayout());
        formRow.setOpaque(false);
        formRow.add(form, BorderLayout.WEST);
        card.add(formRow, BorderLayout.NORTH);
        card.add(chips, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 16));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    /** Checks the three boxes and adds the process. Returns false (after showing why) if something is wrong. */
    private boolean addJob() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = nextJobName();
        double mem;
        double burst;
        try {
            if (name.matches(".*[\\s,;:].*"))
                throw new IllegalArgumentException("The name can't have spaces, commas or colons.");
            for (String[] job : addedJobs) {
                if (job[0].equalsIgnoreCase(name))
                    throw new IllegalArgumentException("There is already a process named " + name + ".");
            }
            mem = MultiprogrammingModel.parseDecimal(entryMemField.getText(), "Memory of " + name);
            burst = MultiprogrammingModel.parseDecimal(entryBurstField.getText(), "Burst of " + name);
            if (mem <= 0) throw new IllegalArgumentException("Process " + name + ": memory must be greater than 0.");
            if (burst <= 0) throw new IllegalArgumentException("Process " + name + ": burst must be greater than 0.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return false;
        }
        addedJobs.add(new String[]{name, String.valueOf(mem), String.valueOf(burst)});
        nameField.setText("");
        entryMemField.setText("");
        entryBurstField.setText("");
        refreshChips();
        nameField.requestFocusInWindow();
        return true;
    }

    /** Something typed in the boxes that was not added yet. */
    private boolean hasPendingEntry() {
        return !nameField.getText().trim().isEmpty() || !entryMemField.getText().trim().isEmpty()
                || !entryBurstField.getText().trim().isEmpty();
    }

    /** First free letter (A, B, C ...) for a process that was added without a name. */
    private String nextJobName() {
        for (int i = 0; ; i++) {
            String candidate = String.valueOf((char) ('A' + i % 26)) + (i < 26 ? "" : String.valueOf(i / 26));
            boolean used = false;
            for (String[] job : addedJobs) used |= job[0].equalsIgnoreCase(candidate);
            if (!used) return candidate;
        }
    }

    private void refreshChips() {
        chipRow.removeAll();
        if (addedJobs.isEmpty()) {
            chipRow.add(smallLabel("No processes added yet"));
        }
        for (int i = 0; i < addedJobs.size(); i++) {
            chipRow.add(new JobChip(i, addedJobs.get(i)));
            chipRow.add(Box.createHorizontalStrut(6));
        }
        chipRow.add(Box.createHorizontalGlue());
        chipRow.revalidate();
        chipRow.repaint();
    }

    private void removeJob(int index) {
        if (index >= 0 && index < addedJobs.size()) addedJobs.remove(index);
        refreshChips();
    }

    private JComponent buildBody() {
        Box side = Box.createVerticalBox();
        side.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 2));
        for (int i = 0; i < nav.length; i++) {
            final int index = i;
            nav[i].addActionListener(e -> showView(index));
            side.add(nav[i]);
            side.add(Box.createVerticalStrut(6));
        }
        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.add(side, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildProcessesView(), "0");
        cards.add(buildStepsView(), "1");
        cards.add(buildHistoryView(), "2");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(sideWrap, BorderLayout.WEST);
        body.add(cards, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildProcessesView() {
        JLabel title = new JLabel("Processes");
        title.setFont(ui(Font.BOLD, 22));
        title.setForeground(TEXT);

        timeLabel.setFont(ui(Font.PLAIN, 13));
        timeLabel.setForeground(MUTED);
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeLabel.setPreferredSize(new Dimension(118, 30));

        prevBtn.addActionListener(e -> {
            pause();
            showStep(cur - 1);
        });
        nextBtn.addActionListener(e -> {
            pause();
            showStep(cur + 1);
        });
        simulateBtn.addActionListener(e -> onSimulate());
        resetBtn.addActionListener(e -> onReset());

        Box actions = Box.createHorizontalBox();
        for (JComponent comp : new JComponent[]{prevBtn, timeLabel, nextBtn}) comp.setAlignmentY(0.5f);
        actions.add(prevBtn);
        actions.add(timeLabel);
        actions.add(nextBtn);
        actions.add(Box.createHorizontalStrut(10));
        simulateBtn.setAlignmentY(0.5f);
        resetBtn.setAlignmentY(0.5f);
        actions.add(simulateBtn);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(resetBtn);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        RoundCard tableCard = new RoundCard(new BorderLayout(), 12);
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(10, 140));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 464;
        c.insets = new Insets(0, 0, 0, 10);
        bottom.add(memoryCard, c);
        c.gridx = 1;
        c.weightx = 111;
        bottom.add(levelCard, c);
        c.gridx = 2;
        bottom.add(waitCard, c);
        c.gridx = 3;
        c.insets = new Insets(0, 0, 0, 0);
        bottom.add(cpuCard, c);

        JPanel view = new JPanel(new BorderLayout(0, 12));
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 16));
        view.add(header, BorderLayout.NORTH);
        view.add(tableCard, BorderLayout.CENTER);
        view.add(bottom, BorderLayout.SOUTH);
        return view;
    }

    private JComponent buildStepsView() {
        JLabel title = new JLabel("Step by Step");
        title.setFont(ui(Font.BOLD, 22));
        title.setForeground(TEXT);
        JLabel hint = new JLabel("Every CPU turn of the current run. Click a step to show it on the Processes screen.");
        hint.setFont(ui(Font.PLAIN, 12));
        hint.setForeground(MUTED);

        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(hint, BorderLayout.SOUTH);

        RoundCard ganttCard = new RoundCard(new BorderLayout(), 12);
        ganttCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel ganttTitle = new JLabel("CPU timeline");
        ganttTitle.setFont(ui(Font.BOLD, 12));
        ganttTitle.setForeground(MUTED);
        ganttCard.add(ganttTitle, BorderLayout.NORTH);
        ganttCard.add(stepsGantt, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(ganttCard, BorderLayout.CENTER);

        stepsScroll = new JScrollPane(stepsList);
        stepsScroll.setBorder(null);
        stepsScroll.setOpaque(false);
        stepsScroll.getViewport().setOpaque(false);
        stepsScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel view = new JPanel(new BorderLayout(0, 10));
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 16));
        view.add(north, BorderLayout.NORTH);
        view.add(stepsScroll, BorderLayout.CENTER);
        return view;
    }

    private JComponent buildHistoryView() {
        JLabel title = new JLabel("History");
        title.setFont(ui(Font.BOLD, 22));
        title.setForeground(TEXT);
        JLabel hint = new JLabel("Every simulation you have run. Pick one to see each process and every step again.");
        hint.setFont(ui(Font.PLAIN, 12));
        hint.setForeground(MUTED);
        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(hint, BorderLayout.SOUTH);

        historyList.setCellRenderer(new RunRenderer());
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setFixedCellHeight(-1);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedHistory();
        });
        JScrollPane listScroll = new JScrollPane(historyList);
        listScroll.setBorder(BorderFactory.createLineBorder(LINE));
        listScroll.getViewport().setBackground(Color.WHITE);

        historyDetail.setBorder(BorderFactory.createLineBorder(LINE));
        historyDetail.getViewport().setBackground(BG);
        historyDetail.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, historyDetail);
        split.setResizeWeight(0.0);
        split.setDividerLocation(250);
        split.setBorder(null);
        split.setOpaque(false);

        FlatButton clear = new FlatButton("Clear history", false);
        clear.addActionListener(e -> {
            if (controller != null) controller.clearHistory();
            else {
                historyEntries.clear();
                refreshHistory(-1);
            }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(clear);

        JPanel view = new JPanel(new BorderLayout(0, 10));
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 16));
        view.add(head, BorderLayout.NORTH);
        view.add(split, BorderLayout.CENTER);
        view.add(bottom, BorderLayout.SOUTH);
        return view;
    }

    private void showView(int index) {
        cardLayout.show(cards, String.valueOf(index));
        for (int i = 0; i < nav.length; i++) nav[i].setSelectedNav(i == index);
    }

    // =====================================================================================
    //  Playing the simulation
    // =====================================================================================

    private void onSimulate() {
        if (playing) {
            pause();
        } else if (paused && run != null && cur < run.steps.size() - 1) {
            paused = false;
            playing = true;
            timer.start();
            updateButtons();
        } else {
            if (hasPendingEntry() && !addJob()) return;   // whatever is typed in the boxes counts too
            if (controller != null) {
                // delegate to the controller so the model validates the input
                try {
                    Run r = controller.buildRun();
                    showRun(r);
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                }
            } else {
                showError("The simulation is not connected to its controller.");
            }
        }
    }

    /** Back to the empty start: clears the inputs and the screen (History is kept). */
    private void onReset() {
        timer.stop();
        playing = false;
        paused = false;
        run = null;
        cur = 0;
        addedJobs.clear();
        nameField.setText("");
        entryMemField.setText("");
        entryBurstField.setText("");
        refreshChips();
        memoryField.setText("");
        quantumField.setText("");
        rebuildSteps();
        showEmpty();
    }

    private void showEmpty() {
        table.set(null, null);
        memoryCard.set(null, null);
        levelCard.set("0", "multiprogramming\nlevel");
        waitCard.set("0", "in the job queue");
        cpuCard.set("0%", "idle");
        timeLabel.setText("Time t = 0");
        stepsGantt.set(null, -1);
        updateButtons();
    }

    private void pause() {
        if (!playing) return;
        timer.stop();
        playing = false;
        paused = true;
        updateButtons();
    }

    private void tick() {
        if (cur < run.steps.size() - 1) {
            showStep(cur + 1);
        }
        if (cur >= run.steps.size() - 1) {
            timer.stop();
            playing = false;
            paused = false;
            updateButtons();
        }
    }

    private void updateButtons() {
        if (run == null) {
            simulateBtn.setLabel("Simulate", FlatButton.PLAY);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }
        if (playing) {
            simulateBtn.setLabel("Pause", FlatButton.PAUSE);
        } else if (paused && cur < run.steps.size() - 1) {
            simulateBtn.setLabel("Resume", FlatButton.PLAY);
        } else {
            simulateBtn.setLabel("Simulate", FlatButton.PLAY);
        }
        prevBtn.setEnabled(cur > 0);
        nextBtn.setEnabled(cur < run.steps.size() - 1);
    }

    /** Shows step i on every part of the Processes screen. */
    private void showStep(int i) {
        if (run == null) {
            showEmpty();
            return;
        }
        cur = Math.max(0, Math.min(i, run.steps.size() - 1));
        Step st = run.steps.get(cur);
        Snap s = st.snap;

        table.set(run, s);
        memoryCard.set(run, s);
        int waiting = 0;
        for (int state : s.state) if (state == WAITING) waiting++;
        levelCard.set(String.valueOf(s.memOrder.length), "multiprogramming\nlevel");
        waitCard.set(String.valueOf(waiting), "in the job queue");
        if (s.running >= 0) cpuCard.set("100%", "never idle");
        else cpuCard.set("0%", "all done");
        timeLabel.setText(st.last ? "Time t = " + s.time + " · Done" : "Time t = " + s.time);

        stepsGantt.set(run, cur);
        for (int k = 0; k < stepCards.size(); k++) stepCards.get(k).setSelected(k == cur);
        if (cur < stepCards.size()) {
            StepCard card = stepCards.get(cur);
            card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
        }
        updateButtons();
    }

    // =====================================================================================
    //  Steps list and history
    // =====================================================================================

    private void rebuildSteps() {
        stepsList.removeAll();
        stepCards.clear();
        stepsList.setLayout(new BoxLayout(stepsList, BoxLayout.Y_AXIS));
        if (run == null) {
            JLabel none = new JLabel("Nothing to show yet. Add some processes on the Processes screen and press Simulate.");
            none.setFont(ui(Font.PLAIN, 13));
            none.setForeground(MUTED);
            none.setBorder(BorderFactory.createEmptyBorder(16, 8, 0, 0));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            stepsList.add(none);
            stepsList.revalidate();
            stepsList.repaint();
            return;
        }
        for (int k = 0; k < run.steps.size(); k++) {
            final int index = k;
            StepCard card = new StepCard(run, run.steps.get(k), () -> {
                pause();
                showStep(index);
            });
            stepCards.add(card);
            stepsList.add(card);
            stepsList.add(Box.createVerticalStrut(8));
        }
        stepsList.revalidate();
        stepsList.repaint();
    }

    private void refreshHistory(int selectIndex) {
        historyModel.clear();
        for (Run r : historyEntries) historyModel.addElement(r);
        if (selectIndex >= 0 && selectIndex < historyModel.size()) historyList.setSelectedIndex(selectIndex);
        else historyList.clearSelection();
        showSelectedHistory();
    }

    private void showSelectedHistory() {
        int idx = historyList.getSelectedIndex();
        if (idx < 0 || idx >= historyEntries.size()) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
            p.setBackground(BG);
            JLabel l = new JLabel(historyEntries.isEmpty()
                    ? "No history yet. Press Simulate to record the first run."
                    : "Select a run on the left to see its processes and steps.");
            l.setFont(ui(Font.PLAIN, 13));
            l.setForeground(MUTED);
            p.add(l);
            historyDetail.setViewportView(p);
        } else {
            historyDetail.setViewportView(buildRunDetail(historyEntries.get(idx)));
            historyDetail.getVerticalScrollBar().setValue(0);
        }
    }

    /** Everything about one finished run: per-process results, the CPU timeline, and every step. */
    private static JComponent buildRunDetail(Run r) {
        ScrollPanel p = new ScrollPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Run #" + r.number + "  ·  " + r.time);
        title.setFont(ui(Font.BOLD, 17));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title);
        JLabel sub = new JLabel(r.jobs.size() + " jobs · " + DecimalInput.format(r.memory)
                + " KB memory · quantum " + DecimalInput.format(r.quantum)
                + " · input: " + r.input);
        sub.setFont(ui(Font.PLAIN, 12));
        sub.setForeground(MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(10));

        // per-process results
        String[] heads = {"Process", "Memory", "Burst", "Loaded", "Finished", "Turnaround", "Waiting"};
        RoundCard grid = new RoundCard(new GridLayout(0, heads.length, 6, 4), 12);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        for (String h : heads) grid.add(cell(h, false, MUTED, h.equals("Process")));
        double totalTurn = 0;
        double totalWait = 0;
        for (Job j : r.jobs) {
            double turn = j.finish;
            double wait = turn - j.burst;
            totalTurn += turn;
            totalWait += wait;
            grid.add(cell("Job " + j.name, true, TEXT, true));
            grid.add(cell(j.memory + " KB", false, TEXT, false));
            grid.add(cell(String.valueOf(j.burst), false, TEXT, false));
            grid.add(cell("t=" + j.loadedAt, false, TEXT, false));
            grid.add(cell("t=" + j.finish, false, TEXT, false));
            grid.add(cell(String.valueOf(turn), false, TEXT, false));
            grid.add(cell(String.valueOf(wait), false, TEXT, false));
        }
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        p.add(grid);
        p.add(Box.createVerticalStrut(6));

        int n = Math.max(1, r.jobs.size());
        JLabel summary = new JLabel(String.format(
                "<html>All jobs finished at <b>t=%s</b> · highest degree of multiprogramming <b>%d</b>"
                        + " · average turnaround <b>%s</b> · average waiting <b>%s</b></html>",
                DecimalInput.format(r.total), r.maxDegree,
                DecimalInput.format(totalTurn / n), DecimalInput.format(totalWait / n)));
        summary.setFont(ui(Font.PLAIN, 12));
        summary.setForeground(TEXT);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(summary);
        p.add(Box.createVerticalStrut(12));

        // CPU timeline
        RoundCard ganttCard = new RoundCard(new BorderLayout(), 12);
        ganttCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel gt = new JLabel("CPU timeline");
        gt.setFont(ui(Font.BOLD, 12));
        gt.setForeground(MUTED);
        GanttBar gantt = new GanttBar();
        gantt.set(r, -1);
        ganttCard.add(gt, BorderLayout.NORTH);
        ganttCard.add(gantt, BorderLayout.CENTER);
        ganttCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        ganttCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, ganttCard.getPreferredSize().height));
        p.add(ganttCard);
        p.add(Box.createVerticalStrut(12));

        JLabel stepsTitle = new JLabel("Step by step");
        stepsTitle.setFont(ui(Font.BOLD, 14));
        stepsTitle.setForeground(TEXT);
        stepsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(stepsTitle);
        p.add(Box.createVerticalStrut(6));
        for (Step s : r.steps) {
            StepCard card = new StepCard(r, s, null);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(card);
            p.add(Box.createVerticalStrut(8));
        }
        return p;
    }

    private static JLabel cell(String text, boolean bold, Color color, boolean left) {
        JLabel l = new JLabel(text, left ? SwingConstants.LEFT : SwingConstants.RIGHT);
        l.setFont(ui(bold ? Font.BOLD : Font.PLAIN, 12));
        l.setForeground(color);
        return l;
    }

    // =====================================================================================
    //  Desktop icon
    // =====================================================================================

    /** The 52 x 52 desktop icon: the Multiprogramming image (embedded), with a drawn copy as a backup. */
    public static Icon desktopIcon() {
        Icon img = MultiprogrammingIcon.create(52);
        return img != null ? img : new TileIcon(52);
    }

    /** Fallback tile (blue with three bars), used only if the embedded image cannot be decoded. */
    private static class TileIcon implements Icon {
        private final int size;

        TileIcon(int size) {
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setPaint(new GradientPaint(x, y, new Color(40, 140, 250), x, y + size, new Color(10, 80, 200)));
            g2.fillRoundRect(x, y, size, size, size / 4, size / 4);
            g2.setColor(Color.WHITE);
            int bw = Math.max(3, size / 6);
            int base = y + size - size / 5;
            g2.fillRoundRect(x + size / 5, base - size * 2 / 5, bw, size * 2 / 5, 3, 3);
            g2.fillRoundRect(x + size / 2 - bw / 2, base - size * 3 / 5, bw, size * 3 / 5, 3, 3);
            g2.fillRoundRect(x + size - size / 5 - bw, base - size / 2, bw, size / 2, 3, 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    // =====================================================================================
    //  Small helpers
    // =====================================================================================

    private static Font ui(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static Color mix(Color a, Color b, double f) {
        f = Math.max(0, Math.min(1, f));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    private static Color heat(double f) {
        return mix(new Color(255, 248, 226), new Color(255, 200, 64), f);
    }

    private static void drawRight(Graphics2D g2, String text, int xRight, int y) {
        g2.drawString(text, xRight - g2.getFontMetrics().stringWidth(text), y);
    }

    private static void drawCentered(Graphics2D g2, String text, int cx, int y) {
        g2.drawString(text, cx - g2.getFontMetrics().stringWidth(text) / 2, y);
    }

    private static JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ui(Font.PLAIN, 12));
        l.setForeground(MUTED);
        l.setAlignmentY(0.5f);
        return l;
    }

    private static String ordinal(int value) {
        int mod100 = value % 100;
        if (mod100 >= 11 && mod100 <= 13) return value + "th";
        switch (value % 10) {
            case 1: return value + "st";
            case 2: return value + "nd";
            case 3: return value + "rd";
            default: return value + "th";
        }
    }

    /** Text field that shows a gray example while it is empty. */
    private static class HintField extends JTextField {
        private final String hint;
        private final boolean centered;

        HintField(String hint, boolean centered) {
            this.hint = hint;
            this.centered = centered;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setFont(getFont());
            g2.setColor(new Color(160, 160, 160));
            FontMetrics fm = g2.getFontMetrics();
            int x = centered ? (getWidth() - fm.stringWidth(hint)) / 2 : getInsets().left;
            g2.drawString(hint, x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static JTextField miniField(String hint, int width) {
        JTextField f = new HintField(hint, true);
        f.setFont(ui(Font.PLAIN, 13));
        f.setForeground(TEXT);
        f.setHorizontalAlignment(SwingConstants.CENTER);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 4, 5, 4)));
        Dimension d = new Dimension(width, 34);
        f.setPreferredSize(d);
        f.setMaximumSize(d);
        f.setAlignmentY(0.5f);
        return f;
    }

    /** Draws the memory bar: one colored segment per job in memory and a gray one for the free space. */
    private static void drawMemoryBar(Graphics2D g2, int x, int y, int w, int h, Run r, Snap s, boolean labels) {
        Shape oldClip = g2.getClip();
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, w, h, 12, 12);
        g2.clip(shape);
        g2.setColor(new Color(236, 236, 236));
        g2.fillRect(x, y, w, h);
        double scale = w / (double) Math.max(1, s.total);
        double px = x;
        g2.setFont(ui(Font.PLAIN, 12));
        for (int idx : s.memOrder) {
            Job j = r.jobs.get(idx);
            double sw = j.memory * scale;
            g2.setColor(PALETTE[idx % PALETTE.length]);
            g2.fillRect((int) Math.round(px), y, (int) Math.round(px + sw) - (int) Math.round(px), h);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillRect((int) Math.round(px), y, 1, h);
            if (labels && sw >= 44) {
                g2.setColor(TEXT);
                drawCentered(g2, j.name + " " + j.memory, (int) Math.round(px + sw / 2),
                        y + (h + g2.getFontMetrics().getAscent()) / 2 - 2);
            }
            px += sw;
        }
        double free = s.total - s.used;
        double fw = free * scale;
        if (labels && free > 0 && fw >= 56) {
            g2.setColor(MUTED);
            drawCentered(g2, "Free " + free, (int) Math.round(px + fw / 2),
                    y + (h + g2.getFontMetrics().getAscent()) / 2 - 2);
        }
        g2.setClip(oldClip);
        g2.setColor(new Color(205, 205, 205));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 12, 12));
    }

    // =====================================================================================
    //  Custom components
    // =====================================================================================

    /** White rounded panel with a thin border; its children are clipped to the rounded shape. */
    private static class RoundCard extends JPanel {
        private final int arc;

        RoundCard(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            super.paintChildren(g2);
            g2.dispose();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }

    /** A panel that always matches the width of its scroll pane. */
    private static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(16, r.height - 32);
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

    /** The search-box style field at the top: magnifier, a label, and the text the user types. */
    /** White rounded card that holds the "Add a process" row. */
    private static class EntryCard extends JPanel {
        EntryCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 92));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(new Color(212, 212, 212));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
        }
    }

    /** One added process: a colored dot, "A  200 KB · burst 5", and a small x to remove it. */
    private final class JobChip extends JPanel {
        JobChip(int index, String[] job) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 6));
            final Color color = PALETTE[index % PALETTE.length];
            JComponent dot = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    smooth(g2);
                    g2.setColor(color);
                    g2.fillOval(0, (getHeight() - 10) / 2, 10, 10);
                    g2.dispose();
                }
            };
            Dimension dotSize = new Dimension(10, 14);
            dot.setPreferredSize(dotSize);
            dot.setMinimumSize(dotSize);
            dot.setMaximumSize(dotSize);
            dot.setAlignmentY(0.5f);
            JLabel text = new JLabel(job[0] + "   " + job[1] + " KB · burst " + job[2]);
            text.setFont(ui(Font.PLAIN, 12));
            text.setForeground(TEXT);
            text.setAlignmentY(0.5f);
            CloseButton close = new CloseButton(() -> removeJob(index));
            close.setAlignmentY(0.5f);
            add(dot);
            add(Box.createHorizontalStrut(6));
            add(text);
            add(Box.createHorizontalStrut(8));
            add(close);
            setAlignmentY(0.5f);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(new Color(244, 246, 250));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(new Color(214, 218, 226));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
        }
    }

    /** Small x that runs an action when clicked. */
    private static class CloseButton extends JComponent {
        private boolean hover;

        CloseButton(Runnable onClick) {
            Dimension size = new Dimension(14, 14);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }

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
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(hover ? new Color(196, 43, 28) : MUTED);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(4, 4, getWidth() - 5, getHeight() - 5);
            g2.drawLine(getWidth() - 5, 4, 4, getHeight() - 5);
            g2.dispose();
        }
    }

    /** Flat rounded button. The blue one is the main action, the white one is secondary. */
    private static class FlatButton extends JButton {
        static final int NONE = 0;
        static final int PLAY = 1;
        static final int PAUSE = 2;
        private final boolean primary;
        private int icon = NONE;

        FlatButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(ui(primary ? Font.BOLD : Font.PLAIN, 13));
            setForeground(primary ? Color.WHITE : TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (primary) icon = PLAY;
        }

        void setLabel(String text, int iconKind) {
            setText(text);
            icon = iconKind;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int content = fm.stringWidth(getText()) + (icon != NONE ? 20 : 0);
            return new Dimension(Math.max(primary ? 108 : 70, content + 36), 34);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            boolean pressed = getModel().isPressed();
            boolean hover = getModel().isRollover();
            if (primary) {
                g2.setColor(pressed ? UiKit.ACCENT_PRESSED : hover ? UiKit.ACCENT_HOVER : UiKit.ACCENT);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            } else {
                g2.setColor(pressed ? new Color(236, 236, 236) : hover ? new Color(246, 246, 246) : Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.setColor(new Color(204, 204, 204));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int content = fm.stringWidth(getText()) + (icon != NONE ? 20 : 0);
            int x = (w - content) / 2;
            int cy = h / 2;
            g2.setColor(getForeground());
            if (icon == PLAY) {
                g2.fillPolygon(new int[]{x, x, x + 9}, new int[]{cy - 5, cy + 5, cy}, 3);
                x += 20;
            } else if (icon == PAUSE) {
                g2.fillRect(x, cy - 5, 3, 10);
                g2.fillRect(x + 6, cy - 5, 3, 10);
                x += 20;
            }
            g2.drawString(getText(), x, cy + (fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Small square icon button: menu, previous step, next step. */
    private static class GlyphButton extends JButton {
        static final int MENU = 0;
        static final int PREV = 1;
        static final int NEXT = 2;
        private final int kind;

        GlyphButton(int kind) {
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension d = new Dimension(kind == MENU ? 38 : 30, kind == MENU ? 36 : 30);
            setPreferredSize(d);
            setMaximumSize(d);
            setToolTipText(kind == MENU ? "Show or hide names" : kind == PREV ? "Previous step" : "Next step");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            if (isEnabled() && (getModel().isRollover() || getModel().isPressed())) {
                g2.setColor(getModel().isPressed() ? new Color(222, 222, 222) : new Color(232, 232, 232));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            g2.setColor(isEnabled() ? TEXT : new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            if (kind == MENU) {
                for (int i = -1; i <= 1; i++) g2.drawLine(cx - 7, cy + i * 5, cx + 7, cy + i * 5);
            } else if (kind == PREV) {
                g2.drawPolyline(new int[]{cx + 3, cx - 3, cx + 3}, new int[]{cy - 6, cy, cy + 6}, 3);
            } else {
                g2.drawPolyline(new int[]{cx - 3, cx + 3, cx - 3}, new int[]{cy - 6, cy, cy + 6}, 3);
            }
            g2.dispose();
        }
    }

    /** Sidebar button with a drawn icon. Shows its name too when the sidebar is open. */
    private static class NavButton extends JButton {
        static final int LIST = 0;
        static final int CHART = 1;
        static final int CLOCK = 2;
        private final int kind;
        private final String label;
        private boolean selected;
        private boolean open;

        NavButton(int kind, String label) {
            this.kind = kind;
            this.label = label;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(label);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setSelectedNav(boolean value) {
            selected = value;
            repaint();
        }

        void setOpen(boolean value) {
            open = value;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(open ? 150 : 38, 36);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            if (selected || getModel().isRollover()) {
                g2.setColor(selected ? new Color(229, 229, 229) : new Color(236, 236, 236));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }
            if (selected) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, h / 2 - 8, 3, 16, 3, 3);
            }
            g2.setColor(TEXT);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 19;
            int cy = h / 2;
            if (kind == LIST) {   // a small table, so it does not look like a menu button
                g2.drawRoundRect(cx - 8, cy - 7, 16, 14, 4, 4);
                g2.drawLine(cx - 8, cy - 2, cx + 8, cy - 2);
                g2.drawLine(cx - 8, cy + 3, cx + 8, cy + 3);
            } else if (kind == CHART) {
                g2.drawPolyline(new int[]{cx - 8, cx - 3, cx + 1, cx + 8}, new int[]{cy + 4, cy - 1, cy + 2, cy - 6}, 4);
            } else {
                g2.drawOval(cx - 7, cy - 7, 14, 14);
                g2.drawLine(cx, cy, cx, cy - 4);
                g2.drawLine(cx, cy, cx + 3, cy + 2);
            }
            if (open) {
                g2.setFont(ui(selected ? Font.BOLD : Font.PLAIN, 13));
                g2.drawString(label, 40, cy + (g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    /** The big table: groups (in memory / waiting / finished) with heat-colored CPU and memory columns. */
    private static class ProcessTable extends JComponent implements Scrollable {
        private static final int HEAD = 54;
        private static final int GROUP = 32;
        private static final int ROW = 40;
        private Run r;
        private Snap s;

        void set(Run r, Snap s) {
            this.r = r;
            this.s = s;
            revalidate();
            repaint();
        }

        /** Rows as {type, value}: type 0 = group heading (value = group id), type 1 = job (value = job index). */
        private List<int[]> rows() {
            List<int[]> out = new ArrayList<>();
            if (r == null || s == null) return out;
            int n = r.jobs.size();
            List<Integer> inMem = new ArrayList<>();
            List<Integer> wait = new ArrayList<>();
            List<Integer> done = new ArrayList<>();
            for (int i = 0; i < n; i++) if (s.state[i] == RUNNING) inMem.add(i);
            for (int p = 1; p <= n; p++) for (int i = 0; i < n; i++) if (s.state[i] == READY && s.pos[i] == p) inMem.add(i);
            for (int i = 0; i < n; i++) if (s.state[i] == WAITING) wait.add(i);
            for (int i = 0; i < n; i++) if (s.state[i] == DONE) done.add(i);
            done.sort((a, b) -> s.doneAt[a] != s.doneAt[b]
                    ? Double.compare(s.doneAt[a], s.doneAt[b]) : Integer.compare(a, b));
            addGroup(out, 0, inMem);
            addGroup(out, 1, wait);
            addGroup(out, 2, done);
            return out;
        }

        private static void addGroup(List<int[]> out, int id, List<Integer> jobs) {
            if (jobs.isEmpty()) return;
            out.add(new int[]{0, id, jobs.size()});
            for (int j : jobs) out.add(new int[]{1, j, 0});
        }

        @Override
        public Dimension getPreferredSize() {
            int h = HEAD + 2;
            for (int[] row : rows()) h += row[0] == 0 ? GROUP : ROW;
            return new Dimension(560, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            int memW = 110;
            int cpuW = 90;
            int memX = Math.max(300, w - 211);
            int cpuX = memX - cpuW;
            int statusX = Math.max(150, Math.min(368, cpuX - 175));
            int right = w - 12;

            int cpuPct = s != null && s.running >= 0 ? 100 : 0;
            int memPct = s == null || s.total == 0 ? 0 : (int) Math.round(s.used * 100.0 / s.total);

            // header
            g2.setColor(heat(cpuPct / 100.0));
            g2.fillRect(cpuX, 0, cpuW, HEAD);
            g2.setColor(heat(memPct / 100.0));
            g2.fillRect(memX, 0, memW, HEAD);
            g2.setColor(TEXT);
            g2.setFont(ui(Font.BOLD, 15));
            drawCentered(g2, cpuPct + "%", cpuX + cpuW / 2, 26);
            drawCentered(g2, memPct + "%", memX + memW / 2, 26);
            g2.setFont(ui(Font.PLAIN, 12));
            g2.setColor(MUTED);
            drawCentered(g2, "CPU", cpuX + cpuW / 2, 43);
            drawCentered(g2, "Memory", memX + memW / 2, 43);
            g2.drawString("Name", 40, 36);
            g2.drawString("Status", statusX, 36);
            drawRight(g2, "Burst left", right, 36);
            g2.setColor(LINE);
            g2.drawLine(0, HEAD, w, HEAD);

            if (r == null || s == null) {   // nothing to show yet
                int cy = HEAD + Math.max(60, (h - HEAD) / 2);
                g2.setColor(TEXT);
                g2.setFont(ui(Font.BOLD, 15));
                drawCentered(g2, "No processes yet", w / 2, cy - 12);
                g2.setColor(MUTED);
                g2.setFont(ui(Font.PLAIN, 12));
                drawCentered(g2, "Add your processes above (name, memory, burst), set the total memory and the quantum,",
                        w / 2, cy + 10);
                drawCentered(g2, "then press Simulate.", w / 2, cy + 28);
                g2.dispose();
                return;
            }

            double maxMem = 1;
            for (Job j : r.jobs) maxMem = Math.max(maxMem, j.memory);

            int y = HEAD + 1;
            for (int[] row : rows()) {
                if (row[0] == 0) {
                    String[] names = {"In memory", "Waiting for memory", "Finished"};
                    Color[] colors = {ACCENT, BROWN, GREEN};
                    Color c = colors[row[1]];
                    int cy = y + GROUP / 2;
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(new int[]{15, 19, 23}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g2.setFont(ui(Font.BOLD, 13));
                    g2.drawString(names[row[1]] + " (" + row[2] + ")", 32,
                            cy + (g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                    y += GROUP;
                    continue;
                }
                int i = row[1];
                Job job = r.jobs.get(i);
                int state = s.state[i];
                boolean inMemory = state == RUNNING || state == READY;
                if (state == RUNNING) {
                    g2.setColor(new Color(224, 238, 251));
                    g2.fillRect(0, y, w, ROW);
                }
                if (inMemory) {
                    g2.setColor(heat(state == RUNNING ? 1.0 : 0.0));
                    g2.fillRect(cpuX, y, cpuW, ROW);
                    g2.setColor(heat(0.1 + 0.9 * job.memory / (double) maxMem * 0.8));
                    g2.fillRect(memX, y, memW, ROW);
                }
                g2.setColor(new Color(240, 240, 240));
                g2.drawLine(0, y + ROW - 1, w, y + ROW - 1);

                int cy = y + ROW / 2;
                // badge + name
                g2.setColor(PALETTE[i % PALETTE.length]);
                g2.fillRoundRect(30, cy - 11, 22, 22, 7, 7);
                g2.setColor(Color.WHITE);
                g2.setFont(ui(Font.BOLD, 12));
                String letter = job.name.substring(0, 1).toUpperCase();
                drawCentered(g2, letter, 41, cy + (g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                g2.setColor(TEXT);
                g2.setFont(ui(Font.PLAIN, 13));
                int base = cy + (g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
                g2.drawString("Job " + job.name, 62, base);

                // status
                Color dot;
                Color textColor;
                String status;
                int weight = Font.PLAIN;
                if (state == RUNNING) {
                    dot = GREEN;
                    textColor = GREEN;
                    status = "Running";
                    weight = Font.BOLD;
                } else if (state == READY) {
                    dot = new Color(140, 140, 140);
                    textColor = MUTED;
                    status = "Ready · " + ordinal(s.pos[i]) + " in queue";
                } else if (state == WAITING) {
                    dot = AMBER;
                    textColor = BROWN;
                    status = "Not enough memory";
                } else {
                    dot = ACCENT;
                    textColor = ACCENT;
                    status = "Finished at t=" + s.doneAt[i];
                }
                g2.setColor(dot);
                g2.fill(new Ellipse2D.Double(statusX, cy - 4, 8, 8));
                g2.setColor(textColor);
                g2.setFont(ui(weight, 13));
                g2.drawString(status, statusX + 14, base);

                // cpu, memory, burst left
                g2.setFont(ui(Font.PLAIN, 13));
                g2.setColor(state == RUNNING ? TEXT : MUTED);
                drawRight(g2, state == RUNNING ? "100%" : "0%", cpuX + cpuW - 10, base);
                if (inMemory) {
                    g2.setColor(TEXT);
                    drawRight(g2, job.memory + " KB", memX + memW - 10, base);
                } else if (state == WAITING) {
                    g2.setColor(BROWN);
                    drawRight(g2, "needs " + job.memory, memX + memW - 10, base);
                } else {
                    g2.setColor(MUTED);
                    drawRight(g2, "freed " + job.memory, memX + memW - 10, base);
                }
                g2.setColor(TEXT);
                drawRight(g2, String.valueOf(s.left[i]), right, base);
                y += ROW;
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle rect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle rect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** The "Memory" card: the bar with every loaded job, then in use / available. */
    private static class MemoryCard extends JComponent {
        private Run r;
        private Snap s;

        MemoryCard() {
            setPreferredSize(new Dimension(464, 140));
        }

        void set(Run r, Snap s) {
            this.r = r;
            this.s = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            if (r != null && s != null) {
                g2.setColor(TEXT);
                g2.setFont(ui(Font.BOLD, 14));
                g2.drawString("Memory", 18, 30);
                g2.setFont(ui(Font.PLAIN, 12));
                g2.setColor(MUTED);
                drawRight(g2, s.total + " KB total", w - 18, 30);
                drawMemoryBar(g2, 18, 46, w - 36, 38, r, s, true);
                g2.setFont(ui(Font.PLAIN, 12));
                g2.setColor(MUTED);
                int y = 108;
                g2.drawString("In use", 18, y);
                g2.setFont(ui(Font.BOLD, 12));
                g2.setColor(TEXT);
                g2.drawString(s.used + " KB", 18 + 44, y);
                g2.setFont(ui(Font.BOLD, 12));
                String avail = (s.total - s.used) + " KB";
                drawRight(g2, avail, w - 18, y);
                g2.setFont(ui(Font.PLAIN, 12));
                g2.setColor(MUTED);
                drawRight(g2, "Available", w - 18 - g2.getFontMetrics(ui(Font.BOLD, 12)).stringWidth(avail) - 6, y);
            } else {
                g2.setColor(TEXT);
                g2.setFont(ui(Font.BOLD, 14));
                g2.drawString("Memory", 18, 30);
                g2.setColor(new Color(236, 236, 236));
                g2.fillRoundRect(18, 46, w - 37, 37, 12, 12);
                g2.setColor(new Color(205, 205, 205));
                g2.drawRoundRect(18, 46, w - 37, 37, 12, 12);
                g2.setFont(ui(Font.PLAIN, 12));
                g2.setColor(MUTED);
                drawCentered(g2, "Empty", w / 2, 69);
                g2.drawString("In use", 18, 108);
                g2.setFont(ui(Font.BOLD, 12));
                g2.setColor(TEXT);
                g2.drawString("0 KB", 62, 108);
            }
            g2.dispose();
        }
    }

    /** A small number card: title, big colored number, caption. */
    private static class StatCard extends JComponent {
        private final String title;
        private final Color color;
        private String value = "0";
        private String caption = "";

        StatCard(String title, Color color) {
            this.title = title;
            this.color = color;
            setPreferredSize(new Dimension(111, 140));
        }

        void set(String value, String caption) {
            this.value = value;
            this.caption = caption;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setFont(ui(Font.PLAIN, 12));
            g2.setColor(color == BLUE ? BLUE : color);
            int y = 30;
            for (String line : title.split("\n")) {
                g2.drawString(line, 14, y);
                y += 15;
            }
            g2.setFont(ui(Font.BOLD, 30));
            g2.setColor(color);
            g2.drawString(value, 14, 84);
            int size = 11;
            g2.setFont(ui(Font.PLAIN, size));
            for (String line : caption.split("\n")) {   // shrink the caption a little if a line is too wide
                while (size > 8 && g2.getFontMetrics().stringWidth(line) > w - 20) {
                    size--;
                    g2.setFont(ui(Font.PLAIN, size));
                }
            }
            g2.setColor(MUTED);
            y = 102;
            for (String line : caption.split("\n")) {
                g2.drawString(line, 14, y);
                y += 14;
            }
            g2.dispose();
        }
    }

    /** One bar per CPU turn, colored by job, with the time under every boundary. */
    private static class GanttBar extends JComponent {
        private Run r;
        private int current = -1;

        GanttBar() {
            setPreferredSize(new Dimension(300, 64));
        }

        void set(Run r, int current) {
            this.r = r;
            this.current = current;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (r == null || r.total <= 0) {
                Graphics2D g0 = (Graphics2D) g.create();
                smooth(g0);
                g0.setFont(ui(Font.PLAIN, 12));
                g0.setColor(MUTED);
                g0.drawString("No run yet.", 4, 28);
                g0.dispose();
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int left = 4;
            int w = getWidth() - 8;
            int y = 8;
            int bh = 30;
            double scale = w / (double) r.total;
            g2.setFont(ui(Font.PLAIN, 11));
            int lastLabelRight = -100;
            for (int k = 0; k < r.steps.size(); k++) {
                Step s = r.steps.get(k);
                if (s.job < 0) continue;
                int x1 = left + (int) Math.round(s.start * scale);
                int x2 = left + (int) Math.round(s.end * scale);
                g2.setColor(PALETTE[s.job % PALETTE.length]);
                g2.fillRect(x1, y, x2 - x1, bh);
                g2.setColor(Color.WHITE);
                g2.fillRect(x2 - 1, y, 1, bh);
                if (x2 - x1 >= 14) {
                    g2.setColor(TEXT);
                    g2.setFont(ui(Font.BOLD, 12));
                    drawCentered(g2, r.jobs.get(s.job).name.substring(0, 1).toUpperCase(), (x1 + x2) / 2, y + 20);
                    g2.setFont(ui(Font.PLAIN, 11));
                }
                String label = DecimalInput.format(s.start);
                int lw = g2.getFontMetrics().stringWidth(label);
                if (x1 - lw / 2 > lastLabelRight + 4) {
                    g2.setColor(MUTED);
                    g2.drawString(label, x1 - lw / 2, y + bh + 15);
                    lastLabelRight = x1 + lw / 2;
                }
                if (k == current) {
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRect(x1, y - 1, Math.max(1, x2 - x1 - 1), bh + 1);
                }
            }
            String end = String.valueOf(r.total);
            g2.setColor(MUTED);
            g2.setFont(ui(Font.PLAIN, 11));
            drawRight(g2, end, left + w + 4, y + bh + 15);
            g2.dispose();
        }
    }

    /** Small round dot used in front of a step line. */
    private static class DotIcon implements Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x + 1, y + 2, 8, 8));
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    private static Color lineColor(int kind) {
        switch (kind) {
            case L_LOAD: return BLUE;
            case L_RUN: return GREEN;
            case L_DONE: return PURPLE;
            case L_QUEUE: return AMBER;
            default: return new Color(150, 150, 160);
        }
    }

    /** One CPU turn: what the CPU took, how long it ran, what happened after, and the memory when it started. */
    private static class StepCard extends JPanel {
        private boolean selected;
        private final Color accent;

        StepCard(Run r, Step s, Runnable onClick) {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 14));
            accent = s.last ? GREEN : PALETTE[s.job % PALETTE.length];

            JLabel badge = new JLabel(s.last ? "DONE" : "STEP " + s.no);
            badge.setFont(ui(Font.BOLD, 12));
            badge.setForeground(s.last ? GREEN : ACCENT);
            JLabel title = new JLabel(s.title);
            title.setFont(ui(Font.BOLD, 13));
            title.setForeground(TEXT);
            JLabel when = new JLabel(s.last ? "t = " + s.start : "t = " + s.start + " → " + s.end);
            when.setFont(ui(Font.PLAIN, 12));
            when.setForeground(MUTED);
            JPanel head = new JPanel(new BorderLayout(12, 0));
            head.setOpaque(false);
            head.add(badge, BorderLayout.WEST);
            head.add(title, BorderLayout.CENTER);
            head.add(when, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            for (Object[] line : s.lines) {
                JLabel l = new JLabel((String) line[1], new DotIcon(lineColor((Integer) line[0])), SwingConstants.LEFT);
                l.setFont(ui(Font.PLAIN, 12));
                l.setForeground(TEXT);
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(l);
                body.add(Box.createVerticalStrut(3));
            }
            final Snap snap = s.snap;
            JComponent mem = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    smooth(g2);
                    drawMemoryBar(g2, 0, 0, getWidth(), getHeight(), r, snap, true);
                    g2.dispose();
                }
            };
            mem.setPreferredSize(new Dimension(10, 24));
            mem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            mem.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel memTitle = new JLabel(s.last ? "Memory at the end" : "Memory when this step starts");
            memTitle.setFont(ui(Font.PLAIN, 11));
            memTitle.setForeground(MUTED);
            memTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(Box.createVerticalStrut(3));
            body.add(memTitle);
            body.add(Box.createVerticalStrut(3));
            body.add(mem);
            add(body, BorderLayout.CENTER);

            if (onClick != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        onClick.run();
                    }
                });
            }
        }

        void setSelected(boolean value) {
            if (selected != value) {
                selected = value;
                repaint();
            }
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(selected ? new Color(236, 245, 254) : Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Double(0, 0, w, h, 12, 12));
            g2.setColor(accent);
            g2.fillRect(0, 0, 6, h);
            g2.setClip(oldClip);
            g2.setColor(selected ? ACCENT : LINE);
            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            g2.drawRoundRect(selected ? 1 : 0, selected ? 1 : 0, w - (selected ? 3 : 1), h - (selected ? 3 : 1), 12, 12);
            g2.dispose();
        }
    }

    /** History list entry: run number and time on top, the summary below. */
    private static class RunRenderer extends JLabel implements ListCellRenderer<Run> {
        RunRenderer() {
            setOpaque(true);
            setFont(ui(Font.PLAIN, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Run> list, Run r, int index,
                                                      boolean selected, boolean focus) {
            setText("<html><b>Run #" + r.number + "</b> &nbsp;" + r.time + "<br><font color='#606060'>"
                    + r.summary + "</font></html>");
            setForeground(TEXT);
            setBackground(selected ? new Color(224, 238, 251) : Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 1, 0, selected ? ACCENT : Color.WHITE),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(238, 238, 238)),
                            BorderFactory.createEmptyBorder(7, 8, 7, 6))));
            return this;
        }
    }
}
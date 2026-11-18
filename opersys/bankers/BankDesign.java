package opersys.bankers;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BANK DESIGN = the screen. DESIGN ONLY: layout, colors and buttons, no algorithm code.
 *
 * What you see: a navy header on top, the customer table (Allocated / Max / Need) on the left with a
 * status card under it, and the blue "bank vault" on the right where you type the Available resources
 * and press Check safety. Two extra buttons in the vault open the Steps window (how the check thought
 * it through) and the History window (your old checks).
 *
 * It never decides anything itself. It just tells the controller "the user pressed the button"
 * and shows whatever answer the controller gives back.
 */
public class BankDesign extends JPanel {

    /** The controller plugs into this, so the screen can say "the user pressed Check safety" without knowing any algorithm stuff. */
    public interface Listener {
        void onCheckRequested();
    }

    // ---- colors (navy + gold = that "bank" look) ----
    private static final Color NAVY = new Color(0x0B2E6B);
    private static final Color NAVY_DARK = new Color(0x0A1F55);
    private static final Color VAULT_TOP = new Color(0x1E56C8);
    private static final Color GOLD = new Color(0xF5B800);
    private static final Color PAGE_BG = new Color(0xEEF3FA);
    private static final Color MUTED = new Color(0x6B7A99);
    private static final Color BORDER_LIGHT = new Color(0xD5DCE8);
    private static final Color BORDER_CELL = new Color(0xB7C6DE);
    private static final Color NEED_BG = new Color(0xEDF2FA);     // Need column, even
    private static final Color NEED_ALT = new Color(0xBFD2EC);    // Need column, odd (clearly darker)
    private static final Color CELL_ALT = new Color(0xC9DCF5);    // Allocated / Max, odd (clearly blue)
    private static final Color GREEN = new Color(0x1E8E3E);
    private static final Color RED = new Color(0xC62828);
    private static final Color SOFT_TEXT = new Color(0xC9D8F5);
    private static final Color[] PROC_COLORS = {
            new Color(0x5BC0F8), new Color(0xFFB24D), new Color(0x7EE08A),
            new Color(0xC58CFF), new Color(0xFF8A9B)
    };
    // the number font used in the table cells
    private static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 15);

    // who to tell when Check safety is pressed (the controller sets this)
    private Listener listener;

    // the two number boxes above the table: how many customers (processes) and how many resource types
    private final JSpinner processSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
    private final JSpinner resourceSpinner = new JSpinner(new SpinnerNumberModel(3, 1, null, 1));

    // the typing boxes: Allocated, Max, and Available. Need is NOT typed, it is calculated, so it is just a badge (label).
    private JTextField[][] allocField = new JTextField[0][0];
    private JTextField[][] maxField = new JTextField[0][0];
    private Badge[][] needBadge = new Badge[0][0];
    private JTextField[] availField = new JTextField[0];

    // The table and the resource list are the only parts that scroll. Everything else stays fixed.
    private final JPanel tableHolder = new JPanel(new BorderLayout());
    private final JScrollPane tableScroll = styledScroll(tableHolder);
    private final JPanel resourceList = new JPanel();
    private final JScrollPane resourceScroll = styledScroll(wrapTop(resourceList));

    // the status card (bottom left): the round icon, the "Safe / Unsafe" text, and the approval order chips
    private final Badge statusIcon = new Badge("?", Color.LIGHT_GRAY, -1, Color.WHITE, 22f);
    private final JLabel statusTitle = new JLabel();
    private final JLabel statusSub = new JLabel();
    private final JPanel orderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // extras: the saved history (the controller hands it over), the steps of the last check, and the Steps button
    private BankHistory history;
    private List<BankStep> lastSteps = new ArrayList<>();
    private JButton stepsButton;

    /** Builds the whole screen: header on top, customer table + status on the left, bank vault on the right. */
    public BankDesign() {
        super(new BorderLayout());
        setBackground(PAGE_BG);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));
        body.add(buildLeft(), BorderLayout.CENTER);
        body.add(buildVault(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        // changing the number of customers or resources redraws the table (and the vault list)
        processSpinner.addChangeListener(e -> rebuildTable());
        resourceSpinner.addChangeListener(e -> {
            rebuildTable();
            rebuildResources();
        });

        // first draw: empty boxes, and the status card says "Not checked yet"
        rebuildTable();
        rebuildResources();
        resetStatus();
    }

    // =========================================================
    // Public API used by the controller
    // =========================================================

    /** The controller calls this once to say who wants to hear about the Check safety button. */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** The controller gives us the saved history, so the History button has something to show. */
    public void setHistory(BankHistory history) {
        this.history = history;
    }

    /** The raw text from the Allocated boxes (the model turns it into numbers). */
    public String[][] readAllocation() {
        return readMatrix(allocField);
    }

    /** The raw text from the Max boxes. */
    public String[][] readMax() {
        return readMatrix(maxField);
    }

    /** The raw text from the Available boxes in the vault. */
    public String[] readAvailable() {
        String[] out = new String[availField.length];
        for (int r = 0; r < availField.length; r++) out[r] = availField[r].getText();
        return out;
    }

    /** Fills the form with numbers (used when you load an old check from History). The table resizes to fit. */
    public void setData(int[][] alloc, int[][] max, int[] avail) {
        processSpinner.setValue(alloc.length);
        resourceSpinner.setValue(avail.length);

        for (int p = 0; p < alloc.length; p++)
            for (int r = 0; r < avail.length; r++) {
                allocField[p][r].setText(String.valueOf(alloc[p][r]));
                maxField[p][r].setText(String.valueOf(max[p][r]));
            }
        for (int r = 0; r < avail.length; r++)
            availField[r].setText(String.valueOf(avail[r]));

        refreshNeeds();
        resetStatus();
    }

    /** Shows the green "Safe state" card with the approval order chips, and keeps the steps for the Steps button. */
    public void showSafe(List<Integer> order, List<BankStep> steps) {
        lastSteps = new ArrayList<>(steps);
        stepsButton.setEnabled(true);
        statusIcon.setBg(GREEN);
        statusIcon.setText("\u2713");
        statusTitle.setText("Safe state");
        statusTitle.setForeground(GREEN);
        statusSub.setText("<html>All requests can be<br>approved</html>");

        orderPanel.removeAll();
        for (int i = 0; i < order.size(); i++) {
            orderPanel.add(chip(order.get(i)));
            if (i < order.size() - 1) orderPanel.add(label("\u2192", 16, NAVY));
        }
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    /** Shows the red "Unsafe state" card. The steps are still kept, so you can see where it got stuck. */
    public void showUnsafe(List<BankStep> steps) {
        lastSteps = new ArrayList<>(steps);
        stepsButton.setEnabled(true);
        statusIcon.setBg(RED);
        statusIcon.setText("\u2717");
        statusTitle.setText("Unsafe state");
        statusTitle.setForeground(RED);
        statusSub.setText("No safe order exists. Do not grant the requests.");

        orderPanel.removeAll();
        orderPanel.add(label("No safe order", 13, MUTED));
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    /** A popup for bad input (a letter instead of a number, a negative number, ...). */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check the input", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================
    // Layout building
    // =========================================================

    /** The navy bar on top: logo + "SafeBank" on the left, the subtitle on the right. */
    private JPanel buildHeader() {
        Card header = new Card(NAVY, NAVY_DARK, 0);
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 3, 0, GOLD),
                new EmptyBorder(14, 20, 14, 20)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        Badge shield = new Badge("\u2713", GOLD, 14, NAVY_DARK, 22f);
        shield.setPreferredSize(new Dimension(44, 44));
        JLabel title = new JLabel("SafeBank");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        brand.add(shield);
        brand.add(title);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Deadlock \u00B7 Banker's Algorithm");
        t1.setFont(new Font("SansSerif", Font.BOLD, 15));
        t1.setForeground(Color.WHITE);
        t1.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel t2 = label("Check if the bank can safely approve every request", 12, SOFT_TEXT);
        t2.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(t1);
        right.add(t2);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Left side: the customer table (scrolls inside its card) and the status card (fixed). */
    private JPanel buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 16));
        left.setOpaque(false);
        left.add(buildCustomerCard(), BorderLayout.CENTER);
        left.add(buildStatusCard(), BorderLayout.SOUTH);
        return left;
    }

    /** The white card with the title, the Processes / Resources spinners, and the scrolling table. */
    private JPanel buildCustomerCard() {
        Card card = new Card(Color.WHITE, null, 18);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(heading("Customer accounts", 18, NAVY));
        titles.add(Box.createVerticalStrut(4));
        titles.add(label("Each customer is a process. Enter what they hold and the most they may ask for.", 12, MUTED));

        top.add(titles, BorderLayout.WEST);
        top.add(buildSizeControls(), BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(tableScroll, BorderLayout.CENTER);
        card.add(label("Need = Max \u2212 Allocated. It is calculated for you.", 12, MUTED), BorderLayout.SOUTH);
        return card;
    }

    /** The little "Processes [5]  Resources [3]" spinners in the top right of the card. */
    private JPanel buildSizeControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        processSpinner.setPreferredSize(new Dimension(64, 28));
        resourceSpinner.setPreferredSize(new Dimension(64, 28));
        p.add(label("Processes", 12, MUTED));
        p.add(processSpinner);
        p.add(label("Resources", 12, MUTED));
        p.add(resourceSpinner);
        return p;
    }

    /**
     * Draws the Allocated / Max / Need grid again for the current number of customers and resources.
     * Anything you already typed is kept. Columns take turns being white and blue, so it's easy to follow
     * a column when the table gets big.
     */
    private void rebuildTable() {
        String[][] oldAlloc = readMatrix(allocField);
        String[][] oldMax = readMatrix(maxField);
        int p = processes();
        int r = resources();

        allocField = new JTextField[p][r];
        maxField = new JTextField[p][r];
        needBadge = new Badge[p][r];

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);

        // top row: the three group titles (Allocated / Max / Need) with a colored line under each
        String[] groups = {"Allocated", "Max", "Need (auto)"};
        Color[] groupColor = {NAVY, GOLD, new Color(0xB8C6E0)};
        for (int g = 0; g < 3; g++) {
            JLabel gl = new JLabel(groups[g], SwingConstants.CENTER);
            gl.setFont(new Font("SansSerif", Font.BOLD, 13));
            gl.setForeground(g == 2 ? MUTED : NAVY);
            gl.setBorder(new MatteBorder(0, 0, 3, 0, groupColor[g]));
            c.gridx = 1 + g * r;
            c.gridy = 0;
            c.gridwidth = r;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(gl, c);
        }

        // second row: the resource letters (A, B, C ...) under each group
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        for (int g = 0; g < 3; g++)
            for (int j = 0; j < r; j++) {
                JLabel sub = label(letter(j), 12, MUTED);
                sub.setHorizontalAlignment(SwingConstants.CENTER);
                c.gridx = 1 + g * r + j;
                c.gridy = 1;
                grid.add(sub, c);
            }

        // one row per customer: the P chip first, then its Allocated boxes, Max boxes, and the auto Need badges
        for (int i = 0; i < p; i++) {
            c.gridy = 2 + i;
            c.gridx = 0;
            grid.add(chip(i), c);

            for (int j = 0; j < r; j++) {
                int allocCol = j;          // column index across the whole grid: Allocated
                int maxCol = r + j;        // Max
                int needCol = 2 * r + j;   // Need

                allocField[i][j] = createCell(at(oldAlloc, i, j), cellBg(allocCol));
                c.gridx = 1 + j;
                grid.add(allocField[i][j], c);

                maxField[i][j] = createCell(at(oldMax, i, j), cellBg(maxCol));
                c.gridx = 1 + r + j;
                grid.add(maxField[i][j], c);

                needBadge[i][j] = new Badge("-", needBg(needCol), 12, NAVY, 15f);
                needBadge[i][j].setFont(MONO);
                needBadge[i][j].setPreferredSize(new Dimension(58, 42));
                c.gridx = 1 + 2 * r + j;
                grid.add(needBadge[i][j], c);
            }
        }

        // Grid sits at the top-left of the scroll area, so it doesn't stretch to fill the card.
        tableHolder.removeAll();
        tableHolder.add(wrapTop(grid), BorderLayout.CENTER);
        refreshNeeds();
        refit();
    }

    /** The blue "bank vault" on the right: the Available boxes, the Check safety button, and the Steps / History buttons. */
    private JPanel buildVault() {
        Card vault = new Card(VAULT_TOP, NAVY_DARK, 18);
        vault.setLayout(new BorderLayout(0, 0));
        vault.setBorder(new EmptyBorder(22, 22, 22, 22));

        // Top: fixed title
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(align(label("BANK VAULT", 12, GOLD)));
        top.add(Box.createVerticalStrut(6));
        top.add(align(heading("Available resources", 22, Color.WHITE)));
        top.add(Box.createVerticalStrut(4));
        top.add(align(label("What the bank can lend right now.", 12, SOFT_TEXT)));
        top.add(Box.createVerticalStrut(16));

        // Middle: only the resource list scrolls
        resourceList.setLayout(new BoxLayout(resourceList, BoxLayout.Y_AXIS));
        resourceList.setOpaque(false);
        resourceScroll.setPreferredSize(new Dimension(260, 200));

        // Bottom: fixed info text and Check safety button
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x3F6FD0));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(sep);
        bottom.add(Box.createVerticalStrut(14));
        JLabel info = new JLabel("<html><body style='width:230px'>The bank approves a customer only if it can still lend what that customer might need.</body></html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        info.setForeground(SOFT_TEXT);
        bottom.add(align(info));
        bottom.add(Box.createVerticalStrut(20));
        bottom.add(buildCheckButton());
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(buildExtraButtons());

        vault.add(top, BorderLayout.NORTH);
        vault.add(resourceScroll, BorderLayout.CENTER);
        vault.add(bottom, BorderLayout.SOUTH);
        return vault;
    }

    /** Rebuilds the Available resource fields for the current resource count, keeping typed values. */
    private void rebuildResources() {
        String[] old = readAvailable();
        int r = resources();

        availField = new JTextField[r];
        resourceList.removeAll();
        for (int j = 0; j < r; j++) {
            availField[j] = createVaultField(at(old, j));
            resourceList.add(resourceRow(j, availField[j]));
            resourceList.add(Box.createVerticalStrut(12));
        }
        resourceList.revalidate();
        resourceList.repaint();
        refit();
    }

    /** One line in the vault: the round letter badge, the name ("Resource A"), and the box you type in. */
    private JPanel resourceRow(int j, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        Badge badge = new Badge(letter(j), GOLD, -1, NAVY_DARK, 16f);
        badge.setPreferredSize(new Dimension(38, 38));

        row.add(badge, BorderLayout.WEST);
        row.add(label("Resource " + letter(j), 15, Color.WHITE), BorderLayout.CENTER);
        row.add(field, BorderLayout.EAST);
        return row;
    }

    /** The big gold "Check safety" button. It paints its own rounded gold background. */
    private JButton buildCheckButton() {
        JButton check = new JButton("\u2713  Check safety") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GOLD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        check.setFont(new Font("SansSerif", Font.BOLD, 15));
        check.setForeground(NAVY_DARK);
        check.setContentAreaFilled(false);
        check.setFocusPainted(false);
        check.setBorderPainted(false);
        check.setAlignmentX(Component.LEFT_ALIGNMENT);
        check.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        check.setPreferredSize(new Dimension(230, 50));
        check.addActionListener(e -> {
            if (listener != null) listener.onCheckRequested();
        });
        return check;
    }

    /** The white card under the table: status icon + text on the left, the approval order on the right. */
    private JPanel buildStatusCard() {
        Card card = new Card(Color.WHITE, null, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel left = new JPanel(new BorderLayout(14, 0));
        left.setOpaque(false);
        statusIcon.setPreferredSize(new Dimension(52, 52));
        left.add(statusIcon, BorderLayout.WEST);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        statusTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusSub.setForeground(MUTED);
        texts.add(statusTitle);
        texts.add(Box.createVerticalStrut(2));
        texts.add(statusSub);
        left.add(texts, BorderLayout.CENTER);

        JPanel divider = new JPanel();
        divider.setBackground(BORDER_LIGHT);
        divider.setPreferredSize(new Dimension(1, 56));
        divider.setMaximumSize(new Dimension(1, 56));

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(label("Approval order", 12, MUTED), BorderLayout.NORTH);
        orderPanel.setOpaque(false);
        right.add(orderPanel, BorderLayout.CENTER);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(left);
        row.add(Box.createHorizontalStrut(24));
        row.add(divider);
        row.add(Box.createHorizontalStrut(24));
        row.add(right);

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    // =========================================================
    // Steps and History (the two extra buttons in the vault)
    // =========================================================

    /** The row under Check safety with two small buttons: Steps and History. */
    private JPanel buildExtraButtons() {
        JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setPreferredSize(new Dimension(230, 38));

        stepsButton = softButton("Steps");
        stepsButton.setEnabled(false);   // nothing to show until a check has been run
        stepsButton.addActionListener(e -> showStepsWindow());

        JButton historyButton = softButton("History");
        historyButton.addActionListener(e -> showHistoryWindow());

        row.add(stepsButton);
        row.add(historyButton);
        return row;
    }

    /** A small see-through white button that looks nice on the blue vault. */
    private static JButton softButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, isEnabled() ? 45 : 20));   // dimmer when disabled
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        return b;
    }

    /** Pops up the story of the last check: round by round, who could finish and who had to wait. */
    private void showStepsWindow() {
        if (lastSteps.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        int round = 0;
        for (BankStep s : lastSteps) {
            if (s.getRound() != round) {   // a new pass over the customers starts here
                round = s.getRound();
                if (sb.length() > 0) sb.append('\n');
                sb.append("Round ").append(round).append('\n');
            }
            sb.append("  ").append(s.describe()).append('\n');
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(640, 320));
        JOptionPane.showMessageDialog(this, scroll, "Step by step", JOptionPane.PLAIN_MESSAGE);
    }

    /** Pops up the list of old checks. You can load one back into the form, or clear the list. */
    private void showHistoryWindow() {
        if (history == null || history.entries().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No checks yet. Press Check safety first.",
                    "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultListModel<String> model = new DefaultListModel<>();
        for (BankHistory.Entry e : history.entries()) model.addElement(e.summary());   // newest first
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(640, 300));

        Object[] options = {"Load into form", "Clear history", "Close"};
        int choice = JOptionPane.showOptionDialog(this, scroll, "History", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0 && list.getSelectedIndex() >= 0) {
            BankHistory.Entry e = history.entries().get(list.getSelectedIndex());
            setData(e.getAllocation(), e.getMax(), e.getAvailable());   // puts the old numbers back in the boxes
        } else if (choice == 1) {
            history.clear();
        }
    }

    // =========================================================
    // Small helpers
    // =========================================================

    // how many customers the spinner says
    private int processes() {
        return (Integer) processSpinner.getValue();
    }

    // how many resource types the spinner says
    private int resources() {
        return (Integer) resourceSpinner.getValue();
    }

    /** One typing box for the table (Allocated or Max). */
    private JTextField createCell(String text, Color bg) {
        JTextField f = new JTextField(text, 3);
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setFont(MONO);
        f.setForeground(NAVY);
        f.setBackground(bg);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_CELL, 1, true),
                new EmptyBorder(6, 4, 6, 4)));
        f.setPreferredSize(new Dimension(58, 42));
        watch(f);
        return f;
    }

    /** One typing box for the vault (Available). Same idea, but white and bigger. */
    private JTextField createVaultField(String text) {
        JTextField f = new JTextField(text, 3);
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setFont(new Font("SansSerif", Font.BOLD, 18));
        f.setForeground(NAVY);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.WHITE, 1, true),
                new EmptyBorder(8, 8, 8, 8)));
        f.setPreferredSize(new Dimension(80, 44));
        watch(f);
        return f;
    }

    /** Even columns are white, odd columns are clearly blue, so the eye can follow each column. */
    private static Color cellBg(int col) {
        return col % 2 == 1 ? CELL_ALT : Color.WHITE;
    }

    /** Need columns alternate between a light and a darker blue-gray. */
    private static Color needBg(int col) {
        return col % 2 == 1 ? NEED_ALT : NEED_BG;
    }

    /** Listens to a box: whenever the text changes, we update the Need numbers and reset the status. */
    private void watch(JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onInputEdited(); }
            public void removeUpdate(DocumentEvent e) { onInputEdited(); }
            public void changedUpdate(DocumentEvent e) { onInputEdited(); }
        });
    }

    // something was typed: the old answer is no longer true, so refresh Need and go back to "Not checked yet"
    private void onInputEdited() {
        refreshNeeds();
        resetStatus();
    }

    // recalculates every Need badge = Max - Allocated (shows "-" if a box has no valid number yet)
    private void refreshNeeds() {
        for (int i = 0; i < needBadge.length; i++)
            for (int j = 0; j < needBadge[i].length; j++)
                needBadge[i][j].setText(
                        BankModel.needText(maxField[i][j].getText(), allocField[i][j].getText()));
    }

    // puts the status card back to the gray "Not checked yet" look
    private void resetStatus() {
        statusIcon.setBg(Color.LIGHT_GRAY);
        statusIcon.setText("?");
        statusTitle.setText("Not checked yet");
        statusTitle.setForeground(MUTED);
        statusSub.setText("Press Check safety to run the algorithm");
        lastSteps = new ArrayList<>();   // the old steps don't match the new numbers anymore
        if (stepsButton != null) stepsButton.setEnabled(false);
        orderPanel.removeAll();
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    // asks Swing to lay everything out and repaint again after the table changed size
    private void refit() {
        revalidate();
        repaint();
    }

    /** The colored "P0", "P1", ... chip. Each customer keeps its own color. */
    private Badge chip(int p) {
        Badge b = new Badge("P" + p, PROC_COLORS[p % PROC_COLORS.length], 12, NAVY_DARK, 15f);
        b.setFont(MONO);
        b.setPreferredSize(new Dimension(56, 40));
        return b;
    }

    /** A scroll pane with no border, transparent background, and a small preferred size. */
    private static JScrollPane styledScroll(Component view) {
        JScrollPane s = new JScrollPane(view);
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(16);
        s.getHorizontalScrollBar().setUnitIncrement(16);
        s.setPreferredSize(new Dimension(420, 200));
        s.setMinimumSize(new Dimension(120, 80));
        return s;
    }

    /** Puts a component at the top of a transparent panel so it keeps its natural size. */
    private static JPanel wrapTop(Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    // 0 -> "A", 1 -> "B", ...
    private static String letter(int j) {
        return String.valueOf((char) ('A' + j));
    }

    // reads every box in a table into a grid of text
    private static String[][] readMatrix(JTextField[][] fields) {
        String[][] out = new String[fields.length][];
        for (int i = 0; i < fields.length; i++) {
            out[i] = new String[fields[i].length];
            for (int j = 0; j < fields[i].length; j++) out[i][j] = fields[i][j].getText();
        }
        return out;
    }

    // safe lookup: gives "" if that box does not exist yet (used when the table gets bigger)
    private static String at(String[][] m, int i, int j) {
        return (i < m.length && j < m[i].length) ? m[i][j] : "";
    }

    // same, for a single row
    private static String at(String[] v, int j) {
        return j < v.length ? v[j] : "";
    }

    // quick plain label with a size and color
    private static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    // same as label, but bold
    private static JLabel heading(String text, int size, Color color) {
        JLabel l = label(text, size, color);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        return l;
    }

    // left-aligns a label inside a vertical box
    private static JLabel align(JLabel l) {
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // =========================================================
    // Custom painted components
    // =========================================================

    /** A rounded box with a flat color or a top-to-bottom gradient. All the white and blue cards on screen are these. */
    static class Card extends JPanel {
        private final Color top, bottom;
        private final int radius;

        Card(Color top, Color bottom, int radius) {
            this.top = top;
            this.bottom = bottom;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(bottom == null ? top : new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A small colored round or rounded label: the P chips, the A / B / C letters, the Need numbers and the status icon. */
    static class Badge extends JLabel {
        private Color bg;
        private final int arc;

        Badge(String text, Color bg, int arc, Color fg, float size) {
            super(text, SwingConstants.CENTER);
            this.bg = bg;
            this.arc = arc;
            setForeground(fg);
            setFont(getFont().deriveFont(Font.BOLD, size));
            setOpaque(false);
        }

        void setBg(Color c) {
            bg = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            if (arc < 0) g2.fillOval(0, 0, getWidth(), getHeight());
            else g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
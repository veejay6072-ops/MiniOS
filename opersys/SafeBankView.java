import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/** DESIGN ONLY. A panel that shows the Banker's Algorithm UI. It has no algorithm code. */
public class SafeBankView extends JPanel {

    public interface Listener {
        void onCheckRequested();
    }

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
    private static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 15);

    private Listener listener;

    private final JSpinner processSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
    private final JSpinner resourceSpinner = new JSpinner(new SpinnerNumberModel(3, 1, null, 1));

    private JTextField[][] allocField = new JTextField[0][0];
    private JTextField[][] maxField = new JTextField[0][0];
    private Badge[][] needBadge = new Badge[0][0];
    private JTextField[] availField = new JTextField[0];

    // The table and the resource list are the only parts that scroll. Everything else stays fixed.
    private final JPanel tableHolder = new JPanel(new BorderLayout());
    private final JScrollPane tableScroll = styledScroll(tableHolder);
    private final JPanel resourceList = new JPanel();
    private final JScrollPane resourceScroll = styledScroll(wrapTop(resourceList));

    private final Badge statusIcon = new Badge("?", Color.LIGHT_GRAY, -1, Color.WHITE, 22f);
    private final JLabel statusTitle = new JLabel();
    private final JLabel statusSub = new JLabel();
    private final JPanel orderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    public SafeBankView() {
        super(new BorderLayout());
        setBackground(PAGE_BG);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));
        body.add(buildLeft(), BorderLayout.CENTER);
        body.add(buildVault(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        processSpinner.addChangeListener(e -> rebuildTable());
        resourceSpinner.addChangeListener(e -> {
            rebuildTable();
            rebuildResources();
        });

        rebuildTable();
        rebuildResources();
        resetStatus();
    }

    // =========================================================
    // Public API used by the controller
    // =========================================================

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String[][] readAllocation() {
        return readMatrix(allocField);
    }

    public String[][] readMax() {
        return readMatrix(maxField);
    }

    public String[] readAvailable() {
        String[] out = new String[availField.length];
        for (int r = 0; r < availField.length; r++) out[r] = availField[r].getText();
        return out;
    }

    /** Fills the form. Resizes the table to match the data. */
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

    public void showSafe(List<Integer> order) {
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

    public void showUnsafe() {
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

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check the input", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================
    // Layout building
    // =========================================================

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
     * Rebuilds the Allocated / Max / Need grid for the current sizes, keeping typed values.
     * Columns alternate background colors so each row is easier to follow when the table is large.
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
    // Small helpers
    // =========================================================

    private int processes() {
        return (Integer) processSpinner.getValue();
    }

    private int resources() {
        return (Integer) resourceSpinner.getValue();
    }

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

    private void watch(JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onInputEdited(); }
            public void removeUpdate(DocumentEvent e) { onInputEdited(); }
            public void changedUpdate(DocumentEvent e) { onInputEdited(); }
        });
    }

    private void onInputEdited() {
        refreshNeeds();
        resetStatus();
    }

    private void refreshNeeds() {
        for (int i = 0; i < needBadge.length; i++)
            for (int j = 0; j < needBadge[i].length; j++)
                needBadge[i][j].setText(
                        BankModel.needText(maxField[i][j].getText(), allocField[i][j].getText()));
    }

    private void resetStatus() {
        statusIcon.setBg(Color.LIGHT_GRAY);
        statusIcon.setText("?");
        statusTitle.setText("Not checked yet");
        statusTitle.setForeground(MUTED);
        statusSub.setText("Press Check safety to run the algorithm");
        orderPanel.removeAll();
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    private void refit() {
        revalidate();
        repaint();
    }

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

    private static String letter(int j) {
        return String.valueOf((char) ('A' + j));
    }

    private static String[][] readMatrix(JTextField[][] fields) {
        String[][] out = new String[fields.length][];
        for (int i = 0; i < fields.length; i++) {
            out[i] = new String[fields[i].length];
            for (int j = 0; j < fields[i].length; j++) out[i][j] = fields[i][j].getText();
        }
        return out;
    }

    private static String at(String[][] m, int i, int j) {
        return (i < m.length && j < m[i].length) ? m[i][j] : "";
    }

    private static String at(String[] v, int j) {
        return j < v.length ? v[j] : "";
    }

    private static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private static JLabel heading(String text, int size, Color color) {
        JLabel l = label(text, size, color);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        return l;
    }

    private static JLabel align(JLabel l) {
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // =========================================================
    // Custom painted components
    // =========================================================

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
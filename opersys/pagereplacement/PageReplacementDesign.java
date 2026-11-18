package opersys.pagereplacement;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.IntConsumer;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Page Replacement window. DESIGN ONLY: all the logic lives in PageReplacement.
 * It looks like a browser: a tab bar at the top (one tab per frame), a toolbar, and the
 * tab history table. The input boxes start empty. The user types the pages and frames,
 * then presses Run.
 */
public class PageReplacementDesign extends JPanel {
    private static final String FONT = "Segoe UI";
    private static final Color BLUE = new Color(31, 111, 235);
    private static final Color RED = new Color(200, 30, 30);
    private static final Color GREEN = new Color(30, 130, 60);
    private static final Color INK = new Color(30, 30, 34);
    private static final Color MUTED = new Color(120, 120, 128);
    private static final Color LINE = new Color(228, 228, 232);
    private static final Color CHROME = new Color(214, 218, 226);
    private static final Color[] PAGE_COLORS = {
            new Color(255, 178, 74), new Color(92, 190, 255), new Color(126, 222, 136),
            new Color(214, 140, 255), new Color(255, 120, 150), new Color(255, 224, 90),
            new Color(80, 214, 200), new Color(255, 140, 90)};

    private static Font font(int style, int size) {
        return new Font(FONT, style, size);
    }

    private static Color colorOf(int page) {
        return PAGE_COLORS[Math.floorMod(page, PAGE_COLORS.length)];
    }

    private static void centerText(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    // ---------- state ----------

    private PageReplacement.Result result;
    private int current = -1;
    private Runnable listener;

    // ---------- controller-friendly API ----------

    /** The controller calls this to wire up its own run handler. */
    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    /** The page numbers the user typed, as text. */
    public String getPagesText() {
        return pagesField.getText();
    }

    /** The number of frames the user typed, as text. */
    public String getFramesText() {
        return framesField.getText();
    }

    /** The algorithm the user picked. */
    public PageReplacement.Algorithm getSelectedAlgorithm() {
        return (PageReplacement.Algorithm) algoBox.getSelectedItem();
    }

    /** Show the result of a run (the controller has already validated the input). */
    public void showResult(PageReplacementModel.Result result) {
        this.result = result.getInner();
        current = this.result.steps.size() - 1;
        refresh();
    }

    /** Show a friendly error message. */
    public void showError(String message) {
        setBanner(message, true);
    }

    // ---------- parts of the window ----------

    private final JPanel tabBar = new WrapPanel(FlowLayout.LEFT, 4, 0);
    private final JLabel tabSummary = new JLabel(" ");
    private final NavButton backButton = new NavButton(0);
    private final NavButton forwardButton = new NavButton(1);
    private final NavButton resetButton = new NavButton(2);
    private final JComboBox<PageReplacement.Algorithm> algoBox =
            new JComboBox<>(PageReplacement.Algorithm.values());
    private final StyledField framesField = new StyledField(4);
    private final StyledField pagesField = new StyledField(26);
    private final JTextArea banner = new WrapArea();
    private final HistoryGrid grid = new HistoryGrid();
    private final JScrollBar gridBar = new JScrollBar(Adjustable.HORIZONTAL);
    private final StatCard faultCard = new StatCard("Page faults", "the tab was not open yet", RED);
    private final StatCard hitCard = new StatCard("Page hits", "the tab was already open", GREEN);
    private final StatCard ratioCard = new StatCard("Hit ratio", " ", BLUE);
    private JScrollPane pageScroll;

    public PageReplacementDesign() {
        super(new BorderLayout());
        setBackground(Color.WHITE);

        // browser chrome: tab bar on top, toolbar under it. Always visible, does not scroll.
        tabBar.setBackground(CHROME);
        tabBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        tabSummary.setFont(font(Font.PLAIN, 13));
        tabSummary.setForeground(new Color(70, 74, 84));
        tabSummary.setBorder(BorderFactory.createEmptyBorder(6, 12, 0, 0));

        JPanel chrome = new JPanel(new BorderLayout());
        chrome.add(tabBar, BorderLayout.NORTH);
        chrome.add(buildToolbar(), BorderLayout.CENTER);
        add(chrome, BorderLayout.NORTH);

        // one scroll bar for the whole page: the mouse wheel always works
        JScrollPane scroll = new JScrollPane(new Page(buildBody()),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        pageScroll = scroll;
        add(scroll, BorderLayout.CENTER);

        // every resize: re-measure the wrapped rows and text, so nothing overlaps
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });

        backButton.addActionListener(e -> showStep(current - 1));
        forwardButton.addActionListener(e -> showStep(current + 1));
        resetButton.addActionListener(e -> showStep(0));
        grid.onColumnClick = this::showStep;   // click a column in the table to jump to that step
        refresh();
    }

    // ---------- building the window ----------

    /** Toolbar: back / forward / reset, algorithm, frames, pages and Run. Wraps when narrow. */
    private JPanel buildToolbar() {
        JPanel bar = new WrapPanel(FlowLayout.LEFT, 8, 8);
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        bar.add(backButton);
        bar.add(forwardButton);
        bar.add(resetButton);
        styleCombo(algoBox);
        bar.add(algoBox);
        bar.add(smallLabel("Frames"));
        bar.add(framesField);
        bar.add(smallLabel("Pages"));
        bar.add(pagesField);

        RunButton run = new RunButton();
        run.addActionListener(e -> runSimulation());
        bar.add(run);
        return bar;
    }

    /** Everything under the toolbar: banner, table card and stat cards. */
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        banner.setFont(font(Font.PLAIN, 13));
        banner.setOpaque(true);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        FitPanel bannerHolder = new FitPanel(new BorderLayout());
        bannerHolder.add(banner, BorderLayout.CENTER);

        JPanel stats = new WrapPanel(FlowLayout.LEFT, 14, 14);
        stats.add(faultCard);
        stats.add(hitCard);
        stats.add(ratioCard);

        body.add(bannerHolder);
        body.add(Box.createVerticalStrut(14));
        body.add(card(buildGridCard()));
        body.add(Box.createVerticalStrut(14));
        body.add(stats);
        return body;
    }

    /** "Tab history" card: one column per page request, one row per tab. */
    private JPanel buildGridCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        head.setOpaque(false);
        JLabel title = new JLabel("Tab history");
        title.setFont(font(Font.BOLD, 17));
        title.setForeground(INK);
        JLabel sub = new JLabel("(one column per page visited)");
        sub.setFont(font(Font.PLAIN, 13));
        sub.setForeground(MUTED);
        head.add(title);
        head.add(sub);

        // the table draws itself; a thin bar underneath pans it sideways when it is wide
        JPanel gridArea = new JPanel(new BorderLayout());
        gridArea.setOpaque(false);
        gridArea.add(grid, BorderLayout.CENTER);
        gridArea.add(gridBar, BorderLayout.SOUTH);
        gridBar.setVisible(false);
        gridBar.addAdjustmentListener(e -> grid.setOffset(gridBar.getValue()));
        grid.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridBar();
            }
        });
        MouseWheelListener wheel = e -> {
            if (e.isShiftDown() && gridBar.isVisible()) {      // Shift + wheel: sideways in the table
                gridBar.setValue(gridBar.getValue() + e.getWheelRotation() * HistoryGrid.CELL_W);
            } else {                                           // plain wheel: always scrolls the page
                JScrollBar v = pageScroll.getVerticalScrollBar();
                v.setValue(v.getValue() + e.getWheelRotation() * v.getUnitIncrement());
            }
            e.consume();
        };
        grid.addMouseWheelListener(wheel);
        gridBar.addMouseWheelListener(wheel);

        p.add(head, BorderLayout.NORTH);
        p.add(gridArea, BorderLayout.CENTER);
        return p;
    }

    /** White rounded card with a light border around a panel. */
    private static JPanel card(JComponent inner) {
        FitPanel card = new FitPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(LINE, 1, true),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private static JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        return l;
    }

    private static void styleCombo(JComboBox<?> box) {
        box.setFont(font(Font.PLAIN, 14));
        box.setBackground(Color.WHITE);
        box.setFocusable(false);
        box.setPreferredSize(new Dimension(120, 34));
    }

    // ---------- what the window does (collects input, shows results) ----------

    private void runSimulation() {
        if (listener != null) listener.run();
    }

    private static int[] parsePages(String text) {
        String t = text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Enter the page numbers, separated by spaces or commas.");
        String[] parts = t.split("[\\s,]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("\"" + parts[i] + "\" is not a page number.");
            }
        }
        return out;
    }

    private static int parseFrames(String text) {
        String t = text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Enter the number of frames.");
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The number of frames must be a whole number.");
        }
    }

    private void showStep(int index) {
        if (result == null || index < 0 || index >= result.steps.size()) return;
        current = index;
        refresh();
    }

    /** Redraws everything from the current result and step. */
    private void refresh() {
        if (result == null) {
            setBanner("Enter the pages and the number of frames, then press Run.", false);
            rebuildTabs(null);
            grid.setResult(null, -1);
            faultCard.setValue("\u2013");
            hitCard.setValue("\u2013");
            ratioCard.setValue("\u2013");
            ratioCard.setSub(" ");
        } else {
            PageReplacement.Step step = result.steps.get(current);
            setBanner("Step " + (current + 1) + ": " + step.message, false);
            rebuildTabs(step);
            grid.setResult(result, current);
            faultCard.setValue(String.valueOf(result.faults));
            hitCard.setValue(String.valueOf(result.hits));
            ratioCard.setValue(String.format("%.1f%%", result.hitRatio()));
            ratioCard.setSub(result.hits + " hits out of " + result.steps.size() + " pages");
        }
        updateGridBar();
        backButton.setEnabled(result != null && current > 0);
        forwardButton.setEnabled(result != null && current < result.steps.size() - 1);
        resetButton.setEnabled(result != null);
        revalidate();
        repaint();
    }

    /** Browser tabs for the frames at this step. The tab that holds the page asked for is active. */
    private void rebuildTabs(PageReplacement.Step step) {
        tabBar.removeAll();
        if (step == null) {
            tabBar.add(new BrowserTab(-1, PageReplacement.EMPTY, false, true));   // "New tab" placeholder
            tabSummary.setText(" ");
        } else {
            int active = step.hit ? indexOf(step.slots, step.page) : step.loadedSlot;
            // one tab per frame, always in frame order, so the bar never reorders itself
            for (int s = 0; s < step.slots.length; s++) {
                if (step.slots[s] == PageReplacement.EMPTY) {
                    tabBar.add(new BrowserTab(s, PageReplacement.EMPTY, false, false));
                    continue;
                }
                boolean isNew = !step.hit && s == step.loadedSlot;
                BrowserTab tab = new BrowserTab(s, step.slots[s], isNew, s == active);
                int page = step.slots[s];
                tab.setOnClick(() -> showStep(stepOfPage(page)));   // click a tab to jump to that page
                tabBar.add(tab);
            }
            int n = result.frames;
            tabSummary.setText(n + (n == 1 ? " frame" : " frames") + " = " + n + (n == 1 ? " open tab" : " open tabs"));
        }
        tabBar.add(tabSummary);
        tabBar.revalidate();
        tabBar.repaint();
    }

    /** The latest step at or before the current one where this page was asked for. */
    private int stepOfPage(int page) {
        for (int i = current; i >= 0; i--) {
            if (result.steps.get(i).page == page) return i;
        }
        return current;
    }

    private static int indexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) return i;
        }
        return -1;
    }

    private void setBanner(String text, boolean error) {
        banner.setText(text);
        banner.setBackground(error ? new Color(255, 236, 236) : new Color(232, 240, 255));
        banner.setForeground(error ? RED : new Color(30, 80, 170));
        banner.revalidate();
    }

    /** Keeps the horizontal bar under the table in step with the table's width. */
    private void updateGridBar() {
        int total = grid.contentWidth();
        int visible = grid.getWidth();
        if (result == null || visible <= 0 || total <= visible) {
            gridBar.setVisible(false);
            grid.setOffset(0);
            return;
        }
        gridBar.setVisible(true);
        int value = Math.min(grid.getOffset(), total - visible);
        gridBar.setValues(value, visible, 0, total);
        gridBar.setBlockIncrement(visible);
        gridBar.setUnitIncrement(HistoryGrid.CELL_W);
        grid.setOffset(value);
    }

    // ---------- layout helpers ----------

    /** Panel whose maximum height follows its current preferred height, so BoxLayout never clips it. */
    private static class FitPanel extends JPanel {
        FitPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** The page inside the one scroll pane. Always as wide as the window, scrolls only up and down. */
    private static class Page extends JPanel implements Scrollable {
        Page(JComponent content) {
            super(new BorderLayout());
            setBackground(Color.WHITE);
            add(content, BorderLayout.CENTER);
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
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            Container p = getParent();
            return p instanceof JViewport && ((JViewport) p).getHeight() > getPreferredSize().height;
        }
    }

    /** Width this component really has room for: its scroll viewport if it is in one, else the window. */
    static int availableWidth(Component c) {
        JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, c);
        if (vp != null && vp.getWidth() > 0) return vp.getWidth();
        Component app = SwingUtilities.getAncestorOfClass(PageReplacementDesign.class, c);
        if (app != null && app.getWidth() > 0) return app.getWidth();
        return c.getWidth();
    }

    /** FlowLayout that moves items to the next row when they do not fit. */
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return sizeFor(target, target.getWidth());
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = sizeFor(target, target.getWidth());
            d.width = 0;
            return d;
        }

        /** The size this layout needs if the container is {@code width} pixels wide. */
        Dimension sizeFor(Container target, int width) {
            synchronized (target.getTreeLock()) {
                int avail = width > 0 ? width : Integer.MAX_VALUE;
                Insets insets = target.getInsets();
                int hgap = getHgap();
                int vgap = getVgap();
                int maxWidth = avail - insets.left - insets.right - hgap * 2;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = m.getPreferredSize();
                    if (rowWidth > 0 && rowWidth + hgap + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight, vgap);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth > 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight, vgap);
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }

        private static void addRow(Dimension dim, int rowWidth, int rowHeight, int vgap) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += vgap;
            dim.height += rowHeight;
        }
    }

    /** Panel of wrapping items. Always reports the height it needs at the current width. */
    static class WrapPanel extends JPanel {
        WrapPanel(int align, int hgap, int vgap) {
            super(new WrapLayout(align, hgap, vgap));
        }

        @Override
        public Dimension getPreferredSize() {
            int w = availableWidth(this);
            return ((WrapLayout) getLayout()).sizeFor(this, w > 0 ? w : getWidth());
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Text box that wraps to the width it has and reports the height the wrapped text needs. */
    static class WrapArea extends JTextArea {
        WrapArea() {
            setEditable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredSize() {
            int avail = availableWidth(this);
            if (avail <= 0 || getUI() == null) return super.getPreferredSize();
            Insets in = getInsets();
            int textW = avail - in.left - in.right;
            if (textW <= 0) return super.getPreferredSize();
            View root = getUI().getRootView(this);
            root.setSize(textW, Integer.MAX_VALUE);
            int h = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + in.top + in.bottom;
            return new Dimension(avail, h);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    // ---------- small pieces ----------

    /** Plain rounded text box. It starts completely empty. */
    private static class StyledField extends JTextField {
        StyledField(int columns) {
            super(columns);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(INK);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(LINE, 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            setPreferredSize(new Dimension(columns * 9 + 40, 34));
        }
    }

    /** Back / forward / reset drawn with lines, so they never depend on a font having the symbol. */
    private static class NavButton extends JButton {
        private final int kind;   // 0 = back, 1 = forward, 2 = reset

        NavButton(int kind) {
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(32, 32));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (isEnabled() && getModel().isRollover()) {
                g2.setColor(new Color(0, 0, 0, 22));
                g2.fillOval(0, 0, w - 1, h - 1);
            }
            g2.setColor(isEnabled() ? INK : new Color(190, 190, 196));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cx = w / 2.0;
            double cy = h / 2.0;
            if (kind == 0 || kind == 1) {
                double dir = kind == 0 ? -1 : 1;   // back points left, forward points right
                double tipX = cx + dir * 7;
                double tailX = cx - dir * 7;
                g2.draw(new Line2D.Double(tailX, cy, tipX, cy));
                g2.draw(new Line2D.Double(tipX, cy, tipX - dir * 5, cy - 5));
                g2.draw(new Line2D.Double(tipX, cy, tipX - dir * 5, cy + 5));
            } else {
                g2.draw(new Arc2D.Double(cx - 6, cy - 6, 12, 12, 30, 290, Arc2D.OPEN));
                double tx = cx + 5.2;
                double ty = cy - 3;
                g2.draw(new Line2D.Double(tx, ty, tx - 4, ty));
                g2.draw(new Line2D.Double(tx, ty, tx, ty + 4));
            }
            g2.dispose();
        }
    }

    /** Blue "Run" button with a play triangle. */
    private static class RunButton extends JButton {
        RunButton() {
            super("Run");
            setFont(font(Font.BOLD, 14));
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 34, 0, 26));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(108, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color c = getModel().isPressed() ? new Color(20, 80, 190)
                    : getModel().isRollover() ? new Color(60, 135, 245) : BLUE;
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
            g2.setColor(Color.WHITE);
            int cy = h / 2;
            g2.fillPolygon(new int[]{20, 20, 28}, new int[]{cy - 5, cy + 5, cy}, 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * One browser tab for a frame: colored badge with the tab number, "Page n", a NEW badge for a
     * page just loaded, and a close mark. The active tab is white and sits on top of the bar.
     * A tab with page == EMPTY is the "New tab" placeholder shown before the first Run.
     */
    private static class BrowserTab extends JComponent {
        private final int slot;
        private final int page;
        private final boolean isNew;
        private final boolean active;
        private Runnable onClick;

        BrowserTab(int slot, int page, boolean isNew, boolean active) {
            this.slot = slot;
            this.page = page;
            this.isNew = isNew;
            this.active = active;
            setPreferredSize(new Dimension(page == PageReplacement.EMPTY ? 130 : (isNew ? 170 : 150), 34));
            setCursor(Cursor.getPredefinedCursor(page == PageReplacement.EMPTY ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null && page != PageReplacement.EMPTY) onClick.run();
                }
            });
        }

        void setOnClick(Runnable r) {
            onClick = r;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // rounded top, extended down so it joins the toolbar below
            g2.setColor(active ? Color.WHITE : new Color(236, 238, 242));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h + 12, 14, 14));

            if (page == PageReplacement.EMPTY) {
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("New tab", 14, (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
                return;
            }

            int d = 20;
            g2.setColor(colorOf(page));
            g2.fillOval(10, (h - d) / 2, d, d);
            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.BOLD, 10));
            centerText(g2, String.valueOf(slot), 10 + d / 2, h / 2);

            g2.setColor(INK);
            g2.setFont(font(active ? Font.BOLD : Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Page " + page, 36, (h + fm.getAscent() - fm.getDescent()) / 2);

            if (isNew) {
                int bw = 40;
                int bh = 18;
                int bx = w - bw - 10;
                int by = (h - bh) / 2;
                g2.setColor(BLUE);
                g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh));
                g2.setColor(Color.WHITE);
                g2.setFont(font(Font.BOLD, 10));
                centerText(g2, "NEW", bx + bw / 2, h / 2);
            } else {
                int cx = w - 14;
                int cy = h / 2;
                g2.setColor(MUTED);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(cx - 4, cy - 4, cx + 4, cy + 4));
                g2.draw(new Line2D.Double(cx + 4, cy - 4, cx - 4, cy + 4));
            }
            g2.dispose();
        }
    }

    /** Big number card: title, large value and a small line underneath. */
    private static class StatCard extends JPanel {
        private final JLabel value = new JLabel("\u2013");
        private final JLabel sub = new JLabel(" ");

        StatCard(String title, String subtitle, Color valueColor) {
            super(new GridLayout(3, 1, 0, 2));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(LINE, 1, true),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));

            JLabel t = new JLabel(title);
            t.setFont(font(Font.BOLD, 13));
            t.setForeground(INK);
            value.setFont(font(Font.BOLD, 34));
            value.setForeground(valueColor);
            sub.setFont(font(Font.PLAIN, 12));
            sub.setForeground(MUTED);
            sub.setText(subtitle);

            add(t);
            add(value);
            add(sub);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(230, d.width), d.height);
        }

        void setValue(String v) {
            value.setText(v);
        }

        void setSub(String s) {
            sub.setText(s);
        }
    }

    /**
     * The table: row 1 is the page asked for, one row per tab, and the last row is Hit or Fault.
     * Each column is one page request. The tab that holds the asked page gets a blue outline.
     * It is painted at an offset, so it can be panned with the bar under it instead of having
     * its own scroll pane (which is what made the mouse wheel stop working).
     */
    private static class HistoryGrid extends JComponent {
        static final int LABEL_W = 112;
        static final int CELL_W = 62;
        static final int ROW_H = 44;
        static final int PAD = 12;

        private PageReplacement.Result result;
        private int current = -1;
        private int offset = 0;
        IntConsumer onColumnClick = i -> { };   // set by the window: jump to this step

        HistoryGrid() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (result == null) return;
                    int x = e.getX() + offset - PAD - LABEL_W;
                    if (x < 0) return;
                    int col = x / CELL_W;
                    if (col < result.steps.size()) onColumnClick.accept(col);
                }
            });
        }

        void setResult(PageReplacement.Result r, int cur) {
            result = r;
            current = cur;
            repaint();
        }

        int getOffset() {
            return offset;
        }

        void setOffset(int o) {
            if (o == offset) return;
            offset = o;
            repaint();
        }

        int contentWidth() {
            return result == null ? 0 : LABEL_W + result.steps.size() * CELL_W + PAD * 2;
        }

        private int contentHeight() {
            return result == null ? 60 : (result.frames + 2) * ROW_H + PAD * 2;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = result == null ? 420 : Math.min(contentWidth(), 520);
            return new Dimension(w, contentHeight());
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, contentHeight());
        }

        private static int colX(int c) {
            return PAD + LABEL_W + c * CELL_W;
        }

        private static int rowY(int r) {
            return PAD + r * ROW_H;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (result == null) {
                g2.setFont(font(Font.PLAIN, 14));
                g2.setColor(MUTED);
                centerText(g2, "Run a simulation to see the tab history here.", getWidth() / 2, getHeight() / 2);
                g2.dispose();
                return;
            }

            g2.translate(-offset, 0);
            int frames = result.frames;
            int rows = frames + 2;
            int cols = result.steps.size();

            if (current >= 0) {   // faint band behind the column being shown
                g2.setColor(new Color(31, 111, 235, 22));
                g2.fill(new RoundRectangle2D.Double(colX(current), PAD - 4, CELL_W, rows * ROW_H + 8, 14, 14));
            }

            g2.setFont(font(Font.PLAIN, 13));
            g2.setColor(new Color(70, 70, 78));
            rowLabel(g2, "Page asked", 0);
            for (int s = 0; s < frames; s++) rowLabel(g2, "Tab " + (s + 1), 1 + s);
            rowLabel(g2, "Result", frames + 1);

            for (int c = 0; c < cols; c++) {
                PageReplacement.Step st = result.steps.get(c);
                int x = colX(c);
                cell(g2, x, rowY(0), colorOf(st.page), String.valueOf(st.page), false);
                for (int s = 0; s < frames; s++) {
                    int v = st.slots[s];
                    if (v == PageReplacement.EMPTY) {
                        blankCell(g2, x, rowY(1 + s));
                    } else {
                        cell(g2, x, rowY(1 + s), colorOf(v), String.valueOf(v), v == st.page);
                    }
                }
                resultPill(g2, x, rowY(frames + 1), st.hit);
            }
            g2.dispose();
        }

        private void rowLabel(Graphics2D g2, String text, int row) {
            FontMetrics fm = g2.getFontMetrics();
            int cy = rowY(row) + ROW_H / 2;
            g2.drawString(text, PAD, cy + (fm.getAscent() - fm.getDescent()) / 2);
        }

        private static void cell(Graphics2D g2, int x, int y, Color fill, String text, boolean outline) {
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(x + 5, y + 5, CELL_W - 10, ROW_H - 10, 10, 10));
            if (outline) {
                g2.setColor(BLUE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new RoundRectangle2D.Double(x + 6.25, y + 6.25, CELL_W - 12.5, ROW_H - 12.5, 9, 9));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.setFont(font(Font.BOLD, 15));
            g2.setColor(INK);
            centerText(g2, text, x + CELL_W / 2, y + ROW_H / 2);
        }

        private static void blankCell(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(242, 242, 245));
            g2.fill(new RoundRectangle2D.Double(x + 5, y + 5, CELL_W - 10, ROW_H - 10, 10, 10));
            g2.setFont(font(Font.BOLD, 15));
            g2.setColor(MUTED);
            centerText(g2, "\u2013", x + CELL_W / 2, y + ROW_H / 2);
        }

        private static void resultPill(Graphics2D g2, int x, int y, boolean hit) {
            int pw = CELL_W - 16;
            int ph = ROW_H - 24;
            g2.setColor(hit ? new Color(226, 248, 234) : new Color(255, 228, 228));
            g2.fill(new RoundRectangle2D.Double(x + 8, y + 12, pw, ph, ph, ph));
            g2.setFont(font(Font.BOLD, 12));
            g2.setColor(hit ? GREEN : RED);
            centerText(g2, hit ? "Hit" : "Fault", x + CELL_W / 2, y + ROW_H / 2);
        }
    }
}
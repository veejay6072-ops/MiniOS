package opersys.paging;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** DESIGN ONLY. The PageStay paging screen. It shows what the controller gives it and has no paging rules. */
public class PagingView extends JPanel {

    public interface Listener {
        void onRun();
        void onFind();
    }

    static final Color ACCENT = new Color(0xE0245E);
    static final Color ACCENT_SOFT = new Color(0xFDE9F0);
    static final Color PAGE_BG = new Color(0xF5F5F8);
    static final Color BORDER = new Color(0xE3E3EA);
    static final Color INK = new Color(0x1E1E26);
    static final Color MUTED = new Color(0x80808C);
    static final Color TAKEN_BG = new Color(0xEAEAEE);
    static final Color TAKEN_INK = new Color(0x9A9AA5);
    static final Color FREE_LINE = new Color(0xC4C4CE);
    static final Color FRAME_BG = new Color(0xF4F4F7);
    static final Color GREEN = new Color(0x7EE08A);
    private static final Color[] PAGE_COLORS = {
            new Color(0x64BEFB), new Color(0xFFB24D), new Color(0x7EE08A),
            new Color(0xC58CFF), new Color(0xFF8A9B)
    };
    private static final String FONT = "Segoe UI";
    private static final Stroke DASH = new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10f, new float[]{4f, 3f}, 0f);

    private Listener listener;
    private PagingModel.Layout layout;
    private PagingModel.Translation translation;

    private final Field processField = new Field("e.g. 18", 6, 15);
    private final Field pageField = new Field("e.g. 4", 6, 15);
    private final Field framesField = new Field("e.g. 10", 6, 15);
    private final Field takenField = new Field("e.g. 1, 4, 8", 9, 15);
    private final Field addressField = new Field("e.g. 9", 6, 22);
    private final RunButton runButton = new RunButton();

    // Only these two holders scroll: the rooms list and the page table list.
    private final JPanel roomsHolder = transparent(new BorderLayout());
    private final JPanel reservationsHolder = transparent(new BorderLayout());
    private final StatCard pagesCard = new StatCard("Pages needed");
    private final StatCard wastedCard = new StatCard("Wasted space");
    private final ValueBox addressBox = new ValueBox("Address", addressField);
    private final ValueBox pageOffsetBox = new ValueBox("Page · Offset", null);
    private final ValueBox frameBox = new ValueBox("Frame", null);
    private final ValueBox realBox = new ValueBox("Real address", null);
    private final JLabel explain = label("", 12, MUTED);
    private final JPanel errorBanner = new JPanel(new BorderLayout());
    private final JLabel errorText = label("", 13, ACCENT);

    public PagingView() {
        super(new BorderLayout());
        setBackground(PAGE_BG);

        // Header (input bar and error banner) stays fixed at the top. It wraps instead of cropping.
        add(buildHeader(), BorderLayout.NORTH);

        // Left: rooms and find-address cards in two equal rows, so neither gets squeezed.
        JPanel left = transparent(new GridLayout(2, 1, 0, 16));
        left.add(buildRoomsCard());
        left.add(buildFindCard());

        // Right: reservations (scrolls) in the middle, stat cards (fixed) at the bottom.
        JPanel right = transparent(new BorderLayout(0, 16));
        right.setPreferredSize(new Dimension(330, 0));
        right.add(buildReservationsCard(), BorderLayout.CENTER);
        right.add(buildStatsRow(), BorderLayout.SOUTH);

        JPanel body = transparent(new BorderLayout(16, 0));
        body.setBorder(pad(4, 20, 20, 20));
        body.add(left, BorderLayout.CENTER);
        body.add(right, BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        processField.addActionListener(e -> fireRun());
        pageField.addActionListener(e -> fireRun());
        framesField.addActionListener(e -> fireRun());
        takenField.addActionListener(e -> fireRun());
        runButton.addActionListener(e -> fireRun());
        addressField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireFind();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireFind();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireFind();
            }
        });

        renderBoard();
        renderTranslation();
    }

    // =========================================================
    // Public API used by the controller
    // =========================================================

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String getProcessText() {
        return processField.getText();
    }

    public String getPageText() {
        return pageField.getText();
    }

    public String getFramesText() {
        return framesField.getText();
    }

    public String getTakenText() {
        return takenField.getText();
    }

    public String getAddressText() {
        return addressField.getText();
    }

    /** Shows a new memory layout and clears any previous translation and error. */
    public void showLayout(PagingModel.Layout l) {
        layout = l;
        translation = null;
        clearError();
        renderBoard();
        renderTranslation();
    }

    /** Shows a translation and clears any previous error. */
    public void showTranslation(PagingModel.Translation t) {
        translation = t;
        clearError();
        renderBoard();
        renderTranslation();
    }

    public void clearTranslation() {
        translation = null;
        renderBoard();
        renderTranslation();
    }

    /** Shows the error as a red banner under the header. It stays until the next valid result. */
    public void showError(String message) {
        errorText.setText(message);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearError() {
        errorText.setText("");
        errorBanner.setVisible(false);
        revalidate();
        repaint();
    }

    private void fireRun() {
        if (listener != null) listener.onRun();
    }

    private void fireFind() {
        if (listener != null) listener.onFind();
    }

    // =========================================================
    // Header: logo row, input bar (wraps), error banner
    // =========================================================

    private JPanel buildHeader() {
        JPanel logoRow = transparent(new BorderLayout(16, 0));
        logoRow.add(new Logo(), BorderLayout.WEST);
        logoRow.add(label("Paging", 13, MUTED), BorderLayout.EAST);

        JPanel top = transparent(new BorderLayout(0, 10));
        top.setBorder(pad(14, 20, 6, 20));
        top.add(logoRow, BorderLayout.NORTH);
        top.add(buildPill(), BorderLayout.CENTER);

        Card box = new Card(ACCENT_SOFT, 14, new BorderLayout());
        box.setStyle(ACCENT_SOFT, ACCENT);
        box.setBorder(pad(8, 14, 8, 14));
        box.add(errorText, BorderLayout.CENTER);

        errorBanner.setOpaque(false);
        errorBanner.setBorder(pad(0, 20, 6, 20));
        errorBanner.add(box, BorderLayout.CENTER);
        errorBanner.setVisible(false);

        JPanel wrap = transparent(new BorderLayout());
        wrap.add(top, BorderLayout.NORTH);
        wrap.add(errorBanner, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildPill() {
        Card pill = new Card(Color.WHITE, 40, new WrapLayout(FlowLayout.LEFT, 14, 8));
        pill.setBorder(pad(4, 10, 4, 10));
        pill.add(group("Process size (KB)", processField));
        pill.add(divider());
        pill.add(group("Page size (KB)", pageField));
        pill.add(divider());
        pill.add(group("Frames", framesField));
        pill.add(divider());
        pill.add(group("Taken frames", takenField));
        pill.add(runButton);
        return pill;
    }

    // =========================================================
    // Rooms card (memory frames): title fixed, grid scrolls
    // =========================================================

    private JPanel buildRoomsCard() {
        Card card = new Card(Color.WHITE, 18, new BorderLayout(0, 14));
        card.setBorder(pad(18, 18, 18, 18));

        // Title and legend share one wrapping row, so they never overlap when the window is narrow.
        JPanel head = transparent(new WrapLayout(FlowLayout.LEFT, 12, 6));
        head.add(sectionTitle("Rooms in the hotel", "(memory frames)"));
        head.add(legendItem("Taken", TAKEN_BG, false));
        head.add(legendItem("Free", Color.WHITE, true));

        card.add(head, BorderLayout.NORTH);
        card.add(innerScroll(roomsHolder), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRoomGrid() {
        int frames = layout.getFrameCount();
        int cols = Math.min(5, Math.max(1, frames));
        JPanel grid = transparent(new GridLayout(0, cols, 12, 14));
        for (int f = 0; f < frames; f++) grid.add(roomCell(f));
        return grid;
    }

    private JPanel roomCell(int f) {
        int page = layout.pageInFrame(f);
        boolean hl = okTranslation() && translation.getFrame() == f;
        Tile tile = tile(100, 62);
        String caption;
        if (layout.isTaken(f)) {
            tile.set(Tile.Kind.TAKEN, "Taken", null, false);
            caption = layout.getPageSize() + " KB";
        } else if (page >= 0) {
            tile.set(Tile.Kind.PAGE, "Page " + page, pageColor(page), hl);
            caption = usedText(page);
        } else {
            tile.set(Tile.Kind.FREE, "Free", null, false);
            caption = layout.getPageSize() + " KB";
        }

        JPanel info = transparent(new GridLayout(2, 1, 0, 0));
        JLabel name = label("Frame " + f, 13, INK);
        name.setFont(new Font(FONT, Font.BOLD, 13));
        info.add(name);
        info.add(label(caption, 11, MUTED));

        JPanel cell = transparent(new BorderLayout(0, 6));
        cell.add(tile, BorderLayout.CENTER);
        cell.add(info, BorderLayout.SOUTH);
        return cell;
    }

    private String usedText(int page) {
        int used = layout.usedInPage(page);
        return used < layout.getPageSize()
                ? used + " of " + layout.getPageSize() + " KB used"
                : layout.getPageSize() + " KB";
    }

    // =========================================================
    // Reservations card (page table): title fixed, list scrolls
    // =========================================================

    private JPanel buildReservationsCard() {
        Card card = new Card(Color.WHITE, 18, new BorderLayout(0, 12));
        card.setBorder(pad(18, 18, 18, 18));

        JPanel head = transparent(new GridLayout(2, 1, 0, 2));
        head.add(sectionTitle("Reservations", "(page table)"));
        head.add(label("Which room holds each page.", 12, MUTED));

        JPanel top = transparent(new BorderLayout(0, 10));
        top.add(head, BorderLayout.NORTH);
        top.add(threeCol(label("Page", 11, MUTED), new JLabel(), label("Frame", 11, MUTED)), BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(innerScroll(reservationsHolder), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildReservationList() {
        JPanel list = transparent(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int p = 0; p < layout.getPageCount(); p++) {
            int f = layout.frameOfPage(p);
            boolean hlPage = okTranslation() && translation.getPage() == p;
            boolean hlFrame = okTranslation() && translation.getFrame() == f;

            Tile pageTile = tile(100, 44);
            pageTile.set(Tile.Kind.PAGE, "Page " + p, pageColor(p), hlPage);
            Tile frameTile = tile(100, 44);
            frameTile.set(Tile.Kind.FRAME, "Frame " + f, null, hlFrame);

            JPanel row = threeCol(pageTile, arrow(), frameTile);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(row);
            list.add(Box.createVerticalStrut(10));
        }
        return list;
    }

    // =========================================================
    // Stats (right, bottom, fixed)
    // =========================================================

    private JPanel buildStatsRow() {
        JPanel row = transparent(new GridLayout(1, 2, 12, 0));
        row.add(pagesCard);
        row.add(wastedCard);
        return row;
    }

    // =========================================================
    // Find the real address (fixed). The chain wraps to a second row when narrow.
    // =========================================================

    private JPanel buildFindCard() {
        Card card = new Card(Color.WHITE, 18, new BorderLayout(0, 12));
        card.setBorder(pad(18, 18, 18, 18));

        JPanel head = transparent(new GridLayout(2, 1, 0, 2));
        head.add(sectionTitle("Find the real address", ""));
        head.add(label("Type an address from the process and see where it really is.", 12, MUTED));

        JPanel chain = transparent(new WrapLayout(FlowLayout.LEFT, 6, 10));
        ValueBox[] boxes = {addressBox, pageOffsetBox, frameBox, realBox};
        for (int i = 0; i < boxes.length; i++) {
            chain.add(boxes[i]);
            if (i < boxes.length - 1) chain.add(arrow());
        }

        explain.setBorder(pad(0, 2, 0, 0));
        card.add(head, BorderLayout.NORTH);
        card.add(chain, BorderLayout.CENTER);
        card.add(explain, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================
    // Rendering
    // =========================================================

    /** Rebuilds the rooms, the page table and the stats from the current layout. */
    private void renderBoard() {
        roomsHolder.removeAll();
        reservationsHolder.removeAll();
        if (layout == null) {
            roomsHolder.add(emptyNote("Enter the process size, page size and frames, then press Run."),
                    BorderLayout.NORTH);
            reservationsHolder.add(emptyNote("The page table appears after Run."), BorderLayout.NORTH);
            pagesCard.set("-", "Press Run to see this");
            wastedCard.set("-", "");
        } else {
            roomsHolder.add(buildRoomGrid(), BorderLayout.NORTH);
            reservationsHolder.add(buildReservationList(), BorderLayout.NORTH);
            pagesCard.set(String.valueOf(layout.getPageCount()),
                    layout.getProcessSize() + " KB ÷ " + layout.getPageSize() + " KB, rounded up");
            wastedCard.set(layout.getWastedSpace() + " KB", "unused in the last page");
        }
        roomsHolder.revalidate();
        roomsHolder.repaint();
        reservationsHolder.revalidate();
        reservationsHolder.repaint();
    }

    /** Fills the four boxes of the address chain and the explanation line. */
    private void renderTranslation() {
        if (layout == null) {
            setChainEmpty();
            setExplain("", MUTED);
            return;
        }
        if (translation == null) {
            setChainEmpty();
            setExplain("Type an address to see where it really is.", MUTED);
            return;
        }
        if (translation.isTrap()) {
            setChainEmpty();
            setExplain("TRAP: address " + translation.getAddress() + " is outside the process (0 to "
                    + (layout.getProcessSize() - 1) + ").", ACCENT);
            return;
        }

        int a = translation.getAddress();
        int p = translation.getPage();
        int o = translation.getOffset();
        int f = translation.getFrame();
        int ps = layout.getPageSize();
        pageOffsetBox.setValue(p + " · " + o);
        frameBox.setValue(String.valueOf(f));
        frameBox.setStyle(GREEN, null);
        realBox.setValue(String.valueOf(translation.getRealAddress()));
        realBox.setStyle(ACCENT_SOFT, ACCENT);
        setExplain("Address " + a + " ÷ " + ps + " = page " + p + ", offset " + a + " mod " + ps + " = " + o
                + " · Real address = " + f + " × " + ps + " + " + o + " = " + translation.getRealAddress(), MUTED);
    }

    /** Shows the explanation. It is wrapped in HTML so long text breaks into lines instead of cropping. */
    private void setExplain(String text, Color color) {
        explain.setForeground(color);
        explain.setText(text.isEmpty() ? "" : "<html><body style='width:100%'>" + text + "</body></html>");
    }

    private void setChainEmpty() {
        pageOffsetBox.setValue("-");
        frameBox.setValue("-");
        realBox.setValue("-");
        frameBox.setStyle(FRAME_BG, BORDER);
        realBox.setStyle(FRAME_BG, BORDER);
    }

    private boolean okTranslation() {
        return translation != null && !translation.isTrap();
    }

    // =========================================================
    // Small helpers
    // =========================================================

    /** A scroll pane for one list only. It has no border and a transparent background. */
    private static JScrollPane innerScroll(JComponent view) {
        JScrollPane s = new JScrollPane(view);
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(16);
        s.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setMinimumSize(new Dimension(120, 100));
        return s;
    }

    private static Color pageColor(int page) {
        return PAGE_COLORS[page % PAGE_COLORS.length];
    }

    private static Tile tile(int w, int h) {
        Tile t = new Tile();
        t.setPreferredSize(new Dimension(w, h));
        return t;
    }

    private static JPanel group(String title, JComponent field) {
        JPanel p = transparent(new BorderLayout(0, 2));
        p.add(label(title, 11, MUTED), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JSeparator divider() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 36));
        return s;
    }

    private static JLabel arrow() {
        JLabel l = label("\u2192", 18, ACCENT);
        l.setFont(new Font(FONT, Font.BOLD, 18));
        return l;
    }

    private static JPanel threeCol(JComponent left, JComponent mid, JComponent right) {
        JPanel p = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(left, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 12, 0, 12);
        p.add(mid, c);
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(right, c);
        return p;
    }

    private static JPanel legendItem(String text, Color fill, boolean dashed) {
        JComponent swatch = new JComponent() {
            {
                setPreferredSize(new Dimension(14, 14));
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                smooth(g);
                RoundRectangle2D r = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);
                g.setColor(fill);
                g.fill(r);
                g.setColor(FREE_LINE);
                if (dashed) g.setStroke(DASH);
                g.draw(r);
                g.dispose();
            }
        };
        JPanel p = transparent(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.add(swatch);
        p.add(label(text, 12, MUTED));
        return p;
    }

    /** Title with the optional muted note. Left edge is flush so it lines up with the subtitle below it. */
    private static JPanel sectionTitle(String main, String sub) {
        JPanel p = transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel a = label(main, 18, INK);
        a.setFont(new Font(FONT, Font.BOLD, 18));
        p.add(a);
        if (!sub.isEmpty()) {
            JLabel s = label(sub, 13, MUTED);
            s.setBorder(pad(0, 6, 0, 0));
            p.add(s);
        }
        return p;
    }

    private static JLabel emptyNote(String text) {
        JLabel l = label(text, 13, MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(pad(40, 10, 10, 10));
        return l;
    }

    static JPanel transparent(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    private static Border pad(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // =========================================================
    // Custom painted components
    // =========================================================

    /**
     * A FlowLayout that wraps to a new row when the container is too narrow.
     * This is what stops things from cropping or overlapping when the window is small.
     */
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target);
            d.width = 0;
            return d;
        }

        private Dimension layoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                int maxWidth = target.getWidth();
                if (maxWidth <= 0) maxWidth = Integer.MAX_VALUE;
                Insets in = target.getInsets();
                maxWidth -= in.left + in.right + getHgap() * 2;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = m.getPreferredSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth > 0) rowWidth += getHgap();
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += in.left + in.right + getHgap() * 2;
                dim.height += in.top + in.bottom + getVgap() * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }

    /** White rounded panel. */
    static class Card extends JPanel {
        private Color fill;
        private Color stroke = BORDER;
        private final int radius;

        Card(Color fill, int radius, LayoutManager lm) {
            super(lm);
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        void setStyle(Color fill, Color stroke) {
            this.fill = fill;
            this.stroke = stroke;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                g.setColor(stroke);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** One box in the address chain: title on top, then a value or an input field. */
    static class ValueBox extends Card {
        private final JLabel value;

        ValueBox(String title, JComponent input) {
            super(FRAME_BG, 14, new BorderLayout(0, 4));
            setBorder(pad(10, 14, 10, 14));
            value = input == null ? valueLabel() : null;
            add(label(title, 11, MUTED), BorderLayout.NORTH);
            add(input != null ? input : value, BorderLayout.CENTER);
        }

        void setValue(String text) {
            if (value != null) value.setText(text);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(130, super.getMinimumSize().height);
        }

        private static JLabel valueLabel() {
            JLabel l = new JLabel("-");
            l.setFont(new Font(FONT, Font.BOLD, 22));
            l.setForeground(INK);
            return l;
        }
    }

    /** Big number card for "Pages needed" and "Wasted space". The note wraps inside the card. */
    static class StatCard extends Card {
        private final JLabel value = new JLabel("-");
        private final JLabel note = label(" ", 11, MUTED);

        StatCard(String title) {
            super(Color.WHITE, 16, null);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(pad(14, 16, 14, 16));
            JLabel t = label(title, 13, INK);
            t.setFont(new Font(FONT, Font.BOLD, 13));
            t.setAlignmentX(LEFT_ALIGNMENT);
            value.setFont(new Font(FONT, Font.BOLD, 30));
            value.setForeground(ACCENT);
            value.setAlignmentX(LEFT_ALIGNMENT);
            note.setAlignmentX(LEFT_ALIGNMENT);
            add(t);
            add(Box.createVerticalStrut(6));
            add(value);
            add(note);
        }

        void set(String v, String n) {
            value.setText(v);
            note.setText(n.isEmpty() ? " " : "<html><body style='width:150px'>" + n + "</body></html>");
        }
    }

    /** One room or one page-table slot: a rounded box that can be a page, a frame slot, taken or free. */
    static class Tile extends JComponent {
        enum Kind { PAGE, FRAME, TAKEN, FREE }

        private Kind kind = Kind.FREE;
        private String text = "";
        private Color color = Color.WHITE;
        private boolean highlight;

        void set(Kind kind, String text, Color color, boolean highlight) {
            this.kind = kind;
            this.text = text;
            this.color = color;
            this.highlight = highlight;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            RoundRectangle2D box = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            switch (kind) {
                case PAGE:
                    g.setColor(color);
                    g.fill(box);
                    break;
                case FRAME:
                    g.setColor(FRAME_BG);
                    g.fill(box);
                    g.setColor(BORDER);
                    g.draw(box);
                    break;
                case TAKEN:
                    g.setColor(TAKEN_BG);
                    g.fill(box);
                    break;
                default:
                    g.setColor(Color.WHITE);
                    g.fill(box);
                    g.setColor(FREE_LINE);
                    g.setStroke(DASH);
                    g.draw(box);
            }
            if (highlight) {
                g.setColor(ACCENT);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, 16, 16));
            }
            g.setFont(new Font(FONT, Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(kind == Kind.TAKEN ? TAKEN_INK : kind == Kind.FREE ? MUTED : INK);
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Text field with a grey hint shown while empty. */
    static class Field extends JTextField {
        private final String hint;

        Field(String hint, int cols, int size) {
            super(cols);
            this.hint = hint;
            setOpaque(false);
            setFont(new Font(FONT, Font.BOLD, size));
            setForeground(INK);
            setCaretColor(ACCENT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(2, 2, 4, 2)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setFont(getFont().deriveFont(Font.PLAIN));
            g2.setColor(new Color(0xB0B0BA));
            FontMetrics fm = g2.getFontMetrics();
            Insets in = getInsets();
            g2.drawString(hint, in.left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Pink rounded "Run" button with a play triangle. */
    static class RunButton extends JButton {
        RunButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(116, 46));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(getModel().isRollover() ? new Color(0xC91C51) : ACCENT);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
            Path2D tri = new Path2D.Double();
            tri.moveTo(38, h / 2.0 - 8);
            tri.lineTo(38, h / 2.0 + 8);
            tri.lineTo(51, h / 2.0);
            tri.closePath();
            g.setColor(Color.WHITE);
            g.fill(tri);
            g.setFont(new Font(FONT, Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("Run", 60, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** The PageStay logo: pink rounded square with a house, then the name. */
    static class Logo extends JComponent {
        Logo() {
            setPreferredSize(new Dimension(200, 46));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            g.setColor(ACCENT);
            g.fill(new RoundRectangle2D.Double(0, 5, 38, 38, 12, 12));
            g.setColor(Color.WHITE);
            Path2D roof = new Path2D.Double();
            roof.moveTo(19, 12);
            roof.lineTo(31, 22);
            roof.lineTo(7, 22);
            roof.closePath();
            g.fill(roof);
            g.fill(new Rectangle2D.Double(11, 22, 16, 11));
            g.setColor(ACCENT);
            g.fill(new Rectangle2D.Double(16, 27, 6, 6));

            g.setFont(new Font(FONT, Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g.setColor(INK);
            g.drawString("Page", 48, y);
            g.setColor(ACCENT);
            g.drawString("Stay", 48 + fm.stringWidth("Page"), y);
            g.dispose();
        }
    }
}
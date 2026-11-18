package opersys.segmentation;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import opersys.core.UiKit;

/**
 * Segmentation ("SegDrive" look): a segment table (base/limit), a memory map, and a translator
 * that turns a (segment, offset) logical address into a physical address or a segmentation fault.
 */
public class SegmentationDesign extends JPanel {
    private static final Color BG = new Color(233, 241, 237);
    private static final Color GREEN = new Color(14, 122, 76);
    private static final Color GREEN_DARK = new Color(10, 92, 58);
    private static final Color GREEN_SOFT = new Color(197, 236, 212);
    private static final Color PANEL_SOFT = new Color(230, 243, 234);
    private static final Color RED = new Color(188, 48, 48);
    private static final Color RED_SOFT = new Color(252, 232, 230);
    private static final Color INK = new Color(32, 32, 32);
    private static final Color MUTED = new Color(100, 100, 100);
    private static final Color LINE = new Color(228, 228, 228);
    private static final Color[] PALETTE = {
            new Color(86, 190, 255), new Color(255, 178, 72), new Color(126, 224, 138), new Color(200, 130, 255),
            new Color(255, 128, 150), new Color(255, 220, 90), new Color(110, 220, 210), new Color(160, 170, 255)};
    public interface Listener {
        String onAddSegment(String name, String base, String limit);
        void onRemoveSegment(int index);
        void onSelectSegment(int index);
        void onTranslate(String segment, String offset);
    }

    private final List<SegmentationEngine.Segment> segments = new ArrayList<>();
    private int selected = -1;
    private SegmentationEngine.Result result;
    private int total = 4000;
    private int used;
    private Listener listener;

    private final RoundField segField = new RoundField("0", 3);
    private final RoundField offField = new RoundField("100", 5);
    private final TableView tableView = new TableView();
    private final MapView mapView = new MapView();
    private final ResultView resultView = new ResultView();
    private final UsageBar usageBar = new UsageBar();
    private final JLabel mapTitle = new JLabel();
    private final JPanel mapBlock = new JPanel();
    private final NavItem[] nav = new NavItem[3];

    public SegmentationDesign() {
        super(new BorderLayout());
        setBackground(BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);

        refresh();
        setView(0);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String getSegText() {
        return segField.getText();
    }

    public String getOffText() {
        return offField.getText();
    }

    public void setSegText(String text) {
        segField.setText(text);
    }

    public void setState(List<SegmentationEngine.Segment> segments, int selected,
                         SegmentationEngine.Result result, int total, int used) {
        this.segments.clear();
        this.segments.addAll(segments);
        this.selected = selected;
        this.result = result;
        this.total = total;
        this.used = used;
        refresh();
    }

    public void showError(String message) {
        UiKit.error(this, message);
    }

    private void removeRow(int row) {
        if (listener != null) listener.onRemoveSegment(row);
    }

    private void selectRow(int row) {
        if (row < 0 || row >= segments.size()) return;
        segField.setText(String.valueOf(row));
        if (listener != null) listener.onSelectSegment(row);
    }

    private void requestTranslation() {
        if (listener != null) listener.onTranslate(getSegText(), getOffText());
    }

    private void addSegmentDialog() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "New segment", Dialog.ModalityType.APPLICATION_MODAL);
        Color fieldFill = new Color(238, 244, 240);
        RoundField name = new RoundField("Stack", 18, fieldFill);
        RoundField base = new RoundField("3000", 18, fieldFill);
        RoundField limit = new RoundField("400", 18, fieldFill);
        for (RoundField f : new RoundField[]{name, base, limit}) {
            f.setHorizontalAlignment(JTextField.LEFT);
            f.setFont(font(Font.PLAIN, 14));
            f.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            f.setAlignmentX(LEFT_ALIGNMENT);
        }

        RoundCard card = new RoundCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        Object[][] rows = {{"Name", name}, {"Base (KB)", base}, {"Limit (KB)", limit}};
        for (int i = 0; i < rows.length; i++) {
            JLabel l = small((String) rows[i][0]);
            l.setAlignmentX(LEFT_ALIGNMENT);
            card.add(l);
            card.add(Box.createVerticalStrut(4));
            card.add((JComponent) rows[i][1]);
            if (i < rows.length - 1) card.add(Box.createVerticalStrut(12));
        }

        JLabel error = new JLabel(" ");
        error.setFont(font(Font.PLAIN, 12));
        error.setForeground(RED);

        PillButton add = new PillButton("Add segment");
        PillButton cancel = new PillButton("Cancel", false);
        Runnable submit = () -> {
            if (listener == null) {
                error.setText("Segmentation is not ready yet.");
                return;
            }
            String message = listener.onAddSegment(name.getText(), base.getText(), limit.getText());
            if (message != null) {
                error.setText(message);
                return;
            }
            dlg.dispose();
        };
        add.addActionListener(e -> submit.run());
        cancel.addActionListener(e -> dlg.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(add);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(error, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.EAST);

        JLabel heading = new JLabel("New segment");
        heading.setFont(font(Font.BOLD, 18));
        heading.setForeground(INK);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        root.add(heading, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.getRootPane().setDefaultButton(add);
        dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setSize(Math.max(dlg.getWidth(), 380), dlg.getHeight());
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void refresh() {
        mapTitle.setText("<html><b>Where the segments sit in memory</b> <font color='#8a8a8a'>("
                + String.format("%,d", total) + " KB total)</font></html>");
        tableView.revalidate();
        revalidate();
        repaint();
    }

    private void setView(int view) {
        for (int i = 0; i < nav.length; i++) nav[i].selected = (i == view);
        tableView.setVisible(view != 1);
        mapBlock.setVisible(view != 2);
        resultView.setVisible(view != 1);
        for (NavItem n : nav) n.repaint();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 20));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        Icon logo = SegmentationIcon.create(38);
        if (logo != null) brand.add(new JLabel(logo));
        JLabel title = new JLabel("Segmentation");
        title.setFont(font(Font.BOLD, 21));
        title.setForeground(INK);
        brand.add(title);
        header.add(brand, BorderLayout.WEST);

        Pill pill = new Pill();
        PillButton go = new PillButton("Translate");
        go.addActionListener(e -> requestTranslation());
        segField.addActionListener(e -> requestTranslation());
        offField.addActionListener(e -> requestTranslation());
        pill.add(new SearchIcon());
        pill.add(small("Segment"));
        pill.add(segField);
        pill.add(small("Offset"));
        pill.add(offField);
        pill.add(go);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(pill);
        header.add(center, BorderLayout.CENTER);

        return header;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 8));
        side.setPreferredSize(new Dimension(212, 0));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        NewButton add = new NewButton("New segment");
        add.addActionListener(e -> addSegmentDialog());
        add.setAlignmentX(LEFT_ALIGNMENT);
        top.add(add);
        top.add(Box.createVerticalStrut(18));
        String[] names = {"Segment table", "Memory map", "Translation"};
        for (int i = 0; i < nav.length; i++) {
            final int idx = i;
            nav[i] = new NavItem(names[i], i);
            nav[i].setAlignmentX(LEFT_ALIGNMENT);
            nav[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setView(idx);
                }
            });
            top.add(nav[i]);
            top.add(Box.createVerticalStrut(4));
        }
        side.add(top, BorderLayout.NORTH);
        side.add(usageBar, BorderLayout.SOUTH);
        return side;
    }

    private JComponent buildContent() {
        JLabel crumb = new JLabel("<html><span style='color:#646464'>My Drive &nbsp;\u203A&nbsp; Process P1 &nbsp;\u203A&nbsp;</span>"
                + " <b>Segments</b></html>");
        crumb.setFont(font(Font.PLAIN, 17));
        crumb.setForeground(INK);

        mapTitle.setFont(font(Font.PLAIN, 13));
        mapTitle.setForeground(INK);

        mapBlock.setOpaque(false);
        mapBlock.setLayout(new BoxLayout(mapBlock, BoxLayout.Y_AXIS));
        mapTitle.setAlignmentX(LEFT_ALIGNMENT);
        mapView.setAlignmentX(LEFT_ALIGNMENT);
        mapBlock.add(mapTitle);
        mapBlock.add(Box.createVerticalStrut(4));
        mapBlock.add(mapView);
        mapBlock.setAlignmentX(LEFT_ALIGNMENT);

        crumb.setAlignmentX(LEFT_ALIGNMENT);
        tableView.setAlignmentX(LEFT_ALIGNMENT);
        resultView.setAlignmentX(LEFT_ALIGNMENT);

        ScrollColumn column = new ScrollColumn();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(crumb);
        column.add(Box.createVerticalStrut(14));
        column.add(tableView);
        column.add(Box.createVerticalStrut(14));
        column.add(mapBlock);
        column.add(Box.createVerticalStrut(10));
        column.add(resultView);

        JScrollPane scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        RoundCard card = new RoundCard();
        card.add(scroll, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 12));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private static JLabel small(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(new Color(70, 70, 70));
        return l;
    }

    // ------------------------------------------------------------------ painting helpers

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private static int baseline(FontMetrics fm, int centerY) {
        return centerY + (fm.getAscent() - fm.getDescent()) / 2;
    }

    private Color colorOf(int i) {
        return PALETTE[i % PALETTE.length];
    }

    /** Panel whose maximum height equals its preferred height (so BoxLayout does not stretch it). */
    private abstract static class View extends JPanel {
        View() {
            setOpaque(false);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Scroll content that always matches the window width (it only scrolls up and down). */
    private static final class ScrollColumn extends JPanel implements Scrollable {
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
            return 64;
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

    private static final class RoundCard extends JPanel {
        RoundCard() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g2.dispose();
        }
    }

    private static final class Pill extends JPanel {
        Pill() {
            super(new FlowLayout(FlowLayout.LEFT, 10, 7));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(new Color(222, 233, 228));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }

    private static final class SearchIcon extends JComponent {
        SearchIcon() {
            setPreferredSize(new Dimension(22, 22));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(3, 3, 11, 11);
            g2.drawLine(13, 13, 19, 19);
            g2.dispose();
        }
    }

    private static final class RoundField extends JTextField {
        private final String hint;
        private final Color fill;

        RoundField(String hint, int columns) {
            this(hint, columns, Color.WHITE);
        }

        RoundField(String hint, int columns, Color fill) {
            super(columns);
            this.hint = hint;
            this.fill = fill;
            setOpaque(false);
            setFont(new Font("Consolas", Font.BOLD, 14));
            setForeground(INK);
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            super.paintComponent(g2);
            if (getText().isEmpty()) {   // gray example, shown only while the box is empty
                g2.setFont(getFont());
                g2.setColor(new Color(170, 170, 170));
                FontMetrics fm = g2.getFontMetrics();
                int hx = getHorizontalAlignment() == CENTER ? (getWidth() - fm.stringWidth(hint)) / 2 : getInsets().left;
                g2.drawString(hint, hx, baseline(fm, getHeight() / 2));
            }
            g2.dispose();
        }
    }

    private static final class PillButton extends JButton {
        private final boolean primary;

        PillButton(String text) {
            this(text, true);
        }

        PillButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(font(Font.BOLD, 14));
            setForeground(primary ? Color.WHITE : INK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(9, 26, 9, 26));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            if (primary)
                g2.setColor(getModel().isPressed() ? new Color(8, 80, 50) : getModel().isRollover() ? GREEN_DARK : GREEN);
            else
                g2.setColor(getModel().isPressed() ? new Color(196, 214, 204)
                        : getModel().isRollover() ? new Color(210, 226, 217) : new Color(222, 233, 228));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class NewButton extends JButton {
        NewButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 14));
            setForeground(INK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 52, 0, 12));
            setPreferredSize(new Dimension(170, 52));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(0, 2, w, h - 1, 22, 22);
            g2.setColor(getModel().isRollover() ? new Color(246, 252, 248) : Color.WHITE);
            g2.fillRoundRect(0, 0, w, h - 2, 22, 22);
            g2.setColor(GREEN_DARK);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 28, cy = (h - 2) / 2;
            g2.drawLine(cx - 8, cy, cx + 8, cy);
            g2.drawLine(cx, cy - 8, cx, cy + 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class NavItem extends JComponent {
        final String text;
        final int kind;
        boolean selected;

        NavItem(String text, int kind) {
            this.text = text;
            this.kind = kind;
            setPreferredSize(new Dimension(170, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight(), cy = h / 2;
            Color ink = selected ? GREEN_DARK : new Color(60, 60, 60);
            if (selected) {
                g2.setColor(GREEN_SOFT);
                g2.fillRoundRect(0, 0, w, h, h, h);
            }
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ix = 16;
            if (kind == 0) {            // folder
                g2.fillRoundRect(ix, cy - 6, 14, 12, 3, 3);
                g2.fillRoundRect(ix, cy - 8, 6, 4, 2, 2);
            } else if (kind == 1) {     // map
                g2.drawRoundRect(ix, cy - 4, 14, 8, 3, 3);
            } else {                    // arrow
                g2.drawLine(ix, cy, ix + 13, cy);
                g2.drawLine(ix + 8, cy - 4, ix + 13, cy);
                g2.drawLine(ix + 8, cy + 4, ix + 13, cy);
            }
            g2.setFont(font(selected ? Font.BOLD : Font.PLAIN, 14));
            g2.drawString(text, 44, baseline(g2.getFontMetrics(), cy));
            g2.dispose();
        }
    }

    private final class UsageBar extends JComponent {
        UsageBar() {
            setPreferredSize(new Dimension(170, 78));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            g2.setFont(font(Font.PLAIN, 13));
            g2.setColor(new Color(60, 60, 60));
            g2.drawString("Memory used", 8, 18);
            g2.setColor(new Color(205, 218, 211));
            g2.fillRoundRect(8, 30, w - 16, 7, 7, 7);
            g2.setColor(used > total ? RED : GREEN);
            g2.fillRoundRect(8, 30, (int) Math.round((w - 16) * Math.min(1.0, used / (double) total)), 7, 7, 7);
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(MUTED);
            g2.drawString(String.format("%,d of %,d KB used", used, total), 8, 58);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ table

    private final class TableView extends View {
        private static final int HEAD = 32;
        private static final int ROW = 41;

        TableView() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handle(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handle(e);
                }

                private void handle(MouseEvent e) {
                    int row = (e.getY() - HEAD) / ROW;
                    if (e.getY() < HEAD || row >= segments.size()) return;
                    if (e.isPopupTrigger()) {
                        selectRow(row);
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem remove = new JMenuItem("Remove segment S" + row);
                        remove.addActionListener(a -> removeRow(row));
                        menu.add(remove);
                        menu.show(TableView.this, e.getX(), e.getY());
                    } else if (e.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
                        selectRow(row);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, HEAD + ROW * Math.max(1, segments.size()) + 4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int segX = (int) (w * 0.52), baseX = (int) (w * 0.67), limX = (int) (w * 0.83);

            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(MUTED);
            g2.drawString("Name", 8, 20);
            g2.drawString("Segment", segX, 20);
            g2.drawString("Base (KB)", baseX, 20);
            g2.drawString("Limit (KB)", limX, 20);
            g2.setColor(LINE);
            g2.drawLine(0, HEAD - 3, w, HEAD - 3);

            Font mono = new Font("Consolas", Font.PLAIN, 13);
            for (int i = 0; i < segments.size(); i++) {
                SegmentationEngine.Segment s = segments.get(i);
                int y = HEAD + i * ROW;
                int cy = y + ROW / 2;
                boolean sel = i == selected;
                if (sel) {
                    g2.setColor(GREEN_SOFT);
                    g2.fillRoundRect(0, y, w, ROW - 1, 12, 12);
                } else if (i > 0) {
                    g2.setColor(new Color(238, 238, 238));
                    g2.drawLine(0, y, w, y);
                }
                g2.setColor(colorOf(i));
                g2.fillRoundRect(8, cy - 12, 27, 24, 8, 8);
                g2.setColor(INK);
                g2.setFont(font(Font.BOLD, 11));
                String badge = "S" + i;
                g2.drawString(badge, 8 + (27 - g2.getFontMetrics().stringWidth(badge)) / 2,
                        baseline(g2.getFontMetrics(), cy));

                g2.setFont(font(sel ? Font.BOLD : Font.PLAIN, 14));
                g2.drawString(clip(s.name, g2.getFontMetrics(), segX - 60), 48, baseline(g2.getFontMetrics(), cy));
                g2.drawString(String.valueOf(i), segX, baseline(g2.getFontMetrics(), cy));
                g2.setFont(mono);
                g2.drawString(String.valueOf(s.base), baseX, baseline(g2.getFontMetrics(), cy));
                g2.drawString(String.valueOf(s.limit), limX, baseline(g2.getFontMetrics(), cy));
            }
            if (segments.isEmpty()) {
                g2.setFont(font(Font.PLAIN, 13));
                g2.setColor(MUTED);
                g2.drawString("No segments yet. Press \"New segment\" to add one.", 8, HEAD + 25);
            }
            g2.dispose();
        }
    }

    private static String clip(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "...") > width) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    // ------------------------------------------------------------------ memory map

    private final class MapView extends View {
        MapView() {
            setPreferredSize(new Dimension(200, 104));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int barY = 30, barH = 38;
            int total = SegmentationDesign.this.total;

            g2.setColor(new Color(228, 234, 231));
            g2.fillRoundRect(0, barY, w, barH, 12, 12);

            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < segments.size(); i++) {
                    boolean sel = i == selected;
                    if (sel != (pass == 1)) continue;
                    SegmentationEngine.Segment s = segments.get(i);
                    int x = (int) Math.round((double) s.base / total * w);
                    int bw = Math.max(8, (int) Math.round((double) s.limit / total * w));
                    g2.setColor(colorOf(i));
                    g2.fillRoundRect(x, barY + 2, bw, barH - 4, 10, 10);
                    if (sel) {
                        g2.setColor(GREEN_DARK);
                        g2.setStroke(new BasicStroke(2.6f));
                        g2.drawRoundRect(x, barY + 1, bw, barH - 3, 10, 10);
                    }
                    g2.setColor(INK);
                    g2.setFont(font(Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String label = "S" + i;
                    if (fm.stringWidth(label) < bw)
                        g2.drawString(label, x + (bw - fm.stringWidth(label)) / 2, baseline(fm, barY + barH / 2));
                }
            }

            // tick labels
            int step = total <= 6000 ? 1000 : (int) Math.ceil(total / 5.0 / 1000) * 1000;
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(MUTED);
            FontMetrics fm = g2.getFontMetrics();
            for (int v = 0; v <= total; v += step) {
                int x = (int) Math.round((double) v / total * w);
                String t = String.valueOf(v);
                int tx = v == 0 ? 0 : v == total ? w - fm.stringWidth(t) : x - fm.stringWidth(t) / 2;
                g2.drawString(t, Math.min(tx, w - fm.stringWidth(t)), barY + barH + 16);
            }
            g2.drawString(String.valueOf(total), w - fm.stringWidth(String.valueOf(total)), barY + barH + 16);

            // marker for the last translated physical address
            if (result != null && result.isOk()) {
                int px = (int) Math.round((double) result.physical / total * w);
                px = Math.max(5, Math.min(w - 5, px));
                g2.setColor(GREEN_DARK);
                g2.fillPolygon(new int[]{px - 5, px + 5, px}, new int[]{barY - 12, barY - 12, barY - 4}, 3);
                g2.setFont(font(Font.BOLD, 12));
                String t = String.valueOf(result.physical);
                int tw = g2.getFontMetrics().stringWidth(t);
                int tx = px + 10 + tw > w ? px - 10 - tw : px + 10;
                g2.drawString(t, tx, barY - 4);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ translation result

    private static final class Run {
        final String text;
        final boolean bold;
        final Color color;

        Run(String text, boolean bold, Color color) {
            this.text = text;
            this.bold = bold;
            this.color = color;
        }
    }

    private final class ResultView extends View {
        ResultView() {
            setPreferredSize(new Dimension(200, 132));
        }

        /** Draws a rounded chip and returns the x just past its right edge. mark: 0 none, 1 check, 2 cross. */
        private int chip(Graphics2D g2, int x, int cy, Color fill, Color border, int mark, Color markColor, Run... runs) {
            int h = 36, pad = 16, markW = mark != 0 ? 20 : 0;
            int w = pad * 2 + markW;
            for (Run r : runs) w += g2.getFontMetrics(font(r.bold ? Font.BOLD : Font.PLAIN, 14)).stringWidth(r.text);
            g2.setColor(fill);
            g2.fillRoundRect(x, cy - h / 2, w, h, h, h);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, cy - h / 2, w - 1, h - 1, h, h);
            }
            int tx = x + pad;
            if (mark != 0) {
                g2.setColor(markColor);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (mark == 1) {
                    g2.drawLine(tx, cy, tx + 4, cy + 4);
                    g2.drawLine(tx + 4, cy + 4, tx + 11, cy - 5);
                } else {
                    g2.drawLine(tx, cy - 5, tx + 10, cy + 5);
                    g2.drawLine(tx, cy + 5, tx + 10, cy - 5);
                }
                tx += markW;
            }
            for (Run r : runs) {
                g2.setFont(font(r.bold ? Font.BOLD : Font.PLAIN, 14));
                g2.setColor(r.color);
                g2.drawString(r.text, tx, baseline(g2.getFontMetrics(), cy));
                tx += g2.getFontMetrics().stringWidth(r.text);
            }
            return x + w;
        }

        private int arrow(Graphics2D g2, int x, int cy) {
            g2.setColor(GREEN);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 8, cy, x + 20, cy);
            g2.drawLine(x + 15, cy - 4, x + 20, cy);
            g2.drawLine(x + 15, cy + 4, x + 20, cy);
            return x + 28;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(PANEL_SOFT);
            g2.fillRoundRect(0, 0, w, h, 18, 18);

            int cy = 46;
            Color edge = new Color(214, 226, 218);
            SegmentationEngine.Result r = result;
            if (r == null) {
                g2.setFont(font(Font.PLAIN, 14));
                g2.setColor(MUTED);
                g2.drawString("Type a segment and an offset above, then press Translate.", 20, cy + 5);
            } else {
                int x = 20;
                x = chip(g2, x, cy, Color.WHITE, edge, 0, null,
                        new Run("Address  ", false, INK), new Run("(" + r.seg + ", " + r.offset + ")", true, INK));
                x = arrow(g2, x, cy);
                if (r.isOk()) {
                    x = chip(g2, x, cy, Color.WHITE, edge, 1, GREEN,
                            new Run(r.offset + " < limit " + r.limit, false, INK));
                    x = arrow(g2, x, cy);
                    chip(g2, x, cy, GREEN, null, 0, null,
                            new Run("Physical address  ", false, Color.WHITE),
                            new Run(r.base + " + " + r.offset + " = " + r.physical, true, Color.WHITE));
                } else {
                    String why = r.isNoSegment()
                            ? "Segment " + r.seg + " does not exist"
                            : r.offset + (r.offset < 0 ? " is negative" : " \u2265 limit " + r.limit);
                    x = chip(g2, x, cy, RED_SOFT, null, 2, RED, new Run(why, false, RED));
                    x = arrow(g2, x, cy);
                    chip(g2, x, cy, RED, null, 0, null, new Run("Segmentation fault", true, Color.WHITE));
                }
            }
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(MUTED);
            g2.drawString("If the offset is not smaller than the limit, it is a segmentation fault.", 20, cy + 44);
            g2.dispose();
        }
    }
}
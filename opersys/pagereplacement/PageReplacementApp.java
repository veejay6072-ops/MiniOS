package opersys.pagereplacement;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Standalone launcher for page replacement. */
public final class PageReplacementApp {
    private PageReplacementApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Page Replacement");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new PageReplacementController(new PageReplacementDesign()).getView());
            frame.setSize(1000, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

package opersys.paging;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Standalone launcher for paging. */
public final class PagingApp {
    private PagingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Paging");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new PagingController().getView());
            frame.setSize(1000, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

package opersys.segmentation;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Standalone launcher for segmentation. */
public final class SegmentationApp {
    private SegmentationApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Segmentation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new SegmentationController(new SegmentationDesign()).getView());
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

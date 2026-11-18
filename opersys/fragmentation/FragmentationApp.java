package opersys.fragmentation;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Icon;

/** Standalone launcher for the fragmentation simulator. */
public final class FragmentationApp {
    private FragmentationApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Fragmentation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new FragmentationController().getView());
            frame.setSize(860, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static Icon desktopIcon() {
        return FragmentationDesign.desktopIcon();
    }
}

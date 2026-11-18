package opersys.diskscheduling;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Icon;

/** Standalone launcher for disk scheduling. */
public final class DiskSchedulingApp {
    private DiskSchedulingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Disk Scheduling");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new DiskSchedulingController(new DiskSchedulingDesign()).getView());
            frame.setSize(900, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static Icon icon(int size) {
        return DiskSchedulingDesign.icon(size);
    }

    public static Icon desktopIcon() {
        return DiskSchedulingDesign.desktopIcon();
    }
}

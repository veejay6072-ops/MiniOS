package opersys.cpuscheduling;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Standalone launcher for CPU scheduling. */
public final class CpuSchedulingApp {
    private CpuSchedulingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CPU Scheduling");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new CpuController(new CpuSchedulingDesign()).getView());
            frame.setSize(1000, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

package opersys.multiprogramming;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Standalone launcher for multiprogramming. */
public final class MultiprogrammingApp {
    private MultiprogrammingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multiprogramming");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new MultiprogrammingController(new MultiprogrammingDesign()).getView());
            frame.setSize(900, 650);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static Icon desktopIcon() {
        return MultiprogrammingDesign.desktopIcon();
    }
}

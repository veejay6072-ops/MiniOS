import javax.swing.*;

/** Standalone launcher: opens the Banker's Algorithm in its own window (without VirtualOS). */
public class SafeBankApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Banker's Algorithm");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new SafeBankController().getView());
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
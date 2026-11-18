package opersys.bankers;

import javax.swing.*;

/**
 * BANK APP = a tiny launcher so you can open the Banker's Algorithm in its own window,
 * without the whole VirtualOS desktop. Handy for testing just this module.
 */
public class BankApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Banker's Algorithm");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new BankController().getView());   // the controller builds the screen for us
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);   // center on the screen
            frame.setVisible(true);
        });
    }
}
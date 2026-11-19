package opersys;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.*;

/**
 * Entrypoint dito mag run.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("[Main] started");
        System.out.println("[Main] Java " + System.getProperty("java.version")
                + ", folder: " + System.getProperty("user.dir"));

        // Anything that goes wrong on the Swing thread gets printed here and shown in a dialog.
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.err.println("[Main] uncaught error in thread " + thread.getName());
            error.printStackTrace();
        });

        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("[Main] using the default look and feel (" + e + ")");
            }

            System.out.println("[Main] creating the desktop...");
            VirtualDesktop desktop = new VirtualDesktop();

            desktop.setVisible(true);
            desktop.setAlwaysOnTop(true);   // pop in front of the IDE once
            desktop.toFront();
            desktop.requestFocus();
            desktop.setAlwaysOnTop(false);

            System.out.println("[Main] desktop is open");
        } catch (Throwable t) {
            t.printStackTrace();
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            String text = trace.toString();
            if (text.length() > 1500) text = text.substring(0, 1500) + "\n...";
            JOptionPane.showMessageDialog(null,
                    "Could not start the desktop:\n\n" + text,
                    "Virtual Desktop", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
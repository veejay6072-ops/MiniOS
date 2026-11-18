import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

// Runs the Virtual Desktop app.
public class Main {
    public static void main(String[] args) {
        File root = findProjectRoot();
        if (root == null) {
            fail("Could not find the 'opersys' folder (the one with VirtualDesktop.java inside).");
            return;
        }
        File out = new File(root, "out");
        out.mkdirs();
        if (!compile(root, out)) return;
        SwingUtilities.invokeLater(() -> launch(out));
    }

    // Finds the project folder.
    private static File findProjectRoot() {
        File root = search(new File(System.getProperty("user.dir")).getAbsoluteFile());
        if (root != null) return root;
        try {
            File here = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return search(here.getAbsoluteFile());
        } catch (Exception e) {
            return null;
        }
    }

    private static File search(File start) {
        for (File d = start; d != null; d = d.getParentFile()) {
            if (new File(d, "opersys/VirtualDesktop.java").isFile()) return d;
            if (d.getName().equals("opersys") && new File(d, "VirtualDesktop.java").isFile()) {
                return d.getParentFile();
            }
        }
        return null;
    }

    // Compiles the desktop and its related classes.
    private static boolean compile(File root, File out) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            fail("No Java compiler found. Install a JDK (not just a JRE) and run again.");
            return false;
        }
        File desktop = new File(root, "opersys/VirtualDesktop.java");
        int result = javac.run(null, null, null,
                "-encoding", "UTF-8",
                "-d", out.getPath(),
                "-cp", out.getPath(),
                "-sourcepath", root.getPath(),
                desktop.getPath());
        if (result != 0) {
            fail("Compile failed. The errors are listed in the Output / Terminal panel.");
            return false;
        }
        return true;
    }

    // Opens the compiled desktop window.
    private static void launch(File out) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            // Load the newest compiled classes.
            URLClassLoader loader = new URLClassLoader(new URL[]{out.toURI().toURL()},
                    ClassLoader.getPlatformClassLoader());
            Class<?> type;
            try {
                type = loader.loadClass("opersys.VirtualDesktop");
            } catch (ClassNotFoundException e) {
                type = loader.loadClass("VirtualDesktop"); // Tries the fallback class name.
            }
            JFrame desktop = (JFrame) type.getDeclaredConstructor().newInstance();
            desktop.setVisible(true);
            // Bring the desktop window to the front.
            desktop.setAlwaysOnTop(true);
            desktop.toFront();
            desktop.requestFocus();
            desktop.setAlwaysOnTop(false);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Could not start the desktop: " + t);
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, "Virtual Desktop", JOptionPane.ERROR_MESSAGE);
    }
}
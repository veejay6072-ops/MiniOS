import javax.swing.*;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

// Runs the Virtual Desktop app. Press Run on this file, nothing else is needed.
// There is no "package" line on purpose: this file only finds, compiles and starts
// opersys.VirtualDesktop by itself, so VS Code can run it wherever the source root is set.
public class Main {
    public static void main(String[] args) {
        File root = findProjectRoot();
        if (root == null) {
            fail("Could not find the 'opersys' folder (the one with VirtualDesktop.java inside).");
            return;
        }
        File out = new File(root, "out_virtualos");   // its own folder, so old .class files can't clash
        out.mkdirs();
        if (!compile(root, out)) return;
        SwingUtilities.invokeLater(() -> launch(out));
    }

    // Finds the folder that CONTAINS the 'opersys' folder.
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

    // Compiles the desktop and everything it uses (found through -sourcepath).
    private static boolean compile(File root, File out) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            fail("No Java compiler found. Run with a JDK (not just a JRE).");
            return false;
        }
        File desktop = new File(root, "opersys/VirtualDesktop.java");
        DiagnosticCollector<JavaFileObject> problems = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = javac.getStandardFileManager(problems, null, null)) {
            boolean ok = javac.getTask(null, files, problems,
                    Arrays.asList("-encoding", "UTF-8",
                            "-d", out.getPath(),
                            "-cp", out.getPath(),
                            "-sourcepath", root.getPath()),
                    null, files.getJavaFileObjects(desktop)).call();
            if (ok) return true;
        } catch (Exception e) {
            fail("Compile failed: " + e);
            return false;
        }

        // Show the real reasons, so a missing file or a typo is easy to spot.
        StringBuilder msg = new StringBuilder("Compile failed:\n\n");
        int shown = 0;
        for (Diagnostic<? extends JavaFileObject> d : problems.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            if (shown++ == 8) {
                msg.append("...more errors in the terminal\n");
                break;
            }
            String file = d.getSource() == null ? "?" : new File(d.getSource().getName()).getName();
            msg.append(file).append(" line ").append(d.getLineNumber()).append(": ")
                    .append(d.getMessage(null)).append("\n");
        }
        fail(msg.toString());
        return false;
    }

    // Opens the compiled desktop window.
    private static void launch(File out) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            URLClassLoader loader = new URLClassLoader(new URL[]{out.toURI().toURL()},
                    ClassLoader.getPlatformClassLoader());
            Class<?> type = loader.loadClass("opersys.VirtualDesktop");
            JFrame desktop = (JFrame) type.getDeclaredConstructor().newInstance();
            desktop.setVisible(true);
            desktop.setAlwaysOnTop(true);   // bring the window to the front once
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
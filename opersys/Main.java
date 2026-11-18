import javax.swing.*;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 Entrypoint for the Virtual Desktop project. Compiles and launches the desktop.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("[Main] started, folder: " + System.getProperty("user.dir"));
        File root = findProjectRoot();
        if (root == null) {
            fail("Could not find the 'opersys' folder (the one with VirtualDesktop.java inside).");
            return;
        }
        System.out.println("[Main] project root: " + root);
        try {
            Path out = Files.createTempDirectory("virtualos-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteTree(out)));
            System.out.println("[Main] compiling...");
            if (compile(root, out.toFile())) {
                System.out.println("[Main] compiled, opening the desktop...");
                SwingUtilities.invokeLater(() -> launch(root, out.toFile()));
            }
        } catch (IOException e) {
            fail("Could not create a temporary folder: " + e);
        }
    }

    // ---------- find ----------

    /** The folder that CONTAINS the 'opersys' folder. */
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
        for (File dir = start; dir != null; dir = dir.getParentFile()) {
            if (new File(dir, "opersys/VirtualDesktop.java").isFile()) return dir;
            if (new File(dir, "src/opersys/VirtualDesktop.java").isFile()) return new File(dir, "src");
            if (dir.getName().equals("opersys") && new File(dir, "VirtualDesktop.java").isFile()) {
                return dir.getParentFile();
            }
        }
        return null;
    }

    // ---------- compile ----------

    /** Compiles VirtualDesktop and everything it uses (found through -sourcepath). */
    private static boolean compile(File root, File out) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            fail("No Java compiler found. Run with a JDK (not just a JRE).");
            return false;
        }
        DiagnosticCollector<JavaFileObject> problems = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = javac.getStandardFileManager(problems, null, null)) {
            boolean ok = javac.getTask(null, files, problems,
                    Arrays.asList("-encoding", "UTF-8",
                            "-d", out.getPath(),
                            "-sourcepath", root.getPath()),
                    null,
                    files.getJavaFileObjects(new File(root, "opersys/VirtualDesktop.java"))).call();
            if (ok) return true;
        } catch (IOException e) {
            fail("Compile failed: " + e);
            return false;
        }
        fail(describe(problems));
        return false;
    }

    /** The first few compile errors, one per line, so a missing file or a typo is easy to spot. */
    private static String describe(DiagnosticCollector<JavaFileObject> problems) {
        StringBuilder text = new StringBuilder("Compile failed:\n\n");
        int shown = 0;
        for (Diagnostic<? extends JavaFileObject> d : problems.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            if (shown++ == 8) {
                text.append("...and more\n");
                break;
            }
            String file = d.getSource() == null ? "?" : new File(d.getSource().getName()).getName();
            text.append(file).append(" line ").append(d.getLineNumber())
                    .append(": ").append(d.getMessage(null)).append('\n');
        }
        return text.toString();
    }

    // ---------- launch ----------

    private static void launch(File root, File out) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // the default look is fine
            }
            // The project folder is on the path too, so the wallpaper (bliss.jpg) can be found there.
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{out.toURI().toURL(), root.toURI().toURL()},
                    ClassLoader.getPlatformClassLoader());
            JFrame desktop = (JFrame) loader.loadClass("opersys.VirtualDesktop")
                    .getDeclaredConstructor().newInstance();
            desktop.setVisible(true);
            desktop.setAlwaysOnTop(true);   // pop in front of VS Code once
            desktop.toFront();
            desktop.requestFocus();
            desktop.setAlwaysOnTop(false);
            System.out.println("[Main] desktop is open");
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Could not start the desktop: " + t);
        }
    }

    // ---------- helpers ----------

    private static void deleteTree(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // a leftover temp folder is harmless
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, "Virtual Desktop", JOptionPane.ERROR_MESSAGE);
    }
}
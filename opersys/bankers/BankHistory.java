package opersys.bankers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * BANK HISTORY = remembers the old safety checks (newest first).
 *
 * Every time you press "Check safety" the setup and the answer get saved here. The list is also written to a
 * small text file in your home folder, so it is still there the next time you open the program.
 * You can look at old runs and load one back into the form. It has no screen code, just the saving and loading.
 */
public final class BankHistory {
    private static final int MAX_ENTRIES = 50;   // keep only the latest 50 so the file doesn't grow forever

    /** One saved check: the tables you typed and what the bank answered. */
    public static final class Entry {
        private final long time;           // when you pressed Check safety
        private final boolean safe;        // the answer: safe or unsafe
        private final int[] order;         // the safe order (empty if unsafe)
        private final double[] available;     // the vault numbers you typed
        private final double[][] max;         // the Max table
        private final double[][] allocation;  // the Allocated table

        /** The normal way to make an entry (the controller uses this one). */
        public Entry(long time, boolean safe, List<Integer> order, double[] available,
                     double[][] max, double[][] allocation) {
            this(time, safe, toArray(order), available, max, allocation);
        }

        /** Used when reading the file back. Everything is copied so the saved data can't be changed by accident. */
        private Entry(long time, boolean safe, int[] order, double[] available,
                      double[][] max, double[][] allocation) {
            this.time = time;
            this.safe = safe;
            this.order = order.clone();
            this.available = available.clone();
            this.max = copy(max);
            this.allocation = copy(allocation);
        }

        public long getTime() {
            return time;
        }

        public boolean isSafe() {
            return safe;
        }

        public int[] getOrder() {
            return order.clone();
        }

        public double[] getAvailable() {
            return available.clone();
        }

        public double[][] getMax() {
            return copy(max);
        }

        public double[][] getAllocation() {
            return copy(allocation);
        }

        /** One line for the history list, like "Nov 18, 4:30 PM   SAFE   P1 -> P3 -> P0   [5 processes, 3 resources]". */
        public String summary() {
            String when = new SimpleDateFormat("MMM d, h:mm a").format(new Date(time));
            String answer = safe ? "SAFE    " + orderText() : "UNSAFE  (no safe order)";
            return when + "   " + answer + "   [" + max.length + " processes, " + available.length + " resources]";
        }

        private String orderText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < order.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append("P").append(order[i]);
            }
            return sb.toString();
        }

        /** Same tables as another entry? (Used to skip saving the exact same check twice in a row.) */
        boolean sameSetup(Entry o) {
            return Arrays.equals(available, o.available)
                    && Arrays.deepEquals(max, o.max)
                    && Arrays.deepEquals(allocation, o.allocation);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final File file;   // where the history is saved

    /** Uses the normal save file in the user's home folder. */
    public BankHistory() {
        this(new File(System.getProperty("user.home", "."), ".virtualos_bank_history.txt"));
    }

    /** Uses a file of your choice (handy for testing). Loads whatever was saved before. */
    public BankHistory(File file) {
        this.file = file;
        load();
    }

    /** The saved checks, newest first. You can look but not change the list. */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Puts a check at the top. Skipped if it is exactly the same as the latest one. */
    public void add(Entry e) {
        if (!entries.isEmpty() && entries.get(0).sameSetup(e)) return;
        entries.add(0, e);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save();
    }

    /** Wipes the whole history (and the file). */
    public void clear() {
        entries.clear();
        save();
    }

    // ---------- saving and loading ----------
    // One line per entry:  time|safe|order|available|allocation|max
    //   order and available look like 1,3,0     tables look like 0,1,0;2,0,0;3,0,2  (rows split by ;)

    private void load() {
        if (!file.exists()) return;
        try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    String[] p = line.split("\\|", -1);
                    entries.add(new Entry(Long.parseLong(p[0]), Boolean.parseBoolean(p[1]),
                            parseIntRow(p[2]), parseDoubleRow(p[3]), parseDoubleTable(p[5]), parseDoubleTable(p[4])));
                } catch (RuntimeException badLine) {
                    // a damaged line? skip it and keep the rest
                }
            }
        } catch (IOException ignored) {
            // can't read the file? just start with an empty history
        }
    }

    private void save() {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            for (Entry e : entries) {
                out.println(e.time + "|" + e.safe + "|" + join(e.order) + "|" + join(e.available)
                        + "|" + join(e.allocation) + "|" + join(e.max));
            }
        } catch (IOException ignored) {
            // can't write the file? the history still works while the program is open
        }
    }

    /** {1,3,0} -> "1,3,0" */
    private static String join(int[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) sb.append(i > 0 ? "," : "").append(row[i]);
        return sb.toString();
    }

    /** {{0,1},{2,3}} -> "0,1;2,3" */
    private static String join(double[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) sb.append(i > 0 ? "," : "").append(row[i]);
        return sb.toString();
    }

    private static String join(double[][] table) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < table.length; i++) sb.append(i > 0 ? ";" : "").append(join(table[i]));
        return sb.toString();
    }

    private static int[] parseIntRow(String text) {
        if (text.trim().isEmpty()) return new int[0];
        String[] parts = text.split(",");
        int[] row = new int[parts.length];
        for (int i = 0; i < parts.length; i++) row[i] = Integer.parseInt(parts[i].trim());
        return row;
    }

    private static double[] parseDoubleRow(String text) {
        if (text.trim().isEmpty()) return new double[0];
        String[] parts = text.split(",");
        double[] row = new double[parts.length];
        for (int i = 0; i < parts.length; i++) row[i] = Double.parseDouble(parts[i].trim());
        return row;
    }

    private static double[][] parseDoubleTable(String text) {
        String[] rows = text.split(";");
        double[][] table = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) table[i] = parseDoubleRow(rows[i]);
        return table;
    }

    private static int[] toArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
    }

    private static double[][] copy(double[][] table) {
        double[][] out = new double[table.length][];
        for (int i = 0; i < table.length; i++) out[i] = table[i].clone();
        return out;
    }
}
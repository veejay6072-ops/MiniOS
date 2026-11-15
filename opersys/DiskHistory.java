import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Remembers the previous Disk Scheduling runs (newest first). Saved to a small text file in the
 * user's home folder so the history is still there the next time the program starts.
 */
public final class DiskHistory {
    private static final int MAX_ENTRIES = 100;

    /** One finished setup: the algorithm, direction, head, disk size and request queue. */
    public static final class Entry {
        public final long time;
        public final String algo;
        public final boolean up;
        public final int head;
        public final int tracks;
        public final int[] queue;
        public final int total;

        public Entry(long time, String algo, boolean up, int head, int tracks, int[] queue, int total) {
            this.time = time;
            this.algo = algo;
            this.up = up;
            this.head = head;
            this.tracks = tracks;
            this.queue = queue.clone();
            this.total = total;
        }

        public double average() {
            return queue.length == 0 ? 0 : (double) total / queue.length;
        }

        boolean sameSetup(Entry o) {
            return algo.equals(o.algo) && up == o.up && head == o.head && tracks == o.tracks
                    && Arrays.equals(queue, o.queue);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final File file;

    public DiskHistory() {
        this(new File(System.getProperty("user.home", "."), ".virtualos_disk_history.txt"));
    }

    public DiskHistory(File file) {
        this.file = file;
        load();
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Adds a run at the top. Skipped if it is identical to the most recent one. */
    public void add(Entry e) {
        if (!entries.isEmpty() && entries.get(0).sameSetup(e)) return;
        entries.add(0, e);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save();
    }

    public void clear() {
        entries.clear();
        save();
    }

    // time|algo|up|head|tracks|total|q1,q2,q3
    private void load() {
        if (!file.exists()) return;
        try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    String[] p = line.split("\\|");
                    String[] q = p[6].split(",");
                    int[] queue = new int[q.length];
                    for (int i = 0; i < q.length; i++) queue[i] = Integer.parseInt(q[i].trim());
                    entries.add(new Entry(Long.parseLong(p[0]), p[1], Boolean.parseBoolean(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]), queue, Integer.parseInt(p[5])));
                } catch (RuntimeException badLine) {
                    // skip a damaged line and keep the rest
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void save() {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            for (Entry e : entries) {
                StringBuilder q = new StringBuilder();
                for (int i = 0; i < e.queue.length; i++) q.append(i > 0 ? "," : "").append(e.queue[i]);
                out.println(e.time + "|" + e.algo + "|" + e.up + "|" + e.head + "|" + e.tracks + "|" + e.total + "|" + q);
            }
        } catch (IOException ignored) {
            // history still works in memory if the file cannot be written
        }
    }
}
package opersys.diskscheduling;

import java.util.Arrays;

/**
 * DISK SCHEDULING MODEL = the middleman between the screen and the algorithm. LOGIC ONLY, no Swing here.
 *
 * The screen only has text boxes, so everything arrives as text ("53", "abc", ""...).
 * This class turns that text into real numbers (complaining nicely if something is wrong),
 * hands the numbers to DiskScheduler, and packs the answer into a Result for the controller.
 */
public final class DiskSchedulingModel {

    private DiskSchedulingModel() {
    }

    /** What came out of a run: the stops, distances, cumulative totals and total head movement. */
    public static final class Result {
        private final DiskScheduler.Stop[] stops;
        private final int[] dist;
        private final int[] cum;
        private final int total;
        private final int head;
        private final int tracks;
        private final int maxTrack;
        private final int[] queue;
        private final String algo;
        private final boolean up;

        Result(DiskScheduler.Stop[] stops, int[] dist, int[] cum, int total,
               int head, int tracks, int maxTrack, int[] queue, String algo, boolean up) {
            this.stops = stops;
            this.dist = dist;
            this.cum = cum;
            this.total = total;
            this.head = head;
            this.tracks = tracks;
            this.maxTrack = maxTrack;
            this.queue = queue;
            this.algo = algo;
            this.up = up;
        }

        public DiskScheduler.Stop[] getStops() {
            return stops;
        }

        public int[] getDist() {
            return dist;
        }

        public int[] getCum() {
            return cum;
        }

        public int getTotal() {
            return total;
        }

        public int getHead() {
            return head;
        }

        public int getTracks() {
            return tracks;
        }

        public int getMaxTrack() {
            return maxTrack;
        }

        public int[] getQueue() {
            return queue;
        }

        public String getAlgo() {
            return algo;
        }

        public boolean isUp() {
            return up;
        }

        public double average() {
            return queue.length == 0 ? 0 : (double) total / queue.length;
        }
    }

    /**
     * Reads the raw text from the screen, then runs the simulation.
     * Throws IllegalArgumentException on bad input.
     */
    public static Result run(String headText, String tracksText, int[] queue, String algo, boolean up) {
        if (headText == null || headText.trim().isEmpty())
            throw new IllegalArgumentException("Type the head position and the number of tracks first.");
        if (tracksText == null || tracksText.trim().isEmpty())
            throw new IllegalArgumentException("Type the head position and the number of tracks first.");
        int h = parseNonNegative(headText, "Head position");
        int tr = parseWhole(tracksText, "Number of tracks");
        if (tr < 2) throw new IllegalArgumentException("Tracks must be at least 2.");
        int max = tr - 1;
        if (h < 0 || h > max) throw new IllegalArgumentException("The head must start between 0 and " + max + ".");
        if (queue == null || queue.length == 0)
            throw new IllegalArgumentException("Add the requests first: press the + button in the Request queue box.");
        for (int r : queue) {
            if (r < 0 || r > max) throw new IllegalArgumentException("Track " + r + " is outside 0.." + max + ".");
        }

        DiskScheduler.Stop[] stops = DiskScheduler.schedule(queue, h, max, up, algo).toArray(new DiskScheduler.Stop[0]);
        int n = stops.length;
        int[] dist = new int[n];
        int[] cum = new int[n];
        int cur = h;
        int sum = 0;
        for (int i = 0; i < n; i++) {
            dist[i] = Math.abs(stops[i].track - cur);
            sum += dist[i];
            cum[i] = sum;
            cur = stops[i].track;
        }
        return new Result(stops, dist, cum, sum, h, tr, max, queue, algo, up);
    }

    /** Parses a whole number from text, throwing a friendly error. */
    public static int parseWhole(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
    }

    /** Parses a non-negative whole number from text, throwing a friendly error. */
    public static int parseNonNegative(String text, String what) {
        int value = parseWhole(text, what);
        if (value < 0) throw new IllegalArgumentException(what + " cannot be negative.");
        return value;
    }

    /** Parses a comma/space-separated list of track numbers. */
    public static int[] parseQueue(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return new int[0];
        String[] parts = t.split("[\\s,]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("\"" + parts[i] + "\" is not a track number.");
            }
        }
        return out;
    }
}
package opersys.diskscheduling;

import java.util.ArrayList;
import java.util.List;

/**
 * Disk-scheduling algorithms (FCFS, SSTF, SCAN, C-SCAN, LOOK). Pure logic with no UI, so the
 * answers can be checked on their own.
 *
 * Conventions (the usual textbook ones):
 *  - Tracks run from 0 to maxTrack. Head movement is the sum of |track differences|.
 *  - SSTF: if two requests are equally close, the one that arrived first is served first.
 *  - SCAN: the head always travels to the end of the disk in its current direction, reverses,
 *    and then stops at the last request on the way back (it does not go on to the other end).
 *  - C-SCAN: like SCAN, but after reaching the end the head jumps to the opposite end and
 *    sweeps the same way again. That jump IS counted as head movement.
 *  - LOOK: like SCAN, but it reverses at the last request instead of the disk edge.
 *  - A request on the head's own track is served first (0 movement).
 */
public final class DiskScheduler {
    public static final String[] ALGORITHMS = {"FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK"};

    /** Why the head stops at a track. */
    public enum Kind {
        REQUEST,   // a real request
        EDGE,      // the head ran to the end of the disk (SCAN / C-SCAN)
        JUMP       // C-SCAN's return jump to the opposite end
    }

    public static final class Stop {
        public final int track;
        public final Kind kind;

        Stop(int track, Kind kind) {
            this.track = track;
            this.kind = kind;
        }
    }

    private DiskScheduler() {
    }

    /** True for the algorithms where the starting direction changes the result. */
    public static boolean usesDirection(String algo) {
        return algo.equals("SCAN") || algo.equals("C-SCAN") || algo.equals("LOOK");
    }

    public static List<Stop> schedule(int[] requests, int head, int maxTrack, boolean up, String algo) {
        List<Stop> out = new ArrayList<>();
        switch (algo) {
            case "FCFS":
                for (int r : requests) out.add(new Stop(r, Kind.REQUEST));
                break;
            case "SSTF":
                sstf(requests, head, out);
                break;
            case "SCAN":
                sweep(requests, head, maxTrack, up, true, out);
                for (int r : behind(requests, head, up)) out.add(new Stop(r, Kind.REQUEST));
                break;
            case "LOOK":
                sweep(requests, head, maxTrack, up, false, out);
                for (int r : behind(requests, head, up)) out.add(new Stop(r, Kind.REQUEST));
                break;
            case "C-SCAN": {
                sweep(requests, head, maxTrack, up, true, out);
                List<Integer> wrapped = wrapped(requests, head, up);
                if (!wrapped.isEmpty()) {
                    out.add(new Stop(up ? 0 : maxTrack, Kind.JUMP));
                    for (int r : wrapped) out.add(new Stop(r, Kind.REQUEST));
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algo);
        }
        return out;
    }

    /** Total head movement, starting from the head's first position. */
    public static int totalMovement(int head, List<Stop> stops) {
        int total = 0;
        int cur = head;
        for (Stop s : stops) {
            total += Math.abs(s.track - cur);
            cur = s.track;
        }
        return total;
    }

    // ---- helpers ----

    private static void sstf(int[] requests, int head, List<Stop> out) {
        List<Integer> left = new ArrayList<>();
        for (int r : requests) left.add(r);
        int cur = head;
        while (!left.isEmpty()) {
            int best = 0;
            for (int i = 1; i < left.size(); i++) {
                if (Math.abs(left.get(i) - cur) < Math.abs(left.get(best) - cur)) best = i;   // strict: earlier arrival wins ties
            }
            cur = left.remove(best);
            out.add(new Stop(cur, Kind.REQUEST));
        }
    }

    /** The first sweep: requests in the direction of travel, then (optionally) the disk edge. */
    private static void sweep(int[] requests, int head, int maxTrack, boolean up, boolean toEdge, List<Stop> out) {
        List<Integer> ahead = new ArrayList<>();
        for (int r : requests) {
            if (up ? r >= head : r <= head) ahead.add(r);
        }
        ahead.sort(up ? (a, b) -> a - b : (a, b) -> b - a);
        for (int r : ahead) out.add(new Stop(r, Kind.REQUEST));
        if (toEdge) {
            int edge = up ? maxTrack : 0;
            boolean alreadyThere = !ahead.isEmpty() && ahead.get(ahead.size() - 1) == edge;
            if (!alreadyThere) out.add(new Stop(edge, Kind.EDGE));
        }
    }

    /** Requests on the other side of the head, nearest first (the way back for SCAN and LOOK). */
    private static List<Integer> behind(int[] requests, int head, boolean up) {
        List<Integer> list = new ArrayList<>();
        for (int r : requests) {
            if (up ? r < head : r > head) list.add(r);
        }
        list.sort(up ? (a, b) -> b - a : (a, b) -> a - b);
        return list;
    }

    /** Requests served after C-SCAN's jump: they are swept in the same direction as before. */
    private static List<Integer> wrapped(int[] requests, int head, boolean up) {
        List<Integer> list = new ArrayList<>();
        for (int r : requests) {
            if (up ? r < head : r > head) list.add(r);
        }
        list.sort(up ? (a, b) -> a - b : (a, b) -> b - a);
        return list;
    }
}
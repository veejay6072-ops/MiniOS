package opersys.segmentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentation logic only: a segment table plus a translator that maps a
 * (segment, offset) logical address to a physical address or a segmentation fault.
 *
 * Nothing in this file knows about Swing, so it can be tested or reused on its own.
 */
public final class SegmentationEngine {

    private SegmentationEngine() {
    }

    public static SegmentationEngine create() {
        return new SegmentationEngine();
    }

    /** One segment: a name, a base address and a limit (size). */
    public static final class Segment {
        public final String name;
        public final int base;
        public final int limit;

        public Segment(String name, int base, int limit) {
            this.name = name;
            this.base = base;
            this.limit = limit;
        }

        @Override
        public String toString() {
            return name + " (base=" + base + ", limit=" + limit + ")";
        }
    }

    /** Outcome of the last translation. */
    public static final class Result {
        public static final int OK = 0, FAULT = 1, NO_SEGMENT = 2;

        public final int kind;
        public final int seg;
        public final int offset;
        public final int limit;
        public final int base;
        public final int physical;

        private Result(int kind, int seg, int offset, int limit, int base, int physical) {
            this.kind = kind;
            this.seg = seg;
            this.offset = offset;
            this.limit = limit;
            this.base = base;
            this.physical = physical;
        }

        public static Result ok(int seg, int offset, int limit, int base, int physical) {
            return new Result(OK, seg, offset, limit, base, physical);
        }

        public static Result fault(int seg, int offset, int limit, int base) {
            return new Result(FAULT, seg, offset, limit, base, -1);
        }

        public static Result noSegment(int seg, int offset) {
            return new Result(NO_SEGMENT, seg, offset, -1, -1, -1);
        }

        public boolean isOk() {
            return kind == OK;
        }

        public boolean isFault() {
            return kind == FAULT;
        }

        public boolean isNoSegment() {
            return kind == NO_SEGMENT;
        }
    }

    private final List<Segment> segments = new ArrayList<>();

    public List<Segment> getSegments() {
        return segments;
    }

    public int size() {
        return segments.size();
    }

    public Segment get(int index) {
        return segments.get(index);
    }

    public void add(Segment segment) {
        segments.add(segment);
    }

    public void remove(int index) {
        segments.remove(index);
    }

    public void clear() {
        segments.clear();
    }

    /** The highest physical address touched by any segment, rounded up to the next 1 KB. */
    public int total() {
        int end = 4000;
        for (Segment s : segments) {
            end = Math.max(end, s.base + s.limit);
        }
        return (int) (Math.ceil(end / 1000.0) * 1000);
    }

    /** Sum of all segment limits (the amount of memory in use). */
    public int used() {
        int sum = 0;
        for (Segment s : segments) {
            sum += s.limit;
        }
        return sum;
    }

    /**
     * Translates a logical address (segment number + offset) into a physical address.
     *
     * @param seg    zero-based segment index
     * @param offset offset inside the segment
     * @return the translation result (never null)
     */
    public Result translate(int seg, int offset) {
        if (seg < 0 || seg >= segments.size()) {
            return Result.noSegment(seg, offset);
        }
        Segment s = segments.get(seg);
        if (offset < 0 || offset >= s.limit) {
            return Result.fault(seg, offset, s.limit, s.base);
        }
        return Result.ok(seg, offset, s.limit, s.base, s.base + offset);
    }
}
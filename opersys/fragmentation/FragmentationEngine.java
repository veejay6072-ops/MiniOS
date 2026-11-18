package opersys.fragmentation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Allocation logic for first, next, best, and worst fit. */
public final class FragmentationEngine {
    private static final int MAX_MEMORY = 1000;

    private int total;
    private boolean created;
    private int[] memory = new int[0];
    private int nextId;
    private int rover;

    public static final class State {
        public final int total;
        public final boolean created;
        public final int[] memory;
        public final int nextId;
        public final List<int[]> holes;
        public final List<int[]> runs;

        private State(int total, boolean created, int[] memory, int nextId,
                      List<int[]> holes, List<int[]> runs) {
            this.total = total;
            this.created = created;
            this.memory = memory;
            this.nextId = nextId;
            this.holes = holes;
            this.runs = runs;
        }
    }

    public static final class DropResult {
        public final FragmentationStep step;
        public final boolean placed;
        public final boolean fragmented;
        public final int pieceId;
        public final int size;
        public final int free;
        public final int biggest;
        public final int start;
        public final int holeSize;
        public final int leftover;
        public final String algorithm;

        private DropResult(FragmentationStep step, boolean placed, boolean fragmented, int pieceId,
                           int size, int free, int biggest, int start, int holeSize, int leftover,
                           String algorithm) {
            this.step = step;
            this.placed = placed;
            this.fragmented = fragmented;
            this.pieceId = pieceId;
            this.size = size;
            this.free = free;
            this.biggest = biggest;
            this.start = start;
            this.holeSize = holeSize;
            this.leftover = leftover;
            this.algorithm = algorithm;
        }
    }

    public static final class CompactResult {
        public final FragmentationStep step;
        public final boolean compacted;
        public final int unused;

        private CompactResult(FragmentationStep step, boolean compacted, int unused) {
            this.step = step;
            this.compacted = compacted;
            this.unused = unused;
        }
    }

    public static FragmentationEngine create() {
        return new FragmentationEngine();
    }

    private FragmentationEngine() {
    }

    public State snapshot() {
        return new State(total, created, memory.clone(), nextId,
                copyRanges(holes()), copyRanges(runs()));
    }

    public FragmentationStep createMemory(int size) {
        if (size < 1 || size > MAX_MEMORY) {
            throw new IllegalArgumentException("Memory size must be between 1 and " + MAX_MEMORY + " KB.");
        }
        FragmentationStep step = newStep("CREATE", "Memory of " + size + " KB");
        total = size;
        memory = new int[size];
        resetMemory();
        created = true;
        step.kind = 2;
        step.addLine(FragmentationStep.INFO, "A memory of " + size + " KB was created and it is completely unused");
        step.addLine(FragmentationStep.INFO, "Type a piece size and press DROP to place the first piece (A)");
        finish(step, "Created memory: " + size + " KB");
        return step;
    }

    public DropResult drop(int size, String algorithm) {
        if (!created) throw new IllegalStateException("Create memory before placing a piece.");
        if (size < 1 || size > total) throw new IllegalArgumentException("Piece size must be between 1 and " + total + " KB.");
        String algo = algorithm == null ? "First Fit" : algorithm;
        int id = nextId;
        char name = letter(id);
        List<int[]> availableHoles = holes();
        int free = 0;
        int biggest = 0;
        for (int[] hole : availableHoles) {
            free += hole[1];
            biggest = Math.max(biggest, hole[1]);
        }

        FragmentationStep step = newStep("DROP", "Piece " + name + " asks for " + size + " KB (" + algo + ")");
        step.addLine(FragmentationStep.INFO, "Unused space: " + free + " KB in " + availableHoles.size()
                + " hole(s), biggest hole " + biggest + " KB");
        int chosen = choose(availableHoles, size, algo, step);
        DropResult result;
        String outcome;
        if (chosen < 0) {
            step.kind = 1;
            step.addLine(FragmentationStep.FAIL, free >= size
                    ? "No hole is big enough, but " + free + " KB is unused in total"
                    : "Only " + free + " KB is unused in total, so memory is simply full");
            step.addLine(FragmentationStep.INFO, free >= size
                    ? "External fragmentation: the unused space is cut into small holes. COMPACT would fix it."
                    : "COMPACT will not help, there is not enough space at all.");
            outcome = "CAN'T FIT";
            result = new DropResult(step, false, free >= size, id, size, free, biggest,
                    -1, 0, 0, algo);
        } else {
            int[] hole = availableHoles.get(chosen);
            for (int k = 0; k < size; k++) memory[hole[0] + k] = id;
            nextId++;
            rover = hole[0] + size;
            int leftover = hole[1] - size;
            step.kind = 2;
            step.addLine(FragmentationStep.INFO, "Piece " + name + " takes blocks "
                    + (hole[0] + 1) + "-" + (hole[0] + size));
            step.addLine(FragmentationStep.INFO, leftover > 0
                    ? "Leftover " + leftover + " KB stays as an unused hole"
                    : "The hole is used up exactly, nothing is left over");
            outcome = "placed at blocks " + (hole[0] + 1) + "-" + (hole[0] + size);
            result = new DropResult(step, true, false, id, size, free, biggest,
                    hole[0], hole[1], leftover, algo);
        }
        finish(step, "Piece " + name + ", " + size + " KB, " + algo + ": " + outcome);
        return result;
    }

    public CompactResult compact() {
        if (!created) throw new IllegalStateException("Create memory before compacting.");
        FragmentationStep step = newStep("COMPACT", "Slide every piece to the left");
        List<int[]> before = holes();
        step.addLine(FragmentationStep.INFO, "Before: " + before.size() + " unused hole(s)");
        boolean compacted = !(before.size() <= 1
                && (before.isEmpty() || before.get(0)[0] + before.get(0)[1] == total));
        int unused;
        if (!compacted) {
            step.kind = 0;
            step.addLine(FragmentationStep.INFO, "Nothing to do, all pieces are already packed to the left");
            unused = before.isEmpty() ? 0 : before.get(0)[1];
            finish(step, "Compact: nothing to do");
        } else {
            int write = 0;
            for (int i = 0; i < total; i++) {
                if (memory[i] != -1) {
                    int piece = memory[i];
                    memory[i] = -1;
                    memory[write++] = piece;
                }
            }
            rover = write;
            unused = total - write;
            step.kind = 2;
            step.addLine(FragmentationStep.INFO, "Every piece slid to the left and kept its order");
            step.addLine(FragmentationStep.PICK, "Unused space is now one hole: blocks "
                    + (write + 1) + "-" + total + " (" + unused + " KB)");
            finish(step, "Compact: unused space is one hole of " + unused + " KB");
        }
        return new CompactResult(step, compacted, unused);
    }

    public FragmentationStep reset() {
        if (!created) throw new IllegalStateException("Create memory before resetting.");
        FragmentationStep step = newStep("RESET", "Memory cleared");
        resetMemory();
        step.kind = 0;
        step.addLine(FragmentationStep.INFO, "Every piece was removed, all " + total + " KB are unused again");
        step.addLine(FragmentationStep.INFO, "The next piece is A again");
        finish(step, "Memory reset");
        return step;
    }

    private void resetMemory() {
        for (int i = 0; i < memory.length; i++) memory[i] = -1;
        nextId = 0;
        rover = 0;
    }

    private List<int[]> holes() {
        List<int[]> list = new ArrayList<>();
        int i = 0;
        while (i < total) {
            if (memory[i] == -1) {
                int start = i;
                while (i < total && memory[i] == -1) i++;
                list.add(new int[]{start, i - start});
            } else {
                i++;
            }
        }
        return list;
    }

    private List<int[]> runs() {
        List<int[]> list = new ArrayList<>();
        int i = 0;
        while (i < total) {
            int start = i;
            int id = memory[i];
            while (i < total && memory[i] == id) i++;
            list.add(new int[]{start, i - start, id});
        }
        return list;
    }

    private int choose(List<int[]> holes, int size, String algorithm, FragmentationStep step) {
        if (algorithm.equals("Next Fit")) {
            step.addLine(FragmentationStep.INFO, "Next Fit starts at block " + (rover + 1)
                    + " (where the last piece ended) and wraps around");
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < holes.size(); i++) if (holes.get(i)[0] >= rover) order.add(i);
            int wrapAt = order.size();
            for (int i = 0; i < holes.size(); i++) if (holes.get(i)[0] < rover) order.add(i);
            for (int pos = 0; pos < order.size(); pos++) {
                if (pos == wrapAt && wrapAt > 0)
                    step.addLine(FragmentationStep.INFO, "Reached the end of memory, going back to the start");
                int i = order.get(pos);
                String label = holeLabel(holes, i);
                if (holes.get(i)[1] < size) {
                    step.addLine(FragmentationStep.SKIP, label + " - too small");
                    continue;
                }
                step.addLine(FragmentationStep.PICK, label + " - fits, Next Fit takes it and stops");
                return i;
            }
            return -1;
        }

        int chosen = -1;
        for (int i = 0; i < holes.size(); i++) {
            int length = holes.get(i)[1];
            String label = holeLabel(holes, i);
            if (length < size) {
                step.addLine(FragmentationStep.SKIP, label + " - too small");
                continue;
            }
            if (chosen == -1) {
                chosen = i;
                if (algorithm.equals("First Fit")) {
                    step.addLine(FragmentationStep.PICK, label + " - fits, First Fit takes the first one and stops");
                    return i;
                }
                step.addLine(FragmentationStep.TRY, label + " - fits, first candidate");
                continue;
            }
            int current = holes.get(chosen)[1];
            boolean best = algorithm.equals("Best Fit");
            boolean better = best ? length < current : length > current;
            if (better) {
                chosen = i;
                step.addLine(FragmentationStep.TRY, label + " - fits and is "
                        + (best ? "smaller" : "bigger") + " than " + current + " KB, new choice");
            } else {
                step.addLine(FragmentationStep.SKIP, label + " - fits but is not "
                        + (best ? "smaller" : "bigger") + " than " + current + " KB, skip");
            }
        }
        if (chosen >= 0)
            step.addLine(FragmentationStep.PICK,
                    "Winner: hole " + (chosen + 1) + " (" + holes.get(chosen)[1] + " KB)");
        return chosen;
    }

    private void finish(FragmentationStep step, String summary) {
        step.after = memory.clone();
        step.stats = statsLine();
        step.summary = summary;
    }

    private String statsLine() {
        List<int[]> currentHoles = holes();
        int free = 0;
        int biggest = 0;
        for (int[] hole : currentHoles) {
            free += hole[1];
            biggest = Math.max(biggest, hole[1]);
        }
        int fragmented = free == 0 ? 0 : Math.round((1f - biggest / (float) free) * 100);
        return "Unused " + free + " KB, biggest hole " + biggest + " KB, "
                + currentHoles.size() + " holes, " + fragmented + "% fragmented";
    }

    private FragmentationStep newStep(String badge, String title) {
        return new FragmentationStep(new SimpleDateFormat("HH:mm:ss").format(new Date()),
                badge, title, memory);
    }

    private static char letter(int id) {
        return (char) ('A' + id % 26);
    }

    private static String holeLabel(List<int[]> holes, int index) {
        int start = holes.get(index)[0];
        int length = holes.get(index)[1];
        return "Hole " + (index + 1) + ": blocks " + (start + 1) + "-" + (start + length)
                + " (" + length + " KB)";
    }

    private static List<int[]> copyRanges(List<int[]> ranges) {
        List<int[]> copy = new ArrayList<>(ranges.size());
        for (int[] range : ranges) copy.add(range.clone());
        return copy;
    }
}

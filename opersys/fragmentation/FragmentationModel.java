package opersys.fragmentation;

import java.util.ArrayList;
import java.util.List;

/** Validates fragmentation input, coordinates the engine, and keeps the view-ready state. */
public final class FragmentationModel {
    private final FragmentationEngine engine = FragmentationEngine.create();
    private final FragmentationHistory history = FragmentationHistory.shared();
    private FragmentationStep latestStep;

    public static final class Snapshot {
        public final int total;
        public final boolean created;
        public final int[] memory;
        public final int nextId;
        public final int previewSize;
        public final List<int[]> holes;
        public final List<int[]> runs;
        public final List<FragmentationStep> history;
        public final FragmentationStep latestStep;

        private Snapshot(FragmentationEngine.State state, int previewSize,
                         List<FragmentationStep> history, FragmentationStep latestStep) {
            total = state.total;
            created = state.created;
            memory = state.memory;
            nextId = state.nextId;
            this.previewSize = previewSize;
            holes = state.holes;
            runs = state.runs;
            this.history = history;
            this.latestStep = latestStep;
        }
    }

    public static final class Feedback {
        public final String error;
        public final String title;
        public final String body;
        public final int kind;

        private Feedback(String error, String title, String body, int kind) {
            this.error = error;
            this.title = title;
            this.body = body;
            this.kind = kind;
        }

        private static Feedback message(String title, String body, int kind) {
            return new Feedback(null, title, body, kind);
        }

        private static Feedback error(String message) {
            return new Feedback(message, null, null, 0);
        }
    }

    public Snapshot snapshot(String pieceText) {
        FragmentationEngine.State state = engine.snapshot();
        return new Snapshot(state, previewSize(pieceText, state), history.entries(), latestStep);
    }

    public Feedback createMemory(String sizeText) {
        int size;
        try {
            size = Integer.parseInt(sizeText == null ? "" : sizeText.trim());
        } catch (NumberFormatException ex) {
            return Feedback.message("MEMORY SIZE",
                    "Type the memory size as a whole number of KB, for example 72.", 1);
        }
        if (size < 1) {
            return Feedback.message("MEMORY SIZE", "The memory size must be at least 1 KB.", 1);
        }
        record(engine.createMemory(size));
        return Feedback.message("MEMORY CREATED!", "Your memory is " + size + " KB and completely empty."
                + "<br>Type a piece size, pick an algorithm and press DROP.", 2);
    }

    public Feedback drop(String pieceText, String algorithm) {
        FragmentationEngine.State state = engine.snapshot();
        if (!state.created) {
            return Feedback.message("CREATE FIRST",
                    "There is no memory yet.<br>Type a memory size on the left and press CREATE.", 1);
        }
        int size;
        try {
            size = Integer.parseInt(pieceText == null ? "" : pieceText.trim());
        } catch (NumberFormatException ex) {
            return Feedback.error("Piece size must be a whole number.");
        }
        if (size < 1 || size > state.total) {
            return Feedback.error("Piece size must be between 1 and " + state.total + " KB.");
        }

        FragmentationEngine.DropResult result = engine.drop(size, algorithm);
        record(result.step);
        char name = letter(result.pieceId);
        if (!result.placed && result.fragmented) {
            return Feedback.message("CAN'T FIT!", String.format("Piece %c needs %d KB. There are %d KB unused, but the biggest hole is only %d KB."
                    + "<br>The unused space is cut into small pieces. This is external fragmentation.",
                    name, size, result.free, result.biggest), 1);
        }
        if (!result.placed) {
            return Feedback.message("CAN'T FIT!", String.format("Piece %c needs %d KB but only %d KB is unused in total."
                    + "<br>Memory is full, so compacting will not help.", name, size, result.free), 1);
        }
        return Feedback.message("PLACED!", String.format("Piece %c (%d KB) placed in blocks %d-%d using %s."
                + "<br>The hole it used was %d KB, leftover %d KB.",
                name, size, result.start + 1, result.start + size, result.algorithm,
                result.holeSize, result.leftover), 2);
    }

    public Feedback compact() {
        if (!engine.snapshot().created) return noMemory();
        FragmentationEngine.CompactResult result = engine.compact();
        record(result.step);
        if (!result.compacted) {
            return Feedback.message("COMPACT", "Nothing to compact. All pieces are already packed to the left.", 0);
        }
        return Feedback.message("COMPACTED!", "All pieces slid to the left. The unused space is now one big hole."
                + "<br>In a real OS this costs time, because every moved piece has to be copied.", 2);
    }

    public Feedback deleteProcess(int pieceId) {
        FragmentationEngine.State state = engine.snapshot();
        if (!state.created) return noMemory();
        boolean found = false;
        for (int block : state.memory) {
            if (block == pieceId) {
                found = true;
                break;
            }
        }
        if (!found) return Feedback.error("That process is no longer in memory.");

        FragmentationStep step = engine.deleteProcess(pieceId);
        record(step);
        return Feedback.message("PROCESS DELETED", step.summary + ". The freed space is available for new processes.", 2);
    }

    public Feedback reset() {
        if (!engine.snapshot().created) return noMemory();
        record(engine.reset());
        return Feedback.message("RESET", "Memory is empty again. Type a piece size and press DROP.", 0);
    }

    public void clearHistory() {
        history.clear();
    }

    private void record(FragmentationStep step) {
        history.add(step);
        latestStep = step;
    }

    private static Feedback noMemory() {
        return Feedback.message("CREATE FIRST",
                "There is no memory yet.<br>Type a memory size on the left and press CREATE.", 1);
    }

    private static int previewSize(String text, FragmentationEngine.State state) {
        try {
            int size = Integer.parseInt(text == null ? "" : text.trim());
            return size >= 1 && (!state.created || size <= state.total) ? size : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static char letter(int id) {
        return (char) ('A' + id % 26);
    }
}

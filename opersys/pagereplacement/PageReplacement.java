package opersys.pagereplacement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Page replacement LOGIC only: FIFO, LRU and Optimal (OPT).
 * Nothing in this file knows about Swing. The GUI (PageReplacementApp) calls {@link #run}
 * and draws the {@link Result}.
 */
public final class PageReplacement {
    private PageReplacement() {
    }

    /** Marks a tab (frame) that is still empty. */
    public static final int EMPTY = -1;

    public enum Algorithm {
        FIFO, LRU, OPT
    }

    /** One page request and what the tabs looked like afterwards. */
    public static final class Step {
        public final int index;        // position in the reference string, 0-based
        public final int page;         // page that was asked for
        public final boolean hit;      // true = the page was already open
        public final int[] slots;      // tab contents after this step (EMPTY for a free tab)
        public final int loadedSlot;   // tab that received the page on a fault, -1 on a hit
        public final int victim;       // page that was closed to make room, EMPTY if none
        public final String message;   // plain-language explanation of this step

        Step(int index, int page, boolean hit, int[] slots, int loadedSlot, int victim, String message) {
            this.index = index;
            this.page = page;
            this.hit = hit;
            this.slots = slots;
            this.loadedSlot = loadedSlot;
            this.victim = victim;
            this.message = message;
        }
    }

    /** The whole simulation. */
    public static final class Result {
        public final Algorithm algorithm;
        public final int frames;
        public final List<Step> steps;
        public final int hits;
        public final int faults;

        Result(Algorithm algorithm, int frames, List<Step> steps, int hits, int faults) {
            this.algorithm = algorithm;
            this.frames = frames;
            this.steps = steps;
            this.hits = hits;
            this.faults = faults;
        }

        /** Hits as a percentage of all page requests. */
        public double hitRatio() {
            return steps.isEmpty() ? 0 : hits * 100.0 / steps.size();
        }
    }

    /**
     * Runs one simulation.
     *
     * @throws IllegalArgumentException with a readable message if the input is not valid
     */
    public static Result run(Algorithm algorithm, int[] refs, int frames) {
        if (algorithm == null) throw new IllegalArgumentException("Choose an algorithm.");
        if (refs == null || refs.length == 0) throw new IllegalArgumentException("Enter at least one page number.");
        if (frames <= 0) throw new IllegalArgumentException("The number of frames must be greater than 0.");
        for (int r : refs) {
            if (r < 0) throw new IllegalArgumentException("Page numbers cannot be negative.");
        }

        int[] slots = new int[frames];
        int[] loadedAt = new int[frames];   // when each tab received its page (for FIFO)
        int[] lastUsed = new int[frames];   // when each tab was last used (for LRU)
        Arrays.fill(slots, EMPTY);

        List<Step> steps = new ArrayList<>();
        int hits = 0;
        int faults = 0;

        for (int i = 0; i < refs.length; i++) {
            int page = refs[i];
            int found = indexOf(slots, page);

            if (found >= 0) {   // HIT
                hits++;
                lastUsed[found] = i;
                steps.add(new Step(i, page, true, copy(slots), -1, EMPTY,
                        "Page " + page + " was already open, so it is a hit."));
                continue;
            }

            // FAULT
            faults++;
            int slot = indexOf(slots, EMPTY);
            int victim = EMPTY;
            String message;
            if (slot >= 0) {
                message = "Page " + page + " was not open, so a page fault happened. It opened in an empty tab.";
            } else {
                slot = chooseVictim(algorithm, slots, loadedAt, lastUsed, refs, i);
                victim = slots[slot];
                message = "Page " + page + " was not open, so a page fault happened. "
                        + algorithm + " closed " + describe(algorithm) + ", Page " + victim + ".";
            }
            slots[slot] = page;
            loadedAt[slot] = i;
            lastUsed[slot] = i;
            steps.add(new Step(i, page, false, copy(slots), slot, victim, message));
        }

        return new Result(algorithm, frames, Collections.unmodifiableList(steps), hits, faults);
    }

    /** Picks the tab to close when every tab is full. */
    private static int chooseVictim(Algorithm algorithm, int[] slots, int[] loadedAt, int[] lastUsed,
                                    int[] refs, int now) {
        int best = 0;
        for (int s = 1; s < slots.length; s++) {
            switch (algorithm) {
                case FIFO:
                    if (loadedAt[s] < loadedAt[best]) best = s;
                    break;
                case LRU:
                    if (lastUsed[s] < lastUsed[best]) best = s;
                    break;
                default: // OPT: the page needed farthest in the future (or never again)
                    if (nextUse(refs, slots[s], now) > nextUse(refs, slots[best], now)) best = s;
                    break;
            }
        }
        return best;
    }

    private static int nextUse(int[] refs, int page, int now) {
        for (int j = now + 1; j < refs.length; j++) {
            if (refs[j] == page) return j;
        }
        return Integer.MAX_VALUE;
    }

    private static String describe(Algorithm algorithm) {
        switch (algorithm) {
            case FIFO:
                return "the oldest tab";
            case LRU:
                return "the least recently used tab";
            default:
                return "the tab that will not be needed for the longest time";
        }
    }

    private static int indexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) return i;
        }
        return -1;
    }

    private static int[] copy(int[] values) {
        return Arrays.copyOf(values, values.length);
    }
}
package opersys.bankers;

import opersys.DecimalInput;
import java.util.List;

/**
 * BANK MODEL = the middleman between the screen and the algorithm. LOGIC ONLY, no Swing here.
 *
 * The screen only has text boxes, so everything arrives as text ("7", "abc", ""...).
 * This class turns that text into real numbers (complaining nicely if something is wrong),
 * hands the numbers to BankAlgorithm, and packs the answer into a Result for the controller.
 */
public final class BankModel {

    private BankModel() {}   // nobody makes one of these, it's just a toolbox of static methods

    /** What came out of a safety check: safe or not, the safe order, the step-by-step story, and the numbers used. */
    public static final class Result {
        private final boolean safe;
        private final List<Integer> order;
        private final List<BankStep> steps;
        private final double[][] allocation;
        private final double[][] max;
        private final double[] available;

        Result(boolean safe, List<Integer> order, List<BankStep> steps,
               double[][] allocation, double[][] max, double[] available) {
            this.safe = safe;
            this.order = order;
            this.steps = steps;
            this.allocation = allocation;
            this.max = max;
            this.available = available;
        }

        /** True = the bank is in a safe state. */
        public boolean isSafe() {
            return safe;
        }

        /** The safe order of customers (empty when unsafe). */
        public List<Integer> getOrder() {
            return order;
        }

        /** Every question the algorithm asked while checking, in order (for the Steps window). */
        public List<BankStep> getSteps() {
            return steps;
        }

        /** The numbers that were checked (so the controller can save them in the history). */
        public double[][] getAllocation() {
            return allocation;
        }

        public double[][] getMax() {
            return max;
        }

        public double[] getAvailable() {
            return available;
        }
    }

    /** Reads the raw text from the screen, then runs the safety check. Throws IllegalArgumentException on bad input. */
    public static Result check(String[][] allocation, String[][] max, String[] available) {
        int processes = max.length;
        int resources = available.length;

        if (processes == 0 || resources == 0)
            throw new IllegalArgumentException("Need at least one process and one resource.");
        if (allocation.length != processes)
            throw new IllegalArgumentException("Allocated table must have " + processes + " rows.");

        // text -> numbers (this is where "abc" or a negative number gets rejected)
        double[][] alloc = parseMatrix(allocation, processes, resources, "Allocated");
        double[][] maxValues = parseMatrix(max, processes, resources, "Max");
        double[] avail = parseVector(available, "Available");

        BankAlgorithm bank = new BankAlgorithm(avail, maxValues, alloc);
        List<Integer> order = bank.findSafeSequence();
        List<BankStep> steps = bank.trace();
        return new Result(!order.isEmpty(), order, steps, alloc, maxValues, avail);
    }

    /** Need = Max - Allocated, as text for display. Returns "-" if either value is invalid. */
    public static String needText(String max, String allocated) {
        try {
            return DecimalInput.format(DecimalInput.parseDecimal(max, "Max")
                    - DecimalInput.parseDecimal(allocated, "Allocated"));
        } catch (IllegalArgumentException e) {
            return "-";
        }
    }

    /** Turns text into a decimal amount that is 0 or more. "what" names the value in errors. */
    public static double parseNonNegative(String text, String what) {
        double value = DecimalInput.parseDecimal(text, what);
        if (value < 0) throw new IllegalArgumentException(what + " cannot be negative.");
        return value;
    }

    /** A table of text -> a table of resource amounts, checking every row has the right number of values. */
    private static double[][] parseMatrix(String[][] text, int rows, int cols, String name) {
        double[][] out = new double[rows][cols];
        for (int p = 0; p < rows; p++) {
            if (text[p].length != cols)
                throw new IllegalArgumentException(name + " row P" + p + " must have " + cols + " values.");
            for (int r = 0; r < cols; r++)
                out[p][r] = parseNonNegative(text[p][r], name + " of P" + p + " (" + letter(r) + ")");
        }
        return out;
    }

    /** One row of text (the Available boxes) -> numbers. */
    private static double[] parseVector(String[] text, String name) {
        double[] out = new double[text.length];
        for (int r = 0; r < text.length; r++)
            out[r] = parseNonNegative(text[r], name + " " + letter(r));
        return out;
    }

    /** 0 -> "A", 1 -> "B", ... (resource names) */
    private static String letter(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
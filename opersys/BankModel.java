import java.util.List;

/** LOGIC ONLY. No Swing here. Validates raw text input and runs BankersAlgorithm. */
public final class BankModel {

    private BankModel() {}

    /** Outcome of a safety check. */
    public static final class Result {
        private final boolean safe;
        private final List<Integer> order;

        Result(boolean safe, List<Integer> order) {
            this.safe = safe;
            this.order = order;
        }

        public boolean isSafe() {
            return safe;
        }

        public List<Integer> getOrder() {
            return order;
        }
    }

    /** Parses the raw text from the UI, then runs the safety check. Throws IllegalArgumentException on bad input. */
    public static Result check(String[][] allocation, String[][] max, String[] available) {
        int processes = max.length;
        int resources = available.length;

        if (processes == 0 || resources == 0)
            throw new IllegalArgumentException("Need at least one process and one resource.");
        if (allocation.length != processes)
            throw new IllegalArgumentException("Allocated table must have " + processes + " rows.");

        int[][] alloc = parseMatrix(allocation, processes, resources, "Allocated");
        int[][] maxValues = parseMatrix(max, processes, resources, "Max");
        int[] avail = parseVector(available, "Available");

        BankersAlgorithm bank = new BankersAlgorithm(avail, maxValues, alloc);
        List<Integer> order = bank.findSafeSequence();
        return new Result(!order.isEmpty(), order);
    }

    /** Need = Max - Allocated, as text for display. Returns "-" if either value is not a whole number. */
    public static String needText(String max, String allocated) {
        try {
            return String.valueOf(Integer.parseInt(max.trim()) - Integer.parseInt(allocated.trim()));
        } catch (NumberFormatException e) {
            return "-";
        }
    }

    public static int parseNonNegative(String text, String what) {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
        if (value < 0) throw new IllegalArgumentException(what + " cannot be negative.");
        return value;
    }

    private static int[][] parseMatrix(String[][] text, int rows, int cols, String name) {
        int[][] out = new int[rows][cols];
        for (int p = 0; p < rows; p++) {
            if (text[p].length != cols)
                throw new IllegalArgumentException(name + " row P" + p + " must have " + cols + " values.");
            for (int r = 0; r < cols; r++)
                out[p][r] = parseNonNegative(text[p][r], name + " of P" + p + " (" + letter(r) + ")");
        }
        return out;
    }

    private static int[] parseVector(String[] text, String name) {
        int[] out = new int[text.length];
        for (int r = 0; r < text.length; r++)
            out[r] = parseNonNegative(text[r], name + " " + letter(r));
        return out;
    }

    private static String letter(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
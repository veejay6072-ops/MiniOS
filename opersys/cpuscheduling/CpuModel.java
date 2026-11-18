package opersys.cpuscheduling;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU MODEL = the middleman between the screen and the scheduler. LOGIC ONLY, no Swing here.
 *
 * The screen only has text boxes, so everything arrives as text. This class turns that text into
 * real numbers (complaining nicely if something is wrong), hands the numbers to CpuScheduler,
 * and packs the answer into a Result for the controller.
 */
public final class CpuModel {

    private CpuModel() {
    }

    /** What came out of a simulation: the algorithm, the Gantt chart, the per-process numbers. */
    public static final class Result {
        private final CpuScheduler.Algorithm algorithm;
        private final int quantum;
        private final List<CpuScheduler.Slice> slices;
        private final List<CpuScheduler.Row> rows;
        private final int totalTime;
        private final double averageWaiting;
        private final double averageTurnaround;

        Result(CpuScheduler.Algorithm algorithm, int quantum, List<CpuScheduler.Slice> slices,
               List<CpuScheduler.Row> rows, int totalTime, double averageWaiting, double averageTurnaround) {
            this.algorithm = algorithm;
            this.quantum = quantum;
            this.slices = slices;
            this.rows = rows;
            this.totalTime = totalTime;
            this.averageWaiting = averageWaiting;
            this.averageTurnaround = averageTurnaround;
        }

        public CpuScheduler.Algorithm getAlgorithm() {
            return algorithm;
        }

        public int getQuantum() {
            return quantum;
        }

        public List<CpuScheduler.Slice> getSlices() {
            return slices;
        }

        public List<CpuScheduler.Row> getRows() {
            return rows;
        }

        public int getTotalTime() {
            return totalTime;
        }

        public double getAverageWaiting() {
            return averageWaiting;
        }

        public double getAverageTurnaround() {
            return averageTurnaround;
        }
    }

    /** One process typed in by the user (as text). */
    public static final class JobInput {
        private final String name;
        private final String arrival;
        private final String burst;
        private final String priority;

        JobInput(String name, String arrival, String burst, String priority) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public String getArrival() {
            return arrival;
        }

        public String getBurst() {
            return burst;
        }

        public String getPriority() {
            return priority;
        }
    }

    /**
     * Reads the raw text from the screen, then runs the simulation.
     * Throws IllegalArgumentException on bad input.
     */
    public static Result run(CpuScheduler.Algorithm algorithm, int quantum,
                             List<JobInput> jobs) {
        if (algorithm == null) throw new IllegalArgumentException("Choose an algorithm.");
        if (jobs == null || jobs.isEmpty())
            throw new IllegalArgumentException("Add at least one process first.");

        List<CpuScheduler.Job> parsed = new ArrayList<>();
        for (JobInput j : jobs) {
            String name = j.getName().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Every process needs a name.");
            int arrival = parseNonNegative(j.getArrival(), "Arrival time of " + name);
            int burst = parsePositive(j.getBurst(), "Burst time of " + name);
            int priority = 0;
            if (algorithm == CpuScheduler.Algorithm.PRIORITY) {
                priority = parseNonNegative(j.getPriority(), "Priority of " + name);
            }
            parsed.add(new CpuScheduler.Job(name, arrival, burst, priority));
        }

        if (algorithm == CpuScheduler.Algorithm.ROUND_ROBIN && quantum <= 0)
            throw new IllegalArgumentException("The time quantum must be greater than 0.");

        CpuScheduler.Result schedulerResult = CpuScheduler.run(algorithm, parsed, quantum);
        return new Result(algorithm, schedulerResult.quantum, schedulerResult.slices,
                schedulerResult.rows, schedulerResult.totalTime,
                schedulerResult.averageWaiting, schedulerResult.averageTurnaround);
    }

    /** A whole number that is 0 or more. */
    public static int parseNonNegative(String text, String what) {
        if (text == null || text.trim().isEmpty())
            throw new IllegalArgumentException(what + " is empty.");
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
        if (value < 0) throw new IllegalArgumentException(what + " cannot be negative.");
        return value;
    }

    /** A whole number that is 1 or more. */
    public static int parsePositive(String text, String what) {
        int value = parseNonNegative(text, what);
        if (value <= 0) throw new IllegalArgumentException(what + " must be greater than 0.");
        return value;
    }
}
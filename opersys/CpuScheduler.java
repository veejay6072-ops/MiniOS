import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * The CPU scheduling LOGIC only: FCFS, SJF, Priority (non-preemptive) and Round Robin.
 * Nothing in this file knows about Swing, so it can be tested or reused without any GUI.
 * The GUI (CpuSchedulingApp) just calls {@link #run} and draws the {@link Result}.
 */
public final class CpuScheduler {
    private CpuScheduler() {
    }

    public enum Algorithm {
        FCFS("FCFS"),
        SJF("SJF"),
        PRIORITY("Priority"),
        ROUND_ROBIN("Round Robin");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** One process typed in by the user. A lower priority number means more important. */
    public static final class Job {
        public final String name;
        public final int arrival;
        public final int burst;
        public final int priority;

        public Job(String name, int arrival, int burst, int priority) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
        }
    }

    /** A stretch of time on the CPU (one block of the Gantt chart). */
    public static final class Slice {
        public static final String IDLE = "IDLE";
        public final String name;
        public final int start;
        public final int end;

        public Slice(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        public boolean isIdle() {
            return IDLE.equals(name);
        }

        public int length() {
            return end - start;
        }
    }

    /** The final numbers of one process. */
    public static final class Row {
        public final String name;
        public final int arrival;
        public final int burst;
        public final int priority;
        public final int start;
        public final int finish;
        public final int turnaround;
        public final int waiting;

        public Row(String name, int arrival, int burst, int priority, int start, int finish,
                   int turnaround, int waiting) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.start = start;
            this.finish = finish;
            this.turnaround = turnaround;
            this.waiting = waiting;
        }
    }

    /** Everything a simulation produces. */
    public static final class Result {
        public final Algorithm algorithm;
        public final int quantum;               // 0 when the algorithm has no quantum
        public final List<Slice> slices;        // the Gantt chart, in time order
        public final List<Row> rows;            // one per process, in the order they were entered
        public final int totalTime;
        public final double averageWaiting;
        public final double averageTurnaround;

        Result(Algorithm algorithm, int quantum, List<Slice> slices, List<Row> rows, int totalTime,
               double averageWaiting, double averageTurnaround) {
            this.algorithm = algorithm;
            this.quantum = quantum;
            this.slices = slices;
            this.rows = rows;
            this.totalTime = totalTime;
            this.averageWaiting = averageWaiting;
            this.averageTurnaround = averageTurnaround;
        }
    }

    /** A job while it is being scheduled. */
    private static final class Work {
        final Job job;
        int remaining;
        int start = -1;
        int finish;

        Work(Job job) {
            this.job = job;
            this.remaining = job.burst;
        }
    }

    /**
     * Runs one simulation.
     *
     * @param quantum only used by Round Robin
     * @throws IllegalArgumentException with a readable message if the input is not valid
     */
    public static Result run(Algorithm algorithm, List<Job> jobs, int quantum) {
        if (algorithm == null) throw new IllegalArgumentException("Choose an algorithm.");
        if (jobs == null || jobs.isEmpty()) throw new IllegalArgumentException("Add at least one process first.");
        for (Job j : jobs) {
            if (j.arrival < 0)
                throw new IllegalArgumentException("The arrival time of " + j.name + " cannot be negative.");
            if (j.burst <= 0)
                throw new IllegalArgumentException("The burst time of " + j.name + " must be greater than 0.");
        }
        if (algorithm == Algorithm.ROUND_ROBIN && quantum <= 0)
            throw new IllegalArgumentException("The time quantum must be greater than 0.");

        List<Work> work = new ArrayList<>();
        for (Job j : jobs) work.add(new Work(j));

        List<Slice> raw = new ArrayList<>();
        if (algorithm == Algorithm.ROUND_ROBIN) roundRobin(work, quantum, raw);
        else nonPreemptive(work, algorithm, raw);
        List<Slice> slices = merge(raw);

        List<Row> rows = new ArrayList<>();
        double totalTurnaround = 0;
        double totalWaiting = 0;
        for (Work w : work) {
            int turnaround = w.finish - w.job.arrival;
            int waiting = turnaround - w.job.burst;
            totalTurnaround += turnaround;
            totalWaiting += waiting;
            rows.add(new Row(w.job.name, w.job.arrival, w.job.burst, w.job.priority,
                    w.start, w.finish, turnaround, waiting));
        }
        int total = slices.isEmpty() ? 0 : slices.get(slices.size() - 1).end;
        return new Result(algorithm, algorithm == Algorithm.ROUND_ROBIN ? quantum : 0, slices,
                Collections.unmodifiableList(rows), total,
                totalWaiting / work.size(), totalTurnaround / work.size());
    }

    /** FCFS, SJF and Priority: once a process starts it runs to completion. */
    private static void nonPreemptive(List<Work> all, Algorithm algo, List<Slice> out) {
        List<Work> left = new ArrayList<>(all);
        int time = 0;
        while (!left.isEmpty()) {
            List<Work> ready = new ArrayList<>();
            for (Work w : left) if (w.job.arrival <= time) ready.add(w);

            if (ready.isEmpty()) {   // nobody has arrived yet: the CPU waits
                int next = Integer.MAX_VALUE;
                for (Work w : left) next = Math.min(next, w.job.arrival);
                addSlice(out, Slice.IDLE, time, next);
                time = next;
                continue;
            }

            Work chosen = ready.get(0);
            for (Work w : ready) if (better(w, chosen, algo)) chosen = w;

            chosen.start = time;
            addSlice(out, chosen.job.name, time, time + chosen.job.burst);
            time += chosen.job.burst;
            chosen.finish = time;
            left.remove(chosen);
        }
    }

    private static boolean better(Work a, Work b, Algorithm algo) {
        if (algo == Algorithm.SJF) {
            return a.job.burst != b.job.burst ? a.job.burst < b.job.burst : a.job.arrival < b.job.arrival;
        }
        if (algo == Algorithm.PRIORITY) {
            return a.job.priority != b.job.priority ? a.job.priority < b.job.priority
                    : a.job.arrival < b.job.arrival;
        }
        return a.job.arrival < b.job.arrival;   // FCFS
    }

    /** Round Robin: each process gets at most one quantum, then goes to the back of the queue. */
    private static void roundRobin(List<Work> all, int quantum, List<Slice> out) {
        List<Work> pending = new ArrayList<>(all);
        pending.sort(Comparator.comparingInt((Work w) -> w.job.arrival));
        Deque<Work> ready = new ArrayDeque<>();
        int time = 0;
        int next = 0;
        int done = 0;

        while (done < all.size()) {
            while (next < pending.size() && pending.get(next).job.arrival <= time) ready.add(pending.get(next++));

            if (ready.isEmpty()) {
                int arrive = pending.get(next).job.arrival;
                addSlice(out, Slice.IDLE, time, arrive);
                time = arrive;
                continue;
            }

            Work p = ready.poll();
            if (p.start < 0) p.start = time;
            int run = Math.min(quantum, p.remaining);
            addSlice(out, p.job.name, time, time + run);
            time += run;
            p.remaining -= run;

            // New arrivals enter the queue before the process that just used its quantum.
            while (next < pending.size() && pending.get(next).job.arrival <= time) ready.add(pending.get(next++));

            if (p.remaining == 0) {
                p.finish = time;
                done++;
            } else {
                ready.add(p);
            }
        }
    }

    private static void addSlice(List<Slice> out, String name, int start, int end) {
        if (end > start) out.add(new Slice(name, start, end));
    }

    /** Joins back-to-back slices of the same process into one block (a process that keeps the CPU). */
    private static List<Slice> merge(List<Slice> raw) {
        List<Slice> merged = new ArrayList<>();
        for (Slice s : raw) {
            if (!merged.isEmpty()) {
                Slice last = merged.get(merged.size() - 1);
                if (last.name.equals(s.name) && last.end == s.start) {
                    merged.set(merged.size() - 1, new Slice(last.name, last.start, s.end));
                    continue;
                }
            }
            merged.add(s);
        }
        return Collections.unmodifiableList(merged);
    }
}
package opersys.multiprogramming;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Runs the memory-admission and round-robin simulation without UI code. */
public final class MultiprogrammingEngine {
    public static final int RUNNING = 0;
    public static final int READY = 1;
    public static final int WAITING = 2;
    public static final int DONE = 3;
    public static final int L_INFO = 0;
    public static final int L_LOAD = 1;
    public static final int L_RUN = 2;
    public static final int L_DONE = 3;
    public static final int L_QUEUE = 4;

    private MultiprogrammingEngine() {
    }

    public static final class Job {
        public final int index;
        public final String name;
        public final int memory;
        public final int burst;
        public int remaining;
        public int loadedAt = -1;
        public int firstRun = -1;
        public int finish = -1;

        Job(int index, String name, int memory, int burst) {
            this.index = index;
            this.name = name;
            this.memory = memory;
            this.burst = burst;
            remaining = burst;
        }
    }

    public static final class Snap {
        public int time;
        public int running = -1;
        public int used;
        public int total;
        public int[] state;
        public int[] pos;
        public int[] left;
        public int[] doneAt;
        public int[] memOrder;
    }

    public static final class Step {
        public int no;
        public int start;
        public int end;
        public int job = -1;
        public boolean last;
        public String title;
        public Snap snap;
        public final List<Object[]> lines = new ArrayList<>();

        void add(int kind, String text) {
            lines.add(new Object[]{kind, text});
        }
    }

    public static class Result {
        public final int memory;
        public final int quantum;
        public final int total;
        public final int maxDegree;
        public final List<Job> jobs;
        public final List<Step> steps;
        public final String summary;

        Result(int memory, int quantum, int total, int maxDegree, List<Job> jobs,
               List<Step> steps, String summary) {
            this.memory = memory;
            this.quantum = quantum;
            this.total = total;
            this.maxDegree = maxDegree;
            this.jobs = jobs;
            this.steps = steps;
            this.summary = summary;
        }
    }

    public static Result simulate(List<Job> jobs, int totalMemory, int quantum) {
        for (Job job : jobs) {
            if (job.memory > totalMemory)
                throw new IllegalArgumentException(job.name + " needs more memory than the system has.");
        }
        List<Job> waiting = new ArrayList<>(jobs);
        List<Job> inMemory = new ArrayList<>();
        Deque<Job> ready = new ArrayDeque<>();
        List<Object[]> pendingLog = new ArrayList<>();
        int[] used = {0};
        int time = 0;

        admit(waiting, inMemory, ready, used, totalMemory, time, pendingLog);
        int maxDegree = inMemory.size();
        int stepNo = 0;
        List<Step> steps = new ArrayList<>();

        while (!ready.isEmpty()) {
            Job job = ready.poll();
            if (job.firstRun < 0) job.firstRun = time;
            Step step = new Step();
            step.no = ++stepNo;
            step.start = time;
            step.job = job.index;
            step.snap = snapshot(jobs, time, job, ready, inMemory, used[0], totalMemory);
            step.lines.addAll(pendingLog);
            pendingLog.clear();

            List<Job> queueNow = new ArrayList<>();
            queueNow.add(job);
            queueNow.addAll(ready);
            step.add(L_INFO, "Ready queue: " + names(queueNow)
                    + " - the CPU takes Job " + job.name + " from the front");

            int slice = Math.min(quantum, job.remaining);
            step.add(L_RUN, "Job " + job.name + " runs from t=" + time + " to t=" + (time + slice)
                    + " (" + slice + (slice == 1 ? " unit" : " units") + ": the smaller of quantum "
                    + quantum + " and " + job.remaining + " left)");
            time += slice;
            job.remaining -= slice;
            step.end = time;
            step.title = "Job " + job.name + " runs for " + slice + (slice == 1 ? " unit" : " units");

            if (job.remaining == 0) {
                job.finish = time;
                used[0] -= job.memory;
                inMemory.remove(job);
                step.add(L_DONE, "Job " + job.name + " finished at t=" + time + " and freed "
                        + job.memory + " KB");
                admit(waiting, inMemory, ready, used, totalMemory, time, step.lines);
            } else {
                ready.add(job);
                step.add(L_QUEUE, "Job " + job.name + " still needs " + job.remaining
                        + " more and goes to the back of the queue");
            }
            maxDegree = Math.max(maxDegree, inMemory.size());
            step.add(L_INFO, "Memory in use: " + used[0] + " / " + totalMemory
                    + " KB · Waiting for memory: " + (waiting.isEmpty() ? "none" : names(waiting)));
            steps.add(step);
        }

        Step end = new Step();
        end.no = ++stepNo;
        end.start = time;
        end.end = time;
        end.last = true;
        end.title = "All jobs finished";
        end.snap = snapshot(jobs, time, null, ready, inMemory, used[0], totalMemory);
        end.add(L_DONE, "All " + jobs.size() + " jobs finished at t=" + time);
        end.add(L_INFO, "Degree of multiprogramming (most jobs in memory at once): " + maxDegree);
        steps.add(end);

        String summary = jobs.size() + " jobs · " + totalMemory + " KB · q=" + quantum + " · done at t=" + time;
        return new Result(totalMemory, quantum, time, maxDegree, jobs, steps, summary);
    }

    private static void admit(List<Job> waiting, List<Job> inMemory, Deque<Job> ready, int[] used,
                              int totalMemory, int time, List<Object[]> log) {
        for (int i = 0; i < waiting.size();) {
            Job job = waiting.get(i);
            if (used[0] + job.memory <= totalMemory) {
                waiting.remove(i);
                used[0] += job.memory;
                job.loadedAt = time;
                inMemory.add(job);
                ready.add(job);
                log.add(new Object[]{L_LOAD, "Job " + job.name + " (" + job.memory
                        + " KB) loaded into memory at t=" + time});
            } else {
                i++;
            }
        }
    }

    private static Snap snapshot(List<Job> jobs, int time, Job running, Deque<Job> ready,
                                 List<Job> inMemory, int used, int total) {
        Snap snapshot = new Snap();
        snapshot.time = time;
        snapshot.used = used;
        snapshot.total = total;
        snapshot.running = running == null ? -1 : running.index;
        snapshot.state = new int[jobs.size()];
        snapshot.pos = new int[jobs.size()];
        snapshot.left = new int[jobs.size()];
        snapshot.doneAt = new int[jobs.size()];
        int position = 1;
        for (Job job : ready) snapshot.pos[job.index] = position++;
        for (Job job : jobs) {
            int index = job.index;
            snapshot.left[index] = job.remaining;
            snapshot.doneAt[index] = job.finish;
            if (job == running) snapshot.state[index] = RUNNING;
            else if (snapshot.pos[index] > 0) snapshot.state[index] = READY;
            else if (job.finish >= 0) snapshot.state[index] = DONE;
            else snapshot.state[index] = WAITING;
        }
        snapshot.memOrder = new int[inMemory.size()];
        for (int i = 0; i < snapshot.memOrder.length; i++) snapshot.memOrder[i] = inMemory.get(i).index;
        return snapshot;
    }

    private static String names(List<Job> jobs) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < jobs.size(); i++) {
            if (i > 0) text.append(", ");
            text.append(jobs.get(i).name);
        }
        return text.append("]").toString();
    }
}

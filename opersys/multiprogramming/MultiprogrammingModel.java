package opersys.multiprogramming;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MULTIPROGRAMMING MODEL = the middleman between the screen and the simulation. LOGIC ONLY, no Swing here.
 */
public final class MultiprogrammingModel {

    private MultiprogrammingModel() {
    }

    /** One job the user typed: name, memory (KB) and burst (time units). */
    public static final class JobInput {
        public final String name;
        public final int memory;
        public final int burst;

        JobInput(String name, int memory, int burst) {
            this.name = name;
            this.memory = memory;
            this.burst = burst;
        }
    }

    public static final class Run extends MultiprogrammingEngine.Result {
        public int number;
        public String time;
        public String input;

        private Run(MultiprogrammingEngine.Result result) {
            super(result.memory, result.quantum, result.total, result.maxDegree, result.jobs,
                    result.steps, result.summary);
        }
    }

    /** Reads the text fields, validates them, and returns a simulation result. */
    public static Run build(String jobsText, String memoryText, String quantumText) {
        List<JobInput> inputs = parseJobs(jobsText);
        int totalMemory = parseWhole(memoryText, "Total memory");
        int quantum = parseWhole(quantumText, "Quantum");
        if (totalMemory <= 0) throw new IllegalArgumentException("Total memory must be greater than 0.");
        if (quantum <= 0) throw new IllegalArgumentException("Quantum must be greater than 0.");
        for (JobInput j : inputs) {
            if (j.memory > totalMemory)
                throw new IllegalArgumentException(j.name + " needs more memory than the system has.");
        }
        List<MultiprogrammingEngine.Job> jobs = new ArrayList<>();
        for (JobInput in : inputs) {
            jobs.add(new MultiprogrammingEngine.Job(jobs.size(), in.name, in.memory, in.burst));
        }
        MultiprogrammingEngine.Result result = MultiprogrammingEngine.simulate(jobs, totalMemory, quantum);
        Run run = new Run(result);
        run.input = jobsText.trim();
        run.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return run;
    }

    /**
     * Parses jobs typed the easy way: "A 200 5, B 300 3" (name, memory, burst).
     * Jobs are separated by a comma, semicolon or new line. Inside a job, a space is enough
     * (the old "A:200:5" still works too). The name is optional: "200 5, 300 3" gives jobs A and B.
     */
    public static List<JobInput> parseJobs(String text) {
        List<JobInput> out = new ArrayList<>();
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Add at least one process first (name, memory, burst).");
        for (String token : t.split("\\s*[,;\\n]+\\s*")) {
            token = token.trim();
            if (token.isEmpty()) continue;
            String[] parts = token.split("[\\s:]+");
            String name;
            String memText;
            String burstText;
            if (parts.length == 3) {
                name = parts[0];
                memText = parts[1];
                burstText = parts[2];
            } else if (parts.length == 2 && parts[0].matches("\\d+")) {
                name = autoName(out.size());
                memText = parts[0];
                burstText = parts[1];
            } else {
                throw new IllegalArgumentException("Use name memory burst, e.g. A 200 5");
            }
            int mem = parseWhole(memText, "Memory of " + name);
            int burst = parseWhole(burstText, "Burst of " + name);
            if (mem <= 0) throw new IllegalArgumentException("Job " + name + ": memory must be greater than 0.");
            if (burst <= 0) throw new IllegalArgumentException("Job " + name + ": burst must be greater than 0.");
            out.add(new JobInput(name, mem, burst));
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Add at least one process first (name, memory, burst).");
        return out;
    }

    /** A, B, C ... Z, then A1, B1 ... for jobs typed without a name. */
    private static String autoName(int index) {
        String letter = String.valueOf((char) ('A' + index % 26));
        return index < 26 ? letter : letter + (index / 26);
    }

    /** Parses a whole number from text, throwing a friendly error. */
    public static int parseWhole(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
    }
}
package opersys.multiprogramming;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MULTIPROGRAMMING MODEL = the middleman between the screen and the simulation. LOGIC ONLY, no Swing here.
 *
 * The screen only has text boxes, so everything arrives as text ("A:200:5", "800", "2"...).
 * This class turns that text into real numbers (complaining nicely if something is wrong),
 * runs the simulation, and packs the answer into a Result for the controller.
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

    /** Parses "A:200:5, B:300:3" into a list of jobs. */
    public static List<JobInput> parseJobs(String text) {
        List<JobInput> out = new ArrayList<>();
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Type the jobs first, e.g. A:200:5, B:300:3");
        for (String token : t.split("\\s*,\\s*")) {
            String[] parts = token.trim().split("\\s*:\\s*");
            if (parts.length != 3 || parts[0].isEmpty())
                throw new IllegalArgumentException("Use name:memory:burst, e.g. A:200:5");
            int mem = parseWhole(parts[1], "Memory of " + parts[0]);
            int burst = parseWhole(parts[2], "Burst of " + parts[0]);
            if (mem <= 0) throw new IllegalArgumentException("Job " + parts[0] + ": memory must be greater than 0.");
            if (burst <= 0) throw new IllegalArgumentException("Job " + parts[0] + ": burst must be greater than 0.");
            out.add(new JobInput(parts[0], mem, burst));
        }
        return out;
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
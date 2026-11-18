package opersys.bankers;

import java.util.ArrayList;
import java.util.List;


public class BankAlgorithm {
    private final int processCount;    
    private final int resourceCount;  
    private final int[] available;     
    private final int[][] max;         
    private final int[][] allocation;  

    public BankAlgorithm(int[] available, int[][] max, int[][] allocation) {
        this.processCount = max.length;
        this.resourceCount = available.length;
        this.available = available.clone();
        this.max = max;
        this.allocation = allocation;
        validate();
    }

    private void validate() {
        if (allocation.length != processCount)
            throw new IllegalArgumentException("Allocation must have " + processCount + " rows (one per process).");
        for (int p = 0; p < processCount; p++) {
            if (max[p].length != resourceCount || allocation[p].length != resourceCount)
                throw new IllegalArgumentException("Every row must have " + resourceCount + " values.");
            for (int r = 0; r < resourceCount; r++)
                if (allocation[p][r] > max[p][r])
                    throw new IllegalArgumentException("P" + p + " allocation is larger than its max.");
        }
    }

    public int getProcessCount() {
        return processCount;
    }

    public int[] getAvailable() {
        return available.clone();
    }

    public int need(int p, int r) {
        return max[p][r] - allocation[p][r];
    }

    public int[] getNeed(int p) {
        int[] row = new int[resourceCount];
        for (int r = 0; r < resourceCount; r++) row[r] = need(p, r);
        return row;
    }

 
    public List<Integer> findSafeSequence() {
        int[] work = available.clone();
        boolean[] finished = new boolean[processCount];
        List<Integer> sequence = new ArrayList<>();
        boolean progress = true;

        // keep looping over the customers until a full pass finishes nobody new
        while (progress) {
            progress = false;
            for (int p = 0; p < processCount; p++) {
                if (!finished[p] && canFinish(p, work)) {
                    for (int r = 0; r < resourceCount; r++) work[r] += allocation[p][r];   // they hand everything back
                    finished[p] = true;
                    sequence.add(p);
                    progress = true;
                }
            }
        }
        return sequence.size() == processCount ? sequence : new ArrayList<>();
    }

    /**
     * Same safety check as above, but it writes down every question it asks (one BankStep each),
     * so the screen can show the thinking step by step. It changes nothing in the bank.
     */
    public List<BankStep> trace() {
        int[] work = available.clone();
        boolean[] finished = new boolean[processCount];
        List<BankStep> steps = new ArrayList<>();
        boolean progress = true;
        int round = 0;

        while (progress) {
            progress = false;
            round++;   // a new pass over the customers
            for (int p = 0; p < processCount; p++) {
                if (finished[p]) continue;
                int[] before = work.clone();
                if (canFinish(p, work)) {
                    for (int r = 0; r < resourceCount; r++) work[r] += allocation[p][r];
                    finished[p] = true;
                    progress = true;
                    steps.add(new BankStep(round, p, getNeed(p), before, true, work));
                } else {
                    steps.add(new BankStep(round, p, getNeed(p), before, false, before));
                }
            }
        }
        return steps;
    }

    /** True if the pile ("work") covers everything this customer may still need. */
    private boolean canFinish(int p, int[] work) {
        for (int r = 0; r < resourceCount; r++)
            if (need(p, r) > work[r]) return false;
        return true;
    }

    /**
     * A customer asks for more. We only say yes if (1) they aren't asking for more than their Need,
     * (2) the bank actually has it, and (3) the bank would STILL be safe afterwards.
     * We try the loan first, and if it turns out unsafe we undo it and say no.
     */
    public boolean request(int p, int[] req) {
        if (p < 0 || p >= processCount) throw new IllegalArgumentException("There is no process P" + p + ".");
        if (req.length != resourceCount) throw new IllegalArgumentException("Request must have " + resourceCount + " values.");
        for (int r = 0; r < resourceCount; r++)
            if (req[r] > need(p, r))
                throw new IllegalStateException("P" + p + " requests more than its declared need.");
        for (int r = 0; r < resourceCount; r++)
            if (req[r] > available[r]) return false;   // the bank simply doesn't have enough right now

        // pretend we lend it...
        for (int r = 0; r < resourceCount; r++) {
            available[r] -= req[r];
            allocation[p][r] += req[r];
        }
        // ...and if that leaves the bank unsafe, take it back
        if (findSafeSequence().isEmpty()) {
            for (int r = 0; r < resourceCount; r++) {
                available[r] += req[r];
                allocation[p][r] -= req[r];
            }
            return false;
        }
        return true;
    }
}
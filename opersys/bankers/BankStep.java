package opersys.bankers;

import java.util.Arrays;

/**
 * BANK STEP = one line of the "step by step" story of the safety check.
 *
 * While the algorithm hunts for a safe order, it looks at one customer at a time and asks:
 * "does the bank have enough to cover what this customer might still need?"
 * Each of those little questions (and its answer) is saved as one BankStep, so the screen can
 * replay the whole thinking process later. It is just a little notepad entry, it never changes anything.
 */
public final class BankStep {
    private final int round;           // which pass over the customers this was (the algorithm may loop a few times)
    private final int process;         // which customer we looked at (0 = P0, 1 = P1, ...)
    private final int[] need;          // what that customer may still ask for (Max - Allocation)
    private final int[] workBefore;    // what the bank had at that moment
    private final boolean canFinish;   // true = the bank could cover the need, so the customer finishes
    private final int[] workAfter;     // what the bank has afterwards (same as before if the customer had to wait)

    /** Package-private on purpose: only BankAlgorithm writes steps. Arrays are copied so nobody can change them later. */
    BankStep(int round, int process, int[] need, int[] workBefore, boolean canFinish, int[] workAfter) {
        this.round = round;
        this.process = process;
        this.need = need.clone();
        this.workBefore = workBefore.clone();
        this.canFinish = canFinish;
        this.workAfter = workAfter.clone();
    }

    public int getRound() {
        return round;
    }

    public int getProcess() {
        return process;
    }

    public int[] getNeed() {
        return need.clone();
    }

    public int[] getWorkBefore() {
        return workBefore.clone();
    }

    public boolean canFinish() {
        return canFinish;
    }

    public int[] getWorkAfter() {
        return workAfter.clone();
    }

    /** The step written as one plain sentence, ready to show on screen. */
    public String describe() {
        String p = "P" + process;
        String start = p + " needs " + Arrays.toString(need) + ", bank has " + Arrays.toString(workBefore) + " -> ";
        if (canFinish) {
            return start + "enough! " + p + " finishes and gives back everything it holds. Bank now has "
                    + Arrays.toString(workAfter) + ".";
        }
        return start + "not enough, " + p + " has to wait.";
    }
}
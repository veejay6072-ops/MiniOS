package opersys.multiprogramming;

import java.util.ArrayList;
import java.util.List;

/** Keeps the latest multiprogramming runs in memory, newest first. */
public final class MultiprogrammingHistory {
    private static final MultiprogrammingHistory SHARED = new MultiprogrammingHistory();
    private final List<MultiprogrammingModel.Run> entries = new ArrayList<>();
    private int runCounter;

    private MultiprogrammingHistory() {
    }

    public static MultiprogrammingHistory shared() {
        return SHARED;
    }

    public List<MultiprogrammingModel.Run> entries() {
        return new ArrayList<>(entries);
    }

    public void add(MultiprogrammingModel.Run run) {
        run.number = ++runCounter;
        entries.add(0, run);
    }

    public void clear() {
        entries.clear();
        runCounter = 0;
    }
}

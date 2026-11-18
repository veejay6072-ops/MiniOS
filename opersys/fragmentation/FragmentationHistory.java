package opersys.fragmentation;

import java.util.ArrayList;
import java.util.List;

/** Keeps fragmentation actions newest first, shared between app windows. */
public final class FragmentationHistory {
    private static final FragmentationHistory SHARED = new FragmentationHistory();
    private final List<FragmentationStep> entries = new ArrayList<>();
    private int runCounter;

    private FragmentationHistory() {
    }

    public static FragmentationHistory shared() {
        return SHARED;
    }

    public List<FragmentationStep> entries() {
        return new ArrayList<>(entries);
    }

    void add(FragmentationStep step) {
        step.number = ++runCounter;
        entries.add(0, step);
    }

    public void clear() {
        entries.clear();
    }
}

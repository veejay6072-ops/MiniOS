package opersys.fragmentation;

import java.util.ArrayList;
import java.util.List;

/** One saved fragmentation action and the trace shown in the step and history views. */
public final class FragmentationStep {
    public static final int INFO = 0;
    public static final int SKIP = 1;
    public static final int TRY = 2;
    public static final int PICK = 3;
    public static final int FAIL = 4;

    public static final class Line {
        public final int kind;
        public final String text;

        Line(int kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    public final String time;
    public final String badge;
    public final String title;
    public final int[] before;
    public final List<Line> lines = new ArrayList<>();
    public int[] after;
    public String stats = "";
    public String summary = "";
    public int kind;
    public int number;

    FragmentationStep(String time, String badge, String title, int[] before) {
        this.time = time;
        this.badge = badge;
        this.title = title;
        this.before = before.clone();
    }

    void addLine(int lineKind, String text) {
        lines.add(new Line(lineKind, text));
    }
}

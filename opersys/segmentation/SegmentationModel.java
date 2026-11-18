package opersys.segmentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link SegmentationEngine} and exposes UI-friendly helpers:
 * it validates raw text input, keeps the selected segment and exposes the
 * last translation result. No Swing here, so the UI can be tested separately.
 */
public final class SegmentationModel {

    private SegmentationModel() {
    }

    private final SegmentationEngine engine = SegmentationEngine.create();
    private int selected = -1;
    private SegmentationEngine.Result lastResult;

    public static SegmentationModel create() {
        return new SegmentationModel();
    }

    public SegmentationEngine getEngine() {
        return engine;
    }

    public List<SegmentationEngine.Segment> getSegments() {
        return new ArrayList<>(engine.getSegments());
    }

    public int size() {
        return engine.size();
    }

    public SegmentationEngine.Segment get(int index) {
        return engine.get(index);
    }

    public int getSelected() {
        return selected;
    }

    public SegmentationEngine.Result getLastResult() {
        return lastResult;
    }

    /** Add a segment and select it. */
    public void add(String name, String baseText, String limitText) {
        int base = parseNonNegative(baseText, "Base");
        int limit = parseNonNegative(limitText, "Limit");
        if (limit == 0) {
            throw new IllegalArgumentException("Limit must be greater than 0.");
        }
        String segmentName = name == null || name.trim().isEmpty()
                ? "Seg " + engine.size()
                : name.trim();
        engine.add(new SegmentationEngine.Segment(segmentName, base, limit));
        select(engine.size() - 1);
        lastResult = null;
    }

    public void remove(int index) {
        if (index < 0 || index >= engine.size()) {
            return;
        }
        engine.remove(index);
        lastResult = null;
        if (engine.size() == 0) {
            selected = -1;
        } else {
            selected = Math.min(index, engine.size() - 1);
        }
    }

    public void select(int index) {
        if (index < 0 || index >= engine.size()) {
            return;
        }
        selected = index;
    }

    /**
     * Validates the raw text fields and runs the translation.
     *
     * @return null if the input is valid and a result was produced, otherwise an error message
     */
    public String translate(String segText, String offText) {
        lastResult = null;
        if (engine.size() == 0) {
            return "Add a segment first. Press \"New segment\" on the left.";
        }
        if (segText == null || segText.trim().isEmpty() || offText == null || offText.trim().isEmpty()) {
            return "Type both a segment number and an offset.";
        }
        try {
            int seg = Integer.parseInt(segText.trim());
            int off = Integer.parseInt(offText.trim());
            lastResult = engine.translate(seg, off);
            if (lastResult.isNoSegment()) {
                selected = -1;
            } else {
                selected = seg;
            }
            return null;
        } catch (NumberFormatException ex) {
            return "Segment and offset must be whole numbers.";
        }
    }

    private static int parseNonNegative(String text, String field) {
        int value;
        try {
            value = Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
        return value;
    }

    public int total() {
        return engine.total();
    }

    public int used() {
        return engine.used();
    }
}
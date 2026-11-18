package opersys.segmentation;

/**
 * SEGMENTATION CONTROLLER = the glue. It connects the design (SegmentationApp) to the logic
 * (SegmentationModel / SegmentationEngine). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class SegmentationController {

    private final SegmentationApp view;
    private final SegmentationModel model = SegmentationModel.create();

    public SegmentationController(SegmentationApp view) {
        this.view = view;
        view.bindController(this);
    }

    /** The panel to place inside a window. */
    public SegmentationApp getView() {
        return view;
    }

    public SegmentationModel getModel() {
        return model;
    }

    /** Add a segment (called by the view when the user presses "New segment"). */
    public void addSegment(String name, int base, int limit) {
        model.add(name, base, limit);
    }

    /** Remove a segment (called by the view when the user deletes a row). */
    public void removeSegment(int index) {
        model.remove(index);
    }

    /** Select a segment (called by the view when the user clicks a row). */
    public void selectSegment(int index) {
        model.select(index);
    }

    /**
     * Translate the typed segment/offset. Returns an error message or null on success.
     * The view should call this when the user presses Translate.
     */
    public String translate(String segText, String offText) {
        return model.translate(segText, offText);
    }

    /** The total memory size (rounded up). */
    public int total() {
        return model.total();
    }

    /** The amount of memory in use. */
    public int used() {
        return model.used();
    }
}
package opersys.segmentation;

/**
 * SEGMENTATION CONTROLLER = the glue. It connects the design (SegmentationDesign) to the logic
 * (SegmentationModel / SegmentationEngine). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class SegmentationController implements SegmentationDesign.Listener {

    private final SegmentationDesign view;
    private final SegmentationModel model = SegmentationModel.create();

    public SegmentationController(SegmentationDesign view) {
        this.view = view;
        view.setListener(this);
        refreshView();
    }

    /** The panel to place inside a window. */
    public SegmentationDesign getView() {
        return view;
    }

    @Override
    public String onAddSegment(String name, String base, String limit) {
        try {
            model.add(name, base, limit);
            view.setSegText(String.valueOf(model.getSelected()));
            translateQuietly();
            return null;
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public void onRemoveSegment(int index) {
        model.remove(index);
        translateQuietly();
    }

    @Override
    public void onSelectSegment(int index) {
        model.select(index);
        translateQuietly();
    }

    @Override
    public void onTranslate(String segment, String offset) {
        String error = model.translate(segment, offset);
        refreshView();
        if (error != null) {
            view.showError(error);
        }
    }

    private void translateQuietly() {
        int selected = model.getSelected();
        view.setSegText(selected >= 0 ? String.valueOf(selected) : "");
        model.translate(view.getSegText(), view.getOffText());
        refreshView();
    }

    private void refreshView() {
        view.setState(model.getSegments(), model.getSelected(), model.getLastResult(),
                model.total(), model.used());
    }
}
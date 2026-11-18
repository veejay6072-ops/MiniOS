package opersys.fragmentation;

/** Connects the fragmentation screen to its model. */
public final class FragmentationController implements FragmentationDesign.Listener {
    private final FragmentationDesign view = new FragmentationDesign();
    private final FragmentationModel model = new FragmentationModel();

    public FragmentationController() {
        view.setListener(this);
        refreshView();
    }

    public FragmentationDesign getView() {
        return view;
    }

    @Override
    public void onCreateMemory(String sizeText) {
        apply(model.createMemory(sizeText));
    }

    @Override
    public void onDrop(String pieceText, String algorithm) {
        apply(model.drop(pieceText, algorithm));
    }

    @Override
    public void onDeleteProcess(int pieceId) {
        apply(model.deleteProcess(pieceId));
    }

    @Override
    public void onCompact() {
        apply(model.compact());
    }

    @Override
    public void onReset() {
        apply(model.reset());
    }

    @Override
    public void onClearHistory() {
        model.clearHistory();
        refreshView();
    }

    @Override
    public void onPieceSizeChanged(String pieceText) {
        refreshView();
    }

    @Override
    public void onAlgorithmChanged(String algorithm) {
        view.showMessage("READY", "Algorithm set to " + algorithm + ". Press DROP to place the next piece.", 0);
    }

    private void apply(FragmentationModel.Feedback feedback) {
        if (feedback.error != null) {
            view.showError(feedback.error);
        } else {
            view.showMessage(feedback.title, feedback.body, feedback.kind);
        }
        refreshView();
    }

    private void refreshView() {
        view.setState(model.snapshot(view.getPieceSizeText()));
    }
}

package opersys.pagereplacement;

/**
 * PAGE REPLACEMENT CONTROLLER = the glue. It connects the design (PageReplacementApp) to the logic
 * (PageReplacementModel / PageReplacement). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class PageReplacementController {

    private final PageReplacementApp view;

    public PageReplacementController(PageReplacementApp view) {
        this.view = view;
        view.setListener(this::runSimulation);
    }

    /** The panel to place inside a window. */
    public PageReplacementApp getView() {
        return view;
    }

    /** Run button was pressed: read the boxes, run the simulation, show the answer. */
    private void runSimulation() {
        try {
            PageReplacement.Algorithm algo = view.getSelectedAlgorithm();
            PageReplacementModel.Result result = PageReplacementModel.run(
                    algo, view.getPagesText(), view.getFramesText());
            view.showResult(result);
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());
        }
    }
}
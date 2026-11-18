package opersys.pagereplacement;

/**
 * PAGE REPLACEMENT CONTROLLER = the glue. It connects the design (PageReplacementDesign) to the logic
 * (PageReplacementModel / PageReplacement). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class PageReplacementController {

    private final PageReplacementDesign view;

    public PageReplacementController(PageReplacementDesign view) {
        this.view = view;
        view.setListener(this::runSimulation);
    }

    /** The panel to place inside a window. */
    public PageReplacementDesign getView() {
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
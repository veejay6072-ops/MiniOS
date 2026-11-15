/** Connects the view (design) to the model (logic). Holds no layout code and no algorithm code. */
public class SafeBankController {

    private final SafeBankView view = new SafeBankView();

    public SafeBankController() {
        view.setListener(this::runSafetyCheck);
    }

    /** The panel to place inside a window (for example the VirtualOS internal frame). */
    public SafeBankView getView() {
        return view;
    }

    private void runSafetyCheck() {
        try {
            BankModel.Result result = BankModel.check(
                    view.readAllocation(), view.readMax(), view.readAvailable());
            if (result.isSafe()) view.showSafe(result.getOrder());
            else view.showUnsafe();
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());
        }
    }
}
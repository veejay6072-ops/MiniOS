package opersys.bankers;

/**
 * BANK CONTROLLER = the glue. It connects the design (BankDesign) to the logic (BankModel / BankAlgorithm).
 * It has no layout code and no algorithm code, it only says "screen, give me the numbers" and
 * "screen, show this answer".
 */
public class BankController {

    private final BankDesign view = new BankDesign();          // the screen
    private final BankHistory history = new BankHistory();     // the saved old checks

    public BankController() {
        view.setHistory(history);                    // the screen needs the history for its History button
        view.setListener(this::runSafetyCheck);      // when Check safety is pressed, call runSafetyCheck()
    }

    /** The panel to place inside a window (for example the VirtualOS internal frame). */
    public BankDesign getView() {
        return view;
    }

    /** Check safety was pressed: read the boxes, run the check, save it in the history, show the answer. */
    private void runSafetyCheck() {
        try {
            BankModel.Result result = BankModel.check(
                    view.readAllocation(), view.readMax(), view.readAvailable());

            history.add(new BankHistory.Entry(System.currentTimeMillis(), result.isSafe(), result.getOrder(),
                    result.getAvailable(), result.getMax(), result.getAllocation()));

            if (result.isSafe()) view.showSafe(result.getOrder(), result.getSteps());
            else view.showUnsafe(result.getSteps());
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());   // bad input (a letter, a negative number...) -> friendly popup
        }
    }
}
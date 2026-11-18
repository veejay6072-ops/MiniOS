package opersys.diskscheduling;

/**
 * DISK SCHEDULING CONTROLLER = the glue. It connects the design (DiskSchedulingDesign) to the logic
 * (DiskSchedulingModel / DiskScheduler / DiskHistory). It has no layout code and no algorithm code,
 * it only says "screen, give me the numbers" and "screen, show this answer".
 */
public class DiskSchedulingController {

    private final DiskSchedulingDesign view;
    private final DiskHistory history = new DiskHistory();

    public DiskSchedulingController(DiskSchedulingDesign view) {
        this.view = view;
        view.bindController(this);
    }

    /** The panel to place inside a window. */
    public DiskSchedulingDesign getView() {
        return view;
    }

    public DiskHistory getHistory() {
        return history;
    }

    /** Run button was pressed: read the boxes, run the simulation, show the answer. */
    public DiskSchedulingModel.Result run(String headText, String tracksText, int[] queue, String algo, boolean up) {
        return DiskSchedulingModel.run(headText, tracksText, queue, algo, up);
    }

    /** Save a run in the history. */
    public void record(DiskSchedulingModel.Result result) {
        boolean dir = DiskScheduler.usesDirection(result.getAlgo());
        history.add(new DiskHistory.Entry(System.currentTimeMillis(), result.getAlgo(),
                dir ? result.isUp() : true, result.getHead(), result.getTracks(),
                result.getQueue(), result.getTotal()));
    }

    /** Clear the history. */
    public void clearHistory() {
        history.clear();
    }
}
package opersys.multiprogramming;

import java.util.List;

/**
 * MULTIPROGRAMMING CONTROLLER = the glue. It connects the design (MultiprogrammingDesign) to the logic
 * (MultiprogrammingModel). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class MultiprogrammingController {

    private final MultiprogrammingDesign view;
    private final MultiprogrammingHistory history = MultiprogrammingHistory.shared();

    public MultiprogrammingController(MultiprogrammingDesign view) {
        this.view = view;
        view.bindController(this);
        view.setHistory(history.entries());
    }

    /** The panel to place inside a window. */
    public MultiprogrammingDesign getView() {
        return view;
    }

    /** The jobs text the user typed. */
    public String getJobsText() {
        return view.getJobsText();
    }

    /** The total memory text. */
    public String getMemoryText() {
        return view.getMemoryText();
    }

    /** The quantum text. */
    public String getQuantumText() {
        return view.getQuantumText();
    }

    /** Parse the jobs text into a list (throws on bad input). */
    public List<MultiprogrammingModel.JobInput> readJobs() {
        return MultiprogrammingModel.parseJobs(view.getJobsText());
    }

    /** The shared history (newest first). */
    public List<MultiprogrammingModel.Run> getHistory() {
        return history.entries();
    }

    /** Build, save, and return a run. */
    public MultiprogrammingModel.Run buildRun() {
        MultiprogrammingModel.Run run = MultiprogrammingModel.build(
                view.getJobsText(), view.getMemoryText(), view.getQuantumText());
        history.add(run);
        view.setHistory(history.entries());
        return run;
    }

    /** Clear the shared history. */
    public void clearHistory() {
        history.clear();
        view.setHistory(history.entries());
    }
}
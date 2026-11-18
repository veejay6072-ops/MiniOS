package opersys.multiprogramming;

import java.util.ArrayList;
import java.util.List;

/**
 * MULTIPROGRAMMING CONTROLLER = the glue. It connects the design (MultiprogrammingApp) to the logic
 * (MultiprogrammingModel). It has no layout code and no algorithm code, it only
 * says "screen, give me the numbers" and "screen, show this answer".
 */
public class MultiprogrammingController {

    private final MultiprogrammingApp view;

    public MultiprogrammingController(MultiprogrammingApp view) {
        this.view = view;
        view.bindController(this);
    }

    /** The panel to place inside a window. */
    public MultiprogrammingApp getView() {
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

    /** Build a full run from the current text boxes (throws on bad input). */
    public MultiprogrammingApp.Run buildRun() {
        MultiprogrammingApp.Run r = MultiprogrammingModel.build(
                view.getJobsText(), view.getMemoryText(), view.getQuantumText());
        r.input = view.getJobsText();
        r.time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        return r;
    }

    /** The shared history (newest first). */
    public List<MultiprogrammingApp.Run> getHistory() {
        return MultiprogrammingApp.getHistory();
    }

    /** Clear the shared history. */
    public void clearHistory() {
        MultiprogrammingApp.clearHistory();
    }
}
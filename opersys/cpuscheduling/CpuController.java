package opersys.cpuscheduling;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU CONTROLLER = the glue. It connects the design (CpuSchedulingDesign) to the logic (CpuModel / CpuScheduler).
 * It has no layout code and no algorithm code, it only says "screen, give me the numbers" and
 * "screen, show this answer".
 */
public class CpuController {

    private final CpuSchedulingDesign view;
    private CpuModel.Result result;

    public CpuController(CpuSchedulingDesign view) {
        this.view = view;
        view.setListener(this::runSimulation);
    }

    /** The panel to place inside a window. */
    public CpuSchedulingDesign getView() {
        return view;
    }

    /** Run button was pressed: read the boxes, run the check, show the answer. */
    private void runSimulation() {
        try {
            CpuScheduler.Algorithm algo = view.getSelectedAlgorithm();
            double quantum = 0;
            if (algo == CpuScheduler.Algorithm.ROUND_ROBIN) {
                quantum = CpuModel.parseNonNegative(view.getQuantumText(), "Quantum");
            }

            List<CpuModel.JobInput> jobs = new ArrayList<>();
            for (CpuSchedulingDesign.ProcessRow row : view.getProcessRows()) {
                String name = "P" + (row.getIndex() + 1);
                jobs.add(new CpuModel.JobInput(name, row.getArrivalText(),
                        row.getBurstText(), row.getPriorityText()));
            }

            result = CpuModel.run(algo, quantum, jobs);
            view.showResult(result);
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());
        }
    }
}
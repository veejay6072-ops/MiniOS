package opersys.paging;

/**
 * Controller for the Paging screen. Reads what the user typed, asks PagingModel for the answer,
 * and shows it with PagingDesign.
 */
public final class PagingController {
    private final PagingDesign view = new PagingDesign();
    private PagingModel.Layout layout;

    public PagingController() {
        view.setListener(new PagingDesign.Listener() {
            @Override
            public void onRun() {
                run();
            }

            @Override
            public void onFind() {
                find();
            }
        });
    }

    public PagingDesign getView() {
        return view;
    }

    /** Press Run: builds the hotel from the process size, page size, frames and taken frames. */
    private void run() {
        try {
            PagingModel.Layout next = PagingModel.build(
                    PagingModel.parseWhole(view.getProcessText(), "Process size"),
                    PagingModel.parseWhole(view.getPageText(), "Page size"),
                    PagingModel.parseWhole(view.getFramesText(), "Frames"),
                    PagingModel.parseList(view.getTakenText(), "Taken frames"));
            layout = next;
            view.showLayout(layout);
            find();   // if an address is already typed, translate it right away
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());   // keep the last good layout on screen
        }
    }

    /** Translates the typed address. Empty address clears the result. */
    private void find() {
        if (layout == null || view.getAddressText().trim().isEmpty()) {
            view.clearTranslation();
            return;
        }
        try {
            int address = PagingModel.parseWhole(view.getAddressText(), "Address");
            view.showTranslation(PagingModel.translate(layout, address));
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());
        }
    }
}
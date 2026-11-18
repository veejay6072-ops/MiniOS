package opersys.pagereplacement;

/**
 * PAGE REPLACEMENT MODEL = the middleman between the screen and the algorithm. LOGIC ONLY, no Swing here.
 *
 * The screen only has text boxes, so everything arrives as text ("7", "abc", ""...).
 * This class turns that text into real numbers (complaining nicely if something is wrong),
 * hands the numbers to PageReplacement, and packs the answer into a Result for the controller.
 */
public final class PageReplacementModel {

    private PageReplacementModel() {
    }

    /** What came out of a simulation: the algorithm, frames, steps, hits and faults. */
    public static final class Result {
        private final PageReplacement.Result inner;

        Result(PageReplacement.Result inner) {
            this.inner = inner;
        }

        public PageReplacement.Result getInner() {
            return inner;
        }

        public PageReplacement.Algorithm getAlgorithm() {
            return inner.algorithm;
        }

        public int getFrames() {
            return inner.frames;
        }

        public PageReplacement.Step[] getSteps() {
            return inner.steps.toArray(new PageReplacement.Step[0]);
        }

        public int getHits() {
            return inner.hits;
        }

        public int getFaults() {
            return inner.faults;
        }

        public double getHitRatio() {
            return inner.hitRatio();
        }
    }

    /** Reads the raw text from the screen, then runs the simulation. Throws IllegalArgumentException on bad input. */
    public static Result run(PageReplacement.Algorithm algorithm, String pagesText, String framesText) {
        int[] pages = parsePages(pagesText);
        int frames = parseFrames(framesText);
        PageReplacement.Result inner = PageReplacement.run(algorithm, pages, frames);
        return new Result(inner);
    }

    /** A list of page numbers separated by spaces or commas. */
    public static int[] parsePages(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Enter the page numbers, separated by spaces or commas.");
        String[] parts = t.split("[\\s,]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("\"" + parts[i] + "\" is not a page number.");
            }
        }
        return out;
    }

    /** The number of frames must be a whole number greater than 0. */
    public static int parseFrames(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Enter the number of frames.");
        try {
            int value = Integer.parseInt(t);
            if (value <= 0) throw new IllegalArgumentException("The number of frames must be greater than 0.");
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The number of frames must be a whole number.");
        }
    }
}
import java.util.Arrays;

/**
 * LOGIC ONLY. Paging rules: how many pages a process needs, which free frame each page gets,
 * and how a logical address becomes a physical address. No Swing here.
 */
public final class PagingModel {

    private PagingModel() {
    }

    /** Memory after the process is placed: each frame, the page in it (if any), and frames taken by others. */
    public static final class Layout {
        private final int processSize;
        private final int pageSize;
        private final int[] pageToFrame;
        private final int[] frameToPage;
        private final boolean[] taken;

        private Layout(int processSize, int pageSize, int[] pageToFrame, int[] frameToPage, boolean[] taken) {
            this.processSize = processSize;
            this.pageSize = pageSize;
            this.pageToFrame = pageToFrame;
            this.frameToPage = frameToPage;
            this.taken = taken;
        }

        public int getProcessSize() {
            return processSize;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getPageCount() {
            return pageToFrame.length;
        }

        public int getFrameCount() {
            return frameToPage.length;
        }

        /** Unused space in the last page. */
        public int getWastedSpace() {
            return getPageCount() * pageSize - processSize;
        }

        public int frameOfPage(int page) {
            return pageToFrame[page];
        }

        /** The page stored in this frame, or -1 if the frame is free or taken by another process. */
        public int pageInFrame(int frame) {
            return frameToPage[frame];
        }

        public boolean isTaken(int frame) {
            return taken[frame];
        }

        /** Units actually used in this page. Only the last page can be partly used. */
        public int usedInPage(int page) {
            return page == getPageCount() - 1 ? processSize - page * pageSize : pageSize;
        }
    }

    /** Result of translating one logical address. */
    public static final class Translation {
        private final int address;
        private final int page;
        private final int offset;
        private final int frame;
        private final int realAddress;
        private final boolean trap;

        private Translation(int address, int page, int offset, int frame, int realAddress, boolean trap) {
            this.address = address;
            this.page = page;
            this.offset = offset;
            this.frame = frame;
            this.realAddress = realAddress;
            this.trap = trap;
        }

        public int getAddress() {
            return address;
        }

        public int getPage() {
            return page;
        }

        public int getOffset() {
            return offset;
        }

        public int getFrame() {
            return frame;
        }

        public int getRealAddress() {
            return realAddress;
        }

        /** True when the address is outside the process. */
        public boolean isTrap() {
            return trap;
        }
    }

    /**
     * Places the process in memory. Each page gets the lowest-numbered free frame, in order.
     *
     * @param takenFrames frames already used by other processes (may be empty)
     * @throws IllegalArgumentException if the inputs are invalid or there are not enough free frames
     */
    public static Layout build(int processSize, int pageSize, int frameCount, int[] takenFrames) {
        if (processSize <= 0) throw new IllegalArgumentException("Process size must be greater than 0.");
        if (pageSize <= 0) throw new IllegalArgumentException("Page size must be greater than 0.");
        if (frameCount <= 0) throw new IllegalArgumentException("Frames must be greater than 0.");

        boolean[] taken = new boolean[frameCount];
        for (int f : takenFrames) {
            if (f < 0 || f >= frameCount) {
                throw new IllegalArgumentException("Taken frame " + f + " is outside 0 to " + (frameCount - 1) + ".");
            }
            taken[f] = true;
        }

        int free = 0;
        for (boolean b : taken) if (!b) free++;
        int pages = (processSize + pageSize - 1) / pageSize;
        if (pages > free) {
            throw new IllegalArgumentException("The process needs " + pages + " frames but only " + free + " are free.");
        }

        int[] frameToPage = new int[frameCount];
        Arrays.fill(frameToPage, -1);
        int[] pageToFrame = new int[pages];
        int next = 0;
        for (int p = 0; p < pages; p++) {
            while (taken[next]) next++;
            pageToFrame[p] = next;
            frameToPage[next] = p;
            next++;
        }
        return new Layout(processSize, pageSize, pageToFrame, frameToPage, taken);
    }

    /** Translates a logical address into page, offset, frame and physical (real) address. */
    public static Translation translate(Layout layout, int address) {
        if (address < 0) throw new IllegalArgumentException("Address cannot be negative.");
        if (address >= layout.getProcessSize()) {
            return new Translation(address, -1, -1, -1, -1, true);
        }
        int page = address / layout.getPageSize();
        int offset = address % layout.getPageSize();
        int frame = layout.frameOfPage(page);
        int real = frame * layout.getPageSize() + offset;
        return new Translation(address, page, offset, frame, real, false);
    }

    /** Reads one whole number. Empty or non-number input is rejected. */
    public static int parseWhole(String text, String what) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(what + " is empty.");
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
    }

    /** Reads a list of whole numbers separated by commas or spaces. Empty input gives an empty list. */
    public static int[] parseList(String text, String what) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return new int[0];
        String[] parts = t.split("[,\\s]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(what + " must be whole numbers separated by commas.");
            }
        }
        return out;
    }
}
/**
 * The two kinds of memory a user can log for a friend.
 * A memory is either GOOD or BAD; there is no "meh".
 *
 * @author Vincent
 * @version 2026.10.15
 */
public enum MemoryType {
    /** A positive moment. */
    GOOD,
    /** A negative moment. */
    BAD;

    /**
     * Turns raw user text into a MemoryType.
     * Leading/trailing spaces and letter case are ignored, so
     * "g", "G", " good ", "GOOD" all give GOOD and
     * "b", "Bad", "BAD" all give BAD.
     *
     * @param text what the user typed; may be null
     * @return GOOD or BAD, or null if the text is not recognized
     */
    public static MemoryType fromInput(String text) {
        if (text == null) {
            return null;
        }

        String t = text.trim().toLowerCase();

        if (t.equals("g") || t.equals("good")) {
            return GOOD;
        }
        if (t.equals("b") || t.equals("bad")) {
            return BAD;
        }
        return null;
    }
}

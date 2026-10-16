import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

/**
 * One logged moment with a friend: what happened, whether it was good
 * or bad, when it happened, and how important it was (1 to 5).
 *
 * A Memory is immutable: every field is final and set once in the
 * constructor, which validates them. Two memories are equal when all
 * four fields match, which is what lets LinkedChain.remove(T) and
 * count(T) find a specific memory.
 *
 * @author Vincent
 * @version 2026.10.15
 */
public class Memory {

    /** Longest description allowed, in characters. */
    public static final int MAX_LENGTH = 280;

    /** Importance used by the MVP when the user is not asked for one. */
    public static final int DEFAULT_IMPORTANCE = 3;

    /** Lowest allowed importance. */
    public static final int MIN_IMPORTANCE = 1;

    /** Highest allowed importance. */
    public static final int MAX_IMPORTANCE = 5;

    /**
     * Sorts memories by date, newest first. Used on a toArray copy of a
     * chain, since the chain itself promises no order.
     */
    public static final Comparator<Memory> NEWEST_FIRST =
        (a, b) -> b.date.compareTo(a.date);

    /** How dates are printed by toString. */
    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final String description;
    private final MemoryType type;
    private final LocalDate date;
    private final int importance;

    /**
     * Creates a memory, validating every field.
     *
     * @param description what happened; trimmed, 1 to MAX_LENGTH characters
     * @param type GOOD or BAD
     * @param date when it happened; not in the future
     * @param importance 1 to 5
     * @throws IllegalArgumentException if any argument breaks a rule above
     */
    public Memory(String description, MemoryType type, LocalDate date,
        int importance) {

        if (description == null) {
            throw new IllegalArgumentException("Description can't be null");
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Description can't be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Description can't be longer than " + MAX_LENGTH);
        }

        if (type == null) {
            throw new IllegalArgumentException("Type can't be null");
        }

        if (date == null) {
            throw new IllegalArgumentException("Date can't be null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date can't be in the future");
        }

        if (importance < MIN_IMPORTANCE || importance > MAX_IMPORTANCE) {
            throw new IllegalArgumentException(
                "Importance must be " + MIN_IMPORTANCE + " to " + MAX_IMPORTANCE);
        }

        this.description = trimmed;
        this.type = type;
        this.date = date;
        this.importance = importance;
    }

    /**
     * @return the trimmed description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return GOOD or BAD
     */
    public MemoryType getType() {
        return type;
    }

    /**
     * @return the date this happened
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * @return importance from 1 to 5
     */
    public int getImportance() {
        return importance;
    }

    /**
     * @return true if this is a GOOD memory
     */
    public boolean isGood() {
        return type == MemoryType.GOOD;
    }

    /**
     * Two memories are equal when description, type, date and
     * importance all match.
     *
     * @param other the object to compare to
     * @return true if other is a Memory with the same four fields
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Memory)) {
            return false;
        }
        Memory that = (Memory) other;
        return importance == that.importance
            && type == that.type
            && date.equals(that.date)
            && description.equals(that.description);
    }

    /**
     * Consistent with equals: built from the same four fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(description, type, date, importance);
    }

    /**
     * Example: {@code 10/01/2026 GOOD drove me to the airport}
     *
     * @return a one-line display string
     */
    @Override
    public String toString() {
        return date.format(DISPLAY_FORMAT) + " " + type + " " + description;
    }
}
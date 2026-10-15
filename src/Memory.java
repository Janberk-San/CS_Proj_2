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
}
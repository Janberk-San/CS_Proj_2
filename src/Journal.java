import java.util.Arrays;
import java.util.Comparator;

/**
 * The whole journal: a bag of friends stored in a LinkedChain, plus the
 * name-matching rules (exact match ignoring case/spacing, and "did you
 * mean" close matches by edit distance).
 *
 * Journal never compares friends with equals; it compares their
 * normalized names. Removing a friend uses identity, so only the exact
 * Friend object that was added is removed.
 *
 * @author Oscar
 * @version 2026.10.15
 */
public class Journal {

   
}
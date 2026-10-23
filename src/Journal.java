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

    /** Names within this many edits count as a close match. */
    public static final int CLOSE_MATCH_DISTANCE = 2;

    private final LinkedChain<Friend> friends;

    /**
     * Creates an empty journal.
     */
    public Journal() {
        friends = new LinkedChain<Friend>();
    }

    /**
     * Adds a friend. Duplicate-name checks happen before this, in
     * RewindApp, via findByName.
     *
     * @param friend the friend to add
     * @throws IllegalArgumentException if friend is null (from the chain)
     */
    public void addFriend(Friend friend) {
        friends.add(friend);
    }

    /**
     * Removes that exact friend and, with it, all their memories.
     *
     * @param friend the friend object to remove
     * @return true if the friend was in the journal
     */
    public boolean removeFriend(Friend friend) {
        return friends.remove(friend);
    }

    /**
     * Combines two records for the same person. All source memories are
     * copied to the target (equal memories remain separate entries), and
     * the source is removed from this journal. Both must be members.
     *
     * @param source record to remove
     * @param target record to keep
     * @throws IllegalArgumentException for null, absent, or identical records
     */
    public void merge(Friend source, Friend target) {
        if (source == null || target == null || source == target
            || !friends.contains(source) || !friends.contains(target)) {
            throw new IllegalArgumentException("Choose two different friends in the journal");
        }
        for (Memory memory : source.getMemoriesNewestFirst()) {
            target.addMemory(memory);
        }
        friends.remove(source);
    }

    /**
     * @return how many friends are in the journal
     */
    public int friendCount() {
        return friends.size();
    }

    /**
     * @return true if no friends have been added
     */
    public boolean isEmpty() {
        return friends.isEmpty();
    }

    /**
     * Copies every friend out of the chain. The copy is the caller's to
     * sort or filter; changing it never changes the chain.
     *
     * @return an array of all friends, in no promised order
     */
    private Friend[] allFriends() {
        return friends.toArray(new Friend[friends.size()]);
    }

    /**
     * Every friend whose name matches, ignoring case and extra spaces.
     * "Alex", " alex " and "ALEX" all match each other.
     *
     * @param name the name to look for
     * @return matching friends; empty (not null) if none
     */
    public Friend[] findByName(String name) {
        String target = Friend.normalize(name);
        Friend[] all = allFriends();

        int count = 0;
        for (Friend f : all) {
            if (Friend.normalize(f.getName()).equals(target)) {
                count++;
            }
        }

        Friend[] result = new Friend[count];
        int i = 0;
        for (Friend f : all) {
            if (Friend.normalize(f.getName()).equals(target)) {
                result[i] = f;
                i++;
            }
        }
        return result;
    }

    /**
     * Friends whose normalized name is within CLOSE_MATCH_DISTANCE edits
     * of the given name, excluding exact matches. Used for
     * "No friend named Jordn. Did you mean: Jordan?"
     *
     * @param name the name the user typed
     * @return close matches; empty (not null) if none
     */
    public Friend[] findCloseMatches(String name) {
        String target = Friend.normalize(name);
        Friend[] all = allFriends();

        int count = 0;
        for (Friend f : all) {
            if (isClose(target, f)) {
                count++;
            }
        }

        Friend[] result = new Friend[count];
        int i = 0;
        for (Friend f : all) {
            if (isClose(target, f)) {
                result[i] = f;
                i++;
            }
        }
        return result;
    }

    /**
     * @param target an already-normalized name
     * @param f the friend to test
     * @return true if f's name is 1 to CLOSE_MATCH_DISTANCE edits away
     */
    private boolean isClose(String target, Friend f) {
        int d = editDistance(target, Friend.normalize(f.getName()));
        return d >= 1 && d <= CLOSE_MATCH_DISTANCE;
    }

    /**
     * All friends sorted by normalized name, then by nickname (friends
     * with no nickname come first).
     *
     * @return a sorted copy; empty if the journal is empty
     */
    public Friend[] getFriendsSorted() {
        Friend[] arr = allFriends();
        Arrays.sort(arr, BY_NAME_THEN_NICKNAME);
        return arr;
    }

    /** Sort order used by getFriendsSorted. */
    private static final Comparator<Friend> BY_NAME_THEN_NICKNAME =
        (a, b) -> {
            int byName = Friend.normalize(a.getName())
                .compareTo(Friend.normalize(b.getName()));
            if (byName != 0) {
                return byName;
            }
            String na = a.getNickname();
            String nb = b.getNickname();
            if (na == null && nb == null) {
                return 0;
            }
            if (na == null) {
                return -1;
            }
            if (nb == null) {
                return 1;
            }
            return na.compareToIgnoreCase(nb);
        };

    /**
     * Levenshtein edit distance: the fewest single-character inserts,
     * deletes or substitutions that turn a into b.
     * Package-private so JournalTest can test it directly.
     *
     * @param a first string
     * @param b second string
     * @return the edit distance
     */
    static int editDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int delete = d[i - 1][j] + 1;
                int insert = d[i][j - 1] + 1;
                int substitute = d[i - 1][j - 1] + cost;
                d[i][j] = Math.min(delete, Math.min(insert, substitute));
            }
        }

        return d[a.length()][b.length()];
    }
}
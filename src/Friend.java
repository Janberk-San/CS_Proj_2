import java.util.Arrays;
import java.util.Locale;

/**
 * One friend in the journal: a name, an optional nickname (used to tell
 * apart two friends with the same name), and that friend's own bag of
 * memories stored in a LinkedChain.
 *
 * Friend does NOT override equals or hashCode on purpose: two friends
 * both named Alex are different people. Name matching is a Journal rule,
 * not an equality rule.
 *
 * @author Vincent
 * @version 2026.10.15
 */
public class Friend {

    private final String name;
    private final String nickname;
    private final LinkedChain<Memory> memories;

    /**
     * Creates a friend with no memories yet.
     *
     * @param name the friend's name; blank names are rejected, extra spaces
     *             are trimmed and collapsed, letter case is kept
     * @param nickname optional label like "home" or "class"; may be null
     * @throws IllegalArgumentException if name is null or blank
     */
    public Friend(String name, String nickname) {
        if (name == null || normalize(name).isEmpty()) {
            throw new IllegalArgumentException("Name can't be blank");
        }
        this.name = collapseSpaces(name);
        this.nickname = nickname == null || nickname.trim().isEmpty()
            ? null : collapseSpaces(nickname);
        this.memories = new LinkedChain<Memory>();
    }

    /**
     * Turns a name into its comparison form: trimmed, inner runs of
     * whitespace collapsed to one space, and lowercased.
     * Example: {@code "  ALEX   Kim "} becomes {@code "alex kim"}.
     *
     * @param name the raw name
     * @return the normalized name; empty string for a blank name
     */
    public static String normalize(String name) {
        return collapseSpaces(name).toLowerCase(Locale.ROOT);
    }

    /**
     * Trims and collapses inner whitespace, keeping letter case.
     *
     * @param text the raw text
     * @return the cleaned text
     */
    private static String collapseSpaces(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * @return the cleaned name with original case
     */
    public String getName() {
        return name;
    }

    /**
     * @return the nickname, or null if none
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @return "Alex" or "Alex (home)" when there is a nickname
     */
    public String displayName() {
        if (nickname == null) {
            return name;
        }
        return name + " (" + nickname + ")";
    }

    /**
     * Logs a memory for this friend.
     *
     * @param memory the memory to add
     * @throws IllegalArgumentException if memory is null (from the chain)
     */
    public void addMemory(Memory memory) {
        memories.add(memory);
    }

    /**
     * Removes one memory equal to the given one.
     *
     * @param memory the memory to remove
     * @return true if a matching memory was removed
     */
    public boolean removeMemory(Memory memory) {
        return memories.remove(memory);
    }

    /**
     * @return how many memories are logged
     */
    public int memoryCount() {
        return memories.size();
    }

    /**
     * Copies the memories out of the chain and sorts the copy by date,
     * newest first. Changing the returned array never changes the chain.
     *
     * @return a sorted array; empty (not null) if there are no memories
     */
    public Memory[] getMemoriesNewestFirst() {
        Memory[] arr = memories.toArray(new Memory[memories.size()]);
        Arrays.sort(arr, Memory.NEWEST_FIRST);
        return arr;
    }

    /**
     * Like getMemoriesNewestFirst but only the GOOD ones, in an
     * exact-size array.
     *
     * @return a sorted array of good memories; empty if none
     */
    public Memory[] getGoodMemoriesNewestFirst() {
        Memory[] all = getMemoriesNewestFirst();
        Memory[] result = new Memory[countGood()];
        int i = 0;
        for (Memory m : all) {
            if (m.isGood()) {
                result[i] = m;
                i++;
            }
        }
        return result;
    }

    /**
     * @return how many memories are GOOD
     */
    public int countGood() {
        Memory[] all = memories.toArray(new Memory[memories.size()]);
        int count = 0;
        for (Memory m : all) {
            if (m.isGood()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return how many memories are BAD
     */
    public int countBad() {
        return memoryCount() - countGood();
    }
}

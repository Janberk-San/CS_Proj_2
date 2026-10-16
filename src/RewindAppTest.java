import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for RewindApp: the whole app driven by a scripted
 * Scanner with its output captured, following the spec's test plan.
 *
 * @author Janberk
 * @version 2026.10.15
 */
public class RewindAppTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private ByteArrayOutputStream buffer;
    private Journal journal;

    /**
     * Fresh buffer and journal before every test.
     */
    @Before
    public void setUp() {
        buffer = new ByteArrayOutputStream();
        journal = new Journal();
    }

    /**
     * Runs the app to completion on a script and returns its output.
     *
     * @param script everything the "user" types
     * @return everything the app printed
     */
    private String run(String script) {
        PrintStream out = new PrintStream(buffer);
        InputReader input = new InputReader(new Scanner(script), out, TODAY);
        new RewindApp(journal, input, out).run();
        return buffer.toString();
    }

    // ---- run: normal ----

    /** Add Alex, log a good memory, Cool-Down, quit → memory and "1 good vs 0 bad". */
    @Test
    public void testAddLogCoolDownQuit() {
        String out = run(
            "1\nAlex\n\n"                                   // add Alex, no nickname
            + "3\nAlex\ndrove me to the airport\ng\n10/01/2026\n"  // log
            + "5\nAlex\n"                                   // cool-down
            + "0\n");                                       // quit
        assertTrue(out.contains("Added Alex"));
        assertTrue(out.contains("Logged"));
        assertTrue(out.contains("Cool-Down for Alex"));
        assertTrue(out.contains("10/01/2026 GOOD drove me to the airport"));
        assertTrue(out.contains("1 good vs 0 bad"));
        assertTrue(out.contains("Goodbye"));
        assertEquals(1, journal.friendCount());
        assertEquals(1, journal.findByName("Alex")[0].memoryCount());
    }

    /** Cool-Down shows good only; view shows all newest first. */
    @Test
    public void testViewAndCoolDownFilterAndSort() {
        String out = run(
            "1\nAlex\n\n"
            + "3\nAlex\nbeach trip\ng\n09/30/2026\n"
            + "3\nAlex\nbailed on plans\nb\n10/01/2026\n"
            + "4\nAlex\n"
            + "5\nAlex\n"
            + "0\n");
        int view = out.indexOf("Memories with Alex:");
        int bad = out.indexOf("1. 10/01/2026 BAD bailed on plans", view);
        int good = out.indexOf("2. 09/30/2026 GOOD beach trip", view);
        assertTrue(view >= 0 && bad > view && good > bad);

        int cool = out.indexOf("Cool-Down for Alex");
        assertTrue(cool > good);
        assertTrue(out.indexOf("1. 09/30/2026 GOOD beach trip", cool) > cool);
        assertFalse(out.substring(cool).contains("bailed on plans"));
        assertTrue(out.contains("1 good vs 1 bad"));
    }

    /** Delete a memory by number, then view shows one fewer. */
    @Test
    public void testDeleteMemory() {
        run("1\nAlex\n\n"
            + "3\nAlex\nfirst\ng\n09/01/2026\n"
            + "3\nAlex\nsecond\ng\n09/15/2026\n"
            + "6\nAlex\n1\n"            // delete #1 = newest = "second"
            + "0\n");
        Friend alex = journal.findByName("Alex")[0];
        assertEquals(1, alex.memoryCount());
        assertEquals("first", alex.getMemoriesNewestFirst()[0].getDescription());
        assertTrue(buffer.toString().contains("Deleted"));
    }

    /** Delete with 0 cancels and changes nothing. */
    @Test
    public void testDeleteCancel() {
        run("1\nAlex\n\n"
            + "3\nAlex\nfirst\ng\n09/01/2026\n"
            + "6\nAlex\n0\n"
            + "0\n");
        assertEquals(1, journal.findByName("Alex")[0].memoryCount());
        assertFalse(buffer.toString().contains("Deleted"));
    }

    /** Remove a friend after confirming; journal is empty afterwards. */
    @Test
    public void testRemoveFriend() {
        String out = run("1\nAlex\n\n2\nAlex\ny\n0\n");
        assertTrue(out.contains("Removed Alex"));
        assertTrue(journal.isEmpty());
    }

    /** Remove a friend but answer no; friend stays. */
    @Test
    public void testRemoveFriendDeclined() {
        String out = run("1\nAlex\n\n2\nAlex\nn\n0\n");
        assertFalse(out.contains("Removed"));
        assertEquals(1, journal.friendCount());
    }

    // ---- run: bad input ----

    /** View memories for "Jordn" with only Jordan saved → did-you-mean, journal unchanged. */
    @Test
    public void testUnknownFriendSuggestsCloseMatch() {
        String out = run("1\nJordan\n\n4\nJordn\n0\n");
        assertTrue(out.contains("No friend named Jordn"));
        assertTrue(out.contains("Did you mean: Jordan?"));
        assertEquals(1, journal.friendCount());
        assertEquals(0, journal.findByName("Jordan")[0].memoryCount());
    }

    /** Unknown friend with nothing close → message without suggestions. */
    @Test
    public void testUnknownFriendNoSuggestion() {
        String out = run("1\nJordan\n\n4\nZzzzzz\n0\n");
        assertTrue(out.contains("No friend named Zzzzzz"));
        assertFalse(out.contains("Did you mean"));
    }

    /** Same name in different case → warn, then require a nickname. */
    @Test
    public void testDuplicateNameRequiresNickname() {
        String out = run("1\nAlex\n\n1\nALEX\ny\nclass\n0\n");
        assertTrue(out.contains("You already have a friend named ALEX"));
        assertTrue(out.contains("Added ALEX (class)"));
        assertEquals(2, journal.findByName("alex").length);
    }

    /** Same name, user says it is the same person → nothing added. */
    @Test
    public void testDuplicateNameDeclined() {
        run("1\nAlex\n\n1\n alex \nn\n0\n");
        assertEquals(1, journal.friendCount());
    }

    /** Two Alexes: choosing by number picks the right one. */
    @Test
    public void testChooseBetweenSameName() {
        String out = run("1\nAlex\nhome\n"
            + "1\nAlex\ny\nclass\n"
            + "3\nAlex\n2\nstudied together\ng\n10/01/2026\n"
            + "0\n");
        assertTrue(out.contains("There are 2 friends named Alex"));
        // The list order is whatever findByName returns (no order is
        // promised), so read which friend was printed as #2 and check
        // that exactly that one received the memory.
        int at = out.indexOf("2. Alex (");
        assertTrue(at >= 0);
        String pickedNick = out.substring(at + "2. Alex (".length(),
            out.indexOf(")", at));
        for (Friend f : journal.findByName("Alex")) {
            int expected = pickedNick.equals(f.getNickname()) ? 1 : 0;
            assertEquals(expected, f.memoryCount());
        }
    }

    /** Acting on an empty journal → "Nothing here yet", no exception. */
    @Test
    public void testEmptyJournalActions() {
        String out = run("2\n3\n4\n5\n6\n0\n");
        assertEquals(5, countOccurrences(out, "Nothing here yet"));
        assertTrue(out.contains("Goodbye"));
    }

    /** Viewing or deleting for a friend with 0 memories → "Nothing here yet". */
    @Test
    public void testEmptyMemoriesActions() {
        String out = run("1\nAlex\n\n4\nAlex\n5\nAlex\n6\nAlex\n0\n");
        assertEquals(3, countOccurrences(out, "Nothing here yet"));
    }

    /** Bad menu choices are re-prompted, then the app continues normally. */
    @Test
    public void testBadMenuChoiceRecovers() {
        String out = run("abc\n9\n\n0\n");
        assertEquals(3, countOccurrences(out, "Enter a number from 0 to 6"));
        assertTrue(out.contains("Goodbye"));
    }

    /** Bad date and type while logging are re-prompted; memory still logged. */
    @Test
    public void testBadDateAndTypeRecover() {
        String out = run("1\nAlex\n\n"
            + "3\nAlex\nlunch\nmeh\ng\n02/30/2026\n01/01/2030\n10/01/2026\n"
            + "0\n");
        assertTrue(out.contains("Type g/good or b/bad"));
        assertTrue(out.contains("Use MM/DD/YYYY"));
        assertTrue(out.contains("Memories can't be in the future"));
        assertTrue(out.contains("Logged"));
        assertEquals(1, journal.findByName("Alex")[0].memoryCount());
    }

    // ---- run: end of input ----

    /** Script ends after "1\nAlex\n" → "Goodbye", no exception. */
    @Test
    public void testEndOfInputMidPrompt() {
        String out = run("1\nAlex\n");
        assertTrue(out.contains("Goodbye"));
        // the friend was never completed, so nothing was added
        assertTrue(journal.isEmpty());
    }

    /** Script of only "" → "Goodbye" immediately. */
    @Test
    public void testEndOfInputImmediately() {
        String out = run("");
        assertTrue(out.contains("Welcome to Rewind."));
        assertTrue(out.contains("Goodbye"));
    }

    /**
     * Counts how many times needle appears in text.
     *
     * @param text where to look
     * @param needle what to count
     * @return the number of occurrences
     */
    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf(needle, from);
            if (at < 0) {
                return count;
            }
            count++;
            from = at + needle.length();
        }
    }
}

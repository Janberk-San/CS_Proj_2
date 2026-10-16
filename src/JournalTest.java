import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Journal, following the spec's test plan.
 *
 * @author Oscar
 * @version 2026.10.15
 */
public class JournalTest {

    private Journal journal;

    /**
     * Fresh empty journal before every test.
     */
    @Before
    public void setUp() {
        journal = new Journal();
    }

    // ---- addFriend / friendCount / isEmpty ----

    /** Add 2 → count 2. */
    @Test
    public void testAddFriendNormal() {
        assertTrue(journal.isEmpty());
        journal.addFriend(new Friend("Alex", null));
        journal.addFriend(new Friend("Sam", null));
        assertEquals(2, journal.friendCount());
        assertFalse(journal.isEmpty());
    }

    /** addFriend(null) → IllegalArgumentException. */
    @Test
    public void testAddFriendNull() {
        try {
            journal.addFriend(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            assertEquals(0, journal.friendCount());
        }
    }

    // ---- removeFriend ----

    /** Remove an added friend → true, gone from getFriendsSorted. */
    @Test
    public void testRemoveFriendNormal() {
        Friend alex = new Friend("Alex", null);
        Friend sam = new Friend("Sam", null);
        journal.addFriend(alex);
        journal.addFriend(sam);
        assertTrue(journal.removeFriend(alex));
        Friend[] left = journal.getFriendsSorted();
        assertEquals(1, left.length);
        assertSame(sam, left[0]);
    }

    /** Remove a friend never added → false. */
    @Test
    public void testRemoveFriendMissing() {
        journal.addFriend(new Friend("Alex", null));
        // same name, different object: identity equality means not found
        assertFalse(journal.removeFriend(new Friend("Alex", null)));
        assertFalse(journal.removeFriend(null));
        assertEquals(1, journal.friendCount());
    }

    // ---- findByName ----

    /** Friends "Alex" and "Sam"; search " alex " → [Alex]. */
    @Test
    public void testFindByNameNormal() {
        Friend alex = new Friend("Alex", null);
        journal.addFriend(alex);
        journal.addFriend(new Friend("Sam", null));
        Friend[] found = journal.findByName(" alex ");
        assertEquals(1, found.length);
        assertSame(alex, found[0]);
    }

    /** Search "Jordan" → empty array; two Alexes → both returned. */
    @Test
    public void testFindByNameNoneAndMany() {
        journal.addFriend(new Friend("Alex", "home"));
        journal.addFriend(new Friend("alex", "class"));
        Friend[] none = journal.findByName("Jordan");
        assertNotNull(none);
        assertEquals(0, none.length);
        assertEquals(2, journal.findByName("ALEX").length);
    }

    /** Empty journal → empty array, not null. */
    @Test
    public void testFindByNameEmptyJournal() {
        assertEquals(0, journal.findByName("Alex").length);
    }

    // ---- findCloseMatches ----

    /** Friend "Jordan", search "Jordn" → [Jordan]. */
    @Test
    public void testFindCloseMatchesNormal() {
        Friend jordan = new Friend("Jordan", null);
        journal.addFriend(jordan);
        journal.addFriend(new Friend("Sam", null));
        Friend[] close = journal.findCloseMatches("Jordn");
        assertEquals(1, close.length);
        assertSame(jordan, close[0]);
    }

    /** Search "Zzzzzz" → empty; exact "jordan" → not listed. */
    @Test
    public void testFindCloseMatchesBad() {
        journal.addFriend(new Friend("Jordan", null));
        assertEquals(0, journal.findCloseMatches("Zzzzzz").length);
        assertEquals(0, journal.findCloseMatches("jordan").length);
    }

    // ---- getFriendsSorted ----

    /** Added Sam, alex, Bea → alex, Bea, Sam. */
    @Test
    public void testGetFriendsSortedNormal() {
        journal.addFriend(new Friend("Sam", null));
        journal.addFriend(new Friend("alex", null));
        journal.addFriend(new Friend("Bea", null));
        Friend[] sorted = journal.getFriendsSorted();
        assertEquals(3, sorted.length);
        assertEquals("alex", sorted[0].getName());
        assertEquals("Bea", sorted[1].getName());
        assertEquals("Sam", sorted[2].getName());
    }

    /** Same name sorts null nickname first, then nickname alphabetically. */
    @Test
    public void testGetFriendsSortedByNickname() {
        journal.addFriend(new Friend("Alex", "home"));
        journal.addFriend(new Friend("Alex", null));
        journal.addFriend(new Friend("Alex", "class"));
        Friend[] sorted = journal.getFriendsSorted();
        assertEquals(null, sorted[0].getNickname());
        assertEquals("class", sorted[1].getNickname());
        assertEquals("home", sorted[2].getNickname());
    }

    /** Empty journal → empty array. */
    @Test
    public void testGetFriendsSortedEmpty() {
        Friend[] sorted = journal.getFriendsSorted();
        assertNotNull(sorted);
        assertEquals(0, sorted.length);
    }

    // ---- editDistance ----

    /** "jordan" vs "jordn" → 1. */
    @Test
    public void testEditDistanceNormal() {
        assertEquals(1, Journal.editDistance("jordan", "jordn"));
        assertEquals(0, Journal.editDistance("alex", "alex"));
        assertEquals(1, Journal.editDistance("alex", "alec"));
        assertEquals(3, Journal.editDistance("kitten", "sitting"));
    }

    /** "" vs "abc" → 3. */
    @Test
    public void testEditDistanceEmpty() {
        assertEquals(3, Journal.editDistance("", "abc"));
        assertEquals(3, Journal.editDistance("abc", ""));
        assertEquals(0, Journal.editDistance("", ""));
    }
}

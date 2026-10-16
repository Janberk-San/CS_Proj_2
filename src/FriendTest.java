import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Friend, following the spec's test plan.
 *
 * @author Vincent
 * @version 2026.10.15
 */
public class FriendTest {

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEP_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    private Friend alex;

    /**
     * A fresh friend with no memories before every test.
     */
    @Before
    public void setUp() {
        alex = new Friend("Alex", null);
    }

    private static Memory good(String desc, LocalDate date) {
        return new Memory(desc, MemoryType.GOOD, date, 3);
    }

    private static Memory bad(String desc, LocalDate date) {
        return new Memory(desc, MemoryType.BAD, date, 3);
    }

    // ---- constructor / getters ----

    /** (" Alex ", "home") → name "Alex", nickname "home". */
    @Test
    public void testConstructorNormal() {
        Friend f = new Friend(" Alex ", "home");
        assertEquals("Alex", f.getName());
        assertEquals("home", f.getNickname());
        assertEquals(0, f.memoryCount());
    }

    /** Inner spaces collapse but case is kept. */
    @Test
    public void testConstructorCollapsesSpaces() {
        Friend f = new Friend("  ALEX   Kim ", null);
        assertEquals("ALEX Kim", f.getName());
    }

    /** Blank name → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBlankName() {
        new Friend("   ", null);
    }

    /** Null name → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullName() {
        new Friend(null, null);
    }

    // ---- displayName ----

    /** With nickname → "Alex (home)". */
    @Test
    public void testDisplayNameWithNickname() {
        assertEquals("Alex (home)", new Friend("Alex", "home").displayName());
    }

    /** Null nickname → "Alex". */
    @Test
    public void testDisplayNameNoNickname() {
        assertEquals("Alex", alex.displayName());
    }

    // ---- normalize ----

    /** " ALEX Kim " → "alex kim". */
    @Test
    public void testNormalizeNormal() {
        assertEquals("alex kim", Friend.normalize(" ALEX   Kim "));
        assertEquals("alex", Friend.normalize("   ALEX "));
    }

    /** " " → "". */
    @Test
    public void testNormalizeBlank() {
        assertEquals("", Friend.normalize(" "));
        assertEquals("", Friend.normalize(""));
    }

    // ---- addMemory / memoryCount ----

    /** Add 2 → count 2. */
    @Test
    public void testAddMemoryNormal() {
        alex.addMemory(good("a", SEP_1));
        alex.addMemory(bad("b", OCT_1));
        assertEquals(2, alex.memoryCount());
    }

    /** addMemory(null) → IllegalArgumentException, count unchanged. */
    @Test
    public void testAddMemoryNull() {
        alex.addMemory(good("a", SEP_1));
        try {
            alex.addMemory(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            assertEquals(1, alex.memoryCount());
        }
    }

    // ---- removeMemory ----

    /** Remove a logged memory → true, count drops. */
    @Test
    public void testRemoveMemoryNormal() {
        alex.addMemory(good("a", SEP_1));
        alex.addMemory(good("b", OCT_1));
        // an equal-but-separate object must match, via Memory.equals
        assertTrue(alex.removeMemory(good("a", SEP_1)));
        assertEquals(1, alex.memoryCount());
    }

    /** Remove one never logged → false. */
    @Test
    public void testRemoveMemoryMissing() {
        alex.addMemory(good("a", SEP_1));
        assertFalse(alex.removeMemory(good("zzz", SEP_1)));
        assertFalse(alex.removeMemory(null));
        assertEquals(1, alex.memoryCount());
    }

    // ---- getMemoriesNewestFirst ----

    /** Dates 09/01, 10/01, 09/15 → 10/01, 09/15, 09/01. */
    @Test
    public void testGetMemoriesNewestFirstNormal() {
        alex.addMemory(good("first", SEP_1));
        alex.addMemory(good("third", OCT_1));
        alex.addMemory(good("second", SEP_15));
        Memory[] sorted = alex.getMemoriesNewestFirst();
        assertEquals(3, sorted.length);
        assertEquals(OCT_1, sorted[0].getDate());
        assertEquals(SEP_15, sorted[1].getDate());
        assertEquals(SEP_1, sorted[2].getDate());
    }

    /** No memories → empty array, not null. */
    @Test
    public void testGetMemoriesNewestFirstEmpty() {
        Memory[] sorted = alex.getMemoriesNewestFirst();
        assertNotNull(sorted);
        assertEquals(0, sorted.length);
    }

    /** Sorting the copy does not change the chain. */
    @Test
    public void testGetMemoriesNewestFirstIsCopy() {
        alex.addMemory(good("a", SEP_1));
        Memory[] copy = alex.getMemoriesNewestFirst();
        copy[0] = null;
        assertEquals(1, alex.memoryCount());
        assertNotNull(alex.getMemoriesNewestFirst()[0]);
    }

    // ---- getGoodMemoriesNewestFirst ----

    /** 3 good, 1 bad → 3 good, sorted. */
    @Test
    public void testGetGoodMemoriesNewestFirstNormal() {
        alex.addMemory(good("g1", SEP_1));
        alex.addMemory(bad("b1", SEP_15));
        alex.addMemory(good("g2", OCT_1));
        alex.addMemory(good("g3", SEP_15));
        Memory[] goodOnes = alex.getGoodMemoriesNewestFirst();
        assertEquals(3, goodOnes.length);
        for (Memory m : goodOnes) {
            assertTrue(m.isGood());
        }
        assertEquals(OCT_1, goodOnes[0].getDate());
        assertEquals(SEP_15, goodOnes[1].getDate());
        assertEquals(SEP_1, goodOnes[2].getDate());
    }

    /** All bad → empty array. */
    @Test
    public void testGetGoodMemoriesNewestFirstAllBad() {
        alex.addMemory(bad("b1", SEP_1));
        alex.addMemory(bad("b2", OCT_1));
        Memory[] goodOnes = alex.getGoodMemoriesNewestFirst();
        assertNotNull(goodOnes);
        assertEquals(0, goodOnes.length);
    }

    // ---- countGood / countBad ----

    /** 23 good, 4 bad → 23 / 4. */
    @Test
    public void testCountsNormal() {
        for (int i = 0; i < 23; i++) {
            alex.addMemory(good("g" + i, SEP_1));
        }
        for (int i = 0; i < 4; i++) {
            alex.addMemory(bad("b" + i, SEP_1));
        }
        assertEquals(23, alex.countGood());
        assertEquals(4, alex.countBad());
        assertEquals(27, alex.memoryCount());
    }

    /** No memories → 0 / 0. */
    @Test
    public void testCountsEmpty() {
        assertEquals(0, alex.countGood());
        assertEquals(0, alex.countBad());
    }

    // ---- identity, not equals ----

    /** Two friends named Alex are different objects and not equal. */
    @Test
    public void testFriendsAreIdentityEqual() {
        Friend other = new Friend("Alex", null);
        assertFalse(alex.equals(other));
        assertTrue(alex.equals(alex));
        assertNull(alex.getNickname());
    }
}

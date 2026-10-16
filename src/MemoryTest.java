import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;

import org.junit.Test;

/**
 * Tests for Memory, following the spec's test plan.
 *
 * @author Vincent
 * @version 2026.10.15
 */
public class MemoryTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    // ---- constructor / getters ----

    /** Valid fields → getters return them, description trimmed. */
    @Test
    public void testConstructorNormal() {
        Memory m = new Memory("  drove me to the airport  ",
            MemoryType.GOOD, OCT_1, 4);
        assertEquals("drove me to the airport", m.getDescription());
        assertEquals(MemoryType.GOOD, m.getType());
        assertEquals(OCT_1, m.getDate());
        assertEquals(4, m.getImportance());
    }

    /** Blank description → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBlankDescription() {
        new Memory("   ", MemoryType.GOOD, OCT_1, 3);
    }

    /** Null description → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullDescription() {
        new Memory(null, MemoryType.GOOD, OCT_1, 3);
    }

    /** Description over MAX_LENGTH → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTooLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= Memory.MAX_LENGTH; i++) {
            sb.append('x');
        }
        new Memory(sb.toString(), MemoryType.GOOD, OCT_1, 3);
    }

    /** Null type → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullType() {
        new Memory("x", null, OCT_1, 3);
    }

    /** Null date → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullDate() {
        new Memory("x", MemoryType.GOOD, null, 3);
    }

    /** Date after today → IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFutureDate() {
        new Memory("x", MemoryType.GOOD, LocalDate.now().plusDays(1), 3);
    }

    /** Importance 0 or 6 → IllegalArgumentException each. */
    @Test
    public void testConstructorImportanceOutOfRange() {
        try {
            new Memory("x", MemoryType.GOOD, OCT_1, 0);
            fail("Expected IllegalArgumentException for importance 0");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new Memory("x", MemoryType.GOOD, OCT_1, 6);
            fail("Expected IllegalArgumentException for importance 6");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    /** Importance 1 and 5 are the valid edges. */
    @Test
    public void testConstructorImportanceEdges() {
        assertEquals(1,
            new Memory("x", MemoryType.GOOD, OCT_1, 1).getImportance());
        assertEquals(5,
            new Memory("x", MemoryType.GOOD, OCT_1, 5).getImportance());
    }

    // ---- isGood ----

    /** GOOD memory → true. */
    @Test
    public void testIsGoodTrue() {
        assertTrue(new Memory("x", MemoryType.GOOD, OCT_1, 3).isGood());
    }

    /** BAD memory → false. */
    @Test
    public void testIsGoodFalse() {
        assertFalse(new Memory("x", MemoryType.BAD, OCT_1, 3).isGood());
    }

    // ---- equals / hashCode ----

    /** Two memories with identical fields → equal, same hash. */
    @Test
    public void testEqualsNormal() {
        Memory a = new Memory("lunch", MemoryType.GOOD, OCT_1, 3);
        Memory b = new Memory("lunch", MemoryType.GOOD, OCT_1, 3);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
    }

    /** Different date → not equal; compared to null or a String → false. */
    @Test
    public void testEqualsBad() {
        Memory a = new Memory("lunch", MemoryType.GOOD, OCT_1, 3);
        Memory diffDate = new Memory("lunch", MemoryType.GOOD,
            OCT_1.minusDays(1), 3);
        Memory diffType = new Memory("lunch", MemoryType.BAD, OCT_1, 3);
        Memory diffDesc = new Memory("dinner", MemoryType.GOOD, OCT_1, 3);
        Memory diffImp = new Memory("lunch", MemoryType.GOOD, OCT_1, 4);
        assertNotEquals(a, diffDate);
        assertNotEquals(a, diffType);
        assertNotEquals(a, diffDesc);
        assertNotEquals(a, diffImp);
        assertFalse(a.equals(null));
        assertFalse(a.equals("lunch"));
    }

    // ---- toString ----

    /** Contains date, GOOD, description. */
    @Test
    public void testToStringNormal() {
        Memory m = new Memory("drove me to the airport",
            MemoryType.GOOD, OCT_1, 3);
        assertEquals("10/01/2026 GOOD drove me to the airport", m.toString());
    }

    /** Description with commas prints unchanged. */
    @Test
    public void testToStringCommas() {
        Memory m = new Memory("we argued, then laughed",
            MemoryType.BAD, OCT_1, 3);
        assertEquals("10/01/2026 BAD we argued, then laughed", m.toString());
    }

    // ---- NEWEST_FIRST ----

    /** The comparator orders later dates first. */
    @Test
    public void testNewestFirstComparator() {
        Memory older = new Memory("x", MemoryType.GOOD, OCT_1.minusDays(5), 3);
        Memory newer = new Memory("x", MemoryType.GOOD, OCT_1, 3);
        assertTrue(Memory.NEWEST_FIRST.compare(newer, older) < 0);
        assertTrue(Memory.NEWEST_FIRST.compare(older, newer) > 0);
        assertEquals(0, Memory.NEWEST_FIRST.compare(newer, newer));
    }
}

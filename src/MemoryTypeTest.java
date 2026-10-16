import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests for MemoryType.fromInput, following the spec's test plan.
 *
 * @author Vincent
 * @version 2026.10.15
 */
public class MemoryTypeTest {

    /** "g", " GOOD ", "Bad" → GOOD, GOOD, BAD. */
    @Test
    public void testFromInputNormal() {
        assertEquals(MemoryType.GOOD, MemoryType.fromInput("g"));
        assertEquals(MemoryType.GOOD, MemoryType.fromInput(" GOOD "));
        assertEquals(MemoryType.BAD, MemoryType.fromInput("Bad"));
        assertEquals(MemoryType.BAD, MemoryType.fromInput("b"));
    }

    /** "meh", "", null → null. */
    @Test
    public void testFromInputBad() {
        assertNull(MemoryType.fromInput("meh"));
        assertNull(MemoryType.fromInput(""));
        assertNull(MemoryType.fromInput(null));
    }

    /** The enum has exactly the two constants. */
    @Test
    public void testValues() {
        assertEquals(2, MemoryType.values().length);
    }
}

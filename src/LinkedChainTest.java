import static org.junit.Assert.*;

import org.junit.Test;

/** Exercises node removal, duplicates, typed copies and generic reuse. */
public class LinkedChainTest {
    @Test
    public void headMiddleAndTailRemovalKeepTheChainConnected() {
        LinkedChain<String> chain = new LinkedChain<>();
        chain.addToEnd("a");
        chain.addToEnd("b");
        chain.addToEnd("c");
        chain.addToEnd("d");
        assertTrue(chain.remove("b"));
        assertTrue(chain.remove("d"));
        assertTrue(chain.remove("a"));
        assertArrayEquals(new String[] {"c"}, chain.toArray(new String[1]));
        assertEquals("c", chain.remove());
        assertTrue(chain.isEmpty());
        assertNull(chain.remove());
    }

    @Test
    public void duplicatesAreCountedAndRemovedOneAtATime() {
        LinkedChain<Integer> chain = new LinkedChain<>();
        chain.add(7);
        chain.add(2);
        chain.add(7);
        assertEquals(2, chain.count(7));
        assertTrue(chain.remove(Integer.valueOf(7)));
        assertEquals(1, chain.count(7));
        assertEquals(2, chain.size());
        assertTrue(chain.contains(7));
        assertFalse(chain.contains(null));
        assertFalse(chain.remove(Integer.valueOf(9)));
        assertFalse(chain.remove(null));
    }

    @Test
    public void typedCopyDoesNotExposeInternalNodes() {
        LinkedChain<String> chain = new LinkedChain<>();
        chain.add("last");
        chain.add("first");
        String[] copy = chain.toArray(new String[2]);
        assertArrayEquals(new String[] {"first", "last"}, copy);
        copy[0] = "changed";
        assertEquals("first", chain.get(0));
        assertEquals("last", chain.get(1));
        assertEquals("[first, last]", chain.toString());
    }

    @Test
    public void rejectsNullEntriesWithoutChangingSize() {
        LinkedChain<String> chain = new LinkedChain<>();
        assertThrows(IllegalArgumentException.class, () -> chain.add(null));
        assertThrows(IllegalArgumentException.class, () -> chain.addToEnd(null));
        assertTrue(chain.isEmpty());
    }

    @Test
    public void rejectsOutOfBoundsIndicesAndUndersizedArrays() {
        LinkedChain<String> chain = new LinkedChain<>();
        chain.add("a");
        assertThrows(IndexOutOfBoundsException.class, () -> chain.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> chain.get(1));
        assertThrows(IllegalArgumentException.class, () -> chain.toArray(null));
        assertThrows(IllegalArgumentException.class, () -> chain.toArray(new String[0]));
        assertEquals(1, chain.size());
    }

    @Test
    public void clearAllowsReuseWithDifferentElementTypes() {
        LinkedChain<Friend> friends = new LinkedChain<>();
        Friend alex = new Friend("Alex", null);
        friends.add(alex);
        assertTrue(friends.contains(alex));
        friends.clear();
        assertEquals(0, friends.size());
        assertEquals("[]", friends.toString());
        friends.addToEnd(alex);
        assertSame(alex, friends.remove());
        LinkedChain<MemoryType> types = new LinkedChain<>();
        types.add(MemoryType.GOOD);
        assertArrayEquals(new MemoryType[] {MemoryType.GOOD},
            types.toArray(new MemoryType[1]));
    }
}

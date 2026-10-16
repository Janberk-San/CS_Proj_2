import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for InputReader, following the spec's test plan. Every reader is
 * built on a Scanner over a string, a PrintStream over a byte buffer, and
 * a fixed today of 10/02/2026, so no test needs a keyboard or the clock.
 *
 * @author Janberk
 * @version 2026.10.15
 */
public class InputReaderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private ByteArrayOutputStream buffer;

    /**
     * Fresh output buffer before every test.
     */
    @Before
    public void setUp() {
        buffer = new ByteArrayOutputStream();
    }

    /**
     * Builds a reader over scripted input.
     *
     * @param script the lines the "user" types, separated by \n
     * @return the reader
     */
    private InputReader reader(String script) {
        return new InputReader(new Scanner(script),
            new PrintStream(buffer), TODAY);
    }

    /**
     * @return everything printed so far
     */
    private String output() {
        return buffer.toString();
    }

    /**
     * Counts how many times needle appears in the captured output.
     *
     * @param needle the text to count
     * @return the number of occurrences
     */
    private int countInOutput(String needle) {
        String haystack = output();
        int count = 0;
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return count;
            }
            count++;
            from = at + needle.length();
        }
    }

    // ---- readMenuChoice ----

    /** Input "3" → 3. */
    @Test
    public void testReadMenuChoiceNormal() {
        assertEquals(3, reader("3\n").readMenuChoice("> ", 0, 6));
        assertTrue(output().contains("> "));
        assertEquals(0, countInOutput("Enter a number"));
    }

    /** "abc\n2.5\n\n-1\n99\n4" → 4, with 5 error messages. */
    @Test
    public void testReadMenuChoiceBad() {
        int result = reader("abc\n2.5\n\n-1\n99\n4\n")
            .readMenuChoice("> ", 0, 6);
        assertEquals(4, result);
        assertEquals(5, countInOutput("Enter a number from 0 to 6"));
    }

    /** The edges 0 and 6 are accepted. */
    @Test
    public void testReadMenuChoiceEdges() {
        assertEquals(0, reader("0\n").readMenuChoice("> ", 0, 6));
        assertEquals(6, reader("6\n").readMenuChoice("> ", 0, 6));
    }

    // ---- readText ----

    /** "beach trip" → "beach trip". */
    @Test
    public void testReadTextNormal() {
        assertEquals("beach trip", reader("beach trip\n").readText("? ", 280));
    }

    /** " \nhi" → "hi" after a blank-line error. */
    @Test
    public void testReadTextBlankThenValid() {
        assertEquals("hi", reader(" \nhi\n").readText("? ", 280));
        assertEquals(1, countInOutput("This can't be blank"));
    }

    /** 300 characters → first 280 plus a trim notice. */
    @Test
    public void testReadTextTooLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append('x');
        }
        String result = reader(sb.toString() + "\n").readText("? ", 280);
        assertEquals(280, result.length());
        assertTrue(output().contains("Trimmed to 280 characters"));
    }

    /** Surrounding whitespace is trimmed. */
    @Test
    public void testReadTextTrims() {
        assertEquals("hi", reader("   hi   \n").readText("? ", 280));
    }

    // ---- readOptionalText ----

    /** "home" → "home". */
    @Test
    public void testReadOptionalTextNormal() {
        assertEquals("home", reader("home\n").readOptionalText("? ", 280));
    }

    /** Blank → null. */
    @Test
    public void testReadOptionalTextBlank() {
        assertNull(reader("\n").readOptionalText("? ", 280));
        assertNull(reader("   \n").readOptionalText("? ", 280));
    }

    /** Long optional text is cut like readText. */
    @Test
    public void testReadOptionalTextTooLong() {
        String result = reader("abcdefghij\n").readOptionalText("? ", 5);
        assertEquals("abcde", result);
        assertTrue(output().contains("Trimmed to 5 characters"));
    }

    // ---- readDate ----

    /** "09/30/2026" → 2026-09-30. */
    @Test
    public void testReadDateNormal() {
        assertEquals(LocalDate.of(2026, 9, 30),
            reader("09/30/2026\n").readDate("d "));
    }

    /** Today itself is allowed (not after today). */
    @Test
    public void testReadDateToday() {
        assertEquals(TODAY, reader("10/02/2026\n").readDate("d "));
    }

    /** "02/30/2026\n13/01/2026\ntomorrow\n01/01/2030\n10/01/2026" → 2026-10-01 after 4 errors. */
    @Test
    public void testReadDateBad() {
        LocalDate d = reader(
            "02/30/2026\n13/01/2026\ntomorrow\n01/01/2030\n10/01/2026\n")
            .readDate("d ");
        assertEquals(LocalDate.of(2026, 10, 1), d);
        int formatErrors = countInOutput("Use MM/DD/YYYY");
        int futureErrors = countInOutput("Memories can't be in the future");
        assertEquals(3, formatErrors);
        assertEquals(1, futureErrors);
    }

    // ---- readMemoryType ----

    /** "G" → GOOD. */
    @Test
    public void testReadMemoryTypeNormal() {
        assertEquals(MemoryType.GOOD, reader("G\n").readMemoryType("t "));
    }

    /** "meh\nbad" → BAD after 1 error. */
    @Test
    public void testReadMemoryTypeBad() {
        assertEquals(MemoryType.BAD, reader("meh\nbad\n").readMemoryType("t "));
        assertEquals(1, countInOutput("Type g/good or b/bad"));
    }

    // ---- readYesNo ----

    /** "YES" → true. */
    @Test
    public void testReadYesNoNormal() {
        assertTrue(reader("YES\n").readYesNo("y "));
        assertTrue(reader("y\n").readYesNo("y "));
        assertFalse(reader("NO\n").readYesNo("y "));
    }

    /** "maybe\nn" → false after 1 error. */
    @Test
    public void testReadYesNoBad() {
        assertFalse(reader("maybe\nn\n").readYesNo("y "));
        assertEquals(1, countInOutput("Type y or n"));
    }

    // ---- end of input ----

    /** Empty input stream → InputClosedException from every read method. */
    @Test(expected = InputReader.InputClosedException.class)
    public void testReadTextClosed() {
        reader("").readText("? ", 280);
    }

    /** Same for readMenuChoice. */
    @Test(expected = InputReader.InputClosedException.class)
    public void testReadMenuChoiceClosed() {
        reader("").readMenuChoice("> ", 0, 6);
    }

    /** Same for readDate, even after one bad attempt. */
    @Test(expected = InputReader.InputClosedException.class)
    public void testReadDateClosedAfterError() {
        reader("tomorrow\n").readDate("d ");
    }

    /** Same for readOptionalText, readMemoryType and readYesNo. */
    @Test
    public void testOtherReadsClosed() {
        int caught = 0;
        try {
            reader("").readOptionalText("? ", 280);
        }
        catch (InputReader.InputClosedException e) {
            caught++;
        }
        try {
            reader("").readMemoryType("t ");
        }
        catch (InputReader.InputClosedException e) {
            caught++;
        }
        try {
            reader("").readYesNo("y ");
        }
        catch (InputReader.InputClosedException e) {
            caught++;
        }
        assertEquals(3, caught);
    }
}

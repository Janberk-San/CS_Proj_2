import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

/**
 * Reads one valid value at a time from a Scanner, re-prompting until the
 * user types something acceptable. Every method explains what was wrong
 * and shows a valid example before asking again, so RewindApp never sees
 * bad data.
 *
 * All three dependencies (input, output, today's date) are injected so
 * tests can run on a Scanner over a string, capture prompts in a
 * ByteArrayOutputStream, and use a fixed "today".
 *
 * @author Janberk
 * @version 2026.10.15
 */
public class InputReader {

    /**
     * Strict MM/DD/YYYY. With ResolverStyle.STRICT, 02/30/2026 is an
     * error instead of silently becoming 02/28. The pattern must use
     * "uuuu" (not "yyyy") for STRICT to work.
     */
    public static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final Scanner in;
    private final PrintStream out;
    private final LocalDate today;

    /**
     * Thrown when the input stream ends (Ctrl+D / Ctrl+Z, or a test
     * script runs out of lines). Caught only in RewindApp.run().
     */
    public static class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception.
         */
        public InputClosedException() {
            super("Input ended");
        }
    }

    /**
     * Creates a reader.
     *
     * @param in where lines come from
     * @param out where prompts and error messages go
     * @param today the date used to reject future dates
     */
    public InputReader(Scanner in, PrintStream out, LocalDate today) {
        this.in = in;
        this.out = out;
        this.today = today;
    }

    /**
     * Prints the prompt and reads one raw line.
     *
     * @param prompt text shown before reading
     * @return the line exactly as typed
     * @throws InputClosedException if there are no more lines
     */
    private String nextLine(String prompt) {
        out.print(prompt);
        if (!in.hasNextLine()) {
            throw new InputClosedException();
        }
        return in.nextLine();
    }

    /**
     * Reads a whole number in [min, max].
     *
     * @param prompt text shown before reading
     * @param min smallest accepted value
     * @param max largest accepted value
     * @return the number the user chose
     */
    public int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            String line = nextLine(prompt).trim();
            try {
                int n = Integer.parseInt(line);
                if (n >= min && n <= max) {
                    return n;
                }
            }
            catch (NumberFormatException e) {
                // fall through to the error message
            }
            out.println("Enter a number from " + min + " to " + max);
        }
    }

    /**
     * Reads a menu choice, showing context again after each invalid answer.
     *
     * @param prompt question to display
     * @param min lowest valid choice
     * @param max highest valid choice
     * @param showChoices prints the choices before each attempt
     * @return a valid choice
     */
    public int readMenuChoice(String prompt, int min, int max,
        Runnable showChoices) {
        while (true) {
            showChoices.run();
            String line = nextLine(prompt).trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            }
            catch (NumberFormatException e) {
                // Explain and show the choices again.
            }
            out.println("Enter a number from " + min + " to " + max);
        }
    }

    /**
     * @param prompt question to display
     * @return importance from 1 (least) to 5 (most)
     */
    public int readImportance(String prompt) {
        return readMenuChoice(prompt, Memory.MIN_IMPORTANCE,
            Memory.MAX_IMPORTANCE);
    }

    /**
     * Reads non-blank text. Longer input is cut to maxLength with a
     * notice; the cut text is kept.
     *
     * @param prompt text shown before reading
     * @param maxLength longest result allowed
     * @return trimmed, non-empty text of at most maxLength characters
     */
    public String readText(String prompt, int maxLength) {
        while (true) {
            String line = nextLine(prompt).trim();
            if (line.isEmpty()) {
                out.println("This can't be blank");
                continue;
            }
            return cutToLength(line, maxLength);
        }
    }

    /**
     * Like readText, but a blank answer means "none" and returns null.
     * Used for the optional nickname.
     *
     * @param prompt text shown before reading
     * @param maxLength longest result allowed
     * @return trimmed text, or null if the user typed nothing
     */
    public String readOptionalText(String prompt, int maxLength) {
        String line = nextLine(prompt).trim();
        if (line.isEmpty()) {
            return null;
        }
        return cutToLength(line, maxLength);
    }

    /**
     * Cuts text to maxLength, telling the user if it was cut.
     *
     * @param text already-trimmed text
     * @param maxLength the cap
     * @return text, or its first maxLength characters
     */
    private String cutToLength(String text, int maxLength) {
        if (text.length() > maxLength) {
            out.println("Trimmed to " + maxLength + " characters");
            return text.substring(0, maxLength);
        }
        return text;
    }

    /**
     * Reads a real date in strict MM/DD/YYYY that is not after today.
     *
     * @param prompt text shown before reading
     * @return the date
     */
    public LocalDate readDate(String prompt) {
        while (true) {
            String line = nextLine(prompt).trim();
            try {
                LocalDate d = LocalDate.parse(line, DATE_FORMAT);
                if (d.isAfter(today)) {
                    out.println("Memories can't be in the future");
                    continue;
                }
                return d;
            }
            catch (DateTimeParseException e) {
                out.println("Use MM/DD/YYYY, e.g. 10/02/2026");
            }
        }
    }

    /**
     * Reads g/good or b/bad in any case.
     *
     * @param prompt text shown before reading
     * @return GOOD or BAD
     */
    public MemoryType readMemoryType(String prompt) {
        while (true) {
            MemoryType t = MemoryType.fromInput(nextLine(prompt));
            if (t != null) {
                return t;
            }
            out.println("Type g/good or b/bad");
        }
    }

    /**
     * Reads y/yes or n/no in any case.
     *
     * @param prompt text shown before reading
     * @return true for yes, false for no
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            String s = nextLine(prompt).trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
            out.println("Type y or n");
        }
    }
}
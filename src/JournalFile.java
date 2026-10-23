import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Versioned UTF-8 journal storage. Escaped, line-oriented records keep bad
 * records isolated. File-local IDs preserve separate same-name friends.
 */
public final class JournalFile {
    private static final String HEADER = "REWIND|1";

    private JournalFile() {
        // Static file operations only.
    }

    /** A recovered journal and the number of invalid record lines skipped. */
    public static final class LoadResult {
        private final Journal journal;
        private final int skippedLines;

        private LoadResult(Journal journal, int skippedLines) {
            this.journal = journal;
            this.skippedLines = skippedLines;
        }

        /** @return the valid friends and memories from the file */
        public Journal getJournal() {
            return journal;
        }

        /** @return malformed record count (blank lines are ignored) */
        public int getSkippedLines() {
            return skippedLines;
        }
    }

    /**
     * Loads valid records and counts malformed ones. An absent or empty file
     * is a new journal; unsupported formats and I/O failures are errors.
     *
     * @param path file to read
     * @return journal and recovery count
     * @throws IOException if the file cannot be read or has an unknown header
     */
    public static LoadResult load(Path path) throws IOException {
        Journal journal = new Journal();
        Map<Integer, Friend> friends = new HashMap<>();
        int skipped = 0;
        try (BufferedReader reader = Files.newBufferedReader(path,
            StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return new LoadResult(journal, 0);
            }
            if (!HEADER.equals(header)) {
                throw new IOException("Unsupported journal format: " + path);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    loadRecord(split(line), journal, friends);
                }
                catch (IllegalArgumentException | java.time.DateTimeException e) {
                    skipped++;
                }
            }
        }
        catch (NoSuchFileException e) {
            return new LoadResult(journal, 0);
        }
        return new LoadResult(journal, skipped);
    }

    /** Validates a whole record before adding anything to the journal. */
    private static void loadRecord(List<String> fields, Journal journal,
        Map<Integer, Friend> friends) {
        if (fields.size() < 2) {
            throw new IllegalArgumentException("Missing record fields");
        }
        int id = Integer.parseInt(fields.get(1));
        if (id <= 0) {
            throw new IllegalArgumentException("Friend IDs must be positive");
        }
        if (fields.size() == 4 && fields.get(0).equals("F")) {
            if (friends.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate friend ID");
            }
            String nickname = fields.get(3).isEmpty() ? null : fields.get(3);
            Friend friend = new Friend(fields.get(2), nickname);
            friends.put(id, friend);
            journal.addFriend(friend);
        }
        else if (fields.size() == 6 && fields.get(0).equals("M")) {
            Friend friend = friends.get(id);
            if (friend == null) {
                throw new IllegalArgumentException("Unknown friend ID");
            }
            Memory memory = new Memory(fields.get(5),
                MemoryType.valueOf(fields.get(2)),
                LocalDate.parse(fields.get(3)), Integer.parseInt(fields.get(4)));
            friend.addMemory(memory);
        }
        else {
            throw new IllegalArgumentException("Unknown record or wrong field count");
        }
    }

    /**
     * Writes a complete temporary file before replacing the destination.
     * Uses an atomic replacement when the filesystem supports it.
     *
     * @param journal data to save
     * @param path destination; its parent directory must exist
     * @throws IOException if writing or replacing the destination fails
     */
    public static void save(Journal journal, Path path) throws IOException {
        Path destination = path.toAbsolutePath();
        Path temporary = Files.createTempFile(destination.getParent(),
            ".rewind-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary,
                StandardCharsets.UTF_8)) {
                writer.write(HEADER);
                writer.newLine();
                Friend[] friends = journal.getFriendsSorted();
                for (int index = 0; index < friends.length; index++) {
                    Friend friend = friends[index];
                    int id = index + 1;
                    writer.write("F|" + id + "|" + escape(friend.getName())
                        + "|" + escape(friend.getNickname()));
                    writer.newLine();
                    Memory[] memories = friend.getMemoriesNewestFirst();
                    // Reverse traversal preserves equal-date order on load,
                    // since LinkedChain.add inserts at the front.
                    for (int m = memories.length - 1; m >= 0; m--) {
                        Memory memory = memories[m];
                        writer.write("M|" + id + "|" + memory.getType()
                            + "|" + memory.getDate() + "|" + memory.getImportance()
                            + "|" + escape(memory.getDescription()));
                        writer.newLine();
                    }
                }
            }
            try {
                Files.move(temporary, destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination,
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Encodes separators and line breaks without losing literal slashes. */
    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("|", "\\|")
            .replace("\n", "\\n").replace("\r", "\\r");
    }

    /** Decodes one record; rejects truncated and unknown escape sequences. */
    private static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (escaped) {
                switch (character) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case '|':
                        value.append('|');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown escape");
                }
                escaped = false;
            }
            else if (character == '\\') {
                escaped = true;
            }
            else if (character == '|') {
                fields.add(value.toString());
                value.setLength(0);
            }
            else {
                value.append(character);
            }
        }
        if (escaped) {
            throw new IllegalArgumentException("Incomplete escape");
        }
        fields.add(value.toString());
        return fields;
    }
}

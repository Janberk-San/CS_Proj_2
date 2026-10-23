import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Persistence contracts exercised against real temporary files. */
public class JournalFileTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private Path file() {
        return temporary.getRoot().toPath().resolve("journal.txt");
    }

    private void write(String contents) throws IOException {
        Files.write(file(), contents.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void roundTripPreservesIdentitiesEmptyFriendsAndEscapes() throws IOException {
        Journal journal = new Journal();
        Friend first = new Friend("Alex | 李", "home\\work");
        Friend second = new Friend("Alex | 李", "home\\work");
        Friend empty = new Friend("Jordan", null);
        Memory memory = new Memory("said \"hi\" | café\\path\nnext\rline",
            MemoryType.GOOD, LocalDate.of(2020, 1, 2), 5);
        first.addMemory(memory);
        first.addMemory(memory);
        second.addMemory(new Memory("argument", MemoryType.BAD,
            LocalDate.of(2020, 2, 3), 1));
        journal.addFriend(first);
        journal.addFriend(second);
        journal.addFriend(empty);
        JournalFile.save(journal, file());
        JournalFile.LoadResult loaded = JournalFile.load(file());
        assertEquals(0, loaded.getSkippedLines());
        assertEquals(3, loaded.getJournal().friendCount());
        Friend[] alexes = loaded.getJournal().findByName("Alex | 李");
        assertEquals(2, alexes.length);
        int memoryCount = 0;
        for (Friend friend : alexes) {
            assertEquals("home\\work", friend.getNickname());
            memoryCount += friend.memoryCount();
            if (friend.memoryCount() == 2) {
                assertArrayEquals(new Memory[] {memory, memory},
                    friend.getMemoriesNewestFirst());
            }
        }
        assertEquals(3, memoryCount);
        assertEquals(0, loaded.getJournal().findByName("Jordan")[0].memoryCount());
        assertNull(loaded.getJournal().findByName("Jordan")[0].getNickname());
    }

    @Test
    public void missingAndEmptyFilesLoadEmpty() throws IOException {
        assertTrue(JournalFile.load(file()).getJournal().isEmpty());
        write("");
        assertTrue(JournalFile.load(file()).getJournal().isEmpty());
    }

    @Test
    public void handwrittenFileLoadsAndCountsMalformedRecords() throws IOException {
        write("REWIND|1\nF|1|Alex|\n"
            + "M|1|GOOD|2020-01-02|5|lunch\n"
            + "only|three|fields\n"
            + "M|1|GOOD|2020-02-30|3|invalid date\n"
            + "M|1|GOOD|2020-01-02|9|invalid importance\n"
            + "M|99|GOOD|2020-01-02|3|unknown friend\n"
            + "M|1|OTHER|2020-01-02|3|invalid type\n"
            + "M|1|BAD|2020-01-03|1|argument\n");
        JournalFile.LoadResult loaded = JournalFile.load(file());
        assertEquals(5, loaded.getSkippedLines());
        Friend friend = loaded.getJournal().findByName("Alex")[0];
        assertEquals(2, friend.memoryCount());
        assertEquals(1, friend.countGood());
        assertEquals(1, friend.countBad());
    }

    @Test
    public void rejectsBadEscapesDuplicateIdsBlankNamesAndFutureDates() throws IOException {
        write("REWIND|1\nF|1|Alex|\nF|1|Jordan|\nF|2|   |\n"
            + "F|0|Zero|\nM|1|GOOD|2020-01-02|3|bad\\qescape\n"
            + "M|1|GOOD|2020-01-02|3|trailing\\\n"
            + "M|1|GOOD|" + LocalDate.now().plusDays(1) + "|3|future\n");
        JournalFile.LoadResult loaded = JournalFile.load(file());
        assertEquals(6, loaded.getSkippedLines());
        assertEquals(1, loaded.getJournal().friendCount());
        assertEquals(0, loaded.getJournal().findByName("Alex")[0].memoryCount());
    }

    @Test(expected = IOException.class)
    public void unknownFileVersionCannotBeOverwrittenAsAnEmptyJournal() throws IOException {
        write("REWIND|99\n");
        JournalFile.load(file());
    }

    @Test(expected = IOException.class)
    public void missingHeaderIsRejected() throws IOException {
        write("some other file\n");
        JournalFile.load(file());
    }

    @Test(expected = IOException.class)
    public void directoryIsAnIoErrorNotAnEmptyJournal() throws IOException {
        JournalFile.load(temporary.getRoot().toPath());
    }

    @Test
    public void repeatedSavesReplaceInsteadOfAppending() throws IOException {
        Journal journal = new Journal();
        journal.addFriend(new Friend("Alex", null));
        JournalFile.save(journal, file());
        JournalFile.save(new Journal(), file());
        assertTrue(JournalFile.load(file()).getJournal().isEmpty());
    }

    @Test
    public void failedSaveLeavesExistingDestinationUntouched() throws IOException {
        Path directory = temporary.newFolder("not-a-file").toPath();
        Path marker = directory.resolve("keep.txt");
        Files.write(marker, new byte[] {42});
        try {
            JournalFile.save(new Journal(), directory);
            fail("Expected IOException");
        }
        catch (IOException expected) {
            assertArrayEquals(new byte[] {42}, Files.readAllBytes(marker));
        }
    }
    @Test
    public void roundTripPreservesOrderingOfMemoriesOnTheSameDate() throws IOException {
        Journal book = new Journal();
        Friend friend = new Friend("Alex", null);
        Memory first = new Memory("first", MemoryType.GOOD,
            LocalDate.of(2020, 1, 2), 3);
        Memory second = new Memory("second", MemoryType.BAD,
            LocalDate.of(2020, 1, 2), 4);
        friend.addMemory(first);
        friend.addMemory(second);
        book.addFriend(friend);
        JournalFile.save(book, file());
        Friend loaded = JournalFile.load(file()).getJournal().findByName("Alex")[0];
        assertArrayEquals(new Memory[] {second, first}, loaded.getMemoriesNewestFirst());
    }
}

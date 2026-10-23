import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Verifies the actual startup, save and reload lifecycle. */
public class RewindPersistenceTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private Path path() {
        return temporary.getRoot().toPath().resolve("test.journal");
    }

    private int launch(String script) {
        return RewindApp.start(new String[] {path().toString()},
            new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(output));
    }

    @Test
    public void dataSurvivesQuitAndRestart() throws Exception {
        assertEquals(0, launch("1\nAlex\n\n3\nAlex\nlunch\ng\n01/02/2020\n5\n0\n"));
        assertEquals(0, launch("4\nAlex\n0\n"));
        String text = output.toString("UTF-8");
        assertTrue(text.contains("01/02/2020 GOOD lunch [importance 5/5]"));
        assertEquals(1, JournalFile.load(path()).getJournal().friendCount());
    }

    @Test
    public void eofSavesCompletedChangesButNotPartialMemory() throws Exception {
        assertEquals(0, launch("1\nAlex\n\n3\nAlex\nlunch\ng\n"));
        Friend friend = JournalFile.load(path()).getJournal().findByName("Alex")[0];
        assertEquals(0, friend.memoryCount());
    }

    @Test
    public void undoIsSavedAndHistoryDoesNotLeakBetweenSessions() throws Exception {
        assertEquals(0, launch("1\nAlex\n\n7\n0\n"));
        assertTrue(JournalFile.load(path()).getJournal().isEmpty());
        assertEquals(0, launch("1\nJordan\n\n0\n"));
        assertEquals(0, launch("7\n0\n"));
        assertEquals(1, JournalFile.load(path()).getJournal().friendCount());
    }

    @Test
    public void invalidHeaderAbortsWithoutChangingTheFile() throws Exception {
        byte[] original = "REWIND|99\n".getBytes(StandardCharsets.UTF_8);
        Files.write(path(), original);
        assertEquals(1, launch("1\nAlex\n\n0\n"));
        assertArrayEquals(original, Files.readAllBytes(path()));
        assertFalse(output.toString("UTF-8").contains("Welcome to Rewind"));
    }

    @Test
    public void malformedRecordsAreBackedUpBeforeRecoveredDataIsSaved() throws Exception {
        byte[] original = "REWIND|1\nF|1|Alex|\nbad record\n"
            .getBytes(StandardCharsets.UTF_8);
        Files.write(path(), original);
        assertEquals(0, launch("1\nJordan\n\n0\n"));
        assertEquals(2, JournalFile.load(path()).getJournal().friendCount());
        try (Stream<Path> paths = Files.list(temporary.getRoot().toPath())) {
            Path backup = paths.filter(p -> p.toString().endsWith(".bak"))
                .findFirst().get();
            assertArrayEquals(original, Files.readAllBytes(backup));
        }
        assertTrue(output.toString("UTF-8").contains("Skipped 1"));
    }

    @Test
    public void anotherSessionCannotOverwriteAnOpenJournal() throws Exception {
        Path lockPath = path().resolveSibling("test.journal.lock");
        try (FileChannel channel = FileChannel.open(lockPath,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertEquals(1, launch("1\nAlex\n\n0\n"));
            assertFalse(Files.exists(path()));
        }
    }

    @Test
    public void helpDoesNotCreateData() {
        int result = RewindApp.start(new String[] {"--help"},
            new ByteArrayInputStream(new byte[0]), new PrintStream(output));
        assertEquals(0, result);
        assertTrue(output.toString().contains("Usage:"));
    }

    @Test
    public void invalidArgumentsDoNotStartSession() {
        int result = RewindApp.start(new String[] {"one", "two"},
            new ByteArrayInputStream(new byte[0]), new PrintStream(output));
        assertEquals(1, result);
        assertFalse(output.toString().contains("Welcome to Rewind"));
    }
    @Test
    public void failedAutosaveRemainsInMemoryAndCanBeRetried() throws Exception {
        Files.createDirectory(path());
        Journal journal = new Journal();
        PrintStream out = new PrintStream(output);
        InputReader input = new InputReader(new java.util.Scanner("1\nAlex\n\n"),
            out, java.time.LocalDate.now());
        RewindApp app = new RewindApp(journal, input, out, path());
        app.run();
        assertEquals(1, journal.friendCount());
        assertTrue(output.toString("UTF-8").contains("NOT SAVED"));
        assertTrue(Files.isDirectory(path()));
        Files.delete(path());
        app.run(); // EOF retries the pending save after the filesystem is repaired.
        assertEquals(1, JournalFile.load(path()).getJournal().friendCount());
    }

    @Test
    public void unrecoverableSaveFailureReturnsNonzeroStatus() throws Exception {
        byte[] script = "1\nAlex\n\n0\ny\n".getBytes(StandardCharsets.UTF_8);
        java.io.InputStream input = new java.io.FilterInputStream(
            new ByteArrayInputStream(script)) {
            private boolean changed;

            @Override
            public int read(byte[] bytes, int offset, int length)
                throws java.io.IOException {
                if (!changed) {
                    // Simulate the destination becoming unwritable after startup.
                    Files.createDirectory(path());
                    changed = true;
                }
                return super.read(bytes, offset, length);
            }
        };
        int result = RewindApp.start(new String[] {path().toString()},
            input, new PrintStream(output));
        assertEquals(1, result);
        assertTrue(output.toString("UTF-8").contains("Exit without saving?"));
        assertTrue(Files.isDirectory(path()));
    }
}

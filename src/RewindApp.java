import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Entry point for Rewind. Runs the text menu and turns each choice into
 * calls on Journal and Friend. Keeps a session-only stack of undo actions.
 *
 * Everything is injected through the constructor so RewindAppTest can
 * drive the whole app with a scripted Scanner and capture its output.
 *
 * @author Janberk
 * @version 2026.10.15
 */
public class RewindApp {

    /** Menu option: quit. */
    private static final int QUIT = 0;
    /** Menu option: add a friend. */
    private static final int ADD_FRIEND = 1;
    /** Menu option: remove a friend. */
    private static final int REMOVE_FRIEND = 2;
    /** Menu option: log a memory. */
    private static final int LOG_MEMORY = 3;
    /** Menu option: view memories. */
    private static final int VIEW_MEMORIES = 4;
    /** Menu option: Cool-Down mode. */
    private static final int COOL_DOWN = 5;
    /** Menu option: delete a memory. */
    private static final int DELETE_MEMORY = 6;

    /** Menu option: undo the most recent change. */
    private static final int UNDO = 7;
    /** Menu option: merge two friend records. */
    private static final int MERGE = 8;

    private final LinkedChain<Runnable> undoStack = new LinkedChain<>();

    /** Longest name or nickname accepted. */
    private static final int NAME_MAX = 280;

    private final Journal journal;
    private final InputReader input;
    private final PrintStream out;
    private final Path dataFile;
    private boolean dirty;

    /**
     * Launches the persistent journal and reports failures to the shell.
     * @param args optional journal file path, or --help
     */
    public static void main(String[] args) {
        int status = start(args, System.in, System.out);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Owns a persistent console session, including the exclusive file lock.
     * @param args optional journal path, or --help
     * @param in keyboard or other input stream
     * @param out messages and prompts
     * @return zero on success, one on load/lock/save failure or invalid arguments
     */
    public static int start(String[] args, InputStream in, PrintStream out) {
        if (args.length == 1 && args[0].equals("--help")) {
            out.println("Usage: java RewindApp [journal-file]");
            out.println("Default: rewind.journal in the current directory.");
            return 0;
        }
        if (args.length > 1) {
            out.println("Usage: java RewindApp [journal-file]");
            return 1;
        }
        try {
            Path path = Paths.get(args.length == 0 ? "rewind.journal" : args[0])
                .toAbsolutePath().normalize();
            // Resolve existing symlinks before choosing the lock and save paths.
            if (Files.exists(path)) {
                path = path.toRealPath();
            }
            else {
                path = path.getParent().toRealPath().resolve(path.getFileName());
            }
            Path lockPath = path.resolveSibling(path.getFileName() + ".lock");
            try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    out.println("This journal is already open in another Rewind session.");
                    return 1;
                }
                JournalFile.LoadResult loaded = JournalFile.load(path);
                if (loaded.getSkippedLines() > 0) {
                    Path backup = Files.createTempFile(path.getParent(),
                        path.getFileName() + ".recovery-", ".bak");
                    Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                    out.println("Skipped " + loaded.getSkippedLines()
                        + " invalid record(s). Original backed up to " + backup);
                }
                out.println("Journal file: " + path);
                Scanner keyboard = new Scanner(in, StandardCharsets.UTF_8.name());
                InputReader reader = new InputReader(keyboard, out, LocalDate.now());
                RewindApp app = new RewindApp(loaded.getJournal(), reader, out, path);
                app.run();
                return app.dirty ? 1 : 0;
            }
        }
        catch (OverlappingFileLockException e) {
            out.println("This journal is already open in another Rewind session.");
            return 1;
        }
        catch (IOException | InvalidPathException e) {
            out.println("Could not open journal: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Creates the app with injected dependencies.
     *
     * @param journal where friends and memories live
     * @param input source of validated user input
     * @param out where menus, lists and messages are printed
     */
    public RewindApp(Journal journal, InputReader input, PrintStream out) {
        this(journal, input, out, null);
    }

    /**
     * Creates a session with autosave after every completed change.
     * @param journal loaded data
     * @param input validated input
     * @param out output stream
     * @param dataFile destination, or null for an in-memory session
     */
    public RewindApp(Journal journal, InputReader input, PrintStream out,
        Path dataFile) {
        this.journal = journal;
        this.input = input;
        this.out = out;
        this.dataFile = dataFile;
    }

    /**
     * The menu loop. Returns when the user picks 0 or the input ends.
     */
    public void run() {
        out.println("Welcome to Rewind.");
        try {
            while (true) {
                printMenu();
                int choice = input.readMenuChoice("> ", QUIT, MERGE);
                switch (choice) {
                    case ADD_FRIEND:
                        addFriend();
                        break;
                    case REMOVE_FRIEND:
                        removeFriend();
                        break;
                    case LOG_MEMORY:
                        logMemory();
                        break;
                    case VIEW_MEMORIES:
                        viewMemories();
                        break;
                    case COOL_DOWN:
                        coolDown();
                        break;
                    case DELETE_MEMORY:
                        deleteMemory();
                        break;
                    case UNDO:
                        undo();
                        break;
                    case MERGE:
                        mergeFriends();
                        break;
                    default:
                        if (!saveChanges() && !input.readYesNo(
                            "Exit without saving? (y/n) ")) {
                            break;
                        }
                        out.println("Goodbye");
                        return;
                }
            }
        }
        catch (InputReader.InputClosedException e) {
            saveChanges();
            out.println();
            out.println("Goodbye");
        }
    }

    /**
     * Prints the seven menu options.
     */
    private void printMenu() {
        out.println();
        out.println("1. Add a friend");
        out.println("2. Remove a friend");
        out.println("3. Log a memory");
        out.println("4. View memories");
        out.println("5. Cool-Down mode");
        out.println("6. Delete a memory");
        out.println("7. Undo last change");
        out.println("8. Merge friends");
        out.println("0. Quit");
    }

    // ---- the one helper every friend action uses ----

    /**
     * Asks for a friend's name and resolves it to one Friend.
     * Exactly one match: returns it. Several (same name, different
     * nicknames): asks the user to pick. None: prints close matches as
     * "did you mean" and returns null. Also returns null if the journal
     * is empty or the user cancels.
     *
     * @return the chosen friend, or null if nothing should happen
     */
    private Friend chooseFriend() {
        if (journal.isEmpty()) {
            out.println("Nothing here yet");
            return null;
        }

        String name = input.readText("Friend name: ", NAME_MAX);
        Friend[] matches = journal.findByName(name);

        if (matches.length == 1) {
            return matches[0];
        }

        if (matches.length > 1) {
            out.println("There are " + matches.length
                + " friends named " + name + ":");
            for (int i = 0; i < matches.length; i++) {
                out.println((i + 1) + ". " + matches[i].displayName());
            }
            int pick = input.readMenuChoice(
                "Which one? (0 cancels) ", 0, matches.length);
            if (pick == 0) {
                return null;
            }
            return matches[pick - 1];
        }

        // zero matches
        Friend[] close = journal.findCloseMatches(name);
        out.print("No friend named " + name);
        if (close.length > 0) {
            out.print(". Did you mean: ");
            for (int i = 0; i < close.length; i++) {
                if (i > 0) {
                    out.print(", ");
                }
                out.print(close[i].displayName());
            }
            out.print("?");
        }
        out.println();
        return null;
    }

    /**
     * Menu 1. Warns before creating a friend whose name already exists;
     * if the user says it is a different person, a nickname is required.
     */
    private void addFriend() {
        String name = input.readText("Name: ", NAME_MAX);
        String nickname;

        if (journal.findByName(name).length > 0) {
            out.println("You already have a friend named " + name);
            if (!input.readYesNo("Is this a different person? (y/n) ")) {
                return;
            }
            nickname = input.readText(
                "Nickname to tell them apart: ", NAME_MAX);
        }
        else {
            nickname = input.readOptionalText(
                "Nickname (optional): ", NAME_MAX);
        }

        while (nickname != null && nicknameExists(name, nickname)) {
            out.println("That nickname is already used for this name.");
            nickname = input.readText("Choose a different nickname: ", NAME_MAX);
        }
        Friend friend = new Friend(name, nickname);
        journal.addFriend(friend);
        recordChange(() -> journal.removeFriend(friend));
        out.println("Added " + friend.displayName());
    }

    /** Checks labels using the same case and spacing rules as names. */
    private boolean nicknameExists(String name, String nickname) {
        for (Friend friend : journal.findByName(name)) {
            if (friend.getNickname() != null
                && Friend.normalize(friend.getNickname()).equals(Friend.normalize(nickname))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Menu 2. Removes a friend and all their memories after confirming.
     */
    private void removeFriend() {
        Friend f = chooseFriend();
        if (f == null) {
            return;
        }
        if (input.readYesNo("Remove " + f.displayName()
            + " and all memories? (y/n) ")) {
            journal.removeFriend(f);
            recordChange(() -> journal.addFriend(f));
            out.println("Removed " + f.displayName());
        }
    }

    /**
     * Menu 3. Logs one memory for a friend.
     */
    private void logMemory() {
        Friend f = chooseFriend();
        if (f == null) {
            return;
        }
        String desc = input.readText("What happened? ", Memory.MAX_LENGTH);
        MemoryType type = input.readMemoryType("Good or bad? (g/b) ");
        LocalDate date = input.readDate("Date (MM/DD/YYYY): ");
        int importance = input.readImportance("Importance (1-5): ");
        Memory memory = new Memory(desc, type, date, importance);
        f.addMemory(memory);
        recordChange(() -> f.removeMemory(memory));
        out.println("Logged");
    }

    /**
     * Menu 4. Shows every memory for a friend, newest first.
     */
    private void viewMemories() {
        Friend f = chooseFriend();
        if (f == null) {
            return;
        }
        if (f.memoryCount() == 0) {
            out.println("Nothing here yet");
            return;
        }
        out.println("Memories with " + f.displayName() + ":");
        printNumbered(f.getMemoriesNewestFirst());
    }

    /**
     * Menu 5. Shows only the good memories plus the good/bad tally.
     */
    private void coolDown() {
        Friend f = chooseFriend();
        if (f == null) {
            return;
        }
        if (f.memoryCount() == 0) {
            out.println("Nothing here yet");
            return;
        }
        out.println("Cool-Down for " + f.displayName());
        printNumbered(f.getGoodMemoriesNewestFirst());
        out.println(f.countGood() + " good vs " + f.countBad() + " bad");
    }

    /**
     * Menu 6. Lists memories and deletes the one the user picks.
     */
    private void deleteMemory() {
        Friend f = chooseFriend();
        if (f == null) {
            return;
        }
        if (f.memoryCount() == 0) {
            out.println("Nothing here yet");
            return;
        }
        Memory[] list = f.getMemoriesNewestFirst();
        int pick = input.readMenuChoice(
            "Delete which? (0 cancels) ", 0, list.length,
            () -> printNumbered(list));
        if (pick == 0) {
            return;
        }
        Memory removed = list[pick - 1];
        f.removeMemory(removed);
        recordChange(() -> f.addMemory(removed));
        out.println("Deleted");
    }

    /** Combines two chosen friend records after confirmation. */
    private void mergeFriends() {
        if (journal.friendCount() < 2) {
            out.println("Add at least two friends before merging");
            return;
        }
        out.println("Choose the source record to remove:");
        Friend source = chooseFriend();
        if (source == null) {
            return;
        }
        out.println("Choose the destination record to keep:");
        Friend target = chooseFriend();
        if (target == null) {
            return;
        }
        if (source == target) {
            out.println("Choose two different friends");
            return;
        }
        if (!input.readYesNo("Merge " + source.displayName() + " into "
            + target.displayName() + "? (y/n) ")) {
            return;
        }
        Memory[] transferred = source.getMemoriesNewestFirst();
        journal.merge(source, target);
        recordChange(() -> {
            for (Memory memory : transferred) {
                target.removeMemory(memory);
            }
            journal.addFriend(source);
        });
        out.println("Merged into " + target.displayName());
    }

    /** Records an inverse action and immediately persists the completed change. */
    private void recordChange(Runnable inverse) {
        undoStack.add(inverse);
        dirty = dataFile != null;
        saveChanges();
    }

    /** Retries pending writes; failures keep the data in memory for another try. */
    private boolean saveChanges() {
        if (!dirty) {
            return true;
        }
        try {
            JournalFile.save(journal, dataFile);
            dirty = false;
            return true;
        }
        catch (IOException e) {
            out.println("NOT SAVED: " + e.getMessage());
            out.println("Changes remain in memory. Fix the file problem and quit to retry.");
            return false;
        }
    }

    /** Reverses the latest completed change; reads and cancellations do not count. */
    private void undo() {
        Runnable action = undoStack.remove();
        if (action == null) {
            out.println("Nothing to undo");
            return;
        }
        action.run();
        dirty = dataFile != null;
        saveChanges();
        out.println("Undone");
    }

    /**
     * Prints an array as a 1-based numbered list.
     *
     * @param items the memories to print
     */
    private void printNumbered(Memory[] items) {
        for (int i = 0; i < items.length; i++) {
            out.println((i + 1) + ". " + items[i]
                + " [importance " + items[i].getImportance() + "/5]");
        }
    }
}
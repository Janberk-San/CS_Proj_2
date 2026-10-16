import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Entry point for Rewind. Runs the text menu and turns each choice into
 * calls on Journal and Friend. Holds no data of its own.
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

    /** Longest name or nickname accepted. */
    private static final int NAME_MAX = 280;

    private final Journal journal;
    private final InputReader input;
    private final PrintStream out;

    /**
     * Builds the app on the real keyboard, screen and clock, then runs it.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        InputReader reader =
            new InputReader(keyboard, System.out, LocalDate.now());
        RewindApp app = new RewindApp(new Journal(), reader, System.out);
        app.run();
        keyboard.close();
    }

    /**
     * Creates the app with injected dependencies.
     *
     * @param journal where friends and memories live
     * @param input source of validated user input
     * @param out where menus, lists and messages are printed
     */
    public RewindApp(Journal journal, InputReader input, PrintStream out) {
        this.journal = journal;
        this.input = input;
        this.out = out;
    }

    /**
     * The menu loop. Returns when the user picks 0 or the input ends.
     */
    public void run() {
        out.println("Welcome to Rewind.");
        try {
            while (true) {
                printMenu();
                int choice = input.readMenuChoice("> ", QUIT, DELETE_MEMORY);
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
                    default:
                        out.println("Goodbye");
                        return;
                }
            }
        }
        catch (InputReader.InputClosedException e) {
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

        Friend friend = new Friend(name, nickname);
        journal.addFriend(friend);
        out.println("Added " + friend.displayName());
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
        f.addMemory(new Memory(desc, type, date, Memory.DEFAULT_IMPORTANCE));
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
        printNumbered(list);
        int pick = input.readMenuChoice(
            "Delete which? (0 cancels) ", 0, list.length);
        if (pick == 0) {
            return;
        }
        f.removeMemory(list[pick - 1]);
        out.println("Deleted");
    }

    /**
     * Prints an array as a 1-based numbered list.
     *
     * @param items the memories to print
     */
    private void printNumbered(Memory[] items) {
        for (int i = 0; i < items.length; i++) {
            out.println((i + 1) + ". " + items[i]);
        }
    }
}
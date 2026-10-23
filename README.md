# Rewind — CS 2114 Project 2

A console friendship journal by **Janberk San, Vincent, and Oscar**.
Log good and bad moments for each friend. Cool-Down shows the good memories
and a good-versus-bad tally so you can consider the whole friendship.

## Run

Build with **JDK 17 or newer** (not just a JRE). There are no runtime libraries
or services to install. The generated application also targets Java 8 bytecode.

### macOS / Linux

```sh
git clone https://github.com/Janberk-San/CS_Proj_2.git
cd CS_Proj_2
./run.sh
```

`run.sh` builds and launches the real `RewindApp` entry point. It uses
`JAVA_HOME`, a local `.tools/jdk/Contents/Home` if present, or Java on `PATH`.
The local JDK is a convenience for this checkout and is not committed.

The default journal is `rewind.journal` in the project folder. To choose another:

```sh
./run.sh /absolute/path/to/my.journal
```

Relative paths passed to `run.sh` are relative to the project folder. The parent
directory must already exist. `./run.sh --help` prints usage.

### Maven / Windows

With JDK 17+ and Maven installed, run these commands from the project folder:

```sh
mvn verify
java -jar target/rewind-1.0.0.jar
```

You can also run `./build.sh`, then `java -jar build/rewind.jar` on macOS/Linux.
When running the JAR directly, the default file is in the current working directory.

### Eclipse

Import this folder as an existing Java project and run `RewindApp` as a Java
Application. The `.classpath` includes the JUnit 4 container for tests. Select an
installed JDK in Eclipse's Java settings. The old hello-world `Main` was removed.

## Using the journal

| Choice | Action |
| --- | --- |
| 1 | Add a friend, with an optional nickname. Same-name friends require a distinct nickname. |
| 2 | Remove a friend and their memories after confirmation. |
| 3 | Log a memory: description, good/bad, date, then importance from 1 to 5. |
| 4 | View all memories for a friend, newest first. |
| 5 | Cool-Down: show good memories only, followed by the good/bad counts. |
| 6 | Delete a numbered memory. Invalid input shows the list again; 0 cancels. |
| 7 | Undo the last completed change. Repeat to undo earlier changes from this session. |
| 8 | Merge two friend records: choose the source to remove, then the destination to keep, and confirm. |
| 0 | Quit. |

Names match regardless of capitalization or extra spaces. When multiple friends
share a name, choose the appropriate nickname from the numbered list; 0 cancels.
Misspelled names receive close-match suggestions. Dates use `MM/DD/YYYY`, must
exist on the calendar, and cannot be in the future. Descriptions are limited to
280 characters. Empty/invalid answers are explained and re-prompted.

A merge moves every memory into the destination, including equal-valued entries;
it does not guess whether two identical descriptions represent the same event.
Undo restores the original two records. Undo also supports adding/removing
friends and logging/deleting memories. Reads, invalid input, and cancelled actions
do not add undo entries. Undo history ends when the program exits.

## Storage and recovery

- The journal loads on startup and saves after every completed change, including undo.
- Quit and end-of-input retry any pending save. Incomplete prompts do not create partial entries.
- A new/missing or empty file starts an empty journal. Other read errors stop startup.
- Saves write a complete temporary file before replacing the destination, atomically
  where supported. A save error displays **NOT SAVED**, keeps changes in memory,
  and lets you retry by fixing the destination and choosing Quit again. Exiting
  with unsaved data returns a nonzero status.
- Malformed records are skipped and counted. Before the recovered journal can be
  saved, startup creates a uniquely named `.recovery-*.bak` copy of the original.
  An unknown file header/version stops startup without overwriting the file.
- A `.lock` file prevents two Rewind sessions from editing the same journal at
  once. It can remain after exit; the operating-system lock is released on exit.
  Do not delete the lock file while Rewind is running.
- Journal data, backups, compiled classes, and downloaded tools are gitignored.
  Data is ordinary local UTF-8 text, not encrypted. Use a `.journal` extension for
  custom files so the default ignore rules cover them.

See [the file format](docs/file-format.md) for hand-written examples and recovery.

## Tests and checks

```sh
./test.sh       # compile with warnings as errors, then run all JUnit tests
mvn verify      # tests, executable JAR, and repository Checkstyle checks
```

`test.sh` downloads pinned JUnit 4.13.2 and Hamcrest 1.3 JARs on its first run
and verifies their SHA-256 checksums. Later runs work offline. The application
itself needs no downloads once a JDK is installed.

The suite contains **129 tests**, including the existing domain/input tests and
new coverage for persistence, real startup/restart, save failures and retries,
recovery backups, locks, merge/undo, nickname collisions, importance, and
LinkedChain behavior. All tests use temporary files or in-memory journals;
they do not touch your normal journal. GitHub Actions runs `mvn verify` on
Java 17 and 21 for pushes and pull requests.

`config/checkstyle.xml` is a repository baseline. No instructor-specific
Checkstyle configuration or Deliverable 2 specification was included in this
repository, so course-specific grading requirements must be checked separately.

## Completion and data structures

The prototype's documented remaining work is implemented: native file storage,
undo, merge, importance input, delete retries, a working entry point, tests, and
updated usage instructions. The prototype reported 114 passing tests, but the
checkout contained 91 and no `LinkedChainTest`; the missing chain suite is now
included. The intentionally dropped stretch ideas (charts, tags/search, and
weighted random reminders) remain outside this project's documented scope.

The original generic `LinkedChain<T>` implementation is unchanged. It stores
`Friend` entries in `Journal`, `Memory` entries in each `Friend`, and inverse
`Runnable` actions in the undo stack. `add`/`remove()` provide stack push/pop.
Sorting uses defensive array copies. `JournalFile` uses file-local IDs to keep
same-name friends distinct and standard parsing collections for file records.

## Attribution and AI assistance

Original responsibilities recorded by the team:

| Source / tests | Original author or owner |
| --- | --- |
| `LinkedChain` | Oscar; described in the prototype as course-provided |
| `MemoryType`, `Memory`, `Friend` and their tests | Vincent |
| `Journal` and tests | Oscar |
| `InputReader`, `RewindApp` and their tests | Janberk |
| Original README | Janberk draft, team review |

The prototype documents Claude assistance with pseudocode and test drafting.
This completion pass used Codex to implement the documented remaining features,
extend and run tests, add build/CI tooling, and update documentation. The additions
include `JournalFile`, `JournalFileTest`, `RewindPersistenceTest`, and the missing
`LinkedChainTest`, plus changes to the existing classes and tests.

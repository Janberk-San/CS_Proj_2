Rewind — CS 2114 Project 2 (Deliverable 3: Prototype)
A friendship memory journal with a Cool-Down mode. For each friend you log good and bad moments; when you're upset with someone, Cool-Down shows only the good memories plus a tally (for example, 23 good vs 4 bad) so you see the whole friendship before reacting to one bad moment.
Team: Janberk San, Vincent, Oscar
How the LinkedChain is used
One generic LinkedChain<T> class stores two element types through the same code:
Where
Field
Element type
Methods called
Journal
friends
LinkedChain<Friend>
add, remove(T), size, isEmpty, toArray(T[])
Friend
memories
LinkedChain<Memory>
add, remove(T), size, toArray(T[])


Reflection
What changed from your specification?
Classes and method signatures: nothing. All seven classes exist with the names in the spec.
What we added that the spec did not list
Named constants instead of magic numbers: Memory.MIN_IMPORTANCE/MAX_IMPORTANCE (1 and 5), Journal.CLOSE_MATCH_DISTANCE (2), and RewindApp's menu constants (ADD_FRIEND = 1 … QUIT = 0) and NAME_MAX.

Where validation lives: as specified, with one wrinkle. User mistakes are caught in InputReader and RewindApp and answered with a re-prompt; programmer mistakes are caught in Memory, Friend and LinkedChain and answered with IllegalArgumentException. 
Test cases: the plan was followed and then extended. Every row in the spec's test tables became at least one test method. On top of that we added cases the tables did not have. Total: 114 tests, all passing.
What changed in the LinkedChain?
Nothing. The application calls six of its public methods (add, remove(T), size, isEmpty, toArray(T[]), and remove(T) via Friend.removeMemory/Journal.removeFriend) 
What happened to your unknowns?
Unknown from the scope
Status
Generic node design
Figured out
Sorting an unordered bag for display
Figured out
equals() for our own classes
Figured out
Removing a specific node from a singly linked chain
Figured out
Testing console input
Figured out
File I/O (stretch)
Still open

What doesn't work yet?
No persistence. Data lives only while the program runs, as the scope allows for the MVP. The biggest thing we would like to add going forward is native data storage.

Which stretch goals will you attempt
Save and load to a file. We don't know how we would start this in a java program quite yet but we will learn
Dropping:
Friendship trend (month-by-month bar chart). Mostly formatting work; little new data-structure logic.
Tags and search across all friends. Would add the third element type (LinkedChain<String>), 
"Remind me of something good" (weighted random).
What new tests will you need?
Work item
Normal case
Bad-input case
JournalFile.save
Round-trip: save then load gives equal memories per friend, same friend count
Description containing |, quotes and a newline survives the round-trip unchanged
JournalFile.load
Load a hand-written valid file → friends and memories present
Missing file → empty journal, no exception; a line with 3 fields instead of 6, a bad date, importance 9 → each skipped and counted, rest loaded; empty file → empty journal


Who does what?
Item
Owner
Delete Main.java; re-show list on bad delete number; both RewindAppTest cases
Janberk
Checkstyle pass on InputReader, RewindApp, their tests, and this README
Janberk
Undo stack in RewindApp + RewindAppTest undo cases
Janberk
Journal.merge + JournalTest merge cases; merge menu flow with Janberk
Oscar
JournalFile save/load + JournalFileTest (all escaping and bad-file cases)
Oscar
Checkstyle pass on LinkedChain, Journal, their tests
Oscar
Importance prompt: InputReader.readImportance (Janberk) and wiring into logMemory; Memory already validates 1–5
Vincent + Janberk
Checkstyle pass on MemoryType, Memory, Friend, their tests
Vincent
Integration run of all tests at lab; final README update
all three

Attribution
One row per class and test file. Every file was built from the pseudocode in our Deliverable 2 spec, in the spec's dependency order.
File
Who
How
LinkedChain.java
Oscar (owner); course-provided
Provided class. Oscar verified its API against the spec's signature table and confirmed no modifications were needed.
LinkedChainTest.java
Oscar
Written from the spec's LinkedChainTest table, plus a generic-reuse test.
MemoryType.java
Vincent
Enum with fromInput, written from the spec's pseudocode.
MemoryTypeTest.java
Vincent
Written from the spec's MemoryTypeTest rows.
Memory.java
Vincent
Immutable value class; equals/hashCode/NEWEST_FIRST per the generic contract.
MemoryTest.java
Vincent
Written from the spec's MemoryTest table; added importance-edge and comparator tests.
Friend.java
Vincent
Domain class holding LinkedChain<Memory>; normalize and sorted views.
FriendTest.java
Vincent
Written from the spec's FriendTest table; added copy-independence and identity tests.
Journal.java
Oscar
Domain class holding LinkedChain<Friend>; name matching and Levenshtein editDistance.
JournalTest.java
Oscar
Written from the spec's JournalTest table; added nickname sort order test.
InputReader.java
Janberk
Injected-dependency I/O helper; strict date parsing; InputClosedException.
InputReaderTest.java
Janberk
Written from the spec's InputReaderTest table with scripted Scanners and a fixed today.
RewindApp.java
Janberk
Entry point, menu loop, chooseFriend and one handler per menu item.
RewindAppTest.java
Janberk
Integration tests from the spec's RewindAppTest table, plus one scripted session per bad-input case in the scope.
README.md
Janberk (draft), all three (review)
Reflection, next steps and this table.

How we used GenAI: As in Deliverable 2, we used GenAI (Claude) as a drafting partner. For this deliverable it helped turn the spec's method tables into class pseudocode as well as draft the JUnit tests from the spec's test tables. 

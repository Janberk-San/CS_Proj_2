# Journal file format, version 1

Files are UTF-8 text. The first line is exactly `REWIND|1`. Each subsequent
nonempty line is one friend or memory record. Empty files are accepted as empty
journals. Unknown versions and missing headers are rejected.

```text
REWIND|1
F|1|Alex|class
M|1|GOOD|2020-01-02|5|Helped me study
M|1|BAD|2020-01-03|2|Cancelled our plans
F|2|Alex|home
F|3|Jordan|
```

Friend records have four fields: `F|id|name|nickname`. IDs are positive integers,
unique within the file. An empty nickname means no nickname. Friends with no
memories still get a record. Name/nickname equality does not combine identities.

Memory records have six fields: `M|friend-id|type|date|importance|description`.
The friend must have appeared earlier. Type is exactly `GOOD` or `BAD`; date is
ISO `YYYY-MM-DD` and cannot be in the future; importance is an integer from 1 to
5; the trimmed description contains 1–280 characters. Duplicate memories are
kept as separate entries.

Text fields use these escapes:

| Original character | Written form |
| --- | --- |
| Backslash | `\\` |
| Pipe | `\|` |
| Newline | `\n` |
| Carriage return | `\r` |

Quotes, commas, tabs, emoji, and other Unicode characters need no additional
escaping. Unknown or unfinished escapes invalidate the whole record.

Example: `said "hello" | café` becomes `said "hello" \| café` in a field.

Loading skips invalid records, including bad field counts, unknown IDs,
duplicate friend IDs, invalid dates, types, and importance. A rejected friend
record does not insert a partial friend. Startup reports the skipped count and
backs up the original bytes before permitting autosave of recovered data.

To recover skipped information, close Rewind, inspect the named `.bak` copy,
correct malformed lines in a separate `.journal` file, and run Rewind with that
file's path. Keep the backup until you have verified the recovered contents.
Do not edit the active file while Rewind is running: a later autosave would
replace those external edits.

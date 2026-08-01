# Mini DBMS

A simplified, file-based Database Management System implemented in Java, built as coursework for **CSEN604 — Databases II**.

Records are stored as paginated, serialized objects on disk. The engine supports table creation, insertion, paged storage, linear and bitmap-indexed selection, and page-level crash recovery.

## Project structure

```
src/DBMS/
├── DBApp.java             # Public API: create/insert/select, indexing, recovery, main() demo
├── Table.java              # Table metadata: columns, page/record counts, trace log, indexed columns
├── Page.java                # A fixed-capacity page of records (records held as String[])
├── FileManager.java     # Serializes/deserializes Table, Page, and BitmapIndex objects to disk
└── BitmapIndex.java     # Per-column bitmap index (value -> bit string over all records)
```

Compiled `.class` output goes to `bin/`. Table data is persisted under a `Tables/` directory created next to the compiled classes (see `FileManager.directory`).

## Core concepts

- **Table**: identified by name, with a fixed set of column names. Metadata (page count, record count, trace log, indexed columns) is stored in `<table>/<table>.db`.
- **Page**: holds up to `DBApp.dataPageSize` records (default `2`). Each page is serialized to its own file `<table>/<pageNumber>.db`.
- **Record**: a `String[]` whose values align positionally with the table's column names.
- **Trace log**: every table operation (create, insert, select, indexing, recovery) appends a human-readable trace entry, retrievable via `getFullTrace` / `getLastTrace`.
- **Bitmap Index**: for an indexed column, each distinct value maps to a bit string (one bit per record, in insertion order) marking which records hold that value. Multi-column indexed selects `AND` the relevant bitmaps before falling back to a linear scan for any non-indexed conditions.
- **Recovery**: `validateRecords` detects missing/corrupted page files by cross-referencing the table's insert trace; `recoverRecords` re-inserts previously logged records into fresh pages.

## Public API (`DBApp`)

| Method | Description |
|---|---|
| `createTable(tableName, columnsNames)` | Creates a new table and persists its metadata. |
| `insert(tableName, record)` | Appends a record, creating a new page when the current one is full. |
| `select(tableName)` | Returns all records in the table. |
| `select(tableName, pageNumber, recordNumber)` | Returns a single record by page/record position. |
| `select(tableName, cols, vals)` | Linear scan filtered by column/value equality conditions. |
| `createBitMapIndex(tableName, colName)` | Builds and persists a bitmap index for a column. |
| `getValueBits(tableName, colName, value)` | Returns the raw bitmap string for a given value. |
| `selectIndex(tableName, cols, vals)` | Select using bitmap indices where available, linear scan otherwise. |
| `validateRecords(tableName)` | Detects records lost due to missing page files. |
| `recoverRecords(tableName, missing)` | Re-inserts missing records recovered via the trace log. |
| `getFullTrace(tableName)` / `getLastTrace(tableName)` | Retrieve the table's operation history. |

`FileManager.reset()` wipes all persisted table data (useful between test runs / demos).

## Requirements

- Java SE 8 (JDK 1.8) — see `.classpath`
- JUnit 4 — on the build classpath (see `.classpath`)

## Building & running

This is an Eclipse Java project (`.project` / `.classpath` included). To build and run without an IDE:

```bash
# Compile
javac -d bin -cp "path/to/junit-4.jar;path/to/hamcrest-core.jar" src/DBMS/*.java

# Run the demo in DBApp.main
java -cp "bin;path/to/junit-4.jar;path/to/hamcrest-core.jar" DBMS.DBApp
```

On first run, `Tables/` is created automatically next to the compiled classes to store persisted data.
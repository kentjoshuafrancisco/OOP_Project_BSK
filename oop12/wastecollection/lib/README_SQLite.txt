SQLite JDBC library
-------------------

This project can use SQLite for persistent storage. To enable SQLite support, download the SQLite JDBC driver JAR and place it in the `lib/` folder.

Recommended driver (drop-in):
- https://github.com/xerial/sqlite-jdbc/releases
  - Download a recent release, e.g. `sqlite-jdbc-3.42.0.0.jar` or similar.

After downloading, put the JAR into this `lib/` directory and run the application with the `lib/*` classpath so the JDBC driver is available at runtime.

Windows example (from project root):
  java -cp "lib/*;bin" BarangayWasteSystemFull

Linux/macOS example:
  java -cp "lib/*:bin" BarangayWasteSystemFull

Notes:
- If you use an IDE (Eclipse/IntelliJ/VSCode), add the JAR to the project's classpath or 'Referenced Libraries'.
- After adding the driver, the application will attempt to use SQLite automatically.

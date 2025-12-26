@echo off
REM Run the application including all jars in lib/ plus compiled classes in bin/
SETLOCAL
if not exist bin (
  echo Compiled classes not found in bin\ - please compile first.
  pause
  exit /b 1
)
echo Running with jars in lib\*
java -cp "lib\*;bin" BarangayWasteSystemFull
ENDLOCAL
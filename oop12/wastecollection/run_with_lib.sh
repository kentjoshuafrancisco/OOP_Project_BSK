#!/usr/bin/env bash
# Run the application including all jars in lib/ plus compiled classes in bin/
if [ ! -d bin ]; then
  echo "Compiled classes not found in bin/ - please compile first."
  exit 1
fi
echo "Running with jars in lib/*"
java -cp "lib/*:bin" BarangayWasteSystemFull
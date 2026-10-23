#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/java-env.sh
mkdir -p build/classes
find build/classes -type f -name '*.class' -delete
sources=()
for source in src/*.java; do
    [[ "$source" == *Test.java ]] || sources+=("$source")
done
"$REWIND_JAVAC" --release 8 -encoding UTF-8 -Xlint:all -Werror \
    -d build/classes "${sources[@]}"
"$REWIND_JAR" cfe build/rewind.jar RewindApp -C build/classes .
echo 'Built build/rewind.jar'

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/java-env.sh
./build.sh
mkdir -p .tools build/test-classes
find build/test-classes -type f -name '*.class' -delete
# Pinned JUnit dependencies, downloaded once and verified before execution.
verify() {
    if command -v shasum >/dev/null 2>&1; then
        printf '%s  %s\n' "$2" "$1" | shasum -a 256 -c >/dev/null
    else
        printf '%s  %s\n' "$2" "$1" | sha256sum -c - >/dev/null
    fi
}
fetch() {
    local file="$1" url="$2" checksum="$3"
    if [[ ! -f "$file" ]]; then
        curl --fail --location --retry 2 "$url" -o "$file.tmp"
        verify "$file.tmp" "$checksum" || { echo "Checksum mismatch: $file" >&2; exit 1; }
        mv "$file.tmp" "$file"
    fi
    verify "$file" "$checksum" || { echo "Checksum mismatch: $file" >&2; exit 1; }
}
fetch .tools/junit-4.13.2.jar \
    https://repo.maven.apache.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar \
    8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3
fetch .tools/hamcrest-core-1.3.jar \
    https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar \
    66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9
classpath='build/classes:.tools/junit-4.13.2.jar:.tools/hamcrest-core-1.3.jar'
"$REWIND_JAVAC" --release 8 -encoding UTF-8 -Xlint:all -Werror \
    -cp "$classpath" -d build/test-classes src/*Test.java
tests=()
for source in src/*Test.java; do
    name="${source##*/}"
    tests+=("${name%.java}")
done
"$REWIND_JAVA" -cp "build/test-classes:$classpath" org.junit.runner.JUnitCore "${tests[@]}"

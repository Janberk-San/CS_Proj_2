# Shared by build.sh, run.sh and test.sh. All paths are relative to repo root.
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    REWIND_JAVA="$JAVA_HOME/bin/java"
    REWIND_JAVAC="$JAVA_HOME/bin/javac"
    REWIND_JAR="$JAVA_HOME/bin/jar"
elif [[ -x .tools/jdk/Contents/Home/bin/javac ]]; then
    REWIND_JAVA="$PWD/.tools/jdk/Contents/Home/bin/java"
    REWIND_JAVAC="$PWD/.tools/jdk/Contents/Home/bin/javac"
    REWIND_JAR="$PWD/.tools/jdk/Contents/Home/bin/jar"
else
    REWIND_JAVA=java
    REWIND_JAVAC=javac
    REWIND_JAR=jar
fi
if ! "$REWIND_JAVAC" -version >/dev/null 2>&1; then
    echo 'A Java JDK (17 or newer) is required. Install one and set JAVA_HOME.' >&2
    exit 1
fi

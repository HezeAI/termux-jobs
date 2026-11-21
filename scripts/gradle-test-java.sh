#!/bin/bash
# Gradle 7.2 writes its test worker classpath as a bare line in a Java @argfile.
# Java treats an apostrophe in that line as a quote, even without spaces.
set -euo pipefail
args=()
for arg in "$@"; do
    case "$arg" in
        @"$GRADLE_USER_HOME"/.tmp/gradle-worker-classpath*txt)
            mapfile -t worker < "${arg#@}"
            if [[ ${#worker[@]} -lt 2 || "${worker[0]}" != -cp ]]; then
                echo "Unexpected Gradle test worker argument file." >&2
                exit 1
            fi
            classpath="${worker[1]}"
            if [[ "$classpath" == \"*\" ]]; then
                classpath="${classpath:1:-1}"
            fi
            args+=(-cp "$classpath" "${worker[@]:2}")
            ;;
        *) args+=("$arg");;
    esac
done
exec "$FORK_TEST_JAVA" "${args[@]}"

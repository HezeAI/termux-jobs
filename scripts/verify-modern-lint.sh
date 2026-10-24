#!/usr/bin/env bash
# Independent analysis stage. The APK build remains on its existing AGP/Gradle.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/build-env.sh
FORK_LINT_GRADLE="$FORK_ROOT/.tools/gradle-7.5.1/bin/gradle"
FORK_LINT_REPORT="$FORK_ROOT/.verification/modern-lint-report"
if [[ ! -x "$FORK_LINT_GRADLE" ]]; then
    echo "Prepare the SHA-256 verified Gradle7.5.1 distribution in .tools/gradle-7.5.1; see docs/fork/LINT.md." >&2
    exit 1
fi
mkdir -p "$FORK_LINT_REPORT"
exec 9> "$FORK_ROOT/.verification/modern-lint.lock"
flock -n 9 || { echo "Modern lint is already running; its stage cannot be replaced." >&2; exit 1; }
rm -f "$FORK_LINT_REPORT/summary.json" "$FORK_LINT_REPORT/lint.xml" "$FORK_LINT_REPORT/lint.html"
python3 scripts/prepare-java-verification.py --modern-lint > "$FORK_LINT_REPORT/stage.log"
FORK_LINT_STAGE="$FORK_ROOT/.verification/modern-lint"
# Never consume a stale success report if Gradle fails before analysis starts.
rm -f "$FORK_LINT_STAGE/app/build/reports/lint-results-release.xml" "$FORK_LINT_STAGE/app/build/reports/lint-results-release.html"
cd "$FORK_LINT_STAGE"
FORK_LINT_STATUS=0
"$FORK_LINT_GRADLE" --no-daemon --no-watch-fs --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
    -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
    -x :app:downloadBootstraps :app:lintRelease > "$FORK_LINT_REPORT/run.log" 2>&1 || FORK_LINT_STATUS=$?
if [[ -f app/build/reports/lint-results-release.xml ]]; then
    cp app/build/reports/lint-results-release.xml "$FORK_LINT_REPORT/lint.xml"
    cp app/build/reports/lint-results-release.html "$FORK_LINT_REPORT/lint.html"
else
    rm -f "$FORK_LINT_REPORT/lint.xml" "$FORK_LINT_REPORT/lint.html" "$FORK_LINT_REPORT/summary.json"
fi
python3 "$FORK_ROOT/scripts/check-lint-report.py" --xml "$FORK_LINT_REPORT/lint.xml" \
    --log "$FORK_LINT_REPORT/run.log" --out "$FORK_LINT_REPORT/summary.json" --gradle-exit-code "$FORK_LINT_STATUS"
exit "$FORK_LINT_STATUS"

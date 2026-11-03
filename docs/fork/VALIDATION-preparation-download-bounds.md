# Bounded agent component downloads (local, unpublished)

2026-11-03. This is the second local remediation after the structured-status
commit `8c5f88e6ff7baad50e92dcdd496b331801891db3`. It does not change the published
v2.1.2 APK, Components, tag or update channel. No APK was built for this change.

## Exact policy

`agent-download.sh` supplies all nine managed CLI/Node/Cloud download sites for
Codex, Claude Code, Gemini and OpenCode, including their normal launch contexts.
The base Ubuntu image downloader is separate and unchanged.

| Limit | Production value | Meaning |
| --- | --- | --- |
| Connection | 45 seconds | Includes connection establishment/TLS under curl's connect timeout. |
| Low-speed check | 1 byte/second over 180 seconds | Stops a stalled or extremely low-speed transfer; not a percentage or an estimated completion time. |
| Transfer attempt | 7,200 seconds | Native curl max-time for one attempt, even if it still makes progress. |
| Attempts | At most 3 total | Only the explicit helper loop retries; curl internal retries are disabled. |
| Backoff | 2 and 4 seconds | No unbounded server Retry-After wait. |
| Combined transfer/retry budget | At most 21,600 seconds of curl deadlines plus 6 seconds of pauses | Six hours plus six seconds in the three-attempt case, excluding OS scheduling and subsequent process/staging cleanup. |

The two-hour attempt ceiling is a conservative chosen allowance, not a measured
mobile-network optimum. As an arithmetic example, 100 MiB at 16 KiB/s takes
6,400 seconds (about 107 minutes) and fits. A slower/larger continuous transfer
can reach this ceiling and fail; the implementation does not promise to accept
all possible slow networks. The low-speed threshold is deliberately only 1 byte/s.
This is curl's low-speed window, not an exact reset-on-every-byte watchdog.

No limit above is a timeout for the entire environment preparation. APT/dpkg,
archive verification/extraction, CLI checks, lock acquisition and cleanup remain
separate. No package transaction or working session is killed by this change.

## Process lifetime and errors

Curl runs synchronously in the existing owned child shell. On success/failure
that shell retains its staging/lock cleanup. The Java reader still drains through
EOF and waits for its child to exit before Job.run leaves the serialized worker.
It never marks the Job inactive just because a timer expired or the window closed.
No new process-signal API, PID search, background watchdog or early queue release
was added. Closing/reopening the window retains the existing operation.

Only fixed STAGE/COMPONENT/ERROR records reach preparation status. Curl output and
Bash's own signal diagnostic are suppressed in the managed transfer group. No
raw exception/response/log fallback is restored. Download URLs, pinned versions,
SHA-256 checks, missing-only recipes and original-file preservation are unchanged.
A download's actual process exit code is retained; error 28 is described as a stall
or time limit, without inventing which timeout caused it.

Managed transfers ignore curlrc (`--disable`) so an ambient configuration cannot
silently add URLs/retries or override this fixed protocol. Environment-based
proxy and CA settings remain available. HTTPS and HTTPS redirects are required.

## Checked anew

Evidence: `.verification/preparation-download-bounds/`.

`scripts/tests/test_agent_download.py` uses real curl and a loopback HTTPS server
with a disposable fixture CA. No TLS verification bypass, external request, model,
real credential or real package installation is used. A fixture copy changes only
the policy constants: connection/idle 1 second, attempt budget 12 seconds,
three attempts, 1/2-second pauses; a separate ceiling case uses 1 second/attempt.
The production helper hash is saved with successful final results.

Separate scenarios cover:

1. Accepted TCP connection with no TLS handshake response.
2. Verified TLS/HTTP connection, one body byte, then a complete stop.
3. Slow continuous response lasting longer than the scaled idle interval; full
   bytes received successfully. This is distinct from case 2.
4. Three HTTP failures, exact retry exhaustion, and a transient failure then success.
5. Native per-attempt deadlines exhausted while a slow response still progresses.
6. A subsequent explicit invocation receiving a complete successful body.
7. External interruption of only the fixture curl after checking PID, parent,
   start identity and fixture URL. Exit 143 and a fixed interruption record;
   no test signal reaches a working session.
8. Production Java Job/start/prepare/reader with synthetic Context/checker/proot:
   another agent remains queued; the same agent joins the existing Job; detaching
   the view observer does not stop it. The child shell's EXIT cleanup is deliberately
   held for one extra second. The next Worker is forbidden to start before the
   cleanup-complete marker and owned-child exit. It then starts, and retry succeeds.

The existing synthetic-output pipeline checks also run again. JVM/Robolectric
preparation/launcher/permissions tests validate status allowlists and copy of the
helper into all four launch directories. Shell routing tests cover consent,
missing-only behavior, preservation and hash failure before extraction. URLs and
all pinned SHA values are compared against the first local remediation.
Final checks passed: 49 JVM/Robolectric tests (no failures/skips), 13 host-routing
tests, existing preparation shell checks, locale checks and all five modules of
modern Lint (zero errors, 234 warnings). All final production Java/assets match
the verification stages. Per-run source hashes and test summaries are saved.

## Corrections and limits of verification

An initial test reused a fixed stub directory and picked up an old probe class.
It failed compilation; the test now creates a fresh task-owned directory each run.
An initial design used `/proc/uptime` for an aggregate timer. That file was observed
to remain constant in this execution environment while time.monotonic advanced.
That design was rejected before commit. The final implementation uses native curl
attempt timers and a finite retry count, with no proc-clock dependency.
The initial shell signal diagnostic was found and suppressed; final tests require
all returned lines to belong to the fixed structured protocol. Failed/intermediate
logs are retained and are not reported as successful final checks.

Reused tools: existing local SDK/JDK/Gradle/dependency caches, not old test outcomes.
JVM checks use `testDebugUnitTest`; Lint analyzes release sources. Neither verifies
a new signed release APK. No push, APK build or publication was performed.

Not checked: actual Android UI, real clean Ubuntu/component installation, normal
production-length timers on a phone, mobile network handovers, actual on-device
curl build/DNS behavior, and arbitrarily slow filesystem/package cleanup. These
remain explicit device/integration checks. A bounded download is not a proof that
all other preparation steps are bounded or that every OS stall is interruptible.

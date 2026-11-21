# Current project handoff

## Current goal

Prepare and verify the stable **2.1.9 / 1549** release on the published
fork/base ancestry. The [release candidate](docs/fork/VALIDATION-v2.1.9.md)
has a signed ARM64 APK and its RU/EN update metadata. The Git setup and path
fix keep the exact directory selected in the project browser. The signed APK,
update metadata and local verification logs are under
`.verification/release-v2.1.9/`.

Before publishing, verify release test results, the clean unsigned comparison
and the exact source commit. After publication, verify the public downloads,
updater metadata, tag ancestry and final fork/base CI. The previous stable
release remains active until the new release is published. Phone installation
requires its own authorization.

## Source and earlier builds

The patch from `/storage/emulated/0/termux-job's` was applied in the original
Ubuntu AI build checkout at `/root/project/Termux-job's`. The 117 affected
paths matched the expected baselines and patch bytes before the targeted
build fix. The comparison and prior copies are under
`.verification/next-build-import/`.

Prior private artifacts remain available. See the [test7 verification](docs/fork/VALIDATION-2.1.9-test7.md),
the [test6 verification](docs/fork/VALIDATION-2.1.9-test6.md),
the [test5 startup report](docs/fork/VALIDATION-2.1.9-test5.md)
and the [test4 creation form report](docs/fork/VALIDATION-2.1.9-test4.md).
The [test3 report](docs/fork/VALIDATION-2.1.9-test3.md)
records the 17 failures in 237 JVM tests, including chat scroll positioning,
action dialogs and instruction screens. These areas were outside the test4
change. See also [test2 validation](docs/fork/VALIDATION-2.1.9-test2-cli-update.md)
and [test1 validation](docs/fork/VALIDATION-devkit-timeline-2026-11-20.md).
The last published stable release remains [2.1.8 / 1540](docs/fork/VALIDATION-v2.1.8.md).

## Earlier phone acceptance

For the delivered build, check the updater from both agents' Ubuntu AI chat
menus and confirm that a running chat switches versions only after the next
launch. Verify Antigravity sign-in on opening a new chat while signed out.
Check terminal sign-in, the first reply, and the remaining test6 instruction,
skill and preparation flows. The earlier chat rendering, timeline animation, action details,
child history and persistence of Codex settings also await phone acceptance.

## Workspace and environment

The local Java test entry points now select JDK 11 and a short socket fixture
directory before Gradle starts. The standard runner supports repeated
`--tests` filters for focused retries. See [build notes](FORK.md#checks-and-local-builds)
and local evidence in `.verification/test-toolchain/`.

The original Linux checkout uses `/root/project/Termux-job's`. Its first local commit records
the current imported source tree, including the work already present when Git
was initialized. The original archive and manifest remain the historical
baseline for comparison. No remote is configured. If Git is missing from PATH,
run `.tools/git-local/usr/bin/git` from the project root. Follow the
[project rules](AGENTS.md) and read the sole [project memory](LEARNING.md).

The current Ubuntu AI environment has Codex CLI 0.171.0 in `/usr/local/bin`.
After restarting the chat, `codex --version` resolved to 0.171.0. The launcher
uses the existing private Node interpreter. The pinned 0.163.0 binary was
preserved. Version and app-server help were verified with a fresh Ubuntu PATH.
Live provider integration remains unverified. The [CLI check record](.verification/cli-update/status.json)
has the details.

## Open issues and boundaries

- The older X11 notification recovery and active-desktop preservation scenarios
  still need device checks. The historical Arch X11 process had no confirmed
  test-session owner. See the [notification report](docs/fork/VALIDATION-X11-NOTIFICATIONS.md).
- Image accumulation and Codex disconnects remain open in the
  [media investigation](docs/fork/MEDIA-CONTEXT-INVESTIGATION.md).
- Android handoff and Keystore, parts of export/restore, live Gemini API/Vertex,
  and legacy/manual paths have coverage gaps in the
  [audit matrix](docs/fork/SECURITY-AUDIT-STAGE2.md).
- The historical exit to Android Home during long scrolling has no confirmed
  cause or resolution. See the [original report](docs/fork/VALIDATION-2.1.5-test7.md).
  Check the relevance of older phone and GPU scenarios before acting on them.
- Preserve working environments, processes, chats, and authentication, as well
  as prior APKs, published tags, update.json, and Components. Voice input was
  removed. MCP and plugins are outside scope.

For earlier work, see the [primary handoff archive](docs/fork/history/HANDOFF-through-2026-11-09.md),
[legacy handoff archive](docs/fork/history/HANDOFF-legacy-through-2026-11-09.md),
and [investigation history](docs/fork/history/INVESTIGATION-NOTES-through-2026-11-09.md).
Archive statuses and permissions apply only to their original tasks and dates.

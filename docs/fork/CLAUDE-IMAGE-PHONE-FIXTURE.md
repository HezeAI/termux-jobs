# Claude image phone fixture — 2026-11-07

User explicitly requested the former fake-authorization workflow, usable in the app.
No APK/source-runtime changes. Current test19/1514 remains the supplied build.

Entry: `/root/termux-fork/.verification/claude-image-phone/launch-claude`.
Working directory: `/root/termux-fork/.verification/claude-image-phone/project`.
New Claude / Sonnet / manual / Ubuntu AI profile. Send `IMAGE TEST`, allow the
fixture Python command once. The provider is started by the connection wrapper
for this profile and stops with its controller. No global shell/auth/profile edits.
This is synthetic Anthropic-compatible traffic, not successful Anthropic login.

## Checked anew

Existing phone-permission fixture mechanisms reused with a separate provider copy
under `.verification/claude-image-fixture-code`. Native CLI hash and Node22.22.1
validated by prepare; binaries linked rather than copied/installed. Fresh HOME,
CLAUDE_CONFIG_DIR, git project and fake key; no actual account files supplied.
Kernel local-network-only guard freshly built and tested: owned listener works;
external/foreign-localhost/UDP/raw/fast-open/exec/traversal checks block access.
This is not an OS filesystem jail. Only fixed fixture-owned Bash and Read actions
are emitted. Logs contain bounded metadata, not prompts/keys/inline image bodies.

Native probe runs ordinary bootstrap/controller/CLI in a PTY and sends IMAGE TEST
through the production chat socket. Bash creates valid1280x720PNG (58,323bytes),
Read reads that image, then Read reads image-info.txt. Provider receives one image
block; snapshot contains exactly one image action, statuscompleted, exact original
path. PNG decoded/verified with Pillow. Stop acknowledged and owned process exited.
No other chat/session/X11 was started or stopped. No installation or publication.

Initial probe wiring pointed to the old helper and failed source-identity checking;
corrected to the new helper. Next probe reached completion but its assertion did
not handle null actionPreview on other rows; assertion corrected. Both initial logs
are retained, not counted as successful tests.

Evidence: `.verification/claude-image-fixture-code/native-probe.log`,
`guard-check.log`, and fixture `native-proof.json`, `image-snapshot.json`,
`identity.json`, `runs/provider-*.json`. Stable entry above points to the unique
fixture directory named in `.verification/claude-image-fixture-code/root.txt`.

## Not checked

Actual Android card→thumbnail→zoom/export needs user interaction. The other phone's
failing threads-menu.png is not provided and its cause is still unestablished.
The generated PNG is not a real desktop screenshot or a genuine model vision answer.
The native/projection test is not labelled phone UI success.

Instructions: `/sdcard/Download/Claude-fake-image-test.txt`. Prepared paths exist on
this phone only. Keep fixture/helper/runtime links while the user tests; no cleanup.

## Existing-profile failure diagnosed — 2026-11-07

User's TESTCLAUDE controller was launched with `chat=id` and an existing thread UUID.
Whitelisted process-environment checks confirmed the synthetic key, loopback provider
and isolated HOME reached that controller; no real credentials were read or printed.
That UUID had no history file under the isolated fixture configuration.
A separate native probe reproduced `No conversation found with session ID`, followed
by `Claude Code exited (1)`. Stop was acknowledged and the owned probe exited.
The first diagnostic probe stopped during initial auth startup; it was inconclusive.
Its wait condition was corrected to wait for error rows, rather than needsTerminal.
A fresh `new` probe then passed Bash→PNG→Read→one completed image action again.
Evidence: `.verification/claude-image-fixture-code/resume-missing.log` and
`native-probe-recheck.log`. This diagnoses the missing-history launch failure; it
does not establish the cause of the separate native-terminal connectivity warning.

Create a new test profile with the connection command set BEFORE its first launch.
Do not retrofit the fake HOME onto an existing chat and resume its old UUID.
No user session was stopped, restarted, or modified during this diagnosis.
Android card/zoom/export remain unverified by the assistant.

## Larger PNG — 2026-11-07

User reports thumbnail→original works on the phone with the small fixture.
This is user evidence only; no decoder fix was made during fixture setup.
Generator now produces a deterministic noisy gradient PNG, 4096×3072,
30,822,506 bytes (29.39 MiB). Original small generator/image/info preserved
with `-small-original` filenames. Native probe passed Bash→Read→one image action
again; Pillow fully decoded the larger file. Evidence: native-probe-heavy.log.
Send IMAGE TEST again in the existing fixture chat and open the NEW action.
The already running scenario server still has a hardcoded 1280×720 reply label;
image-info.txt and actual PNG dimensions are authoritative for this heavier test.
No app code, provider process, or user session was restarted or changed.
Heavy-image Android zoom/export remain unverified.

User follow-up on 2026-11-07: the larger PNG also opens successfully. This is
user-reported phone evidence, not an assistant device test or reproduction of
the original failure on the other phone.

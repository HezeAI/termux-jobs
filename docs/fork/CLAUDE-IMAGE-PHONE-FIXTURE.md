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

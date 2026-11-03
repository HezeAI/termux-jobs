# Private test28 — Agent skills

Target: com.termux, 2.1.1-test28 / versionCode1463, same signer as test27.
Active checkout: `/root/termux-fork/.verification/rollback-live-actions/source`.
Includes existing dirty/new instruction-editor work from test27; no reset to HEAD.
Guide: [Agent skills](AGENT-SKILLS.md). No public release is prepared here.

## Scope and review

General settings adds one entry beside Agent instructions. The UI reuses the
existing Ubuntu helper boundary and file browser. Requests dispatch to a separate
non-executing package module. The existing instruction editor/default initialization
continues down its old branch. Only skill requests have a bounded longer timeout.
No new chat engine, process-group termination, session/preset migration, auth reader,
installer, environment update, X11 layout or driver path is introduced.

Reviewed trust boundaries: public-only HTTPS GitHub redirects and pinned commits;
no credential helper or imported commands; UTF-8/safe scalar metadata; bounded
snapshots/archives; path/link/hardlink/traversal/special-file rejection; exclusive
lock, content conflicts, atomic install/exchange, transaction recovery; one outside-
scan rollback; external packages read-only; canceled previews and late UI callbacks.
Skill packages are untrusted instructions: validation is not a malware audit or
proof that every declared dependency is available. Native agent permissions remain
unchanged. Common secret filenames are refused, not a general content-secret scan.

## Checked anew

- 36 Python manager cases: both agents and scopes; folder/ZIP and pinned mocked
  GitHub; multiple candidates; unsafe/invalid/corrupt/oversized archives; links and
  hardlinks; sensitive filenames; metadata; name/local-change conflicts; immutable
  preview snapshots; cancellation/preview cap; atomic rename failure; interruption
  before/after exchange and explicit recovery; update/rollback/delete; external and
  plugin ownership; compact list/explicit inspection; excluded .git metadata;
  dependency declarations displayed without execution.
- 54 instruction-backend cases passed (45 Codex/common +9 Claude), covering the
  unchanged editor semantics and shared helper regression.
- Java/Robolectric suite covers the skills UI, instructions UI, helper commands and
  agent environments. Includes explicit agent/scope, no model/write on opening,
  errors, selected state, scrollable preview with footer and RU/EN narrow/large-font/
  reduced-height geometry. This is model coverage, not phone acceptance.
- Real Codex0.166.0 app-server, isolated HOME/CODEX_HOME/project: user .agents,
  legacy .codex, project .agents discovery; same names listed separately;
  added skill found with skills/list forceReload. No model request.
- Real pinned Claude2.1.296, isolated HOME/config/project and loopback synthetic
  provider: initialize catalog and explicit harmless skills confirmed personal and
  project discovery; personal wins same-name project collision. A new skill was
  not loaded merely by writing its file; /reload-skills then loaded it in the same
  process. This does not prove arbitrary skill quality or every version's behavior.
- Actual public GitHub preparation/install in an isolated fixture: anthropics/skills,
  subdirectory skills/skill-creator, resolved commit
  ca45fc496a3436251f191354091d557da31c15b5. SKILL.md installed, content hashes recorded;
  scripts not executed. Mocked tests separately cover revision resolution and errors.
- Final source/helper syntax, build, Lint and exact APK gates are recorded below.

## Reused

- Existing local SDK/JDK/Gradle/dependency caches; no full SDK/rootfs/model copies.
- Packaged native output reuse is checked against signed test27. 148 relevant native,
  terminal and build-source paths match test27's recorded hashes; no new paths in
  those modules. The final APK compares all three native library bytes again.
- Existing instruction templates/user files remain unchanged. The active shared
  `/root/.codex/AGENTS.md` and main project AGENTS.md match test27's recorded hashes.
  The project AGENTS.md remains19,109 bytes, below the restructuring trigger.
- This reuses native outputs/unchanged source evidence, not a new graphics run or
  proof of every device's current environment. No claimed external-network repair.

## Not checked / device acceptance

- Real phone keyboard/touch/accessibility, enlarged-font rendering, narrow-screen
  navigation, long paths and repeated closing/reopening still require acceptance.
- No arbitrary third-party skill execution/dependency installation. Functionality
  remains not checked; directory presence never becomes confirmed discovery.
- No working CLI sessions restarted/queried with model requests; no working tunnels,
  servers, Ubuntu installs, desktop/driver runtime or user auth were modified.
- No private GitHub support; local sources must be accessible inside Ubuntu AI.
  Conservative YAML, file/archive/preview limits are explicit in the guide.
- Custom launchers, enterprise policy and unusual plugin roots can change native
  discovery. The UI does not claim an exhaustive active-process catalog.

## Initial failures retained

The first manager test exposed mixed-scope list ordering; the list now filters the
selected scope and reports cross-scope name collisions separately. A fixture also
incorrectly assumed an external package had a manager revision; it now checks the
actual read-only path. The initial Claude test assumed automatic late discovery;
that failed, leading to the verified native /reload-skills scenario and conservative
UI wording. The first JVM run exceeded Unix-socket path limits in its temporary
fixture; the existing short project-local temp path fixed it. Initial modern Lint
reported WrongConstant for a literal orientation; replaced with LinearLayout.VERTICAL.
Original logs/summaries remain, and failed runs are not counted as successful gates.

Compact evidence: `.verification/agent-skills-test28/`, source/input inventories,
native fixture result.json files and referenced build/runtime/signature summaries.

## Final gates and artifact

- Final relevant JVM run: **69 passed**, no failures/errors/skips (53 instructions
  UI, 6 skills UI, 4 helper-command, 6 agent-environment cases).
- Full modern Lint7.4.2, all five modules: **0 errors, 236 warnings**, no
  infrastructure diagnostics. Existing compileSdk/plugin and metadata/toolchain
  warnings remain visible. This private build used full Lint conservatively.
- Final ZIP integrity, package/version, ARM64-only ABI, non-debuggable manifest,
  APK Signature Scheme v2 and single expected signing certificate passed.
- Runtime gate: 14,115 defined classes; 31 unresolved types all covered by explicit
  existing review notes; no unreviewed missing required runtime definitions.
- All three native libraries are byte-identical to test27. Packaged helper/templates
  match source; shared templates remain byte-identical to test27. Only the new
  skill helper and its dispatch are added to assets. Private/auth/key filenames
  were not found; this is an inventory guard, not a universal secret-content scan.
- The extracted final-APK helpers were executed with Python `-I` in a fresh HOME:
  skills/list dispatch resolved the fixture HOME correctly, did not create general
  instructions, and returned no model requests. Download copy SHA-256 matches.

**Download:** `/sdcard/Download/Termux-Jobs-test28.apk`\
**Identity:** com.termux, 2.1.1-test28, versionCode1463, 43,094,279 bytes.\
**SHA-256:** `0e8dba69a134df6854e19d27ff811321e0bf39b8f3a0ed6615ddee67e9e7b51f`\
**Certificate SHA-256:** `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

VersionCode exceeds test27/1462 and earlier issued reserves. No existing settings
or session schema changes, resets or automatic instruction replacement. Working
sessions and connection routes remain untouched. Phone update/data acceptance is
separate from static identity checks. Existing settings backup format was not
expanded to include native skill packages or their management records.

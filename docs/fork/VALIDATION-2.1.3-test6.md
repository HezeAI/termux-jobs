# Private test APK 2.1.3-test6

2026-11-04. VersionCode 1482. Signed release variant for device acceptance.
No push or publication. Published v2.1.2, its tag/update metadata and Components
remain unchanged. This build preserves old profiles and existing Ubuntu AI.

**Attachment accumulation/disconnections are not fixed in this APK.** The separate
investigation covers images, video frames, large text/documents and tool/MCP
results across Codex, Claude, Gemini and OpenCode. No common fix is claimed.

## Included changes

Includes test5's audit fixes and Codex startup/verified policy, plus local
`e676dd42`: OpenCode connection state export/restore with a separate default-off
choice. Both RU/EN forms warn before export about plaintext credentials, history,
logs, internal project snapshots, and incomplete recovery when excluded.

Connection state is restored to a fresh UUID without replacing existing data.
Old v1/v2 archives remain readable; selected connection exports use v3 and require
the new reader. Restoring a receipt does not itself grant permission or execute
its command. See [backup contents](BACKUP-RESTORE.md) and the [audit](SECURITY-AUDIT-STAGE2.md).

Основной [актуальный план аудита](SECURITY-AUDIT-STAGE2.md#актуальная-матрица-исправлений-и-оставшихся-действий)
содержит статусы, доказательства, ответственных, исключения и условия завершения.
Дополнительные тесты и исправления фикстур: локальный коммит `687674b2`.

## Verification scope

Evidence: `.verification/audit-test6/`, `.verification/release-2.1.3-test6/`
and matching `.verification/opencode-backup-fix/` inputs. Fixtures use artificial
keys/files and a kernel-enforced own-listener network guard. No real account
credentials or working sessions are used. Debug JVM tests are internal; only
a signed release APK is delivered. Local-provider success does not verify a
production account, Android/proot transport or device Keystore.

The additional saved-profile recovery test writes ciphertext to an isolated
WorkspaceStore, closes/reopens it, observes owner death without decrypting,
rejects the old run ID and delivers an artificial key only to the new run.
Android socket credentials are shadows and the AES key is synthetic: this is
not app-process death or Android Keystore acceptance on a phone.

The first Node run found a missing parent in the auth fixture and blocked nested
Unix paths in the chat fixture. A retry revealed a stale synthetic callback that
omitted production delivery metadata. The harness now uses fresh private fixture
roots matching the unchanged guard; the callback uses production historyMeta.
A repeated attempt also exposed reused fixture folders (EEXIST); final attempts
use fresh folders. Original failed attempts remain recorded, not counted as passes.
No application fallback or relaxation of the network filter was introduced.

## Checked anew

- **65 scoped JVM methods**, 0 failures/errors/skips. Includes 8 key-broker tests,
  ciphertext save/reopen/owner-death/new-run recovery, API/Vertex UI/policy,
  backup consent/import/default-off choice and session lifecycle. Task executed.
- **7 explicit native integration methods / 65 scenarios** for four pinned CLIs,
  including actual application permission answers, schedulers, shell/direct file
  effects and new-profile save/reopen. Kernel network negatives pass anew.
  Native versions: Codex 0.166.0, Claude 2.1.296, Gemini 0.64.0, OpenCode 2.0.28.
- **10 Node suites** pass with fresh private fixture roots and the unchanged
  kernel network guard: key transport/crash, API/Vertex, owned socket chat,
  permissions/receipts, dedup/retry and Gemini/OpenCode recovery. No cloud login.
- **8 release-signer fixture cases** pass; no real private key contents exported.
  Localization: 586 mapped strings and format contracts pass.
- Full modern Lint 7.4.2, all five modules: **0 errors, 236 warnings**;
  no rule-loading/infrastructure failure. Compile SDK exceeds AGP's tested range.
- Final APK: **com.termux, 2.1.3-test6, versionCode 1482**, ARM64 only,
  non-debuggable signed release, **40,470,938 bytes**. APK v2 signature verifies.
- APK SHA-256: `3ffcda395d46b2977ae6c4cc58ff5f34adb5cfd7670f3943c7a4eac59351e263`.
- Public certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP integrity/outer container, all 121 assets, seven ARM64 ELF payloads,
  573 production Java/resource/asset inputs and DEX/runtime checks pass.
  DEX definitions: 14,202; optional reviewed unresolved types: 31.
  OpenCodeBackupRestore and GeminiKeyBroker are packaged; notices remain.
  No voice feature, test harness, synthetic key or cache is packaged.
- Mandatory verifyReleaseSigner and verifyX11Inputs ran successfully. Legacy
  packaging Lint/Kotlin metadata diagnostics remain in build.log; they are not
  counted as a successful static analysis. The modern independent gate passes.

## Reused with input checks

- 89 Python backup tests and both real OpenCode clean export/restore/continued
  profile cycles from e676dd42; all 11 recorded functional production/test/runner
  inputs still match. Current backup JVM tests executed anew above.
  The network guard, force-execution init and pinned CLI inventory match the
  same Git commit. Documentation/version-only changes do not alter those paths.
- Three native libraries reused byte-for-byte from the verified v2.1.2 APK.
  All 95 recorded native source/config inputs and compile/link build script match.
  Java/resources/assets rebuilt using existing local SDK/Gradle/dependency caches.
  No fresh GPU execution or clean dependency download is claimed.
- Developer Codex test5 phone acceptance retained with its exact reported scope.
  No new phone acceptance or GitHub CI run is claimed.

## Remaining phone acceptance

Use separate test profiles and disposable directories with artificial data.
Do not log out of working accounts, replace authorization or stop active sessions.
Never send keys, backup archives or full private diagnostics.

| Action | Expected result | Report on failure |
| --- | --- | --- |
| Open full and selective backup in RU/EN. | Connection-data choice starts off; secret and incomplete-recovery warnings appear before export. Selecting library alone does not select connection data. | Language, form, selections, safe error. |
| On a spare clean test instance, export a fake OpenCode profile with connection data, restore and reopen it. | New connection/profile; original remains intact; history continues after explicit review. Import runs no commands. New shell/write still asks; No preserves the test file. | CLI version, selected components, approval choice and artificial file result. Do not send archive. |
| In that instance, read an old v1/v2 archive or exclude connection data. | Profile imports; missing connection state is clearly stated and needs separate setup. | Archive format, selections and safe error. |
| Save/reopen a Gemini API test profile with an artificial invalid key; start, cancel, retry. | Mode/policy survive. Key handoff succeeds or reports a safe error. Provider rejection of a fake key is expected, not a successful login. | Stage, error code and exact cancel/retry order; no key. |
| On an isolated instance with no working sessions, end the app process during a test Gemini launch, reopen and retry. | No duplicate launch or cross-profile key use; retry starts or gives an explicit error. Closing a window is a separate operation. | Stage before termination, method used, state and result after reopen. Skip on a working phone. |
| Create disposable Claude/Gemini/OpenCode profiles. Reject shell and direct file changes; inspect markers, then allow once. | No leaves files unchanged; Allow changes them. Outside-project native gates may reject before our card. | Agent, tool, path inside/outside, choice and marker. |
| With a separate test account only if available, check Gemini API and Vertex access. | Correct requested auth mode; successful test access or clear provider/IAM error, without fallback. | Mode and safe stage/error; no credentials. |

### Безопасное завершение процесса для проверки

1. В общем рабочем приложении создайте отдельную Gemini-сессию с искусственным
   ключом и одноразовой папкой. Для обычной отмены используйте её штатную кнопку
   остановки с подтверждением. Не применяйте «закрыть всё», killall/pkill или
   принудительную остановку общего приложения. Остановка сессии проверяет
   отмену/повтор, **не аварийное восстановление процесса Android**.
2. Проверку аварии приложения выполняйте только на отдельном тестовом телефоне
   или независимо изолированном экземпляре с собственными данными. Убедитесь,
   что там нет рабочих сессий и авторизаций, а единственный запущенный профиль —
   искусственный Gemini. Отдельный профиль внутри рабочего приложения недостаточен.
3. Начните тестовый запуск, запишите показанный этап, затем в настройках Android
   остановите именно этот тестовый экземпляр. Откройте его снова и повторите
   запуск сохранённого профиля. Ожидаются прежний режим, отсутствие дубля/чужого
   ключа и запуск либо понятная безопасная ошибка. Зафиксируйте, остался ли
   прежний дочерний процесс; не считайте его завершившимся только по закрытому окну.
4. Если изоляцию экземпляра или принадлежность процесса подтвердить нельзя,
   **пропустите аварийную проверку** и сообщите это. Не подставляйте PID вручную
   и не завершайте неизвестный процесс. Телефонная проверка остаётся незавершённой.

Codex new-profile startup and shell/direct-edit No/Allow passed the developer's
test5 phone acceptance. That result is retained, and does not close the remaining
agents, authorization, process recovery or backup checks above.

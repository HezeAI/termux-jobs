# Release signer gate (local remediation of F-SIGN-01)

2026-11-03. Release now uses an explicit signingConfigs.release inheriting the
existing key location, alias and credential settings. The expected public
certificate SHA-256 is pinned in gradle/release-certificate.sha256. No password,
keystore or private key was added to the repository.

The ARM64 release entry point fails immediately when its existing keystore is
missing and no longer calls create-debug-key.sh. App release validation/packaging
and assemble/bundle/sign tasks depend on verifyReleaseSigner. The task has no
cached output: it checks the selected public certificate on each invocation.
Missing key/pin, malformed pin, unavailable certificate or mismatch fail closed.
There is no new skip flag, key regeneration or automatic pin replacement.

The verifier uses KeyStore.getCertificate, not getKey. Its temporary password
array is cleared; it does not claim total JVM memory erasure. The existing signing
settings and AGP signer still use the existing key normally for the authorized APK
build. The guard itself only requests a public certificate. Credentials and raw
keystore bytes are never printed/exported.

Checked anew: eight Gradle cases on a generated artificial JKS (missing key,
wrong certificate, missing pin, wrong password, missing alias, success, mismatch
again, successful retry); separate release-entry missing-key fixture proves no
replacement generation. The exact verifier also passed against the existing
public certificate: dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.
Gradle dry-run confirms the gate precedes release packaging; that dry-run itself
is not an APK build. Evidence: .verification/release-signer-check/.

Final signed APK verification remains mandatory. Neither this gate nor a matching
certificate proves secure past key custody, absence of private-key compromise,
code safety, independent audit or compatibility with official Termux builds.
The published v2.1.2 APK/tag/Components remain unchanged. Device installation and
UI tests are separate; no APK was installed by this task.


## Урок: 2026-11-03 — Release signer проверяется до упаковки, а не подменяется

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Release signer проверяется до упаковки, а не подменяется

- Контекст: локальное исправление F-SIGN-01 после аудита опубликованного v2.1.2.
- Вывод: `scripts/build-local-arm64.sh` больше не создаёт отсутствующий ключ. Release packaging требует существующий ключ и публичный SHA-256 из `gradle/release-certificate.sha256`; отсутствие/несовпадение прекращает сборку. Проверка публичного сертификата не доказывает прошлую сохранность закрытого ключа.
- Применимость: локальные release entry points и задачи AGP packaging/signing. Lint/анализ исходников не должны требовать ключ лишь из-за preReleaseBuild; обязательная зависимость стоит на release signing/packaging/assemble/bundle.
- Подтверждение: отдельный коммит `d3ea24ae`, 8 Gradle-случаев с искусственным JKS, проверка missing-key entry point и финального APK 2.1.3-test1 с прежним публичным сертификатом. См. docs/fork/VALIDATION-release-signer-gate.md актуального checkout. APK не установлен и не опубликован.
